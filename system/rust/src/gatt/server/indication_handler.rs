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
        send_packet: impl FnOnce(AttChild) -> Result<(), SendError>,
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
    pub fn on_confirmation(&self) {
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

#[cfg(test)]
mod test {
    use tokio::{sync::oneshot, task::spawn_local};

    use crate::utils::task::block_on_locally;

    use super::*;

    const HANDLE: AttHandle = AttHandle(1);

    fn get_data() -> AttAttributeDataChild {
        AttAttributeDataChild::RawData([1, 2, 3].into())
    }

    #[test]
    fn test_indication_sent() {
        block_on_locally(async move {
            // arrange
            let (mut indication_handler, _confirmation_watcher) = IndicationHandler::new();
            let (tx, rx) = oneshot::channel();

            // act: send an indication
            spawn_local(async move {
                indication_handler
                    .send(HANDLE, get_data(), move |packet| {
                        tx.send(packet).unwrap();
                        Ok(())
                    })
                    .await
            });

            // assert: that an AttHandleValueIndication was sent on the channel
            let AttChild::AttHandleValueIndication(indication) = rx.await.unwrap() else {
                unreachable!()
            };
            assert_eq!(
                indication,
                AttHandleValueIndicationBuilder {
                    handle: HANDLE.into(),
                    value: build_att_data(get_data()),
                }
            );
        });
    }

    #[test]
    fn test_confirmation_handled() {
        block_on_locally(async move {
            // arrange
            let (mut indication_handler, confirmation_watcher) = IndicationHandler::new();
            let (tx, rx) = oneshot::channel();

            // act: send an indication
            let pending_result = spawn_local(async move {
                indication_handler
                    .send(HANDLE, get_data(), move |packet| {
                        tx.send(packet).unwrap();
                        Ok(())
                    })
                    .await
            });
            // when the indication is sent, send a confirmation in response
            rx.await.unwrap();
            confirmation_watcher.on_confirmation();

            // assert: the indication was successfully sent
            assert!(matches!(pending_result.await.unwrap(), Ok(())));
        });
    }

    #[test]
    fn test_unblock_on_disconnect() {
        block_on_locally(async move {
            // arrange
            let (mut indication_handler, confirmation_watcher) = IndicationHandler::new();
            let (tx, rx) = oneshot::channel();

            // act: send an indication
            let pending_result = spawn_local(async move {
                indication_handler
                    .send(HANDLE, get_data(), move |packet| {
                        tx.send(packet).unwrap();
                        Ok(())
                    })
                    .await
            });
            // when the indication is sent, drop the confirmation watcher (as would happen upon a disconnection)
            rx.await.unwrap();
            drop(confirmation_watcher);

            // assert: we get the appropriate error
            assert!(matches!(pending_result.await.unwrap(), Err(SendError::ConnectionDropped)));
        });
    }

    #[test]
    fn test_spurious_confirmations() {
        block_on_locally(async move {
            // arrange: send a few confirmations in advance
            let (mut indication_handler, confirmation_watcher) = IndicationHandler::new();
            let (tx, rx) = oneshot::channel();
            confirmation_watcher.on_confirmation();
            confirmation_watcher.on_confirmation();

            // act: send an indication
            let pending_result = spawn_local(async move {
                indication_handler
                    .send(HANDLE, get_data(), move |packet| {
                        tx.send(packet).unwrap();
                        Ok(())
                    })
                    .await
            });
            // when the indication is sent, drop the confirmation watcher (so we won't block forever)
            rx.await.unwrap();
            drop(confirmation_watcher);

            // assert: we get the appropriate error, rather than an Ok(())
            // (which would have been the case if we had processed the spurious confirmations)
            assert!(matches!(pending_result.await.unwrap(), Err(SendError::ConnectionDropped)));
        });
    }
}
