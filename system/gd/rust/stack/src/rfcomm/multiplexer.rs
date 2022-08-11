use tokio::sync::{
    mpsc::{channel, Receiver, Sender},
    oneshot,
};

use crate::l2cap::{owned_handle::OwnedHandle, types::IdentityAddress, L2cap, L2capChannel};

use super::{RfcommChannel, ServerChannel};

// TODO: handle clashes where both sides try to open the channel simultaneously (retry w/ random time interval)

enum RfcommRole {
    /// If we are initiating a connection, we need to create the channel ourselves
    Initiator,
    /// If we are the recipient of an RFCOMM connection, then a channel is already opened for us
    Recipient(L2capChannel),
}

/// This loop is only responsible for events that go outside each DLCI
/// e.g. initial connection setup, global flow control (if enabled), creating a new channel (and other operations on the control DLCI that are not channel-specific)
/// All other events are forwarded to the DLCI-multiplexer (which handles both control + data packets, to keep them in order)
/// Connection shutoff is done from each channel handler, and we peek at the outgoing packets to see if we need to take down the L2CAP channel
/// After we begin L2CAP takedown (synchronously), we can process no further events. TODO: now what?
async fn event_loop(role: RfcommRole, peer_addr: IdentityAddress, event_rx: Receiver<RfcommEvent>) {
    // before accepting events, we need to open the channel
    // if the channel fails to open, we will drop the receiver, so events will be rejected and callers will know to retry (as opposed to returning with an error)

    // match role {
    //     Initiator()
    // }
}

enum RfcommEvent {}

#[derive(Debug)]
pub struct Multiplexer {
    event_tx: Sender<RfcommEvent>,
    event_loop: OwnedHandle<()>,
}

impl Multiplexer {
    pub fn new(l2cap: L2cap, event_tx: Receiver<RfcommEvent>) -> Self {
        let event_loop = OwnedHandle::new(handle);
        Multiplexer { event_tx, event_loop }
    }
}
