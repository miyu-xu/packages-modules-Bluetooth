//! Shared data-types and utility methods go here.

pub mod address;
mod ffi;
pub mod shared_box;
pub mod shared_mutex;
pub mod uuid;

use std::{pin::Pin, rc::Rc, thread};

use bt_common::init_flags::rust_event_loop_is_enabled;
use cxx::{SharedPtr, UniquePtr};

use crate::{
    connection::{AddressResolverImpl, AddressResolverShim, LeAclManagerImpl, LeAclManagerShim},
    gatt::ffi::{AttTransportImpl, GattCallbacksImpl},
    GlobalModuleRegistry, MainThreadTxMessage, GLOBAL_MODULE_REGISTRY,
};

use self::ffi::{future_ready, Future, GattServerCallbacks};

pub use ffi::{invoke_callback, Callback};

fn start(
    gatt_server_callbacks: UniquePtr<GattServerCallbacks>,
    le_acl_manager: UniquePtr<LeAclManagerShim>,
    address_resolver: SharedPtr<AddressResolverShim>,
    on_started: Pin<&'static mut Future>,
) {
    if rust_event_loop_is_enabled() {
        thread::spawn(move || {
            GlobalModuleRegistry::start(
                Rc::new(GattCallbacksImpl(gatt_server_callbacks)),
                Rc::new(AttTransportImpl()),
                LeAclManagerImpl(le_acl_manager),
                AddressResolverImpl(address_resolver),
                || {
                    future_ready(on_started);
                },
            );
        });
    }
}

fn stop() {
    let _ = GLOBAL_MODULE_REGISTRY
        .try_lock()
        .unwrap()
        .as_ref()
        .map(|registry| registry.task_tx.send(MainThreadTxMessage::Stop));
}
