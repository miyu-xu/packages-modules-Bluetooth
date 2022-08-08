use std::{cell::RefCell, num::NonZeroU16};

use anyhow::{anyhow, Context, Result};
use tokio::sync::mpsc::Sender;

use crate::l2cap::types::L2capPsm;

use super::{
    bridge::EventChannel,
    types::{IdentityAddress, L2capChannelId, RawAddress},
};

/// These events are sent from the C++ main thread to the Rust runtime for dispatch
#[derive(Debug)]
pub enum CallbackEvent {
    /// An incoming L2CAP connection to a registered listener ("connection indication")
    IncomingConnectionEstablished {
        incoming_addr: IdentityAddress,
        local_cid: L2capChannelId,
        psm: L2capPsm,
    },
    /// An outgoing L2CAP connection to a registered listener ("connection response")
    OutgoingConnectionEstablished { local_cid: L2capChannelId },
    /// An error occurred tied to a particular channel ID ("connection response")
    ChannelError { local_cid: L2capChannelId, error_code: NonZeroU16 },
    /// Incoming (unparsed) data on an L2CAP channel
    IncomingData { local_cid: L2capChannelId, data: Box<[u8]> },
    /// An L2CAP channel was disconnected by the remote
    ChannelDisconnect { local_cid: L2capChannelId },
}

struct StaticHandlers(Sender<CallbackEvent>);

// These channels are used for interop with the L2CAP C layer, that expects function pointers
// Do not take them as good practice!!! Static variables should be avoided whenever possible,
// including use of the lazy_static crate! Consider using an instance variable on the module
// instead.
thread_local! {
    static STATIC_HANDLERS: RefCell<Option<StaticHandlers>> = RefCell::new(None);
}

fn with_static_handlers<T, F>(f: F) -> Result<T>
where
    F: FnOnce(&mut StaticHandlers) -> Result<T>,
{
    STATIC_HANDLERS.with(|x| {
        x.borrow_mut()
            .as_mut()
            .ok_or_else(|| anyhow!("event arrived but L2CAP stack not yet up, so cannot handle"))
            .and_then(f)
    })
}

fn log_errors<T>(f: impl FnOnce() -> Result<T>) {
    if let Err(err) = f() {
        log::error!("an error in an incoming callback was dropped: {}", err);
    }
}

pub fn initialize_l2cap_tx(tx: &mut EventChannel) {
    STATIC_HANDLERS.with(|handlers| {
        *handlers.borrow_mut() = Some(StaticHandlers(tx.0.clone()));
    });
}

/// Handles incoming connections to a registered listening PSM (interface: tL2CA_CONNECT_IND_CB)
pub fn incoming_connection_handler(remote_addr: &RawAddress, local_cid: u16, psm: u16, _id: u8) {
    log::info!("receiving incoming L2CAP connection from {remote_addr:?} to listener at {psm:?} with allocated lcid={local_cid:?}");

    log_errors(|| {
        with_static_handlers(|handlers| {
            handlers
                .0
                .blocking_send(CallbackEvent::IncomingConnectionEstablished {
                    incoming_addr: IdentityAddress(*remote_addr),
                    local_cid: L2capChannelId { cid: local_cid },
                    psm: L2capPsm { psm },
                })
                .with_context(|| {
                    format!("failed to enqueue incoming connection to {psm:?}, ignoring it")
                })
        })
    })
}

/// Triggered when an outgoing connection completes (interface: tL2CA_CONNECT_CFM_CB)
pub fn outgoing_connection_handler(local_cid: u16, result: u16) {
    log::info!("outgoing L2CAP connection potentially established at lcid={local_cid} (result={result} should be 0 for success)");

    let event = if result == 0 {
        CallbackEvent::OutgoingConnectionEstablished {
            local_cid: L2capChannelId { cid: local_cid },
        }
    } else {
        CallbackEvent::ChannelError {
            local_cid: L2capChannelId { cid: local_cid },
            error_code: NonZeroU16::new(result).unwrap(),
        }
    };
    log_errors(|| {
        with_static_handlers(|handlers| {
            handlers.0.blocking_send(event).with_context(|| {
                format!(
                    "failed to enqueue completed connection to channel {local_cid:?}, ignoring it"
                )
            })
        })
    });
}

/// Triggered when incoming data is available (interface: tL2CA_DATA_IND_CB)
pub fn incoming_data_handler(local_cid: u16, data: &[u8]) {
    log_errors(|| {
        with_static_handlers(|handlers| {
            handlers
                .0
                .blocking_send(CallbackEvent::IncomingData {
                    local_cid: L2capChannelId { cid: local_cid },
                    data: Box::from(data),
                })
                .with_context(|| {
                    format!("failed to enqueue new data from channel {local_cid:?}, DANGEROUSLY discarding it")
                })
        })
    })
}

pub fn disconnect_connection_handler(local_cid: u16, _should_ack: bool) {
    log_errors(|| {
        with_static_handlers(|handlers| {
            handlers
                .0
                .blocking_send(CallbackEvent::ChannelDisconnect {
                    local_cid: L2capChannelId { cid: local_cid },
                })
                .with_context(|| {
                    format!(
                    "failed to enqueue remote disconnection from channel {local_cid:?}, dropping it"
                )
                })
        })
    })
}
