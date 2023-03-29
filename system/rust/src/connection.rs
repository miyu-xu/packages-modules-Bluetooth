//! This module manages LE connection requests and active
//! LE connections. In particular, it de-duplicates connection requests,
//! avoids duplicate connections to the same devices (even with different RPAs),
//! and retries failed connections

use std::{cell::RefCell, collections::HashSet, fmt::Debug, hash::Hash, ops::Deref, rc::Rc};

use crate::{
    core::{
        address::AddressWithType,
        shared_box::{SharedBox, WeakBox},
    },
    gatt::ids::ServerId,
};

use self::le_manager::{
    ErrorCode, InactiveLeAclManager, LeAclManager, LeAclManagerConnectionCallbacks,
};

mod ffi;
pub mod le_manager;

pub use ffi::{register_callbacks, LeAclManagerImpl, LeAclManagerShim};

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
    _le_manager: Box<dyn LeAclManager>,
    state: RefCell<ConnectionManagerState>,
}

#[derive(Debug)]
struct ConnectionManagerState {
    /// The addresses currently connected to
    current_connections: HashSet<AddressWithType>,
}

struct ConnectionManagerCallbackHandler(Option<WeakBox<ConnectionManager>>);

impl LeAclManagerConnectionCallbacks for ConnectionManagerCallbackHandler {
    fn on_le_connect_success(&self, address: AddressWithType) {
        self.with_manager(|manager| manager.on_le_connect_success(address))
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
                _le_manager: Box::new(le_manager),
                state: RefCell::new(ConnectionManagerState { current_connections: HashSet::new() }),
            }
        }))
        .expect("Rc<> did not have exactly 1 strong reference")
    }

    /// Start a direct connection to a peer device from a specified client.
    pub fn start_direct_connection(
        &self,
        _client: ConnectionManagerClient,
        _address: AddressWithType,
    ) -> Result<(), CreateConnectionFailure> {
        todo!()
    }

    /// Cancel direct connection attempts from this client to the specified address.
    pub fn cancel_direct_connection(
        &self,
        _client: ConnectionManagerClient,
        _address: AddressWithType,
    ) -> Result<(), CancelConnectFailure> {
        todo!()
    }

    /// Start a background connection to a peer device with given parameters from a specified client.
    pub fn add_background_connection(
        &self,
        _client: ConnectionManagerClient,
        _address: AddressWithType,
    ) -> Result<(), CreateConnectionFailure> {
        todo!()
    }

    /// Cancel background connection attempts from this client to the specified address.
    pub fn remove_background_connection(
        &self,
        _client: ConnectionManagerClient,
        _address: AddressWithType,
    ) -> Result<(), CancelConnectFailure> {
        todo!()
    }

    /// Handle an incoming HCI event
    fn on_le_connect_success(&self, address: AddressWithType) {
        self.state.borrow_mut().current_connections.insert(address);
        todo!()
    }

    fn on_le_connect_fail(&self, _address: AddressWithType, _status: ErrorCode) {
        todo!()
    }

    fn on_disconnect(&self, address: AddressWithType) {
        self.state.borrow_mut().current_connections.remove(&address);
        todo!()
    }
}
