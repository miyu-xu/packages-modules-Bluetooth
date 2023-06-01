//! This module manages all GATT databases - specifically, the global database,
//! as well as per-connection databases.

use std::{
    collections::HashMap,
    sync::{Arc, Mutex, MutexGuard},
};

use anyhow::{bail, Result};
use bt_common::init_flags::always_use_private_gatt_for_debugging_is_enabled;

use crate::{
    core::shared_box::SharedBox,
    gatt::ids::{AdvertiserId, ServerId, TransportIndex},
};

use super::{
    gatt_database::GattDatabase, isolation_manager::IsolationManager,
    services::register_builtin_services,
};

pub struct GattDatabaseManager {
    global_database: SharedBox<GattDatabase>,
    single_server_databases: HashMap<ServerId, SharedBox<GattDatabase>>,
    // NOTE: this is logically owned by the GattDatabaseManager. We share it behind a Mutex just so we
    // can use it as part of the Arbiter. Once the Arbiter is removed, this should be owned
    // fully by the GattDatabaseManager.
    //
    // Invariant: all associated servers must be opened when their association is created.
    isolation_manager: Arc<Mutex<IsolationManager>>,
}

fn new_database_with_services() -> GattDatabase {
    let mut db = GattDatabase::new();
    register_builtin_services(&mut db)
        .expect("builtin services should never fail to register on an empty db");
    db
}

impl GattDatabaseManager {
    /// Constructor
    pub fn new(isolation_manager: Arc<Mutex<IsolationManager>>) -> Self {
        Self {
            global_database: new_database_with_services().into(),
            single_server_databases: HashMap::new(),
            isolation_manager,
        }
    }

    /// Open a GATT server
    pub(super) fn open_gatt_server(&mut self, server_id: ServerId) -> Result<()> {
        let old =
            self.single_server_databases.insert(server_id, new_database_with_services().into());
        if old.is_some() {
            bail!("GATT server {server_id:?} already exists but was re-opened, clobbering old value...")
        }
        Ok(())
    }

    /// Close a GATT server
    pub(super) fn close_gatt_server(&mut self, server_id: ServerId) -> Result<()> {
        let old = self.single_server_databases.remove(&server_id);
        if old.is_none() {
            bail!("GATT server {server_id:?} did not exist")
        };

        if !always_use_private_gatt_for_debugging_is_enabled() {
            self.get_isolation_manager().clear_server(server_id);
        }

        Ok(())
    }

    /// Handle incoming connection and return associated database
    pub(super) fn on_le_connect(
        &self,
        tcb_idx: TransportIndex,
        advertiser: Option<AdvertiserId>,
    ) -> &SharedBox<GattDatabase> {
        let mut isolation_manager = self.get_isolation_manager();
        let server_id = isolation_manager.on_le_connect(tcb_idx, advertiser);

        if let Some(server_id) = server_id {
            if let Some(server) = self.single_server_databases.get(&server_id) {
                return server;
            }
            // This should be unreachable, since we enforce that servers can only be associated with an advertiser
            // if they are opened at the time of association, and we clear all associations when the server
            // is closed
            unreachable!("Server is currently associated with advertisement from isolation manager, but does not exist")
        }
        &self.global_database
    }

    /// Handle disconnection
    pub(super) fn on_le_disconnect(&self, tcb_idx: TransportIndex) {
        self.get_isolation_manager().on_le_disconnect(tcb_idx)
    }

    /// Apply a function on all databases, reporting an error if it fails on a database
    /// without continuing.
    pub(super) fn for_each_database(
        &self,
        server_id: ServerId,
        mut f: impl FnMut(&SharedBox<GattDatabase>) -> Result<()>,
    ) -> Result<()> {
        if !self.single_server_databases.contains_key(&server_id) {
            bail!("server {server_id:?} is not opened")
        }
        f(&self.global_database)?;
        self.single_server_databases.get(&server_id).map(f).transpose()?;
        Ok(())
    }

