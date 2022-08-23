//! This library provides access to Linux uinput.

use libc;
use log::error;

// uinput setup constants
const UINPUT_MAX_NAME_SIZE: usize = 80;
const ABS_MAX: usize = 0x3F;
const BUS_BLUETOOTH: u16 = 0x05;

#[repr(C, packed)]
struct UInputId {
    bustype: libc::c_ushort,
    vendor: libc::c_ushort,
    product: libc::c_ushort,
    version: libc::c_ushort,
}

#[repr(C, packed)]
struct UInputDev {
    name: [libc::c_char; UINPUT_MAX_NAME_SIZE],
    id: UInputId,
    ff_effects_max: libc::c_int,
    absmax: [libc::c_int; ABS_MAX + 1],
    absmin: [libc::c_int; ABS_MAX + 1],
    absfuzz: [libc::c_int; ABS_MAX + 1],
    absflat: [libc::c_int; ABS_MAX + 1],
}

#[allow(dead_code)]
pub struct UInput {
    fd: i32,
    ready: bool,
    device: UInputDev,
}

// Close given file descriptor.
fn close_fd(fd: i32) -> i32 {
    unsafe { libc::close(fd) }
}

impl Drop for UInput {
    fn drop(&mut self) {
        if self.is_initialized() {
            close_fd(self.fd);
        }
    }
}

impl UInput {
    pub fn new() -> Self {
        UInput {
            fd: -1,
            ready: false,
            device: UInputDev {
                name: [0; UINPUT_MAX_NAME_SIZE],
                id: UInputId { bustype: BUS_BLUETOOTH, vendor: 0, product: 0, version: 0 },
                ff_effects_max: 0,
                absmax: [0; ABS_MAX + 1],
                absmin: [0; ABS_MAX + 1],
                absfuzz: [0; ABS_MAX + 1],
                absflat: [0; ABS_MAX + 1],
            },
        }
    }

    // Return true if uinput is open and a valid fd is retrieved.
    pub fn is_initialized(&self) -> bool {
        self.fd >= 0
    }

    // If a uinput device is created and ready for events.
    pub fn is_ready(&self) -> bool {
        self.ready
    }

    pub fn init(&mut self) {
        if self.is_initialized() {
            return;
        }

        unsafe {
            let mut fd = libc::open("/dev/uinput\0".as_ptr().cast(), libc::O_RDWR);
            if fd < 0 {
                fd = libc::open("/dev/input/uinput\0".as_ptr().cast(), libc::O_RDWR);
                if fd < 0 {
                    fd = libc::open("/dev/misc/uinput\0".as_ptr().cast(), libc::O_RDWR);
                    if fd < 0 {
                        error!("Failed to open uinput")
                    }
                }
            }
            self.fd = fd;
        }
    }
}
