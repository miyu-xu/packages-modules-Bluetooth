//! Anything related to the Qualification API (IBluetoothQA).

use crate::Message;
use tokio::sync::mpsc::Sender;

/// Defines the Qualification API
pub trait IBluetoothQA {
    fn enable_a2dp_sink(&self);
    fn send_avrcp_pass_through(&self, address: String, key_code: u8, key_state: u8);
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
    fn enable_a2dp_sink(&self) {
        let txl = self.tx.clone();
        tokio::spawn(async move {
            let _ = txl.send(Message::QaEnableA2dpSink).await;
        });
    }

    fn send_avrcp_pass_through(&self, address: String, key_code: u8, key_state: u8) {
        let txl = self.tx.clone();
        tokio::spawn(async move {
            let _ = txl.send(Message::QaSendAvrcpPassThrough(address, key_code, key_state)).await;
        });
    }
}
