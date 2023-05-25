//! This module logs RFCOMM connection metrics
//!
//! Atoms:
//!  - BluetoothRfcommConnectionAttemptComplete
//!  - BluetoothRfcommDisconnection

use std::{
    collections::{hash_map::Entry, HashMap, VecDeque},
    rc::Rc,
    time::{Duration, Instant},
};

use crate::core::address::RawAddress;

use super::atoms::{
    AppUid, AtomLogger, RfcommConnectionAttemptCompleteStatus, RfcommConnectionMetadata,
    RfcommConnectionMetadataSecurityRequirement, RfcommConnectionMetadataTarget,
};
use super::device::Devices;

use log::{error, info};

#[derive(Clone, Copy, PartialEq, Eq, Hash, Debug)]
pub struct AttemptId(pub u32);

/// A failed connect() call to a peer device
struct RfcommFailedConnectionAttempt {
    /// The time when the attempt failed (NOT when it started)
    completed_time: Instant,
}

#[derive(Debug, Clone)]
struct RfcommConnectionAttempt {
    peer_addr: RawAddress,
    is_secured: bool,
    app_uid: AppUid,
    start_time: Instant,
    target: RfcommConnectionMetadataTarget,
}

/// This tracks the state of past RFCOMM attempts, as well as all active connections.
pub struct RfcommTracker {
    /// Shim on tops of statslog for testability
    logger: Rc<dyn AtomLogger>,
    /// Access device metadata for metrics purposes
    devices: Rc<Devices>,
    /// All active connection attempts TODO(aryarahul): remove stale ones
    sockets: HashMap<AttemptId, RfcommConnectionAttempt>,
    /// All recent failed attempts to a given peer. When an attempt succeeds, the history is
    /// cleared for that device. Attempts older than MAX_ATTEMPT_AGE are also removed.
    /// The VecDeque is sorted from oldest to newest.
    ///
    /// Note: this can lead to unbounded memory usage if a client calls connect() in a tight loop.
    /// But many other things will go wrong first before the memory usage here becomes an issue.
    failed_attempts_by_address: HashMap<RawAddress, VecDeque<RfcommFailedConnectionAttempt>>,
}

impl RfcommTracker {
    const MAX_ATTEMPT_AGE: Duration = Duration::from_secs(5 * 60);

    pub fn new(logger: Rc<dyn AtomLogger>, devices: Rc<Devices>) -> Self {
        Self {
            logger,
            devices,
            sockets: Default::default(),
            failed_attempts_by_address: Default::default(),
        }
    }

    fn clear_old_failed_attempts(&mut self, peer_addr: RawAddress) {
        let Entry::Occupied(mut entry) = self.failed_attempts_by_address.entry(peer_addr) else { return };
        let attempts = entry.get_mut();
        while let Some(oldest) = attempts.front() {
            if Instant::now() - oldest.completed_time > Self::MAX_ATTEMPT_AGE {
                attempts.pop_front();
            }
        }
    }

    /// Record when an RFCOMM connect() call is made
    pub fn log_rfcomm_client_connection_attempt_start(
        &mut self,
        attempt_id: AttemptId,
        peer_addr: RawAddress,
        is_secured: bool,
        target_uuid: Option<String>,
        target_port: i32,
        app_uid: AppUid,
    ) {
        let target = if let Some(target_uuid) = target_uuid {
            if target_port <= 0 {
                RfcommConnectionMetadataTarget::Uuid(target_uuid)
            } else {
                RfcommConnectionMetadataTarget::Port(target_port)
            }
        } else {
            RfcommConnectionMetadataTarget::Port(target_port)
        };

        info!("logging RFCOMM connection attempt start {attempt_id:?} from {app_uid:?} to {target:?}");
        self.sockets.insert(
            attempt_id,
            RfcommConnectionAttempt { peer_addr, is_secured, app_uid, start_time: Instant::now(), target },
        );
    }

    /// Record when an RFCOMM connect() call completes, whether successful or failed.
    pub fn log_rfcomm_client_connection_complete(&mut self, attempt_id: AttemptId, success: bool) {
        let Some(attempt) = self.sockets.get(&attempt_id).cloned() else {
            error!("Got RFCOMM connection complete for unknown attempt ID {attempt_id:?}");
            return;
        };

        if !success {
            let failed_attempt = RfcommFailedConnectionAttempt { completed_time: Instant::now() };
            self.failed_attempts_by_address
                .entry(attempt.peer_addr)
                .or_default()
                .push_back(failed_attempt);
        }

        self.clear_old_failed_attempts(attempt.peer_addr);
        let retries_before_current = self
            .failed_attempts_by_address
            .get(&attempt.peer_addr)
            .map(|attempts| attempts.len())
            .unwrap_or(0);

        let socket_connection_latency = Instant::now() - attempt.start_time;

        info!("logging RFCOMM connection attempt complete {attempt_id:?}, success={success}, latency={socket_connection_latency:?}");

        self.logger.log_rfcomm_client_connection(
            if success {
                RfcommConnectionAttemptCompleteStatus::Success
            } else {
                RfcommConnectionAttemptCompleteStatus::UnknownFailed
            },
            RfcommConnectionMetadata {
                local_device: self.devices.get_local_device(),
                peer_device: self.devices.get_peer_device(attempt.peer_addr),
                security_requirement: if attempt.is_secured {
                    RfcommConnectionMetadataSecurityRequirement::Secure
                } else {
                    RfcommConnectionMetadataSecurityRequirement::None
                },
                target: attempt.target,
                caller_uid: attempt.app_uid,
            },
            socket_connection_latency.as_millis() as i32,
            retries_before_current as i32,
        );
    }
}
