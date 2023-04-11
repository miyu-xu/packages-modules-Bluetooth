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

use crate::core::{start, stop};

use cxx::{type_id, ExternType};
pub use inner::*;
use tokio::sync::oneshot;

unsafe impl Send for GattServerCallbacks {}

unsafe impl ExternType for Uuid {
    type Id = type_id!("bluetooth::Uuid");
    type Kind = cxx::kind::Trivial;
}

unsafe impl ExternType for AddressWithType {
    type Id = type_id!("bluetooth::core::AddressWithType");
    type Kind = cxx::kind::Trivial;
}

#[allow(dead_code, missing_docs)]
#[cxx::bridge]
mod inner {
    #[derive(Debug)]
    pub enum AddressTypeForFFI {
        Public,
        Random,
    }

    #[namespace = "bluetooth::core"]
    extern "C++" {
        include!("src/core/ffi/types.h");
        type AddressWithType = crate::core::address::AddressWithType;
    }

    #[namespace = "bluetooth"]
    extern "C++" {
        include!("bluetooth/uuid.h");
        type Uuid = crate::core::uuid::Uuid;
    }

    #[namespace = "bluetooth::gatt"]
    unsafe extern "C++" {
        include!("src/gatt/ffi/gatt_shim.h");
        type GattServerCallbacks = crate::gatt::GattServerCallbacks;
    }

    #[namespace = "bluetooth::connection"]
    unsafe extern "C++" {
        include!("src/connection/ffi/connection_shim.h");
        type LeAclManagerShim = crate::connection::LeAclManagerShim;
        type AddressResolverShim = crate::connection::AddressResolverShim;
    }

    #[namespace = "bluetooth::rust_shim"]
    extern "Rust" {
        fn start(
            gatt_server_callbacks: UniquePtr<GattServerCallbacks>,
            le_acl_manager: UniquePtr<LeAclManagerShim>,
            address_resolver: SharedPtr<AddressResolverShim>,
        );

        fn stop();
    }
}

/// A callback passed by mutable reference to C++ (to avoid boxing),
/// that can only be invoked once.
pub struct Callback<T>(Option<oneshot::Sender<T>>);

impl<T> Callback<T> {
    /// Invoke the callback. Panics if invoked more than once.
    pub fn invoke(&mut self, val: T) {
        let _ = self.0.take().expect("callback can only be invoked once").send(val);
    }
}

impl<T> From<oneshot::Sender<T>> for Callback<T> {
    fn from(tx: oneshot::Sender<T>) -> Self {
        Self(Some(tx))
    }
}
