//! Anything related to the Qualification API (IBluetoothQA).

use crate::Message;
use tokio::sync::mpsc::Sender;

/// Defines the Qualification API
pub trait IBluetoothQA {
    fn rfcomm_start_control_request(&self, dlci: u8, addr: String);
}

pub struct BluetoothQA {
    tx: Sender<Message>,
}

impl BluetoothQA {
    pub fn new(tx: Sender<Message>) -> BluetoothQA {
        BluetoothQA { tx }
    }
}

impl IBluetoothQA for BluetoothQA {
    fn rfcomm_start_control_request(&self, dlci: u8, addr: String) {
        let txl = self.tx.clone();
        tokio::spawn(async move {
            let _ = txl.send(Message::QaRfcommStartControlRequest(dlci, addr)).await;
        });
    }
}
