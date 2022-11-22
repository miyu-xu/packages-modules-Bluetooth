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

#[allow(dead_code)]
#[allow(unused_must_use)]
#[cxx::bridge]
mod inner {

    #[namespace = "bluetooth::shim"]
    unsafe extern "C++" {
        include!("main/shim/entry.h");

        fn GetStorage() -> *mut StorageModule;
    }

    #[namespace = "bluetooth::storage"]
    unsafe extern "C++" {
        include!("gd/storage/storage_module.h");
        include!("gd/storage/mutation.h");
        include!("gd/storage/mutation_entry.h");
        include!("gd/storage/keystore_interface.h");

        type StorageModule;
        type Mutation;
        type MutationEntry;
        type Device;
        type ClassicDevice;
        type LeDevice;

        /// An interface injected into the StorageModule that lets it proxy key reads/writes
        type BluetoothKeystoreInterface;

        fn Commit(self: Pin<&mut Mutation>);
        fn ProvideKeystoreInterface(
            self: Pin<&mut StorageModule>,
            interface: UniquePtr<BluetoothKeystoreInterface>,
        );
    }

    #[namespace = "bluetooth::storage::rust_shim"]
    #[repr(i32)]
    enum PropertyType {
        NORMAL,
        MEMORY_ONLY,
    }

    #[namespace = "bluetooth::hci"]
    extern "C++" {
        type DeviceType = crate::core::DeviceType;
    }

    #[namespace = "bluetooth::hci::rust_shim"]
    extern "C++" {
        type RawAddress = crate::core::RawAddress;
    }

    #[namespace = "bluetooth::storage::rust_shim"]
    unsafe extern "C++" {
        include!("storage/storage_shim.h");
        type PropertyType;

        fn GetBondedDevices(storage: &StorageModule) -> UniquePtr<CxxVector<Device>>;

        fn GetDeviceType(device: &Device) -> DeviceType;

        fn GetAddress(device: &Device) -> RawAddress;

        fn Classic(device: Pin<&mut Device>) -> UniquePtr<ClassicDevice>;

        fn GetRawLinkKey(device: &ClassicDevice) -> UniquePtr<CxxString>;
        fn SetRawLinkKey(device: Pin<&mut ClassicDevice>, value: &str) -> UniquePtr<MutationEntry>;

        fn Le(device: Pin<&mut Device>) -> UniquePtr<LeDevice>;

        fn GetLocalId(device: &LeDevice) -> UniquePtr<CxxString>;
        fn GetPeerId(device: &LeDevice) -> UniquePtr<CxxString>;
        fn GetLocalEncryptionKeys(device: &LeDevice) -> UniquePtr<CxxString>;
        fn GetPeerEncryptionKeys(device: &LeDevice) -> UniquePtr<CxxString>;
        fn GetLocalSignatureResolvingKeys(device: &LeDevice) -> UniquePtr<CxxString>;
        fn GetPeerSignatureResolvingKeys(device: &LeDevice) -> UniquePtr<CxxString>;

        #[must_use]
        fn SetLocalId(device: Pin<&mut LeDevice>, value: &str) -> UniquePtr<MutationEntry>;
        #[must_use]
        fn SetPeerId(device: Pin<&mut LeDevice>, value: &str) -> UniquePtr<MutationEntry>;
        #[must_use]
        fn SetLocalEncryptionKeys(
            device: Pin<&mut LeDevice>,
            value: &str,
        ) -> UniquePtr<MutationEntry>;
        #[must_use]
        fn SetPeerEncryptionKeys(
            device: Pin<&mut LeDevice>,
            value: &str,
        ) -> UniquePtr<MutationEntry>;
        #[must_use]
        fn SetLocalSignatureResolvingKeys(
            device: Pin<&mut LeDevice>,
            value: &str,
        ) -> UniquePtr<MutationEntry>;
        #[must_use]
        fn SetPeerSignatureResolvingKeys(
            device: Pin<&mut LeDevice>,
            value: &str,
        ) -> UniquePtr<MutationEntry>;

        #[must_use]
        fn ModifyOnHeap(module: Pin<&mut StorageModule>) -> UniquePtr<Mutation>;

        fn Add(mutation: Pin<&mut Mutation>, entry: UniquePtr<MutationEntry>);

        #[must_use]
        fn Set(
            property_type: PropertyType,
            section_param: &str,
            property_param: &str,
            value_param: &str,
        ) -> UniquePtr<MutationEntry>;
    }
}
