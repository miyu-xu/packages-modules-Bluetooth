// Copyright 2022, The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//! The core event loop for Rust modules. Here we start up Rust modules in dependency order.
//! We expect GD C++ to already be running so we can use entry.cc to obtain references to C++ modules.
//! In production, this should be triggered from JNI so we can inject JniCallbacks.

use gatt::GattJniCallbacks;
use log::warn;
use std::sync::mpsc;
use std::sync::Mutex;

#[cfg(feature = "via_android_bp")]
mod do_not_use {
    // DO NOT USE
    #[allow(unused)]
    use bt_shim::*;
}

pub mod core;
pub mod gatt;

/// The owner of the main Rust thread on which all Rust modules run
pub struct GlobalModuleRegistry {
    task_tx: MainThreadTx,
}

/// The ModuleViews lets us access all publicly accessible Rust modules from Java / C++ while the stack is
/// running. If a module should not be exposed outside of Rust GD, there is no need to include it here.
pub struct ModuleViews<'a, 'b>
where
    'b: 'a,
{
    /// Proxies calls into GATT server
    pub gatt_module: &'a gatt::GattModule<'b>,
}

impl GlobalModuleRegistry {
    /// Handles bringup of all Rust modules. This occurs after GD C++ modules have started, but before the legacy stack
    /// has initialized.
    /// Must be invoked from the Rust thread after JNI initializes it and passes in JNI modules.
    pub fn start(gatt_callbacks: &dyn GattJniCallbacks) {
        let (tx, rx) = mpsc::channel::<BoxedMainThreadCallback>();
        let prev_registry = GLOBAL_MODULE_REGISTRY.lock().unwrap().replace(Self { task_tx: tx });

        // initialization should ony happen once
        assert!(prev_registry.is_none());

        // First, load GD modules (as they should be available now).
        // As nothing stops us from having multiple such modules, their constructors are unsafe
        // To avoid having multiple mutable references to interior modules, we wrap them in a Rust shim that owns the single mutable reference
        // see https://users.rust-lang.org/t/single-mutable-reference-rule-and-ffi/50546/6
        // TODO: put some modules here

        // Then we have the pure-Rust modules
        let gatt_module = &gatt::GattModule::new(gatt_callbacks);

        // All modules that are visible from incoming JNI / top-level interfaces should be exposed here
        let modules = ModuleViews { gatt_module };

        // This is the core event loop that serializes incoming requests into the Rust thread
        // do_in_rust_thread lets us post into here from foreign threads
        while let Ok(f) = rx.recv() {
            f(&modules);
        }
        warn!("Rust thread queue has stopped, shutting down executor thread");
    }
}

type BoxedMainThreadCallback = Box<dyn FnOnce(&ModuleViews) + Send>;
type MainThreadTx = mpsc::Sender<BoxedMainThreadCallback>;

static GLOBAL_MODULE_REGISTRY: Mutex<Option<GlobalModuleRegistry>> = Mutex::new(None);

thread_local! {
    /// The TX end of a channel into the Rust thread, so external callers can access Rust modules.
    /// JNI / direct FFI should use do_in_rust_thread for convenience, but objects passed into C++ as callbacks should
    /// clone this channel so we fail loudly if it's not yet initialized.
    ///
    /// This will be lazily initialized on first use from each client thread
    pub static MAIN_THREAD_TX: MainThreadTx =
        GLOBAL_MODULE_REGISTRY.lock().unwrap().as_ref().expect("stack not initialized").task_tx.clone();
}

/// Posts a callback to the Rust thread and gives it access to public Rust modules, used from JNI.
///
/// Do not call this from Rust modules / the Rust thread! Instead, Rust modules should receive references to their dependent modules
/// at startup. If you are passing callbacks into C++, don't use this method either - instead, acquire a clone of
/// MAIN_THREAD_TX when the callback is created. This ensures that we never have "invalid" callbacks that may still work
/// depending on when the GLOBAL_MODULE_REGISTRY is initialized.
pub fn do_in_rust_thread(
    f: impl FnOnce(&ModuleViews) + Send + 'static,
) -> Result<(), mpsc::SendError<BoxedMainThreadCallback>> {
    MAIN_THREAD_TX.with(|tx| tx.send(Box::new(f)))
}
