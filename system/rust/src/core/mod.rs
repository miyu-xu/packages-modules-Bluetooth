//! Shared data-types and utility methods go here.

pub mod address;
mod ffi;
pub mod shared_box;
pub mod shared_mutex;
pub mod uuid;

use std::{rc::Rc, thread};

use bt_common::init_flags::rust_event_loop_is_enabled;
use cxx::UniquePtr;

use crate::{
    connection::{
        AddressManagerImpl, HciConnectProxyImpl, LeAddressManagerShim, LeConnectHciManagerShim,
    },
    gatt::ffi::{AttTransportImpl, GattCallbacksImpl},
    GlobalModuleRegistry,
};

use self::ffi::GattServerCallbacks;

fn init(
    hci_connect_proxy: UniquePtr<LeConnectHciManagerShim>,
    address_manager: UniquePtr<LeAddressManagerShim>,
    gatt_server_callbacks: UniquePtr<GattServerCallbacks>,
) {
    if rust_event_loop_is_enabled() {
        thread::spawn(move || {
            GlobalModuleRegistry::start(
                Rc::new(GattCallbacksImpl(gatt_server_callbacks)),
                Rc::new(AttTransportImpl()),
                Rc::new(HciConnectProxyImpl(hci_connect_proxy)),
                AddressManagerImpl(address_manager),
            );
        });
    }
}
