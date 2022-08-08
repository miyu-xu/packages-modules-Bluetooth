#![allow(dead_code, unused, missing_docs)]
//! link management

mod bridge;
mod control_demultiplexer;
mod demultiplexer;
mod listeners;
pub mod types;

use anyhow::{anyhow, ensure, Result};
use anyhow::{Context, Ok};
use cxx::UniquePtr;
use gddi::module;
use gddi::provides;
use gddi::Stoppable;
use log::{info, warn};
use std::cell::RefCell;
use std::collections::HashMap;
use std::mem::{discriminant, Discriminant};
use std::num::NonZeroU16;
use std::sync::Arc;
use tokio::runtime::Runtime;
use tokio::spawn;
use tokio::sync::mpsc::{channel, Receiver, Sender};
use tokio::sync::Mutex;
use tokio::sync::{oneshot, RwLock};
use tokio::task::JoinHandle;
use tokio_stream::wrappers::{self, ReceiverStream};
use tokio_stream::StreamExt;

use self::bridge::ffi::{
    initialize_l2cap_tx_on_main_thread, L2CA_ConnectReq_from_rust, L2CA_DisconnectReq_from_rust,
    L2CA_Register_from_rust,
};
use self::bridge::EventChannel;
use self::control_demultiplexer::L2capControlDemultiplexer;
use self::demultiplexer::{DemultiplexedReceiver, Demultiplexer};
use self::listeners::CallbackEvent;
use self::types::{Address, IdentityAddress, L2capChannelId, L2capPsm};
use bt_packets::hci::{
    AclPacket, CommandPacket, EventCode, EventPacket, IsoPacket, LeMetaEventPacket, ScoPacket,
    SubeventCode,
};
use std::ptr::null_mut;
module! {
    l2cap_module,
        providers {
        L2cap => provide_l2cap,
    },
}

#[derive(Clone, Stoppable)]
pub struct L2cap(Arc<L2capContents>);

#[derive(Debug)]
struct IncomingConnection {
    local_psm: L2capPsm,
    channel: L2capChannel,
}

#[derive(Debug)]
struct ChannelError {
    local_cid: L2capChannelId,
    error_code: NonZeroU16,
}

#[derive(Debug)]
struct IncomingData {
    local_cid: L2capChannelId,
    data: Box<[u8]>,
}

struct L2capContents {
    /// Incoming connections are dispatched to listeners keyed by their PSM
    incoming_connection_demultiplexer: Demultiplexer<IncomingConnection, L2capPsm>,
    /// Outgoing connections, once successfully established, are dispatched to listeners keyed by their LCID
    outgoing_connection_demultiplexer: Demultiplexer<L2capChannel, L2capChannelId>,
    /// Channel errors are dispatched to listeners keyed by their LCID
    error_demultiplexer: Demultiplexer<ChannelError, L2capChannelId>,
    /// A handle for our event loop, so we can stop it on shutdown
    event_loop_handle: JoinHandle<()>,
}

#[provides]
async fn provide_l2cap(rt: Arc<Runtime>) -> L2cap {
    // TODO: what if we get a ton of (data?) packets arriving? Should we drop them, block the main thread, or grow unbounded?
    let (mut callback_tx, callback_rx) = channel(64);
    unsafe { initialize_l2cap_tx_on_main_thread(&mut EventChannel(callback_tx)) };

    let (mut incoming_connection_tx, incoming_connection_rx) = channel(8);
    let incoming_connection_demultiplexer =
        Demultiplexer::new(incoming_connection_rx, |connection: &IncomingConnection| {
            connection.local_psm
        });

    let (mut outgoing_connection_tx, outgoing_connection_rx) = channel(8);
    let outgoing_connection_demultiplexer =
        Demultiplexer::new(outgoing_connection_rx, |channel: &L2capChannel| channel.local_cid);

    let (mut error_tx, error_rx) = channel(8);
    let error_demultiplexer =
        Demultiplexer::new(error_rx, |channel: &ChannelError| channel.local_cid);

    // start event loop dispatching callbacks to handlers for each event type
    let event_loop_handle = spawn(
        L2capControlDemultiplexer::new(
            callback_rx,
            incoming_connection_tx,
            outgoing_connection_tx,
            error_tx,
        )
        .start(),
    );

    L2cap(Arc::new(L2capContents {
        incoming_connection_demultiplexer,
        outgoing_connection_demultiplexer,
        error_demultiplexer,
        event_loop_handle,
    }))
}

impl L2cap {
    /// Defined in bt_target.h, mirrored here for interop
    const MTU_SIZE: u16 = 1691;

    pub async fn create_channel(
        &self,
        psm: L2capPsm,
        target: IdentityAddress,
    ) -> Result<L2capChannel> {
        info!("creating channel to {target:?} at psm {psm:?}");
        let (mut tx, mut rx) = oneshot::channel();
        unsafe { L2CA_ConnectReq_from_rust(psm.psm, &target.0, &mut tx.into()) }
        let local_cid = L2capChannelId { cid: rx.await? };
        // FIXME: There is a race condition here! What if L2CAP gets back before we register our listener in the multiplexer?
        let mut success_listener =
            self.0.outgoing_connection_demultiplexer.subscribe(local_cid).await.with_context(
                || format!("failed to subscribe to psm {psm:?} in the multiplexer"),
            )?;
        success_listener.recv().await.context("multiplexer shut down before response")
    }

    pub async fn register_service(&self, psm: L2capPsm) -> Result<L2capService> {
        info!("creating listening l2cap socket at psm {psm:?}");
        let (mut tx, mut rx) = oneshot::channel();
        unsafe {
            L2CA_Register_from_rust(psm.psm, true, null_mut(), Self::MTU_SIZE, 0, &mut tx.into());
        }
        let psm = rx.await?;
        ensure!(psm != 0, "unable to register L2cap service");
        let psm = L2capPsm { psm };
        let incoming_channel_rx =
            self.0.incoming_connection_demultiplexer.subscribe(psm).await.with_context(|| {
                format!("failed to subscribed to psm {psm:?} in the multiplexer")
            })?;
        Ok(L2capService { psm, incoming_channel_rx })
    }
}

// a listening PSM for incoming connections
#[derive(Debug)]
pub struct L2capService {
    /// The allocated PSM (typically the same as the requested PSM)
    psm: L2capPsm,
    /// New channels opened by a remote device
    incoming_channel_rx: DemultiplexedReceiver<L2capPsm, IncomingConnection>,
}

impl L2capService {
    pub async fn accept(&mut self) -> Result<L2capChannel> {
        Ok(self.incoming_channel_rx.recv().await.context("listening psm was unregistered")?.channel)
    }
}

/// a channel between a fixed pair of devices
#[derive(Debug)]
pub struct L2capChannel {
    local_cid: L2capChannelId,
    data_rx: DemultiplexedReceiver<L2capChannelId, IncomingData>,
}

impl L2capChannel {
    pub async fn read(&mut self) -> Result<Box<[u8]>> {
        Ok(self.data_rx.recv().await.context("channel closed, probably by remote device")?.data)
    }
}

impl Drop for L2capChannel {
    fn drop(&mut self) {
        unsafe { L2CA_DisconnectReq_from_rust(self.local_cid.cid) }
    }
}
