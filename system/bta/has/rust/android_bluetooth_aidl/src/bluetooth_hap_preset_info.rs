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

//! Rust implementation of `BluetoothHapPresetInfo`.

use binder::{
    binder_impl::{BorrowedParcel, UnstructuredParcelable},
    impl_deserialize_for_unstructured_parcelable, impl_serialize_for_unstructured_parcelable,
    StatusCode,
};

/// Rust implementation of `BluetoothHapPresetInfo`.
#[derive(Clone, Debug, Eq, PartialEq)]
pub struct BluetoothHapPresetInfo {
    preset_index: i32,
    preset_name: String,
    is_writable: bool,
    is_available: bool,
}

impl UnstructuredParcelable for BluetoothHapPresetInfo {
    fn write_to_parcel(&self, parcel: &mut BorrowedParcel) -> Result<(), StatusCode> {
        parcel.write(&self.preset_index)?;
        parcel.write(&self.preset_name)?;
        parcel.write(&self.is_writable)?;
        parcel.write(&self.is_available)?;
        Ok(())
    }

    fn from_parcel(parcel: &BorrowedParcel) -> Result<Self, StatusCode> {
        let preset_index = parcel.read()?;
        let preset_name = parcel.read()?;
        let is_writable = parcel.read()?;
        let is_available = parcel.read()?;
        Ok(Self { preset_index, preset_name, is_writable, is_available })
    }
}

impl_deserialize_for_unstructured_parcelable!(BluetoothHapPresetInfo);
impl_serialize_for_unstructured_parcelable!(BluetoothHapPresetInfo);
