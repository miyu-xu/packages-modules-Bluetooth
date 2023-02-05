use async_trait::async_trait;

use crate::{
    core::{get_128_be_uuid_bytes, CxxUuid},
    gatt::ids::AttHandle,
    packets::{
        AttAttributeDataChild, AttErrorCode, AttHandleBuilder, AttHandleView, ParseError,
        Uuid128Builder, Uuid128View, Uuid16Builder, Uuid16View, UuidBuilder, UuidView,
    },
};

pub const PRIMARY_SERVICE_DECLARATION_UUID: Uuid = Uuid::new([0x00, 0x28, 0x00, 0x00]);
pub const CHARACTERISTIC_UUID: Uuid = Uuid::new([0x03, 0x28, 0x00, 0x00]);

impl From<AttHandleView<'_>> for AttHandle {
    fn from(value: AttHandleView) -> Self {
        AttHandle(value.get_handle() as u16)
    }
}

impl From<AttHandle> for AttHandleBuilder {
    fn from(value: AttHandle) -> Self {
        AttHandleBuilder { handle: value.0 }
    }
}

/// A UUID (See Core Spec 5.3 Vol 1E 2.9.1. Basic Types)
#[derive(PartialEq, Eq, Clone, Copy, Debug)]
pub struct Uuid(pub [u8; 16]);

impl Uuid {
    /// Constructor, from a 4-byte UUID.
    ///
    /// Accepts only 4-bytes, with the 32-byte constructor living in the From<>
    /// implementations, so it can be used it in a const context.
    pub const fn new(bytes: [u8; 4]) -> Self {
        // Magic constant from Core Spec 5.3 Vol.3 B.2.5.1
        Self([
            0xFB, 0x34, 0x9B, 0x5F, 0x80, 0x00, 0x00, 0x80, 0x00, 0x10, 0x00, 0x00, bytes[0],
            bytes[1], bytes[2], bytes[3],
        ])
    }
}

impl TryFrom<UuidView<'_>> for Uuid {
    type Error = ParseError;

    fn try_from(value: UuidView<'_>) -> Result<Self, ParseError> {
        let bytes = value.get_data_iter().collect::<Vec<_>>();
        bytes[..].try_into()
    }
}

impl From<Uuid16View<'_>> for Uuid {
    fn from(uuid: Uuid16View) -> Self {
        let bytes: [u8; 2] = uuid
            .get_data_iter()
            .collect::<Vec<_>>()
            .try_into()
            .expect("Uuid16View MUST have exactly 2 bytes");
        bytes.into()
    }
}

impl From<Uuid128View<'_>> for Uuid {
    fn from(uuid: Uuid128View) -> Self {
        let bytes = uuid
            .get_data_iter()
            .collect::<Vec<_>>()
            .try_into()
            .expect("Uuid128View MUST have exactly 16 bytes");
        Self(bytes)
    }
}

impl From<[u8; 2]> for Uuid {
    fn from(bytes: [u8; 2]) -> Self {
        [bytes[0], bytes[1], 0x00, 0x00].into()
    }
}

impl From<[u8; 4]> for Uuid {
    fn from(bytes: [u8; 4]) -> Self {
        Uuid::new(bytes)
    }
}

impl TryFrom<&[u8]> for Uuid {
    type Error = ParseError;

    fn try_from(bytes: &[u8]) -> Result<Self, ParseError> {
        Ok(match bytes.len() {
            2 => [bytes[0], bytes[1]].into(),
            4 => [bytes[0], bytes[1], bytes[2], bytes[3]].into(),
            _ => Self(bytes.to_vec().try_into().map_err(|_| ParseError::OutOfBoundsAccess)?),
        })
    }
}

impl From<Uuid> for UuidBuilder {
    fn from(value: Uuid) -> Self {
        // TODO(aryarahul): compress to UUID-16 if possible
        UuidBuilder { data: value.0.into_iter().collect() }
    }
}

impl TryFrom<Uuid> for Uuid16Builder {
    type Error = Uuid;

    fn try_from(value: Uuid) -> Result<Self, Self::Error> {
        // check if the first 14 bytes match the default value
        let default = Uuid::from([0, 0, 0, 0]);
        for i in 0..12 {
            if default.0[i] != value.0[i] {
                return Err(value);
            }
        }
        if value.0[14] != 0 || value.0[15] != 0 {
            return Err(value);
        }
        Ok(Uuid16Builder { data: value.0[12..=13].into() })
    }
}

impl From<Uuid> for Uuid128Builder {
    fn from(value: Uuid) -> Self {
        Uuid128Builder { data: value.0.into_iter().collect() }
    }
}

