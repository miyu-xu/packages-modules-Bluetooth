use bluetooth_rs::hal::snoop::{AclHal, ControlHal, IsoHal, ScoHal};
use bt_packets::hci::{AclPacket, CommandPacket, IsoPacket, ScoPacket};
use std::sync::Arc;
use tokio::runtime::Runtime;

pub struct Hal {
    control: ControlHal,
    acl: AclHal,
    sco: ScoHal,
    iso: IsoHal,
    rt: Arc<Runtime>,
}

impl Hal {
    pub fn new(
        control: ControlHal,
        acl: AclHal,
        sco: ScoHal,
        iso: IsoHal,
        rt: Arc<Runtime>,
    ) -> Self {
        Self { control, acl, sco, iso, rt }
    }
}

pub fn hal_send_command(hal: &mut Hal, data: &[u8]) {
    log::error!("sending command: {:02x?}", data);
    match CommandPacket::parse(data) {
        Ok(packet) => {
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
            let tx = hal.iso.tx.clone();
            hal.rt.spawn(async move {
                tx.send(packet).await.unwrap();
            });
        }
        Err(e) => panic!("could not parse iso: {:?} {:02x?}", e, data),
    }
}
