use super::{
    bridge::ffi::{L2CA_DataWrite_from_rust, L2CA_DisconnectReq_from_rust},
    demultiplexer::{DemultiplexedReceiver, Demultiplexer},
    handlemap::HandleMap,
    listeners::CallbackEvent,
    nonce::{Nonce, NonceGenerator},
    types::L2capChannelId,
    ChannelError, IncomingConnection, IncomingData, L2capChannel,
};
use anyhow::{anyhow, bail, Context, Result};
use log::warn;
use tokio::{
    select,
    sync::{
        mpsc::{channel, Receiver, Sender},
        oneshot,
    },
};

/// This module takes in serialized CallbackEvents from the C layer dispatched from the main thread,
/// and demultiplexes them onto the appropriate channels while updating some state synchronously.
///
/// These state updates are why the C layer cannot directly dispatch onto the relevant channels.
/// For example, imagine a channel creation followed immediately by a data packet.
/// If these packets were put onto separate queues, then we could not guarantee that a channel data
/// listener would be registered before the data packet arrives.

// ChannelHandles are used to identify listening channels, to avoid issues related to the reuse
// of L2cap channel IDs. For example, if a channel is disconnected by the remote and the ID reused,
// the Handle of the new channel will still be different, and writes from the old Handle will be
// rejected
#[derive(Debug, Eq, Hash, PartialEq, Copy, Clone)]
pub struct ChannelHandle(Nonce);

#[derive(Debug)]
pub(super) enum OutgoingEvent {
    SendData {
        channel_handle: ChannelHandle,
        data: Box<[u8]>,
        ack: oneshot::Sender<ChannelSendStatus>,
    },
    Disconnect {
        channel_handle: ChannelHandle,
    },
}

#[derive(Debug)]
pub enum ChannelSendStatus {
    Uncongested,
    Congested,
}

pub(super) struct L2capControlDemultiplexer {
    // the event rx that we are demultiplexing
    // note that it is called from legacy code, and we make the assumption that we receive
    // its events in causal order (e.g. we cannot receive incoming data before a connection is
    // established)
    rx: Receiver<CallbackEvent>,
    // these are connected to demultiplexers that dispatch to the relevant listeners
    // they live on the main thread so we can register listeners from the module
    incoming_connection_tx: Sender<IncomingConnection>,
    outgoing_connection_tx: Sender<L2capChannel>,
    error_tx: Sender<ChannelError>,
    // used to demultiplex incoming data packets
    // we own it so we can synchronously register channels as they are created
    // listeners are passed back as part of the L2capChannels
    data_demultiplexer: Demultiplexer<IncomingData, L2capChannelId>,
    // accepted channels are given unique handles, to allow for L2CAP channel ID reuse
    handles: HandleMap<L2capChannelId, ChannelHandle>,
    outgoing_event_tx: Sender<OutgoingEvent>,
    outgoing_event_rx: Receiver<OutgoingEvent>,
}

impl L2capControlDemultiplexer {
    pub(super) fn new(
        rx: Receiver<CallbackEvent>,
        incoming_connection_tx: Sender<IncomingConnection>,
        outgoing_connection_tx: Sender<L2capChannel>,
        error_tx: Sender<ChannelError>,
    ) -> Self {
        let mut nonce_gen = NonceGenerator::new();
        let (outgoing_event_tx, outgoing_event_rx) = channel(16);
        L2capControlDemultiplexer {
            rx,
            incoming_connection_tx,
            outgoing_connection_tx,
            error_tx,
            data_demultiplexer: Demultiplexer::new(|packet: &IncomingData| packet.local_cid),
            handles: HandleMap::new(Box::new(move || ChannelHandle(nonce_gen.next()))),
            outgoing_event_tx,
            outgoing_event_rx,
        }
    }

    // the core event loop of this task
    // note that we take ownership of self so we automatically destruct on exit
    pub async fn start(mut self) {
        loop {
            select! {
                event = self.rx.recv() => {
                    match event {
                        None => {
                            warn!("the l2cap callback event loop has been closed");
                            return;
                        }
                        Some(event) => {
                            let status = self.process_incoming_event(event).await;
                            if let Err(e) = status {
                                log::error!("an error occurred while processing incoming event: {e:?} - event loop will continue");
                            }
                        }
                    };
                }
                event = self.outgoing_event_rx.recv() => {
                    let status = self.process_outgoing_event(event.expect("outgoing evt channel should never be closed")).await;
                    if let Err(e) = status {
                        log::error!("an error occurred while processing outgoing event: {e:?} - event loop will continue");
                    }
                }
            }
        }
    }

