use crate::{
    gatt::{
        ids::AttHandle,
        server::att_database::{AttAttribute, AttDatabase},
    },
    packets::{AttAttributeDataChild, AttAttributeDataView, AttErrorCode},
};

use async_trait::async_trait;
use log::info;
use std::collections::HashMap;

pub struct TestAttDatabase {
    attributes: HashMap<AttHandle, (AttAttribute, Vec<u8>)>,
}

impl TestAttDatabase {
    #[cfg(test)]
    pub fn new(attributes: Vec<(AttAttribute, Vec<u8>)>) -> Self {
        Self {
            attributes: attributes
                .into_iter()
                .map(|(att, data)| (att.handle, (att, data)))
                .collect(),
        }
    }
}

#[async_trait(?Send)]
impl AttDatabase for TestAttDatabase {
    async fn read_attribute(
        &self,
        handle: AttHandle,
    ) -> Result<AttAttributeDataChild, AttErrorCode> {
        info!("reading {handle:?}");
        match self.attributes.get(&handle) {
            Some((_, data)) => Ok(AttAttributeDataChild::RawData(data.clone().into_boxed_slice())),
            None => Err(AttErrorCode::INVALID_HANDLE),
        }
    }
    async fn write_attribute(
        &self,
        _handle: AttHandle,
        _data: AttAttributeDataView<'_>,
    ) -> Result<(), AttErrorCode> {
        Err(AttErrorCode::WRITE_NOT_PERMITTED)
    }
    fn list_attributes(&self) -> Vec<AttAttribute> {
        let mut attrs: Vec<_> = self.attributes.values().map(|(att, _)| att.clone()).collect();
        attrs.sort_by_key(|attr| attr.handle);
        attrs
    }
}
