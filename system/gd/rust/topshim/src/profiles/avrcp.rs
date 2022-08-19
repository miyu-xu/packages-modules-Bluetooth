use crate::btif::{BluetoothInterface, RawAddress};
use crate::topstack::get_dispatchers;

use std::sync::{Arc, Mutex};
use topshim_macros::cb_variant;

#[cxx::bridge(namespace = bluetooth::topshim::rust)]
pub mod ffi {
    #[derive(Debug, Copy, Clone)]
    pub struct RustRawAddress {
        address: [u8; 6],
    }

    unsafe extern "C++" {
        include!("btav/btav_shim.h");

        type AvrcpIntf;

        unsafe fn GetAvrcpProfile(btif: *const u8) -> UniquePtr<AvrcpIntf>;

        fn init(self: Pin<&mut AvrcpIntf>);
        fn cleanup(self: Pin<&mut AvrcpIntf>);
        fn set_volume(self: Pin<&mut AvrcpIntf>, volume: i8);

    }
    extern "Rust" {
        fn avrcp_absolute_volume_enabled(enabled: bool, addr: RustRawAddress);
        fn avrcp_absolute_volume_update(volume: u8);
    }
}

impl From<RawAddress> for ffi::RustRawAddress {
    fn from(addr: RawAddress) -> Self {
        ffi::RustRawAddress { address: addr.val }
    }
}

impl Into<RawAddress> for ffi::RustRawAddress {
    fn into(self) -> RawAddress {
        RawAddress { val: self.address }
    }
}

#[derive(Debug)]
pub enum AvrcpCallbacks {
    AvrcpAbsoluteVolumeEnabled(bool, RawAddress),
    AvrcpAbsoluteVolumeUpdate(u8),
}

pub struct AvrcpCallbacksDispatcher {
    pub dispatch: Box<dyn Fn(AvrcpCallbacks) + Send>,
}

type AvrcpCb = Arc<Mutex<AvrcpCallbacksDispatcher>>;

cb_variant!(
    AvrcpCb,
    avrcp_absolute_volume_enabled -> AvrcpCallbacks::AvrcpAbsoluteVolumeEnabled,
    bool, ffi::RustRawAddress -> RawAddress, {
        let _1 = _1.into();
    }
);

cb_variant!(
    AvrcpCb,
    avrcp_absolute_volume_update -> AvrcpCallbacks::AvrcpAbsoluteVolumeUpdate,
    u8, {}
);

pub struct Avrcp {
    internal: cxx::UniquePtr<ffi::AvrcpIntf>,
    _is_init: bool,
}

// For *const u8 opaque btif
unsafe impl Send for Avrcp {}

impl Avrcp {
    pub fn new(intf: &BluetoothInterface) -> Avrcp {
        let avrcpif: cxx::UniquePtr<ffi::AvrcpIntf>;
        unsafe {
            avrcpif = ffi::GetAvrcpProfile(intf.as_raw_ptr());
        }

        Avrcp { internal: avrcpif, _is_init: false }
    }

    pub fn initialize(&mut self, callbacks: AvrcpCallbacksDispatcher) -> bool {
        if get_dispatchers().lock().unwrap().set::<AvrcpCb>(Arc::new(Mutex::new(callbacks))) {
            panic!("Tried to set dispatcher for Avrcp callbacks while it already exists");
        }
        self.internal.pin_mut().init();
        true
    }

    pub fn cleanup(&mut self) -> bool {
        self.internal.pin_mut().cleanup();
        true
    }

    pub fn set_volume(&mut self, volume: i8) {
        self.internal.pin_mut().set_volume(volume);
    }
}
