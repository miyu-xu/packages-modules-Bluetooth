//! This module manages LE connection requests and active
//! LE connections. In particular, it de-duplicates connection requests,
//! avoids duplicate connections to the same devices (even with different RPAs),
//! and retries failed connections

use std::{
    cell::RefCell, collections::HashSet, fmt::Debug, hash::Hash, ops::Deref, rc::Rc, time::Duration,
};

use crate::{
    core::{
        address::AddressWithType,
        shared_box::{SharedBox, WeakBox},
    },
    gatt::ids::ServerId,
};

use self::{
    attempt_manager::{ConnectionAttempts, ConnectionMode},
    le_manager::{ErrorCode, InactiveLeAclManager, LeAclManager, LeAclManagerConnectionCallbacks},
};

mod attempt_manager;
mod ffi;
pub mod le_manager;

pub use ffi::{register_callbacks, LeAclManagerImpl, LeAclManagerShim};
use tokio::{task::spawn_local, time::timeout};

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

/// An active connection
#[derive(Copy, Clone, Debug)]
pub struct Connection {
    /// The address of the peer device, as reported in the connection complete event
    pub remote_address: AddressWithType,
}

/// Responsible for managing the initiator state and the list of
/// devices on the filter accept list
#[derive(Debug)]
pub struct ConnectionManager {
    le_manager: Box<dyn LeAclManager>,
    state: RefCell<ConnectionManagerState>,
}

#[derive(Debug)]
struct ConnectionManagerState {
    /// All pending connection attempts (unresolved direct + all background)
    attempts: ConnectionAttempts,
    /// The addresses we are currently connected to
    current_connections: HashSet<AddressWithType>,
    /// The connect list in the ACL manager
    direct_list: HashSet<AddressWithType>,
    /// The background connect list in the ACL manager
    background_list: HashSet<AddressWithType>,
}

struct ConnectionManagerCallbackHandler(Option<WeakBox<ConnectionManager>>);

const DIRECT_CONNECTION_TIMEOUT: Duration = Duration::from_secs(
    29, /* ugly hack to avoid fighting with le_impl timeout, until I remove that timeout */
);

impl LeAclManagerConnectionCallbacks for ConnectionManagerCallbackHandler {
    fn on_le_connect_success(&self, conn: Connection) {
        self.with_manager(|manager| manager.on_le_connect_success(conn))
    }

    fn on_le_connect_fail(&self, address: AddressWithType, status: ErrorCode) {
        self.with_manager(|manager| manager.on_le_connect_fail(address, status))
    }

    fn on_disconnect(&self, address: AddressWithType) {
        self.with_manager(|manager| manager.on_disconnect(address))
    }
}

impl ConnectionManagerCallbackHandler {
    fn with_manager(&self, f: impl FnOnce(&ConnectionManager)) {
        self.0
            .as_ref()
            .expect("got connection event before stack initialized")
            .with(|manager| f(manager.expect("got connection event after stack died").deref()))
    }
}

impl ConnectionManager {
    /// Constructor
    pub fn new(le_manager: impl InactiveLeAclManager) -> SharedBox<Self> {
        SharedBox::from_rc(Rc::new_cyclic(|weak| {
            let le_manager = le_manager.register_callbacks(ConnectionManagerCallbackHandler(Some(
                WeakBox::from_weak(weak.clone()),
            )));
            Self {
                le_manager: Box::new(le_manager),
                state: RefCell::new(ConnectionManagerState {
                    attempts: ConnectionAttempts::new(),
                    current_connections: HashSet::new(),
                    direct_list: HashSet::new(),
                    background_list: HashSet::new(),
                }),
            }
        }))
        .expect("Rc<> did not have exactly 1 strong reference")
    }

    /// Start a direct connection to a peer device from a specified client.
    pub fn start_direct_connection(
        &self,
        client: ConnectionManagerClient,
        address: AddressWithType,
    ) -> Result<(), CreateConnectionFailure> {
        let mut state = self.state.borrow_mut();
        // if connected, this is a no-op
        if state.current_connections.contains(&address) {
            return Ok(());
        }
        // TODO(aryarahul): handle timeout callback
        spawn_local(timeout(
            DIRECT_CONNECTION_TIMEOUT,
            state.attempts.direct_connection(client, address)?,
        ));
        self.reconcile_state(&mut state);
        Ok(())
    }

