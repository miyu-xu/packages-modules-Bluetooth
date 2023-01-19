use bluetooth_core::{
    gatt::{channel::AttTransport, ids::TransportIndex},
    packets::{AttBuilder, Serializable, SerializeError},
};
use tokio::sync::mpsc::{self, unbounded_channel, UnboundedReceiver};

pub struct MockAttTransport(mpsc::UnboundedSender<(TransportIndex, AttBuilder)>);

impl MockAttTransport {
    pub fn new() -> (Self, UnboundedReceiver<(TransportIndex, AttBuilder)>) {
        let (tx, rx) = unbounded_channel();
        (Self(tx), rx)
    }
}

impl AttTransport for MockAttTransport {
    fn send_packet(
        &self,
        tcb_idx: TransportIndex,
        packet: AttBuilder,
    ) -> Result<(), SerializeError> {
        packet.to_vec()?; // trigger SerializeError if needed
        Ok(self.0.send((tcb_idx, packet)).unwrap())
    }
}
