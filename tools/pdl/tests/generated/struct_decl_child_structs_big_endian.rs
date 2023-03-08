// @generated rust packets from test

use bytes::{Buf, BufMut, Bytes, BytesMut};
use num_derive::{FromPrimitive, ToPrimitive};
use num_traits::{FromPrimitive, ToPrimitive};
use std::cell::Cell;
use std::convert::{TryFrom, TryInto};
use std::fmt;
use std::sync::Arc;
use thiserror::Error;

type Result<T> = std::result::Result<T, Error>;

#[derive(Debug, Error)]
pub enum Error {
    #[error("Packet parsing failed")]
    InvalidPacketError,
    #[error("{field} was {value:x}, which is not known")]
    ConstraintOutOfBounds { field: String, value: u64 },
    #[error("Got {actual:x}, expected {expected:x}")]
    InvalidFixedValue { expected: u64, actual: u64 },
    #[error("when parsing {obj} needed length of {wanted} but got {got}")]
    InvalidLengthError { obj: String, wanted: usize, got: usize },
    #[error("array size ({array} bytes) is not a multiple of the element size ({element} bytes)")]
    InvalidArraySize { array: usize, element: usize },
    #[error("Due to size restrictions a struct could not be parsed.")]
    ImpossibleStructError,
    #[error("when parsing field {obj}.{field}, {value} is not a valid {type_} value")]
    InvalidEnumValueError { obj: String, field: String, value: u64, type_: String },
    #[error("Found {len_trailing} trailing bytes when parsing {obj}::{child} from {data:x?}")]
    TrailingDataError { obj: String, child: String, data: Vec<u8>, len_trailing: usize },
}
impl Error {
    #[doc = r" Construct a new `TrailingDataError` variant."]
    #[doc = r""]
    #[doc = r" The data will be truncated to max 1024 bytes."]
    fn new_trailing_data_error(obj: &str, child: &str, data: &[u8], len_trailing: usize) -> Error {
        Error::TrailingDataError {
            obj: obj.into(),
            child: child.into(),
            data: data[..std::cmp::min(data.len(), 1024)].into(),
            len_trailing,
        }
    }
}

#[derive(Debug, Error)]
#[error("{0}")]
pub struct TryFromError(&'static str);

pub trait Packet {
    fn to_bytes(self) -> Bytes;
    fn to_vec(self) -> Vec<u8>;
}

#[derive(FromPrimitive, ToPrimitive, Debug, Hash, Eq, PartialEq, Clone, Copy)]
#[repr(u64)]
pub enum Enum16 {
    A = 0x1,
    B = 0x2,
}
#[cfg(feature = "serde")]
impl serde::Serialize for Enum16 {
    fn serialize<S>(&self, serializer: S) -> std::result::Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        serializer.serialize_u64(*self as u64)
    }
}
#[cfg(feature = "serde")]
struct Enum16Visitor;
#[cfg(feature = "serde")]
impl<'de> serde::de::Visitor<'de> for Enum16Visitor {
    type Value = Enum16;
    fn expecting(&self, formatter: &mut std::fmt::Formatter) -> std::fmt::Result {
        formatter.write_str("a valid discriminant")
    }
    fn visit_u64<E>(self, value: u64) -> std::result::Result<Self::Value, E>
    where
        E: serde::de::Error,
    {
        match value {
            0x1 => Ok(Enum16::A),
            0x2 => Ok(Enum16::B),
            _ => Err(E::custom(format!("invalid discriminant: {value}"))),
        }
    }
}
#[cfg(feature = "serde")]
impl<'de> serde::Deserialize<'de> for Enum16 {
    fn deserialize<D>(deserializer: D) -> std::result::Result<Self, D::Error>
    where
        D: serde::Deserializer<'de>,
    {
        deserializer.deserialize_u64(Enum16Visitor)
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct Foo {
    pub a: u8,
    pub b: Enum16,
    pub payload: Vec<u8>,
}
impl Foo {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 4
    }
    pub fn parse(bytes: &[u8]) -> Result<Self> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        if !cell.get().is_empty() {
            return Err(Error::new_trailing_data_error(
                "Foo",
                "<parse_inner>",
                bytes,
                cell.get().len(),
            ));
        }
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self> {
        if bytes.get().remaining() < 1 {
            return Err(Error::InvalidLengthError {
                obj: "Foo".to_string(),
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let a = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 2 {
            return Err(Error::InvalidLengthError {
                obj: "Foo".to_string(),
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let b = Enum16::from_u16(bytes.get_mut().get_u16()).unwrap();
        if bytes.get().remaining() < 1 {
            return Err(Error::InvalidLengthError {
                obj: "Foo".to_string(),
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let payload_size = bytes.get_mut().get_u8() as usize;
        if bytes.get().remaining() < payload_size {
            return Err(Error::InvalidLengthError {
                obj: "Foo".to_string(),
                wanted: payload_size,
                got: bytes.get().remaining(),
            });
        }
        let payload = &bytes.get()[..payload_size];
        bytes.get_mut().advance(payload_size);
        Ok(Self { a, b, payload })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        buffer.put_u8(self.a);
        buffer.put_u16(self.b.to_u16().unwrap());
        if self.child.get_total_size() > 0xff as usize {
            panic!(
                "Invalid length for {}::{}: {} > {}",
                "Foo",
                "_payload_",
                self.child.get_total_size(),
                0xff
            );
        }
        buffer.put_u8(self.child.get_total_size() as u8);
        match &self.child {
            FooDataChild::Bar(child) => child.write_to(buffer),
            FooDataChild::Baz(child) => child.write_to(buffer),
            FooDataChild::Payload(payload) => buffer.put_slice(payload),
            FooDataChild::None => {}
        }
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        4 + self.child.get_total_size()
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct Bar {
    pub x: u8,
}
impl Bar {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 1
    }
    pub fn parse(bytes: &[u8]) -> Result<Self> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        if !cell.get().is_empty() {
            return Err(Error::new_trailing_data_error(
                "Bar",
                "<parse_inner>",
                bytes,
                cell.get().len(),
            ));
        }
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self> {
        if bytes.get().remaining() < 1 {
            return Err(Error::InvalidLengthError {
                obj: "Bar".to_string(),
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let x = bytes.get_mut().get_u8();
        Ok(Self { x })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        buffer.put_u8(self.x);
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        1
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct Baz {
    pub y: u16,
}
impl Baz {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 2
    }
    pub fn parse(bytes: &[u8]) -> Result<Self> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        if !cell.get().is_empty() {
            return Err(Error::new_trailing_data_error(
                "Baz",
                "<parse_inner>",
                bytes,
                cell.get().len(),
            ));
        }
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self> {
        if bytes.get().remaining() < 2 {
            return Err(Error::InvalidLengthError {
                obj: "Baz".to_string(),
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let y = bytes.get_mut().get_u16();
        Ok(Self { y })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        buffer.put_u16(self.y);
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        2
    }
}
