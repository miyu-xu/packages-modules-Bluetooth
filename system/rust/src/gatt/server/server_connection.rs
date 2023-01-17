//! This module handles an individual connection on the ATT fixed channel.
//! It handles GATT transactions and unacknowledged operations, backed by an
//! AttDatabase (that may in turn be backed by an upper-layer protocol)

use std::{
    cell::{Cell, RefCell},
    rc::{Rc, Weak},
};

use log::{info, warn};
use tokio::task::spawn_local;

use crate::{
    gatt::{ids::AttHandle, server::transaction_handler::HACK_child_to_opcode},
    packets::{AttBuilder, AttErrorCode, AttErrorResponseBuilder, AttView, Packet},
    utils::owned_handle::OwnedHandle,
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
    send_packet: Box<dyn Fn(AttBuilder)>,
    mtu: Cell<usize>,
}

impl<T: AttDatabase + 'static> GattServerConnection<T> {
    /// Constructor, wrapping an ATT channel (for outgoing packets) and an AttDatabase
    pub fn new(db: T, send_packet: impl Fn(AttBuilder) + 'static) -> Rc<Self> {
        Self {
            curr_operation: GattServerOperation::Idle(GattRequestHandler::new(db)).into(),
            send_packet: Box::new(send_packet),
            mtu: 31.into(), // default ATT_MTU
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
                            this.send_response(AttBuilder {
                                opcode: HACK_child_to_opcode(&reply),
                                _child_: reply,
                            });
                            // ready for next transaction
                            this.curr_operation.replace(GattServerOperation::Idle(request_handler));
                        }
                    }
                });
                GattServerOperation::Pending(Some(task.into()))
            }
            GattServerOperation::Pending(_) => {
                warn!("multiple GATT operations cannot simultaneously take place, dropping one");
                let _child_ = AttErrorResponseBuilder {
                    opcode_in_error: packet.get_opcode(),
                    handle_in_error: AttHandle(0).into(),
                    error_code: AttErrorCode::UNLIKELY_ERROR,
                }
                .into();
                self.send_response(AttBuilder { opcode: HACK_child_to_opcode(&_child_), _child_ });
                curr_operation
            }
        });
    }

    fn send_response(self: &Rc<Self>, packet: AttBuilder) {
        (self.send_packet)(packet)
    }
}
