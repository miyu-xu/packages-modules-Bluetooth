// @generated rust packets from test

#![allow(warnings, missing_docs)]

use bytes::{Buf, BufMut, Bytes, BytesMut};
use num_derive::{FromPrimitive, ToPrimitive};
use num_traits::{FromPrimitive, ToPrimitive};
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
    #[error("when parsing {obj} needed length of {wanted} but got {got}")]
    InvalidLengthError { obj: String, wanted: usize, got: usize },
    #[error("array size ({array} bytes) is not a multiple of the element size ({element} bytes)")]
    InvalidArraySize { array: usize, element: usize },
    #[error("Due to size restrictions a struct could not be parsed.")]
    ImpossibleStructError,
    #[error("when parsing field {obj}.{field}, {value} is not a valid {type_} value")]
    InvalidEnumValueError { obj: String, field: String, value: u64, type_: String },
}

#[derive(Debug, Error)]
#[error("{0}")]
pub struct TryFromError(&'static str);

pub trait Packet {
    fn to_bytes(self) -> Bytes;
    fn to_vec(self) -> Vec<u8>;
}

#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
struct FooData {
    a: Vec<u16>,
}
#[derive(Debug, Clone)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct Foo {
    foo: Arc<FooData>,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FooBuilder {
    pub a: Vec<u16>,
}
impl FooData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 1
    }
    fn parse(mut bytes: &[u8]) -> Result<Self> {
        if bytes.remaining() < 1 {
            return Err(Error::InvalidLengthError {
                obj: "Foo".to_string(),
                wanted: 1,
                got: bytes.remaining(),
            });
        }
        let a_count = bytes.get_u8();
        if bytes.remaining() < a_count as usize {
            return Err(Error::InvalidLengthError {
                obj: "Foo".to_string(),
                wanted: a_count as usize,
                got: bytes.remaining(),
            });
        }
        let __case_6__ = "666";
        let a =
            (0..a_count).map(|_| Ok::<_, Error>(bytes.get_u16_le())).collect::<Result<Vec<_>>>()?;
        Ok(Self { a })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        buffer.put_u8(self.a.len() as u8);
        for elem in &self.a {
            buffer.put_u16_le(*elem);
        }
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        1 + self.a.len() * 2
    }
}
impl Packet for Foo {
    fn to_bytes(self) -> Bytes {
        let mut buffer = BytesMut::with_capacity(self.foo.get_total_size());
        self.foo.write_to(&mut buffer);
        buffer.freeze()
    }
    fn to_vec(self) -> Vec<u8> {
        self.to_bytes().to_vec()
    }
}
impl From<Foo> for Bytes {
    fn from(packet: Foo) -> Self {
        packet.to_bytes()
    }
}
impl From<Foo> for Vec<u8> {
    fn from(packet: Foo) -> Self {
        packet.to_vec()
    }
}
impl Foo {
    pub fn parse(mut bytes: &[u8]) -> Result<Self> {
        Ok(Self::new(Arc::new(FooData::parse(bytes)?)).unwrap())
    }
    fn new(root: Arc<FooData>) -> std::result::Result<Self, &'static str> {
        let foo = root;
        Ok(Self { foo })
    }
    pub fn get_a(&self) -> &Vec<u16> {
        &self.foo.as_ref().a
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        self.foo.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.foo.get_size()
    }
}
impl FooBuilder {
    pub fn build(self) -> Foo {
        let foo = Arc::new(FooData { a: self.a });
        Foo::new(foo).unwrap()
    }
}

#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
struct BarData {
    x: Vec<Foo>,
}
#[derive(Debug, Clone)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct Bar {
    bar: Arc<BarData>,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct BarBuilder {
    pub x: Vec<Foo>,
}
impl BarData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 1
    }
    fn parse(mut bytes: &[u8]) -> Result<Self> {
        if bytes.remaining() < 1 {
            return Err(Error::InvalidLengthError {
                obj: "Bar".to_string(),
                wanted: 1,
                got: bytes.remaining(),
            });
        }
        let x_count = bytes.get_u8();
        let __case_3__ = "333";
        let x = (0..x_count).map(|_| Foo::parse(bytes)).collect::<Result<Vec<_>>>()?;
        Ok(Self { x })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        buffer.put_u8(self.x.len() as u8);
        for elem in &self.x {
            elem.write_to(buffer);
        }
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        1 + self.x.iter().map(|elem| elem.get_size()).sum::<usize>()
    }
}
impl Packet for Bar {
    fn to_bytes(self) -> Bytes {
        let mut buffer = BytesMut::with_capacity(self.bar.get_total_size());
        self.bar.write_to(&mut buffer);
        buffer.freeze()
    }
    fn to_vec(self) -> Vec<u8> {
        self.to_bytes().to_vec()
    }
}
impl From<Bar> for Bytes {
    fn from(packet: Bar) -> Self {
        packet.to_bytes()
    }
}
impl From<Bar> for Vec<u8> {
    fn from(packet: Bar) -> Self {
        packet.to_vec()
    }
}
impl Bar {
    pub fn parse(mut bytes: &[u8]) -> Result<Self> {
        Ok(Self::new(Arc::new(BarData::parse(bytes)?)).unwrap())
    }
    fn new(root: Arc<BarData>) -> std::result::Result<Self, &'static str> {
        let bar = root;
        Ok(Self { bar })
    }
    pub fn get_x(&self) -> &Vec<Foo> {
        &self.bar.as_ref().x
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        self.bar.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.bar.get_size()
    }
}
impl BarBuilder {
    pub fn build(self) -> Bar {
        let bar = Arc::new(BarData { x: self.x });
        Bar::new(bar).unwrap()
    }
}
