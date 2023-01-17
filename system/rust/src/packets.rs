// We inherit casing from the PDL file
#![allow(non_snake_case)]
#![allow(non_camel_case_types)]
#![allow(warnings, missing_docs)]
#![allow(clippy::all)]
// this is now stable
#![feature(mixed_integer_ops)]

use log::{debug, info};
use std::convert::TryFrom;
use std::convert::TryInto;
use std::ops::Deref;

#[derive(Debug)]
pub enum ParseError {
    InvalidEnumValue,
    DivisionFailure,
    ArithmeticOverflow,
    OutOfBoundsAccess,
    MisalignedPayload,
}

#[derive(Clone, Copy, Debug)]
pub struct BitSlice<'a> {
    // note: the offsets are ENTIRELY UNRELATED to the size of this struct,
    // so indexing needs to be checked to avoid panics
    backing: &'a [u8],

    // invariant: end_bit_offset >= start_bit_offset, so subtraction will NEVER wrap
    start_bit_offset: usize,
    end_bit_offset: usize,
}

#[derive(Clone, Copy, Debug)]
pub struct SizedBitSlice<'a>(BitSlice<'a>);

impl<'a> BitSlice<'a> {
    pub fn offset(&self, offset: usize) -> Result<BitSlice<'a>, ParseError> {
        if self.end_bit_offset - self.start_bit_offset < offset {
            return Err(ParseError::OutOfBoundsAccess);
        }
        Ok(Self {
            backing: self.backing,
            start_bit_offset: self
                .start_bit_offset
                .checked_add(offset)
                .ok_or(ParseError::ArithmeticOverflow)?,
            end_bit_offset: self.end_bit_offset,
        })
    }

    pub fn slice(&self, len: usize) -> Result<SizedBitSlice<'a>, ParseError> {
        if self.end_bit_offset - self.start_bit_offset < len {
            return Err(ParseError::OutOfBoundsAccess);
        }
        Ok(SizedBitSlice(Self {
            backing: self.backing,
            start_bit_offset: self.start_bit_offset,
            end_bit_offset: self
                .start_bit_offset
                .checked_add(len)
                .ok_or(ParseError::ArithmeticOverflow)?,
        }))
    }

    fn byte_at(&self, index: usize) -> Result<u8, ParseError> {
        self.backing.get(index).ok_or(ParseError::OutOfBoundsAccess).copied()
    }
}

impl<'a> Deref for SizedBitSlice<'a> {
    type Target = BitSlice<'a>;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl<'a> From<SizedBitSlice<'a>> for BitSlice<'a> {
    fn from(x: SizedBitSlice<'a>) -> Self {
        *x
    }
}

impl<'a, 'b> From<&'b [u8]> for SizedBitSlice<'a>
where
    'b: 'a,
{
    fn from(backing: &'a [u8]) -> Self {
        Self(BitSlice { backing, start_bit_offset: 0, end_bit_offset: backing.len() * 8 })
    }
}

impl<'a> SizedBitSlice<'a> {
    pub fn try_parse(&self) -> Result<u64, ParseError> {
        if self.end_bit_offset < self.start_bit_offset {
            return Err(ParseError::OutOfBoundsAccess);
        }
        let size_in_bits = self.end_bit_offset - self.start_bit_offset;

        // fields that fit into a u64 don't need to be byte-aligned
        if size_in_bits <= 64 {
            let mut accumulator = 0u64;

            // where we are in our accumulation
            let mut curr_byte_index = self.start_bit_offset / 8;
            let mut curr_bit_offset = self.start_bit_offset % 8;
            let mut remaining_bits = size_in_bits;

            while remaining_bits > 0 {
                // how many bits to take from the current byte?
                // check if this is the last byte
                if curr_bit_offset + remaining_bits <= 8 {
                    let tmp = ((self.byte_at(curr_byte_index)? >> curr_bit_offset) as u64)
                        & ((1u64 << remaining_bits) - 1);
                    accumulator += tmp << (size_in_bits - remaining_bits);
                    break;
                } else {
                    // this is not the last byte, so we have 8 - curr_bit_offset bits to
                    // consume in this byte
                    let bits_to_consume = 8 - curr_bit_offset;
                    let tmp = (self.byte_at(curr_byte_index)? >> curr_bit_offset) as u64;
                    accumulator += tmp << (size_in_bits - remaining_bits);
                    curr_bit_offset = 0;
                    curr_byte_index += 1;
                    remaining_bits -= bits_to_consume as usize;
                }
            }
            Ok(accumulator)
        } else {
            return Err(ParseError::MisalignedPayload);
        }
    }

    pub fn get_size_in_bits(&self) -> usize {
        self.end_bit_offset - self.start_bit_offset
    }
}

pub trait Packet<'a>
where
    Self: Sized,
{
    type Parent;
    type Owned;
    type Builder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError>;
    fn try_parse(parent: Self::Parent) -> Result<Self, ParseError>;
    fn to_owned_packet(&self) -> Self::Owned;
}

pub trait OwnedPacket
where
    Self: Sized,
{
    // Enable GAT when 1.65 is available in AOSP
    // type View<'a> where Self : 'a;
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError>;
    // fn view<'a>(&'a self) -> Self::View<'a>;
}

pub trait Builder: Serializable {
    type OwnedPacket: OwnedPacket;
}

#[derive(Debug)]
pub enum SerializeError {
    NegativePadding,
    IntegerConversionFailure,
    ValueTooLarge,
    AlignmentError,
}

trait BitWriter {
    fn write_bits(
        &mut self,
        num_bits: usize,
        gen_contents: impl FnOnce() -> Result<u64, SerializeError>,
    ) -> Result<(), SerializeError>;
}

pub trait Serializable {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError>;

    fn size_in_bits(&self) -> Result<usize, SerializeError> {
        let mut sizer = Sizer::new();
        self.serialize(&mut sizer)?;
        Ok(sizer.size())
    }

    fn write(&self, vec: &mut Vec<u8>) -> Result<(), SerializeError> {
        let mut serializer = Serializer::new(vec);
        self.serialize(&mut serializer)?;
        serializer.flush();
        Ok(())
    }

    fn to_vec(&self) -> Result<Vec<u8>, SerializeError> {
        let mut out = vec![];
        self.write(&mut out)?;
        Ok(out)
    }
}

struct Sizer {
    size: usize,
}

impl Sizer {
    fn new() -> Self {
        Self { size: 0 }
    }

    fn size(self) -> usize {
        self.size
    }
}

impl BitWriter for Sizer {
    fn write_bits(
        &mut self,
        num_bits: usize,
        gen_contents: impl FnOnce() -> Result<u64, SerializeError>,
    ) -> Result<(), SerializeError> {
        self.size += num_bits;
        Ok(())
    }
}

struct Serializer<'a> {
    buf: &'a mut Vec<u8>,
    curr_byte: u8,
    curr_bit_offset: u8,
}

impl<'a> Serializer<'a> {
    fn new(buf: &'a mut Vec<u8>) -> Self {
        Self { buf, curr_byte: 0, curr_bit_offset: 0 }
    }

    fn flush(self) {
        if self.curr_bit_offset > 0 {
            // partial byte remaining
            self.buf.push(self.curr_byte << (8 - self.curr_bit_offset));
        }
    }
}

