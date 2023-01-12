use std::{
    cell::RefCell,
    collections::{HashMap, HashSet},
    rc::Rc,
};

use async_trait::async_trait;

use crate::{
    gatt::ids::{AttHandle, ConnectionId},
    packets::{
        AttAttributeDataChild, AttCharacteristicDeclarationValueBuilder,
        AttCharacteristicPropertiesBuilder, AttErrorCode, AttServiceDeclarationValueBuilder,
        UuidBuilder,
    },
};

use super::att_database::{
    AttAttribute, AttDatabase, CHARACTERISTIC_UUID, PRIMARY_SERVICE_DECLARATION_UUID,
};

pub use super::att_database::{AttPermissions, AttUuid};

#[derive(Debug, Clone)]
pub struct GattService {
    pub uuid: AttUuid,
    pub characteristics: Vec<GattCharacteristic>,
}

#[derive(Debug, Clone)]
pub struct GattServiceWithHandle {
    pub handle: AttHandle,
    pub uuid: AttUuid,
    pub characteristics: Vec<GattCharacteristicWithHandle>,
}

#[derive(Debug, Clone)]
pub struct GattCharacteristic {
    pub uuid: AttUuid,
    pub permissions: AttPermissions,
}

#[derive(Debug, Clone)]
pub struct GattCharacteristicWithHandle {
    pub handle: AttHandle,
    pub uuid: AttUuid,
    pub permissions: AttPermissions,
}

#[async_trait(?Send)]
pub trait GattDatastore {
    async fn read_characteristic(
        &self,
        conn_id: ConnectionId,
        handle: AttHandle,
    ) -> Result<AttAttributeDataChild, AttErrorCode>;
    async fn write_characteristic(
        &self,
        conn_id: ConnectionId,
        handle: AttHandle,
        data: &[u8],
    ) -> Result<(), AttErrorCode>;
}

pub struct GattDatabase<T: GattDatastore> {
    datastore: T,
    static_data: RefCell<GattDatabaseStaticData>,
}

#[derive(Default)]
struct GattDatabaseStaticData {
    services: Vec<GattServiceWithHandle>,
    attributes: Vec<AttAttribute>,
    fixed_attribute_values: HashMap<AttHandle, AttAttributeDataChild>,
}

impl<T: GattDatastore> GattDatabase<T> {
    pub fn new(datastore: T) -> Self {
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

    pub fn get_att_database(self: &Rc<Self>, conn_id: ConnectionId) -> AttDatabaseImpl<T> {
        AttDatabaseImpl { gatt_db: self.clone(), conn_id }
    }
}

pub struct AttDatabaseImpl<T: GattDatastore> {
    gatt_db: Rc<GattDatabase<T>>,
    conn_id: ConnectionId,
}

#[async_trait(?Send)]
impl<T: GattDatastore> AttDatabase for AttDatabaseImpl<T> {
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

    async fn write_attribute(&self, handle: AttHandle, data: &[u8]) -> Result<(), AttErrorCode> {
        self.gatt_db.datastore.write_characteristic(self.conn_id, handle, data).await
    }

    fn list_attributes(&self) -> Vec<AttAttribute> {
        self.gatt_db.static_data.borrow().attributes.to_owned()
    }
}
