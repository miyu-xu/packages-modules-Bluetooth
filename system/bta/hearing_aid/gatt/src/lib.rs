//! Bluetooth GATT Client

//#![allow(warnings)]

use std::os::fd::RawFd;

mod att;
mod client;
mod database;
mod executor;
mod gatt;
mod uuid;

#[no_mangle]
pub extern "C" fn gatt_executor_setup() -> RawFd {
    executor::setup(async {
        println!("Lol")
    }).unwrap_or_else(|errno| {
        errno.set();
        -1
    })
}

#[no_mangle]
pub extern "C" fn gatt_executor_poll() {
    executor::poll();
}