    /// Link a given GATT server to an LE advertising set, so incoming
    /// connections to this advertiser will be visible only by the linked
    /// server
    pub fn associate_server_with_advertiser(
        &mut self,
        server_id: ServerId,
        advertiser_id: AdvertiserId,
    ) -> Result<()> {
        if !self.single_server_databases.contains_key(&server_id) {
            bail!("Cannot associate inactive server with advertiser");
        }
        self.get_isolation_manager().associate_server_with_advertiser(server_id, advertiser_id)
    }

    /// Unlink all servers associated with the specified advertiser
    pub fn remove_servers_tied_to_advertiser(&mut self, advertiser_id: AdvertiserId) {
        self.get_isolation_manager().clear_advertiser(advertiser_id)
    }

    /// Get the IsolationManager to manage associations between servers + advertisers
    fn get_isolation_manager(&self) -> MutexGuard<'_, IsolationManager> {
        self.isolation_manager.lock().unwrap()
    }
}

#[cfg(test)]
mod test {
    use std::rc::Rc;

    use crate::{
        core::uuid::Uuid,
        gatt::{
            ids::{AdvertiserId, AttHandle},
            mocks::mock_datastore::MockDatastore,
            server::{att_database::AttDatabase, gatt_database::GattServiceWithHandle},
        },
        utils::task::block_on_locally,
    };

    use super::*;

    const SERVER_ID: ServerId = ServerId(1);
    const TCB_IDX: TransportIndex = TransportIndex(2);
    const ADVERTISER_ID: AdvertiserId = AdvertiserId(3);
    const SERVICE_HANDLE: AttHandle = AttHandle(10);

    const ANOTHER_SERVER_ID: ServerId = ServerId(5);
    const ANOTHER_TCB_IDX: TransportIndex = TransportIndex(6);

    const SERVICE_UUID: Uuid = Uuid::new(1);

    fn new_db_manager() -> GattDatabaseManager {
        GattDatabaseManager::new(Mutex::new(IsolationManager::new()).into())
    }

    #[test]
    fn test_initialized_global_server() {
        // arrange
        let db_manager = new_db_manager();
        // arrange: connect with no associated isolated server
        let global_db = db_manager.on_le_connect(TCB_IDX, None);

        // act: get the attributes available on this connection
        let global_attrs = global_db.get_att_database(TCB_IDX);
        let is_initialized = !global_attrs.list_attributes().is_empty();

        // assert: that some attributes are available (so the db is initialized)
        assert!(is_initialized);
    }

    #[test]
    fn test_initialized_isolated_server() {
        // arrange
        let mut db_manager = new_db_manager();
        // arrange: associate a server with a given advertiser
        db_manager.open_gatt_server(SERVER_ID).unwrap();
        db_manager
            .get_isolation_manager()
            .associate_server_with_advertiser(SERVER_ID, ADVERTISER_ID)
            .unwrap();
        // arrange: connect to this advertiser
        let isolated_db = db_manager.on_le_connect(TCB_IDX, Some(ADVERTISER_ID));

        // act: get the attributes available on this connection (which should correspond to the isolated db)
        let isolated_attrs = isolated_db.get_att_database(TCB_IDX);
        let is_initialized = !isolated_attrs.list_attributes().is_empty();

        // assert: that some attributes are available (so the db is initialized)
        assert!(is_initialized);
    }

    #[test]
    fn test_server_isolation() {
        // arrange
        let mut db_manager = new_db_manager();
        // arrange: add two GATT servers
        db_manager.open_gatt_server(SERVER_ID).unwrap();
        db_manager.open_gatt_server(ANOTHER_SERVER_ID).unwrap();
        // arrange: associate one with a given advertiser
        db_manager
            .get_isolation_manager()
            .associate_server_with_advertiser(SERVER_ID, ADVERTISER_ID)
            .unwrap();
        // arrange: add service to the non-isolated server
        db_manager
            .for_each_database(ANOTHER_SERVER_ID, |db| {
                db.add_service_with_handles(
                    GattServiceWithHandle {
                        handle: SERVICE_HANDLE,
                        type_: SERVICE_UUID,
                        characteristics: vec![],
                    },
                    Rc::new(MockDatastore::new().0),
                )
            })
            .unwrap();
        // arrange: connect to this advertiser
        let isolated_db = db_manager.on_le_connect(TCB_IDX, Some(ADVERTISER_ID));
        // arrange: connect as initiator
        let global_db = db_manager.on_le_connect(ANOTHER_TCB_IDX, None);

        // act: get the attributes available on the isolated connection
        let num_isolated_attrs = isolated_db.get_att_database(TCB_IDX).list_attributes().len();
        // act: get the attributes available on the global database
        let num_global_attrs = global_db.get_att_database(ANOTHER_TCB_IDX).list_attributes().len();

        // assert: that the number of attributes differs
        assert!(num_global_attrs > num_isolated_attrs);
    }

