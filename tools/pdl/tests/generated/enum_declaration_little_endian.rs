// @generated rust packets from test

use bytes::{Buf, BufMut, Bytes, BytesMut};
use std::cell::Cell;
use std::convert::{TryFrom, TryInto};
use std::fmt;
use std::sync::Arc;
use thiserror::Error;

type Result<T> = std::result::Result<T, Error>;

#[derive(Debug, Clone, Copy, Hash, PartialEq, Eq, PartialOrd, Ord)]
pub struct Private<T>(T);
impl<T> std::ops::Deref for Private<T> {
    type Target = T;
    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

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

#[derive(Debug, Clone, Copy, Hash, Eq, PartialEq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
#[cfg_attr(feature = "serde", serde(from = "u8", into = "u8"))]
pub enum IncompleteTruncated {
    A,
    B,
    Unknown(Private<u8>),
}
impl From<u8> for IncompleteTruncated {
    fn from(value: u8) -> Self {
        match value & 0x7 {
            0x0 => IncompleteTruncated::A,
            0x1 => IncompleteTruncated::B,
            value => IncompleteTruncated::Unknown(Private(value)),
        }
    }
}
impl From<IncompleteTruncated> for u8 {
    fn from(value: IncompleteTruncated) -> Self {
        match value {
            IncompleteTruncated::A => 0x0,
            IncompleteTruncated::B => 0x1,
            IncompleteTruncated::Unknown(Private(value)) => value,
        }
    }
}

#[derive(Debug, Clone, Copy, Hash, Eq, PartialEq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
#[cfg_attr(feature = "serde", serde(from = "u8", into = "u8"))]
pub enum IncompleteTruncatedWithRange {
    A,
    X,
    Y,
    B(Private<u8>),
    Unknown(Private<u8>),
}
impl From<u8> for IncompleteTruncatedWithRange {
    fn from(value: u8) -> Self {
        match value & 0x7 {
            0x0 => IncompleteTruncatedWithRange::A,
            0x1 => IncompleteTruncatedWithRange::X,
            0x2 => IncompleteTruncatedWithRange::Y,
            0x1..=0x6 => IncompleteTruncatedWithRange::B(Private(value)),
            value => IncompleteTruncatedWithRange::Unknown(Private(value)),
        }
    }
}
impl From<IncompleteTruncatedWithRange> for u8 {
    fn from(value: IncompleteTruncatedWithRange) -> Self {
        match value {
            IncompleteTruncatedWithRange::A => 0x0,
            IncompleteTruncatedWithRange::X => 0x1,
            IncompleteTruncatedWithRange::Y => 0x2,
            IncompleteTruncatedWithRange::B(Private(value)) => value,
            IncompleteTruncatedWithRange::Unknown(Private(value)) => value,
        }
    }
}

#[derive(Debug, Clone, Copy, Hash, Eq, PartialEq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
#[cfg_attr(feature = "serde", serde(from = "u8", into = "u8"))]
pub enum CompleteTruncated {
    A,
    B,
    C,
    D,
    E,
    F,
    G,
    H,
}
impl From<u8> for CompleteTruncated {
    fn from(value: u8) -> Self {
        match value & 0x7 {
            0x0 => CompleteTruncated::A,
            0x1 => CompleteTruncated::B,
            0x2 => CompleteTruncated::C,
            0x3 => CompleteTruncated::D,
            0x4 => CompleteTruncated::E,
            0x5 => CompleteTruncated::F,
            0x6 => CompleteTruncated::G,
            0x7 => CompleteTruncated::H,
            _ => unreachable!(),
        }
    }
}
impl From<CompleteTruncated> for u8 {
    fn from(value: CompleteTruncated) -> Self {
        match value {
            CompleteTruncated::A => 0x0,
            CompleteTruncated::B => 0x1,
            CompleteTruncated::C => 0x2,
            CompleteTruncated::D => 0x3,
            CompleteTruncated::E => 0x4,
            CompleteTruncated::F => 0x5,
            CompleteTruncated::G => 0x6,
            CompleteTruncated::H => 0x7,
        }
    }
}

#[derive(Debug, Clone, Copy, Hash, Eq, PartialEq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
#[cfg_attr(feature = "serde", serde(from = "u8", into = "u8"))]
pub enum CompleteTruncatedWithRange {
    A,
    X,
    Y,
    B(Private<u8>),
}
impl From<u8> for CompleteTruncatedWithRange {
    fn from(value: u8) -> Self {
        match value & 0x7 {
            0x0 => CompleteTruncatedWithRange::A,
            0x1 => CompleteTruncatedWithRange::X,
            0x2 => CompleteTruncatedWithRange::Y,
            0x1..=0x7 => CompleteTruncatedWithRange::B(Private(value)),
            _ => unreachable!(),
        }
    }
}
impl From<CompleteTruncatedWithRange> for u8 {
    fn from(value: CompleteTruncatedWithRange) -> Self {
        match value {
            CompleteTruncatedWithRange::A => 0x0,
            CompleteTruncatedWithRange::X => 0x1,
            CompleteTruncatedWithRange::Y => 0x2,
            CompleteTruncatedWithRange::B(Private(value)) => value,
        }
    }
}

#[derive(Debug, Clone, Copy, Hash, Eq, PartialEq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
#[cfg_attr(feature = "serde", serde(from = "u8", into = "u8"))]
pub enum CompleteWithRange {
    A,
    B,
    C(Private<u8>),
}
impl From<u8> for CompleteWithRange {
    fn from(value: u8) -> Self {
        match value {
            0x0 => CompleteWithRange::A,
            0x1 => CompleteWithRange::B,
            0x2..=0xff => CompleteWithRange::C(Private(value)),
        }
    }
}
impl From<CompleteWithRange> for u8 {
    fn from(value: CompleteWithRange) -> Self {
        match value {
            CompleteWithRange::A => 0x0,
            CompleteWithRange::B => 0x1,
            CompleteWithRange::C(Private(value)) => value,
        }
    }
}
