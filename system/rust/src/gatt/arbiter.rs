//! This module handles "arbitration" of ATT packets, to determine whether they should be handled by the primary stack
//! or by the "Private GATT" stack

use std::{collections::HashMap, sync::Mutex};

use log::{error, info};

use crate::{
    do_in_rust_thread,
    packets::{AttOpcode, OwnedAttView, OwnedPacket},
};

use super::{
    ffi::{InterceptAction, StoreCallbacksFromRust},
    ids::{AdvertiserId, ConnectionId, ServerId, TransportIndex},
};

static ARBITER: Mutex<Option<Arbiter>> = Mutex::new(None);

/// This class is responsible for tracking which connections and advertising we own,
/// and using this information to decide what packets should be intercepted, and which
/// should be forwarded to the legacy stack.
pub struct Arbiter {
    advertiser_to_server: HashMap<AdvertiserId, ServerId>,
    transport_to_owned_connection: HashMap<TransportIndex, ConnectionId>,
}

/// Initialize the Arbiter
pub fn initialize_arbiter() {
    *ARBITER.lock().unwrap() = Some(Arbiter {
        advertiser_to_server: HashMap::new(),
        transport_to_owned_connection: HashMap::new(),
    });

    StoreCallbacksFromRust(on_le_connect, on_le_disconnect, intercept_packet);
}

/// Acquire the mutex holding the Arbiter and provide a mutable reference to the supplied closure
pub fn with_arbiter<T>(f: impl FnOnce(&mut Arbiter) -> T) -> T {
    f(ARBITER.lock().unwrap().as_mut().unwrap())
}

impl Arbiter {
    /// Link a given GATT server to an LE advertising set, so incoming connections to this advertiser
    /// will be visible only by the linked server
    pub fn associate_server_with_advertiser(
        &mut self,
        server_id: ServerId,
        advertiser_id: AdvertiserId,
    ) {
        info!("associating server {server_id:?} with advertising set {advertiser_id:?}");
        let old = self.advertiser_to_server.insert(advertiser_id, server_id);
        if let Some(old) = old {
            error!("new server {server_id:?} associated with same advertiser {advertiser_id:?}, displacing old server {old:?}");
        }
    }

    /// Remove all linked advertising sets from the provided server
    pub fn clear_server(&mut self, server_id: ServerId) {
        info!("clearing advertisers associated with {server_id:?}");
        self.advertiser_to_server.retain(|_, server| *server != server_id);
    }

    /// Clear the server associated with this advertiser, if one exists
    pub fn clear_advertiser(&mut self, advertiser_id: AdvertiserId) {
        info!("removing server (if any) associated with advertiser {advertiser_id:?}");
        self.advertiser_to_server.remove(&advertiser_id);
    }

    /// Check if this conn_id is currently owned by the Rust stack
    pub fn is_connection_isolated(&self, conn_id: ConnectionId) -> bool {
        self.transport_to_owned_connection.values().any(|owned_conn_id| *owned_conn_id == conn_id)
    }

    /// Test to see if a buffer contains a valid ATT packet with an opcode we are interested in intercepting
    pub fn try_parse_att_server_packet(
        &self,
        tcb_idx: TransportIndex,
        packet: Box<[u8]>,
    ) -> Option<(OwnedAttView, ConnectionId)> {
        let conn_id = *self.transport_to_owned_connection.get(&tcb_idx)?;

        let att = OwnedAttView::try_parse(packet).ok()?;

        match att.view().get_opcode() {
            AttOpcode::FIND_INFORMATION_REQUEST
            | AttOpcode::FIND_BY_TYPE_VALUE_REQUEST
            | AttOpcode::READ_BY_TYPE_REQUEST
            | AttOpcode::READ_REQUEST
            | AttOpcode::READ_BLOB_REQUEST
            | AttOpcode::READ_MULTIPLE_REQUEST
            | AttOpcode::READ_BY_GROUP_TYPE_REQUEST => Some((att, conn_id)),
            _ => None,
        }
    }

    /// Check if an incoming connection should be intercepted and, if so, on what conn_id
    pub fn on_le_connect(
        &mut self,
        tcb_idx: TransportIndex,
        advertiser: AdvertiserId,
    ) -> Option<ConnectionId> {
        info!(
            "processing incoming connection on transport {tcb_idx:?} to advertiser {advertiser:?}"
        );
        let server_id = *self.advertiser_to_server.get(&advertiser)?;
        info!("connection is isolated to server {server_id:?}");

        let conn_id = ConnectionId::new(tcb_idx, server_id);
        let old = self.transport_to_owned_connection.insert(tcb_idx, conn_id);
        if old.is_some() {
            error!("new server {server_id:?} on transport {tcb_idx:?} displacing existing registered connection {conn_id:?}")
        }
        Some(conn_id)
    }

    /// Handle a disconnection and return the disconnected conn_id, if any
    pub fn on_le_disconnect(&mut self, tcb_idx: TransportIndex) -> Option<ConnectionId> {
        info!("processing disconnection on transport {tcb_idx:?}");
        self.transport_to_owned_connection.remove(&tcb_idx)
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
