use std::time::Duration;

use log::{trace, warn};
use tokio::{
    sync::mpsc::{self, error::TrySendError},
    time::timeout,
};

use crate::{
    gatt::ids::AttHandle,
    packets::{AttAttributeDataChild, AttChild, AttHandleValueIndicationBuilder},
    utils::packet::build_att_data,
};

use super::att_server_bearer::SendError;

pub struct IndicationHandler {
    pending_confirmation: mpsc::Receiver<()>,
}

impl IndicationHandler {
    pub fn new() -> (Self, ConfirmationWatcher) {
        let (tx, rx) = mpsc::channel(1);
        (Self { pending_confirmation: rx }, ConfirmationWatcher(tx))
    }

    pub async fn send(
        &mut self,
        handle: AttHandle,
        data: AttAttributeDataChild,
        send_packet: impl Fn(AttChild) -> Result<(), SendError>,
    ) -> Result<(), SendError> {
        // flushing any confirmations that arrived before we sent the next indication
        let _ = self.pending_confirmation.try_recv();

        send_packet(
            AttHandleValueIndicationBuilder { handle: handle.into(), value: build_att_data(data) }
                .into(),
        )?;

        match timeout(Duration::from_secs(30), self.pending_confirmation.recv()).await {
            Ok(Some(())) => Ok(()),
            Ok(None) => Err(SendError::ConnectionDropped),
            Err(_) => {
                warn!("Sent indication but received no response for 30s");
                Err(SendError::ConnectionDropped)
            }
        }
    }
}

pub struct ConfirmationWatcher(mpsc::Sender<()>);

impl ConfirmationWatcher {
    pub fn on_confirmation(&mut self) {
        match self.0.try_send(()) {
            Ok(_) => {
                trace!("Got AttHandleValueConfirmation")
            }
            Err(TrySendError::Full(_)) => {
                warn!("Got a second AttHandleValueConfirmation before the first was processed, dropping it")
            }
            Err(TrySendError::Closed(_)) => {
                warn!("Got an AttHandleValueConfirmation while no indications are outstanding, dropping it")
            }
        }
    }
}
