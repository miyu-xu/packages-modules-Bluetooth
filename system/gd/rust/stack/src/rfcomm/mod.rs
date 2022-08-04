#![allow(dead_code, unused, missing_docs)]
//! link management

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

use crate::l2cap::L2cap;
use crate::l2cap::types::IdentityAddress;

module! {
    rfcomm_module,
        providers {
        Rfcomm => provide_rfcomm,
    },
}

#[derive(Clone, Stoppable)]
struct Rfcomm(Arc<RfcommContents>);

enum DLCI {
    Incoming(u8),
    Outgoing(u8),
}

struct RfcommContents {
    // l2cap_channels: HashMap<IdentityAddress, L2capChannel>,
}

#[provides]
async fn provide_rfcomm(rt: Arc<Runtime>, l2cap: L2cap) -> Rfcomm {
    Rfcomm(Arc::new(RfcommContents {}))
}

impl RfcommContents {}
