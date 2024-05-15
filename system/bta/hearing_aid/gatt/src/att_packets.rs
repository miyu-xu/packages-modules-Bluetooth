#![allow(warnings)]
use std::cell::Cell;
use std::convert::{TryFrom, TryInto};
use std::fmt;
use std::result::Result;

/// @generated rust packets from att.pdl.
use bytes::{Buf, BufMut, Bytes, BytesMut};
use pdl_runtime::{DecodeError, EncodeError, Packet};
/// Private prevents users from creating arbitrary scalar values
/// in situations where the value needs to be validated.
/// Users can freely deref the value, but only the backend
/// may create it.
#[derive(Clone, Copy, Hash, PartialEq, Eq, PartialOrd, Ord)]
pub struct Private<T>(T);
impl<T> std::ops::Deref for Private<T> {
    type Target = T;
    fn deref(&self) -> &Self::Target {
        &self.0
    }
}
impl<T: std::fmt::Debug> std::fmt::Debug for Private<T> {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        T::fmt(&self.0, f)
    }
}
use crate::att::AttributeHandle;
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct AttributeValue {
    pub payload: Vec<u8>,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub enum AttributeValueChild {
    ServiceAttributeValueUuid(ServiceAttributeValueUuid),
    ServiceAttributeValueUuid16(ServiceAttributeValueUuid16),
    ServiceAttributeValueUuid128(ServiceAttributeValueUuid128),
    IncludeAttributeValueUuid16(IncludeAttributeValueUuid16),
    IncludeAttributeValueUuid128(IncludeAttributeValueUuid128),
    CharacteristicAttributeValueUuid16(CharacteristicAttributeValueUuid16),
    CharacteristicAttributeValueUuid128(CharacteristicAttributeValueUuid128),
    None,
}
impl AttributeValue {
    pub fn specialize(&self) -> Result<AttributeValueChild, DecodeError> {
        Ok(match () {
            () => AttributeValueChild::ServiceAttributeValueUuid(self.try_into()?),
            () => AttributeValueChild::ServiceAttributeValueUuid16(self.try_into()?),
            () => AttributeValueChild::ServiceAttributeValueUuid128(self.try_into()?),
            () => AttributeValueChild::IncludeAttributeValueUuid16(self.try_into()?),
            () => AttributeValueChild::IncludeAttributeValueUuid128(self.try_into()?),
            () => AttributeValueChild::CharacteristicAttributeValueUuid16(self.try_into()?),
            () => AttributeValueChild::CharacteristicAttributeValueUuid128(self.try_into()?),
            _ => AttributeValueChild::None,
        })
    }
    pub fn payload(&self) -> &[u8] {
        &self.payload
    }
}
impl Packet for AttributeValue {
    fn encoded_len(&self) -> usize {
        self.payload.len()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_slice(&self.payload);
        Ok(())
    }
    fn decode(mut buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let payload = buf.to_vec();
        buf.advance(payload.len());
        let payload = Vec::from(payload);
        Ok((Self { payload }, buf))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct Uuid {
    pub value: Vec<u8>,
}
impl Uuid {
    pub fn value(&self) -> &Vec<u8> {
        &self.value
    }
}
impl Packet for Uuid {
    fn encoded_len(&self) -> usize {
        self.value.len() * 1
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        for elem in &self.value {
            buf.put_u8(*elem);
        }
        Ok(())
    }
    fn decode(mut buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let mut value = Vec::with_capacity(buf.remaining());
        for _ in 0..buf.remaining() {
            value.push(Ok::<_, DecodeError>(buf.get_u8())?);
        }
        Ok((Self { value }, buf))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct Uuid16 {
    pub value: [u8; 2],
}
impl Uuid16 {
    pub fn value(&self) -> &[u8; 2] {
        &self.value
    }
}
impl Packet for Uuid16 {
    fn encoded_len(&self) -> usize {
        self.value.len() * 1
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        for elem in &self.value {
            buf.put_u8(*elem);
        }
        Ok(())
    }
    fn decode(mut buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        if buf.remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "Uuid16",
                wanted: 2,
                got: buf.remaining(),
            });
        }
        let mut value = Vec::with_capacity(2);
        for _ in 0..2 {
            value.push(Ok::<_, DecodeError>(buf.get_u8())?)
        }
        let value = value.try_into().map_err(|_| DecodeError::InvalidPacketError)?;
        Ok((Self { value }, buf))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct Uuid128 {
    pub value: [u8; 16],
}
impl Uuid128 {
    pub fn value(&self) -> &[u8; 16] {
        &self.value
    }
}
impl Packet for Uuid128 {
    fn encoded_len(&self) -> usize {
        self.value.len() * 1
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        for elem in &self.value {
            buf.put_u8(*elem);
        }
        Ok(())
    }
    fn decode(mut buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        if buf.remaining() < 16 {
            return Err(DecodeError::InvalidLengthError {
                obj: "Uuid128",
                wanted: 16,
                got: buf.remaining(),
            });
        }
        let mut value = Vec::with_capacity(16);
        for _ in 0..16 {
            value.push(Ok::<_, DecodeError>(buf.get_u8())?)
        }
        let value = value.try_into().map_err(|_| DecodeError::InvalidPacketError)?;
        Ok((Self { value }, buf))
    }
}
#[repr(u64)]
#[derive(Debug, Clone, Copy, Hash, Eq, PartialEq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
#[cfg_attr(feature = "serde", serde(try_from = "u8", into = "u8"))]
pub enum Opcode {
    AttErrorRsp = 0x1,
    AttExchangeMtuReq = 0x2,
    AttExchangeMtuRsp = 0x3,
    AttFindInformationReq = 0x4,
    AttFindInformationRsp = 0x5,
    AttFindByTypeValueReq = 0x6,
    AttFindByTypeValueRsp = 0x7,
    AttReadByTypeReq = 0x8,
    AttReadByTypeRsp = 0x9,
    AttReadReq = 0xa,
    AttReadRsp = 0xb,
    AttReadBlobReq = 0xc,
    AttReadBlobRsp = 0xd,
    AttReadMultipleReq = 0xe,
    AttReadMultipleRsp = 0xf,
    AttReadByGroupTypeReq = 0x10,
    AttReadByGroupTypeRsp = 0x11,
    AttWriteReq = 0x12,
    AttWriteRsp = 0x13,
    AttWriteCmd = 0x52,
    AttPrepareWriteReq = 0x16,
    AttPrepareWriteRsp = 0x17,
    AttExecuteWriteReq = 0x18,
    AttExecuteWriteRsp = 0x19,
    AttReadMultipleVariableReq = 0x20,
    AttReadMultipleVariableRsp = 0x21,
    AttMultipleHandleValueNtf = 0x23,
    AttHandleValueNtf = 0x1b,
    AttHandleValueInd = 0x1d,
    AttHandleValueCfm = 0x1e,
    AttSignedWriteCmd = 0xd2,
}
impl TryFrom<u8> for Opcode {
    type Error = u8;
    fn try_from(value: u8) -> Result<Self, Self::Error> {
        match value {
            0x1 => Ok(Opcode::AttErrorRsp),
            0x2 => Ok(Opcode::AttExchangeMtuReq),
            0x3 => Ok(Opcode::AttExchangeMtuRsp),
            0x4 => Ok(Opcode::AttFindInformationReq),
            0x5 => Ok(Opcode::AttFindInformationRsp),
            0x6 => Ok(Opcode::AttFindByTypeValueReq),
            0x7 => Ok(Opcode::AttFindByTypeValueRsp),
            0x8 => Ok(Opcode::AttReadByTypeReq),
            0x9 => Ok(Opcode::AttReadByTypeRsp),
            0xa => Ok(Opcode::AttReadReq),
            0xb => Ok(Opcode::AttReadRsp),
            0xc => Ok(Opcode::AttReadBlobReq),
            0xd => Ok(Opcode::AttReadBlobRsp),
            0xe => Ok(Opcode::AttReadMultipleReq),
            0xf => Ok(Opcode::AttReadMultipleRsp),
            0x10 => Ok(Opcode::AttReadByGroupTypeReq),
            0x11 => Ok(Opcode::AttReadByGroupTypeRsp),
            0x12 => Ok(Opcode::AttWriteReq),
            0x13 => Ok(Opcode::AttWriteRsp),
            0x52 => Ok(Opcode::AttWriteCmd),
            0x16 => Ok(Opcode::AttPrepareWriteReq),
            0x17 => Ok(Opcode::AttPrepareWriteRsp),
            0x18 => Ok(Opcode::AttExecuteWriteReq),
            0x19 => Ok(Opcode::AttExecuteWriteRsp),
            0x20 => Ok(Opcode::AttReadMultipleVariableReq),
            0x21 => Ok(Opcode::AttReadMultipleVariableRsp),
            0x23 => Ok(Opcode::AttMultipleHandleValueNtf),
            0x1b => Ok(Opcode::AttHandleValueNtf),
            0x1d => Ok(Opcode::AttHandleValueInd),
            0x1e => Ok(Opcode::AttHandleValueCfm),
            0xd2 => Ok(Opcode::AttSignedWriteCmd),
            _ => Err(value),
        }
    }
}
impl From<&Opcode> for u8 {
    fn from(value: &Opcode) -> Self {
        match value {
            Opcode::AttErrorRsp => 0x1,
            Opcode::AttExchangeMtuReq => 0x2,
            Opcode::AttExchangeMtuRsp => 0x3,
            Opcode::AttFindInformationReq => 0x4,
            Opcode::AttFindInformationRsp => 0x5,
            Opcode::AttFindByTypeValueReq => 0x6,
            Opcode::AttFindByTypeValueRsp => 0x7,
            Opcode::AttReadByTypeReq => 0x8,
            Opcode::AttReadByTypeRsp => 0x9,
            Opcode::AttReadReq => 0xa,
            Opcode::AttReadRsp => 0xb,
            Opcode::AttReadBlobReq => 0xc,
            Opcode::AttReadBlobRsp => 0xd,
            Opcode::AttReadMultipleReq => 0xe,
            Opcode::AttReadMultipleRsp => 0xf,
            Opcode::AttReadByGroupTypeReq => 0x10,
            Opcode::AttReadByGroupTypeRsp => 0x11,
            Opcode::AttWriteReq => 0x12,
            Opcode::AttWriteRsp => 0x13,
            Opcode::AttWriteCmd => 0x52,
            Opcode::AttPrepareWriteReq => 0x16,
            Opcode::AttPrepareWriteRsp => 0x17,
            Opcode::AttExecuteWriteReq => 0x18,
            Opcode::AttExecuteWriteRsp => 0x19,
            Opcode::AttReadMultipleVariableReq => 0x20,
            Opcode::AttReadMultipleVariableRsp => 0x21,
            Opcode::AttMultipleHandleValueNtf => 0x23,
            Opcode::AttHandleValueNtf => 0x1b,
            Opcode::AttHandleValueInd => 0x1d,
            Opcode::AttHandleValueCfm => 0x1e,
            Opcode::AttSignedWriteCmd => 0xd2,
        }
    }
}
impl From<Opcode> for u8 {
    fn from(value: Opcode) -> Self {
        (&value).into()
    }
}
impl From<Opcode> for i16 {
    fn from(value: Opcode) -> Self {
        u8::from(value) as Self
    }
}
impl From<Opcode> for i32 {
    fn from(value: Opcode) -> Self {
        u8::from(value) as Self
    }
}
impl From<Opcode> for i64 {
    fn from(value: Opcode) -> Self {
        u8::from(value) as Self
    }
}
impl From<Opcode> for u16 {
    fn from(value: Opcode) -> Self {
        u8::from(value) as Self
    }
}
impl From<Opcode> for u32 {
    fn from(value: Opcode) -> Self {
        u8::from(value) as Self
    }
}
impl From<Opcode> for u64 {
    fn from(value: Opcode) -> Self {
        u8::from(value) as Self
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct Pdu {
    pub opcode: Opcode,
    pub payload: Vec<u8>,
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub enum PduChild {
    ErrorRsp(ErrorRsp),
    ExchangeMtuReq(ExchangeMtuReq),
    ExchangeMtuRsp(ExchangeMtuRsp),
    FindInformationReq(FindInformationReq),
    FindInformationRsp(FindInformationRsp),
    FindByTypeValueReq(FindByTypeValueReq),
    FindByTypeValueRsp(FindByTypeValueRsp),
    ReadByTypeReq(ReadByTypeReq),
    ReadByTypeRsp(ReadByTypeRsp),
    ReadReq(ReadReq),
    ReadRsp(ReadRsp),
    ReadByGroupTypeReq(ReadByGroupTypeReq),
    ReadByGroupTypeRsp(ReadByGroupTypeRsp),
    None,
}
impl Pdu {
    pub fn specialize(&self) -> Result<PduChild, DecodeError> {
        Ok(match (self.opcode,) {
            (Opcode::AttErrorRsp,) => PduChild::ErrorRsp(self.try_into()?),
            (Opcode::AttExchangeMtuReq,) => PduChild::ExchangeMtuReq(self.try_into()?),
            (Opcode::AttExchangeMtuRsp,) => PduChild::ExchangeMtuRsp(self.try_into()?),
            (Opcode::AttFindInformationReq,) => PduChild::FindInformationReq(self.try_into()?),
            (Opcode::AttFindInformationRsp,) => PduChild::FindInformationRsp(self.try_into()?),
            (Opcode::AttFindByTypeValueReq,) => PduChild::FindByTypeValueReq(self.try_into()?),
            (Opcode::AttFindByTypeValueRsp,) => PduChild::FindByTypeValueRsp(self.try_into()?),
            (Opcode::AttReadByTypeReq,) => PduChild::ReadByTypeReq(self.try_into()?),
            (Opcode::AttReadByTypeRsp,) => PduChild::ReadByTypeRsp(self.try_into()?),
            (Opcode::AttReadReq,) => PduChild::ReadReq(self.try_into()?),
            (Opcode::AttReadRsp,) => PduChild::ReadRsp(self.try_into()?),
            (Opcode::AttReadByGroupTypeReq,) => PduChild::ReadByGroupTypeReq(self.try_into()?),
            (Opcode::AttReadByGroupTypeRsp,) => PduChild::ReadByGroupTypeRsp(self.try_into()?),
            _ => PduChild::None,
        })
    }
    pub fn payload(&self) -> &[u8] {
        &self.payload
    }
    pub fn opcode(&self) -> Opcode {
        self.opcode
    }
}
impl Packet for Pdu {
    fn encoded_len(&self) -> usize {
        1 + self.payload.len()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(u8::from(self.opcode()));
        buf.put_slice(&self.payload);
        Ok(())
    }
    fn decode(mut buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        if buf.remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "Pdu",
                wanted: 1,
                got: buf.remaining(),
            });
        }
        let opcode = Opcode::try_from(buf.get_u8()).map_err(|unknown_val| {
            DecodeError::InvalidEnumValueError {
                obj: "Pdu",
                field: "opcode",
                value: unknown_val as u64,
                type_: "Opcode",
            }
        })?;
        let payload = buf.to_vec();
        buf.advance(payload.len());
        Ok((Self { payload, opcode }, buf))
    }
}
#[derive(Debug, Clone, Copy, Hash, Eq, PartialEq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
#[cfg_attr(feature = "serde", serde(try_from = "u8", into = "u8"))]
pub enum ErrorCode {
    InvalidHandle,
    ReadNotPermitted,
    WriteNotPermitted,
    InvalidPdu,
    InsufficientAuthentication,
    RequestNotSupported,
    InvalidOffset,
    InsufficientAuthorization,
    PrepareQueueFull,
    AttributeNotFound,
    AttributeNotLong,
    EncryptionKeySizeTooShort,
    InvalidAttributeValueLength,
    UnlikelyError,
    InsufficientEncryption,
    UnsupportedGroupType,
    InsufficientResources,
    DatabaseOutOfSync,
    ValueNotAllowed,
    ApplicationError(Private<u8>),
    CommonProfileAndServiceErrorCodes(Private<u8>),
}
impl TryFrom<u8> for ErrorCode {
    type Error = u8;
    fn try_from(value: u8) -> Result<Self, Self::Error> {
        match value {
            0x1 => Ok(ErrorCode::InvalidHandle),
            0x2 => Ok(ErrorCode::ReadNotPermitted),
            0x3 => Ok(ErrorCode::WriteNotPermitted),
            0x4 => Ok(ErrorCode::InvalidPdu),
            0x5 => Ok(ErrorCode::InsufficientAuthentication),
            0x6 => Ok(ErrorCode::RequestNotSupported),
            0x7 => Ok(ErrorCode::InvalidOffset),
            0x8 => Ok(ErrorCode::InsufficientAuthorization),
            0x9 => Ok(ErrorCode::PrepareQueueFull),
            0xa => Ok(ErrorCode::AttributeNotFound),
            0xb => Ok(ErrorCode::AttributeNotLong),
            0xc => Ok(ErrorCode::EncryptionKeySizeTooShort),
            0xd => Ok(ErrorCode::InvalidAttributeValueLength),
            0xe => Ok(ErrorCode::UnlikelyError),
            0xf => Ok(ErrorCode::InsufficientEncryption),
            0x10 => Ok(ErrorCode::UnsupportedGroupType),
            0x11 => Ok(ErrorCode::InsufficientResources),
            0x12 => Ok(ErrorCode::DatabaseOutOfSync),
            0x13 => Ok(ErrorCode::ValueNotAllowed),
            0x80..=0x9f => Ok(ErrorCode::ApplicationError(Private(value))),
            0xe0..=0xff => Ok(ErrorCode::CommonProfileAndServiceErrorCodes(Private(value))),
            _ => Err(value),
        }
    }
}
impl From<&ErrorCode> for u8 {
    fn from(value: &ErrorCode) -> Self {
        match value {
            ErrorCode::InvalidHandle => 0x1,
            ErrorCode::ReadNotPermitted => 0x2,
            ErrorCode::WriteNotPermitted => 0x3,
            ErrorCode::InvalidPdu => 0x4,
            ErrorCode::InsufficientAuthentication => 0x5,
            ErrorCode::RequestNotSupported => 0x6,
            ErrorCode::InvalidOffset => 0x7,
            ErrorCode::InsufficientAuthorization => 0x8,
            ErrorCode::PrepareQueueFull => 0x9,
            ErrorCode::AttributeNotFound => 0xa,
            ErrorCode::AttributeNotLong => 0xb,
            ErrorCode::EncryptionKeySizeTooShort => 0xc,
            ErrorCode::InvalidAttributeValueLength => 0xd,
            ErrorCode::UnlikelyError => 0xe,
            ErrorCode::InsufficientEncryption => 0xf,
            ErrorCode::UnsupportedGroupType => 0x10,
            ErrorCode::InsufficientResources => 0x11,
            ErrorCode::DatabaseOutOfSync => 0x12,
            ErrorCode::ValueNotAllowed => 0x13,
            ErrorCode::ApplicationError(Private(value)) => *value,
            ErrorCode::CommonProfileAndServiceErrorCodes(Private(value)) => *value,
        }
    }
}
impl From<ErrorCode> for u8 {
    fn from(value: ErrorCode) -> Self {
        (&value).into()
    }
}
impl From<ErrorCode> for i16 {
    fn from(value: ErrorCode) -> Self {
        u8::from(value) as Self
    }
}
impl From<ErrorCode> for i32 {
    fn from(value: ErrorCode) -> Self {
        u8::from(value) as Self
    }
}
impl From<ErrorCode> for i64 {
    fn from(value: ErrorCode) -> Self {
        u8::from(value) as Self
    }
}
impl From<ErrorCode> for u16 {
    fn from(value: ErrorCode) -> Self {
        u8::from(value) as Self
    }
}
impl From<ErrorCode> for u32 {
    fn from(value: ErrorCode) -> Self {
        u8::from(value) as Self
    }
}
impl From<ErrorCode> for u64 {
    fn from(value: ErrorCode) -> Self {
        u8::from(value) as Self
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ErrorRsp {
    pub request_opcode_in_error: Opcode,
    pub attribute_handle_in_error: AttributeHandle,
    pub error_code: ErrorCode,
}
impl TryFrom<&Pdu> for ErrorRsp {
    type Error = DecodeError;
    fn try_from(parent: &Pdu) -> Result<ErrorRsp, Self::Error> {
        ErrorRsp::decode_partial(&parent)
    }
}
impl TryFrom<Pdu> for ErrorRsp {
    type Error = DecodeError;
    fn try_from(parent: Pdu) -> Result<ErrorRsp, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&ErrorRsp> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &ErrorRsp) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttErrorRsp, payload })
    }
}
impl TryFrom<ErrorRsp> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: ErrorRsp) -> Result<Pdu, Self::Error> {
        (&packet).try_into()
    }
}
impl ErrorRsp {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if parent.opcode() != Opcode::AttErrorRsp {
            return Err(DecodeError::InvalidFieldValue {
                packet: "ErrorRsp",
                field: "opcode",
                expected: "Opcode::AttErrorRsp",
                actual: format!("{:?}", parent.opcode()),
            });
        }
        if buf.remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ErrorRsp",
                wanted: 1,
                got: buf.remaining(),
            });
        }
        let request_opcode_in_error = Opcode::try_from(buf.get_u8()).map_err(|unknown_val| {
            DecodeError::InvalidEnumValueError {
                obj: "ErrorRsp",
                field: "request_opcode_in_error",
                value: unknown_val as u64,
                type_: "Opcode",
            }
        })?;
        let (attribute_handle_in_error, mut buf) = AttributeHandle::decode(buf)?;
        if buf.remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ErrorRsp",
                wanted: 1,
                got: buf.remaining(),
            });
        }
        let error_code = ErrorCode::try_from(buf.get_u8()).map_err(|unknown_val| {
            DecodeError::InvalidEnumValueError {
                obj: "ErrorRsp",
                field: "error_code",
                value: unknown_val as u64,
                type_: "ErrorCode",
            }
        })?;
        if buf.is_empty() {
            Ok(Self { request_opcode_in_error, attribute_handle_in_error, error_code })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(u8::from(self.request_opcode_in_error()));
        self.attribute_handle_in_error.encode(buf)?;
        buf.put_u8(u8::from(self.error_code()));
        Ok(())
    }
    pub fn request_opcode_in_error(&self) -> Opcode {
        self.request_opcode_in_error
    }
    pub fn attribute_handle_in_error(&self) -> &AttributeHandle {
        &self.attribute_handle_in_error
    }
    pub fn error_code(&self) -> ErrorCode {
        self.error_code
    }
    pub fn opcode(&self) -> Opcode {
        Opcode::AttErrorRsp
    }
}
impl Packet for ErrorRsp {
    fn encoded_len(&self) -> usize {
        19
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(u8::from(self.opcode()));
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = Pdu::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ExchangeMtuReq {
    pub client_rx_mtu: u16,
}
impl TryFrom<&Pdu> for ExchangeMtuReq {
    type Error = DecodeError;
    fn try_from(parent: &Pdu) -> Result<ExchangeMtuReq, Self::Error> {
        ExchangeMtuReq::decode_partial(&parent)
    }
}
impl TryFrom<Pdu> for ExchangeMtuReq {
    type Error = DecodeError;
    fn try_from(parent: Pdu) -> Result<ExchangeMtuReq, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&ExchangeMtuReq> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &ExchangeMtuReq) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttExchangeMtuReq, payload })
    }
}
impl TryFrom<ExchangeMtuReq> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: ExchangeMtuReq) -> Result<Pdu, Self::Error> {
        (&packet).try_into()
    }
}
impl ExchangeMtuReq {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if parent.opcode() != Opcode::AttExchangeMtuReq {
            return Err(DecodeError::InvalidFieldValue {
                packet: "ExchangeMtuReq",
                field: "opcode",
                expected: "Opcode::AttExchangeMtuReq",
                actual: format!("{:?}", parent.opcode()),
            });
        }
        if buf.remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ExchangeMtuReq",
                wanted: 2,
                got: buf.remaining(),
            });
        }
        let client_rx_mtu = buf.get_u16_le();
        if buf.is_empty() {
            Ok(Self { client_rx_mtu })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u16_le(self.client_rx_mtu());
        Ok(())
    }
    pub fn client_rx_mtu(&self) -> u16 {
        self.client_rx_mtu
    }
    pub fn opcode(&self) -> Opcode {
        Opcode::AttExchangeMtuReq
    }
}
impl Packet for ExchangeMtuReq {
    fn encoded_len(&self) -> usize {
        3
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(u8::from(self.opcode()));
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = Pdu::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ExchangeMtuRsp {
    pub server_rx_mtu: u16,
}
impl TryFrom<&Pdu> for ExchangeMtuRsp {
    type Error = DecodeError;
    fn try_from(parent: &Pdu) -> Result<ExchangeMtuRsp, Self::Error> {
        ExchangeMtuRsp::decode_partial(&parent)
    }
}
impl TryFrom<Pdu> for ExchangeMtuRsp {
    type Error = DecodeError;
    fn try_from(parent: Pdu) -> Result<ExchangeMtuRsp, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&ExchangeMtuRsp> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &ExchangeMtuRsp) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttExchangeMtuRsp, payload })
    }
}
impl TryFrom<ExchangeMtuRsp> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: ExchangeMtuRsp) -> Result<Pdu, Self::Error> {
        (&packet).try_into()
    }
}
impl ExchangeMtuRsp {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if parent.opcode() != Opcode::AttExchangeMtuRsp {
            return Err(DecodeError::InvalidFieldValue {
                packet: "ExchangeMtuRsp",
                field: "opcode",
                expected: "Opcode::AttExchangeMtuRsp",
                actual: format!("{:?}", parent.opcode()),
            });
        }
        if buf.remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ExchangeMtuRsp",
                wanted: 2,
                got: buf.remaining(),
            });
        }
        let server_rx_mtu = buf.get_u16_le();
        if buf.is_empty() {
            Ok(Self { server_rx_mtu })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u16_le(self.server_rx_mtu());
        Ok(())
    }
    pub fn server_rx_mtu(&self) -> u16 {
        self.server_rx_mtu
    }
    pub fn opcode(&self) -> Opcode {
        Opcode::AttExchangeMtuRsp
    }
}
impl Packet for ExchangeMtuRsp {
    fn encoded_len(&self) -> usize {
        3
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(u8::from(self.opcode()));
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = Pdu::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FindInformationReq {
    pub starting_handle: AttributeHandle,
    pub ending_handle: AttributeHandle,
}
impl TryFrom<&Pdu> for FindInformationReq {
    type Error = DecodeError;
    fn try_from(parent: &Pdu) -> Result<FindInformationReq, Self::Error> {
        FindInformationReq::decode_partial(&parent)
    }
}
impl TryFrom<Pdu> for FindInformationReq {
    type Error = DecodeError;
    fn try_from(parent: Pdu) -> Result<FindInformationReq, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&FindInformationReq> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationReq) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttFindInformationReq, payload })
    }
}
impl TryFrom<FindInformationReq> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: FindInformationReq) -> Result<Pdu, Self::Error> {
        (&packet).try_into()
    }
}
impl FindInformationReq {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if parent.opcode() != Opcode::AttFindInformationReq {
            return Err(DecodeError::InvalidFieldValue {
                packet: "FindInformationReq",
                field: "opcode",
                expected: "Opcode::AttFindInformationReq",
                actual: format!("{:?}", parent.opcode()),
            });
        }
        let (starting_handle, mut buf) = AttributeHandle::decode(buf)?;
        let (ending_handle, mut buf) = AttributeHandle::decode(buf)?;
        if buf.is_empty() {
            Ok(Self { starting_handle, ending_handle })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.starting_handle.encode(buf)?;
        self.ending_handle.encode(buf)?;
        Ok(())
    }
    pub fn starting_handle(&self) -> &AttributeHandle {
        &self.starting_handle
    }
    pub fn ending_handle(&self) -> &AttributeHandle {
        &self.ending_handle
    }
    pub fn opcode(&self) -> Opcode {
        Opcode::AttFindInformationReq
    }
}
impl Packet for FindInformationReq {
    fn encoded_len(&self) -> usize {
        33
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(u8::from(self.opcode()));
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = Pdu::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FindInformationRsp {
    pub format: u8,
    pub payload: Vec<u8>,
}
impl TryFrom<&Pdu> for FindInformationRsp {
    type Error = DecodeError;
    fn try_from(parent: &Pdu) -> Result<FindInformationRsp, Self::Error> {
        FindInformationRsp::decode_partial(&parent)
    }
}
impl TryFrom<Pdu> for FindInformationRsp {
    type Error = DecodeError;
    fn try_from(parent: Pdu) -> Result<FindInformationRsp, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&FindInformationRsp> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationRsp) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttFindInformationRsp, payload })
    }
}
impl TryFrom<FindInformationRsp> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: FindInformationRsp) -> Result<Pdu, Self::Error> {
        (&packet).try_into()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub enum FindInformationRspChild {
    FindInformationRsp16(FindInformationRsp16),
    FindInformationRsp128(FindInformationRsp128),
    None,
}
impl FindInformationRsp {
    pub fn specialize(&self) -> Result<FindInformationRspChild, DecodeError> {
        Ok(match (self.format,) {
            (1,) => FindInformationRspChild::FindInformationRsp16(self.try_into()?),
            (2,) => FindInformationRspChild::FindInformationRsp128(self.try_into()?),
            _ => FindInformationRspChild::None,
        })
    }
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if parent.opcode() != Opcode::AttFindInformationRsp {
            return Err(DecodeError::InvalidFieldValue {
                packet: "FindInformationRsp",
                field: "opcode",
                expected: "Opcode::AttFindInformationRsp",
                actual: format!("{:?}", parent.opcode()),
            });
        }
        if buf.remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "FindInformationRsp",
                wanted: 1,
                got: buf.remaining(),
            });
        }
        let format = buf.get_u8();
        let payload = buf.to_vec();
        buf.advance(payload.len());
        if buf.is_empty() {
            Ok(Self { payload, format })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(self.format());
        buf.put_slice(&self.payload);
        Ok(())
    }
    pub fn payload(&self) -> &[u8] {
        &self.payload
    }
    pub fn format(&self) -> u8 {
        self.format
    }
    pub fn opcode(&self) -> Opcode {
        Opcode::AttFindInformationRsp
    }
}
impl Packet for FindInformationRsp {
    fn encoded_len(&self) -> usize {
        2 + self.payload.len()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(u8::from(self.opcode()));
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = Pdu::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FindInformationRsp16 {
    pub information_data: Vec<InformationData16>,
}
impl TryFrom<&FindInformationRsp> for FindInformationRsp16 {
    type Error = DecodeError;
    fn try_from(parent: &FindInformationRsp) -> Result<FindInformationRsp16, Self::Error> {
        FindInformationRsp16::decode_partial(&parent)
    }
}
impl TryFrom<FindInformationRsp> for FindInformationRsp16 {
    type Error = DecodeError;
    fn try_from(parent: FindInformationRsp) -> Result<FindInformationRsp16, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&FindInformationRsp16> for FindInformationRsp {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationRsp16) -> Result<FindInformationRsp, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(FindInformationRsp { format: 1, payload })
    }
}
impl TryFrom<FindInformationRsp16> for FindInformationRsp {
    type Error = EncodeError;
    fn try_from(packet: FindInformationRsp16) -> Result<FindInformationRsp, Self::Error> {
        (&packet).try_into()
    }
}
impl TryFrom<&FindInformationRsp16> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationRsp16) -> Result<Pdu, Self::Error> {
        (&FindInformationRsp::try_from(packet)?).try_into()
    }
}
impl TryFrom<FindInformationRsp16> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: FindInformationRsp16) -> Result<Pdu, Self::Error> {
        (&packet).try_into()
    }
}
impl FindInformationRsp16 {
    fn decode_partial(parent: &FindInformationRsp) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if parent.format() != 1 {
            return Err(DecodeError::InvalidFieldValue {
                packet: "FindInformationRsp16",
                field: "format",
                expected: "1",
                actual: format!("{:?}", parent.format()),
            });
        }
        if buf.remaining() % 4 != 0 {
            return Err(DecodeError::InvalidArraySize { array: buf.remaining(), element: 4 });
        }
        let information_data_count = buf.remaining() / 4;
        let mut information_data = Vec::with_capacity(information_data_count);
        for _ in 0..information_data_count {
            information_data.push(InformationData16::decode_mut(&mut buf)?);
        }
        if buf.is_empty() {
            Ok(Self { information_data })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        for elem in &self.information_data {
            elem.encode(buf)?;
        }
        Ok(())
    }
    pub fn information_data(&self) -> &Vec<InformationData16> {
        &self.information_data
    }
    pub fn format(&self) -> u8 {
        1
    }
    pub fn opcode(&self) -> Opcode {
        Opcode::AttFindInformationRsp
    }
}
impl Packet for FindInformationRsp16 {
    fn encoded_len(&self) -> usize {
        2 + self.information_data.len() * 4
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(u8::from(self.opcode()));
        buf.put_u8(self.format());
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = FindInformationRsp::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FindInformationRsp128 {
    pub information_data: Vec<InformationData128>,
}
impl TryFrom<&FindInformationRsp> for FindInformationRsp128 {
    type Error = DecodeError;
    fn try_from(parent: &FindInformationRsp) -> Result<FindInformationRsp128, Self::Error> {
        FindInformationRsp128::decode_partial(&parent)
    }
}
impl TryFrom<FindInformationRsp> for FindInformationRsp128 {
    type Error = DecodeError;
    fn try_from(parent: FindInformationRsp) -> Result<FindInformationRsp128, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&FindInformationRsp128> for FindInformationRsp {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationRsp128) -> Result<FindInformationRsp, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(FindInformationRsp { format: 2, payload })
    }
}
impl TryFrom<FindInformationRsp128> for FindInformationRsp {
    type Error = EncodeError;
    fn try_from(packet: FindInformationRsp128) -> Result<FindInformationRsp, Self::Error> {
        (&packet).try_into()
    }
}
impl TryFrom<&FindInformationRsp128> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationRsp128) -> Result<Pdu, Self::Error> {
        (&FindInformationRsp::try_from(packet)?).try_into()
    }
}
impl TryFrom<FindInformationRsp128> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: FindInformationRsp128) -> Result<Pdu, Self::Error> {
        (&packet).try_into()
    }
}
impl FindInformationRsp128 {
    fn decode_partial(parent: &FindInformationRsp) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if parent.format() != 2 {
            return Err(DecodeError::InvalidFieldValue {
                packet: "FindInformationRsp128",
                field: "format",
                expected: "2",
                actual: format!("{:?}", parent.format()),
            });
        }
        if buf.remaining() % 18 != 0 {
            return Err(DecodeError::InvalidArraySize { array: buf.remaining(), element: 18 });
        }
        let information_data_count = buf.remaining() / 18;
        let mut information_data = Vec::with_capacity(information_data_count);
        for _ in 0..information_data_count {
            information_data.push(InformationData128::decode_mut(&mut buf)?);
        }
        if buf.is_empty() {
            Ok(Self { information_data })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        for elem in &self.information_data {
            elem.encode(buf)?;
        }
        Ok(())
    }
    pub fn information_data(&self) -> &Vec<InformationData128> {
        &self.information_data
    }
    pub fn format(&self) -> u8 {
        2
    }
    pub fn opcode(&self) -> Opcode {
        Opcode::AttFindInformationRsp
    }
}
impl Packet for FindInformationRsp128 {
    fn encoded_len(&self) -> usize {
        2 + self.information_data.len() * 18
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(u8::from(self.opcode()));
        buf.put_u8(self.format());
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = FindInformationRsp::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct InformationData16 {
    pub attribute_handle: AttributeHandle,
    pub uuid: Uuid16,
}
impl InformationData16 {
    pub fn attribute_handle(&self) -> &AttributeHandle {
        &self.attribute_handle
    }
    pub fn uuid(&self) -> &Uuid16 {
        &self.uuid
    }
}
impl Packet for InformationData16 {
    fn encoded_len(&self) -> usize {
        32
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.attribute_handle.encode(buf)?;
        self.uuid.encode(buf)?;
        Ok(())
    }
    fn decode(mut buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (attribute_handle, mut buf) = AttributeHandle::decode(buf)?;
        let (uuid, mut buf) = Uuid16::decode(buf)?;
        Ok((Self { attribute_handle, uuid }, buf))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct InformationData128 {
    pub attribute_handle: AttributeHandle,
    pub uuid: Uuid128,
}
impl InformationData128 {
    pub fn attribute_handle(&self) -> &AttributeHandle {
        &self.attribute_handle
    }
    pub fn uuid(&self) -> &Uuid128 {
        &self.uuid
    }
}
impl Packet for InformationData128 {
    fn encoded_len(&self) -> usize {
        144
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.attribute_handle.encode(buf)?;
        self.uuid.encode(buf)?;
        Ok(())
    }
    fn decode(mut buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (attribute_handle, mut buf) = AttributeHandle::decode(buf)?;
        let (uuid, mut buf) = Uuid128::decode(buf)?;
        Ok((Self { attribute_handle, uuid }, buf))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FindByTypeValueReq {
    pub starting_handle: AttributeHandle,
    pub ending_handle: AttributeHandle,
    pub attribute_type: Uuid16,
    pub attribute_value: AttributeValue,
}
impl TryFrom<&Pdu> for FindByTypeValueReq {
    type Error = DecodeError;
    fn try_from(parent: &Pdu) -> Result<FindByTypeValueReq, Self::Error> {
        FindByTypeValueReq::decode_partial(&parent)
    }
}
impl TryFrom<Pdu> for FindByTypeValueReq {
    type Error = DecodeError;
    fn try_from(parent: Pdu) -> Result<FindByTypeValueReq, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&FindByTypeValueReq> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &FindByTypeValueReq) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttFindByTypeValueReq, payload })
    }
}
impl TryFrom<FindByTypeValueReq> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: FindByTypeValueReq) -> Result<Pdu, Self::Error> {
        (&packet).try_into()
    }
}
impl FindByTypeValueReq {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if parent.opcode() != Opcode::AttFindByTypeValueReq {
            return Err(DecodeError::InvalidFieldValue {
                packet: "FindByTypeValueReq",
                field: "opcode",
                expected: "Opcode::AttFindByTypeValueReq",
                actual: format!("{:?}", parent.opcode()),
            });
        }
        let (starting_handle, mut buf) = AttributeHandle::decode(buf)?;
        let (ending_handle, mut buf) = AttributeHandle::decode(buf)?;
        let (attribute_type, mut buf) = Uuid16::decode(buf)?;
        let (attribute_value, mut buf) = AttributeValue::decode(buf)?;
        if buf.is_empty() {
            Ok(Self { starting_handle, ending_handle, attribute_type, attribute_value })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.starting_handle.encode(buf)?;
        self.ending_handle.encode(buf)?;
        self.attribute_type.encode(buf)?;
        self.attribute_value.encode(buf)?;
        Ok(())
    }
    pub fn starting_handle(&self) -> &AttributeHandle {
        &self.starting_handle
    }
    pub fn ending_handle(&self) -> &AttributeHandle {
        &self.ending_handle
    }
    pub fn attribute_type(&self) -> &Uuid16 {
        &self.attribute_type
    }
    pub fn attribute_value(&self) -> &AttributeValue {
        &self.attribute_value
    }
    pub fn opcode(&self) -> Opcode {
        Opcode::AttFindByTypeValueReq
    }
}
impl Packet for FindByTypeValueReq {
    fn encoded_len(&self) -> usize {
        49
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(u8::from(self.opcode()));
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = Pdu::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FindByTypeValueRsp {
    pub handles_information_list: Vec<HandlesInformation>,
}
impl TryFrom<&Pdu> for FindByTypeValueRsp {
    type Error = DecodeError;
    fn try_from(parent: &Pdu) -> Result<FindByTypeValueRsp, Self::Error> {
        FindByTypeValueRsp::decode_partial(&parent)
    }
}
impl TryFrom<Pdu> for FindByTypeValueRsp {
    type Error = DecodeError;
    fn try_from(parent: Pdu) -> Result<FindByTypeValueRsp, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&FindByTypeValueRsp> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &FindByTypeValueRsp) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttFindByTypeValueRsp, payload })
    }
}
impl TryFrom<FindByTypeValueRsp> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: FindByTypeValueRsp) -> Result<Pdu, Self::Error> {
        (&packet).try_into()
    }
}
impl FindByTypeValueRsp {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if parent.opcode() != Opcode::AttFindByTypeValueRsp {
            return Err(DecodeError::InvalidFieldValue {
                packet: "FindByTypeValueRsp",
                field: "opcode",
                expected: "Opcode::AttFindByTypeValueRsp",
                actual: format!("{:?}", parent.opcode()),
            });
        }
        if buf.remaining() % 4 != 0 {
            return Err(DecodeError::InvalidArraySize { array: buf.remaining(), element: 4 });
        }
        let handles_information_list_count = buf.remaining() / 4;
        let mut handles_information_list = Vec::with_capacity(handles_information_list_count);
        for _ in 0..handles_information_list_count {
            handles_information_list.push(HandlesInformation::decode_mut(&mut buf)?);
        }
        if buf.is_empty() {
            Ok(Self { handles_information_list })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        for elem in &self.handles_information_list {
            elem.encode(buf)?;
        }
        Ok(())
    }
    pub fn handles_information_list(&self) -> &Vec<HandlesInformation> {
        &self.handles_information_list
    }
    pub fn opcode(&self) -> Opcode {
        Opcode::AttFindByTypeValueRsp
    }
}
impl Packet for FindByTypeValueRsp {
    fn encoded_len(&self) -> usize {
        1 + self.handles_information_list.len() * 4
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(u8::from(self.opcode()));
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = Pdu::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct HandlesInformation {
    pub found_attribute_handle: AttributeHandle,
    pub group_end_handle: AttributeHandle,
}
impl HandlesInformation {
    pub fn found_attribute_handle(&self) -> &AttributeHandle {
        &self.found_attribute_handle
    }
    pub fn group_end_handle(&self) -> &AttributeHandle {
        &self.group_end_handle
    }
}
impl Packet for HandlesInformation {
    fn encoded_len(&self) -> usize {
        32
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.found_attribute_handle.encode(buf)?;
        self.group_end_handle.encode(buf)?;
        Ok(())
    }
    fn decode(mut buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (found_attribute_handle, mut buf) = AttributeHandle::decode(buf)?;
        let (group_end_handle, mut buf) = AttributeHandle::decode(buf)?;
        Ok((Self { found_attribute_handle, group_end_handle }, buf))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ReadByTypeReq {
    pub starting_handle: AttributeHandle,
    pub ending_handle: AttributeHandle,
    pub attribute_type: Uuid,
}
impl TryFrom<&Pdu> for ReadByTypeReq {
    type Error = DecodeError;
    fn try_from(parent: &Pdu) -> Result<ReadByTypeReq, Self::Error> {
        ReadByTypeReq::decode_partial(&parent)
    }
}
impl TryFrom<Pdu> for ReadByTypeReq {
    type Error = DecodeError;
    fn try_from(parent: Pdu) -> Result<ReadByTypeReq, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&ReadByTypeReq> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &ReadByTypeReq) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttReadByTypeReq, payload })
    }
}
impl TryFrom<ReadByTypeReq> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: ReadByTypeReq) -> Result<Pdu, Self::Error> {
        (&packet).try_into()
    }
}
impl ReadByTypeReq {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if parent.opcode() != Opcode::AttReadByTypeReq {
            return Err(DecodeError::InvalidFieldValue {
                packet: "ReadByTypeReq",
                field: "opcode",
                expected: "Opcode::AttReadByTypeReq",
                actual: format!("{:?}", parent.opcode()),
            });
        }
        let (starting_handle, mut buf) = AttributeHandle::decode(buf)?;
        let (ending_handle, mut buf) = AttributeHandle::decode(buf)?;
        let (attribute_type, buf) = Uuid::decode(buf)?;
        if buf.is_empty() {
            Ok(Self { starting_handle, ending_handle, attribute_type })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.starting_handle.encode(buf)?;
        self.ending_handle.encode(buf)?;
        self.attribute_type.encode(buf)?;
        Ok(())
    }
    pub fn starting_handle(&self) -> &AttributeHandle {
        &self.starting_handle
    }
    pub fn ending_handle(&self) -> &AttributeHandle {
        &self.ending_handle
    }
    pub fn attribute_type(&self) -> &Uuid {
        &self.attribute_type
    }
    pub fn opcode(&self) -> Opcode {
        Opcode::AttReadByTypeReq
    }
}
impl Packet for ReadByTypeReq {
    fn encoded_len(&self) -> usize {
        33 + self.attribute_type.encoded_len()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(u8::from(self.opcode()));
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = Pdu::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ReadByTypeRsp {
    pub attribute_data_list: Vec<AttributeData>,
}
impl TryFrom<&Pdu> for ReadByTypeRsp {
    type Error = DecodeError;
    fn try_from(parent: &Pdu) -> Result<ReadByTypeRsp, Self::Error> {
        ReadByTypeRsp::decode_partial(&parent)
    }
}
impl TryFrom<Pdu> for ReadByTypeRsp {
    type Error = DecodeError;
    fn try_from(parent: Pdu) -> Result<ReadByTypeRsp, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&ReadByTypeRsp> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &ReadByTypeRsp) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttReadByTypeRsp, payload })
    }
}
impl TryFrom<ReadByTypeRsp> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: ReadByTypeRsp) -> Result<Pdu, Self::Error> {
        (&packet).try_into()
    }
}
impl ReadByTypeRsp {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if parent.opcode() != Opcode::AttReadByTypeRsp {
            return Err(DecodeError::InvalidFieldValue {
                packet: "ReadByTypeRsp",
                field: "opcode",
                expected: "Opcode::AttReadByTypeRsp",
                actual: format!("{:?}", parent.opcode()),
            });
        }
        if buf.remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ReadByTypeRsp",
                wanted: 1,
                got: buf.remaining(),
            });
        }
        let attribute_data_list_element_size = buf.get_u8() as usize;
        if buf.remaining() % attribute_data_list_element_size != 0 {
            return Err(DecodeError::InvalidArraySize {
                array: buf.remaining(),
                element: attribute_data_list_element_size,
            });
        }
        let attribute_data_list = buf
            .chunks(attribute_data_list_element_size)
            .take(buf.remaining() / attribute_data_list_element_size)
            .map(|mut chunk| {
                AttributeData::decode_mut(&mut chunk).and_then(|value| {
                    if chunk.is_empty() {
                        Ok(value)
                    } else {
                        Err(DecodeError::TrailingBytesInArray {
                            obj: "ReadByTypeRsp",
                            field: "attribute_data_list",
                        })
                    }
                })
            })
            .collect::<Result<Vec<_>, DecodeError>>()?;
        buf = &buf[buf.remaining()..];
        if buf.is_empty() {
            Ok(Self { attribute_data_list })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        let attribute_data_list_element_size =
            self.attribute_data_list.get(0).map_or(0, Packet::encoded_len);
        for (element_index, element) in self.attribute_data_list.iter().enumerate() {
            if element.encoded_len() != attribute_data_list_element_size {
                return Err(EncodeError::InvalidArrayElementSize {
                    packet: "ReadByTypeRsp",
                    field: "attribute_data_list",
                    size: element.encoded_len(),
                    expected_size: attribute_data_list_element_size,
                    element_index,
                });
            }
        }
        if attribute_data_list_element_size > 0xff {
            return Err(EncodeError::SizeOverflow {
                packet: "ReadByTypeRsp",
                field: "attribute_data_list",
                size: attribute_data_list_element_size,
                maximum_size: 0xff,
            });
        }
        let attribute_data_list_element_size = attribute_data_list_element_size as u8;
        buf.put_u8(attribute_data_list_element_size);
        for elem in &self.attribute_data_list {
            elem.encode(buf)?;
        }
        Ok(())
    }
    pub fn attribute_data_list(&self) -> &Vec<AttributeData> {
        &self.attribute_data_list
    }
    pub fn opcode(&self) -> Opcode {
        Opcode::AttReadByTypeRsp
    }
}
impl Packet for ReadByTypeRsp {
    fn encoded_len(&self) -> usize {
        2 + self.attribute_data_list.iter().map(Packet::encoded_len).sum::<usize>()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(u8::from(self.opcode()));
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = Pdu::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ReadReq {
    pub attribute_handle: AttributeHandle,
}
impl TryFrom<&Pdu> for ReadReq {
    type Error = DecodeError;
    fn try_from(parent: &Pdu) -> Result<ReadReq, Self::Error> {
        ReadReq::decode_partial(&parent)
    }
}
impl TryFrom<Pdu> for ReadReq {
    type Error = DecodeError;
    fn try_from(parent: Pdu) -> Result<ReadReq, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&ReadReq> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &ReadReq) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttReadReq, payload })
    }
}
impl TryFrom<ReadReq> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: ReadReq) -> Result<Pdu, Self::Error> {
        (&packet).try_into()
    }
}
impl ReadReq {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if parent.opcode() != Opcode::AttReadReq {
            return Err(DecodeError::InvalidFieldValue {
                packet: "ReadReq",
                field: "opcode",
                expected: "Opcode::AttReadReq",
                actual: format!("{:?}", parent.opcode()),
            });
        }
        let (attribute_handle, mut buf) = AttributeHandle::decode(buf)?;
        if buf.is_empty() {
            Ok(Self { attribute_handle })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.attribute_handle.encode(buf)?;
        Ok(())
    }
    pub fn attribute_handle(&self) -> &AttributeHandle {
        &self.attribute_handle
    }
    pub fn opcode(&self) -> Opcode {
        Opcode::AttReadReq
    }
}
impl Packet for ReadReq {
    fn encoded_len(&self) -> usize {
        17
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(u8::from(self.opcode()));
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = Pdu::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ReadRsp {
    pub attribute_value: AttributeValue,
}
impl TryFrom<&Pdu> for ReadRsp {
    type Error = DecodeError;
    fn try_from(parent: &Pdu) -> Result<ReadRsp, Self::Error> {
        ReadRsp::decode_partial(&parent)
    }
}
impl TryFrom<Pdu> for ReadRsp {
    type Error = DecodeError;
    fn try_from(parent: Pdu) -> Result<ReadRsp, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&ReadRsp> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &ReadRsp) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttReadRsp, payload })
    }
}
impl TryFrom<ReadRsp> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: ReadRsp) -> Result<Pdu, Self::Error> {
        (&packet).try_into()
    }
}
impl ReadRsp {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if parent.opcode() != Opcode::AttReadRsp {
            return Err(DecodeError::InvalidFieldValue {
                packet: "ReadRsp",
                field: "opcode",
                expected: "Opcode::AttReadRsp",
                actual: format!("{:?}", parent.opcode()),
            });
        }
        let (attribute_value, mut buf) = AttributeValue::decode(buf)?;
        if buf.is_empty() {
            Ok(Self { attribute_value })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.attribute_value.encode(buf)?;
        Ok(())
    }
    pub fn attribute_value(&self) -> &AttributeValue {
        &self.attribute_value
    }
    pub fn opcode(&self) -> Opcode {
        Opcode::AttReadRsp
    }
}
impl Packet for ReadRsp {
    fn encoded_len(&self) -> usize {
        1
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(u8::from(self.opcode()));
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = Pdu::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct AttributeData {
    pub attribute_handle: AttributeHandle,
    pub attribute_value: AttributeValue,
}
impl AttributeData {
    pub fn attribute_handle(&self) -> &AttributeHandle {
        &self.attribute_handle
    }
    pub fn attribute_value(&self) -> &AttributeValue {
        &self.attribute_value
    }
}
impl Packet for AttributeData {
    fn encoded_len(&self) -> usize {
        16
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.attribute_handle.encode(buf)?;
        self.attribute_value.encode(buf)?;
        Ok(())
    }
    fn decode(mut buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (attribute_handle, mut buf) = AttributeHandle::decode(buf)?;
        let (attribute_value, mut buf) = AttributeValue::decode(buf)?;
        Ok((Self { attribute_handle, attribute_value }, buf))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ReadByGroupTypeReq {
    pub starting_handle: AttributeHandle,
    pub ending_handle: AttributeHandle,
    pub attribute_group_type: Uuid,
}
impl TryFrom<&Pdu> for ReadByGroupTypeReq {
    type Error = DecodeError;
    fn try_from(parent: &Pdu) -> Result<ReadByGroupTypeReq, Self::Error> {
        ReadByGroupTypeReq::decode_partial(&parent)
    }
}
impl TryFrom<Pdu> for ReadByGroupTypeReq {
    type Error = DecodeError;
    fn try_from(parent: Pdu) -> Result<ReadByGroupTypeReq, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&ReadByGroupTypeReq> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &ReadByGroupTypeReq) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttReadByGroupTypeReq, payload })
    }
}
impl TryFrom<ReadByGroupTypeReq> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: ReadByGroupTypeReq) -> Result<Pdu, Self::Error> {
        (&packet).try_into()
    }
}
impl ReadByGroupTypeReq {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if parent.opcode() != Opcode::AttReadByGroupTypeReq {
            return Err(DecodeError::InvalidFieldValue {
                packet: "ReadByGroupTypeReq",
                field: "opcode",
                expected: "Opcode::AttReadByGroupTypeReq",
                actual: format!("{:?}", parent.opcode()),
            });
        }
        let (starting_handle, mut buf) = AttributeHandle::decode(buf)?;
        let (ending_handle, mut buf) = AttributeHandle::decode(buf)?;
        let (attribute_group_type, buf) = Uuid::decode(buf)?;
        if buf.is_empty() {
            Ok(Self { starting_handle, ending_handle, attribute_group_type })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.starting_handle.encode(buf)?;
        self.ending_handle.encode(buf)?;
        self.attribute_group_type.encode(buf)?;
        Ok(())
    }
    pub fn starting_handle(&self) -> &AttributeHandle {
        &self.starting_handle
    }
    pub fn ending_handle(&self) -> &AttributeHandle {
        &self.ending_handle
    }
    pub fn attribute_group_type(&self) -> &Uuid {
        &self.attribute_group_type
    }
    pub fn opcode(&self) -> Opcode {
        Opcode::AttReadByGroupTypeReq
    }
}
impl Packet for ReadByGroupTypeReq {
    fn encoded_len(&self) -> usize {
        33 + self.attribute_group_type.encoded_len()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(u8::from(self.opcode()));
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = Pdu::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ReadByGroupTypeRsp {
    pub attribute_data_list: Vec<GroupAttributeData>,
}
impl TryFrom<&Pdu> for ReadByGroupTypeRsp {
    type Error = DecodeError;
    fn try_from(parent: &Pdu) -> Result<ReadByGroupTypeRsp, Self::Error> {
        ReadByGroupTypeRsp::decode_partial(&parent)
    }
}
impl TryFrom<Pdu> for ReadByGroupTypeRsp {
    type Error = DecodeError;
    fn try_from(parent: Pdu) -> Result<ReadByGroupTypeRsp, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&ReadByGroupTypeRsp> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &ReadByGroupTypeRsp) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttReadByGroupTypeRsp, payload })
    }
}
impl TryFrom<ReadByGroupTypeRsp> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: ReadByGroupTypeRsp) -> Result<Pdu, Self::Error> {
        (&packet).try_into()
    }
}
impl ReadByGroupTypeRsp {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if parent.opcode() != Opcode::AttReadByGroupTypeRsp {
            return Err(DecodeError::InvalidFieldValue {
                packet: "ReadByGroupTypeRsp",
                field: "opcode",
                expected: "Opcode::AttReadByGroupTypeRsp",
                actual: format!("{:?}", parent.opcode()),
            });
        }
        if buf.remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ReadByGroupTypeRsp",
                wanted: 1,
                got: buf.remaining(),
            });
        }
        let attribute_data_list_element_size = buf.get_u8() as usize;
        if buf.remaining() % attribute_data_list_element_size != 0 {
            return Err(DecodeError::InvalidArraySize {
                array: buf.remaining(),
                element: attribute_data_list_element_size,
            });
        }
        let attribute_data_list = buf
            .chunks(attribute_data_list_element_size)
            .take(buf.remaining() / attribute_data_list_element_size)
            .map(|mut chunk| {
                GroupAttributeData::decode_mut(&mut chunk).and_then(|value| {
                    if chunk.is_empty() {
                        Ok(value)
                    } else {
                        Err(DecodeError::TrailingBytesInArray {
                            obj: "ReadByGroupTypeRsp",
                            field: "attribute_data_list",
                        })
                    }
                })
            })
            .collect::<Result<Vec<_>, DecodeError>>()?;
        buf = &buf[buf.remaining()..];
        if buf.is_empty() {
            Ok(Self { attribute_data_list })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        let attribute_data_list_element_size =
            self.attribute_data_list.get(0).map_or(0, Packet::encoded_len);
        for (element_index, element) in self.attribute_data_list.iter().enumerate() {
            if element.encoded_len() != attribute_data_list_element_size {
                return Err(EncodeError::InvalidArrayElementSize {
                    packet: "ReadByGroupTypeRsp",
                    field: "attribute_data_list",
                    size: element.encoded_len(),
                    expected_size: attribute_data_list_element_size,
                    element_index,
                });
            }
        }
        if attribute_data_list_element_size > 0xff {
            return Err(EncodeError::SizeOverflow {
                packet: "ReadByGroupTypeRsp",
                field: "attribute_data_list",
                size: attribute_data_list_element_size,
                maximum_size: 0xff,
            });
        }
        let attribute_data_list_element_size = attribute_data_list_element_size as u8;
        buf.put_u8(attribute_data_list_element_size);
        for elem in &self.attribute_data_list {
            elem.encode(buf)?;
        }
        Ok(())
    }
    pub fn attribute_data_list(&self) -> &Vec<GroupAttributeData> {
        &self.attribute_data_list
    }
    pub fn opcode(&self) -> Opcode {
        Opcode::AttReadByGroupTypeRsp
    }
}
impl Packet for ReadByGroupTypeRsp {
    fn encoded_len(&self) -> usize {
        2 + self.attribute_data_list.iter().map(Packet::encoded_len).sum::<usize>()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(u8::from(self.opcode()));
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = Pdu::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct GroupAttributeData {
    pub attribute_handle: AttributeHandle,
    pub end_group_handle: AttributeHandle,
    pub attribute_value: AttributeValue,
}
impl GroupAttributeData {
    pub fn attribute_handle(&self) -> &AttributeHandle {
        &self.attribute_handle
    }
    pub fn end_group_handle(&self) -> &AttributeHandle {
        &self.end_group_handle
    }
    pub fn attribute_value(&self) -> &AttributeValue {
        &self.attribute_value
    }
}
impl Packet for GroupAttributeData {
    fn encoded_len(&self) -> usize {
        32
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.attribute_handle.encode(buf)?;
        self.end_group_handle.encode(buf)?;
        self.attribute_value.encode(buf)?;
        Ok(())
    }
    fn decode(mut buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (attribute_handle, mut buf) = AttributeHandle::decode(buf)?;
        let (end_group_handle, mut buf) = AttributeHandle::decode(buf)?;
        let (attribute_value, mut buf) = AttributeValue::decode(buf)?;
        Ok((Self { attribute_handle, end_group_handle, attribute_value }, buf))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ServiceAttributeValueUuid {
    pub uuid: Uuid,
}
impl TryFrom<&AttributeValue> for ServiceAttributeValueUuid {
    type Error = DecodeError;
    fn try_from(parent: &AttributeValue) -> Result<ServiceAttributeValueUuid, Self::Error> {
        ServiceAttributeValueUuid::decode_partial(&parent)
    }
}
impl TryFrom<AttributeValue> for ServiceAttributeValueUuid {
    type Error = DecodeError;
    fn try_from(parent: AttributeValue) -> Result<ServiceAttributeValueUuid, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&ServiceAttributeValueUuid> for AttributeValue {
    type Error = EncodeError;
    fn try_from(packet: &ServiceAttributeValueUuid) -> Result<AttributeValue, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(AttributeValue { payload })
    }
}
impl TryFrom<ServiceAttributeValueUuid> for AttributeValue {
    type Error = EncodeError;
    fn try_from(packet: ServiceAttributeValueUuid) -> Result<AttributeValue, Self::Error> {
        (&packet).try_into()
    }
}
impl ServiceAttributeValueUuid {
    fn decode_partial(parent: &AttributeValue) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        let (uuid, buf) = Uuid::decode(buf)?;
        if buf.is_empty() {
            Ok(Self { uuid })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.uuid.encode(buf)?;
        Ok(())
    }
    pub fn uuid(&self) -> &Uuid {
        &self.uuid
    }
}
impl Packet for ServiceAttributeValueUuid {
    fn encoded_len(&self) -> usize {
        self.uuid.encoded_len()
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = AttributeValue::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ServiceAttributeValueUuid16 {
    pub uuid: Uuid16,
}
impl TryFrom<&AttributeValue> for ServiceAttributeValueUuid16 {
    type Error = DecodeError;
    fn try_from(parent: &AttributeValue) -> Result<ServiceAttributeValueUuid16, Self::Error> {
        ServiceAttributeValueUuid16::decode_partial(&parent)
    }
}
impl TryFrom<AttributeValue> for ServiceAttributeValueUuid16 {
    type Error = DecodeError;
    fn try_from(parent: AttributeValue) -> Result<ServiceAttributeValueUuid16, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&ServiceAttributeValueUuid16> for AttributeValue {
    type Error = EncodeError;
    fn try_from(packet: &ServiceAttributeValueUuid16) -> Result<AttributeValue, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(AttributeValue { payload })
    }
}
impl TryFrom<ServiceAttributeValueUuid16> for AttributeValue {
    type Error = EncodeError;
    fn try_from(packet: ServiceAttributeValueUuid16) -> Result<AttributeValue, Self::Error> {
        (&packet).try_into()
    }
}
impl ServiceAttributeValueUuid16 {
    fn decode_partial(parent: &AttributeValue) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        let (uuid, mut buf) = Uuid16::decode(buf)?;
        if buf.is_empty() {
            Ok(Self { uuid })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.uuid.encode(buf)?;
        Ok(())
    }
    pub fn uuid(&self) -> &Uuid16 {
        &self.uuid
    }
}
impl Packet for ServiceAttributeValueUuid16 {
    fn encoded_len(&self) -> usize {
        16
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = AttributeValue::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ServiceAttributeValueUuid128 {
    pub uuid: Uuid128,
}
impl TryFrom<&AttributeValue> for ServiceAttributeValueUuid128 {
    type Error = DecodeError;
    fn try_from(parent: &AttributeValue) -> Result<ServiceAttributeValueUuid128, Self::Error> {
        ServiceAttributeValueUuid128::decode_partial(&parent)
    }
}
impl TryFrom<AttributeValue> for ServiceAttributeValueUuid128 {
    type Error = DecodeError;
    fn try_from(parent: AttributeValue) -> Result<ServiceAttributeValueUuid128, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&ServiceAttributeValueUuid128> for AttributeValue {
    type Error = EncodeError;
    fn try_from(packet: &ServiceAttributeValueUuid128) -> Result<AttributeValue, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(AttributeValue { payload })
    }
}
impl TryFrom<ServiceAttributeValueUuid128> for AttributeValue {
    type Error = EncodeError;
    fn try_from(packet: ServiceAttributeValueUuid128) -> Result<AttributeValue, Self::Error> {
        (&packet).try_into()
    }
}
impl ServiceAttributeValueUuid128 {
    fn decode_partial(parent: &AttributeValue) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        let (uuid, mut buf) = Uuid128::decode(buf)?;
        if buf.is_empty() {
            Ok(Self { uuid })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.uuid.encode(buf)?;
        Ok(())
    }
    pub fn uuid(&self) -> &Uuid128 {
        &self.uuid
    }
}
impl Packet for ServiceAttributeValueUuid128 {
    fn encoded_len(&self) -> usize {
        128
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = AttributeValue::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct IncludeAttributeValueUuid16 {
    pub attribute_handle: AttributeHandle,
    pub end_group_handle: AttributeHandle,
    pub uuid: Uuid16,
}
impl TryFrom<&AttributeValue> for IncludeAttributeValueUuid16 {
    type Error = DecodeError;
    fn try_from(parent: &AttributeValue) -> Result<IncludeAttributeValueUuid16, Self::Error> {
        IncludeAttributeValueUuid16::decode_partial(&parent)
    }
}
impl TryFrom<AttributeValue> for IncludeAttributeValueUuid16 {
    type Error = DecodeError;
    fn try_from(parent: AttributeValue) -> Result<IncludeAttributeValueUuid16, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&IncludeAttributeValueUuid16> for AttributeValue {
    type Error = EncodeError;
    fn try_from(packet: &IncludeAttributeValueUuid16) -> Result<AttributeValue, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(AttributeValue { payload })
    }
}
impl TryFrom<IncludeAttributeValueUuid16> for AttributeValue {
    type Error = EncodeError;
    fn try_from(packet: IncludeAttributeValueUuid16) -> Result<AttributeValue, Self::Error> {
        (&packet).try_into()
    }
}
impl IncludeAttributeValueUuid16 {
    fn decode_partial(parent: &AttributeValue) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        let (attribute_handle, mut buf) = AttributeHandle::decode(buf)?;
        let (end_group_handle, mut buf) = AttributeHandle::decode(buf)?;
        let (uuid, mut buf) = Uuid16::decode(buf)?;
        if buf.is_empty() {
            Ok(Self { attribute_handle, end_group_handle, uuid })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.attribute_handle.encode(buf)?;
        self.end_group_handle.encode(buf)?;
        self.uuid.encode(buf)?;
        Ok(())
    }
    pub fn attribute_handle(&self) -> &AttributeHandle {
        &self.attribute_handle
    }
    pub fn end_group_handle(&self) -> &AttributeHandle {
        &self.end_group_handle
    }
    pub fn uuid(&self) -> &Uuid16 {
        &self.uuid
    }
}
impl Packet for IncludeAttributeValueUuid16 {
    fn encoded_len(&self) -> usize {
        48
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = AttributeValue::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct IncludeAttributeValueUuid128 {
    pub attribute_handle: AttributeHandle,
    pub end_group_handle: AttributeHandle,
}
impl TryFrom<&AttributeValue> for IncludeAttributeValueUuid128 {
    type Error = DecodeError;
    fn try_from(parent: &AttributeValue) -> Result<IncludeAttributeValueUuid128, Self::Error> {
        IncludeAttributeValueUuid128::decode_partial(&parent)
    }
}
impl TryFrom<AttributeValue> for IncludeAttributeValueUuid128 {
    type Error = DecodeError;
    fn try_from(parent: AttributeValue) -> Result<IncludeAttributeValueUuid128, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&IncludeAttributeValueUuid128> for AttributeValue {
    type Error = EncodeError;
    fn try_from(packet: &IncludeAttributeValueUuid128) -> Result<AttributeValue, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(AttributeValue { payload })
    }
}
impl TryFrom<IncludeAttributeValueUuid128> for AttributeValue {
    type Error = EncodeError;
    fn try_from(packet: IncludeAttributeValueUuid128) -> Result<AttributeValue, Self::Error> {
        (&packet).try_into()
    }
}
impl IncludeAttributeValueUuid128 {
    fn decode_partial(parent: &AttributeValue) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        let (attribute_handle, mut buf) = AttributeHandle::decode(buf)?;
        let (end_group_handle, mut buf) = AttributeHandle::decode(buf)?;
        if buf.is_empty() {
            Ok(Self { attribute_handle, end_group_handle })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.attribute_handle.encode(buf)?;
        self.end_group_handle.encode(buf)?;
        Ok(())
    }
    pub fn attribute_handle(&self) -> &AttributeHandle {
        &self.attribute_handle
    }
    pub fn end_group_handle(&self) -> &AttributeHandle {
        &self.end_group_handle
    }
}
impl Packet for IncludeAttributeValueUuid128 {
    fn encoded_len(&self) -> usize {
        32
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = AttributeValue::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CharacteristicAttributeValueUuid16 {
    pub properties: u8,
    pub value_attribute_handle: AttributeHandle,
    pub uuid: Uuid16,
}
impl TryFrom<&AttributeValue> for CharacteristicAttributeValueUuid16 {
    type Error = DecodeError;
    fn try_from(
        parent: &AttributeValue,
    ) -> Result<CharacteristicAttributeValueUuid16, Self::Error> {
        CharacteristicAttributeValueUuid16::decode_partial(&parent)
    }
}
impl TryFrom<AttributeValue> for CharacteristicAttributeValueUuid16 {
    type Error = DecodeError;
    fn try_from(parent: AttributeValue) -> Result<CharacteristicAttributeValueUuid16, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&CharacteristicAttributeValueUuid16> for AttributeValue {
    type Error = EncodeError;
    fn try_from(
        packet: &CharacteristicAttributeValueUuid16,
    ) -> Result<AttributeValue, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(AttributeValue { payload })
    }
}
impl TryFrom<CharacteristicAttributeValueUuid16> for AttributeValue {
    type Error = EncodeError;
    fn try_from(packet: CharacteristicAttributeValueUuid16) -> Result<AttributeValue, Self::Error> {
        (&packet).try_into()
    }
}
impl CharacteristicAttributeValueUuid16 {
    fn decode_partial(parent: &AttributeValue) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if buf.remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CharacteristicAttributeValueUuid16",
                wanted: 1,
                got: buf.remaining(),
            });
        }
        let properties = buf.get_u8();
        let (value_attribute_handle, mut buf) = AttributeHandle::decode(buf)?;
        let (uuid, mut buf) = Uuid16::decode(buf)?;
        if buf.is_empty() {
            Ok(Self { properties, value_attribute_handle, uuid })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(self.properties());
        self.value_attribute_handle.encode(buf)?;
        self.uuid.encode(buf)?;
        Ok(())
    }
    pub fn properties(&self) -> u8 {
        self.properties
    }
    pub fn value_attribute_handle(&self) -> &AttributeHandle {
        &self.value_attribute_handle
    }
    pub fn uuid(&self) -> &Uuid16 {
        &self.uuid
    }
}
impl Packet for CharacteristicAttributeValueUuid16 {
    fn encoded_len(&self) -> usize {
        33
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = AttributeValue::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct CharacteristicAttributeValueUuid128 {
    pub properties: u8,
    pub value_attribute_handle: AttributeHandle,
    pub uuid: Uuid128,
}
impl TryFrom<&AttributeValue> for CharacteristicAttributeValueUuid128 {
    type Error = DecodeError;
    fn try_from(
        parent: &AttributeValue,
    ) -> Result<CharacteristicAttributeValueUuid128, Self::Error> {
        CharacteristicAttributeValueUuid128::decode_partial(&parent)
    }
}
impl TryFrom<AttributeValue> for CharacteristicAttributeValueUuid128 {
    type Error = DecodeError;
    fn try_from(
        parent: AttributeValue,
    ) -> Result<CharacteristicAttributeValueUuid128, Self::Error> {
        (&parent).try_into()
    }
}
impl TryFrom<&CharacteristicAttributeValueUuid128> for AttributeValue {
    type Error = EncodeError;
    fn try_from(
        packet: &CharacteristicAttributeValueUuid128,
    ) -> Result<AttributeValue, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(AttributeValue { payload })
    }
}
impl TryFrom<CharacteristicAttributeValueUuid128> for AttributeValue {
    type Error = EncodeError;
    fn try_from(
        packet: CharacteristicAttributeValueUuid128,
    ) -> Result<AttributeValue, Self::Error> {
        (&packet).try_into()
    }
}
impl CharacteristicAttributeValueUuid128 {
    fn decode_partial(parent: &AttributeValue) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if buf.remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "CharacteristicAttributeValueUuid128",
                wanted: 1,
                got: buf.remaining(),
            });
        }
        let properties = buf.get_u8();
        let (value_attribute_handle, mut buf) = AttributeHandle::decode(buf)?;
        let (uuid, mut buf) = Uuid128::decode(buf)?;
        if buf.is_empty() {
            Ok(Self { properties, value_attribute_handle, uuid })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        buf.put_u8(self.properties());
        self.value_attribute_handle.encode(buf)?;
        self.uuid.encode(buf)?;
        Ok(())
    }
    pub fn properties(&self) -> u8 {
        self.properties
    }
    pub fn value_attribute_handle(&self) -> &AttributeHandle {
        &self.value_attribute_handle
    }
    pub fn uuid(&self) -> &Uuid128 {
        &self.uuid
    }
}
impl Packet for CharacteristicAttributeValueUuid128 {
    fn encoded_len(&self) -> usize {
        145
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = AttributeValue::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
