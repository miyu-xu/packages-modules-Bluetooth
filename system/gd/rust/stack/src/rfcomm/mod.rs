#![allow(dead_code, unused, missing_docs)]
//! rfcomm

mod multiplexer;
mod packet_parse;

use anyhow::{anyhow, bail, Context, Ok, Result};
use cxx::UniquePtr;
use gddi::module;
use gddi::provides;
use gddi::Stoppable;
use log::warn;
use std::cell::RefCell;
use std::collections::hash_map::Entry;
use std::collections::HashMap;
use std::convert::TryFrom;
use std::mem::{discriminant, Discriminant};
use std::num::NonZeroU8;
use std::sync::Arc;
use tokio::runtime::Runtime;
use tokio::spawn;
use tokio::sync::mpsc::{channel, Receiver, Sender};
use tokio::sync::Mutex;
use tokio::sync::{oneshot, RwLock};

use std::ptr::null_mut;

use crate::l2cap::demultiplexer::DemultiplexedReceiver;
use crate::l2cap::demultiplexer::Demultiplexer;
use crate::l2cap::owned_handle::OwnedHandle;
use crate::l2cap::types::{IdentityAddress, L2capPsm};
use crate::l2cap::L2cap;
use crate::l2cap::L2capChannel;
use crate::l2cap::L2capService;
use crate::link::acl::classic::ConnectionEvent;

use self::multiplexer::MultiplexerHandle;
use self::multiplexer::RfcommRole;

module! {
    rfcomm_module,
        providers {
        Rfcomm => provide_rfcomm,
    },
}

const RFCOMM_PSM: L2capPsm = L2capPsm { psm: 3 };

#[derive(Clone, Stoppable)]
struct Rfcomm(Arc<RfcommContents>);

#[derive(Clone, Copy, Hash, Eq, PartialEq, Debug)]
struct ServerChannelId {
    cid: NonZeroU8,
}

#[derive(Clone, Copy, Hash, Eq, PartialEq, Debug)]
enum Dlci {
    Control,
    ForInitiator(ServerChannelId),
    ForResponder(ServerChannelId),
}

impl TryFrom<u8> for Dlci {
    type Error = anyhow::Error;
    fn try_from(value: u8) -> Result<Self> {
        match value {
            0 => Ok(Dlci::Control),
            1 => bail!("1 is not a valid DLCI"),
            _ if value >= 62 => bail!("DLCIs are 0 or within [2, 61], {value} is invalid"),
            value if value % 2 == 0 => Ok(Dlci::ForResponder(ServerChannelId {
                cid: NonZeroU8::new(value / 2).expect("impossible, protected by match"),
            })),
            _ => Ok(Dlci::ForInitiator(ServerChannelId {
                cid: NonZeroU8::new(value / 2).expect("impossible, protected by match"),
            })),
        }
    }
}

struct RfcommContents {
    l2cap: L2cap,
    event_tx: Sender<RfcommConnectionEvent>,
    global_task_handle: OwnedHandle<()>,
}

#[provides]
async fn provide_rfcomm(rt: Arc<Runtime>, l2cap: L2cap) -> Rfcomm {
    let service = l2cap.register_service(RFCOMM_PSM).await.unwrap();
    let (event_tx, event_rx) = channel(16);
    let global_task_handle =
        tokio::spawn(GlobalRfcommMultiplexer::new(l2cap.clone(), service, event_rx).start()).into();
    Rfcomm(Arc::new(RfcommContents { l2cap, global_task_handle, event_tx }))
}

enum RfcommConnectionEvent {
    Listen { channel: ServerChannelId, ack: Sender<RfcommService> },
    Connect { channel: ServerChannelId, addr: IdentityAddress, ack: oneshot::Sender<RfcommChannel> },
}

#[derive(Debug)]
struct RfcommService {
    channel_id: ServerChannelId,
    channel_rx: DemultiplexedReceiver<ServerChannelId, RfcommChannel>,
}

#[derive(Debug)]
struct RfcommChannel {
    channel_id: Dlci,
}

/// This task is responsible for coordinating port listeners and device-specific multiplexers
struct GlobalRfcommMultiplexer {
    /// to create outgoing L2cap connections
    l2cap: L2cap,
    /// to receive incoming L2cap connections
    service: L2capService,
    /// to receive control signals from clients
    event_rx: Receiver<RfcommConnectionEvent>,
    /// active multiplexers for connected devices
    multiplexers: HashMap<IdentityAddress, MultiplexerHandle>,
    /// a demultiplexer to dispatch newly created channels to listeners
    listeners: Demultiplexer<RfcommChannel, ServerChannelId>,
}

impl GlobalRfcommMultiplexer {
    fn new(l2cap: L2cap, service: L2capService, event_rx: Receiver<RfcommConnectionEvent>) -> Self {
        Self {
            l2cap,
            service,
            event_rx,
            multiplexers: HashMap::new(),
            listeners: todo!(),
            // listeners: Demultiplexer::new(|channel: &RfcommChannel| channel.channel_id),
        }
    }

    async fn start(mut self) {
        // we need to be careful about handling connection requests while a multiplexer _exists_, *but is shutting down*
        // multiplexer teardown concludes with shutdown of the L2CAP connection, after which we will re-establish
        // TODO: listen on the L2capService and select! on it
        loop {
            match self.event_rx.recv().await {
                None => return, /* the RFCOMM service has stopped, apparently */
                Some(event) => self.handle_local_event(event).await,
            };
        }
    }

    async fn handle_local_event(&mut self, event: RfcommConnectionEvent) -> Result<()> {
        match event {
            // we don't have to notify any of the active multiplexers, they will discover the
            // subscribed listener when the remote device makes a connection and we attempt to send
            // the new channel through the demultiplexer
            RfcommConnectionEvent::Listen { channel, ack } => {
                self.listeners.subscribe(channel).await?;
                Ok(())
            }
            RfcommConnectionEvent::Connect { channel, addr, ack } => {
                match self.multiplexers.entry(addr) {
                    Entry::Occupied(entry) => todo!("need to figure out how to handle connections when the d-mux is shutting down"),
                    Entry::Vacant(entry) => {
                        entry.insert(MultiplexerHandle::new(RfcommRole::Initiator(addr), self.l2cap.clone()));
                        todo!("now what? the multiplexer has started, but like we need to get the connection back somehow")
                    },
                }
            }
        }
    }
}

impl Rfcomm {
    pub async fn connect(
        &self,
        addr: IdentityAddress,
        channel: ServerChannelId,
    ) -> Result<RfcommChannel> {
        todo!()
        // self.0
        //     .l2cap
        //     .create_channel(RFCOMM_PSM, addr)
        //     .await
        //     .context("failed to register RFCOMM l2cap channel")
    }

    // pub async fn accept(&self, channel: ServerChannelId) -> Result<RfcommChannel> {
    //     todo!()
    // }
}
