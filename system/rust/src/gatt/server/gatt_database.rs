//! This module converts a GattDatastore to an AttDatabase,
//! by converting a registry of services into a list of attributes, and proxying
//! ATT read/write requests into characteristic reads/writes

use std::{
    cell::RefCell,
    collections::{HashMap, HashSet},
    rc::Rc,
};

use async_trait::async_trait;

use crate::{
    gatt::{
        callbacks::GattDatastore,
        ids::{AttHandle, ConnectionId},
    },
    packets::{
        AttAttributeDataChild, AttAttributeDataView, AttCharacteristicDeclarationValueBuilder,
        AttCharacteristicPropertiesBuilder, AttErrorCode, AttServiceDeclarationValueBuilder,
        UuidBuilder,
    },
};

use super::att_database::{
    AttAttribute, AttDatabase, CHARACTERISTIC_UUID, PRIMARY_SERVICE_DECLARATION_UUID,
};

pub use super::att_database::{AttPermissions, AttUuid};

/// A GattService (currently, only primary services are supported) has an identifying
/// UUID and a list of contained characteristics, as well as a handle (indicating the
/// attribute where the service descriptor will live)
#[derive(Debug, Clone)]
pub struct GattServiceWithHandle {
    /// The handle of the service descriptor
    pub handle: AttHandle,
    /// The type of the service
    pub uuid: AttUuid,
    /// A list of contained characteristics (that must have handles between the service
    /// descriptor handle, and that of the next service)
    pub characteristics: Vec<GattCharacteristicWithHandle>,
}

/// A GattCharacteristic consists of a handle (where the value attribute lives), a UUID
/// identifying its type, and permissions indicating what operations can be performed
#[derive(Debug, Clone)]
pub struct GattCharacteristicWithHandle {
    /// The handle of the characteristic value attribute. The characteristic descriptor is
    /// one before this handle.
    pub handle: AttHandle,
    /// The UUID representing the type of the characteristic value.
    pub uuid: AttUuid,
    /// The permissions (read/write) indicate what operations can be performed.
    pub permissions: AttPermissions,
}

/// The GattDatabase implements AttDatabase, and converts attribute reads/writes
/// into GATT operations to be sent to the upper layers
pub struct GattDatabase<T: ?Sized> {
    datastore: Rc<T>,
    static_data: RefCell<GattDatabaseStaticData>,
}

#[derive(Default)]
struct GattDatabaseStaticData {
    services: Vec<GattServiceWithHandle>,
    attributes: Vec<AttAttribute>,
    fixed_attribute_values: HashMap<AttHandle, AttAttributeDataChild>,
}

impl<T: GattDatastore + ?Sized> GattDatabase<T> {
    /// Constructor, wrapping a GattDatastore
    pub fn new(datastore: Rc<T>) -> Self {
        Self { datastore, static_data: Default::default() }
    }

    /// Add a service with pre-allocated handles (for co-existence with C++)
    /// We assume that the characteristic DECLARATION handles are one less than the characteristic handles
    /// Return failure if handles overlap with ones already allocated
    pub fn add_service_with_handles(&self, service: GattServiceWithHandle) -> Result<(), String> {
        let mut fixed_attribute_values = HashMap::new();

        let mut attributes = vec![];
        let mut characteristics = vec![];

        // service definition
        fixed_attribute_values.insert(
            service.handle,
            AttServiceDeclarationValueBuilder { uuid: UuidBuilder::from(service.uuid) }.into(),
        );
        attributes.push(AttAttribute {
            handle: service.handle,
            uuid: PRIMARY_SERVICE_DECLARATION_UUID,
            permissions: AttPermissions { readable: true, writable: false },
        });

        // characteristics
        for characteristic in service.characteristics {
            characteristics.push(GattCharacteristicWithHandle {
                handle: characteristic.handle,
                uuid: characteristic.uuid,
                permissions: characteristic.permissions.clone(),
            });

            // declaration
            let declaration_handle = AttHandle(characteristic.handle.0 - 1);
            fixed_attribute_values.insert(
                declaration_handle,
                AttCharacteristicDeclarationValueBuilder {
                    properties: AttCharacteristicPropertiesBuilder {
                        broadcast: 0,
                        read: characteristic.permissions.readable.into(),
                        write_without_response: 0,
                        write: characteristic.permissions.writable.into(),
                        notify: 0,
                        indicate: 0,
                        authenticated_signed_writes: 0,
                        extended_properties: 0,
                    },
                    handle: characteristic.handle.into(),
                    uuid: characteristic.uuid.into(),
                }
                .into(),
            );
            attributes.push(AttAttribute {
                handle: declaration_handle,
                uuid: CHARACTERISTIC_UUID,
                permissions: AttPermissions { readable: true, writable: false },
            });

            // value
            attributes.push(AttAttribute {
                handle: characteristic.handle,
                uuid: characteristic.uuid,
                permissions: AttPermissions { readable: true, writable: false },
            });
        }

        // validate attributes for overlap
        let mut static_data = self.static_data.borrow_mut();

        let existing_handles = static_data
            .attributes
            .iter()
            .map(|AttAttribute { handle, .. }| *handle)
            .collect::<HashSet<_>>();
        for AttAttribute { handle, .. } in &attributes {
            if existing_handles.contains(handle) {
                return Err("duplicate handle detected".to_string());
            }
        }

        // if we made it here, we successfully loaded the new service
        let service =
            GattServiceWithHandle { handle: service.handle, uuid: service.uuid, characteristics };
        static_data.services.push(service);
        static_data.attributes.extend(attributes.into_iter());
        static_data.fixed_attribute_values.extend(fixed_attribute_values.into_iter());

        Ok(())
    }

    /// Generate an impl AttDatabase from a backing GattDatabase, associated with a given
    /// connection.
    pub fn get_att_database(self: &Rc<Self>, conn_id: ConnectionId) -> AttDatabaseImpl<T> {
        AttDatabaseImpl { gatt_db: self.clone(), conn_id }
    }
}

/// An implementation of AttDatabase wrapping an underlying GattDatabase
pub struct AttDatabaseImpl<T: ?Sized> {
    gatt_db: Rc<GattDatabase<T>>,
    conn_id: ConnectionId,
}

#[async_trait(?Send)]
impl<T> AttDatabase for AttDatabaseImpl<T>
where
    T: GattDatastore + ?Sized,
{
    async fn read_attribute(
        &self,
        handle: AttHandle,
    ) -> Result<AttAttributeDataChild, AttErrorCode> {
        {
            let services = self.gatt_db.static_data.borrow_mut();
            if let Some(fixed_value) = services.fixed_attribute_values.get(&handle) {
                return Ok(fixed_value.clone());
            }
        }

        self.gatt_db.datastore.read_characteristic(self.conn_id, handle).await
    }

    async fn write_attribute(
        &self,
        handle: AttHandle,
        data: AttAttributeDataView<'_>,
    ) -> Result<(), AttErrorCode> {
        self.gatt_db.datastore.write_characteristic(self.conn_id, handle, data).await
    }

    fn list_attributes(&self) -> Vec<AttAttribute> {
        self.gatt_db.static_data.borrow().attributes.to_owned()
    }
}
