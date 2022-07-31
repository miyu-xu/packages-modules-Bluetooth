#![allow(dead_code, unused, missing_docs)]
//! link management

mod bridge;

/// Wraps L2CAP functionality of legacy stack in an asynchronous API
use gddi::module;
use gddi::provides;
use gddi::Stoppable;
use std::sync::Arc;
use tokio::runtime::Runtime;

use self::bridge::ffi::L2CA_Register2;

module! {
    l2cap_module,
        providers {
        L2cap => provide_l2cap,
    },
}

#[derive(Clone, Stoppable)]
struct L2cap {}

#[provides]
async fn provide_l2cap(_rt: Arc<Runtime>) -> L2cap {
    L2cap {}
}

pub struct RawAddress {
    address: [u8; 6],
}

// see go/bluetooth-address for details
pub enum Address {
    // either a BR/EDR address, or the identity address of an LE device (guaranteed to be the same on dual-mode)
    Identity(RawAddress),
    // RPAs are
    ResolvablePrivate(RawAddress),
    NonResolvablePrivate(RawAddress),
}

struct L2capChannelId {
    psm: u16,
}

// a channel between a fixed pair of devices
struct L2capChannel {}

// a listening PSM for incoming connections
struct L2capServerChannel {}

impl L2cap {
    pub fn create_channel(_addr: Address, _channel: L2capChannelId) -> L2capChannel {
        // unsafe {
            // L2CA_Register2(2, std::ptr::null(), false, std::ptr::null(), 10, 10, 10);
        // }
        L2capChannel {}
    }

    pub fn listen_on(_addr: Address, _channel: L2capChannelId) -> L2capServerChannel {
        L2capServerChannel {}
    }
}