    /// Cancel direct connection attempts from this client to the specified address.
    pub fn cancel_direct_connection(
        &self,
        client: ConnectionManagerClient,
        address: AddressWithType,
    ) -> Result<(), CancelConnectFailure> {
        let mut state = self.state.borrow_mut();
        state.attempts.cancel_direct_connection(client, address)?;
        self.reconcile_state(&mut state);
        Ok(())
    }

    /// Start a background connection to a peer device with given parameters from a specified client.
    pub fn add_background_connection(
        &self,
        client: ConnectionManagerClient,
        address: AddressWithType,
    ) -> Result<(), CreateConnectionFailure> {
        let mut state = self.state.borrow_mut();
        state.attempts.add_background_connection(client, address)?;
        self.reconcile_state(&mut state);
        Ok(())
    }

    /// Cancel background connection attempts from this client to the specified address.
    pub fn remove_background_connection(
        &self,
        client: ConnectionManagerClient,
        address: AddressWithType,
    ) -> Result<(), CancelConnectFailure> {
        let mut state = self.state.borrow_mut();
        state.attempts.remove_background_connection(client, address)?;
        self.reconcile_state(&mut state);
        Ok(())
    }

    /// Cancel all connection attempts to this address
    pub fn cancel_unconditionally(&self, address: AddressWithType) {
        let mut state = self.state.borrow_mut();
        state.attempts.remove_unconditionally(address);
        self.reconcile_state(&mut state);
    }

    /// Cancel all connection attempts from this client
    pub fn remove_client(&self, client: ConnectionManagerClient) {
        let mut state = self.state.borrow_mut();
        state.attempts.remove_client(client);
        self.reconcile_state(&mut state);
    }

    fn on_le_connect_success(&self, conn: Connection) {
        let mut state = self.state.borrow_mut();
        // record this connection while it exists
        state.current_connections.insert(conn.remote_address);
        // successful connections remove the address from the direct list
        state.direct_list.remove(&conn.remote_address);
        // invoke any pending callbacks, update set of attempts
        state.attempts.process_connection(conn);
        // update the acceptlist if needed
        self.reconcile_state(&mut state);
    }

    fn on_le_connect_fail(&self, address: AddressWithType, _status: ErrorCode) {
        let mut state = self.state.borrow_mut();
        // this should only occur in the case of an le_impl timeout
        // after timeouts are removed, consider putting an unreachable!() here?
        // ask @rwt first
        if address == AddressWithType::EMPTY {
            return;
        }
        // le_impl appears to pull the device out of the direct connect list (but not the background list...) on error
        state.direct_list.remove(&address);

        self.reconcile_state(&mut state);
    }

    fn on_disconnect(&self, address: AddressWithType) {
        let mut state = self.state.borrow_mut();
        state.current_connections.remove(&address);
        self.reconcile_state(&mut state);
    }

    fn reconcile_state(&self, state: &mut ConnectionManagerState) {
        // first figure out what state we need the ACL manager to be in
        let needed_direct_connections = state
            .attempts
            .active_attempts()
            .filter(|attempt| attempt.mode == ConnectionMode::Direct)
            .map(|attempt| attempt.remote_address)
            .collect::<HashSet<_>>();

        let needed_background_connections = state
            .attempts
            .active_attempts()
            .filter(|attempt| attempt.mode == ConnectionMode::Background)
            .map(|attempt| attempt.remote_address)
            .collect::<HashSet<_>>();

        // next, pull out anything in the ACL manager that we don't need
        // recall that cancel_connect() removes addresses from *both* lists (!)
        for address in state.direct_list.difference(&needed_direct_connections) {
            self.le_manager.remove_from_all_lists(*address);
            state.background_list.remove(address);
        }
        state.direct_list =
            state.direct_list.intersection(&needed_direct_connections).copied().collect();

        for address in state.background_list.difference(&needed_background_connections) {
            self.le_manager.remove_from_all_lists(*address);
            state.direct_list.remove(address);
        }
        state.background_list =
            state.background_list.intersection(&needed_background_connections).copied().collect();

        // now everything extra has been removed, we can put things back in
        for address in needed_direct_connections.difference(&state.direct_list) {
            self.le_manager.add_to_direct_list(*address);
        }
        for address in needed_background_connections.difference(&state.background_list) {
            self.le_manager.add_to_background_list(*address);
        }

        // we should now be in a consistent state!
        state.direct_list = needed_direct_connections;
        state.background_list = needed_background_connections;
    }
}
