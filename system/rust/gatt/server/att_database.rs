use async_trait::async_trait;

use crate::packets::{
    AttAttributeDataChild, AttHandleBuilder, AttHandleView, UuidBuilder, UuidView,
};

pub const PRIMARY_SERVICE_DECLARATION_UUID: AttUuid = AttUuid::new([0x00, 0x28, 0x00, 0x00]);
pub const CHARACTERISTIC_UUID: AttUuid = AttUuid::new([0x03, 0x28, 0x00, 0x00]);

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
pub struct AttHandle(pub u16);

impl From<AttHandleView<'_>> for AttHandle {
    fn from(value: AttHandleView) -> Self {
        AttHandle(value.get_handle() as u16)
    }
}

impl From<AttHandle> for AttHandleBuilder {
    fn from(value: AttHandle) -> Self {
        AttHandleBuilder { handle: value.0 as u64 }
    }
}

#[derive(PartialEq, Eq, Clone, Copy, Debug)]
pub struct AttUuid([u8; 16]);

impl AttUuid {
    pub const fn new(bytes: [u8; 4]) -> Self {
        Self([
            0xFB, 0x34, 0x9B, 0x5F, 0x80, 0x00, 0x00, 0x80, 0x00, 0x10, 0x00, 0x00, bytes[0],
            bytes[1], bytes[2], bytes[3],
        ])
    }
}

impl TryFrom<UuidView<'_>> for AttUuid {
    type Error = String;

    fn try_from(value: UuidView<'_>) -> Result<Self, String> {
        let bytes = value.get_data_iter().map(|x| x as u8).collect::<Vec<_>>();
        bytes[..].try_into()
    }
}

impl From<[u8; 2]> for AttUuid {
    fn from(bytes: [u8; 2]) -> Self {
        [bytes[0], bytes[1], 0x00, 0x00].into()
    }
}

impl From<[u8; 4]> for AttUuid {
    fn from(bytes: [u8; 4]) -> Self {
        AttUuid::new(bytes)
    }
}

impl TryFrom<&[u8]> for AttUuid {
    type Error = String;

    fn try_from(bytes: &[u8]) -> Result<Self, String> {
        Ok(match bytes.len() {
            2 => [bytes[0], bytes[1]].into(),
            4 => [bytes[0], bytes[1], bytes[2], bytes[3]].into(),
            _ => Self(
                bytes
                    .iter()
                    .map(|x| *x as u8)
                    .collect::<Vec<_>>()
                    .try_into()
                    .map_err(|_| format!("invalid UUID size {}", bytes.len()))?,
            ),
        })
    }
}

impl From<AttUuid> for UuidBuilder {
    fn from(value: AttUuid) -> Self {
        UuidBuilder { data: value.0.into_iter().map(|x| x as u64).collect() }
    }
}

#[derive(Debug, Clone)]
pub struct AttAttribute {
    pub handle: AttHandle,
    pub uuid: AttUuid,
    pub permissions: AttPermissions,
}

#[derive(Debug, Clone)]
pub struct AttPermissions {
    pub readable: bool,
    pub writable: bool,
}

#[async_trait(?Send)]
pub trait AttDatabase {
    async fn read_attribute(&self, handle: AttHandle) -> Result<AttAttributeDataChild, String>;
    async fn write_attribute(&self, handle: AttHandle, data: &[u8]) -> Result<(), String>;
    fn list_attributes(&self) -> Vec<AttAttribute>;
}
