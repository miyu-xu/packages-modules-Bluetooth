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

use cxx::type_id;

pub use inner::*;

unsafe impl cxx::ExternType for RawAddress {
    type Id = type_id!("bluetooth::hci::rust_shim::RawAddress");
    type Kind = cxx::kind::Trivial;
}

#[allow(dead_code, missing_docs)]
#[cxx::bridge]
mod inner {
    #[namespace = "bluetooth::hci"]
    #[repr(i32)]
    enum DeviceType {
        UNKNOWN,
        BR_EDR,
        LE,
        DUAL,
    }

    #[namespace = "bluetooth::hci"]
    extern "C++" {
        include!("gd/hci/enum_helper.h");
        type DeviceType;
    }

    #[namespace = "bluetooth::hci::rust_shim"]
    extern "C++" {
        include!("core/hci_shim.h");
        type RawAddress = super::super::RawAddress;
    }
}
