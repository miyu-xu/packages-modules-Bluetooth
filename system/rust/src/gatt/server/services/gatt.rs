//! The GATT service as defined in Core Spec 5.3 Vol 3G Section 7

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
            AttPermissions, GattCharacteristicWithHandle, GattDatabase, GattDescriptorWithHandle,
            GattServiceWithHandle,
        },
    },
    packets::{
        AttAttributeDataChild, AttAttributeDataView, AttClientCharacteristicConfigurationBuilder,
        AttErrorCode,
    },
};

struct GattService;

// Must lie in the range specified by GATT_GATT_START_HANDLE from legacy stack
const GATT_SERVICE_HANDLE: AttHandle = AttHandle(1);
const SERVICE_CHANGE_HANDLE: AttHandle = AttHandle(3);
const SERVICE_CHANGE_CCC_DESCRIPTOR_HANDLE: AttHandle = AttHandle(4);

#[async_trait(?Send)]
impl GattDatastore for GattService {
    async fn read(
        &self,
        _: TransportIndex,
        handle: AttHandle,
        _: AttributeBackingType,
    ) -> Result<AttAttributeDataChild, AttErrorCode> {
        if handle == SERVICE_CHANGE_CCC_DESCRIPTOR_HANDLE {
            Ok(AttClientCharacteristicConfigurationBuilder { notification: 0, indication: 0 }
                .into())
        } else {
            unreachable!()
        }
    }

    async fn write(
        &self,
        _: TransportIndex,
        handle: AttHandle,
        _: AttributeBackingType,
        _: AttAttributeDataView<'_>,
    ) -> Result<(), AttErrorCode> {
        if handle == SERVICE_CHANGE_CCC_DESCRIPTOR_HANDLE {
            Ok(())
        } else {
            unreachable!()
        }
    }
}

/// Register the GATT service in the provided GATT database.
pub fn register_gatt_service(database: &mut GattDatabase) -> Result<()> {
    database.add_service_with_handles(
        // GATT Service
        &GattServiceWithHandle {
            handle: GATT_SERVICE_HANDLE,
            type_: Uuid::new(0x1801),
            // Service Changed Characteristic
            characteristics: vec![GattCharacteristicWithHandle {
                handle: SERVICE_CHANGE_HANDLE,
                type_: Uuid::new(0x2A05),
                permissions: AttPermissions::INDICATE,
                descriptors: vec![GattDescriptorWithHandle {
                    handle: SERVICE_CHANGE_CCC_DESCRIPTOR_HANDLE,
                    type_: Uuid::new(0x2902),
                    permissions: AttPermissions::READABLE | AttPermissions::WRITABLE,
                }],
            }],
        },
        Rc::new(GattService),
    )
}
