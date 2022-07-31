//! Merged bridge

use self::ffi::{L2CA_Register2, prepare_p_cb_info};

#[cxx::bridge(namespace = "")]
pub mod ffi {

    extern "Rust" {
        fn run_l2cap_self_test();
    }
    extern "C++" {
        include!("stack/include/bt_hdr.h");
        include!("stack/include/l2c_api.h");
        include!("src/l2cap/ffi/callbacks.h");

        type tL2CAP_APPL_INFO;
        type tL2CAP_ERTM_INFO;

        unsafe fn L2CA_Register2(
            psm: u16,
            p_cb_info: &tL2CAP_APPL_INFO,
            enable_snoop: bool,
            p_ertm_info: *mut tL2CAP_ERTM_INFO,
            my_mtu: u16,
            required_remote_mtu: u16,
            sec_level: u16,
        ) -> u16;

        unsafe fn prepare_p_cb_info() ->  UniquePtr<tL2CAP_APPL_INFO>;
    }
}

fn run_l2cap_self_test() {
    unsafe {
        L2CA_Register2(2, &prepare_p_cb_info(), false, std::ptr::null_mut(), 10, 10, 10);
    }
}
