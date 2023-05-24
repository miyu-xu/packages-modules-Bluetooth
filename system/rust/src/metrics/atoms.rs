//! This module wraps the underlying atoms to be logged, so we can intercept log attempts for the
//! purpose of unit tests.
//! TODO(aryarahul): generate from proto file directly using Cargo, as well as Soong

use crate::core::address::RawAddress;

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct AppUid(pub u32);

pub enum RfcommConnectionAttemptCompleteStatus {
    Success,
    UnknownFailed,
}

pub enum RfcommConnectionMetadataSecurityRequirement {
    None,
    Secure,
}

pub struct LocalDeviceMetadata {
    pub addr: RawAddress,
}

pub struct PeerDeviceMetadata {
    pub addr: RawAddress,
}

pub enum RfcommConnectionMetadataTarget {
    Uuid(String),
    Port(i32),
}

pub struct RfcommConnectionMetadata {
    pub local_device: LocalDeviceMetadata,
    pub peer_device: PeerDeviceMetadata,
    pub security_requirement: RfcommConnectionMetadataSecurityRequirement,
    pub target: RfcommConnectionMetadataTarget,
    pub caller_uid: AppUid,
}

pub enum RfcommDisconnectionReason {
    LocalInitiated,
    RemoteInitiatedUnknown,
}

pub trait AtomLogger {
    fn log_rfcomm_client_connection(
        &self,
        status: RfcommConnectionAttemptCompleteStatus,
        metadata: RfcommConnectionMetadata,
        latency_millis: i32,
        retries_before_current: i32,
    );

    fn log_rfcomm_client_disconnection(
        &self,
        reason: RfcommDisconnectionReason,
        metadata: RfcommConnectionMetadata,
        duration_millis: i32,
    );
}

pub struct AndroidAtomLogger;

impl AndroidAtomLogger {
    /// Constructor. Panics on non-Android targets.
    #[allow(clippy::new_ret_no_self)]
    pub fn new() -> Box<dyn AtomLogger> {
        #[cfg(not(target_os = "android"))]
        {
            unreachable!("AndroidAtomLogger should only be used on Android");
        }

        #[cfg(target_os = "android")]
        Box::new(Self)
    }
}

#[cfg(target_os = "android")]
impl AtomLogger for AndroidAtomLogger {
    fn log_rfcomm_client_connection(
        &self,
        status: RfcommConnectionAttemptCompleteStatus,
        _metadata: RfcommConnectionMetadata,
        latency_millis: i32,
        retries_before_current: i32,
    ) {
        let _ = statslog_rust::bluetooth_rfcomm_connection_attempt_complete::BluetoothRfcommConnectionAttemptComplete {
        latency_millis,
        metadata: &[],
        retries_before_current,
        status: match status {
           RfcommConnectionAttemptCompleteStatus::Success => statslog_rust::bluetooth_rfcomm_connection_attempt_complete::Status::Success,
           RfcommConnectionAttemptCompleteStatus::UnknownFailed => statslog_rust::bluetooth_rfcomm_connection_attempt_complete::Status::UnknownFailed,
        }
      }.stats_write();
    }

    fn log_rfcomm_client_disconnection(
        &self,
        _reason: RfcommDisconnectionReason,
        _metadata: RfcommConnectionMetadata,
        _duration_millis: i32,
    ) {
        todo!()
    }
}
