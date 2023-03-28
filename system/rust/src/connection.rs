//! This module manages LE connection requests and active
//! LE connections. In particular, it de-duplicates connection requests,
//! avoids duplicate connections to the same devices (even with different RPAs),
//! and retries failed connections

use std::{collections::HashSet, fmt::Debug, hash::Hash, rc::Rc, time::Duration};

use tokio::{task::spawn_local, time::timeout};

use crate::{core::address::AddressWithType, gatt::ids::ServerId};

use self::{
    accept_list_manager::get_target_parameters,
    connection_attempt_manager::ConnectionAttempts,
    hci_operations::{HciConnectProxy, HciEvent},
    state_machine::HciConnectionStateMachine,
};

mod accept_list_manager;
mod connection_attempt_manager;
mod ffi;
pub mod hci_operations;
mod state_machine;

pub use ffi::{
    register_callbacks, AddressManagerImpl, HciConnectProxyImpl, LeAddressManagerShim,
    LeConnectHciManagerShim,
};

/// Possible failures returned when making a connection attempt
#[derive(Debug)]
pub enum CreateConnectionFailure {
    /// This client is already making a connection of the same type
    /// to the same address.
    ConnectionAlreadyPending,
}

/// Failures returned if a connection successfully starts but fails afterwards.
#[derive(Debug)]
pub enum ConnectionFailure {
    /// The connection attempt was cancelled
    Cancelled,
}

#[derive(Debug)]
pub enum CancelConnectFailure {
    /// The connection attempt does not exist
    ConnectionNotPending,
}

#[derive(Debug, Hash, PartialEq, Eq, Clone, Copy)]
pub enum ConnectionManagerClient {
    GattClient(u8),
    GattServer(ServerId),
}

pub trait AddressManager: Debug {
    fn ack_pause(&self);
    fn ack_resume(&self);
}

#[derive(Copy, Clone, Eq, PartialEq, Debug)]
pub enum Role {
    Central,
    Peripheral,
}

#[derive(Copy, Clone, Debug)]
pub struct Connection {
    pub remote_address: AddressWithType,
    pub role: Role,
}

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

    pub fn pause(&mut self) {
        self.initiator.pause();
    }

    pub fn resume(&mut self) {
        self.initiator.resume();
    }

    /// Handle an incoming HCI event
    pub fn on_hci_event(&mut self, event: HciEvent) {
        let conn = self.initiator.on_hci_event(event);
        let Some(conn) = conn else { return };

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
