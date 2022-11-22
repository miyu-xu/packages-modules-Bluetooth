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
    sync::mpsc,
    thread::{self, JoinHandle},
};

use bluetooth_core::{GlobalModuleRegistry, JniCallbacks};
use jni::{objects::JClass, JNIEnv, JavaVM};
use keystore::KeystoreJniModule;
use log::warn;

mod keystore;

/// This class manages all JNI callbacks from Rust modules
///
/// On startup, it registers our thread permanently with the JVM for JNI. Then if a module
/// wishes to call into Java, it can obtain a reference to the JNIEnv from this module.
struct CoreJniModule {
    handle: JoinHandle<()>,
    tx: mpsc::Sender<JniThreadEvent>,
}


impl CoreJniModule {
    fn new(vm: &JavaVM) -> Self {
        let (tx, rx) = mpsc::channel::<JniThreadEvent>();
        let handle = thread::spawn(move || {
            let env = vm.attach_current_thread_permanently().expect("failed to attach JNI thread");
        });
        Self { handle, tx }
    }

    pub fn with_jni_env<T: FnOnce(&mut JNIEnv) + Send>(&self, f: T) {
        self.tx.send(JniThreadEvent::Func(Box::new(f))).expect(
            "JNI env requested when JNI thread queue has stopped - this should never happen",
        );
    }
}

impl Drop for CoreJniModule {
    fn drop(&mut self) {
        self.tx.send(JniThreadEvent::Stop).unwrap()
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

    GlobalModuleRegistry::init(move |rx| {
        let core_jni_module = CoreJniModule::new(&vm);
        let keystore_jni = KeystoreJniModule::new(&core_jni_module);
        let jni_callbacks = JniCallbacks { keystore_jni: &keystore_jni };
        GlobalModuleRegistry::start_event_loop(jni_callbacks, rx);
    });
}
