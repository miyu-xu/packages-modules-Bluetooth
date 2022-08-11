#![allow(dead_code, missing_docs)]
//! link management

mod bridge;
mod control_demultiplexer;
pub mod demultiplexer;
mod handlemap;
mod listeners;
mod nonce;
pub mod owned_handle;
pub mod types;

use anyhow::{ensure, Result};
use anyhow::{Context, Ok};

use gddi::module;
use gddi::provides;
use gddi::Stoppable;
use log::info;

use std::num::NonZeroU16;
use std::sync::Arc;
use tokio::runtime::Runtime;
use tokio::spawn;
use tokio::sync::mpsc::{channel, Sender};

use tokio::sync::oneshot;
use tokio::task::JoinHandle;

use self::bridge::ffi::{
    initialize_l2cap_tx_on_main_thread, L2CA_ConnectReq_from_rust, L2CA_Register_from_rust,
};
use self::bridge::EventChannel;
use self::control_demultiplexer::{
    ChannelHandle, ChannelSendStatus, L2capControlDemultiplexer, OutgoingEvent,
};
use self::demultiplexer::{DemultiplexedReceiver, Demultiplexer};

use self::owned_handle::OwnedHandle;
use self::types::{IdentityAddress, L2capChannelId, L2capPsm};

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
    event_loop_handle: OwnedHandle<()>,
}

#[provides]
async fn provide_l2cap(_rt: Arc<Runtime>) -> L2cap {
    // TODO: what if we get a ton of (data?) packets arriving? Should we drop them, block the main thread, or grow unbounded?
    let (callback_tx, callback_rx) = channel(64);
    unsafe { initialize_l2cap_tx_on_main_thread(&mut EventChannel(callback_tx)) };

    let incoming_connection_demultiplexer =
        Demultiplexer::new(|connection: &IncomingConnection| connection.local_psm);

    let outgoing_connection_demultiplexer =
        Demultiplexer::new(|channel: &L2capChannel| channel.local_cid);

    let error_demultiplexer = Demultiplexer::new(|channel: &ChannelError| channel.local_cid);

    // start event loop dispatching callbacks to handlers for each event type
    let event_loop_handle = spawn(
        L2capControlDemultiplexer::new(
            callback_rx,
            incoming_connection_demultiplexer.event_tx.clone(),
            outgoing_connection_demultiplexer.event_tx.clone(),
            error_demultiplexer.event_tx.clone(),
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
        let (tx, rx) = oneshot::channel();
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
        let (tx, rx) = oneshot::channel();
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
    // note: always use the Handle, not the LCID, since the LCID can be reused before this object
    // is dropped, if the remote closes the channel
    local_cid: L2capChannelId,
    handle: ChannelHandle,
    data_rx: DemultiplexedReceiver<L2capChannelId, IncomingData>,
    data_tx: Sender<OutgoingEvent>,
}

impl L2capChannel {
    pub async fn read(&mut self) -> Result<Box<[u8]>> {
        Ok(self.data_rx.recv().await.context("channel closed, probably by remote device")?.data)
    }

    pub async fn write(&mut self, data: Box<[u8]>) -> Result<ChannelSendStatus> {
        let (tx, rx) = oneshot::channel();
        self.data_tx
            .send(OutgoingEvent::SendData { channel_handle: self.handle, data, ack: tx })
            .await
            .context("channel closed, probably by remote device")?;
        rx.await.with_context(|| format!("failed to write data to channel {self:?}"))
    }
}

impl Drop for L2capChannel {
    fn drop(&mut self) {
        let _ = self.data_tx.try_send(OutgoingEvent::Disconnect { channel_handle: self.handle });
    }
}
