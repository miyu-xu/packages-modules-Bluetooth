//! This module logs RFCOMM connection metrics
//! 
//! Atoms:
//!  - BluetoothRfcommConnectionAttemptComplete
//!  - BluetoothRfcommDisconnection

use std::{collections::{VecDeque, HashMap, hash_map::Entry}, time::{Instant, Duration}, rc::Rc};

use crate::core::address::RawAddress;

use super::atoms::{AppUid, AtomLogger, RfcommConnectionAttemptCompleteStatus, RfcommConnectionMetadata, RfcommConnectionMetadataSecurityRequirement, RfcommConnectionMetadataTarget};
use super::device::Devices;

/// A failed connect() call to a peer device
struct RfcommFailedConnectionAttempt {
  /// The time when the attempt failed (NOT when it started)
  completed_time: Instant,
}

/// This tracks the state of past RFCOMM attempts, as well as all active connections.
pub struct RfcommTracker {
  /// Shim on tops of statslog for testability
  logger: Rc<dyn AtomLogger>,
  /// Access device metadata for metrics purposes
  devices: Rc<Devices>,
  /// All recent failed attempts to a given peer. When an attempt succeeds, the history is
  /// cleared for that device. Attempts older than MAX_ATTEMPT_AGE are also removed.
  /// The VecDeque is sorted from oldest to newest.
  /// 
  /// Note: this can lead to unbounded memory usage if a client calls connect() in a tight loop.
  /// But many other things will go wrong first before the memory usage here becomes an issue.
  failed_attempts_by_address: HashMap<RawAddress, VecDeque<RfcommFailedConnectionAttempt>>
}

impl RfcommTracker {
  const MAX_ATTEMPT_AGE: Duration = Duration::from_secs(5 * 60);

  pub fn new(logger: Rc<dyn AtomLogger>, devices: Rc<Devices>) -> Self {
    Self {
      logger,
      devices,
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

  /// Record when an RFCOMM connect() call completes, whether successful or failed.
  pub fn log_rfcomm_client_connection(
    &mut self,
    peer_addr: RawAddress,
    is_secured: bool,
    success: bool,
    socket_connection_latency: Duration,
    app_uid: AppUid,
  ) {
    if success {
      self.clear_old_failed_attempts(peer_addr);
    } else {
      let attempt = RfcommFailedConnectionAttempt {
        completed_time: Instant::now(),
      };
      self.failed_attempts_by_address.entry(peer_addr).or_default().push_back(attempt);
      self.clear_old_failed_attempts(peer_addr);
    }

    let retries_before_current = self.failed_attempts_by_address.get(&peer_addr).map(|attempts| attempts.len()).unwrap_or(0);

    self.logger.log_rfcomm_client_connection(if success {
      RfcommConnectionAttemptCompleteStatus::Success
    } else {
      RfcommConnectionAttemptCompleteStatus::UnknownFailed
    }, RfcommConnectionMetadata {
        local_device: self.devices.get_local_device(),
        peer_device: self.devices.get_peer_device(peer_addr),
        security_requirement: if is_secured {
          RfcommConnectionMetadataSecurityRequirement::Secure
        } else {
          RfcommConnectionMetadataSecurityRequirement::None
        },
        target: RfcommConnectionMetadataTarget::Uuid("DO NOT SUBMIT".into()),
        caller_uid: app_uid,
    }, socket_connection_latency.as_millis() as i32, retries_before_current as i32);
  }
}
