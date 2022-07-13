use crate::btif::{BtBondState, BtState, BtStatus, RawAddress};

#[cxx::bridge(namespace = bluetooth::topshim::rust)]
mod ffi {
    #[derive(Debug, Copy, Clone)]
    pub struct RustRawAddress {
        address: [u8; 6],
    }

    unsafe extern "C++" {
        include!("metrics/metrics_shim.h");

        fn adapter_state_changed(state: u32);
        fn bond_state_changed(
            status: u32,
            bt_addr: RustRawAddress,
            bond_state: u32,
            fail_reason: i32,
        );
    }
}

impl From<RawAddress> for ffi::RustRawAddress {
    fn from(addr: RawAddress) -> Self {
        ffi::RustRawAddress { address: addr.val }
    }
}

impl Into<RawAddress> for ffi::RustRawAddress {
    fn into(self) -> RawAddress {
        RawAddress { val: self.address }
    }
}

pub fn adapter_state_changed(state: BtState) {
    ffi::adapter_state_changed(state as u32);
}

pub fn bond_state_changed(
    status: BtStatus,
    addr: RawAddress,
    bond_state: BtBondState,
    fail_reason: i32,
) {
    ffi::bond_state_changed(status as u32, addr.into(), bond_state as u32, fail_reason as i32);
}
