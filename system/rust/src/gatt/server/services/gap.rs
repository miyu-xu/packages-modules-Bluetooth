//! The GAP service as defined in Core Spec 5.3 Vol 3C Section 12

use std::rc::Rc;

use anyhow::Result;
use async_trait::async_trait;

use crate::{
    core::uuid::Uuid,
    gatt::{
        callbacks::GattDatastore,
        ffi::AttributeBackingType,
        ids::{AttHandle, TransportIndex},
        server::gatt_database::{
            AttPermissions, GattCharacteristicWithHandle, GattDatabase, GattServiceWithHandle,
        },
    },
    packets::{AttAttributeDataChild, AttAttributeDataView, AttErrorCode},
};

struct GapService;

// Must lie in the range specified by GATT_GAP_START_HANDLE from legacy stack
const GAP_SERVICE_HANDLE: AttHandle = AttHandle(20);
const DEVICE_NAME_HANDLE: AttHandle = AttHandle(22);
const DEVICE_APPEARANCE_HANDLE: AttHandle = AttHandle(24);

#[async_trait(?Send)]
impl GattDatastore for GapService {
    async fn read(
        &self,
        _: TransportIndex,
        handle: AttHandle,
        _: AttributeBackingType,
    ) -> Result<AttAttributeDataChild, AttErrorCode> {
        // TODO(aryarahul): figure out the correct values to use here
        match handle {
            DEVICE_NAME_HANDLE => {
                Ok(AttAttributeDataChild::RawData("Android Phone".as_bytes().into()))
            }
            // 0x0000 from AssignedNumbers => "Unknown"
            DEVICE_APPEARANCE_HANDLE => Ok(AttAttributeDataChild::RawData([0x00, 0x00].into())),
            _ => Err(AttErrorCode::INVALID_HANDLE),
        }
    }

    async fn write(
        &self,
        _: TransportIndex,
        _: AttHandle,
        _: AttributeBackingType,
        _: AttAttributeDataView<'_>,
    ) -> Result<(), AttErrorCode> {
        unreachable!("no GAP data should be writable")
    }
}

/// Register the GAP service in the provided GATT database.
pub fn register_gap_service(database: &mut GattDatabase) -> Result<()> {
    database.add_service_with_handles(
        // GAP Service
        &GattServiceWithHandle {
            handle: GAP_SERVICE_HANDLE,
            type_: Uuid::new(0x1800),
            // Device Name
            characteristics: vec![
                GattCharacteristicWithHandle {
                    handle: DEVICE_NAME_HANDLE,
                    type_: Uuid::new(0x2A00),
                    permissions: AttPermissions::READABLE,
                    descriptors: vec![],
                },
                // Appearance
                GattCharacteristicWithHandle {
                    handle: DEVICE_APPEARANCE_HANDLE,
                    type_: Uuid::new(0x2A01),
                    permissions: AttPermissions::READABLE,
                    descriptors: vec![],
                },
            ],
        },
        Rc::new(GapService),
    )
}
