//! Anything related to the Qualification API (IBluetoothQA).

use crate::Message;
use bt_topshim::btif::BtDiscMode;
use tokio::sync::mpsc::Sender;

/// Defines the Qualification API
pub trait IBluetoothQA {
    fn add_media_player(&self, name: String, browsing_supported: bool);

    fn get_discoverable_mode(&self) -> BtDiscMode;
}

pub struct BluetoothQA {
    tx: Sender<Message>,
    disc_mode: BtDiscMode,
}

impl BluetoothQA {
    pub fn new(tx: Sender<Message>, disc_mode: BtDiscMode) -> BluetoothQA {
        BluetoothQA { tx, disc_mode }
    }

    pub fn qa_on_discoverable_mode_changed(&mut self, mode: BtDiscMode) {
        self.disc_mode = mode;
    }
}

impl IBluetoothQA for BluetoothQA {
    fn add_media_player(&self, name: String, browsing_supported: bool) {
        let txl = self.tx.clone();
        tokio::spawn(async move {
            let _ = txl.send(Message::QaAddMediaPlayer(name, browsing_supported)).await;
        });
    }

    fn get_discoverable_mode(&self) -> BtDiscMode {
        self.disc_mode.clone()
    }
}
