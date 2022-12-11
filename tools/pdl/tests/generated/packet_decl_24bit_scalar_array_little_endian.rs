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
struct FooData {
    x: [u32; 5],
}
#[derive(Debug, Clone)]
pub struct Foo {
    foo: Arc<FooData>,
}
#[derive(Debug)]
pub struct FooBuilder {
    pub x: [u32; 5],
}
impl FooData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 15
    }
    fn parse(mut bytes: &[u8]) -> Result<Self> {
        if bytes.remaining() < 5 * 3 {
            panic!(
                "Invalid packet size for {}::{}: expected {} bytes, got {}",
                "Foo",
                "x",
                bytes.remaining(),
                5 * 3
            );
        }
        let x = std::array::from_fn(|_| bytes.get_uint_le(3) as u32);
        Ok(Self { x })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        for elem in &self.x {
            buffer.put_uint_le(*elem as u64, 3);
        }
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        15
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
    pub fn get_x(&self) -> &[u32; 5] {
        &self.foo.as_ref().x
    }
}
impl FooBuilder {
    pub fn build(self) -> Foo {
        let foo = Arc::new(FooData { x: self.x });
        Foo::new(foo).unwrap()
    }
}
