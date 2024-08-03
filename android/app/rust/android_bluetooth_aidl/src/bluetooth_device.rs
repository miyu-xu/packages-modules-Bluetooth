// Copyright (C) 2024 The Android Open Source Project
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

//! Rust implementation of `BluetoothDevice`.

use binder::{
    binder_impl::{BorrowedParcel, UnstructuredParcelable},
    impl_deserialize_for_unstructured_parcelable, impl_serialize_for_unstructured_parcelable,
    StatusCode,
};

#[derive(Clone, Debug, Eq, PartialEq)]
enum AddressType {
    Public,
    Random,
    Anonymous,
}
impl AddressType {
    fn write_to_parcel(&self, parcel: &mut BorrowedParcel) -> Result<(), StatusCode> {
        parcel.write(&match self {
            AddressType::Public => 0,
            AddressType::Random => 1,
            AddressType::Anonymous => 0xFF,
        })
    }

    fn from_parcel(parcel: &BorrowedParcel) -> Result<Self, StatusCode> {
        match parcel.read()? {
            0 => Ok(AddressType::Public),
            1 => Ok(AddressType::Random),
            0xFF => Ok(AddressType::Anonymous),
            _ => Err(StatusCode::BAD_VALUE),
        }
    }
}

/// Rust implementation of `BluetoothDevice`.
#[derive(Clone, Debug, Eq, PartialEq)]
pub struct BluetoothDevice {
    address: String, // TODO [u8;6]
    address_type: AddressType,
}

impl UnstructuredParcelable for BluetoothDevice {
    fn write_to_parcel(&self, parcel: &mut BorrowedParcel) -> Result<(), StatusCode> {
        parcel.write(&self.address)?;
        self.address_type.write_to_parcel(parcel)?;
        Ok(())
    }

    fn from_parcel(parcel: &BorrowedParcel) -> Result<Self, StatusCode> {
        let address = parcel.read()?;
        // TODO check address is valid
        let address_type = AddressType::from_parcel(parcel)?;
        Ok(Self { address, address_type })
    }
}

impl_deserialize_for_unstructured_parcelable!(BluetoothDevice);
impl_serialize_for_unstructured_parcelable!(BluetoothDevice);
