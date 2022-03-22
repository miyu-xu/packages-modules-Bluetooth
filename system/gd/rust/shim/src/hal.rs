use bluetooth_rs::hal::snoop;
use bt_packets::hci::{AclPacket, CommandPacket, IsoPacket, ScoPacket};
use std::sync::Arc;
use tokio::runtime::Runtime;


pub struct Hal {
    internal: snoop::Hal,
    rt: Arc<Runtime>,
}

impl Hal {
    pub fn new(rt: Arc<Runtime>, internal: snoop::Hal) -> Self {
        Self { internal, rt }
    }
}

pub fn hal_send_command(
    hal: &mut Hal,
    data: &[u8],
) {
    match CommandPacket::parse(data) {
        Ok(packet) => {
            let tx = hal.internal.control.tx.clone();
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
            let tx = hal.internal.acl.tx.clone();
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
            let tx = hal.internal.sco.tx.clone();
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
            let tx = hal.internal.iso.tx.clone();
            hal.rt.spawn(async move {
                tx.send(packet).await.unwrap();
            });
        }
        Err(e) => panic!("could not parse iso: {:?} {:02x?}", e, data),
    }
}