    #[test]
    fn test_all_databases_modified() {
        // arrange
        let mut db_manager = new_db_manager();
        // arrange: add a GATT server
        db_manager.open_gatt_server(SERVER_ID).unwrap();
        // arrange: associate it with a given advertiser
        db_manager
            .get_isolation_manager()
            .associate_server_with_advertiser(SERVER_ID, ADVERTISER_ID)
            .unwrap();
        // arrange: connect to this advertiser
        let isolated_db = db_manager.on_le_connect(TCB_IDX, Some(ADVERTISER_ID));
        // arrange: connect as initiator
        let global_db = db_manager.on_le_connect(ANOTHER_TCB_IDX, None);
        // arrange: get the attributes available on the isolated connection
        let original_num_isolated_attrs =
            isolated_db.get_att_database(TCB_IDX).list_attributes().len();
        // arrange: get the attributes available on the global database
        let original_num_global_attrs: usize =
            global_db.get_att_database(ANOTHER_TCB_IDX).list_attributes().len();

        // act: add service to to the server
        db_manager
            .for_each_database(SERVER_ID, |db| {
                db.add_service_with_handles(
                    GattServiceWithHandle {
                        handle: SERVICE_HANDLE,
                        type_: SERVICE_UUID,
                        characteristics: vec![],
                    },
                    Rc::new(MockDatastore::new().0),
                )
            })
            .unwrap();
        // act: get the attributes now available on the isolated connection
        let num_isolated_attrs = isolated_db.get_att_database(TCB_IDX).list_attributes().len();
        // act: get the attributes now available on the global database
        let num_global_attrs: usize =
            global_db.get_att_database(ANOTHER_TCB_IDX).list_attributes().len();

        // assert: that the the service was added to both the global + isolated databases
        assert_eq!(num_isolated_attrs, original_num_isolated_attrs + 1);
        assert_eq!(num_global_attrs, original_num_global_attrs + 1);
    }

    #[test]
    fn test_close_server_resets_isolation() {
        // arrange
        let mut db_manager = new_db_manager();
        // arrange: add a GATT server
        db_manager.open_gatt_server(SERVER_ID).unwrap();
        // arrange: associate it with a given advertiser
        db_manager.associate_server_with_advertiser(SERVER_ID, ADVERTISER_ID).unwrap();
        // arrange: add a second GATT server with one service
        db_manager.open_gatt_server(ANOTHER_SERVER_ID).unwrap();
        db_manager
            .for_each_database(ANOTHER_SERVER_ID, |db| {
                db.add_service_with_handles(
                    GattServiceWithHandle {
                        handle: SERVICE_HANDLE,
                        type_: SERVICE_UUID,
                        characteristics: vec![],
                    },
                    Rc::new(MockDatastore::new().0),
                )
            })
            .unwrap();

        // act: close the server
        db_manager.close_gatt_server(SERVER_ID).unwrap();
        // act: re-open the server
        db_manager.open_gatt_server(SERVER_ID).unwrap();

        // act: connect to the advertiser
        let db = db_manager.on_le_connect(TCB_IDX, Some(ADVERTISER_ID));
        // act: try to read the service from the second server
        let has_service =
            block_on_locally(db.get_att_database(TCB_IDX).read_attribute(SERVICE_HANDLE));

        // assert: that we can read the service, so we are not isolated
        assert!(has_service.is_ok());
    }
}
