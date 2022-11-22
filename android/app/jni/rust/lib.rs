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

//! The entrypoint from Java where Rust modules are started. This happens after C++ modules have started, but before
//! profiles have been initialized.

use std::{
    sync::mpsc::{self},
    thread,
};

use bluetooth_core::{GlobalModuleRegistry, JniCallbacks, ObjectProxy};
use jni::{objects::JClass, JNIEnv, JavaVM};
use keystore::KeystoreJniModule;
use log::warn;

mod keystore;

/// This class manages all JNI callbacks from Rust modules
///
/// On startup, it registers our thread permanently with the JVM for JNI. Then if a module
/// wishes to call into Java, it can obtain a reference to the JNIEnv from this module.
struct CoreJniModule<'a> {
    env: JNIEnv<'a>,
}

impl<'a> CoreJniModule<'a> {
    fn new(vm: &'a JavaVM) -> Self {
        Self { env: vm.attach_current_thread_permanently().expect("failed to attach JNI thread") }
    }
}

enum JniThreadEvent<'b> {
    Func(Box<dyn for<'a> FnOnce(&'a mut JniCallbacks<'b>) + Send + 'b>),
    Stop,
}

struct JniChannel<'b> {
    tx: mpsc::Sender<JniThreadEvent<'b>>,
}

impl<'b> ObjectProxy<'b> for JniChannel<'b> {
    type T = JniCallbacks<'b>;

    fn with<F: for<'a> FnOnce(&'a mut Self::T) + Send + 'b>(&self, f: F) {
        self.tx.send(JniThreadEvent::Func(Box::new(f)));
    }
}

/// Callback indicating when the KeyStore class is initialized in Java
#[no_mangle]
pub extern "system" fn Java_com_android_bluetooth_btservice_bluetoothkeystore_BluetoothKeystoreNativeInterface_classInitNative(
    env: JNIEnv,
    _class: JClass,
) {
    // TODO(aryarahul) - for the POC, we piggyback on Keystore classInit to start the Rust main thread
    // This should be replaced with a dedicated call from AdapterService
    let vm = env.get_java_vm().expect("failed to get JVM");

    // this is the core Rust thread - everything will take place here or in a child thread
    thread::spawn(move || {
        // this thread is registered with JNI and is where JNI modules run
        // non-JNI modules communicate with it over a channel from a separate thread

        // we initialize the modules in a parent scope to guarantee that JNI is available to Rust modules
        // (this is enforced by exciting lifetime annotations - see object_proxy.rs for the details!)
        let core_jni_module = CoreJniModule::new(&vm);
        let keystore_jni = KeystoreJniModule::new(&core_jni_module);
        let mut jni_callbacks = JniCallbacks { keystore_jni: &keystore_jni };

        thread::scope(|scope| {
            let (tx, rx) = mpsc::channel();

            // this thread starts the Rust module loop, delegating to the JNI thread when needed
            // since we are within a scope, we always have access to the JNI modules
            scope.spawn(move || {
                let jni_proxy = JniChannel { tx };
                GlobalModuleRegistry::start(&jni_proxy);
            });

            // now, on the JNI thread, we can begin processing events from the Rust module loop
            // note that we can't move this block outside of thread::scope, since then we will forever wait for the
            // Rust thread to complete
            while let Ok(event) = rx.recv() {
                match event {
                    JniThreadEvent::Func(f) => f(&mut jni_callbacks),
                    JniThreadEvent::Stop => {
                        warn!("Rust JNI thread queue has been explicitly stopped, shutting down executor thread and dropping queued requests");
                        return;
                    }
                }
            }
            warn!("Rust JNI thread queue has stopped, shutting down executor thread");
        })
    });
}
