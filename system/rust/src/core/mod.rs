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

use std::{fmt::Debug, rc::Rc, thread};

use cxx::UniquePtr;
pub use ffi::Uuid;

use crate::{
    gatt::{
        ids::{AttHandle, ConnectionId, TransactionId},
        GattCallbacks,
    },
    packets::AttAttributeDataView,
    GlobalModuleRegistry,
};

use self::ffi::GattServerCallbacks;

/// A 6-byte MAC address corresponding to a Bluetooth device
///
/// Try to avoid using in favor of an Address tagged with the AddressType
#[repr(C)]
pub struct RawAddress(pub [u8; 6]);

impl Debug for RawAddress {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "xx:xx:xx:xx:{:02x}:{:02x}", self.0[4], self.0[5],)
    }
}

struct GattCallbacksImpl(UniquePtr<GattServerCallbacks>);

impl GattCallbacks for GattCallbacksImpl {
    fn on_server_read_characteristic(
        &self,
        conn_id: ConnectionId,
        trans_id: TransactionId,
        handle: AttHandle,
        offset: u32,
        is_long: bool,
    ) {
        self.0
            .as_ref()
            .unwrap()
            .on_server_read_characteristic(conn_id.0, trans_id.0, handle.0, offset, is_long);
    }

    fn on_server_write_characteristic(
        &self,
        conn_id: ConnectionId,
        trans_id: TransactionId,
        handle: AttHandle,
        offset: u32,
        need_response: bool,
        is_prepare: bool,
        value: AttAttributeDataView,
    ) {
        self.0.as_ref().unwrap().on_server_write_characteristic(
            conn_id.0,
            trans_id.0,
            handle.0,
            offset,
            need_response,
            is_prepare,
            &value.get_raw_payload().collect::<Vec<_>>(),
        );
    }
}

fn init(gatt_server_callbacks: UniquePtr<GattServerCallbacks>) {
    thread::spawn(move || {
        GlobalModuleRegistry::start(Rc::new(GattCallbacksImpl(gatt_server_callbacks)));
    });
}

/// Get the raw bytes (in big-endian order) for a C++ UUID
pub fn get_128_be_uuid_bytes(uuid: &Uuid) -> &[u8; 16] {
    ffi::get_128_be_uuid_bytes(uuid).try_into().expect("ffi should give us exactly 16 bytes")
}
