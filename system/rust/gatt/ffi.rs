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

pub use inner::*;

use crate::do_on_rust_thread;

#[cxx::bridge]
#[allow(unused_must_use)]
mod inner {
    impl UniquePtr<GattServerCallbacks> {}

    #[namespace = "bluetooth"]
    extern "C++" {
        type Uuid = crate::core::Uuid;
    }

    #[namespace = "bluetooth::gatt"]
    unsafe extern "C++" {
        include!("gatt/gatt_shim.h");

        /// This contains the callbacks from Rust into C++ JNI needed for GATT
        type GattServerCallbacks;

        /// This callback is invoked after a server is registered / fails to register
        #[cxx_name = "OnRegisterServer"]
        fn on_register_server(self: &GattServerCallbacks, status: i32, server_if: i32, uuid: &Uuid);
    }

    #[namespace = "bluetooth::gatt"]
    extern "Rust" {
        fn start();
    }
}

fn start() {
    do_on_rust_thread(|modules| {
        modules.gatt_module.start();
    })
    .expect("JNI call failed");
}
