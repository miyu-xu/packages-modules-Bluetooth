use std::{
    cell::RefCell,
    rc::{Rc, Weak},
};

use log::{info, warn};
use tokio::task::spawn_local;

use crate::{
    gatt::server::{att_database::AttHandle, request_handler::HACK_child_to_opcode},
    packets::{AttBuilder, AttErrorCode, AttErrorResponseBuilder, AttView},
    utils::owned_handle::OwnedHandle,
};

use super::{att_database::AttDatabase, request_handler::GattRequestHandler};

enum GattServerOperation<T: AttDatabase> {
    Idle(GattRequestHandler<T>),
    Pending(Option<OwnedHandle<()>>),
}

pub struct GattServerConnection<T: AttDatabase> {
    curr_operation: RefCell<GattServerOperation<T>>,
    send_packet: Box<dyn Fn(AttBuilder)>,
}

impl<T: AttDatabase + 'static> GattServerConnection<T> {
    pub fn new(db: Rc<T>, send_packet: impl Fn(AttBuilder) + 'static) -> Rc<Self> {
        Self {
            curr_operation: GattServerOperation::Idle(GattRequestHandler::new(db)).into(),
            send_packet: Box::new(send_packet),
        }
        .into()
    }

    pub fn try_handle_request(self: &Rc<Self>, packet: AttView<'_>) {
        let curr_operation = self.curr_operation.replace(GattServerOperation::Pending(None));
        self.clone().curr_operation.replace(match curr_operation {
            GattServerOperation::Idle(mut request_handler) => {
                let this = Rc::downgrade(self);
                let packet = packet.to_owned();
                let task = spawn_local(async move {
                    info!("starting GATT transaction");
                    let reply = request_handler.process_packet(packet.view()).await;
                    match Weak::upgrade(&this) {
                        None => {
                            warn!("callback returned after disconnect");
                        }
                        Some(this) => {
                            match reply {
                                Ok(child) => {
                                    info!("sending reply packet");
                                    this.send_response(AttBuilder {
                                        opcode: HACK_child_to_opcode(&child),
                                        command_flag: 0,
                                        _child_: child,
                                    });
                                }
                                Err(err) => {
                                    warn!("{err}");
                                }
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
                let _child_ = AttErrorResponseBuilder {
                    opcode_in_error: packet.get_opcode(),
                    command_flag_in_error: packet.get_command_flag(),
                    handle_in_error: AttHandle(0).into(),
                    error_code: AttErrorCode::UNLIKELY_ERROR,
                }
                .into();
                self.send_response(AttBuilder {
                    opcode: HACK_child_to_opcode(&_child_),
                    command_flag: 0,
                    _child_,
                });
                curr_operation
            }
        });
    }

    fn send_response(self: &Rc<Self>, packet: AttBuilder) {
        (self.send_packet)(packet)
    }
}
