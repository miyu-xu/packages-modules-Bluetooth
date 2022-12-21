//! Various libraries to access the profile interfaces.

/// Generic type for keeping track of profile connections.
#[derive(Clone, Debug, FromPrimitive, PartialEq, ToPrimitive)]
#[repr(u32)]
pub enum ProfileConnectionState {
    /// The profile is completely disconnected.
    Disconnected = 0,

    /// The profile is in the process of disconnecting everything.
    Disconnecting = 1,

    /// The profile is in the process of connecting at least 1 device.
    Connecting = 2,

    /// The profile has at least 1 device connected but not active.
    Connected = 3,

    /// The profile has at least 1 device connected and active. For some apis which don't
    /// distinguish between |Connected| and |Active|, the state should always be |Active|.
    Active = 4,

    /// Invalid connection state which can be used to identify error states.
    Invalid = u32::MAX,
}

pub mod a2dp;
pub mod avrcp;
pub mod gatt;
pub mod hf_client;
pub mod hfp;
pub mod hid_host;
pub mod sdp;
pub mod socket;
