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
    gatt::ffi::{AttTransportImpl, GattCallbacksImpl},
    GlobalModuleRegistry, MainThreadTxMessage, GLOBAL_MODULE_REGISTRY,
};

use self::ffi::GattServerCallbacks;

fn start(gatt_server_callbacks: UniquePtr<GattServerCallbacks>) {
    if rust_event_loop_is_enabled() {
        thread::spawn(move || {
            GlobalModuleRegistry::start(
                Rc::new(GattCallbacksImpl(gatt_server_callbacks)),
                Rc::new(AttTransportImpl()),
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
