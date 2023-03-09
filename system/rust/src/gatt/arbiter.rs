//! This module handles "arbitration" of ATT packets, to determine whether they
//! should be handled by the primary stack or by the "Private GATT" stack

use std::{collections::HashMap, sync::Mutex};

use bt_common::init_flags::full_rust_gatt_server_is_enabled;
use log::{error, info, trace};

use crate::{
    do_in_rust_thread,
    packets::{AttOpcode, OwnedAttView, OwnedPacket},
};

use super::{
    ffi::{InterceptAction, StoreCallbacksFromRust},
    ids::{AdvertiserId, ServerId, TransportIndex},
    mtu::MtuEvent,
    opcode_types::{classify_opcode, OperationType},
    server::isolation_manager::IsolationManager,
};

static ARBITER: Mutex<Option<Arbiter>> = Mutex::new(None);

/// This class is responsible for tracking which connections and advertising we
/// own, and using this information to decide what packets should be
/// intercepted, and which should be forwarded to the legacy stack.
#[derive(Default)]
pub struct Arbiter {
    pub isolation_manager: IsolationManager,
}

/// Initialize the Arbiter
pub fn initialize_arbiter() {
    *ARBITER.lock().unwrap() = Some(Arbiter::new());

    StoreCallbacksFromRust(
        on_le_connect,
        on_le_disconnect,
        intercept_packet,
        |tcb_idx| on_mtu_event(TransportIndex(tcb_idx), MtuEvent::OutgoingRequest),
        |tcb_idx, mtu| on_mtu_event(TransportIndex(tcb_idx), MtuEvent::IncomingResponse(mtu)),
        |tcb_idx, mtu| on_mtu_event(TransportIndex(tcb_idx), MtuEvent::IncomingRequest(mtu)),
    );
}

/// Acquire the mutex holding the Arbiter and provide a mutable reference to the
/// supplied closure
pub fn with_arbiter<T>(f: impl FnOnce(&mut Arbiter) -> T) -> T {
    f(ARBITER.lock().unwrap().as_mut().unwrap())
}

impl Arbiter {
    /// Constructor
    pub fn new() -> Self {
        Self { isolation_manager: IsolationManager::new() }
    }

    /// Test to see if a buffer contains a valid ATT packet with an opcode we
    /// are interested in intercepting
    fn try_parse_att_server_packet(
        &self,
        tcb_idx: TransportIndex,
        packet: Box<[u8]>,
    ) -> Option<OwnedAttView> {
        let att = OwnedAttView::try_parse(packet).ok()?;
        if att.view().get_opcode() == AttOpcode::EXCHANGE_MTU_REQUEST {
            return None;
        }

        match classify_opcode(att.view().get_opcode()) {
            OperationType::Response | OperationType::Indication | OperationType::Notification => {
                None
            }
            OperationType::Request | OperationType::Confirmation | OperationType::Command => {
                if full_rust_gatt_server_is_enabled()
                    || self.isolation_manager.is_connection_isolated(tcb_idx)
                {
                    Some(att)
                } else {
                    None
                }
            }
        }
    }
}

fn on_le_connect(tcb_idx: u8, advertiser: u8) {
    let tcb_idx = TransportIndex(tcb_idx);
    let advertiser = AdvertiserId(advertiser);
    if with_arbiter(|arbiter| arbiter.isolation_manager.on_le_connect(tcb_idx, advertiser))
        .is_some()
    {
        do_in_rust_thread(move |modules| {
            if let Err(err) = modules.gatt_module.on_le_connect(tcb_idx, Some(advertiser)) {
                error!("{err:?}")
            }
        })
    }
}

fn on_le_disconnect(tcb_idx: u8) {
    let tcb_idx = TransportIndex(tcb_idx);
    if with_arbiter(|arbiter| arbiter.isolation_manager.on_le_disconnect(tcb_idx)).is_some() {
        do_in_rust_thread(move |modules| {
            modules.gatt_module.on_le_disconnect(tcb_idx);
        })
    }
}

fn intercept_packet(tcb_idx: u8, packet: Vec<u8>) -> InterceptAction {
    let tcb_idx = TransportIndex(tcb_idx);
    if let Some(att) = with_arbiter(|arbiter| {
        if !full_rust_gatt_server_is_enabled()
            && !arbiter.isolation_manager.is_connection_isolated(tcb_idx)
        {
            return None;
        }
        arbiter.try_parse_att_server_packet(tcb_idx, packet.into_boxed_slice())
    }) {
        do_in_rust_thread(move |modules| {
            trace!("pushing packet to GATT");
            if let Some(bearer) = modules.gatt_module.get_bearer(tcb_idx) {
                bearer.handle_packet(att.view())
            } else {
                error!("{tcb_idx:?} closed, bearer does not exist");
            }
        });
        InterceptAction::Drop
    } else {
        InterceptAction::Forward
    }
}

fn on_mtu_event(tcb_idx: TransportIndex, event: MtuEvent) {
    do_in_rust_thread(move |modules| {
        let Some(bearer) = modules.gatt_module.get_bearer(tcb_idx) else {
            error!("Bearer for {tcb_idx:?} not found");
            return;
        };
        if let Err(err) = bearer.handle_mtu_event(event) {
            error!("{err:?}")
        }
    });
}

#[cfg(test)]
mod test {
    use super::*;

    use crate::{
        gatt::ids::AttHandle,
        packets::{
            AttBuilder, AttExchangeMtuRequestBuilder, AttOpcode, AttReadRequestBuilder,
            Serializable,
        },
    };

    const TCB_IDX: TransportIndex = TransportIndex(1);
    const ADVERTISER_ID: AdvertiserId = AdvertiserId(2);
    const SERVER_ID: ServerId = ServerId(3);

    const ANOTHER_ADVERTISER_ID: AdvertiserId = AdvertiserId(4);

    // #[test]
    // fn test_packet_capture_when_isolated() {
    //     let mut arbiter = Arbiter::new();
    //     arbiter.associate_server_with_advertiser(SERVER_ID, ADVERTISER_ID);
    //     arbiter.on_le_connect(TCB_IDX, ADVERTISER_ID);
    //     let packet = AttBuilder {
    //         opcode: AttOpcode::READ_REQUEST,
    //         _child_: AttReadRequestBuilder { attribute_handle: AttHandle(1).into() }.into(),
    //     };

    //     let out = arbiter.try_parse_att_server_packet(packet.to_vec().unwrap().into());

    //     assert!(out.is_some());
    // }

    // #[test]
    // fn test_mtu_bypass() {
    //     let mut arbiter = Arbiter::new();
    //     arbiter.associate_server_with_advertiser(SERVER_ID, ADVERTISER_ID);
    //     arbiter.on_le_connect(TCB_IDX, ADVERTISER_ID);
    //     let packet = AttBuilder {
    //         opcode: AttOpcode::EXCHANGE_MTU_REQUEST,
    //         _child_: AttExchangeMtuRequestBuilder { mtu: 64 }.into(),
    //     };

    //     let out = arbiter.try_parse_att_server_packet(packet.to_vec().unwrap().into());

    //     assert!(out.is_none());
    // }
}
