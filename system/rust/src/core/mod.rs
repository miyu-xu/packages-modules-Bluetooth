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

use std::{rc::Rc, thread};

use bt_common::init_flags::rust_event_loop_is_enabled;
use cxx::UniquePtr;
#[cfg(not(test))]
pub use ffi::CxxUuid;

use crate::{
    gatt::ffi::{AttTransportImpl, GattCallbacksImpl},
    GlobalModuleRegistry,
};

use self::ffi::GattServerCallbacks;

fn init(gatt_server_callbacks: UniquePtr<GattServerCallbacks>) {
    if rust_event_loop_is_enabled() {
        thread::spawn(move || {
            GlobalModuleRegistry::start(
                Rc::new(GattCallbacksImpl(gatt_server_callbacks)),
                Rc::new(AttTransportImpl()),
            );
        });
    }
}

/// Get the raw bytes (in big-endian order) for a C++ UUID
#[cfg(not(test))]
pub fn get_128_be_uuid_bytes(uuid: &CxxUuid) -> &[u8; 16] {
    ffi::get_128_be_uuid_bytes(uuid).try_into().expect("ffi should give us exactly 16 bytes")
}

#[cfg(test)]
pub use mock::*;

/// Get the raw bytes (in big-endian order) for a C++ UUID
#[cfg(test)]
mod mock {
    use cxx::{type_id, ExternType};

    use crate::gatt::server::gatt_database::Uuid;

    pub struct CxxUuid([u8; 16]);

    unsafe impl ExternType for CxxUuid {
        type Id = type_id!("bluetooth::Uuid");
        type Kind = cxx::kind::Opaque;
    }

    impl CxxUuid {
        pub fn new_mocked(uuid: Uuid) -> Box<Self> {
            let mut data = uuid.0;
            data.reverse();
            Box::new(Self(data))
        }
    }

    pub fn get_128_be_uuid_bytes(uuid: &CxxUuid) -> &[u8; 16] {
        &uuid.0
    }
}
