#![allow(dead_code, unused, missing_docs)]
//! link management

mod bridge;
mod listeners;
mod types;

use anyhow::{anyhow, Result};
use anyhow::{Context, Ok};
use cxx::UniquePtr;
use gddi::module;
use gddi::provides;
use gddi::Stoppable;
use std::cell::RefCell;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::runtime::Runtime;
use tokio::spawn;
use tokio::sync::mpsc::{channel, Receiver, Sender};
use tokio::sync::Mutex;
use tokio::sync::{oneshot, RwLock};

use self::bridge::ffi::{initialize_l2cap_tx_on_main_thread, L2CA_Register_from_rust};
use self::bridge::EventChannel;
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
    /// Mapping letting us demultiplex new connections into ServerChannels
    /// TODO: do we need a RwLock? Alternatives would be to broadcast into all channels (ugh) or to use some lock-free structure
    incoming_connection_txs: RwLock<HashMap<L2capPsm, Sender<L2capChannel>>>,
}

#[provides]
async fn provide_l2cap(rt: Arc<Runtime>, l2cap: L2cap) -> L2cap {
    let l2cap = Arc::new(L2capContents { incoming_connection_txs: RwLock::default() });

    // TODO: what if we get a ton of (data?) packets arriving? Should we drop them, block the main thread, or grow unbounded?
    let (mut tx, mut rx) = channel(64);
    unsafe { initialize_l2cap_tx_on_main_thread(&mut EventChannel(tx)) };

    let l2cap_clone = l2cap.clone();
    // start event loop
    spawn(async move {
        loop {
            let event = rx.recv().await.context("the l2cap event loop has been closed")?;
            let status = match event {
                CallbackEvent::IncomingConnection { incoming_addr, local_cid, psm } => {
                    let incoming_connection_txs = l2cap_clone.incoming_connection_txs.read().await;

                    incoming_connection_txs
                        .get_mut(&psm)
                        .with_context(|| {
                            format!(
                            "l2cap socket psm={psm:?} is not registered, dropping channel creation"
                        )
                        })?
                        .send(L2capChannel { local_cid })
                        .await
                    // .with_context(|| format!("l2cap socket psm={psm:?} has closed the incoming connection channel, dropping channel"))?;
                    // Ok(())
                }
            };
            if let Err(e) = status {
                log::error!("an error occurred while processing event {event:?}: {e:?} - event loop will continue");
            }
        }
        Ok(())
    });

    L2cap(l2cap)
}

impl L2cap {
    /// Defined in bt_target.h, mirrored here for interop
    const MTU_SIZE: u16 = 1691;

    pub fn create_channel(_addr: Address, channel: L2capChannelId) -> L2capChannel {
        L2capChannel { local_cid: channel }
    }

    pub async fn register_service(psm: L2capPsm) -> Result<L2capService> {
        let (mut tx, mut rx) = oneshot::channel();
        unsafe {
            L2CA_Register_from_rust(psm.psm, true, null_mut(), Self::MTU_SIZE, 0, &mut tx.into());
        }
        let psm = L2capPsm { psm: rx.await? };
        let (tx, rx) = channel(16);
        Ok(L2capService { psm, incoming_channel_rx: rx })
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
    local_cid: L2capChannelId,
}
