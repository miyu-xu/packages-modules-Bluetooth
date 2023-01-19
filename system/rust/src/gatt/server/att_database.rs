use async_trait::async_trait;

use crate::{
    core::get_128_be_uuid_bytes,
    gatt::{ffi::Uuid, ids::AttHandle},
    packets::{
        AttAttributeDataChild, AttAttributeDataView, AttErrorCode, AttHandleBuilder, AttHandleView,
        ParseError, Uuid128Builder, Uuid128View, Uuid16Builder, Uuid16View, UuidBuilder, UuidView,
    },
};

pub const PRIMARY_SERVICE_DECLARATION_UUID: AttUuid = AttUuid::new([0x00, 0x28, 0x00, 0x00]);
pub const CHARACTERISTIC_UUID: AttUuid = AttUuid::new([0x03, 0x28, 0x00, 0x00]);

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

/// A UUID used to represent an ATT type
#[derive(PartialEq, Eq, Clone, Copy, Debug)]
pub struct AttUuid([u8; 16]);

impl AttUuid {
    /// Constructor, from a 4-byte UUID.
    ///
    /// We accept only 4-bytes, and let the 32-byte constructor live in the From<> implementations,
    /// so we can use it in a const context.
    pub const fn new(bytes: [u8; 4]) -> Self {
        Self([
            0xFB, 0x34, 0x9B, 0x5F, 0x80, 0x00, 0x00, 0x80, 0x00, 0x10, 0x00, 0x00, bytes[0],
            bytes[1], bytes[2], bytes[3],
        ])
    }
}

impl TryFrom<UuidView<'_>> for AttUuid {
    type Error = ParseError;

    fn try_from(value: UuidView<'_>) -> Result<Self, ParseError> {
        let bytes = value.get_data_iter().map(|x| x as u8).collect::<Vec<_>>();
        bytes[..].try_into()
    }
}

impl From<&Uuid16View<'_>> for AttUuid {
    fn from(uuid: &Uuid16View) -> Self {
        let bytes: [u8; 2] = uuid
            .get_data_iter()
            .map(|x| x as u8)
            .collect::<Vec<_>>()
            .try_into()
            .expect("Uuid16View MUST have exactly 2 bytes");
        bytes.into()
    }
}

impl From<&Uuid128View<'_>> for AttUuid {
    fn from(uuid: &Uuid128View) -> Self {
        let bytes = uuid
            .get_data_iter()
            .map(|x| x as u8)
            .collect::<Vec<_>>()
            .try_into()
            .expect("Uuid128View MUST have exactly 16 bytes");
        Self(bytes)
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
    type Error = ParseError;

    fn try_from(bytes: &[u8]) -> Result<Self, ParseError> {
        Ok(match bytes.len() {
            2 => [bytes[0], bytes[1]].into(),
            4 => [bytes[0], bytes[1], bytes[2], bytes[3]].into(),
            _ => Self(bytes.to_vec().try_into().map_err(|_| ParseError::OutOfBoundsAccess)?),
        })
    }
}

impl From<AttUuid> for UuidBuilder {
    fn from(value: AttUuid) -> Self {
        // TODO: compress to UUID-16 if possible
        UuidBuilder { data: value.0.into_iter().map(|x| x as u64).collect() }
    }
}

impl TryFrom<AttUuid> for Uuid16Builder {
    type Error = AttUuid;

    fn try_from(value: AttUuid) -> Result<Self, Self::Error> {
        // check if the first 14 bytes match the default value
        let default = AttUuid::from([0, 0, 0, 0]);
        for i in 0..12 {
            if default.0[i] != value.0[i] {
                return Err(value);
            }
        }
        if value.0[14] != 0 || value.0[15] != 0 {
            return Err(value);
        }
        Ok(Uuid16Builder {
            data: value.0[12..=13].iter().map(|x| *x as u64).collect::<Vec<_>>().into_boxed_slice(),
        })
    }
}

impl From<AttUuid> for Uuid128Builder {
    fn from(value: AttUuid) -> Self {
        Uuid128Builder { data: value.0.into_iter().map(|x| x as u64).collect() }
    }
}

impl From<&Uuid> for AttUuid {
    fn from(uuid: &Uuid) -> Self {
        let mut bytes = get_128_be_uuid_bytes(uuid).to_owned();
        bytes.reverse();
        Self(bytes)
    }
}

#[derive(Debug, Clone)]
pub struct AttAttribute {
    pub handle: AttHandle,
    pub uuid: AttUuid,
    pub permissions: AttPermissions,
}

/// The attribute properties supported by the current GATT server implementation
/// Unimplemented properties will default to false.
#[derive(Debug, Clone)]
pub struct AttPermissions {
    /// Whether an attribute is readable
    pub readable: bool,
    /// Whether an attribute is writable
    /// (using ATT_WRITE_REQ, so a response is expected)
    pub writable: bool,
}

#[async_trait(?Send)]
pub trait AttDatabase {
    async fn read_attribute(
        &self,
        handle: AttHandle,
    ) -> Result<AttAttributeDataChild, AttErrorCode>;
    async fn write_attribute(
        &self,
        handle: AttHandle,
        data: AttAttributeDataView<'_>,
    ) -> Result<(), AttErrorCode>;
    fn list_attributes(&self) -> Vec<AttAttribute>;
}
