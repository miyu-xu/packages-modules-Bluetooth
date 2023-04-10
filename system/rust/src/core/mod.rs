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
    connection::{LeAclManagerImpl, LeAclManagerShim},
    gatt::ffi::{AttTransportImpl, GattCallbacksImpl},
    GlobalModuleRegistry, MAIN_THREAD_TX, MainThreadTxMessage,
};

use self::ffi::GattServerCallbacks;

fn start(
    gatt_server_callbacks: UniquePtr<GattServerCallbacks>,
    le_acl_manager: UniquePtr<LeAclManagerShim>,
) {
    if rust_event_loop_is_enabled() {
        thread::spawn(move || {
            GlobalModuleRegistry::start(
                Rc::new(GattCallbacksImpl(gatt_server_callbacks)),
                Rc::new(AttTransportImpl()),
                LeAclManagerImpl(le_acl_manager),
            );
        });
    }
}

fn stop() {
    let _ = MAIN_THREAD_TX.with(|tx| tx.send(MainThreadTxMessage::Stop));
}
