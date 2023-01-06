use std::cell::RefCell;

use crate::packets::AttAttributeDataChild;

use super::{att_database::AttHandle, gatt_database::GattDatastore};

use async_trait::async_trait;

pub struct DemoGattDatastore {
    cnt: RefCell<u64>,
}

impl DemoGattDatastore {
    pub fn new() -> Self {
        Self { cnt: RefCell::new(0) }
    }
}

#[async_trait(?Send)]
impl GattDatastore for DemoGattDatastore {
    async fn read_characteristic(
        &self,
        _handle: AttHandle,
    ) -> Result<AttAttributeDataChild, String> {
        *self.cnt.borrow_mut() += 1;
        Ok(AttAttributeDataChild::RawData(Box::new(self.cnt.borrow().to_le_bytes())))
    }

    async fn write_characteristic(&self, _handle: AttHandle, _data: &[u8]) -> Result<(), String> {
        Err("write rejected".to_string())
    }
}
