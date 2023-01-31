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
        AttAttributeDataChild, AttCharacteristicDeclarationValueBuilder,
        AttCharacteristicPropertiesBuilder, AttErrorCode, AttServiceDeclarationValueBuilder,
        UuidBuilder,
    },
};

use super::att_database::{
    AttAttribute, AttDatabase, CHARACTERISTIC_UUID, PRIMARY_SERVICE_DECLARATION_UUID,
};

pub use super::att_database::{AttPermissions, Uuid};

/// A GattService (currently, only primary services are supported) has an
/// identifying UUID and a list of contained characteristics, as well as a
/// handle (indicating the attribute where the service descriptor will live)
#[derive(Debug, Clone)]
pub struct GattServiceWithHandle {
    /// The handle of the service descriptor
    pub handle: AttHandle,
    /// The type of the service
    pub uuid: Uuid,
    /// A list of contained characteristics (that must have handles between the
    /// service descriptor handle, and that of the next service)
    pub characteristics: Vec<GattCharacteristicWithHandle>,
}

/// A GattCharacteristic consists of a handle (where the value attribute lives),
/// a UUID identifying its type, and permissions indicating what operations can
/// be performed
#[derive(Debug, Clone)]
pub struct GattCharacteristicWithHandle {
    /// The handle of the characteristic value attribute. The characteristic
    /// descriptor is one before this handle.
    pub handle: AttHandle,
    /// The UUID representing the type of the characteristic value.
    pub uuid: Uuid,
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
    /// Assumes that the characteristic DECLARATION handles are one less than
    /// the characteristic handles Return failure if handles overlap with
    /// ones already allocated
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
                permissions: characteristic.permissions,
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
        let unique_attributes = attributes.iter().map(|attr| attr.handle).collect::<HashSet<_>>();
        if unique_attributes.len() != attributes.len() {
            return Err("duplicate handle detected".to_string());
        }

        // if we made it here, we successfully loaded the new service
        let service =
            GattServiceWithHandle { handle: service.handle, uuid: service.uuid, characteristics };
        static_data.services.push(service);
        static_data.attributes.extend(attributes.into_iter());
        static_data.attributes.sort_by_key(|attr| attr.handle);
        static_data.fixed_attribute_values.extend(fixed_attribute_values.into_iter());

        Ok(())
    }

    /// Remove a previously-added service by service handle
    pub fn remove_service_at_handle(&self, handle: AttHandle) -> Result<(), String> {
        let mut static_data = self.static_data.borrow_mut();

        // remove old service
        let old_service_i = static_data
            .services
            .iter()
            .enumerate()
            .find(|(_, service)| service.handle == handle)
            .map(|(i, _)| i);
        if let Some(old_service_i) = old_service_i {
            static_data.services.remove(old_service_i);
        } else {
            return Err(format!("service at handle {handle:?} not found, cannot remove"));
        }

        // find next service
        let mut next_service = None;
        for attr in &static_data.attributes {
            if attr.handle <= handle {
                continue;
            }
            if attr.uuid == PRIMARY_SERVICE_DECLARATION_UUID {
                next_service = Some(attr.handle);
                break;
            }
        }

        // clear out attributes
        let in_old_service = |curr_handle| {
            handle <= curr_handle && next_service.map(|x| curr_handle < x).unwrap_or(true)
        };
        static_data.fixed_attribute_values.retain(|curr_handle, _| !in_old_service(*curr_handle));
        static_data.attributes.retain(|attr| !in_old_service(attr.handle));

        Ok(())
    }

    /// Generate an impl AttDatabase from a backing GattDatabase, associated
    /// with a given connection.
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

    fn list_attributes(&self) -> Vec<AttAttribute> {
        self.gatt_db.static_data.borrow().attributes.to_owned()
    }
}

#[cfg(test)]
mod test {
    use crate::gatt::mocks::mock_datastore::MockDatastore;

    use super::*;

    const SERVICE_HANDLE: AttHandle = AttHandle(1);
    const SERVICE_TYPE: Uuid = Uuid::new([1, 2, 3, 4]);

