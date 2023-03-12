//! This module manages LE connection requests and active
//! LE connections. In particular, it de-duplicates connection requests,
//! avoids duplicate connections to the same devices (even with different RPAs),
//! and retries failed connections

use std::{rc::Rc, time::Duration};

use crate::gatt::ids::ServerId;

use self::le_manager::{AddressWithType, LeAclManager};

mod le_manager;

pub enum ConnectionFailure {
    ConnectionAttemptAlreadyExists,
    AlreadyExistsUnderDifferentAddress(AddressWithType),
    Timeout,
    ConnectionCancelled,
}

pub enum CancelConnectFailure {
    ConnectionNotPending,
}

pub enum ConnectionManagerClient {
    GattClientDirectlyManaged(u8),
    GattClientViaTargetedAnnouncement(u8),
    GattServer(ServerId),
    L2capCoc,
    Security,
}

pub enum ConnectionMode {
    Background,
    Direct,
}

pub struct ConnectionParameters {
    mode: ConnectionMode,
    timeout: Duration,
    retry_on_failure: bool,
}

pub struct ConnectionManager {
    le_acl_manager: Box<dyn LeAclManager>,
}

// INVARIANT: each client will connect to each address at most once
impl ConnectionManager {
    pub async fn connect(
        &self,
        client: ConnectionManagerClient,
        address: AddressWithType,
        params: ConnectionParameters,
    ) -> Result<(), ConnectionFailure> {
        todo!()
    }

    pub fn cancel_connect(
        &self,
        client: ConnectionManagerClient,
        address: AddressWithType,
    ) -> Result<(), CancelConnectFailure> {
        todo!()
    }
}
