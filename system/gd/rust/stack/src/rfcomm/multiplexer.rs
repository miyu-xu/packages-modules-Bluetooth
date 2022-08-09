use futures::channel::mpsc::unbounded;
use tokio::oneshot;
use tokio::sync::mpsc::{UnboundedReceiver, UnboundedSender};

use crate::l2cap::types::IdentityAddress;

use super::ServerChannel;

enum RfcommEvent {
    /// Whenever a new channel
    Listen {
        channel: ServerChannel,
        ack: UnboundedSender<RfcommChannel>,
    },
    Connect {
        channel: ServerChannel,
        ack: oneshot::Sender<RfcommChannel>,
    },
}

struct Multiplexer {
    event_tx: UnboundedSender<RfcommEvent>,
}

async fn event_loop(peer_addr: IdentityAddress, event_rx: UnboundedReceiver<RfcommEvent>) {
    // before accepting events, we need to open the channel
    // if the channel fails to open, we will drop the receiver, so 
    loop {}
}

impl Multiplexer {
    fn new() {
        (event_tx, event_rx) = unbounded();
        Multiplexer { event_tx }
    }
}
