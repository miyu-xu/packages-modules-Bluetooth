#![allow(dead_code, unused, missing_docs)]
//! link management

mod multiplexer;

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

use std::ptr::null_mut;

use crate::l2cap::types::IdentityAddress;
use crate::l2cap::L2cap;

module! {
    rfcomm_module,
        providers {
        Rfcomm => provide_rfcomm,
    },
}

const RFCOMM_PSM: u8 = 3;

#[derive(Clone, Stoppable)]
struct Rfcomm(Arc<RfcommContents>);

struct ServerChannel {
    cid: u8,
}

enum DLCI {
    Incoming(ServerChannel),
    Outgoing(ServerChannel),
}

struct RfcommContents {
    l2cap: L2cap,
    l2cap_channels: HashMap<IdentityAddress, L2capChannel>,
}

#[provides]
async fn provide_rfcomm(rt: Arc<Runtime>, l2cap: L2cap) -> Rfcomm {
    Rfcomm(Arc::new(RfcommContents { l2cap, l2cap_channels: HashMap::new() }))
}

impl Rfcomm {
    pub async fn connect(
        &self,
        addr: IdentityAddress,
        channel: ServerChannel,
    ) -> Result<RfcommChannel> {
        self.l2cap
            .create_channel(RFCOMM_PSM, target)
            .await?
            .context("failed to register RFCOMM l2cap channel");
    }
}

struct RfcommChannel {}
