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
#[derive(Debug, Clone, PartialEq, Eq, Default)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct AttributeValue {
    pub payload: Vec<u8>,
}
impl TryFrom<&AttributeValue> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &AttributeValue) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&AttributeValue> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &AttributeValue) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub enum AttributeValueChild {
    ServiceAttributeValueUuid16(ServiceAttributeValueUuid16),
    ServiceAttributeValueUuid128(ServiceAttributeValueUuid128),
    None,
}
impl AttributeValue {
    pub fn specialize(&self) -> Result<AttributeValueChild, DecodeError> {
        Ok(match () {
            () => AttributeValueChild::ServiceAttributeValueUuid16(self.try_into()?),
            () => AttributeValueChild::ServiceAttributeValueUuid128(self.try_into()?),
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
    pub payload: Vec<u8>,
}
impl TryFrom<&Uuid> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &Uuid) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&Uuid> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &Uuid) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub enum UuidChild {
    Uuid16(Uuid16),
    Uuid128(Uuid128),
    None,
}
impl Uuid {
    pub fn specialize(&self) -> Result<UuidChild, DecodeError> {
        Ok(match () {
            () => UuidChild::Uuid16(self.try_into()?),
            () => UuidChild::Uuid128(self.try_into()?),
            _ => UuidChild::None,
        })
    }
    pub fn payload(&self) -> &[u8] {
        &self.payload
    }
}
impl Packet for Uuid {
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
pub struct Uuid16 {
    pub value: [u8; 2],
}
impl TryFrom<&Uuid> for Uuid16 {
    type Error = DecodeError;
    fn try_from(parent: &Uuid) -> Result<Uuid16, Self::Error> {
        Uuid16::decode_partial(&parent)
    }
}
impl TryFrom<&Uuid16> for Uuid {
    type Error = EncodeError;
    fn try_from(packet: &Uuid16) -> Result<Uuid, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Uuid { payload })
    }
}
impl TryFrom<&Uuid16> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &Uuid16) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&Uuid16> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &Uuid16) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl Uuid16 {
    fn decode_partial(parent: &Uuid) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
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
        if buf.is_empty() {
            Ok(Self { value })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        for elem in &self.value {
            buf.put_u8(*elem);
        }
        Ok(())
    }
    pub fn value(&self) -> &[u8; 2] {
        &self.value
    }
}
impl Packet for Uuid16 {
    fn encoded_len(&self) -> usize {
        self.value.len() * 1
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = Uuid::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct Uuid128 {
    pub value: [u8; 16],
}
impl TryFrom<&Uuid> for Uuid128 {
    type Error = DecodeError;
    fn try_from(parent: &Uuid) -> Result<Uuid128, Self::Error> {
        Uuid128::decode_partial(&parent)
    }
}
impl TryFrom<&Uuid128> for Uuid {
    type Error = EncodeError;
    fn try_from(packet: &Uuid128) -> Result<Uuid, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Uuid { payload })
    }
}
impl TryFrom<&Uuid128> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &Uuid128) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&Uuid128> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &Uuid128) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl Uuid128 {
    fn decode_partial(parent: &Uuid) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
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
        if buf.is_empty() {
            Ok(Self { value })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        for elem in &self.value {
            buf.put_u8(*elem);
        }
        Ok(())
    }
    pub fn value(&self) -> &[u8; 16] {
        &self.value
    }
}
impl Packet for Uuid128 {
    fn encoded_len(&self) -> usize {
        self.value.len() * 1
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.encode_partial(buf)?;
        Ok(())
    }
    fn decode(buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (parent, trailing_bytes) = Uuid::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
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
impl TryFrom<&Pdu> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &Pdu) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&Pdu> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &Pdu) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub enum PduChild {
    ErrorRsp(ErrorRsp),
    ExchangeMtuReq(ExchangeMtuReq),
    ExchangeMtuRsp(ExchangeMtuRsp),
    FindInformationReq(FindInformationReq),
    FindInformationRes(FindInformationRes),
    FindByTypeValueReq(FindByTypeValueReq),
    FindByTypeValueRsp(FindByTypeValueRsp),
    ReadByTypeReq(ReadByTypeReq),
    ReadByTypeRsp(ReadByTypeRsp),
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
            (Opcode::AttFindInformationRsp,) => PduChild::FindInformationRes(self.try_into()?),
            (Opcode::AttFindByTypeValueReq,) => PduChild::FindByTypeValueReq(self.try_into()?),
            (Opcode::AttFindByTypeValueRsp,) => PduChild::FindByTypeValueRsp(self.try_into()?),
            (Opcode::AttReadByTypeReq,) => PduChild::ReadByTypeReq(self.try_into()?),
            (Opcode::AttReadByTypeRsp,) => PduChild::ReadByTypeRsp(self.try_into()?),
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
impl TryFrom<&ErrorRsp> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &ErrorRsp) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttErrorRsp, payload })
    }
}
impl TryFrom<&ErrorRsp> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &ErrorRsp) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&ErrorRsp> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &ErrorRsp) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl ErrorRsp {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
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
        let (head, tail) = buf.split_at(2);
        buf = tail;
        let attribute_handle_in_error = AttributeHandle::decode_full(head)?;
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
impl TryFrom<&ExchangeMtuReq> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &ExchangeMtuReq) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttExchangeMtuReq, payload })
    }
}
impl TryFrom<&ExchangeMtuReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &ExchangeMtuReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&ExchangeMtuReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &ExchangeMtuReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl ExchangeMtuReq {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
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
impl TryFrom<&ExchangeMtuRsp> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &ExchangeMtuRsp) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttExchangeMtuRsp, payload })
    }
}
impl TryFrom<&ExchangeMtuRsp> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &ExchangeMtuRsp) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&ExchangeMtuRsp> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &ExchangeMtuRsp) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl ExchangeMtuRsp {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
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
impl TryFrom<&FindInformationReq> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationReq) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttFindInformationReq, payload })
    }
}
impl TryFrom<&FindInformationReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&FindInformationReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl FindInformationReq {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        let (head, tail) = buf.split_at(2);
        buf = tail;
        let starting_handle = AttributeHandle::decode_full(head)?;
        let (head, tail) = buf.split_at(2);
        buf = tail;
        let ending_handle = AttributeHandle::decode_full(head)?;
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
pub struct FindInformationRes {
    pub format: u8,
    pub payload: Vec<u8>,
}
impl TryFrom<&Pdu> for FindInformationRes {
    type Error = DecodeError;
    fn try_from(parent: &Pdu) -> Result<FindInformationRes, Self::Error> {
        FindInformationRes::decode_partial(&parent)
    }
}
impl TryFrom<&FindInformationRes> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationRes) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttFindInformationRsp, payload })
    }
}
impl TryFrom<&FindInformationRes> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationRes) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&FindInformationRes> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationRes) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub enum FindInformationResChild {
    FindInformationRes16(FindInformationRes16),
    FindInformationRes128(FindInformationRes128),
    None,
}
impl FindInformationRes {
    pub fn specialize(&self) -> Result<FindInformationResChild, DecodeError> {
        Ok(match (self.format,) {
            (1,) => FindInformationResChild::FindInformationRes16(self.try_into()?),
            (2,) => FindInformationResChild::FindInformationRes128(self.try_into()?),
            _ => FindInformationResChild::None,
        })
    }
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if buf.remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "FindInformationRes",
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
impl Packet for FindInformationRes {
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
pub struct FindInformationRes16 {
    pub information_data: Vec<InformationData16>,
}
impl TryFrom<&FindInformationRes> for FindInformationRes16 {
    type Error = DecodeError;
    fn try_from(parent: &FindInformationRes) -> Result<FindInformationRes16, Self::Error> {
        FindInformationRes16::decode_partial(&parent)
    }
}
impl TryFrom<&FindInformationRes16> for FindInformationRes {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationRes16) -> Result<FindInformationRes, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(FindInformationRes { format: 1, payload })
    }
}
impl TryFrom<&FindInformationRes16> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationRes16) -> Result<Pdu, Self::Error> {
        (&FindInformationRes::try_from(packet)?).try_into()
    }
}
impl TryFrom<&FindInformationRes16> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationRes16) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&FindInformationRes16> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationRes16) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl FindInformationRes16 {
    fn decode_partial(parent: &FindInformationRes) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
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
impl Packet for FindInformationRes16 {
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
        let (parent, trailing_bytes) = FindInformationRes::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FindInformationRes128 {
    pub information_data: Vec<InformationData128>,
}
impl TryFrom<&FindInformationRes> for FindInformationRes128 {
    type Error = DecodeError;
    fn try_from(parent: &FindInformationRes) -> Result<FindInformationRes128, Self::Error> {
        FindInformationRes128::decode_partial(&parent)
    }
}
impl TryFrom<&FindInformationRes128> for FindInformationRes {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationRes128) -> Result<FindInformationRes, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(FindInformationRes { format: 2, payload })
    }
}
impl TryFrom<&FindInformationRes128> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationRes128) -> Result<Pdu, Self::Error> {
        (&FindInformationRes::try_from(packet)?).try_into()
    }
}
impl TryFrom<&FindInformationRes128> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationRes128) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&FindInformationRes128> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &FindInformationRes128) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl FindInformationRes128 {
    fn decode_partial(parent: &FindInformationRes) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
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
impl Packet for FindInformationRes128 {
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
        let (parent, trailing_bytes) = FindInformationRes::decode(buf)?;
        let packet = Self::decode_partial(&parent)?;
        Ok((packet, trailing_bytes))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct InformationData16 {
    pub attribute_handle: AttributeHandle,
    pub uuid: [u8; 2],
}
impl TryFrom<&InformationData16> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &InformationData16) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&InformationData16> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &InformationData16) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl InformationData16 {
    pub fn attribute_handle(&self) -> &AttributeHandle {
        &self.attribute_handle
    }
    pub fn uuid(&self) -> &[u8; 2] {
        &self.uuid
    }
}
impl Packet for InformationData16 {
    fn encoded_len(&self) -> usize {
        16 + self.uuid.len() * 1
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.attribute_handle.encode(buf)?;
        for elem in &self.uuid {
            buf.put_u8(*elem);
        }
        Ok(())
    }
    fn decode(mut buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (head, tail) = buf.split_at(2);
        buf = tail;
        let attribute_handle = AttributeHandle::decode_full(head)?;
        if buf.remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "InformationData16",
                wanted: 2,
                got: buf.remaining(),
            });
        }
        let mut uuid = Vec::with_capacity(2);
        for _ in 0..2 {
            uuid.push(Ok::<_, DecodeError>(buf.get_u8())?)
        }
        let uuid = uuid.try_into().map_err(|_| DecodeError::InvalidPacketError)?;
        Ok((Self { attribute_handle, uuid }, buf))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct InformationData128 {
    pub attribute_handle: AttributeHandle,
    pub uuid: [u8; 16],
}
impl TryFrom<&InformationData128> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &InformationData128) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&InformationData128> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &InformationData128) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl InformationData128 {
    pub fn attribute_handle(&self) -> &AttributeHandle {
        &self.attribute_handle
    }
    pub fn uuid(&self) -> &[u8; 16] {
        &self.uuid
    }
}
impl Packet for InformationData128 {
    fn encoded_len(&self) -> usize {
        16 + self.uuid.len() * 1
    }
    fn encode(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.attribute_handle.encode(buf)?;
        for elem in &self.uuid {
            buf.put_u8(*elem);
        }
        Ok(())
    }
    fn decode(mut buf: &[u8]) -> Result<(Self, &[u8]), DecodeError> {
        let (head, tail) = buf.split_at(2);
        buf = tail;
        let attribute_handle = AttributeHandle::decode_full(head)?;
        if buf.remaining() < 16 {
            return Err(DecodeError::InvalidLengthError {
                obj: "InformationData128",
                wanted: 16,
                got: buf.remaining(),
            });
        }
        let mut uuid = Vec::with_capacity(16);
        for _ in 0..16 {
            uuid.push(Ok::<_, DecodeError>(buf.get_u8())?)
        }
        let uuid = uuid.try_into().map_err(|_| DecodeError::InvalidPacketError)?;
        Ok((Self { attribute_handle, uuid }, buf))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct FindByTypeValueReq {
    pub starting_handle: AttributeHandle,
    pub ending_handle: AttributeHandle,
    pub attribute_type: [u8; 2],
    pub attribute_value: Vec<u8>,
}
impl TryFrom<&Pdu> for FindByTypeValueReq {
    type Error = DecodeError;
    fn try_from(parent: &Pdu) -> Result<FindByTypeValueReq, Self::Error> {
        FindByTypeValueReq::decode_partial(&parent)
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
impl TryFrom<&FindByTypeValueReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &FindByTypeValueReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&FindByTypeValueReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &FindByTypeValueReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl FindByTypeValueReq {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        let (head, tail) = buf.split_at(2);
        buf = tail;
        let starting_handle = AttributeHandle::decode_full(head)?;
        let (head, tail) = buf.split_at(2);
        buf = tail;
        let ending_handle = AttributeHandle::decode_full(head)?;
        if buf.remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "FindByTypeValueReq",
                wanted: 2,
                got: buf.remaining(),
            });
        }
        let mut attribute_type = Vec::with_capacity(2);
        for _ in 0..2 {
            attribute_type.push(Ok::<_, DecodeError>(buf.get_u8())?)
        }
        let attribute_type =
            attribute_type.try_into().map_err(|_| DecodeError::InvalidPacketError)?;
        let mut attribute_value = Vec::with_capacity(buf.remaining());
        for _ in 0..buf.remaining() {
            attribute_value.push(Ok::<_, DecodeError>(buf.get_u8())?);
        }
        if buf.is_empty() {
            Ok(Self { starting_handle, ending_handle, attribute_type, attribute_value })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        self.starting_handle.encode(buf)?;
        self.ending_handle.encode(buf)?;
        for elem in &self.attribute_type {
            buf.put_u8(*elem);
        }
        for elem in &self.attribute_value {
            buf.put_u8(*elem);
        }
        Ok(())
    }
    pub fn starting_handle(&self) -> &AttributeHandle {
        &self.starting_handle
    }
    pub fn ending_handle(&self) -> &AttributeHandle {
        &self.ending_handle
    }
    pub fn attribute_type(&self) -> &[u8; 2] {
        &self.attribute_type
    }
    pub fn attribute_value(&self) -> &Vec<u8> {
        &self.attribute_value
    }
    pub fn opcode(&self) -> Opcode {
        Opcode::AttFindByTypeValueReq
    }
}
impl Packet for FindByTypeValueReq {
    fn encoded_len(&self) -> usize {
        33 + self.attribute_type.len() * 1 + self.attribute_value.len() * 1
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
impl TryFrom<&FindByTypeValueRsp> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &FindByTypeValueRsp) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttFindByTypeValueRsp, payload })
    }
}
impl TryFrom<&FindByTypeValueRsp> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &FindByTypeValueRsp) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&FindByTypeValueRsp> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &FindByTypeValueRsp) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl FindByTypeValueRsp {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
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
impl TryFrom<&HandlesInformation> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &HandlesInformation) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&HandlesInformation> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &HandlesInformation) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
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
        let (head, tail) = buf.split_at(2);
        buf = tail;
        let found_attribute_handle = AttributeHandle::decode_full(head)?;
        let (head, tail) = buf.split_at(2);
        buf = tail;
        let group_end_handle = AttributeHandle::decode_full(head)?;
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
impl TryFrom<&ReadByTypeReq> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &ReadByTypeReq) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttReadByTypeReq, payload })
    }
}
impl TryFrom<&ReadByTypeReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &ReadByTypeReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&ReadByTypeReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &ReadByTypeReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl ReadByTypeReq {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        let (head, tail) = buf.split_at(2);
        buf = tail;
        let starting_handle = AttributeHandle::decode_full(head)?;
        let (head, tail) = buf.split_at(2);
        buf = tail;
        let ending_handle = AttributeHandle::decode_full(head)?;
        let (head, tail) = buf.split_at(0);
        buf = tail;
        let attribute_type = Uuid::decode_full(head)?;
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
pub struct ReadByTypeRsp {
    pub attribute_data_list: Vec<AttributeData>,
}
impl TryFrom<&Pdu> for ReadByTypeRsp {
    type Error = DecodeError;
    fn try_from(parent: &Pdu) -> Result<ReadByTypeRsp, Self::Error> {
        ReadByTypeRsp::decode_partial(&parent)
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
impl TryFrom<&ReadByTypeRsp> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &ReadByTypeRsp) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&ReadByTypeRsp> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &ReadByTypeRsp) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl ReadByTypeRsp {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if buf.remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ReadByTypeRsp",
                wanted: 1,
                got: buf.remaining(),
            });
        }
        let attribute_data_list_element_size = buf.get_u8() as usize;
        let mut attribute_data_list = Vec::new();
        while !buf.is_empty() {
            let mut element_span = &buf[..attribute_data_list_element_size];
            attribute_data_list.push(AttributeData::decode_mut(&mut element_span)?);
            if !element_span.is_empty() {
                todo!("Error");
            }
            buf = &buf[attribute_data_list_element_size..];
        }
        if buf.is_empty() {
            Ok(Self { attribute_data_list })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        let attribute_data_list_element_size =
            self.attribute_data_list.get(0).map_or(0, |field| field.encoded_len() as u8);
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
pub struct AttributeData {
    pub attribute_handle: AttributeHandle,
    pub attribute_value: AttributeValue,
}
impl TryFrom<&AttributeData> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &AttributeData) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&AttributeData> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &AttributeData) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
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
        let (head, tail) = buf.split_at(2);
        buf = tail;
        let attribute_handle = AttributeHandle::decode_full(head)?;
        let (head, tail) = buf.split_at(0);
        buf = tail;
        let attribute_value = AttributeValue::decode_full(head)?;
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
impl TryFrom<&ReadByGroupTypeReq> for Pdu {
    type Error = EncodeError;
    fn try_from(packet: &ReadByGroupTypeReq) -> Result<Pdu, Self::Error> {
        let mut payload = Vec::new();
        packet.encode_partial(&mut payload)?;
        Ok(Pdu { opcode: Opcode::AttReadByGroupTypeReq, payload })
    }
}
impl TryFrom<&ReadByGroupTypeReq> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &ReadByGroupTypeReq) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&ReadByGroupTypeReq> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &ReadByGroupTypeReq) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl ReadByGroupTypeReq {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        let (head, tail) = buf.split_at(2);
        buf = tail;
        let starting_handle = AttributeHandle::decode_full(head)?;
        let (head, tail) = buf.split_at(2);
        buf = tail;
        let ending_handle = AttributeHandle::decode_full(head)?;
        let (head, tail) = buf.split_at(0);
        buf = tail;
        let attribute_group_type = Uuid::decode_full(head)?;
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
pub struct ReadByGroupTypeRsp {
    pub attribute_data_list: Vec<GroupAttributeData>,
}
impl TryFrom<&Pdu> for ReadByGroupTypeRsp {
    type Error = DecodeError;
    fn try_from(parent: &Pdu) -> Result<ReadByGroupTypeRsp, Self::Error> {
        ReadByGroupTypeRsp::decode_partial(&parent)
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
impl TryFrom<&ReadByGroupTypeRsp> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &ReadByGroupTypeRsp) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&ReadByGroupTypeRsp> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &ReadByGroupTypeRsp) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl ReadByGroupTypeRsp {
    fn decode_partial(parent: &Pdu) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if buf.remaining() < 1 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ReadByGroupTypeRsp",
                wanted: 1,
                got: buf.remaining(),
            });
        }
        let attribute_data_list_element_size = buf.get_u8() as usize;
        let mut attribute_data_list = Vec::new();
        while !buf.is_empty() {
            let mut element_span = &buf[..attribute_data_list_element_size];
            attribute_data_list.push(GroupAttributeData::decode_mut(&mut element_span)?);
            if !element_span.is_empty() {
                todo!("Error");
            }
            buf = &buf[attribute_data_list_element_size..];
        }
        if buf.is_empty() {
            Ok(Self { attribute_data_list })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        let attribute_data_list_element_size =
            self.attribute_data_list.get(0).map_or(0, |field| field.encoded_len() as u8);
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
impl TryFrom<&GroupAttributeData> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &GroupAttributeData) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&GroupAttributeData> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &GroupAttributeData) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
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
        let (head, tail) = buf.split_at(2);
        buf = tail;
        let attribute_handle = AttributeHandle::decode_full(head)?;
        let (head, tail) = buf.split_at(2);
        buf = tail;
        let end_group_handle = AttributeHandle::decode_full(head)?;
        let (head, tail) = buf.split_at(0);
        buf = tail;
        let attribute_value = AttributeValue::decode_full(head)?;
        Ok((Self { attribute_handle, end_group_handle, attribute_value }, buf))
    }
}
#[derive(Debug, Clone, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct ServiceAttributeValueUuid16 {
    pub uuid: [u8; 2],
}
impl TryFrom<&AttributeValue> for ServiceAttributeValueUuid16 {
    type Error = DecodeError;
    fn try_from(parent: &AttributeValue) -> Result<ServiceAttributeValueUuid16, Self::Error> {
        ServiceAttributeValueUuid16::decode_partial(&parent)
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
impl TryFrom<&ServiceAttributeValueUuid16> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &ServiceAttributeValueUuid16) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&ServiceAttributeValueUuid16> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &ServiceAttributeValueUuid16) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl ServiceAttributeValueUuid16 {
    fn decode_partial(parent: &AttributeValue) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if buf.remaining() < 2 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ServiceAttributeValueUuid16",
                wanted: 2,
                got: buf.remaining(),
            });
        }
        let mut uuid = Vec::with_capacity(2);
        for _ in 0..2 {
            uuid.push(Ok::<_, DecodeError>(buf.get_u8())?)
        }
        let uuid = uuid.try_into().map_err(|_| DecodeError::InvalidPacketError)?;
        if buf.is_empty() {
            Ok(Self { uuid })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        for elem in &self.uuid {
            buf.put_u8(*elem);
        }
        Ok(())
    }
    pub fn uuid(&self) -> &[u8; 2] {
        &self.uuid
    }
}
impl Packet for ServiceAttributeValueUuid16 {
    fn encoded_len(&self) -> usize {
        self.uuid.len() * 1
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
    pub uuid: [u8; 16],
}
impl TryFrom<&AttributeValue> for ServiceAttributeValueUuid128 {
    type Error = DecodeError;
    fn try_from(parent: &AttributeValue) -> Result<ServiceAttributeValueUuid128, Self::Error> {
        ServiceAttributeValueUuid128::decode_partial(&parent)
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
impl TryFrom<&ServiceAttributeValueUuid128> for Bytes {
    type Error = EncodeError;
    fn try_from(packet: &ServiceAttributeValueUuid128) -> Result<Self, Self::Error> {
        packet.encode_to_bytes()
    }
}
impl TryFrom<&ServiceAttributeValueUuid128> for Vec<u8> {
    type Error = EncodeError;
    fn try_from(packet: &ServiceAttributeValueUuid128) -> Result<Self, Self::Error> {
        packet.encode_to_vec()
    }
}
impl ServiceAttributeValueUuid128 {
    fn decode_partial(parent: &AttributeValue) -> Result<Self, DecodeError> {
        let mut buf: &[u8] = &parent.payload;
        if buf.remaining() < 16 {
            return Err(DecodeError::InvalidLengthError {
                obj: "ServiceAttributeValueUuid128",
                wanted: 16,
                got: buf.remaining(),
            });
        }
        let mut uuid = Vec::with_capacity(16);
        for _ in 0..16 {
            uuid.push(Ok::<_, DecodeError>(buf.get_u8())?)
        }
        let uuid = uuid.try_into().map_err(|_| DecodeError::InvalidPacketError)?;
        if buf.is_empty() {
            Ok(Self { uuid })
        } else {
            Err(DecodeError::TrailingBytes)
        }
    }
    pub fn encode_partial(&self, buf: &mut impl BufMut) -> Result<(), EncodeError> {
        for elem in &self.uuid {
            buf.put_u8(*elem);
        }
        Ok(())
    }
    pub fn uuid(&self) -> &[u8; 16] {
        &self.uuid
    }
}
impl Packet for ServiceAttributeValueUuid128 {
    fn encoded_len(&self) -> usize {
        self.uuid.len() * 1
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