impl<'a> BitWriter for Serializer<'a> {
    fn write_bits(
        &mut self,
        num_bits: usize,
        gen_contents: impl FnOnce() -> Result<u64, SerializeError>,
    ) -> Result<(), SerializeError> {
        let val = gen_contents()?;

        if num_bits < 64 && val >= 1 << num_bits {
            return Err(SerializeError::ValueTooLarge);
        }

        let mut remaining_val = val;
        let mut remaining_bits = num_bits;
        while remaining_bits > 0 {
            let remaining_bits_in_curr_byte = (8 - self.curr_bit_offset) as usize;
            if remaining_bits < remaining_bits_in_curr_byte {
                // we cannot finish the last byte
                self.curr_byte += (remaining_val as u8) << self.curr_bit_offset;
                self.curr_bit_offset += remaining_bits as u8;
                break;
            } else {
                // finish up our current byte and move on
                let val_for_this_byte =
                    (remaining_val & ((1 << remaining_bits_in_curr_byte) - 1)) as u8;
                let curr_byte = self.curr_byte + (val_for_this_byte << self.curr_bit_offset);
                self.buf.push(curr_byte);

                // clear pending byte
                self.curr_bit_offset = 0;
                self.curr_byte = 0;

                // update what's remaining
                remaining_val >>= remaining_bits_in_curr_byte;
                remaining_bits -= remaining_bits_in_curr_byte;
            }
        }

        Ok(())
    }
}
#[derive(Copy, Clone, PartialEq, Eq, Debug)]
pub enum AttOpcode {
    ERROR_RESPONSE,
    EXCHANGE_MTU_REQUEST,
    EXCHANGE_MTU_RESPONSE,
    FIND_INFORMATION_REQUEST,
    FIND_INFORMATION_RESPONSE,
    FIND_BY_TYPE_VALUE_REQUEST,
    FIND_BY_TYPE_VALUE_RESPONSE,
    READ_BY_TYPE_REQUEST,
    READ_BY_TYPE_RESPONSE,
    READ_REQUEST,
    READ_RESPONSE,
    READ_BLOB_REQUEST,
    READ_BLOB_RESPONSE,
    READ_MULTIPLE_REQUEST,
    READ_MULTIPLE_RESPONSE,
    READ_BY_GROUP_TYPE_REQUEST,
    READ_BY_GROUP_TYPE_RESPONSE,
    WRITE_REQUEST,
    WRITE_RESPONSE,
}
impl AttOpcode {
    fn try_parse(buf: BitSlice) -> Result<Self, ParseError> {
        let value = buf.slice(8usize)?.try_parse()?;
        match value {
            1u64 => Ok(Self::ERROR_RESPONSE),
            2u64 => Ok(Self::EXCHANGE_MTU_REQUEST),
            3u64 => Ok(Self::EXCHANGE_MTU_RESPONSE),
            4u64 => Ok(Self::FIND_INFORMATION_REQUEST),
            5u64 => Ok(Self::FIND_INFORMATION_RESPONSE),
            6u64 => Ok(Self::FIND_BY_TYPE_VALUE_REQUEST),
            7u64 => Ok(Self::FIND_BY_TYPE_VALUE_RESPONSE),
            8u64 => Ok(Self::READ_BY_TYPE_REQUEST),
            9u64 => Ok(Self::READ_BY_TYPE_RESPONSE),
            10u64 => Ok(Self::READ_REQUEST),
            11u64 => Ok(Self::READ_RESPONSE),
            12u64 => Ok(Self::READ_BLOB_REQUEST),
            13u64 => Ok(Self::READ_BLOB_RESPONSE),
            14u64 => Ok(Self::READ_MULTIPLE_REQUEST),
            15u64 => Ok(Self::READ_MULTIPLE_RESPONSE),
            16u64 => Ok(Self::READ_BY_GROUP_TYPE_REQUEST),
            17u64 => Ok(Self::READ_BY_GROUP_TYPE_RESPONSE),
            18u64 => Ok(Self::WRITE_REQUEST),
            19u64 => Ok(Self::WRITE_RESPONSE),
            _ => Err(ParseError::InvalidEnumValue),
        }
    }
    fn value(&self) -> u64 {
        match self {
            Self::ERROR_RESPONSE => 1u64,
            Self::EXCHANGE_MTU_REQUEST => 2u64,
            Self::EXCHANGE_MTU_RESPONSE => 3u64,
            Self::FIND_INFORMATION_REQUEST => 4u64,
            Self::FIND_INFORMATION_RESPONSE => 5u64,
            Self::FIND_BY_TYPE_VALUE_REQUEST => 6u64,
            Self::FIND_BY_TYPE_VALUE_RESPONSE => 7u64,
            Self::READ_BY_TYPE_REQUEST => 8u64,
            Self::READ_BY_TYPE_RESPONSE => 9u64,
            Self::READ_REQUEST => 10u64,
            Self::READ_RESPONSE => 11u64,
            Self::READ_BLOB_REQUEST => 12u64,
            Self::READ_BLOB_RESPONSE => 13u64,
            Self::READ_MULTIPLE_REQUEST => 14u64,
            Self::READ_MULTIPLE_RESPONSE => 15u64,
            Self::READ_BY_GROUP_TYPE_REQUEST => 16u64,
            Self::READ_BY_GROUP_TYPE_RESPONSE => 17u64,
            Self::WRITE_REQUEST => 18u64,
            Self::WRITE_RESPONSE => 19u64,
        }
    }
}
impl Serializable for AttOpcode {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(8usize, || Ok(self.value()));
        Ok(())
    }
}
impl From<AttOpcode> for u64 {
    fn from(x: AttOpcode) -> u64 {
        x.value()
    }
}
impl TryFrom<u64> for AttOpcode {
    type Error = ParseError;
    fn try_from(value: u64) -> Result<Self, ParseError> {
        match value {
            1u64 => Ok(Self::ERROR_RESPONSE),
            2u64 => Ok(Self::EXCHANGE_MTU_REQUEST),
            3u64 => Ok(Self::EXCHANGE_MTU_RESPONSE),
            4u64 => Ok(Self::FIND_INFORMATION_REQUEST),
            5u64 => Ok(Self::FIND_INFORMATION_RESPONSE),
            6u64 => Ok(Self::FIND_BY_TYPE_VALUE_REQUEST),
            7u64 => Ok(Self::FIND_BY_TYPE_VALUE_RESPONSE),
            8u64 => Ok(Self::READ_BY_TYPE_REQUEST),
            9u64 => Ok(Self::READ_BY_TYPE_RESPONSE),
            10u64 => Ok(Self::READ_REQUEST),
            11u64 => Ok(Self::READ_RESPONSE),
            12u64 => Ok(Self::READ_BLOB_REQUEST),
            13u64 => Ok(Self::READ_BLOB_RESPONSE),
            14u64 => Ok(Self::READ_MULTIPLE_REQUEST),
            15u64 => Ok(Self::READ_MULTIPLE_RESPONSE),
            16u64 => Ok(Self::READ_BY_GROUP_TYPE_REQUEST),
            17u64 => Ok(Self::READ_BY_GROUP_TYPE_RESPONSE),
            18u64 => Ok(Self::WRITE_REQUEST),
            19u64 => Ok(Self::WRITE_RESPONSE),
            _ => Err(ParseError::InvalidEnumValue),
        }
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> AttView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_opcode_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get__payload__end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get__payload__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_opcode(&self) -> Result<AttOpcode, ParseError> {
        AttOpcode::try_parse(self.buf.offset(self.try_get_opcode_offset()?)?.into())
    }
    #[inline]
    pub fn get_opcode(&self) -> AttOpcode {
        self.try_get_opcode().unwrap()
    }
    fn try_get_payload(&self) -> Result<SizedBitSlice<'a>, ParseError> {
        let payload_start_offset = self.try_get__payload__offset()?;
        let payload_end_offset = self.try_get__payload__end_offset()?;
        self.buf.offset(payload_start_offset)?.slice(payload_end_offset - payload_start_offset)
    }
    fn try_get_raw_payload(
        &self,
    ) -> Result<impl Iterator<Item = Result<u8, ParseError>> + '_, ParseError> {
        let view = self.try_get_payload()?;
        let count = (view.get_size_in_bits() + 7) / 8;
        Ok((0..count).map(move |i| {
            Ok(view.offset(i * 8)?.slice(8.min(view.get_size_in_bits() - i * 8))?.try_parse()?
                as u8)
        }))
    }
    pub fn get_raw_payload(&self) -> impl Iterator<Item = u8> + '_ {
        self.try_get_raw_payload().unwrap().map(|x| x.unwrap())
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_opcode()?;
        self.try_get_payload()?;
        self.try_get_raw_payload()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttView<'a> {
    type Parent = SizedBitSlice<'a>;
    type Owned = OwnedAttView;
    type Builder = AttBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttView {
        OwnedAttView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttView {
    pub fn view<'a>(&'a self) -> AttView<'a> {
        AttView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttView> for AttView<'a> {
    fn from(x: &'a OwnedAttView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttBuilder {
    pub opcode: AttOpcode,
    pub _child_: AttChild,
}
impl Builder for AttBuilder {
    type OwnedPacket = OwnedAttView;
}
impl Serializable for AttBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.opcode.serialize(writer)?;
        self._child_.serialize(writer)?;
        Ok(())
    }
}
#[derive(Debug, Clone)]
pub enum AttChild {
    RawData(Box<[u8]>),
    AttFindInformationRequest(AttFindInformationRequestBuilder),
    AttFindInformationResponse(AttFindInformationResponseBuilder),
    AttFindByTypeValueRequest(AttFindByTypeValueRequestBuilder),
    AttFindByTypeValueResponse(AttFindByTypeValueResponseBuilder),
    AttReadByGroupTypeRequest(AttReadByGroupTypeRequestBuilder),
    AttReadByGroupTypeResponse(AttReadByGroupTypeResponseBuilder),
    AttReadByTypeRequest(AttReadByTypeRequestBuilder),
    AttReadByTypeResponse(AttReadByTypeResponseBuilder),
    AttReadRequest(AttReadRequestBuilder),
    AttReadResponse(AttReadResponseBuilder),
    AttWriteRequest(AttWriteRequestBuilder),
    AttWriteResponse(AttWriteResponseBuilder),
    AttErrorResponse(AttErrorResponseBuilder),
}
impl Serializable for AttChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
            Self::AttFindInformationRequest(x) => {
                x.serialize(writer)?;
            }
            Self::AttFindInformationResponse(x) => {
                x.serialize(writer)?;
            }
            Self::AttFindByTypeValueRequest(x) => {
                x.serialize(writer)?;
            }
            Self::AttFindByTypeValueResponse(x) => {
                x.serialize(writer)?;
            }
            Self::AttReadByGroupTypeRequest(x) => {
                x.serialize(writer)?;
            }
            Self::AttReadByGroupTypeResponse(x) => {
                x.serialize(writer)?;
            }
            Self::AttReadByTypeRequest(x) => {
                x.serialize(writer)?;
            }
            Self::AttReadByTypeResponse(x) => {
                x.serialize(writer)?;
            }
            Self::AttReadRequest(x) => {
                x.serialize(writer)?;
            }
            Self::AttReadResponse(x) => {
                x.serialize(writer)?;
            }
            Self::AttWriteRequest(x) => {
                x.serialize(writer)?;
            }
            Self::AttWriteResponse(x) => {
                x.serialize(writer)?;
            }
            Self::AttErrorResponse(x) => {
                x.serialize(writer)?;
            }
        }
        Ok(())
    }
}
#[derive(Copy, Clone, PartialEq, Eq, Debug)]
pub enum AttErrorCode {
    INVALID_HANDLE,
    READ_NOT_PERMITTED,
    WRITE_NOT_PERMITTED,
    REQUEST_NOT_SUPPORTED,
    ATTRIBUTE_NOT_FOUND,
    UNLIKELY_ERROR,
}
impl AttErrorCode {
    fn try_parse(buf: BitSlice) -> Result<Self, ParseError> {
        let value = buf.slice(8usize)?.try_parse()?;
        match value {
            1u64 => Ok(Self::INVALID_HANDLE),
            2u64 => Ok(Self::READ_NOT_PERMITTED),
            3u64 => Ok(Self::WRITE_NOT_PERMITTED),
            6u64 => Ok(Self::REQUEST_NOT_SUPPORTED),
            10u64 => Ok(Self::ATTRIBUTE_NOT_FOUND),
            14u64 => Ok(Self::UNLIKELY_ERROR),
            _ => Err(ParseError::InvalidEnumValue),
        }
    }
    fn value(&self) -> u64 {
        match self {
            Self::INVALID_HANDLE => 1u64,
            Self::READ_NOT_PERMITTED => 2u64,
            Self::WRITE_NOT_PERMITTED => 3u64,
            Self::REQUEST_NOT_SUPPORTED => 6u64,
            Self::ATTRIBUTE_NOT_FOUND => 10u64,
            Self::UNLIKELY_ERROR => 14u64,
        }
    }
}
impl Serializable for AttErrorCode {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(8usize, || Ok(self.value()));
        Ok(())
    }
}
impl From<AttErrorCode> for u64 {
    fn from(x: AttErrorCode) -> u64 {
        x.value()
    }
}
impl TryFrom<u64> for AttErrorCode {
    type Error = ParseError;
    fn try_from(value: u64) -> Result<Self, ParseError> {
        match value {
            1u64 => Ok(Self::INVALID_HANDLE),
            2u64 => Ok(Self::READ_NOT_PERMITTED),
            3u64 => Ok(Self::WRITE_NOT_PERMITTED),
            6u64 => Ok(Self::REQUEST_NOT_SUPPORTED),
            10u64 => Ok(Self::ATTRIBUTE_NOT_FOUND),
            14u64 => Ok(Self::UNLIKELY_ERROR),
            _ => Err(ParseError::InvalidEnumValue),
        }
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttHandleView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> AttHandleView<'a> {
    #[inline]
    fn try_get_handle_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_handle(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_handle_offset()?)?.slice(16usize)?.try_parse()
    }
    #[inline]
    pub fn get_handle(&self) -> u64 {
        self.try_get_handle().unwrap()
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_handle()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttHandleView<'a> {
    type Parent = BitSlice<'a>;
    type Owned = OwnedAttHandleView;
    type Builder = AttHandleBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttHandleView {
        OwnedAttHandleView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttHandleView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttHandleView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttHandleView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttHandleView {
    pub fn view<'a>(&'a self) -> AttHandleView<'a> {
        AttHandleView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttHandleView> for AttHandleView<'a> {
    fn from(x: &'a OwnedAttHandleView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttHandleBuilder {
    pub handle: u64,
}
impl Builder for AttHandleBuilder {
    type OwnedPacket = OwnedAttHandleView;
}
impl Serializable for AttHandleBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(16usize, || Ok(self.handle))?;
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttFindInformationRequestView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> AttFindInformationRequestView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_starting_handle_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_ending_handle_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(32i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_starting_handle(&self) -> Result<AttHandleView<'a>, ParseError> {
        AttHandleView::try_parse(self.buf.offset(self.try_get_starting_handle_offset()?)?.into())
    }
    #[inline]
    pub fn get_starting_handle(&self) -> AttHandleView<'a> {
        self.try_get_starting_handle().unwrap()
    }
    fn try_get_ending_handle(&self) -> Result<AttHandleView<'a>, ParseError> {
        AttHandleView::try_parse(self.buf.offset(self.try_get_ending_handle_offset()?)?.into())
    }
    #[inline]
    pub fn get_ending_handle(&self) -> AttHandleView<'a> {
        self.try_get_ending_handle().unwrap()
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_starting_handle()?;
        self.try_get_ending_handle()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttFindInformationRequestView<'a> {
    type Parent = AttView<'a>;
    type Owned = OwnedAttFindInformationRequestView;
    type Builder = AttFindInformationRequestBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: AttView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttFindInformationRequestView {
        OwnedAttFindInformationRequestView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttFindInformationRequestView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttFindInformationRequestView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttFindInformationRequestView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttFindInformationRequestView {
    pub fn view<'a>(&'a self) -> AttFindInformationRequestView<'a> {
        AttFindInformationRequestView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttFindInformationRequestView> for AttFindInformationRequestView<'a> {
    fn from(x: &'a OwnedAttFindInformationRequestView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttFindInformationRequestBuilder {
    pub starting_handle: AttHandleBuilder,
    pub ending_handle: AttHandleBuilder,
}
impl Builder for AttFindInformationRequestBuilder {
    type OwnedPacket = OwnedAttFindInformationRequestView;
}
impl Serializable for AttFindInformationRequestBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.starting_handle.serialize(writer)?;
        self.ending_handle.serialize(writer)?;
        Ok(())
    }
}
impl From<AttFindInformationRequestBuilder> for AttChild {
    fn from(x: AttFindInformationRequestBuilder) -> Self {
        Self::AttFindInformationRequest(x)
    }
}
#[derive(Clone, Copy, Debug)]
pub struct UuidView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> UuidView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_data_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_data_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_header_start_offset()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    #[inline]
    fn try_get_data_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_data_count(&self) -> Result<usize, ParseError> {
        if self.try_get_data_element_size()? == 0
            || self.try_get_data_size()? % self.try_get_data_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_data_size()? / self.try_get_data_element_size()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_data_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_data_offset()?)?;
        let count = self.try_get_data_count()?;
        let element_size = self.try_get_data_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_data_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_data_iter().unwrap().map(|x| x.unwrap())
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        for elem in self.try_get_data_iter()? {
            elem?;
        }
        Ok(())
    }
}
impl<'a> Packet<'a> for UuidView<'a> {
    type Parent = SizedBitSlice<'a>;
    type Owned = OwnedUuidView;
    type Builder = UuidBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedUuidView {
        OwnedUuidView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedUuidView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedUuidView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        UuidView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedUuidView {
    pub fn view<'a>(&'a self) -> UuidView<'a> {
        UuidView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedUuidView> for UuidView<'a> {
    fn from(x: &'a OwnedUuidView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct UuidBuilder {
    pub data: Box<[u64]>,
}
impl Builder for UuidBuilder {
    type OwnedPacket = OwnedUuidView;
}
impl Serializable for UuidBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        for elem in self.data.iter() {
            writer.write_bits(8usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_size_in_bits = 8usize * self.data.len();
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Uuid16View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Uuid16View<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_data_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_data_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    #[inline]
    fn try_get_data_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_data_count(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    fn try_get_data_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_data_offset()?)?;
        let count = self.try_get_data_count()?;
        let element_size = self.try_get_data_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_data_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_data_iter().unwrap().map(|x| x.unwrap())
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        for elem in self.try_get_data_iter()? {
            elem?;
        }
        Ok(())
    }
}
impl<'a> Packet<'a> for Uuid16View<'a> {
    type Parent = BitSlice<'a>;
    type Owned = OwnedUuid16View;
    type Builder = Uuid16Builder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedUuid16View {
        OwnedUuid16View {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedUuid16View {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedUuid16View {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        Uuid16View::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedUuid16View {
    pub fn view<'a>(&'a self) -> Uuid16View<'a> {
        Uuid16View {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedUuid16View> for Uuid16View<'a> {
    fn from(x: &'a OwnedUuid16View) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct Uuid16Builder {
    pub data: Box<[u64]>,
}
impl Builder for Uuid16Builder {
    type OwnedPacket = OwnedUuid16View;
}
impl Serializable for Uuid16Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        for elem in self.data.iter() {
            writer.write_bits(8usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_size_in_bits = 8usize * self.data.len();
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Uuid128View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Uuid128View<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(128i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(128i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_data_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_data_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_data_size(&self) -> Result<usize, ParseError> {
        Ok(16usize)
    }
    #[inline]
    fn try_get_data_count(&self) -> Result<usize, ParseError> {
        Ok(16usize)
    }
    fn try_get_data_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_data_offset()?)?;
        let count = self.try_get_data_count()?;
        let element_size = self.try_get_data_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_data_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_data_iter().unwrap().map(|x| x.unwrap())
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        for elem in self.try_get_data_iter()? {
            elem?;
        }
        Ok(())
    }
}
impl<'a> Packet<'a> for Uuid128View<'a> {
    type Parent = BitSlice<'a>;
    type Owned = OwnedUuid128View;
    type Builder = Uuid128Builder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedUuid128View {
        OwnedUuid128View {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedUuid128View {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedUuid128View {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        Uuid128View::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedUuid128View {
    pub fn view<'a>(&'a self) -> Uuid128View<'a> {
        Uuid128View {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedUuid128View> for Uuid128View<'a> {
    fn from(x: &'a OwnedUuid128View) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct Uuid128Builder {
    pub data: Box<[u64]>,
}
impl Builder for Uuid128Builder {
    type OwnedPacket = OwnedUuid128View;
}
impl Serializable for Uuid128Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        for elem in self.data.iter() {
            writer.write_bits(8usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_size_in_bits = 8usize * self.data.len();
        Ok(())
    }
}
#[derive(Copy, Clone, PartialEq, Eq, Debug)]
pub enum AttFindInformationResponseFormat {
    SHORT,
    LONG,
}
impl AttFindInformationResponseFormat {
    fn try_parse(buf: BitSlice) -> Result<Self, ParseError> {
        let value = buf.slice(8usize)?.try_parse()?;
        match value {
            1u64 => Ok(Self::SHORT),
            2u64 => Ok(Self::LONG),
            _ => Err(ParseError::InvalidEnumValue),
        }
    }
    fn value(&self) -> u64 {
        match self {
            Self::SHORT => 1u64,
            Self::LONG => 2u64,
        }
    }
}
impl Serializable for AttFindInformationResponseFormat {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(8usize, || Ok(self.value()));
        Ok(())
    }
}
impl From<AttFindInformationResponseFormat> for u64 {
    fn from(x: AttFindInformationResponseFormat) -> u64 {
        x.value()
    }
}
impl TryFrom<u64> for AttFindInformationResponseFormat {
    type Error = ParseError;
    fn try_from(value: u64) -> Result<Self, ParseError> {
        match value {
            1u64 => Ok(Self::SHORT),
            2u64 => Ok(Self::LONG),
            _ => Err(ParseError::InvalidEnumValue),
        }
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttFindInformationResponseShortEntryView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> AttFindInformationResponseShortEntryView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(32i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_handle_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_uuid_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_handle(&self) -> Result<AttHandleView<'a>, ParseError> {
        AttHandleView::try_parse(self.buf.offset(self.try_get_handle_offset()?)?.into())
    }
    #[inline]
    pub fn get_handle(&self) -> AttHandleView<'a> {
        self.try_get_handle().unwrap()
    }
    fn try_get_uuid(&self) -> Result<Uuid16View<'a>, ParseError> {
        Uuid16View::try_parse(self.buf.offset(self.try_get_uuid_offset()?)?.into())
    }
    #[inline]
    pub fn get_uuid(&self) -> Uuid16View<'a> {
        self.try_get_uuid().unwrap()
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_handle()?;
        self.try_get_uuid()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttFindInformationResponseShortEntryView<'a> {
    type Parent = BitSlice<'a>;
    type Owned = OwnedAttFindInformationResponseShortEntryView;
    type Builder = AttFindInformationResponseShortEntryBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttFindInformationResponseShortEntryView {
        OwnedAttFindInformationResponseShortEntryView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttFindInformationResponseShortEntryView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttFindInformationResponseShortEntryView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttFindInformationResponseShortEntryView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttFindInformationResponseShortEntryView {
    pub fn view<'a>(&'a self) -> AttFindInformationResponseShortEntryView<'a> {
        AttFindInformationResponseShortEntryView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttFindInformationResponseShortEntryView>
    for AttFindInformationResponseShortEntryView<'a>
{
    fn from(x: &'a OwnedAttFindInformationResponseShortEntryView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttFindInformationResponseShortEntryBuilder {
    pub handle: AttHandleBuilder,
    pub uuid: Uuid16Builder,
}
impl Builder for AttFindInformationResponseShortEntryBuilder {
    type OwnedPacket = OwnedAttFindInformationResponseShortEntryView;
}
impl Serializable for AttFindInformationResponseShortEntryBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.handle.serialize(writer)?;
        self.uuid.serialize(writer)?;
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttFindInformationResponseLongEntryView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> AttFindInformationResponseLongEntryView<'a> {
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(128i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(144i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_uuid_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_handle_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_handle(&self) -> Result<AttHandleView<'a>, ParseError> {
        AttHandleView::try_parse(self.buf.offset(self.try_get_handle_offset()?)?.into())
    }
    #[inline]
    pub fn get_handle(&self) -> AttHandleView<'a> {
        self.try_get_handle().unwrap()
    }
    fn try_get_uuid(&self) -> Result<Uuid128View<'a>, ParseError> {
        Uuid128View::try_parse(self.buf.offset(self.try_get_uuid_offset()?)?.into())
    }
    #[inline]
    pub fn get_uuid(&self) -> Uuid128View<'a> {
        self.try_get_uuid().unwrap()
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_handle()?;
        self.try_get_uuid()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttFindInformationResponseLongEntryView<'a> {
    type Parent = BitSlice<'a>;
    type Owned = OwnedAttFindInformationResponseLongEntryView;
    type Builder = AttFindInformationResponseLongEntryBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttFindInformationResponseLongEntryView {
        OwnedAttFindInformationResponseLongEntryView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttFindInformationResponseLongEntryView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttFindInformationResponseLongEntryView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttFindInformationResponseLongEntryView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttFindInformationResponseLongEntryView {
    pub fn view<'a>(&'a self) -> AttFindInformationResponseLongEntryView<'a> {
        AttFindInformationResponseLongEntryView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttFindInformationResponseLongEntryView>
    for AttFindInformationResponseLongEntryView<'a>
{
    fn from(x: &'a OwnedAttFindInformationResponseLongEntryView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttFindInformationResponseLongEntryBuilder {
    pub handle: AttHandleBuilder,
    pub uuid: Uuid128Builder,
}
impl Builder for AttFindInformationResponseLongEntryBuilder {
    type OwnedPacket = OwnedAttFindInformationResponseLongEntryView;
}
impl Serializable for AttFindInformationResponseLongEntryBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.handle.serialize(writer)?;
        self.uuid.serialize(writer)?;
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttFindInformationResponseView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> AttFindInformationResponseView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_format_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get__payload__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get__payload__end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_format(&self) -> Result<AttFindInformationResponseFormat, ParseError> {
        AttFindInformationResponseFormat::try_parse(
            self.buf.offset(self.try_get_format_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_format(&self) -> AttFindInformationResponseFormat {
        self.try_get_format().unwrap()
    }
    fn try_get_payload(&self) -> Result<SizedBitSlice<'a>, ParseError> {
        let payload_start_offset = self.try_get__payload__offset()?;
        let payload_end_offset = self.try_get__payload__end_offset()?;
        self.buf.offset(payload_start_offset)?.slice(payload_end_offset - payload_start_offset)
    }
    fn try_get_raw_payload(
        &self,
    ) -> Result<impl Iterator<Item = Result<u8, ParseError>> + '_, ParseError> {
        let view = self.try_get_payload()?;
        let count = (view.get_size_in_bits() + 7) / 8;
        Ok((0..count).map(move |i| {
            Ok(view.offset(i * 8)?.slice(8.min(view.get_size_in_bits() - i * 8))?.try_parse()?
                as u8)
        }))
    }
    pub fn get_raw_payload(&self) -> impl Iterator<Item = u8> + '_ {
        self.try_get_raw_payload().unwrap().map(|x| x.unwrap())
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_format()?;
        self.try_get_payload()?;
        self.try_get_raw_payload()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttFindInformationResponseView<'a> {
    type Parent = AttView<'a>;
    type Owned = OwnedAttFindInformationResponseView;
    type Builder = AttFindInformationResponseBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: AttView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttFindInformationResponseView {
        OwnedAttFindInformationResponseView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttFindInformationResponseView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttFindInformationResponseView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttFindInformationResponseView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttFindInformationResponseView {
    pub fn view<'a>(&'a self) -> AttFindInformationResponseView<'a> {
        AttFindInformationResponseView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttFindInformationResponseView> for AttFindInformationResponseView<'a> {
    fn from(x: &'a OwnedAttFindInformationResponseView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttFindInformationResponseBuilder {
    pub format: AttFindInformationResponseFormat,
    pub _child_: AttFindInformationResponseChild,
}
impl Builder for AttFindInformationResponseBuilder {
    type OwnedPacket = OwnedAttFindInformationResponseView;
}
impl Serializable for AttFindInformationResponseBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.format.serialize(writer)?;
        self._child_.serialize(writer)?;
        Ok(())
    }
}
impl From<AttFindInformationResponseBuilder> for AttChild {
    fn from(x: AttFindInformationResponseBuilder) -> Self {
        Self::AttFindInformationResponse(x)
    }
}
#[derive(Debug, Clone)]
pub enum AttFindInformationResponseChild {
    RawData(Box<[u8]>),
    AttFindInformationShortResponse(AttFindInformationShortResponseBuilder),
    AttFindInformationLongResponse(AttFindInformationLongResponseBuilder),
}
impl Serializable for AttFindInformationResponseChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
            Self::AttFindInformationShortResponse(x) => {
                x.serialize(writer)?;
            }
            Self::AttFindInformationLongResponse(x) => {
                x.serialize(writer)?;
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttFindInformationShortResponseView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> AttFindInformationShortResponseView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_data_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_data_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_header_start_offset()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    #[inline]
    fn try_get_data_element_size(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    #[inline]
    fn try_get_data_count(&self) -> Result<usize, ParseError> {
        if self.try_get_data_element_size()? == 0
            || self.try_get_data_size()? % self.try_get_data_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_data_size()? / self.try_get_data_element_size()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_data_iter(
        &self,
    ) -> Result<
        impl Iterator<Item = Result<AttFindInformationResponseShortEntryView<'a>, ParseError>> + 'a,
        ParseError,
    > {
        let view = self.buf.offset(self.try_get_data_offset()?)?;
        let count = self.try_get_data_count()?;
        let element_size = self.try_get_data_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            AttFindInformationResponseShortEntryView::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_data_iter(
        &self,
    ) -> impl Iterator<Item = AttFindInformationResponseShortEntryView<'a>> + 'a {
        self.try_get_data_iter().unwrap().map(|x| x.unwrap())
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        for elem in self.try_get_data_iter()? {
            elem?;
        }
        Ok(())
    }
}
impl<'a> Packet<'a> for AttFindInformationShortResponseView<'a> {
    type Parent = AttFindInformationResponseView<'a>;
    type Owned = OwnedAttFindInformationShortResponseView;
    type Builder = AttFindInformationShortResponseBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: AttFindInformationResponseView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttFindInformationShortResponseView {
        OwnedAttFindInformationShortResponseView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttFindInformationShortResponseView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttFindInformationShortResponseView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttFindInformationShortResponseView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttFindInformationShortResponseView {
    pub fn view<'a>(&'a self) -> AttFindInformationShortResponseView<'a> {
        AttFindInformationShortResponseView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttFindInformationShortResponseView>
    for AttFindInformationShortResponseView<'a>
{
    fn from(x: &'a OwnedAttFindInformationShortResponseView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttFindInformationShortResponseBuilder {
    pub data: Box<[AttFindInformationResponseShortEntryBuilder]>,
}
impl Builder for AttFindInformationShortResponseBuilder {
    type OwnedPacket = OwnedAttFindInformationShortResponseView;
}
impl Serializable for AttFindInformationShortResponseBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        let mut most_recent_array_size_in_bits = 0;
        for elem in self.data.iter() {
            most_recent_array_size_in_bits += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
impl From<AttFindInformationShortResponseBuilder> for AttFindInformationResponseChild {
    fn from(x: AttFindInformationShortResponseBuilder) -> Self {
        Self::AttFindInformationShortResponse(x)
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttFindInformationLongResponseView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> AttFindInformationLongResponseView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_data_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_data_element_size(&self) -> Result<usize, ParseError> {
        Ok(18usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_data_count(&self) -> Result<usize, ParseError> {
        if self.try_get_data_element_size()? == 0
            || self.try_get_data_size()? % self.try_get_data_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_data_size()? / self.try_get_data_element_size()?)
    }
    #[inline]
    fn try_get_data_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_header_start_offset()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    fn try_get_data_iter(
        &self,
    ) -> Result<
        impl Iterator<Item = Result<AttFindInformationResponseLongEntryView<'a>, ParseError>> + 'a,
        ParseError,
    > {
        let view = self.buf.offset(self.try_get_data_offset()?)?;
        let count = self.try_get_data_count()?;
        let element_size = self.try_get_data_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            AttFindInformationResponseLongEntryView::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_data_iter(
        &self,
    ) -> impl Iterator<Item = AttFindInformationResponseLongEntryView<'a>> + 'a {
        self.try_get_data_iter().unwrap().map(|x| x.unwrap())
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        for elem in self.try_get_data_iter()? {
            elem?;
        }
        Ok(())
    }
}
impl<'a> Packet<'a> for AttFindInformationLongResponseView<'a> {
    type Parent = AttFindInformationResponseView<'a>;
    type Owned = OwnedAttFindInformationLongResponseView;
    type Builder = AttFindInformationLongResponseBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: AttFindInformationResponseView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttFindInformationLongResponseView {
        OwnedAttFindInformationLongResponseView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttFindInformationLongResponseView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttFindInformationLongResponseView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttFindInformationLongResponseView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttFindInformationLongResponseView {
    pub fn view<'a>(&'a self) -> AttFindInformationLongResponseView<'a> {
        AttFindInformationLongResponseView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttFindInformationLongResponseView>
    for AttFindInformationLongResponseView<'a>
{
    fn from(x: &'a OwnedAttFindInformationLongResponseView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttFindInformationLongResponseBuilder {
    pub data: Box<[AttFindInformationResponseLongEntryBuilder]>,
}
impl Builder for AttFindInformationLongResponseBuilder {
    type OwnedPacket = OwnedAttFindInformationLongResponseView;
}
impl Serializable for AttFindInformationLongResponseBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        let mut most_recent_array_size_in_bits = 0;
        for elem in self.data.iter() {
            most_recent_array_size_in_bits += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
impl From<AttFindInformationLongResponseBuilder> for AttFindInformationResponseChild {
    fn from(x: AttFindInformationLongResponseBuilder) -> Self {
        Self::AttFindInformationLongResponse(x)
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttCharacteristicPropertiesView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> AttCharacteristicPropertiesView<'a> {
    #[inline]
    fn try_get_write_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_4(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_3()?
            .checked_add_signed(1i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_indicate_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_5()?)
    }
    #[inline]
    fn try_get_custom_offset_6(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_5()?
            .checked_add_signed(1i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_read_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add_signed(1i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_notify_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_4()?)
    }
    #[inline]
    fn try_get_custom_offset_5(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_4()?
            .checked_add_signed(1i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_7(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_6()?
            .checked_add_signed(1i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_8(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_7()?
            .checked_add_signed(1i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_broadcast_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_extended_properties_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_7()?)
    }
    #[inline]
    fn try_get_authenticated_signed_writes_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_6()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(1i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_write_without_response_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(1i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_broadcast(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_broadcast_offset()?)?.slice(1usize)?.try_parse()
    }
    #[inline]
    pub fn get_broadcast(&self) -> u64 {
        self.try_get_broadcast().unwrap()
    }
    fn try_get_read(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_read_offset()?)?.slice(1usize)?.try_parse()
    }
    #[inline]
    pub fn get_read(&self) -> u64 {
        self.try_get_read().unwrap()
    }
    fn try_get_write_without_response(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_write_without_response_offset()?)?.slice(1usize)?.try_parse()
    }
    #[inline]
    pub fn get_write_without_response(&self) -> u64 {
        self.try_get_write_without_response().unwrap()
    }
    fn try_get_write(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_write_offset()?)?.slice(1usize)?.try_parse()
    }
    #[inline]
    pub fn get_write(&self) -> u64 {
        self.try_get_write().unwrap()
    }
    fn try_get_notify(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_notify_offset()?)?.slice(1usize)?.try_parse()
    }
    #[inline]
    pub fn get_notify(&self) -> u64 {
        self.try_get_notify().unwrap()
    }
    fn try_get_indicate(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_indicate_offset()?)?.slice(1usize)?.try_parse()
    }
    #[inline]
    pub fn get_indicate(&self) -> u64 {
        self.try_get_indicate().unwrap()
    }
    fn try_get_authenticated_signed_writes(&self) -> Result<u64, ParseError> {
        self.buf
            .offset(self.try_get_authenticated_signed_writes_offset()?)?
            .slice(1usize)?
            .try_parse()
    }
    #[inline]
    pub fn get_authenticated_signed_writes(&self) -> u64 {
        self.try_get_authenticated_signed_writes().unwrap()
    }
    fn try_get_extended_properties(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_extended_properties_offset()?)?.slice(1usize)?.try_parse()
    }
    #[inline]
    pub fn get_extended_properties(&self) -> u64 {
        self.try_get_extended_properties().unwrap()
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_broadcast()?;
        self.try_get_read()?;
        self.try_get_write_without_response()?;
        self.try_get_write()?;
        self.try_get_notify()?;
        self.try_get_indicate()?;
        self.try_get_authenticated_signed_writes()?;
        self.try_get_extended_properties()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttCharacteristicPropertiesView<'a> {
    type Parent = BitSlice<'a>;
    type Owned = OwnedAttCharacteristicPropertiesView;
    type Builder = AttCharacteristicPropertiesBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttCharacteristicPropertiesView {
        OwnedAttCharacteristicPropertiesView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttCharacteristicPropertiesView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttCharacteristicPropertiesView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttCharacteristicPropertiesView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttCharacteristicPropertiesView {
    pub fn view<'a>(&'a self) -> AttCharacteristicPropertiesView<'a> {
        AttCharacteristicPropertiesView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttCharacteristicPropertiesView> for AttCharacteristicPropertiesView<'a> {
    fn from(x: &'a OwnedAttCharacteristicPropertiesView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttCharacteristicPropertiesBuilder {
    pub broadcast: u64,
    pub read: u64,
    pub write_without_response: u64,
    pub write: u64,
    pub notify: u64,
    pub indicate: u64,
    pub authenticated_signed_writes: u64,
    pub extended_properties: u64,
}
impl Builder for AttCharacteristicPropertiesBuilder {
    type OwnedPacket = OwnedAttCharacteristicPropertiesView;
}
impl Serializable for AttCharacteristicPropertiesBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(1usize, || Ok(self.broadcast))?;
        writer.write_bits(1usize, || Ok(self.read))?;
        writer.write_bits(1usize, || Ok(self.write_without_response))?;
        writer.write_bits(1usize, || Ok(self.write))?;
        writer.write_bits(1usize, || Ok(self.notify))?;
        writer.write_bits(1usize, || Ok(self.indicate))?;
        writer.write_bits(1usize, || Ok(self.authenticated_signed_writes))?;
        writer.write_bits(1usize, || Ok(self.extended_properties))?;
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttCharacteristicDeclarationValueView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> AttCharacteristicDeclarationValueView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_uuid_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_properties_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_handle_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_uuid_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_uuid_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_custom_offset_2()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_properties(&self) -> Result<AttCharacteristicPropertiesView<'a>, ParseError> {
        AttCharacteristicPropertiesView::try_parse(
            self.buf.offset(self.try_get_properties_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_properties(&self) -> AttCharacteristicPropertiesView<'a> {
        self.try_get_properties().unwrap()
    }
    fn try_get_handle(&self) -> Result<AttHandleView<'a>, ParseError> {
        AttHandleView::try_parse(self.buf.offset(self.try_get_handle_offset()?)?.into())
    }
    #[inline]
    pub fn get_handle(&self) -> AttHandleView<'a> {
        self.try_get_handle().unwrap()
    }
    fn try_get_uuid(&self) -> Result<UuidView<'a>, ParseError> {
        UuidView::try_parse(
            self.buf
                .offset(self.try_get_uuid_offset()?)?
                .slice(
                    self.try_get_uuid_end_offset()?
                        .checked_sub(self.try_get_uuid_offset()?)
                        .ok_or(ParseError::ArithmeticOverflow)?,
                )?
                .into(),
        )
    }
    #[inline]
    pub fn get_uuid(&self) -> UuidView<'a> {
        self.try_get_uuid().unwrap()
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_properties()?;
        self.try_get_handle()?;
        self.try_get_uuid()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttCharacteristicDeclarationValueView<'a> {
    type Parent = AttAttributeDataView<'a>;
    type Owned = OwnedAttCharacteristicDeclarationValueView;
    type Builder = AttCharacteristicDeclarationValueBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: AttAttributeDataView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttCharacteristicDeclarationValueView {
        OwnedAttCharacteristicDeclarationValueView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttCharacteristicDeclarationValueView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttCharacteristicDeclarationValueView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttCharacteristicDeclarationValueView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttCharacteristicDeclarationValueView {
    pub fn view<'a>(&'a self) -> AttCharacteristicDeclarationValueView<'a> {
        AttCharacteristicDeclarationValueView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttCharacteristicDeclarationValueView>
    for AttCharacteristicDeclarationValueView<'a>
{
    fn from(x: &'a OwnedAttCharacteristicDeclarationValueView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttCharacteristicDeclarationValueBuilder {
    pub properties: AttCharacteristicPropertiesBuilder,
    pub handle: AttHandleBuilder,
    pub uuid: UuidBuilder,
}
impl Builder for AttCharacteristicDeclarationValueBuilder {
    type OwnedPacket = OwnedAttCharacteristicDeclarationValueView;
}
impl Serializable for AttCharacteristicDeclarationValueBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.properties.serialize(writer)?;
        self.handle.serialize(writer)?;
        self.uuid.serialize(writer)?;
        Ok(())
    }
}
impl From<AttCharacteristicDeclarationValueBuilder> for AttAttributeDataChild {
    fn from(x: AttCharacteristicDeclarationValueBuilder) -> Self {
        Self::AttCharacteristicDeclarationValue(x)
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttServiceDeclarationValueView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> AttServiceDeclarationValueView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_uuid_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_uuid_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_uuid_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_header_start_offset()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_uuid(&self) -> Result<UuidView<'a>, ParseError> {
        UuidView::try_parse(
            self.buf
                .offset(self.try_get_uuid_offset()?)?
                .slice(
                    self.try_get_uuid_end_offset()?
                        .checked_sub(self.try_get_uuid_offset()?)
                        .ok_or(ParseError::ArithmeticOverflow)?,
                )?
                .into(),
        )
    }
    #[inline]
    pub fn get_uuid(&self) -> UuidView<'a> {
        self.try_get_uuid().unwrap()
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_uuid()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttServiceDeclarationValueView<'a> {
    type Parent = AttAttributeDataView<'a>;
    type Owned = OwnedAttServiceDeclarationValueView;
    type Builder = AttServiceDeclarationValueBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: AttAttributeDataView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttServiceDeclarationValueView {
        OwnedAttServiceDeclarationValueView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttServiceDeclarationValueView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttServiceDeclarationValueView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttServiceDeclarationValueView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttServiceDeclarationValueView {
    pub fn view<'a>(&'a self) -> AttServiceDeclarationValueView<'a> {
        AttServiceDeclarationValueView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttServiceDeclarationValueView> for AttServiceDeclarationValueView<'a> {
    fn from(x: &'a OwnedAttServiceDeclarationValueView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttServiceDeclarationValueBuilder {
    pub uuid: UuidBuilder,
}
impl Builder for AttServiceDeclarationValueBuilder {
    type OwnedPacket = OwnedAttServiceDeclarationValueView;
}
impl Serializable for AttServiceDeclarationValueBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.uuid.serialize(writer)?;
        Ok(())
    }
}
impl From<AttServiceDeclarationValueBuilder> for AttAttributeDataChild {
    fn from(x: AttServiceDeclarationValueBuilder) -> Self {
        Self::AttServiceDeclarationValue(x)
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttAttributeDataView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> AttAttributeDataView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get__payload__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get__payload__end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_payload(&self) -> Result<SizedBitSlice<'a>, ParseError> {
        let payload_start_offset = self.try_get__payload__offset()?;
        let payload_end_offset = self.try_get__payload__end_offset()?;
        self.buf.offset(payload_start_offset)?.slice(payload_end_offset - payload_start_offset)
    }
    fn try_get_raw_payload(
        &self,
    ) -> Result<impl Iterator<Item = Result<u8, ParseError>> + '_, ParseError> {
        let view = self.try_get_payload()?;
        let count = (view.get_size_in_bits() + 7) / 8;
        Ok((0..count).map(move |i| {
            Ok(view.offset(i * 8)?.slice(8.min(view.get_size_in_bits() - i * 8))?.try_parse()?
                as u8)
        }))
    }
    pub fn get_raw_payload(&self) -> impl Iterator<Item = u8> + '_ {
        self.try_get_raw_payload().unwrap().map(|x| x.unwrap())
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_payload()?;
        self.try_get_raw_payload()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttAttributeDataView<'a> {
    type Parent = SizedBitSlice<'a>;
    type Owned = OwnedAttAttributeDataView;
    type Builder = AttAttributeDataBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttAttributeDataView {
        OwnedAttAttributeDataView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttAttributeDataView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttAttributeDataView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttAttributeDataView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttAttributeDataView {
    pub fn view<'a>(&'a self) -> AttAttributeDataView<'a> {
        AttAttributeDataView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttAttributeDataView> for AttAttributeDataView<'a> {
    fn from(x: &'a OwnedAttAttributeDataView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttAttributeDataBuilder {
    pub _child_: AttAttributeDataChild,
}
impl Builder for AttAttributeDataBuilder {
    type OwnedPacket = OwnedAttAttributeDataView;
}
impl Serializable for AttAttributeDataBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self._child_.serialize(writer)?;
        Ok(())
    }
}
#[derive(Debug, Clone)]
pub enum AttAttributeDataChild {
    RawData(Box<[u8]>),
    AttCharacteristicDeclarationValue(AttCharacteristicDeclarationValueBuilder),
    AttServiceDeclarationValue(AttServiceDeclarationValueBuilder),
}
impl Serializable for AttAttributeDataChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
            Self::AttCharacteristicDeclarationValue(x) => {
                x.serialize(writer)?;
            }
            Self::AttServiceDeclarationValue(x) => {
                x.serialize(writer)?;
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttFindByTypeValueRequestView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> AttFindByTypeValueRequestView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_starting_handle_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_4(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_attribute_value_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_ending_handle_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_attribute_type_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_attribute_value_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_attribute_value_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_custom_offset_3()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    fn try_get_starting_handle(&self) -> Result<AttHandleView<'a>, ParseError> {
        AttHandleView::try_parse(self.buf.offset(self.try_get_starting_handle_offset()?)?.into())
    }
    #[inline]
    pub fn get_starting_handle(&self) -> AttHandleView<'a> {
        self.try_get_starting_handle().unwrap()
    }
    fn try_get_ending_handle(&self) -> Result<AttHandleView<'a>, ParseError> {
        AttHandleView::try_parse(self.buf.offset(self.try_get_ending_handle_offset()?)?.into())
    }
    #[inline]
    pub fn get_ending_handle(&self) -> AttHandleView<'a> {
        self.try_get_ending_handle().unwrap()
    }
    fn try_get_attribute_type(&self) -> Result<Uuid16View<'a>, ParseError> {
        Uuid16View::try_parse(self.buf.offset(self.try_get_attribute_type_offset()?)?.into())
    }
    #[inline]
    pub fn get_attribute_type(&self) -> Uuid16View<'a> {
        self.try_get_attribute_type().unwrap()
    }
    fn try_get_attribute_value(&self) -> Result<AttAttributeDataView<'a>, ParseError> {
        AttAttributeDataView::try_parse(
            self.buf
                .offset(self.try_get_attribute_value_offset()?)?
                .slice(
                    self.try_get_attribute_value_end_offset()?
                        .checked_sub(self.try_get_attribute_value_offset()?)
                        .ok_or(ParseError::ArithmeticOverflow)?,
                )?
                .into(),
        )
    }
    #[inline]
    pub fn get_attribute_value(&self) -> AttAttributeDataView<'a> {
        self.try_get_attribute_value().unwrap()
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_starting_handle()?;
        self.try_get_ending_handle()?;
        self.try_get_attribute_type()?;
        self.try_get_attribute_value()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttFindByTypeValueRequestView<'a> {
    type Parent = AttView<'a>;
    type Owned = OwnedAttFindByTypeValueRequestView;
    type Builder = AttFindByTypeValueRequestBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: AttView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttFindByTypeValueRequestView {
        OwnedAttFindByTypeValueRequestView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttFindByTypeValueRequestView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttFindByTypeValueRequestView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttFindByTypeValueRequestView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttFindByTypeValueRequestView {
    pub fn view<'a>(&'a self) -> AttFindByTypeValueRequestView<'a> {
        AttFindByTypeValueRequestView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttFindByTypeValueRequestView> for AttFindByTypeValueRequestView<'a> {
    fn from(x: &'a OwnedAttFindByTypeValueRequestView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttFindByTypeValueRequestBuilder {
    pub starting_handle: AttHandleBuilder,
    pub ending_handle: AttHandleBuilder,
    pub attribute_type: Uuid16Builder,
    pub attribute_value: AttAttributeDataBuilder,
}
impl Builder for AttFindByTypeValueRequestBuilder {
    type OwnedPacket = OwnedAttFindByTypeValueRequestView;
}
impl Serializable for AttFindByTypeValueRequestBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.starting_handle.serialize(writer)?;
        self.ending_handle.serialize(writer)?;
        self.attribute_type.serialize(writer)?;
        self.attribute_value.serialize(writer)?;
        Ok(())
    }
}
impl From<AttFindByTypeValueRequestBuilder> for AttChild {
    fn from(x: AttFindByTypeValueRequestBuilder) -> Self {
        Self::AttFindByTypeValueRequest(x)
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttributeHandleRangeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> AttributeHandleRangeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(32i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_found_attribute_handle_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_group_end_handle_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_found_attribute_handle(&self) -> Result<AttHandleView<'a>, ParseError> {
        AttHandleView::try_parse(
            self.buf.offset(self.try_get_found_attribute_handle_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_found_attribute_handle(&self) -> AttHandleView<'a> {
        self.try_get_found_attribute_handle().unwrap()
    }
    fn try_get_group_end_handle(&self) -> Result<AttHandleView<'a>, ParseError> {
        AttHandleView::try_parse(self.buf.offset(self.try_get_group_end_handle_offset()?)?.into())
    }
    #[inline]
    pub fn get_group_end_handle(&self) -> AttHandleView<'a> {
        self.try_get_group_end_handle().unwrap()
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_found_attribute_handle()?;
        self.try_get_group_end_handle()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttributeHandleRangeView<'a> {
    type Parent = BitSlice<'a>;
    type Owned = OwnedAttributeHandleRangeView;
    type Builder = AttributeHandleRangeBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttributeHandleRangeView {
        OwnedAttributeHandleRangeView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttributeHandleRangeView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttributeHandleRangeView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttributeHandleRangeView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttributeHandleRangeView {
    pub fn view<'a>(&'a self) -> AttributeHandleRangeView<'a> {
        AttributeHandleRangeView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttributeHandleRangeView> for AttributeHandleRangeView<'a> {
    fn from(x: &'a OwnedAttributeHandleRangeView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttributeHandleRangeBuilder {
    pub found_attribute_handle: AttHandleBuilder,
    pub group_end_handle: AttHandleBuilder,
}
impl Builder for AttributeHandleRangeBuilder {
    type OwnedPacket = OwnedAttributeHandleRangeView;
}
impl Serializable for AttributeHandleRangeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.found_attribute_handle.serialize(writer)?;
        self.group_end_handle.serialize(writer)?;
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttFindByTypeValueResponseView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> AttFindByTypeValueResponseView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_handles_info_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_handles_info_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_header_start_offset()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    #[inline]
    fn try_get_handles_info_element_size(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    #[inline]
    fn try_get_handles_info_count(&self) -> Result<usize, ParseError> {
        if self.try_get_handles_info_element_size()? == 0
            || self.try_get_handles_info_size()? % self.try_get_handles_info_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_handles_info_size()? / self.try_get_handles_info_element_size()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_handles_info_iter(
        &self,
    ) -> Result<
        impl Iterator<Item = Result<AttributeHandleRangeView<'a>, ParseError>> + 'a,
        ParseError,
    > {
        let view = self.buf.offset(self.try_get_handles_info_offset()?)?;
        let count = self.try_get_handles_info_count()?;
        let element_size = self.try_get_handles_info_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            AttributeHandleRangeView::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_handles_info_iter(&self) -> impl Iterator<Item = AttributeHandleRangeView<'a>> + 'a {
        self.try_get_handles_info_iter().unwrap().map(|x| x.unwrap())
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        for elem in self.try_get_handles_info_iter()? {
            elem?;
        }
        Ok(())
    }
}
impl<'a> Packet<'a> for AttFindByTypeValueResponseView<'a> {
    type Parent = AttView<'a>;
    type Owned = OwnedAttFindByTypeValueResponseView;
    type Builder = AttFindByTypeValueResponseBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: AttView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttFindByTypeValueResponseView {
        OwnedAttFindByTypeValueResponseView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttFindByTypeValueResponseView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttFindByTypeValueResponseView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttFindByTypeValueResponseView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttFindByTypeValueResponseView {
    pub fn view<'a>(&'a self) -> AttFindByTypeValueResponseView<'a> {
        AttFindByTypeValueResponseView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttFindByTypeValueResponseView> for AttFindByTypeValueResponseView<'a> {
    fn from(x: &'a OwnedAttFindByTypeValueResponseView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttFindByTypeValueResponseBuilder {
    pub handles_info: Box<[AttributeHandleRangeBuilder]>,
}
impl Builder for AttFindByTypeValueResponseBuilder {
    type OwnedPacket = OwnedAttFindByTypeValueResponseView;
}
impl Serializable for AttFindByTypeValueResponseBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        let mut most_recent_array_size_in_bits = 0;
        for elem in self.handles_info.iter() {
            most_recent_array_size_in_bits += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
impl From<AttFindByTypeValueResponseBuilder> for AttChild {
    fn from(x: AttFindByTypeValueResponseBuilder) -> Self {
        Self::AttFindByTypeValueResponse(x)
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttReadByGroupTypeRequestView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> AttReadByGroupTypeRequestView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_attribute_group_type_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_ending_handle_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_starting_handle_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_attribute_group_type_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_attribute_group_type_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_custom_offset_2()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    fn try_get_starting_handle(&self) -> Result<AttHandleView<'a>, ParseError> {
        AttHandleView::try_parse(self.buf.offset(self.try_get_starting_handle_offset()?)?.into())
    }
    #[inline]
    pub fn get_starting_handle(&self) -> AttHandleView<'a> {
        self.try_get_starting_handle().unwrap()
    }
    fn try_get_ending_handle(&self) -> Result<AttHandleView<'a>, ParseError> {
        AttHandleView::try_parse(self.buf.offset(self.try_get_ending_handle_offset()?)?.into())
    }
    #[inline]
    pub fn get_ending_handle(&self) -> AttHandleView<'a> {
        self.try_get_ending_handle().unwrap()
    }
    fn try_get_attribute_group_type(&self) -> Result<UuidView<'a>, ParseError> {
        UuidView::try_parse(
            self.buf
                .offset(self.try_get_attribute_group_type_offset()?)?
                .slice(
                    self.try_get_attribute_group_type_end_offset()?
                        .checked_sub(self.try_get_attribute_group_type_offset()?)
                        .ok_or(ParseError::ArithmeticOverflow)?,
                )?
                .into(),
        )
    }
    #[inline]
    pub fn get_attribute_group_type(&self) -> UuidView<'a> {
        self.try_get_attribute_group_type().unwrap()
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_starting_handle()?;
        self.try_get_ending_handle()?;
        self.try_get_attribute_group_type()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttReadByGroupTypeRequestView<'a> {
    type Parent = AttView<'a>;
    type Owned = OwnedAttReadByGroupTypeRequestView;
    type Builder = AttReadByGroupTypeRequestBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: AttView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttReadByGroupTypeRequestView {
        OwnedAttReadByGroupTypeRequestView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttReadByGroupTypeRequestView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttReadByGroupTypeRequestView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttReadByGroupTypeRequestView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttReadByGroupTypeRequestView {
    pub fn view<'a>(&'a self) -> AttReadByGroupTypeRequestView<'a> {
        AttReadByGroupTypeRequestView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttReadByGroupTypeRequestView> for AttReadByGroupTypeRequestView<'a> {
    fn from(x: &'a OwnedAttReadByGroupTypeRequestView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttReadByGroupTypeRequestBuilder {
    pub starting_handle: AttHandleBuilder,
    pub ending_handle: AttHandleBuilder,
    pub attribute_group_type: UuidBuilder,
}
impl Builder for AttReadByGroupTypeRequestBuilder {
    type OwnedPacket = OwnedAttReadByGroupTypeRequestView;
}
impl Serializable for AttReadByGroupTypeRequestBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.starting_handle.serialize(writer)?;
        self.ending_handle.serialize(writer)?;
        self.attribute_group_type.serialize(writer)?;
        Ok(())
    }
}
impl From<AttReadByGroupTypeRequestBuilder> for AttChild {
    fn from(x: AttReadByGroupTypeRequestBuilder) -> Self {
        Self::AttReadByGroupTypeRequest(x)
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttReadByGroupTypeDataElementView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> AttReadByGroupTypeDataElementView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_value_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_value_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_handle_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_end_group_handle_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_value_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_custom_offset_2()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_handle(&self) -> Result<AttHandleView<'a>, ParseError> {
        AttHandleView::try_parse(self.buf.offset(self.try_get_handle_offset()?)?.into())
    }
    #[inline]
    pub fn get_handle(&self) -> AttHandleView<'a> {
        self.try_get_handle().unwrap()
    }
    fn try_get_end_group_handle(&self) -> Result<AttHandleView<'a>, ParseError> {
        AttHandleView::try_parse(self.buf.offset(self.try_get_end_group_handle_offset()?)?.into())
    }
    #[inline]
    pub fn get_end_group_handle(&self) -> AttHandleView<'a> {
        self.try_get_end_group_handle().unwrap()
    }
    fn try_get_value(&self) -> Result<AttAttributeDataView<'a>, ParseError> {
        AttAttributeDataView::try_parse(
            self.buf
                .offset(self.try_get_value_offset()?)?
                .slice(
                    self.try_get_value_end_offset()?
                        .checked_sub(self.try_get_value_offset()?)
                        .ok_or(ParseError::ArithmeticOverflow)?,
                )?
                .into(),
        )
    }
    #[inline]
    pub fn get_value(&self) -> AttAttributeDataView<'a> {
        self.try_get_value().unwrap()
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_handle()?;
        self.try_get_end_group_handle()?;
        self.try_get_value()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttReadByGroupTypeDataElementView<'a> {
    type Parent = SizedBitSlice<'a>;
    type Owned = OwnedAttReadByGroupTypeDataElementView;
    type Builder = AttReadByGroupTypeDataElementBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttReadByGroupTypeDataElementView {
        OwnedAttReadByGroupTypeDataElementView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttReadByGroupTypeDataElementView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttReadByGroupTypeDataElementView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttReadByGroupTypeDataElementView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttReadByGroupTypeDataElementView {
    pub fn view<'a>(&'a self) -> AttReadByGroupTypeDataElementView<'a> {
        AttReadByGroupTypeDataElementView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttReadByGroupTypeDataElementView>
    for AttReadByGroupTypeDataElementView<'a>
{
    fn from(x: &'a OwnedAttReadByGroupTypeDataElementView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttReadByGroupTypeDataElementBuilder {
    pub handle: AttHandleBuilder,
    pub end_group_handle: AttHandleBuilder,
    pub value: AttAttributeDataBuilder,
}
impl Builder for AttReadByGroupTypeDataElementBuilder {
    type OwnedPacket = OwnedAttReadByGroupTypeDataElementView;
}
impl Serializable for AttReadByGroupTypeDataElementBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.handle.serialize(writer)?;
        self.end_group_handle.serialize(writer)?;
        self.value.serialize(writer)?;
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttReadByGroupTypeResponseView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> AttReadByGroupTypeResponseView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_data_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_data_count(&self) -> Result<usize, ParseError> {
        if self.try_get_data_element_size()? == 0
            || self.try_get_data_size()? % self.try_get_data_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_data_size()? / self.try_get_data_element_size()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_data_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_custom_offset_1()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    #[inline]
    fn try_get_data_element_size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(8usize)?.try_parse()?
            as usize)
    }
    fn try_get_data_iter(
        &self,
    ) -> Result<
        impl Iterator<Item = Result<AttReadByGroupTypeDataElementView<'a>, ParseError>> + 'a,
        ParseError,
    > {
        let view = self.buf.offset(self.try_get_data_offset()?)?;
        let count = self.try_get_data_count()?;
        let element_size = self.try_get_data_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            AttReadByGroupTypeDataElementView::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_data_iter(
        &self,
    ) -> impl Iterator<Item = AttReadByGroupTypeDataElementView<'a>> + 'a {
        self.try_get_data_iter().unwrap().map(|x| x.unwrap())
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        for elem in self.try_get_data_iter()? {
            elem?;
        }
        Ok(())
    }
}
impl<'a> Packet<'a> for AttReadByGroupTypeResponseView<'a> {
    type Parent = AttView<'a>;
    type Owned = OwnedAttReadByGroupTypeResponseView;
    type Builder = AttReadByGroupTypeResponseBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: AttView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttReadByGroupTypeResponseView {
        OwnedAttReadByGroupTypeResponseView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttReadByGroupTypeResponseView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttReadByGroupTypeResponseView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttReadByGroupTypeResponseView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttReadByGroupTypeResponseView {
    pub fn view<'a>(&'a self) -> AttReadByGroupTypeResponseView<'a> {
        AttReadByGroupTypeResponseView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttReadByGroupTypeResponseView> for AttReadByGroupTypeResponseView<'a> {
    fn from(x: &'a OwnedAttReadByGroupTypeResponseView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttReadByGroupTypeResponseBuilder {
    pub data: Box<[AttReadByGroupTypeDataElementBuilder]>,
}
impl Builder for AttReadByGroupTypeResponseBuilder {
    type OwnedPacket = OwnedAttReadByGroupTypeResponseView;
}
impl Serializable for AttReadByGroupTypeResponseBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        let get_element_size = || {
            Ok(if let Some(field) = self.data.get(0) {
                let size_in_bits = field.size_in_bits()?;
                if size_in_bits % 8 == 0 {
                    (size_in_bits / 8) as u64
                } else {
                    return Err(SerializeError::AlignmentError);
                }
            } else {
                0
            })
        };
        writer.write_bits(8usize, || get_element_size())?;
        let mut most_recent_array_size_in_bits = 0;
        for elem in self.data.iter() {
            most_recent_array_size_in_bits += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
impl From<AttReadByGroupTypeResponseBuilder> for AttChild {
    fn from(x: AttReadByGroupTypeResponseBuilder) -> Self {
        Self::AttReadByGroupTypeResponse(x)
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttReadByTypeRequestView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> AttReadByTypeRequestView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_attribute_type_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_attribute_type_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_ending_handle_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_starting_handle_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_attribute_type_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_custom_offset_2()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    fn try_get_starting_handle(&self) -> Result<AttHandleView<'a>, ParseError> {
        AttHandleView::try_parse(self.buf.offset(self.try_get_starting_handle_offset()?)?.into())
    }
    #[inline]
    pub fn get_starting_handle(&self) -> AttHandleView<'a> {
        self.try_get_starting_handle().unwrap()
    }
    fn try_get_ending_handle(&self) -> Result<AttHandleView<'a>, ParseError> {
        AttHandleView::try_parse(self.buf.offset(self.try_get_ending_handle_offset()?)?.into())
    }
    #[inline]
    pub fn get_ending_handle(&self) -> AttHandleView<'a> {
        self.try_get_ending_handle().unwrap()
    }
    fn try_get_attribute_type(&self) -> Result<UuidView<'a>, ParseError> {
        UuidView::try_parse(
            self.buf
                .offset(self.try_get_attribute_type_offset()?)?
                .slice(
                    self.try_get_attribute_type_end_offset()?
                        .checked_sub(self.try_get_attribute_type_offset()?)
                        .ok_or(ParseError::ArithmeticOverflow)?,
                )?
                .into(),
        )
    }
    #[inline]
    pub fn get_attribute_type(&self) -> UuidView<'a> {
        self.try_get_attribute_type().unwrap()
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_starting_handle()?;
        self.try_get_ending_handle()?;
        self.try_get_attribute_type()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttReadByTypeRequestView<'a> {
    type Parent = AttView<'a>;
    type Owned = OwnedAttReadByTypeRequestView;
    type Builder = AttReadByTypeRequestBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: AttView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttReadByTypeRequestView {
        OwnedAttReadByTypeRequestView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttReadByTypeRequestView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttReadByTypeRequestView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttReadByTypeRequestView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttReadByTypeRequestView {
    pub fn view<'a>(&'a self) -> AttReadByTypeRequestView<'a> {
        AttReadByTypeRequestView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttReadByTypeRequestView> for AttReadByTypeRequestView<'a> {
    fn from(x: &'a OwnedAttReadByTypeRequestView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttReadByTypeRequestBuilder {
    pub starting_handle: AttHandleBuilder,
    pub ending_handle: AttHandleBuilder,
    pub attribute_type: UuidBuilder,
}
impl Builder for AttReadByTypeRequestBuilder {
    type OwnedPacket = OwnedAttReadByTypeRequestView;
}
impl Serializable for AttReadByTypeRequestBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.starting_handle.serialize(writer)?;
        self.ending_handle.serialize(writer)?;
        self.attribute_type.serialize(writer)?;
        Ok(())
    }
}
impl From<AttReadByTypeRequestBuilder> for AttChild {
    fn from(x: AttReadByTypeRequestBuilder) -> Self {
        Self::AttReadByTypeRequest(x)
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttReadByTypeDataElementView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> AttReadByTypeDataElementView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_value_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_value_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_handle_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_value_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_custom_offset_1()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    fn try_get_handle(&self) -> Result<AttHandleView<'a>, ParseError> {
        AttHandleView::try_parse(self.buf.offset(self.try_get_handle_offset()?)?.into())
    }
    #[inline]
    pub fn get_handle(&self) -> AttHandleView<'a> {
        self.try_get_handle().unwrap()
    }
    fn try_get_value(&self) -> Result<AttAttributeDataView<'a>, ParseError> {
        AttAttributeDataView::try_parse(
            self.buf
                .offset(self.try_get_value_offset()?)?
                .slice(
                    self.try_get_value_end_offset()?
                        .checked_sub(self.try_get_value_offset()?)
                        .ok_or(ParseError::ArithmeticOverflow)?,
                )?
                .into(),
        )
    }
    #[inline]
    pub fn get_value(&self) -> AttAttributeDataView<'a> {
        self.try_get_value().unwrap()
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_handle()?;
        self.try_get_value()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttReadByTypeDataElementView<'a> {
    type Parent = SizedBitSlice<'a>;
    type Owned = OwnedAttReadByTypeDataElementView;
    type Builder = AttReadByTypeDataElementBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttReadByTypeDataElementView {
        OwnedAttReadByTypeDataElementView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttReadByTypeDataElementView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttReadByTypeDataElementView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttReadByTypeDataElementView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttReadByTypeDataElementView {
    pub fn view<'a>(&'a self) -> AttReadByTypeDataElementView<'a> {
        AttReadByTypeDataElementView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttReadByTypeDataElementView> for AttReadByTypeDataElementView<'a> {
    fn from(x: &'a OwnedAttReadByTypeDataElementView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttReadByTypeDataElementBuilder {
    pub handle: AttHandleBuilder,
    pub value: AttAttributeDataBuilder,
}
impl Builder for AttReadByTypeDataElementBuilder {
    type OwnedPacket = OwnedAttReadByTypeDataElementView;
}
impl Serializable for AttReadByTypeDataElementBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.handle.serialize(writer)?;
        self.value.serialize(writer)?;
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttReadByTypeResponseView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> AttReadByTypeResponseView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_data_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_data_element_size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(8usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_data_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_custom_offset_1()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_data_count(&self) -> Result<usize, ParseError> {
        if self.try_get_data_element_size()? == 0
            || self.try_get_data_size()? % self.try_get_data_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_data_size()? / self.try_get_data_element_size()?)
    }
    fn try_get_data_iter(
        &self,
    ) -> Result<
        impl Iterator<Item = Result<AttReadByTypeDataElementView<'a>, ParseError>> + 'a,
        ParseError,
    > {
        let view = self.buf.offset(self.try_get_data_offset()?)?;
        let count = self.try_get_data_count()?;
        let element_size = self.try_get_data_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            AttReadByTypeDataElementView::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_data_iter(&self) -> impl Iterator<Item = AttReadByTypeDataElementView<'a>> + 'a {
        self.try_get_data_iter().unwrap().map(|x| x.unwrap())
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        for elem in self.try_get_data_iter()? {
            elem?;
        }
        Ok(())
    }
}
impl<'a> Packet<'a> for AttReadByTypeResponseView<'a> {
    type Parent = AttView<'a>;
    type Owned = OwnedAttReadByTypeResponseView;
    type Builder = AttReadByTypeResponseBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: AttView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttReadByTypeResponseView {
        OwnedAttReadByTypeResponseView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttReadByTypeResponseView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttReadByTypeResponseView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttReadByTypeResponseView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttReadByTypeResponseView {
    pub fn view<'a>(&'a self) -> AttReadByTypeResponseView<'a> {
        AttReadByTypeResponseView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttReadByTypeResponseView> for AttReadByTypeResponseView<'a> {
    fn from(x: &'a OwnedAttReadByTypeResponseView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttReadByTypeResponseBuilder {
    pub data: Box<[AttReadByTypeDataElementBuilder]>,
}
impl Builder for AttReadByTypeResponseBuilder {
    type OwnedPacket = OwnedAttReadByTypeResponseView;
}
impl Serializable for AttReadByTypeResponseBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        let get_element_size = || {
            Ok(if let Some(field) = self.data.get(0) {
                let size_in_bits = field.size_in_bits()?;
                if size_in_bits % 8 == 0 {
                    (size_in_bits / 8) as u64
                } else {
                    return Err(SerializeError::AlignmentError);
                }
            } else {
                0
            })
        };
        writer.write_bits(8usize, || get_element_size())?;
        let mut most_recent_array_size_in_bits = 0;
        for elem in self.data.iter() {
            most_recent_array_size_in_bits += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
impl From<AttReadByTypeResponseBuilder> for AttChild {
    fn from(x: AttReadByTypeResponseBuilder) -> Self {
        Self::AttReadByTypeResponse(x)
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttReadRequestView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> AttReadRequestView<'a> {
    #[inline]
    fn try_get_attribute_handle_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_attribute_handle(&self) -> Result<AttHandleView<'a>, ParseError> {
        AttHandleView::try_parse(self.buf.offset(self.try_get_attribute_handle_offset()?)?.into())
    }
    #[inline]
    pub fn get_attribute_handle(&self) -> AttHandleView<'a> {
        self.try_get_attribute_handle().unwrap()
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_attribute_handle()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttReadRequestView<'a> {
    type Parent = AttView<'a>;
    type Owned = OwnedAttReadRequestView;
    type Builder = AttReadRequestBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: AttView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttReadRequestView {
        OwnedAttReadRequestView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttReadRequestView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttReadRequestView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttReadRequestView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttReadRequestView {
    pub fn view<'a>(&'a self) -> AttReadRequestView<'a> {
        AttReadRequestView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttReadRequestView> for AttReadRequestView<'a> {
    fn from(x: &'a OwnedAttReadRequestView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttReadRequestBuilder {
    pub attribute_handle: AttHandleBuilder,
}
impl Builder for AttReadRequestBuilder {
    type OwnedPacket = OwnedAttReadRequestView;
}
impl Serializable for AttReadRequestBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.attribute_handle.serialize(writer)?;
        Ok(())
    }
}
impl From<AttReadRequestBuilder> for AttChild {
    fn from(x: AttReadRequestBuilder) -> Self {
        Self::AttReadRequest(x)
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttReadResponseView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> AttReadResponseView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_value_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_value_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_value_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_header_start_offset()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_value(&self) -> Result<AttAttributeDataView<'a>, ParseError> {
        AttAttributeDataView::try_parse(
            self.buf
                .offset(self.try_get_value_offset()?)?
                .slice(
                    self.try_get_value_end_offset()?
                        .checked_sub(self.try_get_value_offset()?)
                        .ok_or(ParseError::ArithmeticOverflow)?,
                )?
                .into(),
        )
    }
    #[inline]
    pub fn get_value(&self) -> AttAttributeDataView<'a> {
        self.try_get_value().unwrap()
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_value()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttReadResponseView<'a> {
    type Parent = AttView<'a>;
    type Owned = OwnedAttReadResponseView;
    type Builder = AttReadResponseBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: AttView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttReadResponseView {
        OwnedAttReadResponseView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttReadResponseView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttReadResponseView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttReadResponseView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttReadResponseView {
    pub fn view<'a>(&'a self) -> AttReadResponseView<'a> {
        AttReadResponseView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttReadResponseView> for AttReadResponseView<'a> {
    fn from(x: &'a OwnedAttReadResponseView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttReadResponseBuilder {
    pub value: AttAttributeDataBuilder,
}
impl Builder for AttReadResponseBuilder {
    type OwnedPacket = OwnedAttReadResponseView;
}
impl Serializable for AttReadResponseBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.value.serialize(writer)?;
        Ok(())
    }
}
impl From<AttReadResponseBuilder> for AttChild {
    fn from(x: AttReadResponseBuilder) -> Self {
        Self::AttReadResponse(x)
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttWriteRequestView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> AttWriteRequestView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_value_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_handle_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_value_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_value_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_custom_offset_1()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_handle(&self) -> Result<AttHandleView<'a>, ParseError> {
        AttHandleView::try_parse(self.buf.offset(self.try_get_handle_offset()?)?.into())
    }
    #[inline]
    pub fn get_handle(&self) -> AttHandleView<'a> {
        self.try_get_handle().unwrap()
    }
    fn try_get_value(&self) -> Result<AttAttributeDataView<'a>, ParseError> {
        AttAttributeDataView::try_parse(
            self.buf
                .offset(self.try_get_value_offset()?)?
                .slice(
                    self.try_get_value_end_offset()?
                        .checked_sub(self.try_get_value_offset()?)
                        .ok_or(ParseError::ArithmeticOverflow)?,
                )?
                .into(),
        )
    }
    #[inline]
    pub fn get_value(&self) -> AttAttributeDataView<'a> {
        self.try_get_value().unwrap()
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_handle()?;
        self.try_get_value()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttWriteRequestView<'a> {
    type Parent = AttView<'a>;
    type Owned = OwnedAttWriteRequestView;
    type Builder = AttWriteRequestBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: AttView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttWriteRequestView {
        OwnedAttWriteRequestView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttWriteRequestView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttWriteRequestView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttWriteRequestView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttWriteRequestView {
    pub fn view<'a>(&'a self) -> AttWriteRequestView<'a> {
        AttWriteRequestView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttWriteRequestView> for AttWriteRequestView<'a> {
    fn from(x: &'a OwnedAttWriteRequestView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttWriteRequestBuilder {
    pub handle: AttHandleBuilder,
    pub value: AttAttributeDataBuilder,
}
impl Builder for AttWriteRequestBuilder {
    type OwnedPacket = OwnedAttWriteRequestView;
}
impl Serializable for AttWriteRequestBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.handle.serialize(writer)?;
        self.value.serialize(writer)?;
        Ok(())
    }
}
impl From<AttWriteRequestBuilder> for AttChild {
    fn from(x: AttWriteRequestBuilder) -> Self {
        Self::AttWriteRequest(x)
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttWriteResponseView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> AttWriteResponseView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        Ok(())
    }
}
impl<'a> Packet<'a> for AttWriteResponseView<'a> {
    type Parent = AttView<'a>;
    type Owned = OwnedAttWriteResponseView;
    type Builder = AttWriteResponseBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: AttView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttWriteResponseView {
        OwnedAttWriteResponseView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttWriteResponseView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttWriteResponseView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttWriteResponseView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttWriteResponseView {
    pub fn view<'a>(&'a self) -> AttWriteResponseView<'a> {
        AttWriteResponseView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttWriteResponseView> for AttWriteResponseView<'a> {
    fn from(x: &'a OwnedAttWriteResponseView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttWriteResponseBuilder {}
impl Builder for AttWriteResponseBuilder {
    type OwnedPacket = OwnedAttWriteResponseView;
}
impl Serializable for AttWriteResponseBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        Ok(())
    }
}
impl From<AttWriteResponseBuilder> for AttChild {
    fn from(x: AttWriteResponseBuilder) -> Self {
        Self::AttWriteResponse(x)
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AttErrorResponseView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> AttErrorResponseView<'a> {
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(32i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_opcode_in_error_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_handle_in_error_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_error_code_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_opcode_in_error(&self) -> Result<AttOpcode, ParseError> {
        AttOpcode::try_parse(self.buf.offset(self.try_get_opcode_in_error_offset()?)?.into())
    }
    #[inline]
    pub fn get_opcode_in_error(&self) -> AttOpcode {
        self.try_get_opcode_in_error().unwrap()
    }
    fn try_get_handle_in_error(&self) -> Result<AttHandleView<'a>, ParseError> {
        AttHandleView::try_parse(self.buf.offset(self.try_get_handle_in_error_offset()?)?.into())
    }
    #[inline]
    pub fn get_handle_in_error(&self) -> AttHandleView<'a> {
        self.try_get_handle_in_error().unwrap()
    }
    fn try_get_error_code(&self) -> Result<AttErrorCode, ParseError> {
        AttErrorCode::try_parse(self.buf.offset(self.try_get_error_code_offset()?)?.into())
    }
    #[inline]
    pub fn get_error_code(&self) -> AttErrorCode {
        self.try_get_error_code().unwrap()
    }
    #[inline]
    fn try_get_header_start_offset(&self) -> Result<usize, ParseError> {
        Ok(0)
    }
    #[inline]
    fn try_get_size(&self) -> Result<usize, ParseError> {
        let size = self.try_get_packet_end_offset()?;
        if size % 8 != 0 {
            return Err(ParseError::MisalignedPayload);
        }
        Ok(size / 8)
    }
    fn validate(&self) -> Result<(), ParseError> {
        self.try_get_opcode_in_error()?;
        self.try_get_handle_in_error()?;
        self.try_get_error_code()?;
        Ok(())
    }
}
impl<'a> Packet<'a> for AttErrorResponseView<'a> {
    type Parent = AttView<'a>;
    type Owned = OwnedAttErrorResponseView;
    type Builder = AttErrorResponseBuilder;
    fn try_parse_from_buffer(buf: impl Into<SizedBitSlice<'a>>) -> Result<Self, ParseError> {
        let out = Self { buf: buf.into().into() };
        out.validate()?;
        Ok(out)
    }
    fn try_parse(parent: AttView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
    fn to_owned_packet(&self) -> OwnedAttErrorResponseView {
        OwnedAttErrorResponseView {
            buf: self.buf.backing.to_owned().into(),
            start_bit_offset: self.buf.start_bit_offset,
            end_bit_offset: self.buf.end_bit_offset,
        }
    }
}
#[derive(Debug)]
pub struct OwnedAttErrorResponseView {
    buf: Box<[u8]>,
    start_bit_offset: usize,
    end_bit_offset: usize,
}
impl OwnedPacket for OwnedAttErrorResponseView {
    fn try_parse(buf: Box<[u8]>) -> Result<Self, ParseError> {
        AttErrorResponseView::try_parse_from_buffer(&buf[..])?;
        let end_bit_offset = buf.len() * 8;
        Ok(Self { buf, start_bit_offset: 0, end_bit_offset })
    }
}
impl OwnedAttErrorResponseView {
    pub fn view<'a>(&'a self) -> AttErrorResponseView<'a> {
        AttErrorResponseView {
            buf: SizedBitSlice(BitSlice {
                backing: &self.buf[..],
                start_bit_offset: self.start_bit_offset,
                end_bit_offset: self.end_bit_offset,
            })
            .into(),
        }
    }
}
impl<'a> From<&'a OwnedAttErrorResponseView> for AttErrorResponseView<'a> {
    fn from(x: &'a OwnedAttErrorResponseView) -> Self {
        x.view()
    }
}
#[derive(Debug, Clone)]
pub struct AttErrorResponseBuilder {
    pub opcode_in_error: AttOpcode,
    pub handle_in_error: AttHandleBuilder,
    pub error_code: AttErrorCode,
}
impl Builder for AttErrorResponseBuilder {
    type OwnedPacket = OwnedAttErrorResponseView;
}
impl Serializable for AttErrorResponseBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.opcode_in_error.serialize(writer)?;
        self.handle_in_error.serialize(writer)?;
        self.error_code.serialize(writer)?;
        Ok(())
    }
}
impl From<AttErrorResponseBuilder> for AttChild {
    fn from(x: AttErrorResponseBuilder) -> Self {
        Self::AttErrorResponse(x)
    }
}
