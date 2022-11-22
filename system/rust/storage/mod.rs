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

mod ffi;

use std::{cell::RefCell, pin::Pin};

use cxx::{CxxString, UniquePtr};
pub use ffi::BluetoothKeystoreInterface;
use paste::paste;

use crate::core::{DeviceType, RawAddress};

pub struct StorageModule {
    inner: RefCell<Pin<&'static mut ffi::StorageModule>>,
}

pub type Device = ffi::Device;
pub type ClassicDevice = ffi::ClassicDevice;
pub type LeDevice = ffi::LeDevice;
pub type Mutation = ffi::Mutation;
pub type MutationEntry = ffi::MutationEntry;

impl StorageModule {
    pub unsafe fn new() -> Self {
        // TODO: get this checked by Rust experts! DO NOT SUBMIT
        Self { inner: RefCell::new(Pin::static_mut(&mut *ffi::GetStorage())) }
    }

    pub fn get_bonded_devices(&self) -> cxx::UniquePtr<cxx::CxxVector<ffi::Device>> {
        ffi::GetBondedDevices(&self.inner.borrow_mut())
    }

    pub fn modify_on_heap(&self) -> cxx::UniquePtr<ffi::Mutation> {
        ffi::ModifyOnHeap(self.inner.borrow_mut().as_mut())
    }

    pub fn provide_keystore_interface(
        &self,
        interface: cxx::UniquePtr<ffi::BluetoothKeystoreInterface>,
    ) {
        self.inner.borrow_mut().as_mut().ProvideKeystoreInterface(interface);
    }
}

pub trait IDevice {
    fn get_address(&self) -> RawAddress;
    fn get_device_type(&self) -> DeviceType;
    fn classic(self: Pin<&mut Self>) -> UniquePtr<ClassicDevice>;
    fn le(self: Pin<&mut Self>) -> UniquePtr<LeDevice>;
}

impl IDevice for ffi::Device {
    fn get_address(&self) -> RawAddress {
        ffi::GetAddress(self)
    }

    fn get_device_type(&self) -> DeviceType {
        ffi::GetDeviceType(self)
    }

    fn classic(self: Pin<&mut Self>) -> UniquePtr<ClassicDevice> {
        ffi::Classic(self)
    }

    fn le(self: Pin<&mut Self>) -> UniquePtr<LeDevice> {
        ffi::Le(self)
    }
}

macro_rules! DeviceGetterSetterInterface {
    ($name : ident, $($field:ident$(,)?)*) => {
        paste! {
           pub trait  [< I$name >] {
                $(fn [<get_ $field>](&self) -> UniquePtr<CxxString>;)*
                $(fn [<set_ $field>](self: Pin<&mut Self>, value: &str) -> UniquePtr<MutationEntry>;)*
           }

           impl [< I$name >] for ffi::$name {
                $(fn [<get_ $field>](&self) -> UniquePtr<CxxString> {
                    ffi::[<Get $field:camel>](self)
                })*
                $(fn [<set_ $field>](self: Pin<&mut Self>, value: &str) -> UniquePtr<MutationEntry> {
                    ffi::[<Set $field:camel>](self, value)
                })*
           }
        }
    };
}

DeviceGetterSetterInterface!(ClassicDevice, raw_link_key);

DeviceGetterSetterInterface!(
    LeDevice,
    local_id,
    peer_id,
    local_encryption_keys,
    peer_encryption_keys,
    local_signature_resolving_keys,
    peer_signature_resolving_keys,
);

pub trait IMutation {
    fn add(self: Pin<&mut Self>, entry: UniquePtr<MutationEntry>);
}

impl IMutation for Mutation {
    fn add(self: Pin<&mut Self>, entry: UniquePtr<MutationEntry>) {
        ffi::Add(self, entry)
    }
}