    const CHARACTERISTIC_DECLARATION_HANDLE: AttHandle = AttHandle(2);
    const CHARACTERISTIC_VALUE_HANDLE: AttHandle = AttHandle(3);
    const CHARACTERISTIC_TYPE: Uuid = Uuid::new([5, 6, 7, 8]);

    #[test]
    fn test_read_empty_db() {
        let (gatt_datastore, _) = MockDatastore::new();
        let gatt_db = Rc::new(GattDatabase::new(gatt_datastore.into()));
        let att_db = gatt_db.get_att_database(ConnectionId(1));

        let resp = tokio_test::block_on(att_db.read_attribute(AttHandle(1)));

        assert_eq!(resp, Err(AttErrorCode::INVALID_HANDLE))
    }

    #[test]
    fn test_single_service() {
        let (gatt_datastore, _) = MockDatastore::new();
        let gatt_db = Rc::new(GattDatabase::new(gatt_datastore.into()));
        gatt_db
            .add_service_with_handles(GattServiceWithHandle {
                handle: SERVICE_HANDLE,
                uuid: SERVICE_TYPE,
                characteristics: vec![],
            })
            .unwrap();
        let att_db = gatt_db.get_att_database(ConnectionId(1));

        let attrs = att_db.list_attributes();
        let service_value = tokio_test::block_on(att_db.read_attribute(SERVICE_HANDLE));

        assert_eq!(
            attrs,
            vec![AttAttribute {
                handle: SERVICE_HANDLE,
                uuid: PRIMARY_SERVICE_DECLARATION_UUID,
                permissions: AttPermissions { readable: true, writable: false }
            }]
        );
        assert_eq!(
            service_value,
            Ok(AttAttributeDataChild::AttServiceDeclarationValue(
                AttServiceDeclarationValueBuilder { uuid: SERVICE_TYPE.into() }
            ))
        );
    }

    #[test]
    fn test_service_removal() {
        // arrange three services, each with a single characteristic
        let (gatt_datastore, _) = MockDatastore::new();
        let gatt_db = Rc::new(GattDatabase::new(gatt_datastore.into()));

        gatt_db
            .add_service_with_handles(GattServiceWithHandle {
                handle: AttHandle(1),
                uuid: SERVICE_TYPE,
                characteristics: vec![GattCharacteristicWithHandle {
                    handle: AttHandle(3),
                    uuid: CHARACTERISTIC_TYPE,
                    permissions: AttPermissions { readable: true, writable: false },
                }],
            })
            .unwrap();
        gatt_db
            .add_service_with_handles(GattServiceWithHandle {
                handle: AttHandle(4),
                uuid: SERVICE_TYPE,
                characteristics: vec![GattCharacteristicWithHandle {
                    handle: AttHandle(6),
                    uuid: CHARACTERISTIC_TYPE,
                    permissions: AttPermissions { readable: true, writable: false },
                }],
            })
            .unwrap();
        gatt_db
            .add_service_with_handles(GattServiceWithHandle {
                handle: AttHandle(7),
                uuid: SERVICE_TYPE,
                characteristics: vec![GattCharacteristicWithHandle {
                    handle: AttHandle(9),
                    uuid: CHARACTERISTIC_TYPE,
                    permissions: AttPermissions { readable: true, writable: false },
                }],
            })
            .unwrap();
        let att_db = gatt_db.get_att_database(ConnectionId(1));
        assert_eq!(att_db.list_attributes().len(), 9);

        // act: remove the middle service
        gatt_db.remove_service_at_handle(AttHandle(4)).unwrap();
        let attrs = att_db.list_attributes();

        // assert that the middle service is gone
        assert_eq!(attrs.len(), 6, "{attrs:?}");

        // assert the other two old services are still there
        assert_eq!(
            attrs[0],
            AttAttribute {
                handle: AttHandle(1),
                uuid: PRIMARY_SERVICE_DECLARATION_UUID,
                permissions: AttPermissions { readable: true, writable: false }
            }
        );
        assert_eq!(
            attrs[3],
            AttAttribute {
                handle: AttHandle(7),
                uuid: PRIMARY_SERVICE_DECLARATION_UUID,
                permissions: AttPermissions { readable: true, writable: false }
            }
        );
    }

