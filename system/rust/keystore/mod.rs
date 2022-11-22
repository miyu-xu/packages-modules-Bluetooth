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

use std::{cell::RefCell, collections::HashMap, pin::Pin, sync::mpsc};

use log::warn;
use paste::paste;

mod ffi;
mod jni_callbacks;

pub use jni_callbacks::KeystoreJniCallbacks;

use crate::{
    core::DeviceType,
    storage::{Device, IClassicDevice, IDevice, ILeDevice, IMutation, Mutation},
    MainThreadTx, MAIN_THREAD_TX,
};

const ENCRYPTED_STRING: &str = "encrypted";

pub struct KeystoreModule<'a> {
    jni_module: &'a dyn jni_callbacks::KeystoreJniCallbacks,
    storage_module: &'a crate::storage::StorageModule,
    parameter_provider_module: &'a crate::parameter_provider::ParameterProvider,
    key_cache: RefCell<HashMap<String, String>>,
}

macro_rules! fix_key {
    ($key : ident, $literal_key : literal, $device : ident, $specialized_device : ident, $mutation : ident, $self : expr) => {
        paste! {
        if let Some(data) = $specialized_device.[<get_$key>]().as_ref() {
            let key = $device.get_address().to_string() + "-" + $literal_key;
            let is_encrypted = data == ENCRYPTED_STRING;
            if $self.parameter_provider_module.is_common_criteria_mode() {
                if !is_encrypted {
                    $self.store_key(
                        &key,
                        data.to_str().expect("corrupt key cannot be converted to UTF-8"),
                    );
                    $mutation.as_mut().add(
                        $specialized_device.[<set_ $key>](
                            ENCRYPTED_STRING
                        )
                    )
                }
                todo!()
            } else {
                if is_encrypted {
                    $mutation.as_mut().add(
                        $specialized_device.as_mut().[<set_ $key>](
                            &$self.get_key(&key),
                        ),
                    );
                }
            }}
        }
    };
}

impl<'a> KeystoreModule<'a> {
    pub fn new(
        jni_module: &'a dyn jni_callbacks::KeystoreJniCallbacks,
        storage_module: &'a crate::storage::StorageModule,
        parameter_provider_module: &'a crate::parameter_provider::ParameterProvider,
    ) -> Self {
        storage_module.provide_keystore_interface(ffi::get_interface(Box::new(
            KeystoreInterfaceImpl::new(MAIN_THREAD_TX.with(|tx| tx.clone())),
        )));
        Self {
            jni_module,
            storage_module,
            parameter_provider_module,
            key_cache: RefCell::new(HashMap::new()),
        }
    }

    fn store_key(&self, prefix: &str, decrypted: &str) {
        self.key_cache.borrow_mut().insert(prefix.to_owned(), decrypted.to_owned());
        self.jni_module.set_encrypt_key_or_remove_key_callback(prefix, decrypted);
    }

    fn get_key(&self, prefix: &str) -> String {
        if let Some(decrypted) = self.key_cache.borrow().get(prefix) {
            decrypted.clone()
        } else {
            let decrypted = self.jni_module.get_key(prefix);
            self.key_cache.borrow_mut().insert(prefix.to_owned(), decrypted.to_owned());
            decrypted
        }
    }

    /// If the keystore is enabled, then we should clear any keys
    /// present in the storage layer (since we will proxy all reads/writes). Conversely, if it is disabled, we should re-populate the storage layer with
    /// keys pulled from the keystore.
    pub fn fix_storage_layer(&self) {
        let mut mutation_ptr = self.storage_module.modify_on_heap();
        let mut mutation = mutation_ptr.pin_mut();

        let mut devices = self.storage_module.get_bonded_devices();

        for mut device in devices.pin_mut() {
            match device.get_device_type() {
                DeviceType::BR_EDR => {
                    self.fix_classic_keys(mutation.as_mut(), device.as_mut());
                    self.fix_le_keys(mutation.as_mut(), device.as_mut());
                }
                DeviceType::LE => {
                    self.fix_le_keys(mutation.as_mut(), device.as_mut());
                }
                DeviceType::DUAL => {
                    self.fix_classic_keys(mutation.as_mut(), device.as_mut());
                }
                _ => {
                    warn!("Unknown DeviceType for device XYZ, skipping")
                }
            }
        }
    }

    fn fix_classic_keys(&self, mut mutation: Pin<&mut Mutation>, mut device: Pin<&mut Device>) {
        let mut classic_device_ptr = device.as_mut().classic();
        let mut classic_device = classic_device_ptr.pin_mut();

        fix_key!(raw_link_key, "LinkKey", device, classic_device, mutation, self);
    }

    fn fix_le_keys(&self, mut mutation: Pin<&mut Mutation>, mut device: Pin<&mut Device>) {
        let mut le_device_ptr = device.as_mut().le();
        let mut le_device = le_device_ptr.pin_mut();

        fix_key!(local_encryption_keys, "LE_KEY_LENC", device, le_device, mutation, self);
        fix_key!(peer_encryption_keys, "LE_KEY_PENC", device, le_device, mutation, self);
        fix_key!(local_id, "LE_KEY_LID", device, le_device, mutation, self);
        fix_key!(peer_id, "LE_KEY_PID", device, le_device, mutation, self);
        fix_key!(local_signature_resolving_keys, "LE_KEY_LCSRK", device, le_device, mutation, self);
        fix_key!(peer_signature_resolving_keys, "LE_KEY_PCSRK", device, le_device, mutation, self);
    }
}

/// This struct implements BluetoothKeystoreInterface and can be passed (via a shim) to the Storage Module in C++
pub struct KeystoreInterfaceImpl {
    tx: MainThreadTx,
}

impl KeystoreInterfaceImpl {
    fn new(tx: MainThreadTx) -> Self {
        Self { tx }
    }
    /// Store the (cleartext) key in the keystore, indexed by prefix
    pub fn store_key(&self, key: &str, value: &str) {
        let key = key.to_string();
        let value = value.to_string();
        self.tx
            .send(Box::new(move |modules| modules.keystore_module.store_key(&key, &value)))
            .unwrap();
    }

    /// Retrieve a key from the keystore by prefix
    pub fn get_key(&self, key: &str) -> String {
        let key = key.to_string();
        let (tx, rx) = mpsc::channel();
        self.tx
            .send(Box::new(move |modules| tx.send(modules.keystore_module.get_key(&key)).unwrap()))
            .unwrap();
        rx.recv().unwrap()
    }
}
