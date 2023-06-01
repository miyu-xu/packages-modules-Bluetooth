//! This module is a simple GATT server that shares the ATT channel with the
//! existing C++ GATT client.
//!
//! The basic data-model is:
//! 1. RawGattDatastore: Trait that represents an "upper-layer", exposing async read() / write() operations
//! 2. GattDatabase: Struct that holds a list of services + characteristics, along with the backing RawGattDatastores
//! 3. AttDatabase: Trait representing a list of attributes, exposing list(), read(), write() operations.
//! 4. ServerId: An ID corresponding to a BluetoothGattServer (in Java).
//!
//! Each BluetoothGattServer can contain multiple GattServices, which can contain multiple GattCharacteristics.
//! A GattDatabase represents the union of some BluetoothGattServers that are visible on a given connection.
//! GattDatabases currently can either contain a single BluetoothGattServer (an "isolated" database), or
//! the union of *all* BluetoothGattServers (the "global" database)
//!
//! An AttDatabase is produced from a GattDatabase for each connection.

mod att_database;
pub mod att_server_bearer;
pub mod gatt_database;
mod indication_handler;
mod request_handler;
pub mod services;
mod transactions;

mod command_handler;
mod database_manager;
pub mod isolation_manager;
#[cfg(test)]
mod test;

use std::{
    collections::HashMap,
    rc::Rc,
    sync::{Arc, Mutex},
};

use crate::{
    core::shared_box::{SharedBox, WeakBox, WeakBoxRef},
    gatt::server::gatt_database::GattDatabase,
};

use self::{
    super::ids::ServerId,
    att_server_bearer::AttServerBearer,
    database_manager::GattDatabaseManager,
    gatt_database::{AttDatabaseImpl, GattServiceWithHandle},
    isolation_manager::IsolationManager,
};

use super::{
    callbacks::RawGattDatastore,
    channel::AttTransport,
    ids::{AdvertiserId, AttHandle, TransportIndex},
};
use anyhow::{bail, Result};
use log::info;

pub use indication_handler::IndicationError;

#[allow(missing_docs)]
pub struct GattModule {
    connections: HashMap<TransportIndex, GattConnection>,
    databases: GattDatabaseManager,
    transport: Rc<dyn AttTransport>,
}

struct GattConnection {
    bearer: SharedBox<AttServerBearer<AttDatabaseImpl>>,
    database: WeakBox<GattDatabase>,
}

impl GattModule {
    /// Constructor.
    pub fn new(
        transport: Rc<dyn AttTransport>,
        isolation_manager: Arc<Mutex<IsolationManager>>,
    ) -> Self {
        Self {
            connections: HashMap::new(),
            databases: GattDatabaseManager::new(isolation_manager),
            transport,
        }
    }

    /// Handle LE link connect
    pub fn on_le_connect(
        &mut self,
        tcb_idx: TransportIndex,
        advertiser_id: Option<AdvertiserId>,
    ) -> Result<()> {
        info!("connected on tcb_idx {tcb_idx:?}");
        let database = self.databases.on_le_connect(tcb_idx, advertiser_id);

        let transport = self.transport.clone();
        let bearer = SharedBox::new(AttServerBearer::new(
            database.get_att_database(tcb_idx),
            move |packet| transport.send_packet(tcb_idx, packet),
        ));
        database.on_bearer_ready(tcb_idx, bearer.as_ref());
        self.connections.insert(tcb_idx, GattConnection { bearer, database: database.downgrade() });
        Ok(())
    }

    /// Handle an LE link disconnect
    pub fn on_le_disconnect(&mut self, tcb_idx: TransportIndex) -> Result<()> {
        info!("disconnected conn_id {tcb_idx:?}");
        self.databases.on_le_disconnect(tcb_idx);
        let connection = self.connections.remove(&tcb_idx);
        let Some(connection) = connection else {
            bail!("got disconnection from {tcb_idx:?} but bearer does not exist");
        };
        drop(connection.bearer);
        connection.database.with(|db| db.map(|db| db.on_bearer_dropped(tcb_idx)));
        Ok(())
    }

    /// Register a new GATT service on a given server
    pub fn register_gatt_service(
        &mut self,
        server_id: ServerId,
        service: GattServiceWithHandle,
        datastore: impl RawGattDatastore + 'static,
    ) -> Result<()> {
        let datastore = Rc::new(datastore);
        self.databases.for_each_database(server_id, |db| {
            db.add_service_with_handles(service.clone(), datastore.clone())
        })
    }

    /// Unregister an existing GATT service on a given server
    pub fn unregister_gatt_service(
        &mut self,
        server_id: ServerId,
        service_handle: AttHandle,
    ) -> Result<()> {
        self.databases
            .for_each_database(server_id, |db| db.remove_service_at_handle(service_handle))
    }

    /// Open a GATT server
    pub fn open_gatt_server(&mut self, server_id: ServerId) -> Result<()> {
        self.databases.open_gatt_server(server_id)
    }

    /// Close a GATT server
    pub fn close_gatt_server(&mut self, server_id: ServerId) -> Result<()> {
        self.databases.close_gatt_server(server_id)
    }

    /// Get an ATT bearer for a particular connection
    pub fn get_bearer(
        &self,
        tcb_idx: TransportIndex,
    ) -> Option<WeakBoxRef<AttServerBearer<AttDatabaseImpl>>> {
        self.connections.get(&tcb_idx).map(|x| x.bearer.as_ref())
    }

    /// Link a given GATT server to an LE advertising set, so incoming
    /// connections to this advertiser will be visible only by the linked
    /// server
    pub fn associate_server_with_advertiser(
        &mut self,
        server_id: ServerId,
        advertiser_id: AdvertiserId,
    ) -> Result<()> {
        self.databases.associate_server_with_advertiser(server_id, advertiser_id)
    }

    /// Unlink all servers associated with the specified advertiser
    pub fn remove_servers_tied_to_advertiser(&mut self, advertiser_id: AdvertiserId) {
        self.databases.remove_servers_tied_to_advertiser(advertiser_id)
    }
}
