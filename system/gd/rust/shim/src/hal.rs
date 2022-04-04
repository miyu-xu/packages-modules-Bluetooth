//! Hal shim

use bluetooth_rs::hal::snoop::{ControlHal, AclHal, IsoHal, ScoHal};
use bt_packets::hci::{AclPacket, CommandPacket, IsoPacket, ScoPacket, EventPacket};
use bt_facade_helpers::RxAdapter;
use std::sync::Arc;
use tokio::runtime::Runtime;
use crate::bridge::ffi;
use crate::hci::CallbackWrapper;

pub struct Hal {
    evt_rx: RxAdapter<EventPacket>,
    acl_rx: RxAdapter<AclPacket>,
    iso_rx: RxAdapter<IsoPacket>,
    sco_rx: RxAdapter<ScoPacket>,
    control: ControlHal,
    acl: AclHal,
    iso: IsoHal,
    sco: ScoHal,
    rt: Arc<Runtime>,
}

impl Hal {
    pub fn new(rt: Arc<Runtime>, control: ControlHal, acl: AclHal, iso: IsoHal, sco: ScoHal) -> Self {
        Self {
            evt_rx: RxAdapter::from_arc(control.rx.clone()),
            acl_rx: RxAdapter::from_arc(acl.rx.clone()),
            iso_rx: RxAdapter::from_arc(iso.rx.clone()),
            sco_rx: RxAdapter::from_arc(sco.rx.clone()),
            control,
            acl,
            iso,
            sco,
            rt
        }
    }
}

pub fn hal_send_command(
    hal: &mut Hal,
    data: &[u8],
) {
    match CommandPacket::parse(data) {
        Ok(packet) => {
            println!("sending hal command {:02x?}", data);
            let tx = hal.control.tx.clone();
            hal.rt.spawn(async move {
                tx.send(packet).await.unwrap();
            });
        }
        Err(e) => panic!("could not parse command: {:?} {:02x?}", e, data),
    }
}

pub fn hal_send_acl(hal: &mut Hal, data: &[u8]) {
    match AclPacket::parse(data) {
        Ok(packet) => {
            println!("sending hal acl {:02x?}", data);
            let tx = hal.acl.tx.clone();
            hal.rt.spawn(async move {
                tx.send(packet).await.unwrap();
            });
        }
        Err(e) => panic!("could not parse acl: {:?} {:02x?}", e, data),
    }
}

pub fn hal_send_sco(hal: &mut Hal, data: &[u8]) {
    match ScoPacket::parse(data) {
        Ok(packet) => {
            println!("sending hal sco {:02x?}", data);
            let tx = hal.sco.tx.clone();
            hal.rt.spawn(async move {
                tx.send(packet).await.unwrap();
            });
        }
        Err(e) => panic!("could not parse sco: {:?} {:02x?}", e, data),
    }
}

pub fn hal_send_iso(hal: &mut Hal, data: &[u8]) {
    match IsoPacket::parse(data) {
        Ok(packet) => {
            println!("sending hal iso {:02x?}", data);
            let tx = hal.iso.tx.clone();
            hal.rt.spawn(async move {
                tx.send(packet).await.unwrap();
            });
        }
        Err(e) => panic!("could not parse iso: {:?} {:02x?}", e, data),
    }
}

pub fn hal_set_acl_callback(hal: &mut Hal, cb: cxx::UniquePtr<ffi::u8SliceCallback>) {
    println!("calling acl callback");
    hal.acl_rx.stream_runnable(&hal.rt, CallbackWrapper { cb });
}

pub fn hal_set_evt_callback(hal: &mut Hal, cb: cxx::UniquePtr<ffi::u8SliceCallback>) {
    println!("calling evt callback");
    hal.evt_rx.stream_runnable(&hal.rt, CallbackWrapper { cb });
}

pub fn hal_set_iso_callback(hal: &mut Hal, cb: cxx::UniquePtr<ffi::u8SliceCallback>) {
    println!("calling iso callback");
    hal.iso_rx.stream_runnable(&hal.rt, CallbackWrapper { cb });
}

pub fn hal_set_sco_callback(hal: &mut Hal, cb: cxx::UniquePtr<ffi::u8SliceCallback>) {
    println!("calling sco callback");
    hal.sco_rx.stream_runnable(&hal.rt, CallbackWrapper { cb });
}
