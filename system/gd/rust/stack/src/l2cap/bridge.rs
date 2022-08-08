//! Merged bridge

use std::ptr::null_mut;

use self::ffi::L2CA_Register_from_rust;

use crate::l2cap::listeners::{
    disconnect_connection_handler, incoming_connection_handler, incoming_data_handler,
    initialize_l2cap_tx, outgoing_connection_handler, CallbackEvent,
};
use crate::l2cap::types::RawAddress;

use cxx::{type_id, ExternType};
use tokio::sync::mpsc::Sender;
use tokio::sync::oneshot;

pub struct OneshotU16(Option<oneshot::Sender<u16>>);

impl From<oneshot::Sender<u16>> for OneshotU16 {
    fn from(tx: oneshot::Sender<u16>) -> OneshotU16 {
        OneshotU16(Some(tx))
    }
}

pub struct EventChannel(pub Sender<CallbackEvent>);

unsafe impl ExternType for RawAddress {
    type Id = type_id!("RawAddress");
    type Kind = cxx::kind::Trivial;
}

#[cxx::bridge(namespace = "")]
pub mod ffi {

    extern "Rust" {
        type OneshotU16;
        type EventChannel;

        fn run_l2cap_self_test();

        fn incoming_connection_handler(remote_addr: &RawAddress, local_cid: u16, psm: u16, id: u8);
        fn outgoing_connection_handler(local_cid: u16, result: u16);
        fn incoming_data_handler(local_cid: u16, result: &[u8]);
        fn disconnect_connection_handler(local_cid: u16, should_ack: bool);

        fn oneshot_send_u16(sender: &mut OneshotU16, value: u16);
        fn initialize_l2cap_tx(tx: &mut EventChannel);
    }
    extern "C++" {
        include!("stack/include/bt_hdr.h");
        include!("stack/include/l2c_api.h");
        include!("src/l2cap/ffi/callbacks.h");

        type RawAddress = crate::l2cap::types::RawAddress;

        type tL2CAP_ERTM_INFO;

        unsafe fn L2CA_Register_from_rust(
            psm: u16,
            enable_snoop: bool,
            p_ertm_info: *mut tL2CAP_ERTM_INFO,
            my_mtu: u16,
            required_remote_mtu: u16,
            completion: &mut OneshotU16,
        );

        unsafe fn L2CA_Deregister_from_rust(psm: u16);

        unsafe fn L2CA_ConnectReq_from_rust(
            psm: u16,
            p_bd_addr: &RawAddress,
            completion: &mut OneshotU16,
        );

        unsafe fn L2CA_DisconnectReq_from_rust(cid: u16);

        unsafe fn L2CA_DataWrite_from_rust(cid: u16, data: &[u8], completion: &mut OneshotU16);

        unsafe fn initialize_l2cap_tx_on_main_thread(tx: &mut EventChannel);

    }
}

fn oneshot_send_u16(sender: &mut OneshotU16, value: u16) {
    match sender.0.take().map(|x| x.send(value)) {
        Some(Ok(())) => (),
        Some(_) => log::error!("failed to send oneshot response back to rust"),
        None => log::error!("attempted to use oneshot completion multiple times"),
    }
}

fn run_l2cap_self_test() {
    unsafe {
        L2CA_Register_from_rust(1, false, null_mut(), 0, 0, unsafe {
            null_mut::<OneshotU16>().as_mut().unwrap()
        });
    }
}
