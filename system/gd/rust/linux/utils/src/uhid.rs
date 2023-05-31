//! This library provides access to Linux uhid.

use std::fs::File;
use uhid_virt::{Bus, CreateParams, UHIDDevice};

pub const RDESC: [u8; 85] = [
    0x05, 0x01, /* USAGE_PAGE (Generic Desktop) */
    0x09, 0x02, /* USAGE (Mouse) */
    0xa1, 0x01, /* COLLECTION (Application) */
    0x09, 0x01, /* USAGE (Pointer) */
    0xa1, 0x00, /* COLLECTION (Physical) */
    0x85, 0x01, /* REPORT_ID (1) */
    0x05, 0x09, /* USAGE_PAGE (Button) */
    0x19, 0x01, /* USAGE_MINIMUM (Button 1) */
    0x29, 0x03, /* USAGE_MAXIMUM (Button 3) */
    0x15, 0x00, /* LOGICAL_MINIMUM (0) */
    0x25, 0x01, /* LOGICAL_MAXIMUM (1) */
    0x95, 0x03, /* REPORT_COUNT (3) */
    0x75, 0x01, /* REPORT_SIZE (1) */
    0x81, 0x02, /* INPUT (Data,Var,Abs) */
    0x95, 0x01, /* REPORT_COUNT (1) */
    0x75, 0x05, /* REPORT_SIZE (5) */
    0x81, 0x01, /* INPUT (Cnst,Var,Abs) */
    0x05, 0x01, /* USAGE_PAGE (Generic Desktop) */
    0x09, 0x30, /* USAGE (X) */
    0x09, 0x31, /* USAGE (Y) */
    0x09, 0x38, /* USAGE (WHEEL) */
    0x15, 0x81, /* LOGICAL_MINIMUM (-127) */
    0x25, 0x7f, /* LOGICAL_MAXIMUM (127) */
    0x75, 0x08, /* REPORT_SIZE (8) */
    0x95, 0x03, /* REPORT_COUNT (3) */
    0x81, 0x06, /* INPUT (Data,Var,Rel) */
    0xc0, /* END_COLLECTION */
    0xc0, /* END_COLLECTION */
    0x05, 0x01, /* USAGE_PAGE (Generic Desktop) */
    0x09, 0x06, /* USAGE (Keyboard) */
    0xa1, 0x01, /* COLLECTION (Application) */
    0x85, 0x02, /* REPORT_ID (2) */
    0x05, 0x08, /* USAGE_PAGE (Led) */
    0x19, 0x01, /* USAGE_MINIMUM (1) */
    0x29, 0x03, /* USAGE_MAXIMUM (3) */
    0x15, 0x00, /* LOGICAL_MINIMUM (0) */
    0x25, 0x01, /* LOGICAL_MAXIMUM (1) */
    0x95, 0x03, /* REPORT_COUNT (3) */
    0x75, 0x01, /* REPORT_SIZE (1) */
    0x91, 0x02, /* Output (Data,Var,Abs) */
    0x95, 0x01, /* REPORT_COUNT (1) */
    0x75, 0x05, /* REPORT_SIZE (5) */
    0x91, 0x01, /* Output (Cnst,Var,Abs) */
    0xc0, /* END_COLLECTION */
];

pub struct UHid {
    /// A vector that holds uhid objects.
    devices: Vec<UHIDDevice<File>>,
}

impl Drop for UHid {
    fn drop(&mut self) {
        for device in self.devices.iter_mut() {
            device.destroy();
        }
    }
}

impl UHid {
    /// Create a new UHid struct that holds a vector of uhid objects.
    pub fn new() -> Self {
        UHid { devices: Vec::<UHIDDevice<File>>::new() }
    }

    /// Initialize a uhid device with kernel.
    pub fn create(&mut self, name: String, phys: String, uniq: String) -> Result<(), String> {
        let rd_data = RDESC.to_vec();
        let create_params = CreateParams {
            name: name,
            phys: phys,
            uniq: uniq,
            bus: Bus::BLUETOOTH,
            vendor: 0x0fff,
            product: 0x0fff,
            version: 0,
            country: 0,
            rd_data,
        };
        match UHIDDevice::create(create_params) {
            Ok(d) => self.devices.push(d),
            Err(e) => return Err(e.to_string()),
        }
        Ok(())
    }

    pub fn clear_all(&mut self) {
        for device in self.devices.iter_mut() {
            device.destroy();
        }
        self.devices.clear();
    }
}
