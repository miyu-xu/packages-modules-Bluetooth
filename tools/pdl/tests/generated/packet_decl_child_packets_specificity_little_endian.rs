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
pub enum ParentDataChild {
    Foo(Arc<FooData>),
    Bar(Arc<BarData>),
    Payload(Bytes),
    None,
}
impl ParentDataChild {
    fn get_total_size(&self) -> usize {
        match self {
            ParentDataChild::Foo(value) => value.get_total_size(),
            ParentDataChild::Bar(value) => value.get_total_size(),
            ParentDataChild::Payload(bytes) => bytes.len(),
            ParentDataChild::None => 0,
        }
    }
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub enum ParentChild {
    Foo(Foo),
    Bar(Bar),
    Payload(Bytes),
    None,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ParentData {
    a: u8,
    b: u8,
    child: ParentDataChild,
}
#[derive(Debug, Clone)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct Parent {
    #[cfg_attr(feature = "serde", serde(flatten))]
    parent: Arc<ParentData>,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ParentBuilder {
    pub a: u8,
    pub b: u8,
    pub payload: Option<Bytes>,
}
impl ParentData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 3
    }
    fn parse(mut bytes: &mut Cell<&[u8]>) -> Result<Self> {
        if bytes.get().remaining() < 1 {
            return Err(Error::InvalidLengthError {
                obj: "Parent".to_string(),
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let a = bytes.get_mut().get_u8();
        if bytes.get().remaining() < 1 {
            return Err(Error::InvalidLengthError {
                obj: "Parent".to_string(),
                wanted: 1,
                got: bytes.get().remaining(),
            });
        }
        let b = bytes.get_mut().get_u8();
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
        let child = match (a, b) {
            (10, 100) => {
                let mut cell = Cell::new(payload);
                let child_data = BarData::parse(&mut cell)?;
                if !cell.get().is_empty() {
                    return Err(Error::InvalidPacketError);
                }
                ParentDataChild::Bar(Arc::new(child_data))
            }
            (10, _) => {
                let mut cell = Cell::new(payload);
                let child_data = FooData::parse(&mut cell)?;
                if !cell.get().is_empty() {
                    return Err(Error::InvalidPacketError);
                }
                ParentDataChild::Foo(Arc::new(child_data))
            }
            _ if !payload.is_empty() => ParentDataChild::Payload(Bytes::copy_from_slice(payload)),
            _ => ParentDataChild::None,
        };
        Ok(Self { a, b, child })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        buffer.put_u8(self.a);
        buffer.put_u8(self.b);
        if self.child.get_total_size() > 0xff {
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
            ParentDataChild::Foo(child) => child.write_to(buffer),
            ParentDataChild::Bar(child) => child.write_to(buffer),
            ParentDataChild::Payload(payload) => buffer.put_slice(payload),
            ParentDataChild::None => {}
        }
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        3 + self.child.get_total_size()
    }
}
impl Packet for Parent {
    fn to_bytes(self) -> Bytes {
        let mut buffer = BytesMut::with_capacity(self.parent.get_size());
        self.parent.write_to(&mut buffer);
        buffer.freeze()
    }
    fn to_vec(self) -> Vec<u8> {
        self.to_bytes().to_vec()
    }
}
impl From<Parent> for Bytes {
    fn from(packet: Parent) -> Self {
        packet.to_bytes()
    }
}
impl From<Parent> for Vec<u8> {
    fn from(packet: Parent) -> Self {
        packet.to_vec()
    }
}
impl Parent {
    pub fn parse(bytes: &[u8]) -> Result<Self> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        if !cell.get().is_empty() {
            return Err(Error::InvalidPacketError);
        }
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self> {
        let data = ParentData::parse(&mut bytes)?;
        Ok(Self::new(Arc::new(data)).unwrap())
    }
    fn new(parent: Arc<ParentData>) -> std::result::Result<Self, &'static str> {
        Ok(Self { parent })
    }
    pub fn get_a(&self) -> u8 {
        self.parent.as_ref().a
    }
    pub fn get_b(&self) -> u8 {
        self.parent.as_ref().b
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        self.parent.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.parent.get_size()
    }
}
impl ParentBuilder {
    pub fn build(self) -> Parent {
        let parent = Arc::new(ParentData {
            a: self.a,
            b: self.b,
            child: match self.payload {
                None => ParentDataChild::None,
                Some(bytes) => ParentDataChild::Payload(bytes),
            },
        });
        Parent::new(parent).unwrap()
    }
}
impl From<ParentBuilder> for Parent {
    fn from(builder: ParentBuilder) -> Parent {
        builder.build().into()
    }
}

#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FooData {
    x: u8,
}
#[derive(Debug, Clone)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct Foo {
    #[cfg_attr(feature = "serde", serde(flatten))]
    parent: Arc<ParentData>,
    #[cfg_attr(feature = "serde", serde(flatten))]
    foo: Arc<FooData>,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FooBuilder {
    pub b: u8,
    pub x: u8,
}
impl FooData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 1
    }
    fn parse(mut bytes: &mut Cell<&[u8]>) -> Result<Self> {
        if bytes.get().remaining() < 1 {
            return Err(Error::InvalidLengthError {
                obj: "Foo".to_string(),
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
impl Packet for Foo {
    fn to_bytes(self) -> Bytes {
        let mut buffer = BytesMut::with_capacity(self.parent.get_size());
        self.parent.write_to(&mut buffer);
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
impl From<Foo> for Parent {
    fn from(packet: Foo) -> Parent {
        Parent::new(packet.parent).unwrap()
    }
}
impl TryFrom<Parent> for Foo {
    type Error = TryFromError;
    fn try_from(packet: Parent) -> std::result::Result<Foo, TryFromError> {
        Foo::new(packet.parent).map_err(TryFromError)
    }
}
impl Foo {
    pub fn parse(bytes: &[u8]) -> Result<Self> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        if !cell.get().is_empty() {
            return Err(Error::InvalidPacketError);
        }
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self> {
        let data = ParentData::parse(&mut bytes)?;
        Ok(Self::new(Arc::new(data)).unwrap())
    }
    fn new(parent: Arc<ParentData>) -> std::result::Result<Self, &'static str> {
        let foo = match &parent.child {
            ParentDataChild::Foo(value) => value.clone(),
            _ => return Err("Could not parse data, wrong child type"),
        };
        Ok(Self { parent, foo })
    }
    pub fn get_a(&self) -> u8 {
        self.parent.as_ref().a
    }
    pub fn get_b(&self) -> u8 {
        self.parent.as_ref().b
    }
    pub fn get_x(&self) -> u8 {
        self.foo.as_ref().x
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        self.foo.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.parent.get_size()
    }
}
impl FooBuilder {
    pub fn build(self) -> Foo {
        let foo = Arc::new(FooData { x: self.x });
        let parent = Arc::new(ParentData { a: 10, b: self.b, child: ParentDataChild::Foo(foo) });
        Foo::new(parent).unwrap()
    }
}
impl From<FooBuilder> for Parent {
    fn from(builder: FooBuilder) -> Parent {
        builder.build().into()
    }
}
impl From<FooBuilder> for Foo {
    fn from(builder: FooBuilder) -> Foo {
        builder.build().into()
    }
}

#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct BarData {
    y: u16,
}
#[derive(Debug, Clone)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct Bar {
    #[cfg_attr(feature = "serde", serde(flatten))]
    parent: Arc<ParentData>,
    #[cfg_attr(feature = "serde", serde(flatten))]
    bar: Arc<BarData>,
}
#[derive(Debug)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct BarBuilder {
    pub y: u16,
}
impl BarData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 2
    }
    fn parse(mut bytes: &mut Cell<&[u8]>) -> Result<Self> {
        if bytes.get().remaining() < 2 {
            return Err(Error::InvalidLengthError {
                obj: "Bar".to_string(),
                wanted: 2,
                got: bytes.get().remaining(),
            });
        }
        let y = bytes.get_mut().get_u16_le();
        Ok(Self { y })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        buffer.put_u16_le(self.y);
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        2
    }
}
impl Packet for Bar {
    fn to_bytes(self) -> Bytes {
        let mut buffer = BytesMut::with_capacity(self.parent.get_size());
        self.parent.write_to(&mut buffer);
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
impl From<Bar> for Parent {
    fn from(packet: Bar) -> Parent {
        Parent::new(packet.parent).unwrap()
    }
}
impl TryFrom<Parent> for Bar {
    type Error = TryFromError;
    fn try_from(packet: Parent) -> std::result::Result<Bar, TryFromError> {
        Bar::new(packet.parent).map_err(TryFromError)
    }
}
impl Bar {
    pub fn parse(bytes: &[u8]) -> Result<Self> {
        let mut cell = Cell::new(bytes);
        let packet = Self::parse_inner(&mut cell)?;
        if !cell.get().is_empty() {
            return Err(Error::InvalidPacketError);
        }
        Ok(packet)
    }
    fn parse_inner(mut bytes: &mut Cell<&[u8]>) -> Result<Self> {
        let data = ParentData::parse(&mut bytes)?;
        Ok(Self::new(Arc::new(data)).unwrap())
    }
    fn new(parent: Arc<ParentData>) -> std::result::Result<Self, &'static str> {
        let bar = match &parent.child {
            ParentDataChild::Bar(value) => value.clone(),
            _ => return Err("Could not parse data, wrong child type"),
        };
        Ok(Self { parent, bar })
    }
    pub fn get_a(&self) -> u8 {
        self.parent.as_ref().a
    }
    pub fn get_b(&self) -> u8 {
        self.parent.as_ref().b
    }
    pub fn get_y(&self) -> u16 {
        self.bar.as_ref().y
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        self.bar.write_to(buffer)
    }
    pub fn get_size(&self) -> usize {
        self.parent.get_size()
    }
}
impl BarBuilder {
    pub fn build(self) -> Bar {
        let bar = Arc::new(BarData { y: self.y });
        let parent = Arc::new(ParentData { a: 10, b: 100, child: ParentDataChild::Bar(bar) });
        Bar::new(parent).unwrap()
    }
}
impl From<BarBuilder> for Parent {
    fn from(builder: BarBuilder) -> Parent {
        builder.build().into()
    }
}
impl From<BarBuilder> for Bar {
    fn from(builder: BarBuilder) -> Bar {
        builder.build().into()
    }
}
