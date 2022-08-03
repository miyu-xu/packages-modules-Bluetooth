#![allow(dead_code, unused, missing_docs)]
//! link management

mod bridge;
mod demultiplexer;
mod listeners;
mod types;

use anyhow::{anyhow, Result};
use anyhow::{Context, Ok};
use cxx::UniquePtr;
use gddi::module;
use gddi::provides;
use gddi::Stoppable;
use log::warn;
use std::cell::RefCell;
use std::collections::HashMap;
use std::mem::{discriminant, Discriminant};
use std::sync::Arc;
use tokio::runtime::Runtime;
use tokio::spawn;
use tokio::sync::mpsc::{channel, Receiver, Sender};
use tokio::sync::Mutex;
use tokio::sync::{oneshot, RwLock};

use self::bridge::ffi::{initialize_l2cap_tx_on_main_thread, L2CA_Register_from_rust};
use self::bridge::EventChannel;
use self::demultiplexer::Demultiplexer;
use self::listeners::CallbackEvent;
use self::types::{Address, L2capChannelId, L2capPsm};
use std::ptr::null_mut;

module! {
    l2cap_module,
        providers {
        L2cap => provide_l2cap,
    },
}

#[derive(Clone, Stoppable)]
struct L2cap(Arc<L2capContents>);

struct L2capContents {
    incoming_connection_demultiplexer: Demultiplexer<L2capChannel, L2capPsm>,
}

#[provides]
async fn provide_l2cap(rt: Arc<Runtime>, l2cap: L2cap) -> L2cap {
    // TODO: what if we get a ton of (data?) packets arriving? Should we drop them, block the main thread, or grow unbounded?
    let (mut callback_tx, callback_rx) = channel(64);
    unsafe { initialize_l2cap_tx_on_main_thread(&mut EventChannel(callback_tx)) };

    let (mut incoming_connection_tx, incoming_connection_rx) = channel(8);
    let incoming_connection_demultiplexer =
        Demultiplexer::new(incoming_connection_rx, |incoming_connection: &L2capChannel| {
            incoming_connection.psm
        });

    // start event loop dispatching callbacks to handlers for each event type
    spawn(incoming_event_demultiplexer(callback_rx, incoming_connection_tx));

    L2cap(Arc::new(L2capContents { incoming_connection_demultiplexer }))
}

async fn incoming_event_demultiplexer(
    mut rx: Receiver<CallbackEvent>,
    incoming_connection_tx: Sender<L2capChannel>,
) {
    loop {
        let event = rx.recv().await;
        match event {
            None => {
                warn!("the l2cap callback event loop has been closed");
                return;
            }
            Some(event) => {
                let status = match event {
                    CallbackEvent::IncomingConnection { incoming_addr, local_cid, psm } => {
                        incoming_connection_tx
                            .send(L2capChannel { psm, local_cid })
                            .await
                            .context("failed to dispatch incoming connection event")
                    }
                };
                if let Err(e) = status {
                    log::error!("an error occurred while processing event {event:?}: {e:?} - event loop will continue");
                }
            }
        };
    }
}

impl L2cap {
    /// Defined in bt_target.h, mirrored here for interop
    const MTU_SIZE: u16 = 1691;

    // pub fn create_channel(_addr: Address, channel: L2capChannelId) -> L2capChannel {
    //     L2capChannel { local_cid: channel }
    // }

    pub async fn register_service(&self, psm: L2capPsm) -> Result<L2capService> {
        let (mut tx, mut rx) = oneshot::channel();
        unsafe {
            L2CA_Register_from_rust(psm.psm, true, null_mut(), Self::MTU_SIZE, 0, &mut tx.into());
        }
        let psm = L2capPsm { psm: rx.await? };
        let incoming_channel_rx =
            self.0.incoming_connection_demultiplexer.subscribe(psm).await.with_context(|| {
                format!("failed to subscribed to psm {psm:?} in the multiplexer")
            })?;
        Ok(L2capService { psm, incoming_channel_rx })
    }
}

// a listening PSM for incoming connections
#[derive(Debug)]
struct L2capService {
    /// The allocated PSM (typically the same as the requested PSM)
    psm: L2capPsm,
    /// New channels opened by a remote device
    incoming_channel_rx: Receiver<L2capChannel>,
}

impl L2capService {
    async fn accept(&mut self) -> Option<L2capChannel> {
        self.incoming_channel_rx.recv().await
    }
}

// a channel between a fixed pair of devices
#[derive(Debug)]
struct L2capChannel {
    psm: L2capPsm, // TODO: do all l2cap channels have psms? or only fixed ones?
    local_cid: L2capChannelId,
}