    #[test]
    fn test_single_characteristic() {
        let (gatt_datastore, _) = MockDatastore::new();
        let gatt_db = Rc::new(GattDatabase::new(gatt_datastore.into()));
        gatt_db
            .add_service_with_handles(GattServiceWithHandle {
                handle: SERVICE_HANDLE,
                uuid: SERVICE_TYPE,
                characteristics: vec![GattCharacteristicWithHandle {
                    handle: CHARACTERISTIC_VALUE_HANDLE,
                    uuid: CHARACTERISTIC_TYPE,
                    permissions: AttPermissions { readable: false, writable: true },
                }],
            })
            .unwrap();
        let att_db = gatt_db.get_att_database(ConnectionId(1));

        let attrs = att_db.list_attributes();
        let characteristic_decl =
            tokio_test::block_on(att_db.read_attribute(CHARACTERISTIC_DECLARATION_HANDLE));
        let characteristic_value =
            tokio_test::block_on(att_db.read_attribute(CHARACTERISTIC_VALUE_HANDLE));

        assert_eq!(attrs.len(), 3);
        assert_eq!(attrs[0].uuid, PRIMARY_SERVICE_DECLARATION_UUID);
        assert_eq!(
            attrs[1],
            AttAttribute {
                handle: CHARACTERISTIC_DECLARATION_HANDLE,
                uuid: CHARACTERISTIC_UUID,
                permissions: AttPermissions { readable: true, writable: false }
            }
        );
        assert_eq!(
            attrs[2],
            AttAttribute {
                handle: CHARACTERISTIC_VALUE_HANDLE,
                uuid: CHARACTERISTIC_TYPE,
                permissions: AttPermissions { readable: false, writable: true }
            }
        );

        assert_eq!(
            characteristic_decl,
            Ok(AttAttributeDataChild::AttCharacteristicDeclarationValue(
                AttCharacteristicDeclarationValueBuilder {
                    properties: AttCharacteristicPropertiesBuilder {
                        read: 0,
                        broadcast: 0,
                        write_without_response: 0,
                        write: 1,
                        notify: 0,
                        indicate: 0,
                        authenticated_signed_writes: 0,
                        extended_properties: 0,
                    },
                    handle: CHARACTERISTIC_VALUE_HANDLE.into(),
                    uuid: CHARACTERISTIC_TYPE.into()
                }
            ))
        );
        // TODO(aryarahul): fix this once attribute value reading works
        assert_eq!(characteristic_value, Err(AttErrorCode::INVALID_HANDLE));
    }

    #[test]
    fn test_handle_clash() {
        let (gatt_datastore, _) = MockDatastore::new();
        let gatt_db = Rc::new(GattDatabase::new(gatt_datastore.into()));

        let result = gatt_db.add_service_with_handles(GattServiceWithHandle {
            handle: SERVICE_HANDLE,
            uuid: SERVICE_TYPE,
            characteristics: vec![GattCharacteristicWithHandle {
                handle: SERVICE_HANDLE,
                uuid: CHARACTERISTIC_TYPE,
                permissions: AttPermissions { readable: false, writable: true },
            }],
        });

        assert!(result.is_err());
    }

    #[test]
    fn test_handle_clash_with_existing() {
        let (gatt_datastore, _) = MockDatastore::new();
        let gatt_db = Rc::new(GattDatabase::new(gatt_datastore.into()));

        gatt_db
            .add_service_with_handles(GattServiceWithHandle {
                handle: SERVICE_HANDLE,
                uuid: SERVICE_TYPE,
                characteristics: vec![],
            })
            .unwrap();

        let result = gatt_db.add_service_with_handles(GattServiceWithHandle {
            handle: SERVICE_HANDLE,
            uuid: SERVICE_TYPE,
            characteristics: vec![],
        });

        assert!(result.is_err());
    }
}
