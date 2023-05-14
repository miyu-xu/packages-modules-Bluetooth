//! This module manages all GATT databases - specifically, the global database,
//! as well as per-connection databases.

use std::{
    collections::HashMap,
    sync::{Arc, Mutex, MutexGuard},
};

use anyhow::{Result, bail};
use bt_common::init_flags::always_use_private_gatt_for_debugging_is_enabled;

use crate::{gatt::ids::{ServerId, TransportIndex}, core::shared_box::SharedBox};

use super::{gatt_database::GattDatabase, isolation_manager::IsolationManager, services::register_builtin_services};

pub struct GattDatabaseManager {
    global_database: SharedBox<GattDatabase>,
    single_server_databases: HashMap<ServerId, SharedBox<GattDatabase>>,
    // NOTE: this is logically owned by the GattDatabaseManager. We share it behind a Mutex just so we
    // can use it as part of the Arbiter. Once the Arbiter is removed, this should be owned
    // fully by the GattDatabaseManager.
    isolation_manager: Arc<Mutex<IsolationManager>>,
}

fn new_database_with_services() -> GattDatabase {
  let mut db = GattDatabase::new();
  register_builtin_services(&mut db).expect("builtin services should never fail to register on an empty db");
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
    pub fn open_gatt_server(&mut self, server_id: ServerId) -> Result<()> {
        let old = self.single_server_databases.insert(server_id, new_database_with_services().into());
        if old.is_some() {
            bail!("GATT server {server_id:?} already exists but was re-opened, clobbering old value...")
        }
        Ok(())
    }

    /// Close a GATT server
    pub fn close_gatt_server(&mut self, server_id: ServerId) -> Result<()> {
        let old = self.single_server_databases.remove(&server_id);
        if old.is_none() {
            bail!("GATT server {server_id:?} did not exist")
        };

        if !always_use_private_gatt_for_debugging_is_enabled() {
            self.get_isolation_manager().clear_server(server_id);
        }

        Ok(())
    }

    /// Apply a function on all databases, reporting an error if it fails on a database
    /// without continuing.
    pub fn for_each_database<T>(
        &self,
        server_id: ServerId,
        mut f: impl FnMut(&SharedBox<GattDatabase>) -> Result<(), T>,
    ) -> Result<(), T> {
        f(&self.global_database)?;
        self.single_server_databases.get(&server_id).map(f).transpose()?;
        Ok(())
    }

    pub fn get_database(&self, tcb_idx: TransportIndex) -> &SharedBox<GattDatabase> {
        let server_id = self.get_isolation_manager().get_server_id(tcb_idx);
        if let Some(server_id) = server_id {
            if let Some(server) = self.single_server_databases.get(&server_id) {
                return server;
            }
        }
        return &self.global_database;
    }

    /// Get the IsolationManager to manage associations between servers + advertisers
    pub fn get_isolation_manager(&self) -> MutexGuard<'_, IsolationManager> {
        self.isolation_manager.lock().unwrap()
    }
}
