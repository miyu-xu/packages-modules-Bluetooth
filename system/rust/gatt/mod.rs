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
mod jni_callbacks;
mod server;

use std::{collections::HashMap, rc::Rc};

use crate::{
    do_in_rust_thread,
    gatt::server::gatt_database::{
        AttPermissions, AttUuid, GattCharacteristic, GattDatabase, GattService,
    },
    packets::{AttBuilder, AttView},
};

pub use self::jni_callbacks::GattCallbacks;
use self::{
    arbiter::try_parse_att_server_packet,
    ffi::{InterceptAction, SendPacketToPeer, StoreCallbacksFromRust},
    server::{demo_database::DemoGattDatastore, server_connection::GattServerConnection},
};

pub use ffi::GattServerCallbacks;
use log::{error, info};

#[allow(missing_docs)]
pub struct GattModule<'a> {
    servers: HashMap<u16, Rc<GattServerConnection<GattDatabase<DemoGattDatastore>>>>,
    callbacks: &'a (dyn GattCallbacks + 'a),
}

impl<'a> GattModule<'a> {
    /// Constructor. Depends on `callbacks` to send callbacks in the JNI thread.
    pub fn new(callbacks: &'a dyn GattCallbacks) -> Self {
        StoreCallbacksFromRust(on_le_connect, on_le_disconnect, intercept_packet);
        Self { callbacks, servers: HashMap::new() }
    }

    /// TEMP
    pub fn start(&self) {
        self.callbacks.ack("hello, world!")
    }

    /// Handle LE link connect
    pub fn on_le_connect(&mut self, handle: u16) {
        info!("connected on handle {handle}");
        let database = Rc::new(GattDatabase::new(DemoGattDatastore::new()));
        database
            .add_service(GattService {
                uuid: AttUuid::new([1, 2, 3, 4]),
                characteristics: vec![GattCharacteristic {
                    uuid: AttUuid::new([5, 6, 7, 8]),
                    permissions: AttPermissions { readable: true, writable: false },
                }],
            })
            .unwrap();
        self.servers.insert(
            handle,
            GattServerConnection::new(database, move |packet| Self::send_packet(handle, packet)),
        );
    }

    /// Handle an LE link disconnect
    pub fn on_le_disconnect(&mut self, handle: u16) {
        info!("disconnected handle {handle}");
        self.servers.remove(&handle);
    }

    /// Handle an incoming ATT packet
    pub fn handle_packet(&mut self, handle: u16, packet: AttView<'_>) {
        match self.servers.get(&handle) {
            Some(server) => server.try_handle_request(packet),
            None => error!("dropping ATT packet for unregistered connection"),
        }
    }

    fn send_packet(handle: u16, packet: AttBuilder) {
        SendPacketToPeer(handle, packet.to_vec().unwrap())
    }
}

fn on_le_connect(handle: u16) {
    do_in_rust_thread(move |modules| {
        modules.gatt_module.on_le_connect(handle);
    })
}

fn on_le_disconnect(handle: u16) {
    do_in_rust_thread(move |modules| {
        modules.gatt_module.on_le_disconnect(handle);
    })
}

fn intercept_packet(handle: u16, packet: Vec<u8>) -> InterceptAction {
    if let Some(att) = try_parse_att_server_packet(packet.into_boxed_slice()) {
        do_in_rust_thread(move |modules| {
            info!("pushing packet to GATT");
            modules.gatt_module.handle_packet(handle, att.view());
        });
        InterceptAction::Drop
    } else {
        InterceptAction::Forward
    }
}