    async fn register_data_channel(
        &mut self,
        local_cid: L2capChannelId,
    ) -> Result<DemultiplexedReceiver<L2capChannelId, IncomingData>> {
        self.data_demultiplexer
            .subscribe(local_cid)
            .await
            .with_context(|| format!("failed to register cid {local_cid:?} in data demultiplexer"))
    }

    async fn unregister_data_channel(&mut self, local_cid: L2capChannelId) -> Result<()> {
        self.handles.free_key(local_cid)?;
        self.data_demultiplexer.unsubscribe(local_cid).await.with_context(|| {
            format!("failed to unregister cid {local_cid:?} in data demultiplexer")
        })
    }

    async fn process_incoming_event(&mut self, event: CallbackEvent) -> Result<()> {
        match event {
            CallbackEvent::IncomingConnectionEstablished { incoming_addr: _, local_cid, psm } => {
                let data_rx = self.register_data_channel(local_cid).await?;
                self.incoming_connection_tx
                    .send(IncomingConnection {
                        local_psm: psm,
                        channel: L2capChannel {
                            local_cid,
                            handle: self.handles.assign_key(local_cid)?,
                            data_rx,
                            data_tx: self.outgoing_event_tx.clone(),
                        },
                    })
                    .await
                    .context("failed to dispatch incoming connection event")
            }
            CallbackEvent::OutgoingConnectionEstablished { local_cid } => {
                let data_rx = self.register_data_channel(local_cid).await?;
                self.outgoing_connection_tx
                    .send(L2capChannel {
                        local_cid,
                        handle: self.handles.assign_key(local_cid)?,
                        data_rx,
                        data_tx: self.outgoing_event_tx.clone(),
                    })
                    .await
                    .context("failed to dispatch established outgoing connection event")
            }
            CallbackEvent::ChannelError { local_cid, error_code } => self
                .error_tx
                .send(ChannelError { local_cid, error_code })
                .await
                .context("failed to dispatch connection error event"),
            CallbackEvent::IncomingData { local_cid, data } => self
                .data_demultiplexer
                .send(IncomingData { local_cid, data })
                .await
                .context("failed to dispatch incoming data event"),
            CallbackEvent::ChannelDisconnect { local_cid } => {
                self.unregister_data_channel(local_cid).await.with_context(|| {
                    format!(
                        "failed to deregister channel {local_cid:?} - are we currently listening?"
                    )
                })
            }
        }
    }

    async fn process_outgoing_event(&mut self, event: OutgoingEvent) -> Result<()> {
        match event {
            OutgoingEvent::SendData { channel_handle, data, ack } => {
                let (tx, rx) = oneshot::channel();
                let channel = self.handles.key_for(channel_handle)?;
                unsafe {
                    L2CA_DataWrite_from_rust(channel.cid, &data, &mut tx.into());
                }
                let status =
                    rx.await.with_context(|| format!("failed to send data on LCID={channel:?}"))?;
                match status {
                    // L2CAP_DW_FAILED
                    0 => {}
                    // L2CAP_DW_SUCCESS
                    1 => {
                        ack.send(ChannelSendStatus::Uncongested)
                            .map_err(|_| anyhow!("data sender hung up on us"))?;
                    }
                    // L2CAP_DW_CONGESTED
                    2 => {
                        ack.send(ChannelSendStatus::Congested)
                            .map_err(|_| anyhow!("data sender hung up on us"))?;
                    }
                    _ => bail!("got unexpected status code after sending data: {status:?}"),
                };
                Ok(())
            }
            OutgoingEvent::Disconnect { channel_handle } => {
                let channel = self.handles.key_for(channel_handle)?;
                unsafe { L2CA_DisconnectReq_from_rust(channel.cid) };
                self.handles.free_key(channel)?;
                Ok(())
            }
        }
    }
}
