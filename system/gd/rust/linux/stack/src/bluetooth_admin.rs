//! Anything related to the Admin API (IBluetoothAdmin).

use std::sync::{Arc, Mutex};

use crate::bluetooth::{Bluetooth};

pub struct BluetoothAdmin {
    adapter: Option<Arc<Mutex<Box<Bluetooth>>>>,
}

impl BluetoothAdmin {
    pub fn new() -> BluetoothAdmin {
        BluetoothAdmin {
            adapter: None,
        }
    }
}

#[cfg(test)]
mod tests{
    use crate::bluetooth_admin::BluetoothAdmin;

    #[test]
    fn new_test() {
        let admin = BluetoothAdmin::new();
    }
}