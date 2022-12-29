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

//! This module is injected into the StorageModule to read/write its keys using the BluetoothKeystoreService in Java,
//! rather than writing to disk. It exposes no external APIs for other modules to use.

mod ffi;
mod jni_callbacks;

pub use self::jni_callbacks::GattJniCallbacks;

pub use ffi::GattServerCallbacks;

#[allow(missing_docs)]
pub struct GattModule<'a> {
    jni_module: &'a (dyn GattJniCallbacks + 'a),
}

impl<'a> GattModule<'a> {
    /// Constructor. Depends on `jni_module` to send callbacks in the JNI thread.
    pub fn new(jni_module: &'a dyn GattJniCallbacks) -> Self {
        Self { jni_module }
    }

    /// TEMP
    pub fn start(&self) {
        self.jni_module.ack("hello, world!")
    }
}
