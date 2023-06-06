#![allow(non_upper_case_globals)]
#![allow(non_camel_case_types)]
#![allow(non_snake_case)]

include!(concat!(env!("OUT_DIR"), "/bindings.rs"));

use log::{info};

pub struct Uhid {
    active_device: String,
}

impl uhid_event {
    pub fn new() -> Self {
        uhid_event {
            type_: uhid_event_type_UHID_CREATE2,
            u: uhid_event__bindgen_ty_1 {
                create2: uhid_create2_req {
                    name: [0; 128], 
                    phys: [0; 64], 
                    uniq: [0; 64], 
                    rd_size: 0, 
                    bus: 0, 
                    vendor:0, 
                    product: 0, 
                    version: 0, 
                    country: 0, 
                    rd_data: [0; 4096],
                }
            },
        }
    }
}

impl Uhid {
    /// Create a new UInput struct that holds a vector of uinput objects.
    pub fn new() -> Self {
        Uhid {
            active_device: String::from("00:00:00:00:00:00"),
        }
    }
    /// Initialize a uinput device with kernel.
    pub fn create(&mut self, name: String, addr: String) -> Result<(), String> {
        info!("uhid create");
        let foo = uhid_event::new();
        Self::uhid_write(&foo);
        Ok(())
    }
    pub fn uhid_write(ev: &uhid_event) -> Result<(), String>
	{  
        Ok(())
	}
}