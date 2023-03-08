// @generated rust packets from foo.pdl

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
