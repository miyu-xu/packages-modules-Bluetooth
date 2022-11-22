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

//! The module where all data about devices are ultimately persisted to disk

mod ffi;

use std::{cell::RefCell, pin::Pin};

use cxx::{CxxString, UniquePtr};
pub use ffi::BluetoothKeystoreInterface;
use paste::paste;

use crate::core::{DeviceType, RawAddress};

#[allow(missing_docs)]
pub struct StorageModule {
    inner: RefCell<Pin<&'static mut ffi::StorageModule>>,
}

/// A handle to a Device (either BR/EDR or LE) in the Storage layer
pub type Device = ffi::Device;
/// A handle to a Device specialized to BR/EDR
pub type ClassicDevice = ffi::ClassicDevice;
/// A handle to a Device specialized to LE
pub type LeDevice = ffi::LeDevice;
/// Pending changes to objects in the Storage layer that are not yet committed
pub type Mutation = ffi::Mutation;
/// Part of a pending change that can be added to a Mutation (and later committed)
pub type MutationEntry = ffi::MutationEntry;

impl StorageModule {
    /// Constructor
    ///
    /// # Safety
    /// Only call this once! Otherwise we will have multiple mutable references to the same object.
    /// All uses of the underlying StorageModule should go through this class so we can mediate accesses
    /// using the RefCell
    pub unsafe fn new() -> Self {
        // TODO: get this checked by Rust experts! DO NOT SUBMIT
        Self { inner: RefCell::new(Pin::static_mut(&mut *ffi::GetStorage())) }
    }

    /// Get a list of bonded devices from config
    pub fn get_bonded_devices(&self) -> cxx::UniquePtr<cxx::CxxVector<ffi::Device>> {
        ffi::GetBondedDevices(&self.inner.borrow_mut())
    }

    /// Modify the underlying config by starting a mutation. All entries in the mutation will be applied atomically when
    /// mutation.commit() is called.
    pub fn modify(&self) -> cxx::UniquePtr<ffi::Mutation> {
        ffi::ModifyOnHeap(self.inner.borrow_mut().as_mut())
    }

    /// Supply the Keystore interface, if in use. Then, key retrieval / storage will be done through this interface
    /// rather than writing to disk.
    pub fn provide_keystore_interface(
        &self,
        interface: cxx::UniquePtr<ffi::BluetoothKeystoreInterface>,
    ) {
        self.inner.borrow_mut().as_mut().ProvideKeystoreInterface(interface);
    }
}

/// Convenience wrapper around shimmed storage::Device FFI methods.
pub trait IDevice {
    /// Get the raw address of the Device from storaeg
    fn get_address(&self) -> RawAddress;
    /// Get the DeviceType (BR/EDR, LE, DUAL, or UNKNOWN)
    fn get_device_type(&self) -> DeviceType;
    /// Specialize to a ClassicDevice. Only works when GetDeviceType() returns BR_EDR or DUAL, will crash otherwise
    fn classic(self: Pin<&mut Self>) -> UniquePtr<ClassicDevice>;
    /// Specialize to an LeDevice. Only works when GetDeviceType() returns LE or DUAL, will crash otherwise
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
            #[allow(missing_docs)]
           pub trait  [< I$name >] {
                $(fn [<get_ $field>](&self) -> UniquePtr<CxxString>;)*
                $(fn [<set_ $field>](self: Pin<&mut Self>, value: &str) -> UniquePtr<MutationEntry>;)*
           }

           impl [< I$name >] for ffi::$name {
                #[allow(missing_docs)]
                $(fn [<get_ $field>](&self) -> UniquePtr<CxxString> {
                    ffi::[<Get $field:camel>](self)
                })*

                #[allow(missing_docs)]
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

/// Convenience wrapper around shimmed Mutation FFI methods.
pub trait IMutation {
    /// Add a MutationEntry change to the pending changeset within the Mutation
    fn add(self: Pin<&mut Self>, entry: UniquePtr<MutationEntry>);
}

impl IMutation for Mutation {
    fn add(self: Pin<&mut Self>, entry: UniquePtr<MutationEntry>) {
        ffi::Add(self, entry)
    }
}
