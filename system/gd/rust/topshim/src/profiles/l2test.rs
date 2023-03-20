use crate::bindings::root as bindings;
use crate::btif::{BluetoothInterface, RawAddress, SupportedProfiles};
use crate::ccall;

struct RawL2testWrapper {
    pub raw: *const bindings::btl2test_interface_t,
}

unsafe impl Send for RawL2testWrapper {}

pub struct L2Test {
    internal: RawL2testWrapper,
}

impl L2Test {
    pub fn new(intf: &BluetoothInterface) -> L2Test {
        let r = intf.get_profile_interface(SupportedProfiles::L2test);
        L2Test { internal: RawL2testWrapper { raw: r as *const bindings::btl2test_interface_t } }
    }

    pub fn send_echo(&self, addr: RawAddress) -> bool {
        ccall!(self, l2cap_echo, &addr)
    }
}
