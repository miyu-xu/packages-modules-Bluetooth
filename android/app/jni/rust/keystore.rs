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

//! JNI methods wrapped by BluetoothKeystoreNativeInterface

use jni::objects::{JClass, JObject};
use jni::JNIEnv;
use libbluetooth_core::do_on_rust_thread;
use libbluetooth_core::keystore::KeystoreJniCallbacks;

use crate::CoreJniModule;

pub struct KeystoreJniModule<'a> {
    core_jni_module: &'a CoreJniModule<'a>,
    class: Option<JClass<'a>>,
}

impl<'a> KeystoreJniModule<'a> {
    pub(crate) fn new(core_jni_module: &'a CoreJniModule) -> Self {
        Self { core_jni_module, class: None }
    }
}

impl<'a> KeystoreJniCallbacks for KeystoreJniModule<'a> {
    fn set_encrypt_key_or_remove_key_callback(&self, prefix: &str, decrypted: &str) {
        let env = self.core_jni_module.env;
        let prefix = env.new_string(prefix).expect("string conversion failed");
        let decrypted = env.new_string(decrypted).expect("string conversion failed");
        env.call_static_method(
            self.class.expect("KeystoreClass not initialized before callback invoked"),
            "setEncryptKeyOrRemoveKeyCallback",
            "(Ljava/lang/String;Ljava/lang/String;)V",
            &[prefix.into(), decrypted.into()],
        )
        .expect("failed to invoke set_encrypt_key_or_remove_key_callback callback");
    }

    fn get_key(&self, prefix: &str) -> String {
        let env = self.core_jni_module.env;
        let prefix = env.new_string(prefix).expect("string conversion failed");
        let ret = env
            .call_static_method(
                self.class.expect("KeystoreClass not initialized before callback invoked"),
                "setEncryptKeyOrRemoveKeyCallback",
                "(Ljava/lang/String;)Ljava/lang/String;",
                &[prefix.into()],
            )
            .expect("failed to invoke get_key callback");
        env.get_string(ret.l().expect("didn't get an object").into())
            .expect("failed to parse string")
            .into()
    }
}

#[no_mangle]
/// Callback indicating when the KeyStore instance is initialized in Java
/// This means we can fix all the storage entries to make them consistent with our encryption mode
pub extern "system" fn Java_com_android_bluetooth_btservice_bluetoothkeystore_BluetoothKeystoreNativeInterface_init(
    _env: JNIEnv,
    _obj: JObject,
) {
    do_on_rust_thread(|modules| modules.keystore_module.fix_storage_layer())
        .expect("stack is shutting down while initializing keystore");
}
