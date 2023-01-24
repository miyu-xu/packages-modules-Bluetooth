//! This module handles an individual connection on the ATT fixed channel.
//! It handles GATT transactions and unacknowledged operations, backed by an
//! AttDatabase (that may in turn be backed by an upper-layer protocol)

use std::{
    cell::{Cell, RefCell},
    rc::{Rc, Weak},
};

use log::{error, info, warn};
use tokio::task::spawn_local;

use crate::{
    gatt::ids::AttHandle,
    packets::{
        AttBuilder, AttChild, AttErrorCode, AttErrorResponseBuilder, AttView, Packet,
        SerializeError,
    },
    utils::{owned_handle::OwnedHandle, packet::HACK_child_to_opcode},
};

use super::{att_database::AttDatabase, transaction_handler::GattRequestHandler};

enum GattServerOperation<T: AttDatabase> {
    Idle(GattRequestHandler<T>),
    Pending(Option<OwnedHandle<()>>),
}

/// This represents a single ATT bearer (currently, always the unenhanced fixed channel on LE)
/// The GattServerOperation ensures that only one transaction can take place at a time
pub struct GattServerConnection<T: AttDatabase> {
    curr_operation: RefCell<GattServerOperation<T>>,
    send_packet: Box<dyn Fn(AttBuilder) -> Result<(), SerializeError>>,
    mtu: Cell<usize>,
}

impl<T: AttDatabase + 'static> GattServerConnection<T> {
    /// Constructor, wrapping an ATT channel (for outgoing packets) and an AttDatabase
    pub fn new(
        db: T,
        send_packet: impl Fn(AttBuilder) -> Result<(), SerializeError> + 'static,
    ) -> Rc<Self> {
        Self {
            curr_operation: GattServerOperation::Idle(GattRequestHandler::new(db)).into(),
            send_packet: Box::new(send_packet),
            mtu: 23.into(), // default ATT_MTU
        }
        .into()
    }

    /// Handle an incoming packet, and send outgoing packets as appropriate
    /// using the owned ATT channel.
    pub fn handle_packet(self: &Rc<Self>, packet: AttView<'_>) {
        let curr_operation = self.curr_operation.replace(GattServerOperation::Pending(None));
        self.clone().curr_operation.replace(match curr_operation {
            GattServerOperation::Idle(mut request_handler) => {
                // even if the MTU is updated afterwards, 5.3 3F 3.4.2.2 states that we should use the MTU at the request-time
                let mtu = self.mtu.get();
                let this = Rc::downgrade(self);
                let packet = packet.to_owned_packet();
                let task = spawn_local(async move {
                    info!("starting GATT transaction");
                    let reply = request_handler.process_packet(packet.view(), mtu).await;
                    match Weak::upgrade(&this) {
                        None => {
                            warn!("callback returned after disconnect");
                        }
                        Some(this) => {
                            info!("sending reply packet");
                            if let Err(err) = this.send_response(reply) {
                                error!("serializer failure {err:?}, dropping packet and sending failed reply");
                                this.send_response(AttErrorResponseBuilder {
                                    opcode_in_error: packet.view().get_opcode(),
                                    handle_in_error: AttHandle(0).into(),
                                    error_code: AttErrorCode::UNLIKELY_ERROR,
                                }).expect("packet should never fail to serialize");
                            }
                            // ready for next transaction
                            this.curr_operation.replace(GattServerOperation::Idle(request_handler));
                        }
                    }
                });
                GattServerOperation::Pending(Some(task.into()))
            }
            GattServerOperation::Pending(_) => {
                warn!("multiple GATT operations cannot simultaneously take place, dropping one");
                self.send_response(AttErrorResponseBuilder {
                    opcode_in_error: packet.get_opcode(),
                    handle_in_error: AttHandle(0).into(),
                    error_code: AttErrorCode::UNLIKELY_ERROR,
                }).expect("packet should never fail to serialize");
                curr_operation
            }
        });
    }

    fn send_response(self: &Rc<Self>, packet: impl Into<AttChild>) -> Result<(), SerializeError> {
        let child = packet.into();
        let packet = AttBuilder { opcode: HACK_child_to_opcode(&child), _child_: child };
        (self.send_packet)(packet)
    }
}

#[cfg(test)]
mod test {
    use tokio::{
        runtime::Runtime,
        sync::mpsc::{unbounded_channel, UnboundedReceiver},
        task::LocalSet,
    };

    use super::*;

    use crate::{
        gatt::server::{
            att_database::{AttAttribute, AttPermissions, AttUuid},
            test::test_att_db::TestAttDatabase,
        },
        packets::{AttOpcode, AttReadRequestBuilder},
        utils::packet::build_att_view_or_crash,
    };

    const VALID_HANDLE: AttHandle = AttHandle(3);
    const INVALID_HANDLE: AttHandle = AttHandle(4);

    fn open_connection(
    ) -> (Rc<GattServerConnection<TestAttDatabase>>, UnboundedReceiver<AttBuilder>) {
        let db = TestAttDatabase::new(vec![(
            AttAttribute {
                handle: VALID_HANDLE,
                uuid: AttUuid::new([1, 2, 3, 4]),
                permissions: AttPermissions { readable: true, writable: false },
            },
            vec![5, 6],
        )]);
        let (tx, rx) = unbounded_channel();
        let conn = GattServerConnection::new(db, move |packet| {
            tx.send(packet).unwrap();
            Ok(())
        });
        (conn, rx)
    }

    #[test]
    fn test_single_transaction() {
        LocalSet::new().block_on(&Runtime::new().unwrap(), async {
            let (conn, mut rx) = open_connection();
            conn.handle_packet(
                build_att_view_or_crash(AttReadRequestBuilder {
                    attribute_handle: VALID_HANDLE.into(),
                })
                .view(),
            );
            assert_eq!(rx.recv().await.unwrap().opcode, AttOpcode::READ_RESPONSE)
        });
    }

    #[test]
    fn test_sequential_transactions() {
        LocalSet::new().block_on(&Runtime::new().unwrap(), async {
            let (conn, mut rx) = open_connection();
            conn.handle_packet(
                build_att_view_or_crash(AttReadRequestBuilder {
                    attribute_handle: INVALID_HANDLE.into(),
                })
                .view(),
            );
            assert_eq!(rx.recv().await.unwrap().opcode, AttOpcode::ERROR_RESPONSE);

            conn.handle_packet(
                build_att_view_or_crash(AttReadRequestBuilder {
                    attribute_handle: VALID_HANDLE.into(),
                })
                .view(),
            );
            assert_eq!(rx.recv().await.unwrap().opcode, AttOpcode::READ_RESPONSE);
        });
    }

    #[test]
    fn test_concurrent_transaction_failure() {
        // TODO(b/255880936) - Add this test once GATT callbacks are available
    }
}
