#![allow(clippy::all)]
#![allow(unused)]
#![allow(missing_docs)]

pub mod hci {
    include!(concat!(env!("OUT_DIR"), "/l2cap_packets.rs"));
}
