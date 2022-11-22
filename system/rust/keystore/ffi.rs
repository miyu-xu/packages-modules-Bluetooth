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

use inner::*;

#[cxx::bridge]
mod inner {

    #[namespace = "bluetooth::storage"]
    extern "C++" {
        type BluetoothKeystoreInterface = crate::storage::BluetoothKeystoreInterface;
    }

    #[namespace = "bluetooth::keystore"]
    unsafe extern "C++" {
        include!("keystore_shim.h");

        #[cxx_name = "GetInterface"]
        #[must_use]
        fn get_interface(ptr: Box<KeystoreInterfaceImpl>) -> UniquePtr<BluetoothKeystoreInterface>;
    }

    #[namespace = "bluetooth::keystore"]
    extern "Rust" {
        type KeystoreInterfaceImpl;

        fn store_key(self: &KeystoreInterfaceImpl, key: &str, value: &str);
        fn get_key(self: &KeystoreInterfaceImpl, key: &str) -> String;
    }
}