impl From<&CxxUuid> for Uuid {
    fn from(uuid: &CxxUuid) -> Self {
        let mut bytes = get_128_be_uuid_bytes(uuid).to_owned();
        bytes.reverse();
        Self(bytes)
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AttAttribute {
    pub handle: AttHandle,
    pub type_: Uuid,
    pub permissions: AttPermissions,
}

/// The attribute properties supported by the current GATT server implementation
/// Unimplemented properties will default to false.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AttPermissions {
    /// Whether an attribute is readable
    pub readable: bool,
    /// Whether an attribute is writable
    /// (using ATT_WRITE_REQ, so a response is expected)
    pub writable: bool,
}

#[async_trait(?Send)]
pub trait AttDatabase {
    /// Read an attribute by handle
    async fn read_attribute(
        &self,
        handle: AttHandle,
    ) -> Result<AttAttributeDataChild, AttErrorCode>;
    fn list_attributes(&self) -> Vec<AttAttribute>;

    fn find_attribute(&self, handle: AttHandle) -> Option<AttAttribute> {
        self.list_attributes().into_iter().find(|attr| attr.handle == handle)
    }
}

#[cfg(test)]
mod test {
    use crate::{gatt::ffi::CxxUuid, utils::packet::build_view_or_crash};

    use super::*;

    #[test]
    fn test_uuid16_builder_successful() {
        let uuid = Uuid::new([1, 2, 0, 0]);
        let builder: Uuid16Builder = uuid.try_into().unwrap();
        assert_eq!(builder.data.into_vec(), vec![1, 2]);
    }

    #[test]
    fn test_uuid16_builder_fail_nonzero_trailing_bytes() {
        let uuid = Uuid::new([1, 2, 0, 1]);
        let res: Result<Uuid16Builder, _> = uuid.try_into();
        assert!(res.is_err());
    }

    #[test]
    fn test_uuid16_builder_fail_invalid_prefix() {
        let mut uuid = Uuid::new([1, 2, 0, 0]);
        uuid.0[0] = 1;

        let res: Result<Uuid16Builder, _> = uuid.try_into();
        assert!(res.is_err());
    }

    #[test]
    fn test_uuid128_builder() {
        let uuid = Uuid::new([1, 2, 3, 4]);
        let builder: Uuid128Builder = uuid.into();
        assert_eq!(builder.data[..], uuid.0);
    }

    #[test]
    fn test_uuid_builder() {
        let uuid = Uuid::new([1, 2, 3, 4]);
        let builder: UuidBuilder = uuid.into();
        assert_eq!(builder.data[..], uuid.0);
    }

    #[test]
    fn test_uuid_from_4_array() {
        let x = Uuid::new([1, 2, 3, 4]);
        let y = Uuid::from([1, 2, 3, 4]);
        assert_eq!(x, y);
    }

    #[test]
    fn test_uuid_from_2_array() {
        let expected = Uuid::new([1, 2, 0, 0]);
        let actual = Uuid::from([1, 2]);
        assert_eq!(expected, actual);
    }

    #[test]
    fn test_uuid_try_from_2_slice() {
        let expected = Uuid::new([1, 2, 0, 0]);
        let actual = Uuid::try_from(&vec![1, 2][..]).unwrap();
        assert_eq!(expected, actual);
    }

    #[test]
    fn test_uuid_try_from_4_slice() {
        let expected = Uuid::new([1, 2, 3, 4]);
        let actual = Uuid::try_from(&vec![1, 2, 3, 4][..]).unwrap();
        assert_eq!(expected, actual);
    }

    #[test]
    fn test_uuid_try_from_16_slice() {
        let data = vec![1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16];
        let uuid = Uuid::try_from(&data[..]).unwrap();
        assert_eq!(&uuid.0, &data[..]);
    }

    #[test]
    fn test_uuid_try_from_invalid_slice() {
        let data = vec![1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
        let res = Uuid::try_from(&data[..]);
        assert!(res.is_err());
    }

    #[test]
    fn test_uuid_try_from_view() {
        let data = build_view_or_crash(UuidBuilder { data: vec![1, 2, 3, 4].into() });
        let uuid = Uuid::try_from(data.view()).unwrap();
        assert_eq!(uuid, Uuid::new([1, 2, 3, 4]));
    }

    #[test]
    fn test_uuid_from_cxx() {
        let expected = Uuid::new([1, 2, 3, 4]);
        let cxx_uuid = CxxUuid::new_mocked(expected);

        let actual = Uuid::from(cxx_uuid.as_ref());

        assert_eq!(expected, actual);
    }
}
