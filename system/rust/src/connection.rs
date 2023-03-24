//! This module manages LE connection requests and active
//! LE connections. In particular, it de-duplicates connection requests,
//! avoids duplicate connections to the same devices (even with different RPAs),
//! and retries failed connections

use std::{rc::Rc, time::Duration};

use crate::{
    core::shared_box::{SharedBox, WeakBox},
    gatt::ids::ServerId,
};

use self::le_manager::{
    AddressWithType, ErrorCode, InactiveLeAclManager, LeAclManager, LeAclManagerConnectionCallbacks,
};

mod le_manager;
pub mod mocks;

pub enum Role {
    Central,
    Peripheral,
}

pub struct Connection {
    pub local_address: AddressWithType,
    pub remote_address: AddressWithType,
    pub role: Role,
}

pub enum ConnectionFailure {
    Timeout,
    ConnectionCancelled,
}

pub enum CancelConnectFailure {
    ConnectionNotPending,
}

pub enum ConnectionManagerClient {
    GattClient(u8),
    GattServer(ServerId),
    L2capCoc,
    Security,
}

pub enum ConnectionMode {
    Background,
    Direct,
    TargetedAnnouncement,
}

pub struct ConnectionParameters {
    pub mode: ConnectionMode,
    pub timeout: Duration,
    pub retry_on_failure: bool,
}

#[derive(Debug)]
pub struct ConnectionManager {
    _le_acl_manager: Box<dyn LeAclManager>,
}

struct ConnectionManagerCallbackHandler(Option<WeakBox<ConnectionManager>>);

impl LeAclManagerConnectionCallbacks for ConnectionManagerCallbackHandler {
    fn on_le_connect_success(&self, _address: AddressWithType) {
        todo!()
    }

    fn on_le_connect_fail(&self, _address: AddressWithType, _status: ErrorCode) {
        todo!()
    }

    fn on_disconnect(&self, _address: AddressWithType) {
        todo!()
    }
}

impl ConnectionManager {
    /// Constructor
    pub fn new(manager: impl InactiveLeAclManager) -> SharedBox<Self> {
        SharedBox::from_rc(Rc::new_cyclic(|weak| Self {
            _le_acl_manager: Box::new(manager.register_callbacks(ConnectionManagerCallbackHandler(
                Some(WeakBox::from_weak(weak.clone())),
            ))),
        }))
        .expect("Rc<> did not have exactly 1 strong reference")
    }

    /// Connect to a peer device with given parameters from a specified client.
    /// The future will resolve once the connection is made or if it fails.
    ///
    /// # Cancellation Safety
    /// The returned future can be safely dropped, and the connection attempt
    /// will be cancelled (as if ConnectionManager#cancel_connect were invoked).
    pub async fn connect(
        &self,
        _client: ConnectionManagerClient,
        _address: AddressWithType,
        _params: ConnectionParameters,
    ) -> Result<Connection, ConnectionFailure> {
        todo!()
    }

    /// Cancel all connection attempts from this client to the specified address.
    /// This will cause any pending futures to resolve with ConnectionFailure::ConnectionCancelled.
    /// 
    /// To cancel just a single connection attempt, drop the future associated with the connection
    /// attempt.
    pub fn cancel_connect(
        &self,
        _client: ConnectionManagerClient,
        _address: AddressWithType,
    ) -> Result<(), CancelConnectFailure> {
        todo!()
    }
}
