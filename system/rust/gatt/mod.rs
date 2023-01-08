// Copyright 2022, The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//! This module is injected into the StorageModule to read/write its keys using the BluetoothKeystoreService in Java,
//! rather than writing to disk. It exposes no external APIs for other modules to use.

pub mod arbiter;
mod ffi;
pub mod ids;
mod jni_callbacks;
mod server;

use std::{collections::HashMap, rc::Rc};

use crate::{
    do_in_rust_thread,
    gatt::{ids::ConnectionId, server::gatt_database::GattDatabase},
    packets::{AttAttributeDataChild, AttBuilder, AttErrorCode, AttView, Serializable},
};

pub use self::jni_callbacks::GattCallbacks;
use self::{
    arbiter::with_arbiter,
    ffi::{InterceptAction, SendPacketToPeer, StoreCallbacksFromRust},
    ids::{AdvertiserId, ServerId, TransactionId, TransportIndex},
    server::{
        callback_transaction_manager::{CallbackGattDatastore, CallbackTransactionManager},
        gatt_database::{AttDatabaseImpl, GattServiceWithHandle},
        server_connection::GattServerConnection,
    },
};

pub use ffi::GattServerCallbacks;
use log::{error, info};

#[allow(missing_docs)]
pub struct GattModule {
    servers:
        HashMap<ConnectionId, Rc<GattServerConnection<AttDatabaseImpl<CallbackGattDatastore>>>>,
    databases: HashMap<ServerId, Rc<GattDatabase<CallbackGattDatastore>>>,
    callback_manager: Rc<CallbackTransactionManager>,
}

impl GattModule {
    /// Constructor. Depends on `callbacks` to send callbacks in the JNI thread.
    pub fn new(callbacks: Rc<dyn GattCallbacks>) -> Self {
        StoreCallbacksFromRust(on_le_connect, on_le_disconnect, intercept_packet);

        arbiter::initialize_arbiter();

        let callback_manager = Rc::new(CallbackTransactionManager::new(&callbacks));
        Self { servers: HashMap::new(), databases: HashMap::new(), callback_manager }
    }

    /// Handle LE link connect
    pub fn on_le_connect(&mut self, conn_id: ConnectionId) {
        info!("connected on conn_id {conn_id:?}");
        let database = self.databases.get(&conn_id.get_server_id());
        if let Some(database) = database {
            self.callback_manager.add_connection(conn_id);
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
        self.callback_manager.remove_connection(conn_id);
    }

    /// Handle an incoming ATT packet
    pub fn handle_packet(&mut self, conn_id: ConnectionId, packet: AttView<'_>) {
        match self.servers.get(&conn_id) {
            Some(server) => server.try_handle_request(packet),
            None => error!("dropping ATT packet for unregistered connection"),
        }
    }

    /// Handle a response to a GATT request from JNI
    pub fn send_response(
        &mut self,
        conn_id: ConnectionId,
        trans_id: TransactionId,
        value: Result<AttAttributeDataChild, AttErrorCode>,
    ) {
        self.callback_manager.send_response(conn_id, trans_id, value);
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
        let old = self.databases.insert(server_id, GattDatabase::new(self.callback_manager.get_datastore()).into());
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

fn on_le_connect(tcb_idx: u8, advertiser: u8) {
    if let Some(conn_id) = with_arbiter(|arbiter| {
        arbiter.on_le_connect(TransportIndex(tcb_idx), AdvertiserId(advertiser))
    }) {
        do_in_rust_thread(move |modules| {
            modules.gatt_module.on_le_connect(conn_id);
        })
    }
}

fn on_le_disconnect(tcb_idx: u8) {
    if let Some(conn_id) = with_arbiter(|arbiter| arbiter.on_le_disconnect(TransportIndex(tcb_idx)))
    {
        do_in_rust_thread(move |modules| {
            modules.gatt_module.on_le_disconnect(conn_id);
        })
    }
}

fn intercept_packet(tcb_idx: u8, packet: Vec<u8>) -> InterceptAction {
    if let Some((att, conn_id)) = with_arbiter(|arbiter| {
        arbiter.try_parse_att_server_packet(TransportIndex(tcb_idx), packet.into_boxed_slice())
    }) {
        do_in_rust_thread(move |modules| {
            info!("pushing packet to GATT");
            modules.gatt_module.handle_packet(conn_id, att.view());
        });
        InterceptAction::Drop
    } else {
        InterceptAction::Forward
    }
}
