//! This module is a simple GATT server that shares the ATT channel with the
//! existing C++ GATT client.

mod att_database;
pub mod att_server_bearer;
pub mod gatt_database;
mod indication_handler;
mod request_handler;
mod transactions;

pub mod isolation_manager;
mod services;
#[cfg(test)]
mod test;

use std::{collections::HashMap, rc::Rc};

use crate::{
    core::shared_box::{SharedBox, WeakBoxRef},
    gatt::server::gatt_database::GattDatabase,
};

use self::{
    super::ids::ServerId,
    att_server_bearer::AttServerBearer,
    gatt_database::{AttDatabaseImpl, GattServiceWithHandle},
    isolation_manager::IsolationManager,
    services::register_builtin_services,
};

use super::{
    arbiter::Arbiter,
    callbacks::GattDatastore,
    channel::AttTransport,
    ids::{AdvertiserId, AttHandle, TransportIndex},
};
use anyhow::{anyhow, bail, Result};
use log::info;

pub use indication_handler::IndicationError;

#[allow(missing_docs)]
pub struct GattModule {
    bearers: HashMap<TransportIndex, SharedBox<AttServerBearer<AttDatabaseImpl>>>,
    databases: HashMap<ServerId, SharedBox<GattDatabase>>,
    global_database: SharedBox<GattDatabase>,
    isolation_manager: IsolationManager,
    transport: Rc<dyn AttTransport>,
}

impl GattModule {
    /// Constructor.
    pub fn new(transport: Rc<dyn AttTransport>) -> Self {
        let mut global_database = GattDatabase::new();
        register_builtin_services(&mut global_database).expect("failed to initialize global db");
        Self {
            bearers: HashMap::new(),
            databases: HashMap::new(),
            global_database: global_database.into(),
            isolation_manager: IsolationManager::new(),
            transport,
        }
    }

    /// Handle LE link connect on an associated advertisement (if we are a peripheral)
    pub fn on_le_connect(
        &mut self,
        tcb_idx: TransportIndex,
        advertiser: Option<AdvertiserId>,
    ) -> Result<()> {
        info!("connected on tcb_idx {tcb_idx:?}");

        let database = if let Some(server_id) = advertiser
            .and_then(|advertiser| self.isolation_manager.on_le_connect(tcb_idx, advertiser))
        {
            self.databases.get(&server_id).ok_or_else(|| anyhow!("got connection to conn_id {tcb_idx:?} (server_id {server_id:?}) but this server does not exist!"))?
        } else {
            &self.global_database
        };

        let transport = self.transport.clone();
        self.bearers.insert(
            tcb_idx,
            AttServerBearer::new(database.get_att_database(tcb_idx), move |packet| {
                transport.send_packet(tcb_idx, packet)
            })
            .into(),
        );
        Ok(())
    }

    /// Handle an LE link disconnect
    pub fn on_le_disconnect(&mut self, tcb_idx: TransportIndex) {
        info!("disconnected tcb_idx {tcb_idx:?}");
        self.bearers.remove(&tcb_idx);
    }

    /// Register a new GATT service on a given server
    pub fn register_gatt_service(
        &mut self,
        server_id: ServerId,
        service: GattServiceWithHandle,
        datastore: Rc<dyn GattDatastore>,
    ) -> Result<()> {
        self.global_database.add_service_with_handles(&service, datastore.clone())?;
        
        self.databases
            .get(&server_id)
            .ok_or_else(|| anyhow!("server {server_id:?} not opened"))?
            .add_service_with_handles(&service, datastore)
    }

    /// Unregister an existing GATT service on a given server
    pub fn unregister_gatt_service(
        &mut self,
        server_id: ServerId,
        service_handle: AttHandle,
    ) -> Result<()> {
        self.global_database.remove_service_at_handle(service_handle)?;

        self.databases
            .get(&server_id)
            .ok_or_else(|| anyhow!("server {server_id:?} not opened"))?
            .remove_service_at_handle(service_handle)
    }

    /// Open a GATT server
    pub fn open_gatt_server(&mut self, server_id: ServerId) -> Result<()> {
        let mut db = GattDatabase::new();
        register_builtin_services(&mut db)?;
        let old = self.databases.insert(server_id, db.into());
        if old.is_some() {
            bail!("GATT server {server_id:?} already exists but was re-opened, clobbering old value...")
        }
        Ok(())
    }

    /// Close a GATT server
    pub fn close_gatt_server(&mut self, server_id: ServerId) -> Result<()> {
        let old = self.databases.remove(&server_id);
        if old.is_none() {
            bail!("GATT server {server_id:?} did not exist")
        };

        Ok(())
    }

    /// Get an ATT bearer for a particular logical transport
    pub fn get_bearer(
        &self,
        tcb_idx: TransportIndex,
    ) -> Option<WeakBoxRef<AttServerBearer<AttDatabaseImpl>>> {
        self.bearers.get(&tcb_idx).map(|x| x.as_ref())
    }
}
