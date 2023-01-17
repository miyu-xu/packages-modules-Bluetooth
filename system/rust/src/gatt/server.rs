//! This module is a simple GATT server that shares the ATT channel with the existing C++ GATT client.
//! See go/private-gatt-in-platform for the design.

mod att_database;
pub mod gatt_database;
pub mod server_connection;
mod transaction_handler;
mod transactions;

#[cfg(test)]
mod test;
mod utils;

use std::{collections::HashMap, rc::Rc};

use crate::{
    gatt::{ids::ConnectionId, server::gatt_database::GattDatabase},
    packets::{AttBuilder, AttView, Serializable},
};

use self::{
    super::ffi::SendPacketToPeer,
    super::ids::ServerId,
    gatt_database::{AttDatabaseImpl, GattServiceWithHandle},
    server_connection::GattServerConnection,
};

use super::callbacks::GattDatastore;
use log::{error, info};

#[allow(missing_docs)]
pub struct GattModule {
    servers: HashMap<ConnectionId, Rc<GattServerConnection<AttDatabaseImpl<dyn GattDatastore>>>>,
    databases: HashMap<ServerId, Rc<GattDatabase<dyn GattDatastore>>>,
    datastore: Rc<dyn GattDatastore>,
}

impl GattModule {
    /// Constructor. Uses `datastore` to read/write characteristics.
    pub fn new(datastore: Rc<dyn GattDatastore>) -> Self {
        Self { servers: HashMap::new(), databases: HashMap::new(), datastore }
    }

    /// Handle LE link connect
    pub fn on_le_connect(&mut self, conn_id: ConnectionId) {
        info!("connected on conn_id {conn_id:?}");
        let database = self.databases.get(&conn_id.get_server_id());
        if let Some(database) = database {
            self.datastore.add_connection(conn_id);
            self.servers.insert(
                conn_id,
                GattServerConnection::new(database.get_att_database(conn_id), move |packet| {
                    Self::send_packet(conn_id, packet)
                }),
            );
        } else {
            error!("got connection to conn_id {conn_id:?} (server_id {:?}) but this server does not exist!", conn_id.get_server_id());
        }
    }

    /// Handle an LE link disconnect
    pub fn on_le_disconnect(&mut self, conn_id: ConnectionId) {
        info!("disconnected conn_id {conn_id:?}");
        self.servers.remove(&conn_id);
        self.datastore.remove_connection(conn_id);
    }

    /// Handle an incoming ATT packet
    pub fn handle_packet(&mut self, conn_id: ConnectionId, packet: AttView<'_>) {
        match self.servers.get(&conn_id) {
            Some(server) => server.handle_packet(packet),
            None => error!("dropping ATT packet for unregistered connection"),
        }
    }

    /// Register a new GATT service on a given server
    pub fn register_gatt_service(
        &mut self,
        server_id: ServerId,
        service: GattServiceWithHandle,
    ) -> Result<(), String> {
        self.databases
            .get(&server_id)
            .ok_or(format!("server {server_id:?} not opened"))?
            .add_service_with_handles(service)
    }

    /// Open a GATT server
    pub fn open_gatt_server(&mut self, server_id: ServerId) {
        let old =
            self.databases.insert(server_id, GattDatabase::new(self.datastore.clone()).into());
        if old.is_some() {
            error!("GATT server {server_id:?} already exists but was re-opened, clobbering old value...")
        }
    }

    /// Close a GATT server
    pub fn close_gatt_server(&mut self, server_id: ServerId) {
        let old = self.databases.remove(&server_id);
        if old.is_none() {
            error!("GATT server {server_id:?} already exists but was re-opened, clobbering old value...")
        }
    }

    fn send_packet(conn_id: ConnectionId, packet: AttBuilder) {
        SendPacketToPeer(conn_id.get_tcb_idx().0, packet.to_vec().unwrap())
    }
}
