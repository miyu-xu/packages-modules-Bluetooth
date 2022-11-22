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

use log::warn;
use std::sync::mpsc;
use std::sync::mpsc::Receiver;
use std::sync::Mutex;
use std::thread::{self, JoinHandle};

pub mod core;
pub mod keystore;
pub mod parameter_provider;
pub mod storage;

#[allow(dead_code)]
pub struct GlobalModuleRegistry {
    handle: JoinHandle<()>,
    task_tx: MainThreadTx,
}

/// The ModuleViews lets us access all publicly accessible Rust modules from Java / C++ while the stack is
/// running. If a module should not be exposed outside of Rust GD, there is no need to include it here.
pub struct ModuleViews<'a, 'b> {
    pub keystore_module: &'b keystore::KeystoreModule<'a>,
}

/// The JniCallbacks struct contains references to all the JNI callback interfaces needed by Rust modules. It is populated by
/// libbluetooth_jni_rs and passed in as a callback during stack startup.
pub struct JniCallbacks<'a> {
    pub keystore_jni: &'a mut dyn keystore::KeystoreJniCallbacks,
}

impl GlobalModuleRegistry {
    /// Handles bringup of all Rust modules. This occurs after GD C++ modules have started, but before the legacy stack
    /// has initialized.
    pub fn init<'a>(
        initialize_jni_and_start_event_loop: impl FnOnce(Receiver<BoxedMainThreadCallback>)
            + Send
            + 'static,
    ) {
        let (tx, rx) = mpsc::channel::<BoxedMainThreadCallback>();
        let handle = thread::spawn(move || {
            initialize_jni_and_start_event_loop(rx);
        });

        let prev_registry =
            GLOBAL_MODULE_REGISTRY.lock().unwrap().replace(Self { handle, task_tx: tx });

        // initialization should ony happen once
        assert!(prev_registry.is_none());
    }

    pub fn start_event_loop<'a>(
        jni_callbacks: JniCallbacks<'a>,
        rx: mpsc::Receiver<BoxedMainThreadCallback>,
    ) {
        // First, load GD modules (as they should be available now).
        // As nothing stops us from having multiple such modules, their constructors are unsafe
        // To avoid having multiple mutable references to interior modules, we wrap them in a Rust shim that owns the single mutable reference
        // see https://users.rust-lang.org/t/single-mutable-reference-rule-and-ffi/50546/6
        let storage_module = unsafe { storage::StorageModule::new() };
        let parameter_provider_module = parameter_provider::ParameterProvider::new();

        // Finally we have the pure-Rust modules
        let keystore_module = keystore::KeystoreModule::new(
            jni_callbacks.keystore_jni,
            &storage_module,
            &parameter_provider_module,
        );

        // All modules that are visible from incoming JNI / top-level interfaces should be exposed here
        let modules = ModuleViews { keystore_module: &keystore_module };

        // This is the core event loop that serializes incoming requests into the Rust thread
        // do_on_rust_thread lets us post into here from foreign threads
        while let Ok(f) = rx.recv() {
            f(&modules);
        }
        warn!("JNI thread queue has stopped, shutting down executor thread");
    }
}

type BoxedMainThreadCallback = Box<dyn FnOnce(&ModuleViews) + Send>;
type MainThreadTx = mpsc::Sender<BoxedMainThreadCallback>;

static GLOBAL_MODULE_REGISTRY: Mutex<Option<GlobalModuleRegistry>> = Mutex::new(None);

thread_local! {
    // This will be lazily initialized on first use from each client thread
    pub static MAIN_THREAD_TX: MainThreadTx = GLOBAL_MODULE_REGISTRY.lock().unwrap().as_ref().expect("stack not initialized").task_tx.clone();
}

/// Posts a callback to the Rust thread and gives it access to public Rust modules, used from JNI.
///
/// Do not call this from Rust! Instead, Rust modules should receive references to their dependent modules
/// at startup. If you are passing callbacks into C++, don't use this method either - instead, acquire a clone of
/// MAIN_THREAD_TX when the callback is created. This ensures that we never have "invalid" callbacks that may still work
/// depending on when the GLOBAL_MODULE_REGISTRY is initialized.
pub fn do_on_rust_thread(
    f: impl FnOnce(&ModuleViews) + Send + 'static,
) -> Result<(), mpsc::SendError<BoxedMainThreadCallback>> {
    MAIN_THREAD_TX.with(|tx| tx.send(Box::new(f)))
}
