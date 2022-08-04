use super::{
    demultiplexer::Demultiplexer, listeners::CallbackEvent, types::L2capChannelId, ChannelError,
    IncomingConnection, IncomingData, L2capChannel,
};
use anyhow::{Context, Result};
use log::{info, warn};
use tokio::sync::mpsc::{channel, Receiver, Sender};

/// This module takes in serialized CallbackEvents from the C layer dispatched from the main thread,
/// and demultiplexes them onto the appropriate channels while updating some state synchronously.
///
/// These state updates are why the C layer cannot directly dispatch onto the relevant channels.
/// For example, imagine a channel creation followed immediately by a data packet.
/// If these packets were put onto separate queues, then we could not guarantee that a channel data
/// listener would be registered before the data packet arrives.

pub(super) struct L2capControlDemultiplexer {
    // the event rx that we are demultiplexing
    rx: Receiver<CallbackEvent>,
    // these are connected to demultiplexers that dispatch to the relevant listeners
    // they live on the main thread so we can register listeners from the module
    incoming_connection_tx: Sender<IncomingConnection>,
    outgoing_connection_tx: Sender<L2capChannel>,
    error_tx: Sender<ChannelError>,
    // used to demultiplex data packets
    // we own it so we can synchronously register channels as they are created
    data_demultiplexer: Demultiplexer<IncomingData, L2capChannelId>,
    data_tx: Sender<IncomingData>,
}

impl L2capControlDemultiplexer {
    pub(super) fn new(
        rx: Receiver<CallbackEvent>,
        incoming_connection_tx: Sender<IncomingConnection>,
        outgoing_connection_tx: Sender<L2capChannel>,
        error_tx: Sender<ChannelError>,
    ) -> L2capControlDemultiplexer {
        let (data_tx, data_rx) = channel(64);
        L2capControlDemultiplexer {
            rx,
            incoming_connection_tx,
            outgoing_connection_tx,
            error_tx,
            data_demultiplexer: Demultiplexer::new(data_rx, |packet: &IncomingData| {
                packet.local_cid
            }),
            data_tx,
        }
    }

    // the core event loop of this task
    // note that we take ownership of self so we automatically destruct on exit
    pub async fn start(mut self) {
        loop {
            let event = self.rx.recv().await;
            match event {
                None => {
                    warn!("the l2cap callback event loop has been closed");
                    return;
                }
                Some(event) => {
                    let status = self.process_incoming_event(event).await;
                    if let Err(e) = status {
                        log::error!("an error occurred while processing event: {e:?} - event loop will continue");
                    }
                }
            };
        }
    }

    async fn register_data_channel(
        &mut self,
        local_cid: L2capChannelId,
    ) -> Result<Receiver<IncomingData>> {
        self.data_demultiplexer
            .subscribe(local_cid)
            .await
            .with_context(|| format!("failed to register cid {local_cid:?} in data demultiplexer"))
    }

    async fn process_incoming_event(&mut self, event: CallbackEvent) -> Result<()> {
        match event {
            CallbackEvent::IncomingConnectionEstablished { incoming_addr, local_cid, psm } => {
                let data_rx = self.register_data_channel(local_cid).await?;
                self.incoming_connection_tx
                    .send(IncomingConnection {
                        local_psm: psm,
                        channel: L2capChannel { local_cid, data_rx },
                    })
                    .await
                    .context("failed to dispatch incoming connection event")
            }
            CallbackEvent::OutgoingConnectionEstablished { local_cid } => {
                let data_rx = self.register_data_channel(local_cid).await?;
                self.outgoing_connection_tx
                    .send(L2capChannel { local_cid, data_rx })
                    .await
                    .context("failed to dispatch established outgoing connection event")
            }
            CallbackEvent::ChannelError { local_cid, error_code } => self
                .error_tx
                .send(ChannelError { local_cid, error_code })
                .await
                .context("failed to dispatch connection error event"),
            CallbackEvent::IncomingData { local_cid, data } => self
                .data_tx
                .send(IncomingData { local_cid, data })
                .await
                .context("failed to dispatch incoming data event"),
        }
    }
}
