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

//! Shared data-types and utility methods go here.

mod ffi;

use std::{
    fmt::{Debug, Display},
    thread,
};

use cxx::UniquePtr;
pub use ffi::Uuid;
use log::info;

use crate::{gatt::GattJniCallbacks, GlobalModuleRegistry};

use self::ffi::GattServerCallbacks;

/// A 6-byte MAC address corresponding to a Bluetooth device
///
/// Try to avoid using in favor of an Address tagged with the AddressType
#[repr(C)]
pub struct RawAddress {
    address: [u8; 6],
}

impl Display for RawAddress {
    fn fmt(&self, _f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        unimplemented!(concat!(
            "Intentionally not implemented, please use either Debug (for debug output) ",
            "or to_unredacted_string() (for lossless serialization)"
        ))
    }
}

impl Debug for RawAddress {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(
            f,
            "{:02x}:{:02x}:{:02x}:{:02x}:{:02x}:{:02x}",
            self.address[0],
            self.address[1],
            self.address[2],
            self.address[3],
            self.address[4],
            self.address[5],
        )
    }
}

struct GattJniCallbacksImpl(UniquePtr<GattServerCallbacks>);

impl GattJniCallbacks for GattJniCallbacksImpl {
    fn ack(&self, _x: &str) {
        info!("Rust POC has started!")
    }
}

fn init(gatt_server_callbacks: UniquePtr<GattServerCallbacks>) {
    thread::spawn(move || {
        GlobalModuleRegistry::start(&GattJniCallbacksImpl(gatt_server_callbacks));
    });
}
