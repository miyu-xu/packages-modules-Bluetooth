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
    # [error ("when found {} bytes of extra data when parsing {obj}::{child}: {data:#04x?}" , . data . len ())]
    TrailingDataError { obj: String, child: String, data: Vec<u8> },
}
impl Error {
    #[doc = r" Construct a new `TrailingDataError` variant."]
    #[doc = r""]
    #[doc = r" The data will be truncated to max 1024 bytes."]
    fn new_trailing_data_error(obj: &str, child: &str, data: &[u8]) -> Error {
        Error::TrailingDataError {
            obj: obj.into(),
            child: child.into(),
            data: data[..std::cmp::min(data.len(), 1024)].into(),
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
pub struct Parent {
    pub foo: Enum16,
    pub bar: Enum16,
    pub baz: Enum16,
    pub payload: Vec<u8>,
}
impl Parent {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 7
    }
    pub fn parse(bytes: &[u8]) -> Result<Self> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        if !cell.get().is_empty() {
            return Err(Error::new_trailing_data_error("Parent", "<parse_inner>", cell.get()));
        }
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self> {
        if bytes.get().remaining() < 2 {
            return Err(Error::InvalidLengthError {
                obj: "Parent".to_string(),
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let foo = Enum16::from_u16(bytes.get_mut().get_u16()).unwrap();
        if bytes.get().remaining() < 2 {
            return Err(Error::InvalidLengthError {
                obj: "Parent".to_string(),
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let bar = Enum16::from_u16(bytes.get_mut().get_u16()).unwrap();
        if bytes.get().remaining() < 2 {
            return Err(Error::InvalidLengthError {
                obj: "Parent".to_string(),
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let baz = Enum16::from_u16(bytes.get_mut().get_u16()).unwrap();
        if bytes.get().remaining() < 1 {
            return Err(Error::InvalidLengthError {
                obj: "Parent".to_string(),
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let payload_size = bytes.get_mut().get_u8() as usize;
        if bytes.get().remaining() < payload_size {
            return Err(Error::InvalidLengthError {
                obj: "Parent".to_string(),
                wanted: payload_size,
                got: bytes.get().remaining(),
            });
        }
        let payload = &bytes.get()[..payload_size];
        bytes.get_mut().advance(payload_size);
        Ok(Self { foo, bar, baz, payload })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        buffer.put_u16(self.foo.to_u16().unwrap());
        buffer.put_u16(self.bar.to_u16().unwrap());
        buffer.put_u16(self.baz.to_u16().unwrap());
        if self.child.get_total_size() > 0xff as usize {
            panic!(
                "Invalid length for {}::{}: {} > {}",
                "Parent",
                "_payload_",
                self.child.get_total_size(),
                0xff
            );
        }
        buffer.put_u8(self.child.get_total_size() as u8);
        match &self.child {
            ParentDataChild::Child(child) => child.write_to(buffer),
            ParentDataChild::Payload(payload) => buffer.put_slice(payload),
            ParentDataChild::None => {}
        }
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        7 + self.child.get_total_size()
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct Child {
    pub quux: Enum16,
    pub payload: Vec<u8>,
}
impl Child {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 2
    }
    pub fn parse(bytes: &[u8]) -> Result<Self> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        if !cell.get().is_empty() {
            return Err(Error::new_trailing_data_error("Child", "<parse_inner>", cell.get()));
        }
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self> {
        if bytes.get().remaining() < 2 {
            return Err(Error::InvalidLengthError {
                obj: "Child".to_string(),
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let quux = Enum16::from_u16(bytes.get_mut().get_u16()).unwrap();
        let payload = bytes.get();
        bytes.get_mut().advance(payload.len());
        Ok(Self { quux, payload })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        buffer.put_u16(self.quux.to_u16().unwrap());
        match &self.child {
            ChildDataChild::GrandChild(child) => child.write_to(buffer),
            ChildDataChild::Payload(payload) => buffer.put_slice(payload),
            ChildDataChild::None => {}
        }
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        2 + self.child.get_total_size()
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct GrandChild {
    pub payload: Vec<u8>,
}
impl GrandChild {
    fn conforms(bytes: &[u8]) -> bool {
        true
    }
    pub fn parse(bytes: &[u8]) -> Result<Self> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        if !cell.get().is_empty() {
            return Err(Error::new_trailing_data_error("GrandChild", "<parse_inner>", cell.get()));
        }
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self> {
        let payload = bytes.get();
        bytes.get_mut().advance(payload.len());
        Ok(Self { payload })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        match &self.child {
            GrandChildDataChild::GrandGrandChild(child) => child.write_to(buffer),
            GrandChildDataChild::Payload(payload) => buffer.put_slice(payload),
            GrandChildDataChild::None => {}
        }
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        self.child.get_total_size()
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct GrandGrandChild {
    pub payload: Vec<u8>,
}
impl GrandGrandChild {
    fn conforms(bytes: &[u8]) -> bool {
        true
    }
    pub fn parse(bytes: &[u8]) -> Result<Self> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        if !cell.get().is_empty() {
            return Err(Error::new_trailing_data_error(
                "GrandGrandChild",
                "<parse_inner>",
                cell.get(),
            ));
        }
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self> {
        let payload = bytes.get();
        bytes.get_mut().advance(payload.len());
        Ok(Self { payload })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        match &self.child {
            GrandGrandChildDataChild::Payload(payload) => buffer.put_slice(payload),
            GrandGrandChildDataChild::None => {}
        }
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        self.child.get_total_size()
    }
}
