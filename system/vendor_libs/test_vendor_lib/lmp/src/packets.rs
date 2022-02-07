pub mod hci {
    pub use bt_packets::custom_types::*;
    pub use bt_packets::hci::*;
}

pub mod lmp {
    #![allow(clippy::all)]
    #![allow(unused)]
    #![allow(missing_docs)]

    include!(concat!(env!("OUT_DIR"), "/lmp_packets.rs"));
}
