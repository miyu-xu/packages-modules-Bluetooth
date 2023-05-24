use crate::btif::{BluetoothInterface, BtStatus, RawAddress, ToggleableProfile};
use crate::topstack::get_dispatchers;

use bitflags::bitflags;
use num_derive::{FromPrimitive, ToPrimitive};
use num_traits::cast::FromPrimitive;
use std::convert::{TryFrom, TryInto};
use std::sync::{Arc, Mutex};
use topshim_macros::{cb_variant, profile_enabled_or};

use log::warn;

#[cxx::bridge(namespace = bluetooth::topshim::rust)]
pub mod ffi {
    unsafe extern "C++" {
        include!("gd/rust/topshim/common/type_alias.h");
        type RawAddress = crate::btif::RawAddress;
    }

    unsafe extern "C++" {
        include!("le_audio/le_audio_shim.h");

        type LeAudioIntf;

        unsafe fn GetLeAudioProfile(btif: *const u8) -> UniquePtr<LeAudioIntf>;

        fn init(self: Pin<&mut LeAudioIntf>) -> i32;
        fn connect(self: Pin<&mut HfpIntf>, bt_addr: RawAddress) -> u32;
        fn connect_audio(
            self: Pin<&mut HfpIntf>,
            bt_addr: RawAddress,
            sco_offload: bool,
            force_cvsd: bool,
        ) -> i32;
        fn set_active_device(self: Pin<&mut HfpIntf>, bt_addr: RawAddress) -> i32;
        fn set_volume(self: Pin<&mut HfpIntf>, volume: i8, bt_addr: RawAddress) -> i32;
        fn disconnect(self: Pin<&mut HfpIntf>, bt_addr: RawAddress) -> u32;
        fn disconnect_audio(self: Pin<&mut HfpIntf>, bt_addr: RawAddress) -> i32;
        fn cleanup(self: Pin<&mut HfpIntf>);

    }
    extern "Rust" {
        fn hfp_connection_state_callback(state: u32, addr: RawAddress);
        fn hfp_audio_state_callback(state: u32, addr: RawAddress);
    }
}
