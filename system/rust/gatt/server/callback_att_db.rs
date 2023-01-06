use crate::packets::AttAttributeDataChild;

use super::att_database::{AttAttribute, AttDatabase, AttHandle};
use async_trait::async_trait;
use std::cell::RefCell;

pub struct CallbackAttDatabase<R, W>
where
    R: FnMut(AttHandle) -> Result<Box<[u8]>, String>,
    W: FnMut(AttHandle, &[u8]) -> Result<(), String>,
{
    read_attribute: RefCell<R>,
    write_attribute: RefCell<W>,
    attributes: Vec<AttAttribute>,
}

impl<R, W> CallbackAttDatabase<R, W>
where
    R: FnMut(AttHandle) -> Result<Box<[u8]>, String>,
    W: FnMut(AttHandle, &[u8]) -> Result<(), String>,
{
    #[cfg(test)]
    pub fn new(r: R, w: W, attributes: Vec<AttAttribute>) -> Self {
        Self { read_attribute: RefCell::new(r), write_attribute: RefCell::new(w), attributes }
    }
}

#[async_trait(?Send)]
impl<R, W> AttDatabase for CallbackAttDatabase<R, W>
where
    R: FnMut(AttHandle) -> Result<Box<[u8]>, String>,
    W: FnMut(AttHandle, &[u8]) -> Result<(), String>,
{
    async fn read_attribute(&self, handle: AttHandle) -> Result<AttAttributeDataChild, String> {
        Ok(AttAttributeDataChild::RawData((self.read_attribute.borrow_mut())(handle)?))
    }
    async fn write_attribute(&self, handle: AttHandle, data: &[u8]) -> Result<(), String> {
        (self.write_attribute.borrow_mut())(handle, data)
    }
    fn list_attributes(&self) -> Vec<AttAttribute> {
        self.attributes.clone()
    }
}
