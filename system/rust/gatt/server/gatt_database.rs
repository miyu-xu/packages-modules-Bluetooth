use std::{
    cell::RefCell,
    collections::{HashMap, LinkedList},
};

use async_trait::async_trait;

use crate::packets::{
    AttAttributeDataChild, AttCharacteristicDeclarationValueBuilder,
    AttCharacteristicPropertiesBuilder, AttServiceDeclarationValueBuilder, UuidBuilder,
};

use super::att_database::{
    AttAttribute, AttDatabase, CHARACTERISTIC_UUID, PRIMARY_SERVICE_DECLARATION_UUID,
};

pub use super::att_database::{AttHandle, AttPermissions, AttUuid};

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
    async fn read_characteristic(&self, handle: AttHandle)
        -> Result<AttAttributeDataChild, String>;
    async fn write_characteristic(&self, handle: AttHandle, data: &[u8]) -> Result<(), String>;
}

struct HandleRange {
    // low_: u16,
    high: u16,
}

struct Counter {
    curr: u16,
}

impl Counter {
    fn next(&mut self) -> Result<AttHandle, String> {
        if self.curr == u16::MAX {
            return Err("out of handles".to_string());
        }
        self.curr += 1;
        Ok(AttHandle(self.curr))
    }
}

pub struct GattDatabase<T: GattDatastore> {
    datastore: T,
    services: RefCell<GattDatabaseStaticData>,
}

#[derive(Default)]
struct GattDatabaseStaticData {
    services: Vec<GattServiceWithHandle>,
    attributes: Vec<AttAttribute>,
    allocated_handle_ranges: LinkedList<HandleRange>,
    fixed_attribute_values: HashMap<AttHandle, AttAttributeDataChild>,
}

impl<T: GattDatastore> GattDatabase<T> {
    pub fn new(datastore: T) -> Self {
        Self { datastore, services: Default::default() }
    }

    pub fn add_service(&self, service: GattService) -> Result<GattServiceWithHandle, String> {
        let mut fixed_attribute_values = HashMap::new();

        let mut attributes = vec![];
        let mut characteristics = vec![];
        let start_handle =
            self.services.borrow().allocated_handle_ranges.back().map(|x| x.high + 1).unwrap_or(1);
        let mut counter = Counter { curr: start_handle };

        // service definition
        let service_handle = counter.next()?;
        fixed_attribute_values.insert(
            service_handle,
            AttServiceDeclarationValueBuilder { uuid: UuidBuilder::from(service.uuid) }.into(),
        );
        attributes.push(AttAttribute {
            handle: service_handle,
            uuid: PRIMARY_SERVICE_DECLARATION_UUID,
            permissions: AttPermissions { readable: true, writable: false },
        });

        // includes (TODO)

        // characteristics
        for characteristic in service.characteristics {
            let declaration_handle = counter.next()?;
            let value_handle = counter.next()?;

            characteristics.push(GattCharacteristicWithHandle {
                handle: value_handle,
                uuid: characteristic.uuid,
                permissions: characteristic.permissions.clone(),
            });

            // declaration
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
                    handle: value_handle.into(),
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
                handle: value_handle,
                uuid: characteristic.uuid,
                permissions: AttPermissions { readable: true, writable: false },
            });
        }

        // if we made it here, we successfully loaded the new service
        let mut services = self.services.borrow_mut();
        let service =
            GattServiceWithHandle { handle: service_handle, uuid: service.uuid, characteristics };
        services.services.push(service.clone());
        services.attributes.extend(attributes.into_iter());
        services
            .allocated_handle_ranges
            .push_back(HandleRange { /*low: start_handle,*/ high: counter.curr });
        services.fixed_attribute_values.extend(fixed_attribute_values.into_iter());

        Ok(service)
    }
}

#[async_trait(?Send)]
impl<T: GattDatastore> AttDatabase for GattDatabase<T> {
    async fn read_attribute(&self, handle: AttHandle) -> Result<AttAttributeDataChild, String> {
        {
            let services = self.services.borrow_mut();
            if let Some(fixed_value) = services.fixed_attribute_values.get(&handle) {
                return Ok(fixed_value.clone());
            }
        }

        self.datastore.read_characteristic(handle).await
    }

    async fn write_attribute(&self, _handle: AttHandle, _data: &[u8]) -> Result<(), String> {
        todo!()
    }

    fn list_attributes(&self) -> Vec<AttAttribute> {
        self.services.borrow().attributes.to_owned()
    }
}
