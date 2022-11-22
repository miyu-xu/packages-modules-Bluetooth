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

use bluetooth_core::{GlobalModuleRegistry, JniCallbacks};
use jni::{objects::JObject, JNIEnv, JavaVM};

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

#[no_mangle]
/// Callback indicating when the KeyStore class is initialized in Java
pub extern "system" fn Java_com_android_bluetooth_btservice_bluetoothkeystore_BluetoothKeystoreNativeInterface_classInit(
    env: JNIEnv,
    _obj: JObject,
) {
    // TODO(aryarahul) - for the POC, we piggyback on Keystore classInit to start the Rust main thread
    // This should be replaced with a dedicated call from AdapterService
    let vm = env.get_java_vm().expect("failed to get JVM");

    GlobalModuleRegistry::init(move |rx| {
        let core_jni_module = CoreJniModule::new(&vm);
        let mut keystore_jni = keystore::KeystoreJniModule::new(&core_jni_module);
        let jni_callbacks = JniCallbacks { keystore_jni: &mut keystore_jni };
        GlobalModuleRegistry::start_event_loop(jni_callbacks, rx);
    });
}
