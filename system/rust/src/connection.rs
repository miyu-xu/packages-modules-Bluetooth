//! This module manages LE connection requests and active
//! LE connections. In particular, it de-duplicates connection requests,
//! avoids duplicate connections to the same devices (even with different RPAs),
//! and retries failed connections

use std::{collections::HashSet, fmt::Debug, hash::Hash, rc::Rc, time::Duration};

use tokio::{task::spawn_local, time::timeout};

use crate::{core::address::AddressWithType, gatt::ids::ServerId};

use self::{
    acceptlist::get_target_parameters,
    connection_attempt_manager::ConnectionAttempts,
    hci_operations::{HciConnectProxy, HciEvent},
    state_machine::{HciConnectionStateMachine, InitiatorStableState},
};

mod acceptlist;
mod connection_attempt_manager;
mod ffi;
pub mod hci_operations;
mod state_machine;

pub use ffi::{
    register_callbacks, AddressManagerImpl, HciConnectProxyImpl, LeAddressManagerShim,
    LeConnectHciManagerShim,
};

/// Possible errors returned when making a connection attempt
#[derive(Debug)]
pub enum CreateConnectionFailure {
    /// This client is already making a connection of the same type
    /// to the same address.
    ConnectionAlreadyPending,
}

/// Errors returned if a connection successfully starts but fails afterwards.
#[derive(Debug)]
pub enum ConnectionFailure {
    /// The connection attempt was cancelled
    Cancelled,
}

/// Errors returned if the client fails to cancel their connection attempt
#[derive(Debug)]
pub enum CancelConnectFailure {
    /// The connection attempt does not exist
    ConnectionNotPending,
}

/// Unique identifiers for a client of the connection manager
#[derive(Debug, Hash, PartialEq, Eq, Clone, Copy)]
pub enum ConnectionManagerClient {
    /// A GATT client with given client ID
    GattClient(u8),
    /// A GATT server with given server ID
    GattServer(ServerId),
}

/// Interface supplying callbacks into the address manager to acknowledge operations
pub trait AddressManager: Debug {
    /// Acknowledge pause completion
    fn ack_pause(&self);
    /// Acknowledge resume completion
    fn ack_resume(&self);
}

/// The role of a device in a connection
#[derive(Copy, Clone, Eq, PartialEq, Debug)]
pub enum Role {
    /// We connected to a peer's advertisement
    Central,
    /// The peer connected to our advertisement
    Peripheral,
}

/// An active connection
#[derive(Copy, Clone, Debug)]
pub struct Connection {
    /// The address of the peer device, as reported in the connection complete event
    pub remote_address: AddressWithType,
    /// Our role on this connection
    pub role: Role,
}

/// Responsible for managing the initiator state and the list of
/// devices on the filter accept list
#[derive(Debug)]
pub struct ConnectionManager {
    initiator: HciConnectionStateMachine,
    hci: Rc<dyn HciConnectProxy>,
    /// The addresses currently connected to
    current_connections: HashSet<AddressWithType>,
    /// The current accept list provided to the controller
    current_accept_list: HashSet<AddressWithType>,
    /// These are all the connection attempts that have not been resolved,
    /// mapping to closures that will clean them up
    /// TODO(aryarahul): make sure we do address resolution correctly here, so if we know what a device is, we
    /// always use the identity address and not some RPA
    active_connection_attempts: ConnectionAttempts,
}

const DIRECT_CONNECTION_TIMEOUT: Duration = Duration::from_secs(30);

impl ConnectionManager {
    /// Constructor
    pub fn new(
        hci: Rc<dyn HciConnectProxy>,
        address_manager: impl AddressManager + 'static,
    ) -> Self {
        Self {
            initiator: HciConnectionStateMachine::new(hci.clone(), address_manager),
            hci,
            current_connections: HashSet::new(),
            current_accept_list: HashSet::new(),
            active_connection_attempts: ConnectionAttempts::new(),
        }
    }

    /// Start a direct connection to a peer device from a specified client.
    pub fn start_direct_connection(
        &mut self,
        client: ConnectionManagerClient,
        address: AddressWithType,
    ) -> Result<(), CreateConnectionFailure> {
        // TODO(aryarahul): report result to upper layer
        spawn_local(timeout(
            DIRECT_CONNECTION_TIMEOUT,
            self.active_connection_attempts.direct_connection(client, address)?,
        ));
        Ok(())
    }

    /// Cancel direct connection attempts from this client to the specified address.
    pub fn cancel_direct_connection(
        &mut self,
        client: ConnectionManagerClient,
        address: AddressWithType,
    ) -> Result<(), CancelConnectFailure> {
        self.active_connection_attempts.cancel_direct_connection(client, address)?;
        self.update_state();
        Ok(())
    }

    /// Start a background connection to a peer device with given parameters from a specified client.
    pub fn add_background_connection(
        &mut self,
        client: ConnectionManagerClient,
        address: AddressWithType,
    ) -> Result<(), CreateConnectionFailure> {
        self.active_connection_attempts.add_background_connection(client, address)?;
        self.update_state();
        Ok(())
    }

    /// Cancel background connection attempts from this client to the specified address.
    pub fn remove_background_connection(
        &mut self,
        client: ConnectionManagerClient,
        address: AddressWithType,
    ) -> Result<(), CancelConnectFailure> {
        self.active_connection_attempts.remove_background_connection(client, address)?;
        self.update_state();
        Ok(())
    }

    /// Pause all initiation (from address manager)
    pub fn pause(&mut self) {
        self.initiator.pause();
    }

    /// Resume initiation (from address manager)
    pub fn resume(&mut self) {
        self.initiator.resume();
    }

    /// Handle an incoming HCI event
    pub fn on_hci_event(&mut self, event: HciEvent) {
        let conn = self.initiator.on_hci_event(event);
        let Some(conn) = conn else { return };

        self.current_connections.insert(conn.remote_address);

        if self.active_connection_attempts.process_connection(conn).is_err()
            && conn.role == Role::Central
        {
            // no one was listening for this connection, yet we were the central
            self.hci.disconnect(conn);
        }

        self.update_state();
    }

    fn update_state(&mut self) {
        let (target_state, target_accept_list) = get_target_parameters(
            self.active_connection_attempts.active_attempts(),
            &self.current_connections,
        );

        if target_state == InitiatorStableState::Stopped {
            // no need to update the acceptlist, just make sure we stay stopped
            self.initiator.request_state(target_state);
            return;
        }

        // update acceptlist first
        // remove any stale entries
        for entry in self.current_accept_list.difference(&target_accept_list) {
            self.hci.remove_from_accept_list(*entry);
        }
        // add new ones
        for entry in target_accept_list.difference(&self.current_accept_list) {
            self.hci.add_to_accept_list(*entry);
        }
        // now the accept list should match what we want
        self.current_accept_list = target_accept_list;

        // finally, update the initiator state (so we have time to pause, if we need to adjust the accept list)
        self.initiator.request_state(target_state);
    }
}
