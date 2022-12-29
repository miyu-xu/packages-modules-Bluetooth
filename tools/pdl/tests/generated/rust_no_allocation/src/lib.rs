// We inherit casing from the PDL file
#![allow(non_snake_case)]
#![allow(non_camel_case_types)]
#![allow(warnings, missing_docs)]
#![allow(clippy::all)]
// this is now stable
#![feature(mixed_integer_ops)]

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
        self.backing
            .get(index)
            .ok_or_else(|| panic!("eek (index={index}, backing={:?})", self.backing))
            .copied()
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
            println!(
                "reading value {accumulator} out of buffer {:?} [start={}, end={}]",
                self.backing, self.start_bit_offset, self.end_bit_offset
            );
            Ok(accumulator)
        } else {
            return Err(ParseError::MisalignedPayload);
        }
    }

    pub fn get_size_in_bits(&self) -> usize {
        self.end_bit_offset - self.start_bit_offset
    }
}

#[derive(Debug)]
pub enum SerializeError {
    NegativePadding,
    IntegerConversionFailure,
}

trait BitWriter {
    fn write_bits(
        &mut self,
        num_bits: usize,
        gen_contents: impl FnOnce() -> Result<u64, SerializeError>,
    ) -> Result<(), SerializeError>;
}

trait Serializable {
    fn size_in_bits(&self) -> Result<usize, SerializeError> {
        let mut sizer = Sizer::new();
        self.serialize(&mut sizer)?;
        Ok(sizer.size())
    }

    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError>;
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
#[derive(Copy, Clone, PartialEq, Eq, Debug)]
pub enum Enum7 {
    A,
    B,
}
impl Enum7 {
    fn try_parse(buf: BitSlice) -> Result<Self, ParseError> {
        let value = buf.slice(7usize)?.try_parse()?;
        match value {
            1u64 => Ok(Self::A),
            2u64 => Ok(Self::B),
            _ => Err(ParseError::InvalidEnumValue),
        }
    }
    fn value(&self) -> u64 {
        match self {
            Self::A => 1u64,
            Self::B => 2u64,
        }
    }
}
impl Serializable for Enum7 {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(7usize, || Ok(self.value()));
        Ok(())
    }
}
impl From<Enum7> for u64 {
    fn from(x: Enum7) -> u64 {
        x.value()
    }
}
#[derive(Copy, Clone, PartialEq, Eq, Debug)]
pub enum Enum16 {
    A,
    B,
}
impl Enum16 {
    fn try_parse(buf: BitSlice) -> Result<Self, ParseError> {
        let value = buf.slice(16usize)?.try_parse()?;
        match value {
            43707u64 => Ok(Self::A),
            52445u64 => Ok(Self::B),
            _ => Err(ParseError::InvalidEnumValue),
        }
    }
    fn value(&self) -> u64 {
        match self {
            Self::A => 43707u64,
            Self::B => 52445u64,
        }
    }
}
impl Serializable for Enum16 {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(16usize, || Ok(self.value()));
        Ok(())
    }
}
impl From<Enum16> for u64 {
    fn from(x: Enum16) -> u64 {
        x.value()
    }
}
#[derive(Clone, Copy, Debug)]
pub struct SizedStructView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> SizedStructView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_a(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_a_offset()?)?.slice(8usize)?.try_parse()
    }
    #[inline]
    pub fn get_a(&self) -> u64 {
        self.try_get_a().unwrap()
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
        self.try_get_a()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct SizedStructBuilder {
    a: u64,
}
impl Serializable for SizedStructBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(8usize, || Ok(self.a))?;
        Ok(())
    }
}
enum SizedStructChild {
    RawData(Box<[u8]>),
}
impl Serializable for SizedStructChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct UnsizedStructView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> UnsizedStructView<'a> {
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(2i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(6i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(2usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        if self.try_get_array_element_size()? == 0
            || self.try_get_array_size()? % self.try_get_array_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_array_size()? / self.try_get_array_element_size()?)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct UnsizedStructBuilder {
    array: Box<[u64]>,
}
impl Serializable for UnsizedStructBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(2usize, || {
            (self.array.len() * 1usize).try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(6usize, || Ok(0u64))?;
        for elem in self.array.iter() {
            writer.write_bits(8usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 8usize * self.array.len();
        Ok(())
    }
}
enum UnsizedStructChild {
    RawData(Box<[u8]>),
}
impl Serializable for UnsizedStructChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct ScalarParentView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> ScalarParentView<'a> {
    #[inline]
    fn try_get__payload__end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get__payload__size()?
                    .checked_mul(8)
                    .ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
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
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get__payload__size()?
                    .checked_mul(8)
                    .ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get__payload__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get__payload__size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_custom_offset_1()?)?.slice(8usize)?.try_parse()? as usize)
    }
    fn try_get_a(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_a_offset()?)?.slice(8usize)?.try_parse()
    }
    #[inline]
    pub fn get_a(&self) -> u64 {
        self.try_get_a().unwrap()
    }
    fn try_get_payload(&self) -> Result<SizedBitSlice<'a>, ParseError> {
        let payload_start_offset = self.try_get__payload__offset()?;
        let payload_end_offset = self.try_get__payload__end_offset()?;
        self.buf.offset(payload_start_offset)?.slice(payload_end_offset - payload_start_offset)
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
        self.try_get_a()?;
        self.try_get_payload()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct ScalarParentBuilder {
    a: u64,
    _child_: ScalarParentChild,
}
impl Serializable for ScalarParentBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(8usize, || Ok(self.a))?;
        writer.write_bits(8usize, || {
            self._child_
                .size_in_bits()?
                .try_into()
                .or(Err(SerializeError::IntegerConversionFailure))
        })?;
        self._child_.serialize(writer)?;
        Ok(())
    }
}
enum ScalarParentChild {
    RawData(Box<[u8]>),
    EmptyParent(EmptyParentBuilder),
    ScalarChild_A(ScalarChild_ABuilder),
    ScalarChild_B(ScalarChild_BBuilder),
}
impl Serializable for ScalarParentChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
            Self::EmptyParent(x) => {
                x.serialize(writer)?;
            }
            Self::ScalarChild_A(x) => {
                x.serialize(writer)?;
            }
            Self::ScalarChild_B(x) => {
                x.serialize(writer)?;
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct EnumParentView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> EnumParentView<'a> {
    #[inline]
    fn try_get__payload__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get__payload__end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get__payload__size()?
                    .checked_mul(8)
                    .ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get__payload__size()?
                    .checked_mul(8)
                    .ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get__payload__size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_custom_offset_1()?)?.slice(8usize)?.try_parse()? as usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_a(&self) -> Result<Enum16, ParseError> {
        Enum16::try_parse(self.buf.offset(self.try_get_a_offset()?)?.into())
    }
    #[inline]
    pub fn get_a(&self) -> Enum16 {
        self.try_get_a().unwrap()
    }
    fn try_get_payload(&self) -> Result<SizedBitSlice<'a>, ParseError> {
        let payload_start_offset = self.try_get__payload__offset()?;
        let payload_end_offset = self.try_get__payload__end_offset()?;
        self.buf.offset(payload_start_offset)?.slice(payload_end_offset - payload_start_offset)
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
        self.try_get_a()?;
        self.try_get_payload()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct EnumParentBuilder {
    a: Enum16,
    _child_: EnumParentChild,
}
impl Serializable for EnumParentBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.a.serialize(writer)?;
        writer.write_bits(8usize, || {
            self._child_
                .size_in_bits()?
                .try_into()
                .or(Err(SerializeError::IntegerConversionFailure))
        })?;
        self._child_.serialize(writer)?;
        Ok(())
    }
}
enum EnumParentChild {
    RawData(Box<[u8]>),
    EnumChild_A(EnumChild_ABuilder),
    EnumChild_B(EnumChild_BBuilder),
}
impl Serializable for EnumParentChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
            Self::EnumChild_A(x) => {
                x.serialize(writer)?;
            }
            Self::EnumChild_B(x) => {
                x.serialize(writer)?;
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct EmptyParentView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> EmptyParentView<'a> {
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
    fn try_get__payload__end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get__payload__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
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
        Ok(())
    }
    pub fn try_parse<'b>(parent: ScalarParentView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
}
pub struct EmptyParentBuilder {
    _child_: EmptyParentChild,
}
impl Serializable for EmptyParentBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self._child_.serialize(writer)?;
        Ok(())
    }
}
enum EmptyParentChild {
    RawData(Box<[u8]>),
    AliasedChild_A(AliasedChild_ABuilder),
    AliasedChild_B(AliasedChild_BBuilder),
}
impl Serializable for EmptyParentChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
            Self::AliasedChild_A(x) => {
                x.serialize(writer)?;
            }
            Self::AliasedChild_B(x) => {
                x.serialize(writer)?;
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct PartialParent5View<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> PartialParent5View<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get__payload__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(5i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
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
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_a(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_a_offset()?)?.slice(5usize)?.try_parse()
    }
    #[inline]
    pub fn get_a(&self) -> u64 {
        self.try_get_a().unwrap()
    }
    fn try_get_payload(&self) -> Result<SizedBitSlice<'a>, ParseError> {
        let payload_start_offset = self.try_get__payload__offset()?;
        let payload_end_offset = self.try_get__payload__end_offset()?;
        self.buf.offset(payload_start_offset)?.slice(payload_end_offset - payload_start_offset)
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
        self.try_get_a()?;
        self.try_get_payload()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct PartialParent5Builder {
    a: u64,
    _child_: PartialParent5Child,
}
impl Serializable for PartialParent5Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(5usize, || Ok(self.a))?;
        self._child_.serialize(writer)?;
        Ok(())
    }
}
enum PartialParent5Child {
    RawData(Box<[u8]>),
    PartialChild5_A(PartialChild5_ABuilder),
    PartialChild5_B(PartialChild5_BBuilder),
}
impl Serializable for PartialParent5Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
            Self::PartialChild5_A(x) => {
                x.serialize(writer)?;
            }
            Self::PartialChild5_B(x) => {
                x.serialize(writer)?;
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct PartialParent12View<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> PartialParent12View<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get__payload__end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(12i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get__payload__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
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
    fn try_get_a(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_a_offset()?)?.slice(12usize)?.try_parse()
    }
    #[inline]
    pub fn get_a(&self) -> u64 {
        self.try_get_a().unwrap()
    }
    fn try_get_payload(&self) -> Result<SizedBitSlice<'a>, ParseError> {
        let payload_start_offset = self.try_get__payload__offset()?;
        let payload_end_offset = self.try_get__payload__end_offset()?;
        self.buf.offset(payload_start_offset)?.slice(payload_end_offset - payload_start_offset)
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
        self.try_get_a()?;
        self.try_get_payload()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct PartialParent12Builder {
    a: u64,
    _child_: PartialParent12Child,
}
impl Serializable for PartialParent12Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(12usize, || Ok(self.a))?;
        self._child_.serialize(writer)?;
        Ok(())
    }
}
enum PartialParent12Child {
    RawData(Box<[u8]>),
    PartialChild12_A(PartialChild12_ABuilder),
    PartialChild12_B(PartialChild12_BBuilder),
}
impl Serializable for PartialParent12Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
            Self::PartialChild12_A(x) => {
                x.serialize(writer)?;
            }
            Self::PartialChild12_B(x) => {
                x.serialize(writer)?;
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Scalar_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Scalar_FieldView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(7i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_c_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(57i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_a(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_a_offset()?)?.slice(7usize)?.try_parse()
    }
    #[inline]
    pub fn get_a(&self) -> u64 {
        self.try_get_a().unwrap()
    }
    fn try_get_c(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_c_offset()?)?.slice(57usize)?.try_parse()
    }
    #[inline]
    pub fn get_c(&self) -> u64 {
        self.try_get_c().unwrap()
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
        self.try_get_a()?;
        self.try_get_c()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Scalar_FieldBuilder {
    a: u64,
    c: u64,
}
impl Serializable for Packet_Scalar_FieldBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(7usize, || Ok(self.a))?;
        writer.write_bits(57usize, || Ok(self.c))?;
        Ok(())
    }
}
enum Packet_Scalar_FieldChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Scalar_FieldChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Enum_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Enum_FieldView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(7i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_c_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(57i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_a(&self) -> Result<Enum7, ParseError> {
        Enum7::try_parse(self.buf.offset(self.try_get_a_offset()?)?.into())
    }
    #[inline]
    pub fn get_a(&self) -> Enum7 {
        self.try_get_a().unwrap()
    }
    fn try_get_c(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_c_offset()?)?.slice(57usize)?.try_parse()
    }
    #[inline]
    pub fn get_c(&self) -> u64 {
        self.try_get_c().unwrap()
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
        self.try_get_a()?;
        self.try_get_c()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Enum_FieldBuilder {
    a: Enum7,
    c: u64,
}
impl Serializable for Packet_Enum_FieldBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.a.serialize(writer)?;
        writer.write_bits(57usize, || Ok(self.c))?;
        Ok(())
    }
}
enum Packet_Enum_FieldChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Enum_FieldChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Reserved_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Reserved_FieldView<'a> {
    #[inline]
    fn try_get_c_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(7i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add_signed(55i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(2i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_a(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_a_offset()?)?.slice(7usize)?.try_parse()
    }
    #[inline]
    pub fn get_a(&self) -> u64 {
        self.try_get_a().unwrap()
    }
    fn try_get_c(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_c_offset()?)?.slice(55usize)?.try_parse()
    }
    #[inline]
    pub fn get_c(&self) -> u64 {
        self.try_get_c().unwrap()
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
        self.try_get_a()?;
        self.try_get_c()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Reserved_FieldBuilder {
    a: u64,
    c: u64,
}
impl Serializable for Packet_Reserved_FieldBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(7usize, || Ok(self.a))?;
        writer.write_bits(2usize, || Ok(0u64))?;
        writer.write_bits(55usize, || Ok(self.c))?;
        Ok(())
    }
}
enum Packet_Reserved_FieldChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Reserved_FieldChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Size_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Size_FieldView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(3i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_b_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(61i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_b_size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(3usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_b_count(&self) -> Result<usize, ParseError> {
        if self.try_get_b_element_size()? == 0
            || self.try_get_b_size()? % self.try_get_b_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_b_size()? / self.try_get_b_element_size()?)
    }
    #[inline]
    fn try_get_b_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_a(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_a_offset()?)?.slice(61usize)?.try_parse()
    }
    #[inline]
    pub fn get_a(&self) -> u64 {
        self.try_get_a().unwrap()
    }
    fn try_get_b_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_b_offset()?)?;
        let count = self.try_get_b_count()?;
        let element_size = self.try_get_b_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_b_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_b_iter().unwrap().map(|x| x.unwrap())
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
        self.try_get_a()?;
        for elem in self.try_get_b_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Size_FieldBuilder {
    a: u64,
    b: Box<[u64]>,
}
impl Serializable for Packet_Size_FieldBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(3usize, || {
            (self.b.len() * 1usize).try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(61usize, || Ok(self.a))?;
        for elem in self.b.iter() {
            writer.write_bits(8usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 8usize * self.b.len();
        Ok(())
    }
}
enum Packet_Size_FieldChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Size_FieldChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Count_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Count_FieldView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(61i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_b_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(3i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_b_size(&self) -> Result<usize, ParseError> {
        self.try_get_b_count()?
            .checked_mul(self.try_get_b_element_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_b_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_b_count(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(3usize)?.try_parse()?
            as usize)
    }
    fn try_get_a(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_a_offset()?)?.slice(61usize)?.try_parse()
    }
    #[inline]
    pub fn get_a(&self) -> u64 {
        self.try_get_a().unwrap()
    }
    fn try_get_b_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_b_offset()?)?;
        let count = self.try_get_b_count()?;
        let element_size = self.try_get_b_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_b_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_b_iter().unwrap().map(|x| x.unwrap())
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
        self.try_get_a()?;
        for elem in self.try_get_b_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Count_FieldBuilder {
    a: u64,
    b: Box<[u64]>,
}
impl Serializable for Packet_Count_FieldBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(3usize, || {
            self.b.len().try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(61usize, || Ok(self.a))?;
        for elem in self.b.iter() {
            writer.write_bits(8usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 8usize * self.b.len();
        Ok(())
    }
}
enum Packet_Count_FieldChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Count_FieldChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_FixedScalar_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_FixedScalar_FieldView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(7i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(57i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_b(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_b_offset()?)?.slice(57usize)?.try_parse()
    }
    #[inline]
    pub fn get_b(&self) -> u64 {
        self.try_get_b().unwrap()
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
        self.try_get_b()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_FixedScalar_FieldBuilder {
    b: u64,
}
impl Serializable for Packet_FixedScalar_FieldBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(7usize, || Ok(7u64))?;
        writer.write_bits(57usize, || Ok(self.b))?;
        Ok(())
    }
}
enum Packet_FixedScalar_FieldChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_FixedScalar_FieldChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_FixedEnum_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_FixedEnum_FieldView<'a> {
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(57i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(7i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_b(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_b_offset()?)?.slice(57usize)?.try_parse()
    }
    #[inline]
    pub fn get_b(&self) -> u64 {
        self.try_get_b().unwrap()
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
        self.try_get_b()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_FixedEnum_FieldBuilder {
    b: u64,
}
impl Serializable for Packet_FixedEnum_FieldBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(7usize, || Ok(Enum7::A.value()))?;
        writer.write_bits(57usize, || Ok(self.b))?;
        Ok(())
    }
}
enum Packet_FixedEnum_FieldChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_FixedEnum_FieldChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Payload_Field_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Payload_Field_VariableSizeView<'a> {
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(5i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get__payload__end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get__payload__size()?
                    .checked_mul(8)
                    .ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get__payload__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get__payload__size()?
                    .checked_mul(8)
                    .ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(3i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get__payload__size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(3usize)?.try_parse()?
            as usize)
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
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Payload_Field_VariableSizeBuilder {
    _child_: Packet_Payload_Field_VariableSizeChild,
}
impl Serializable for Packet_Payload_Field_VariableSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(3usize, || {
            self._child_
                .size_in_bits()?
                .try_into()
                .or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(5usize, || Ok(0u64))?;
        self._child_.serialize(writer)?;
        Ok(())
    }
}
enum Packet_Payload_Field_VariableSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Payload_Field_VariableSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Payload_Field_UnknownSizeView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> Packet_Payload_Field_UnknownSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get__payload__end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(-16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get__payload__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
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
    fn try_get_a(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_a_offset()?)?.slice(16usize)?.try_parse()
    }
    #[inline]
    pub fn get_a(&self) -> u64 {
        self.try_get_a().unwrap()
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
        self.try_get_a()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Payload_Field_UnknownSizeBuilder {
    _child_: Packet_Payload_Field_UnknownSizeChild,
    a: u64,
}
impl Serializable for Packet_Payload_Field_UnknownSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self._child_.serialize(writer)?;
        writer.write_bits(16usize, || Ok(self.a))?;
        Ok(())
    }
}
enum Packet_Payload_Field_UnknownSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Payload_Field_UnknownSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Payload_Field_UnknownSize_TerminalView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> Packet_Payload_Field_UnknownSize_TerminalView<'a> {
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
    fn try_get__payload__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get__payload__end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_a(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_a_offset()?)?.slice(16usize)?.try_parse()
    }
    #[inline]
    pub fn get_a(&self) -> u64 {
        self.try_get_a().unwrap()
    }
    fn try_get_payload(&self) -> Result<SizedBitSlice<'a>, ParseError> {
        let payload_start_offset = self.try_get__payload__offset()?;
        let payload_end_offset = self.try_get__payload__end_offset()?;
        self.buf.offset(payload_start_offset)?.slice(payload_end_offset - payload_start_offset)
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
        self.try_get_a()?;
        self.try_get_payload()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Payload_Field_UnknownSize_TerminalBuilder {
    a: u64,
    _child_: Packet_Payload_Field_UnknownSize_TerminalChild,
}
impl Serializable for Packet_Payload_Field_UnknownSize_TerminalBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(16usize, || Ok(self.a))?;
        self._child_.serialize(writer)?;
        Ok(())
    }
}
enum Packet_Payload_Field_UnknownSize_TerminalChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Payload_Field_UnknownSize_TerminalChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Body_Field_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Body_Field_VariableSizeView<'a> {
    #[inline]
    fn try_get__body__end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get__body__size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(3i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(5i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get__body__size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get__body__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get__body__size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(3usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_payload(&self) -> Result<SizedBitSlice<'a>, ParseError> {
        let payload_start_offset = self.try_get__body__offset()?;
        let payload_end_offset = self.try_get__body__end_offset()?;
        self.buf.offset(payload_start_offset)?.slice(payload_end_offset - payload_start_offset)
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
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Body_Field_VariableSizeBuilder {
    _child_: Packet_Body_Field_VariableSizeChild,
}
impl Serializable for Packet_Body_Field_VariableSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(3usize, || {
            self._child_
                .size_in_bits()?
                .try_into()
                .or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(5usize, || Ok(0u64))?;
        self._child_.serialize(writer)?;
        Ok(())
    }
}
enum Packet_Body_Field_VariableSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Body_Field_VariableSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Body_Field_UnknownSizeView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> Packet_Body_Field_UnknownSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get__body__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(-16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get__body__end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_payload(&self) -> Result<SizedBitSlice<'a>, ParseError> {
        let payload_start_offset = self.try_get__body__offset()?;
        let payload_end_offset = self.try_get__body__end_offset()?;
        self.buf.offset(payload_start_offset)?.slice(payload_end_offset - payload_start_offset)
    }
    fn try_get_a(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_a_offset()?)?.slice(16usize)?.try_parse()
    }
    #[inline]
    pub fn get_a(&self) -> u64 {
        self.try_get_a().unwrap()
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
        self.try_get_a()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Body_Field_UnknownSizeBuilder {
    _child_: Packet_Body_Field_UnknownSizeChild,
    a: u64,
}
impl Serializable for Packet_Body_Field_UnknownSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self._child_.serialize(writer)?;
        writer.write_bits(16usize, || Ok(self.a))?;
        Ok(())
    }
}
enum Packet_Body_Field_UnknownSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Body_Field_UnknownSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Body_Field_UnknownSize_TerminalView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> Packet_Body_Field_UnknownSize_TerminalView<'a> {
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
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get__payload__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_a(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_a_offset()?)?.slice(16usize)?.try_parse()
    }
    #[inline]
    pub fn get_a(&self) -> u64 {
        self.try_get_a().unwrap()
    }
    fn try_get_payload(&self) -> Result<SizedBitSlice<'a>, ParseError> {
        let payload_start_offset = self.try_get__payload__offset()?;
        let payload_end_offset = self.try_get__payload__end_offset()?;
        self.buf.offset(payload_start_offset)?.slice(payload_end_offset - payload_start_offset)
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
        self.try_get_a()?;
        self.try_get_payload()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Body_Field_UnknownSize_TerminalBuilder {
    a: u64,
    _child_: Packet_Body_Field_UnknownSize_TerminalChild,
}
impl Serializable for Packet_Body_Field_UnknownSize_TerminalBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(16usize, || Ok(self.a))?;
        self._child_.serialize(writer)?;
        Ok(())
    }
}
enum Packet_Body_Field_UnknownSize_TerminalChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Body_Field_UnknownSize_TerminalChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Struct_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Struct_FieldView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add(
                self.try_get_b_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_b_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_custom_offset_1()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    fn try_get_a(&self) -> Result<SizedStructView<'a>, ParseError> {
        SizedStructView::try_parse(self.buf.offset(self.try_get_a_offset()?)?.into())
    }
    #[inline]
    pub fn get_a(&self) -> SizedStructView<'a> {
        self.try_get_a().unwrap()
    }
    fn try_get_b(&self) -> Result<UnsizedStructView<'a>, ParseError> {
        UnsizedStructView::try_parse(self.buf.offset(self.try_get_b_offset()?)?.into())
    }
    #[inline]
    pub fn get_b(&self) -> UnsizedStructView<'a> {
        self.try_get_b().unwrap()
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
        self.try_get_a()?;
        self.try_get_b()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Struct_FieldBuilder {
    a: SizedStructBuilder,
    b: UnsizedStructBuilder,
}
impl Serializable for Packet_Struct_FieldBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.a.serialize(writer)?;
        self.b.serialize(writer)?;
        Ok(())
    }
}
enum Packet_Struct_FieldChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Struct_FieldChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_ByteElement_ConstantSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_ByteElement_ConstantSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(32i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(32i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_ByteElement_ConstantSizeBuilder {
    array: Box<[u64]>,
}
impl Serializable for Packet_Array_Field_ByteElement_ConstantSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        for elem in self.array.iter() {
            writer.write_bits(8usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 8usize * self.array.len();
        Ok(())
    }
}
enum Packet_Array_Field_ByteElement_ConstantSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_ByteElement_ConstantSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_ByteElement_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_ByteElement_VariableSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        if self.try_get_array_element_size()? == 0
            || self.try_get_array_size()? % self.try_get_array_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_array_size()? / self.try_get_array_element_size()?)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_ByteElement_VariableSizeBuilder {
    array: Box<[u64]>,
}
impl Serializable for Packet_Array_Field_ByteElement_VariableSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            (self.array.len() * 1usize).try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        for elem in self.array.iter() {
            writer.write_bits(8usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 8usize * self.array.len();
        Ok(())
    }
}
enum Packet_Array_Field_ByteElement_VariableSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_ByteElement_VariableSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_ByteElement_VariableCountView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_ByteElement_VariableCountView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        self.try_get_array_count()?
            .checked_mul(self.try_get_array_element_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_ByteElement_VariableCountBuilder {
    array: Box<[u64]>,
}
impl Serializable for Packet_Array_Field_ByteElement_VariableCountBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            self.array.len().try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        for elem in self.array.iter() {
            writer.write_bits(8usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 8usize * self.array.len();
        Ok(())
    }
}
enum Packet_Array_Field_ByteElement_VariableCountChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_ByteElement_VariableCountChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_ByteElement_UnknownSizeView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> Packet_Array_Field_ByteElement_UnknownSizeView<'a> {
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
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        if self.try_get_array_element_size()? == 0
            || self.try_get_array_size()? % self.try_get_array_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_array_size()? / self.try_get_array_element_size()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_header_start_offset()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_ByteElement_UnknownSizeBuilder {
    array: Box<[u64]>,
}
impl Serializable for Packet_Array_Field_ByteElement_UnknownSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        for elem in self.array.iter() {
            writer.write_bits(8usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 8usize * self.array.len();
        Ok(())
    }
}
enum Packet_Array_Field_ByteElement_UnknownSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_ByteElement_UnknownSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_ScalarElement_ConstantSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_ScalarElement_ConstantSizeView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(8usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_ScalarElement_ConstantSizeBuilder {
    array: Box<[u64]>,
}
impl Serializable for Packet_Array_Field_ScalarElement_ConstantSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        for elem in self.array.iter() {
            writer.write_bits(16usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 16usize * self.array.len();
        Ok(())
    }
}
enum Packet_Array_Field_ScalarElement_ConstantSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_ScalarElement_ConstantSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_ScalarElement_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_ScalarElement_VariableSizeView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        if self.try_get_array_element_size()? == 0
            || self.try_get_array_size()? % self.try_get_array_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_array_size()? / self.try_get_array_element_size()?)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_ScalarElement_VariableSizeBuilder {
    array: Box<[u64]>,
}
impl Serializable for Packet_Array_Field_ScalarElement_VariableSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            (self.array.len() * 2usize).try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        for elem in self.array.iter() {
            writer.write_bits(16usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 16usize * self.array.len();
        Ok(())
    }
}
enum Packet_Array_Field_ScalarElement_VariableSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_ScalarElement_VariableSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_ScalarElement_VariableCountView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_ScalarElement_VariableCountView<'a> {
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        self.try_get_array_count()?
            .checked_mul(self.try_get_array_element_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_ScalarElement_VariableCountBuilder {
    array: Box<[u64]>,
}
impl Serializable for Packet_Array_Field_ScalarElement_VariableCountBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            self.array.len().try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        for elem in self.array.iter() {
            writer.write_bits(16usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 16usize * self.array.len();
        Ok(())
    }
}
enum Packet_Array_Field_ScalarElement_VariableCountChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_ScalarElement_VariableCountChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_ScalarElement_UnknownSizeView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> Packet_Array_Field_ScalarElement_UnknownSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
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
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        if self.try_get_array_element_size()? == 0
            || self.try_get_array_size()? % self.try_get_array_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_array_size()? / self.try_get_array_element_size()?)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_ScalarElement_UnknownSizeBuilder {
    array: Box<[u64]>,
}
impl Serializable for Packet_Array_Field_ScalarElement_UnknownSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        for elem in self.array.iter() {
            writer.write_bits(16usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 16usize * self.array.len();
        Ok(())
    }
}
enum Packet_Array_Field_ScalarElement_UnknownSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_ScalarElement_UnknownSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_EnumElement_ConstantSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_EnumElement_ConstantSizeView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(8usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<Enum16, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            Enum16::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = Enum16> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_EnumElement_ConstantSizeBuilder {
    array: Box<[Enum16]>,
}
impl Serializable for Packet_Array_Field_EnumElement_ConstantSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Packet_Array_Field_EnumElement_ConstantSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_EnumElement_ConstantSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_EnumElement_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_EnumElement_VariableSizeView<'a> {
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        if self.try_get_array_element_size()? == 0
            || self.try_get_array_size()? % self.try_get_array_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_array_size()? / self.try_get_array_element_size()?)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<Enum16, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            Enum16::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = Enum16> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_EnumElement_VariableSizeBuilder {
    array: Box<[Enum16]>,
}
impl Serializable for Packet_Array_Field_EnumElement_VariableSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            (self.array.len() * 2usize).try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Packet_Array_Field_EnumElement_VariableSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_EnumElement_VariableSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_EnumElement_VariableCountView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_EnumElement_VariableCountView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        self.try_get_array_count()?
            .checked_mul(self.try_get_array_element_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<Enum16, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            Enum16::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = Enum16> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_EnumElement_VariableCountBuilder {
    array: Box<[Enum16]>,
}
impl Serializable for Packet_Array_Field_EnumElement_VariableCountBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            self.array.len().try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Packet_Array_Field_EnumElement_VariableCountChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_EnumElement_VariableCountChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_EnumElement_UnknownSizeView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> Packet_Array_Field_EnumElement_UnknownSizeView<'a> {
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
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
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
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        if self.try_get_array_element_size()? == 0
            || self.try_get_array_size()? % self.try_get_array_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_array_size()? / self.try_get_array_element_size()?)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<Enum16, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            Enum16::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = Enum16> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_EnumElement_UnknownSizeBuilder {
    array: Box<[Enum16]>,
}
impl Serializable for Packet_Array_Field_EnumElement_UnknownSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Packet_Array_Field_EnumElement_UnknownSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_EnumElement_UnknownSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_SizedElement_ConstantSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_SizedElement_ConstantSizeView<'a> {
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(32i64 as isize)
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
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<SizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            SizedStructView::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = SizedStructView<'a>> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_SizedElement_ConstantSizeBuilder {
    array: Box<[SizedStructBuilder]>,
}
impl Serializable for Packet_Array_Field_SizedElement_ConstantSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Packet_Array_Field_SizedElement_ConstantSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_SizedElement_ConstantSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_SizedElement_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_SizedElement_VariableSizeView<'a> {
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        if self.try_get_array_element_size()? == 0
            || self.try_get_array_size()? % self.try_get_array_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_array_size()? / self.try_get_array_element_size()?)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<SizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            SizedStructView::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = SizedStructView<'a>> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_SizedElement_VariableSizeBuilder {
    array: Box<[SizedStructBuilder]>,
}
impl Serializable for Packet_Array_Field_SizedElement_VariableSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            (self.array.len() * 1usize).try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Packet_Array_Field_SizedElement_VariableSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_SizedElement_VariableSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_SizedElement_VariableCountView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_SizedElement_VariableCountView<'a> {
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        self.try_get_array_count()?
            .checked_mul(self.try_get_array_element_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<SizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            SizedStructView::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = SizedStructView<'a>> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_SizedElement_VariableCountBuilder {
    array: Box<[SizedStructBuilder]>,
}
impl Serializable for Packet_Array_Field_SizedElement_VariableCountBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            self.array.len().try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Packet_Array_Field_SizedElement_VariableCountChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_SizedElement_VariableCountChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_SizedElement_UnknownSizeView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> Packet_Array_Field_SizedElement_UnknownSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
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
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        if self.try_get_array_element_size()? == 0
            || self.try_get_array_size()? % self.try_get_array_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_array_size()? / self.try_get_array_element_size()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<SizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            SizedStructView::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = SizedStructView<'a>> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_SizedElement_UnknownSizeBuilder {
    array: Box<[SizedStructBuilder]>,
}
impl Serializable for Packet_Array_Field_SizedElement_UnknownSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Packet_Array_Field_SizedElement_UnknownSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_SizedElement_UnknownSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_UnsizedElement_ConstantSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_UnsizedElement_ConstantSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_array_count()? {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<UnsizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let mut view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        Ok((0..count).map(move |i| {
            let parsed = UnsizedStructView::try_parse(view.into())?;
            view = view.offset(parsed.try_get_size()? * 8)?;
            Ok(parsed)
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = UnsizedStructView<'a>> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_UnsizedElement_ConstantSizeBuilder {
    array: Box<[UnsizedStructBuilder]>,
}
impl Serializable for Packet_Array_Field_UnsizedElement_ConstantSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Packet_Array_Field_UnsizedElement_ConstantSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_UnsizedElement_ConstantSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_UnsizedElement_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_UnsizedElement_VariableSizeView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        let mut cnt = 0;
        let mut view = self.buf.offset(self.try_get_custom_offset_2()?)?;
        let mut remaining_size = self.try_get_array_size()?;
        while remaining_size > 0 {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            if next_struct_size > remaining_size {
                return Err(ParseError::OutOfBoundsAccess);
            }
            remaining_size -= next_struct_size;
            view = view.offset(next_struct_size * 8)?;
            cnt += 1;
        }
        Ok(cnt)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<UnsizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let mut view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        Ok((0..count).map(move |i| {
            let parsed = UnsizedStructView::try_parse(view.into())?;
            view = view.offset(parsed.try_get_size()? * 8)?;
            Ok(parsed)
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = UnsizedStructView<'a>> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_UnsizedElement_VariableSizeBuilder {
    array: Box<[UnsizedStructBuilder]>,
}
impl Serializable for Packet_Array_Field_UnsizedElement_VariableSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            self.array.iter().map(|elem| elem.size_in_bits()).fold(Ok(0), |total, next| {
                let total = total?;
                let next =
                    u64::try_from(next?).or(Err(SerializeError::IntegerConversionFailure))?;
                Ok(total + next)
            })
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Packet_Array_Field_UnsizedElement_VariableSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_UnsizedElement_VariableSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_UnsizedElement_VariableCountView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_UnsizedElement_VariableCountView<'a> {
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_custom_offset_2()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_array_count()? {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<UnsizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let mut view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        Ok((0..count).map(move |i| {
            let parsed = UnsizedStructView::try_parse(view.into())?;
            view = view.offset(parsed.try_get_size()? * 8)?;
            Ok(parsed)
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = UnsizedStructView<'a>> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_UnsizedElement_VariableCountBuilder {
    array: Box<[UnsizedStructBuilder]>,
}
impl Serializable for Packet_Array_Field_UnsizedElement_VariableCountBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            self.array.len().try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Packet_Array_Field_UnsizedElement_VariableCountChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_UnsizedElement_VariableCountChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_UnsizedElement_UnknownSizeView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> Packet_Array_Field_UnsizedElement_UnknownSizeView<'a> {
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
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
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
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        let mut cnt = 0;
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut remaining_size = self.try_get_array_size()?;
        while remaining_size > 0 {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            if next_struct_size > remaining_size {
                return Err(ParseError::OutOfBoundsAccess);
            }
            remaining_size -= next_struct_size;
            view = view.offset(next_struct_size * 8)?;
            cnt += 1;
        }
        Ok(cnt)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<UnsizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let mut view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        Ok((0..count).map(move |i| {
            let parsed = UnsizedStructView::try_parse(view.into())?;
            view = view.offset(parsed.try_get_size()? * 8)?;
            Ok(parsed)
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = UnsizedStructView<'a>> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_UnsizedElement_UnknownSizeBuilder {
    array: Box<[UnsizedStructBuilder]>,
}
impl Serializable for Packet_Array_Field_UnsizedElement_UnknownSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Packet_Array_Field_UnsizedElement_UnknownSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_UnsizedElement_UnknownSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_SizedElement_VariableSize_PaddedView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_SizedElement_VariableSize_PaddedView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(24i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_4(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        if self.try_get_array_element_size()? == 0
            || self.try_get_array_size()? % self.try_get_array_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_array_size()? / self.try_get_array_element_size()?)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_SizedElement_VariableSize_PaddedBuilder {
    array: Box<[u64]>,
}
impl Serializable for Packet_Array_Field_SizedElement_VariableSize_PaddedBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            (self.array.len() * 2usize).try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        for elem in self.array.iter() {
            writer.write_bits(16usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 16usize * self.array.len();
        if (most_recent_array_len > 16usize) {
            return Err(SerializeError::NegativePadding);
        }
        for _ in 0..(16usize - most_recent_array_len) {
            writer.write_bits(8, || Ok(0u64))?;
        }
        Ok(())
    }
}
enum Packet_Array_Field_SizedElement_VariableSize_PaddedChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_SizedElement_VariableSize_PaddedChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_UnsizedElement_VariableCount_PaddedView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_UnsizedElement_VariableCount_PaddedView<'a> {
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(24i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_custom_offset_1()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_array_count()? {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(8usize)?.try_parse()?
            as usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<UnsizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let mut view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        Ok((0..count).map(move |i| {
            let parsed = UnsizedStructView::try_parse(view.into())?;
            view = view.offset(parsed.try_get_size()? * 8)?;
            Ok(parsed)
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = UnsizedStructView<'a>> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Packet_Array_Field_UnsizedElement_VariableCount_PaddedBuilder {
    array: Box<[UnsizedStructBuilder]>,
}
impl Serializable for Packet_Array_Field_UnsizedElement_VariableCount_PaddedBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(8usize, || {
            self.array.len().try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        if (most_recent_array_len > 16usize) {
            return Err(SerializeError::NegativePadding);
        }
        for _ in 0..(16usize - most_recent_array_len) {
            writer.write_bits(8, || Ok(0u64))?;
        }
        Ok(())
    }
}
enum Packet_Array_Field_UnsizedElement_VariableCount_PaddedChild {
    RawData(Box<[u8]>),
}
impl Serializable for Packet_Array_Field_UnsizedElement_VariableCount_PaddedChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct ScalarChild_AView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> ScalarChild_AView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_b(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_b_offset()?)?.slice(8usize)?.try_parse()
    }
    #[inline]
    pub fn get_b(&self) -> u64 {
        self.try_get_b().unwrap()
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
        self.try_get_b()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: ScalarParentView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
}
pub struct ScalarChild_ABuilder {
    b: u64,
}
impl Serializable for ScalarChild_ABuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(8usize, || Ok(self.b))?;
        Ok(())
    }
}
enum ScalarChild_AChild {
    RawData(Box<[u8]>),
}
impl Serializable for ScalarChild_AChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct ScalarChild_BView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> ScalarChild_BView<'a> {
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
    fn try_get_c_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_c(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_c_offset()?)?.slice(16usize)?.try_parse()
    }
    #[inline]
    pub fn get_c(&self) -> u64 {
        self.try_get_c().unwrap()
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
        self.try_get_c()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: ScalarParentView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
}
pub struct ScalarChild_BBuilder {
    c: u64,
}
impl Serializable for ScalarChild_BBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(16usize, || Ok(self.c))?;
        Ok(())
    }
}
enum ScalarChild_BChild {
    RawData(Box<[u8]>),
}
impl Serializable for ScalarChild_BChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct EnumChild_AView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> EnumChild_AView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_b(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_b_offset()?)?.slice(8usize)?.try_parse()
    }
    #[inline]
    pub fn get_b(&self) -> u64 {
        self.try_get_b().unwrap()
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
        self.try_get_b()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: EnumParentView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
}
pub struct EnumChild_ABuilder {
    b: u64,
}
impl Serializable for EnumChild_ABuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(8usize, || Ok(self.b))?;
        Ok(())
    }
}
enum EnumChild_AChild {
    RawData(Box<[u8]>),
}
impl Serializable for EnumChild_AChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct EnumChild_BView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> EnumChild_BView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_c_offset(&self) -> Result<usize, ParseError> {
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
    fn try_get_c(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_c_offset()?)?.slice(16usize)?.try_parse()
    }
    #[inline]
    pub fn get_c(&self) -> u64 {
        self.try_get_c().unwrap()
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
        self.try_get_c()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: EnumParentView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
}
pub struct EnumChild_BBuilder {
    c: u64,
}
impl Serializable for EnumChild_BBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(16usize, || Ok(self.c))?;
        Ok(())
    }
}
enum EnumChild_BChild {
    RawData(Box<[u8]>),
}
impl Serializable for EnumChild_BChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AliasedChild_AView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> AliasedChild_AView<'a> {
    #[inline]
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_b(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_b_offset()?)?.slice(8usize)?.try_parse()
    }
    #[inline]
    pub fn get_b(&self) -> u64 {
        self.try_get_b().unwrap()
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
        self.try_get_b()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: EmptyParentView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
}
pub struct AliasedChild_ABuilder {
    b: u64,
}
impl Serializable for AliasedChild_ABuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(8usize, || Ok(self.b))?;
        Ok(())
    }
}
enum AliasedChild_AChild {
    RawData(Box<[u8]>),
}
impl Serializable for AliasedChild_AChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct AliasedChild_BView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> AliasedChild_BView<'a> {
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
    fn try_get_c_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_c(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_c_offset()?)?.slice(16usize)?.try_parse()
    }
    #[inline]
    pub fn get_c(&self) -> u64 {
        self.try_get_c().unwrap()
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
        self.try_get_c()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: EmptyParentView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
}
pub struct AliasedChild_BBuilder {
    c: u64,
}
impl Serializable for AliasedChild_BBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(16usize, || Ok(self.c))?;
        Ok(())
    }
}
enum AliasedChild_BChild {
    RawData(Box<[u8]>),
}
impl Serializable for AliasedChild_BChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct PartialChild5_AView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> PartialChild5_AView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(11i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(11i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_b(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_b_offset()?)?.slice(11usize)?.try_parse()
    }
    #[inline]
    pub fn get_b(&self) -> u64 {
        self.try_get_b().unwrap()
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
        self.try_get_b()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: PartialParent5View<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
}
pub struct PartialChild5_ABuilder {
    b: u64,
}
impl Serializable for PartialChild5_ABuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(11usize, || Ok(self.b))?;
        Ok(())
    }
}
enum PartialChild5_AChild {
    RawData(Box<[u8]>),
}
impl Serializable for PartialChild5_AChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct PartialChild5_BView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> PartialChild5_BView<'a> {
    #[inline]
    fn try_get_c_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(27i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(27i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_c(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_c_offset()?)?.slice(27usize)?.try_parse()
    }
    #[inline]
    pub fn get_c(&self) -> u64 {
        self.try_get_c().unwrap()
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
        self.try_get_c()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: PartialParent5View<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
}
pub struct PartialChild5_BBuilder {
    c: u64,
}
impl Serializable for PartialChild5_BBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(27usize, || Ok(self.c))?;
        Ok(())
    }
}
enum PartialChild5_BChild {
    RawData(Box<[u8]>),
}
impl Serializable for PartialChild5_BChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct PartialChild12_AView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> PartialChild12_AView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_d_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_d(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_d_offset()?)?.slice(4usize)?.try_parse()
    }
    #[inline]
    pub fn get_d(&self) -> u64 {
        self.try_get_d().unwrap()
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
        self.try_get_d()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: PartialParent12View<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
}
pub struct PartialChild12_ABuilder {
    d: u64,
}
impl Serializable for PartialChild12_ABuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || Ok(self.d))?;
        Ok(())
    }
}
enum PartialChild12_AChild {
    RawData(Box<[u8]>),
}
impl Serializable for PartialChild12_AChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct PartialChild12_BView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> PartialChild12_BView<'a> {
    #[inline]
    fn try_get_e_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(20i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(20i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_e(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_e_offset()?)?.slice(20usize)?.try_parse()
    }
    #[inline]
    pub fn get_e(&self) -> u64 {
        self.try_get_e().unwrap()
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
        self.try_get_e()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: PartialParent12View<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
    }
}
pub struct PartialChild12_BBuilder {
    e: u64,
}
impl Serializable for PartialChild12_BBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(20usize, || Ok(self.e))?;
        Ok(())
    }
}
enum PartialChild12_BChild {
    RawData(Box<[u8]>),
}
impl Serializable for PartialChild12_BChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Scalar_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Scalar_FieldView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_c_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(7i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(57i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_a(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_a_offset()?)?.slice(7usize)?.try_parse()
    }
    #[inline]
    pub fn get_a(&self) -> u64 {
        self.try_get_a().unwrap()
    }
    fn try_get_c(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_c_offset()?)?.slice(57usize)?.try_parse()
    }
    #[inline]
    pub fn get_c(&self) -> u64 {
        self.try_get_c().unwrap()
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
        self.try_get_a()?;
        self.try_get_c()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Scalar_FieldBuilder {
    a: u64,
    c: u64,
}
impl Serializable for Struct_Scalar_FieldBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(7usize, || Ok(self.a))?;
        writer.write_bits(57usize, || Ok(self.c))?;
        Ok(())
    }
}
enum Struct_Scalar_FieldChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Scalar_FieldChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Enum_Field_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Enum_Field_View<'a> {
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(57i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_c_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(7i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_a(&self) -> Result<Enum7, ParseError> {
        Enum7::try_parse(self.buf.offset(self.try_get_a_offset()?)?.into())
    }
    #[inline]
    pub fn get_a(&self) -> Enum7 {
        self.try_get_a().unwrap()
    }
    fn try_get_c(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_c_offset()?)?.slice(57usize)?.try_parse()
    }
    #[inline]
    pub fn get_c(&self) -> u64 {
        self.try_get_c().unwrap()
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
        self.try_get_a()?;
        self.try_get_c()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Enum_Field_Builder {
    a: Enum7,
    c: u64,
}
impl Serializable for Struct_Enum_Field_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.a.serialize(writer)?;
        writer.write_bits(57usize, || Ok(self.c))?;
        Ok(())
    }
}
enum Struct_Enum_Field_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Enum_Field_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Enum_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Enum_FieldView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_s(&self) -> Result<Struct_Enum_Field_View<'a>, ParseError> {
        Struct_Enum_Field_View::try_parse(self.buf.offset(self.try_get_s_offset()?)?.into())
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Enum_Field_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Enum_FieldBuilder {
    s: Struct_Enum_Field_Builder,
}
impl Serializable for Struct_Enum_FieldBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Enum_FieldChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Enum_FieldChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Reserved_Field_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Reserved_Field_View<'a> {
    #[inline]
    fn try_get_c_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add_signed(55i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(7i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(2i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_a(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_a_offset()?)?.slice(7usize)?.try_parse()
    }
    #[inline]
    pub fn get_a(&self) -> u64 {
        self.try_get_a().unwrap()
    }
    fn try_get_c(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_c_offset()?)?.slice(55usize)?.try_parse()
    }
    #[inline]
    pub fn get_c(&self) -> u64 {
        self.try_get_c().unwrap()
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
        self.try_get_a()?;
        self.try_get_c()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Reserved_Field_Builder {
    a: u64,
    c: u64,
}
impl Serializable for Struct_Reserved_Field_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(7usize, || Ok(self.a))?;
        writer.write_bits(2usize, || Ok(0u64))?;
        writer.write_bits(55usize, || Ok(self.c))?;
        Ok(())
    }
}
enum Struct_Reserved_Field_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Reserved_Field_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Reserved_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Reserved_FieldView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_s(&self) -> Result<Struct_Reserved_Field_View<'a>, ParseError> {
        Struct_Reserved_Field_View::try_parse(self.buf.offset(self.try_get_s_offset()?)?.into())
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Reserved_Field_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Reserved_FieldBuilder {
    s: Struct_Reserved_Field_Builder,
}
impl Serializable for Struct_Reserved_FieldBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Reserved_FieldChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Reserved_FieldChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Size_Field_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Size_Field_View<'a> {
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(61i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(3i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_b_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_b_size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(3usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_b_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_b_count(&self) -> Result<usize, ParseError> {
        if self.try_get_b_element_size()? == 0
            || self.try_get_b_size()? % self.try_get_b_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_b_size()? / self.try_get_b_element_size()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_a(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_a_offset()?)?.slice(61usize)?.try_parse()
    }
    #[inline]
    pub fn get_a(&self) -> u64 {
        self.try_get_a().unwrap()
    }
    fn try_get_b_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_b_offset()?)?;
        let count = self.try_get_b_count()?;
        let element_size = self.try_get_b_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_b_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_b_iter().unwrap().map(|x| x.unwrap())
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
        self.try_get_a()?;
        for elem in self.try_get_b_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Size_Field_Builder {
    a: u64,
    b: Box<[u64]>,
}
impl Serializable for Struct_Size_Field_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(3usize, || {
            (self.b.len() * 1usize).try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(61usize, || Ok(self.a))?;
        for elem in self.b.iter() {
            writer.write_bits(8usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 8usize * self.b.len();
        Ok(())
    }
}
enum Struct_Size_Field_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Size_Field_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Size_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Size_FieldView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(
                self.try_get_s_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size = Struct_Size_Field_View::try_parse(view)?.try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    fn try_get_s(&self) -> Result<Struct_Size_Field_View<'a>, ParseError> {
        Struct_Size_Field_View::try_parse(self.buf.offset(self.try_get_s_offset()?)?.into())
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Size_Field_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Size_FieldBuilder {
    s: Struct_Size_Field_Builder,
}
impl Serializable for Struct_Size_FieldBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Size_FieldChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Size_FieldChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Count_Field_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Count_Field_View<'a> {
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(3i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(61i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_b_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_b_size(&self) -> Result<usize, ParseError> {
        self.try_get_b_count()?
            .checked_mul(self.try_get_b_element_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_b_count(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(3usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_b_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_a(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_a_offset()?)?.slice(61usize)?.try_parse()
    }
    #[inline]
    pub fn get_a(&self) -> u64 {
        self.try_get_a().unwrap()
    }
    fn try_get_b_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_b_offset()?)?;
        let count = self.try_get_b_count()?;
        let element_size = self.try_get_b_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_b_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_b_iter().unwrap().map(|x| x.unwrap())
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
        self.try_get_a()?;
        for elem in self.try_get_b_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Count_Field_Builder {
    a: u64,
    b: Box<[u64]>,
}
impl Serializable for Struct_Count_Field_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(3usize, || {
            self.b.len().try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(61usize, || Ok(self.a))?;
        for elem in self.b.iter() {
            writer.write_bits(8usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 8usize * self.b.len();
        Ok(())
    }
}
enum Struct_Count_Field_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Count_Field_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Count_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Count_FieldView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(
                self.try_get_s_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size = Struct_Count_Field_View::try_parse(view)?.try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    fn try_get_s(&self) -> Result<Struct_Count_Field_View<'a>, ParseError> {
        Struct_Count_Field_View::try_parse(self.buf.offset(self.try_get_s_offset()?)?.into())
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Count_Field_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Count_FieldBuilder {
    s: Struct_Count_Field_Builder,
}
impl Serializable for Struct_Count_FieldBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Count_FieldChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Count_FieldChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_FixedScalar_Field_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_FixedScalar_Field_View<'a> {
    #[inline]
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(7i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(57i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_b(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_b_offset()?)?.slice(57usize)?.try_parse()
    }
    #[inline]
    pub fn get_b(&self) -> u64 {
        self.try_get_b().unwrap()
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
        self.try_get_b()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_FixedScalar_Field_Builder {
    b: u64,
}
impl Serializable for Struct_FixedScalar_Field_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(7usize, || Ok(7u64))?;
        writer.write_bits(57usize, || Ok(self.b))?;
        Ok(())
    }
}
enum Struct_FixedScalar_Field_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_FixedScalar_Field_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_FixedScalar_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_FixedScalar_FieldView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_s(&self) -> Result<Struct_FixedScalar_Field_View<'a>, ParseError> {
        Struct_FixedScalar_Field_View::try_parse(self.buf.offset(self.try_get_s_offset()?)?.into())
    }
    #[inline]
    pub fn get_s(&self) -> Struct_FixedScalar_Field_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_FixedScalar_FieldBuilder {
    s: Struct_FixedScalar_Field_Builder,
}
impl Serializable for Struct_FixedScalar_FieldBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_FixedScalar_FieldChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_FixedScalar_FieldChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_FixedEnum_Field_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_FixedEnum_Field_View<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(7i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(57i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_b(&self) -> Result<u64, ParseError> {
        self.buf.offset(self.try_get_b_offset()?)?.slice(57usize)?.try_parse()
    }
    #[inline]
    pub fn get_b(&self) -> u64 {
        self.try_get_b().unwrap()
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
        self.try_get_b()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_FixedEnum_Field_Builder {
    b: u64,
}
impl Serializable for Struct_FixedEnum_Field_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(7usize, || Ok(Enum7::A.value()))?;
        writer.write_bits(57usize, || Ok(self.b))?;
        Ok(())
    }
}
enum Struct_FixedEnum_Field_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_FixedEnum_Field_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_FixedEnum_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_FixedEnum_FieldView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_s(&self) -> Result<Struct_FixedEnum_Field_View<'a>, ParseError> {
        Struct_FixedEnum_Field_View::try_parse(self.buf.offset(self.try_get_s_offset()?)?.into())
    }
    #[inline]
    pub fn get_s(&self) -> Struct_FixedEnum_Field_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_FixedEnum_FieldBuilder {
    s: Struct_FixedEnum_Field_Builder,
}
impl Serializable for Struct_FixedEnum_FieldBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_FixedEnum_FieldChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_FixedEnum_FieldChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Struct_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Struct_FieldView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
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
            .checked_add(
                self.try_get_b_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_b_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_custom_offset_1()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    fn try_get_a(&self) -> Result<SizedStructView<'a>, ParseError> {
        SizedStructView::try_parse(self.buf.offset(self.try_get_a_offset()?)?.into())
    }
    #[inline]
    pub fn get_a(&self) -> SizedStructView<'a> {
        self.try_get_a().unwrap()
    }
    fn try_get_b(&self) -> Result<UnsizedStructView<'a>, ParseError> {
        UnsizedStructView::try_parse(self.buf.offset(self.try_get_b_offset()?)?.into())
    }
    #[inline]
    pub fn get_b(&self) -> UnsizedStructView<'a> {
        self.try_get_b().unwrap()
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
        self.try_get_a()?;
        self.try_get_b()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Struct_FieldBuilder {
    a: SizedStructBuilder,
    b: UnsizedStructBuilder,
}
impl Serializable for Struct_Struct_FieldBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.a.serialize(writer)?;
        self.b.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Struct_FieldChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Struct_FieldChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ByteElement_ConstantSize_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ByteElement_ConstantSize_View<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(32i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(32i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_ByteElement_ConstantSize_Builder {
    array: Box<[u64]>,
}
impl Serializable for Struct_Array_Field_ByteElement_ConstantSize_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        for elem in self.array.iter() {
            writer.write_bits(8usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 8usize * self.array.len();
        Ok(())
    }
}
enum Struct_Array_Field_ByteElement_ConstantSize_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_ByteElement_ConstantSize_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ByteElement_ConstantSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ByteElement_ConstantSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(32i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(32i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_ByteElement_ConstantSize_View<'a>, ParseError> {
        Struct_Array_Field_ByteElement_ConstantSize_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_ByteElement_ConstantSize_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_ByteElement_ConstantSizeBuilder {
    s: Struct_Array_Field_ByteElement_ConstantSize_Builder,
}
impl Serializable for Struct_Array_Field_ByteElement_ConstantSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_ByteElement_ConstantSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_ByteElement_ConstantSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ByteElement_VariableSize_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ByteElement_VariableSize_View<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        if self.try_get_array_element_size()? == 0
            || self.try_get_array_size()? % self.try_get_array_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_array_size()? / self.try_get_array_element_size()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_ByteElement_VariableSize_Builder {
    array: Box<[u64]>,
}
impl Serializable for Struct_Array_Field_ByteElement_VariableSize_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            (self.array.len() * 1usize).try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        for elem in self.array.iter() {
            writer.write_bits(8usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 8usize * self.array.len();
        Ok(())
    }
}
enum Struct_Array_Field_ByteElement_VariableSize_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_ByteElement_VariableSize_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ByteElement_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ByteElement_VariableSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(
                self.try_get_s_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size =
                Struct_Array_Field_ByteElement_VariableSize_View::try_parse(view)?
                    .try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_ByteElement_VariableSize_View<'a>, ParseError> {
        Struct_Array_Field_ByteElement_VariableSize_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_ByteElement_VariableSize_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_ByteElement_VariableSizeBuilder {
    s: Struct_Array_Field_ByteElement_VariableSize_Builder,
}
impl Serializable for Struct_Array_Field_ByteElement_VariableSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_ByteElement_VariableSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_ByteElement_VariableSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ByteElement_VariableCount_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ByteElement_VariableCount_View<'a> {
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        self.try_get_array_count()?
            .checked_mul(self.try_get_array_element_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_ByteElement_VariableCount_Builder {
    array: Box<[u64]>,
}
impl Serializable for Struct_Array_Field_ByteElement_VariableCount_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            self.array.len().try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        for elem in self.array.iter() {
            writer.write_bits(8usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 8usize * self.array.len();
        Ok(())
    }
}
enum Struct_Array_Field_ByteElement_VariableCount_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_ByteElement_VariableCount_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ByteElement_VariableCountView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ByteElement_VariableCountView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(
                self.try_get_s_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size =
                Struct_Array_Field_ByteElement_VariableCount_View::try_parse(view)?
                    .try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_ByteElement_VariableCount_View<'a>, ParseError> {
        Struct_Array_Field_ByteElement_VariableCount_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_ByteElement_VariableCount_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_ByteElement_VariableCountBuilder {
    s: Struct_Array_Field_ByteElement_VariableCount_Builder,
}
impl Serializable for Struct_Array_Field_ByteElement_VariableCountBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_ByteElement_VariableCountChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_ByteElement_VariableCountChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ByteElement_UnknownSize_View<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> Struct_Array_Field_ByteElement_UnknownSize_View<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
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
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        if self.try_get_array_element_size()? == 0
            || self.try_get_array_size()? % self.try_get_array_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_array_size()? / self.try_get_array_element_size()?)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_ByteElement_UnknownSize_Builder {
    array: Box<[u64]>,
}
impl Serializable for Struct_Array_Field_ByteElement_UnknownSize_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        for elem in self.array.iter() {
            writer.write_bits(8usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 8usize * self.array.len();
        Ok(())
    }
}
enum Struct_Array_Field_ByteElement_UnknownSize_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_ByteElement_UnknownSize_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ByteElement_UnknownSizeView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> Struct_Array_Field_ByteElement_UnknownSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_s_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
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
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_header_start_offset()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    fn try_get_s(&self) -> Result<Struct_Array_Field_ByteElement_UnknownSize_View<'a>, ParseError> {
        Struct_Array_Field_ByteElement_UnknownSize_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.slice(self.try_get_s_end_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_ByteElement_UnknownSize_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_ByteElement_UnknownSizeBuilder {
    s: Struct_Array_Field_ByteElement_UnknownSize_Builder,
}
impl Serializable for Struct_Array_Field_ByteElement_UnknownSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_ByteElement_UnknownSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_ByteElement_UnknownSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ScalarElement_ConstantSize_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ScalarElement_ConstantSize_View<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(8usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_ScalarElement_ConstantSize_Builder {
    array: Box<[u64]>,
}
impl Serializable for Struct_Array_Field_ScalarElement_ConstantSize_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        for elem in self.array.iter() {
            writer.write_bits(16usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 16usize * self.array.len();
        Ok(())
    }
}
enum Struct_Array_Field_ScalarElement_ConstantSize_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_ScalarElement_ConstantSize_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ScalarElement_ConstantSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ScalarElement_ConstantSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_ScalarElement_ConstantSize_View<'a>, ParseError> {
        Struct_Array_Field_ScalarElement_ConstantSize_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_ScalarElement_ConstantSize_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_ScalarElement_ConstantSizeBuilder {
    s: Struct_Array_Field_ScalarElement_ConstantSize_Builder,
}
impl Serializable for Struct_Array_Field_ScalarElement_ConstantSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_ScalarElement_ConstantSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_ScalarElement_ConstantSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ScalarElement_VariableSize_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ScalarElement_VariableSize_View<'a> {
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        if self.try_get_array_element_size()? == 0
            || self.try_get_array_size()? % self.try_get_array_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_array_size()? / self.try_get_array_element_size()?)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_ScalarElement_VariableSize_Builder {
    array: Box<[u64]>,
}
impl Serializable for Struct_Array_Field_ScalarElement_VariableSize_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            (self.array.len() * 2usize).try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        for elem in self.array.iter() {
            writer.write_bits(16usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 16usize * self.array.len();
        Ok(())
    }
}
enum Struct_Array_Field_ScalarElement_VariableSize_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_ScalarElement_VariableSize_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ScalarElement_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ScalarElement_VariableSizeView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(
                self.try_get_s_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size =
                Struct_Array_Field_ScalarElement_VariableSize_View::try_parse(view)?
                    .try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_ScalarElement_VariableSize_View<'a>, ParseError> {
        Struct_Array_Field_ScalarElement_VariableSize_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_ScalarElement_VariableSize_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_ScalarElement_VariableSizeBuilder {
    s: Struct_Array_Field_ScalarElement_VariableSize_Builder,
}
impl Serializable for Struct_Array_Field_ScalarElement_VariableSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_ScalarElement_VariableSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_ScalarElement_VariableSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ScalarElement_VariableCount_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ScalarElement_VariableCount_View<'a> {
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        self.try_get_array_count()?
            .checked_mul(self.try_get_array_element_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_ScalarElement_VariableCount_Builder {
    array: Box<[u64]>,
}
impl Serializable for Struct_Array_Field_ScalarElement_VariableCount_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            self.array.len().try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        for elem in self.array.iter() {
            writer.write_bits(16usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 16usize * self.array.len();
        Ok(())
    }
}
enum Struct_Array_Field_ScalarElement_VariableCount_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_ScalarElement_VariableCount_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ScalarElement_VariableCountView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ScalarElement_VariableCountView<'a> {
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(
                self.try_get_s_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size =
                Struct_Array_Field_ScalarElement_VariableCount_View::try_parse(view)?
                    .try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_ScalarElement_VariableCount_View<'a>, ParseError> {
        Struct_Array_Field_ScalarElement_VariableCount_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_ScalarElement_VariableCount_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_ScalarElement_VariableCountBuilder {
    s: Struct_Array_Field_ScalarElement_VariableCount_Builder,
}
impl Serializable for Struct_Array_Field_ScalarElement_VariableCountBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_ScalarElement_VariableCountChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_ScalarElement_VariableCountChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ScalarElement_UnknownSize_View<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> Struct_Array_Field_ScalarElement_UnknownSize_View<'a> {
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
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
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
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        if self.try_get_array_element_size()? == 0
            || self.try_get_array_size()? % self.try_get_array_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_array_size()? / self.try_get_array_element_size()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_ScalarElement_UnknownSize_Builder {
    array: Box<[u64]>,
}
impl Serializable for Struct_Array_Field_ScalarElement_UnknownSize_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        for elem in self.array.iter() {
            writer.write_bits(16usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 16usize * self.array.len();
        Ok(())
    }
}
enum Struct_Array_Field_ScalarElement_UnknownSize_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_ScalarElement_UnknownSize_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ScalarElement_UnknownSizeView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> Struct_Array_Field_ScalarElement_UnknownSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_s_end_offset(&self) -> Result<usize, ParseError> {
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
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_header_start_offset()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_ScalarElement_UnknownSize_View<'a>, ParseError> {
        Struct_Array_Field_ScalarElement_UnknownSize_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.slice(self.try_get_s_end_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_ScalarElement_UnknownSize_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_ScalarElement_UnknownSizeBuilder {
    s: Struct_Array_Field_ScalarElement_UnknownSize_Builder,
}
impl Serializable for Struct_Array_Field_ScalarElement_UnknownSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_ScalarElement_UnknownSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_ScalarElement_UnknownSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_EnumElement_ConstantSize_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_EnumElement_ConstantSize_View<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(8usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<Enum16, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            Enum16::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = Enum16> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_EnumElement_ConstantSize_Builder {
    array: Box<[Enum16]>,
}
impl Serializable for Struct_Array_Field_EnumElement_ConstantSize_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Struct_Array_Field_EnumElement_ConstantSize_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_EnumElement_ConstantSize_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_EnumElement_ConstantSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_EnumElement_ConstantSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_EnumElement_ConstantSize_View<'a>, ParseError> {
        Struct_Array_Field_EnumElement_ConstantSize_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_EnumElement_ConstantSize_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_EnumElement_ConstantSizeBuilder {
    s: Struct_Array_Field_EnumElement_ConstantSize_Builder,
}
impl Serializable for Struct_Array_Field_EnumElement_ConstantSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_EnumElement_ConstantSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_EnumElement_ConstantSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_EnumElement_VariableSize_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_EnumElement_VariableSize_View<'a> {
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        if self.try_get_array_element_size()? == 0
            || self.try_get_array_size()? % self.try_get_array_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_array_size()? / self.try_get_array_element_size()?)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<Enum16, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            Enum16::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = Enum16> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_EnumElement_VariableSize_Builder {
    array: Box<[Enum16]>,
}
impl Serializable for Struct_Array_Field_EnumElement_VariableSize_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            (self.array.len() * 2usize).try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Struct_Array_Field_EnumElement_VariableSize_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_EnumElement_VariableSize_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_EnumElement_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_EnumElement_VariableSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(
                self.try_get_s_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size =
                Struct_Array_Field_EnumElement_VariableSize_View::try_parse(view)?
                    .try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_EnumElement_VariableSize_View<'a>, ParseError> {
        Struct_Array_Field_EnumElement_VariableSize_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_EnumElement_VariableSize_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_EnumElement_VariableSizeBuilder {
    s: Struct_Array_Field_EnumElement_VariableSize_Builder,
}
impl Serializable for Struct_Array_Field_EnumElement_VariableSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_EnumElement_VariableSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_EnumElement_VariableSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_EnumElement_VariableCount_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_EnumElement_VariableCount_View<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        self.try_get_array_count()?
            .checked_mul(self.try_get_array_element_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<Enum16, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            Enum16::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = Enum16> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_EnumElement_VariableCount_Builder {
    array: Box<[Enum16]>,
}
impl Serializable for Struct_Array_Field_EnumElement_VariableCount_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            self.array.len().try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Struct_Array_Field_EnumElement_VariableCount_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_EnumElement_VariableCount_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_EnumElement_VariableCountView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_EnumElement_VariableCountView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(
                self.try_get_s_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size =
                Struct_Array_Field_EnumElement_VariableCount_View::try_parse(view)?
                    .try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_EnumElement_VariableCount_View<'a>, ParseError> {
        Struct_Array_Field_EnumElement_VariableCount_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_EnumElement_VariableCount_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_EnumElement_VariableCountBuilder {
    s: Struct_Array_Field_EnumElement_VariableCount_Builder,
}
impl Serializable for Struct_Array_Field_EnumElement_VariableCountBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_EnumElement_VariableCountChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_EnumElement_VariableCountChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_EnumElement_UnknownSize_View<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> Struct_Array_Field_EnumElement_UnknownSize_View<'a> {
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
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        if self.try_get_array_element_size()? == 0
            || self.try_get_array_size()? % self.try_get_array_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_array_size()? / self.try_get_array_element_size()?)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_header_start_offset()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<Enum16, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            Enum16::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = Enum16> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_EnumElement_UnknownSize_Builder {
    array: Box<[Enum16]>,
}
impl Serializable for Struct_Array_Field_EnumElement_UnknownSize_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Struct_Array_Field_EnumElement_UnknownSize_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_EnumElement_UnknownSize_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_EnumElement_UnknownSizeView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> Struct_Array_Field_EnumElement_UnknownSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_s_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_header_start_offset()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    fn try_get_s(&self) -> Result<Struct_Array_Field_EnumElement_UnknownSize_View<'a>, ParseError> {
        Struct_Array_Field_EnumElement_UnknownSize_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.slice(self.try_get_s_end_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_EnumElement_UnknownSize_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_EnumElement_UnknownSizeBuilder {
    s: Struct_Array_Field_EnumElement_UnknownSize_Builder,
}
impl Serializable for Struct_Array_Field_EnumElement_UnknownSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_EnumElement_UnknownSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_EnumElement_UnknownSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_SizedElement_ConstantSize_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_SizedElement_ConstantSize_View<'a> {
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(32i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(32i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<SizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            SizedStructView::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = SizedStructView<'a>> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_SizedElement_ConstantSize_Builder {
    array: Box<[SizedStructBuilder]>,
}
impl Serializable for Struct_Array_Field_SizedElement_ConstantSize_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Struct_Array_Field_SizedElement_ConstantSize_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_SizedElement_ConstantSize_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_SizedElement_ConstantSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_SizedElement_ConstantSizeView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(32i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(32i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_SizedElement_ConstantSize_View<'a>, ParseError> {
        Struct_Array_Field_SizedElement_ConstantSize_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_SizedElement_ConstantSize_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_SizedElement_ConstantSizeBuilder {
    s: Struct_Array_Field_SizedElement_ConstantSize_Builder,
}
impl Serializable for Struct_Array_Field_SizedElement_ConstantSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_SizedElement_ConstantSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_SizedElement_ConstantSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_SizedElement_VariableSize_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_SizedElement_VariableSize_View<'a> {
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        if self.try_get_array_element_size()? == 0
            || self.try_get_array_size()? % self.try_get_array_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_array_size()? / self.try_get_array_element_size()?)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<SizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            SizedStructView::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = SizedStructView<'a>> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_SizedElement_VariableSize_Builder {
    array: Box<[SizedStructBuilder]>,
}
impl Serializable for Struct_Array_Field_SizedElement_VariableSize_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            (self.array.len() * 1usize).try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Struct_Array_Field_SizedElement_VariableSize_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_SizedElement_VariableSize_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_SizedElement_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_SizedElement_VariableSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(
                self.try_get_s_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size =
                Struct_Array_Field_SizedElement_VariableSize_View::try_parse(view)?
                    .try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_SizedElement_VariableSize_View<'a>, ParseError> {
        Struct_Array_Field_SizedElement_VariableSize_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_SizedElement_VariableSize_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_SizedElement_VariableSizeBuilder {
    s: Struct_Array_Field_SizedElement_VariableSize_Builder,
}
impl Serializable for Struct_Array_Field_SizedElement_VariableSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_SizedElement_VariableSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_SizedElement_VariableSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_SizedElement_VariableCount_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_SizedElement_VariableCount_View<'a> {
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        self.try_get_array_count()?
            .checked_mul(self.try_get_array_element_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<SizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            SizedStructView::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = SizedStructView<'a>> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_SizedElement_VariableCount_Builder {
    array: Box<[SizedStructBuilder]>,
}
impl Serializable for Struct_Array_Field_SizedElement_VariableCount_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            self.array.len().try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Struct_Array_Field_SizedElement_VariableCount_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_SizedElement_VariableCount_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_SizedElement_VariableCountView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_SizedElement_VariableCountView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(
                self.try_get_s_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size =
                Struct_Array_Field_SizedElement_VariableCount_View::try_parse(view)?
                    .try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_SizedElement_VariableCount_View<'a>, ParseError> {
        Struct_Array_Field_SizedElement_VariableCount_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_SizedElement_VariableCount_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_SizedElement_VariableCountBuilder {
    s: Struct_Array_Field_SizedElement_VariableCount_Builder,
}
impl Serializable for Struct_Array_Field_SizedElement_VariableCountBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_SizedElement_VariableCountChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_SizedElement_VariableCountChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_SizedElement_UnknownSize_View<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> Struct_Array_Field_SizedElement_UnknownSize_View<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
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
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        if self.try_get_array_element_size()? == 0
            || self.try_get_array_size()? % self.try_get_array_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_array_size()? / self.try_get_array_element_size()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_header_start_offset()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<SizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            SizedStructView::try_parse(curr_view.into())
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = SizedStructView<'a>> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_SizedElement_UnknownSize_Builder {
    array: Box<[SizedStructBuilder]>,
}
impl Serializable for Struct_Array_Field_SizedElement_UnknownSize_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Struct_Array_Field_SizedElement_UnknownSize_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_SizedElement_UnknownSize_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_SizedElement_UnknownSizeView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> Struct_Array_Field_SizedElement_UnknownSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_s_end_offset(&self) -> Result<usize, ParseError> {
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
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_header_start_offset()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_SizedElement_UnknownSize_View<'a>, ParseError> {
        Struct_Array_Field_SizedElement_UnknownSize_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.slice(self.try_get_s_end_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_SizedElement_UnknownSize_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_SizedElement_UnknownSizeBuilder {
    s: Struct_Array_Field_SizedElement_UnknownSize_Builder,
}
impl Serializable for Struct_Array_Field_SizedElement_UnknownSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_SizedElement_UnknownSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_SizedElement_UnknownSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_UnsizedElement_ConstantSize_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_UnsizedElement_ConstantSize_View<'a> {
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_array_count()? {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<UnsizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let mut view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        Ok((0..count).map(move |i| {
            let parsed = UnsizedStructView::try_parse(view.into())?;
            view = view.offset(parsed.try_get_size()? * 8)?;
            Ok(parsed)
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = UnsizedStructView<'a>> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_UnsizedElement_ConstantSize_Builder {
    array: Box<[UnsizedStructBuilder]>,
}
impl Serializable for Struct_Array_Field_UnsizedElement_ConstantSize_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Struct_Array_Field_UnsizedElement_ConstantSize_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_UnsizedElement_ConstantSize_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_UnsizedElement_ConstantSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_UnsizedElement_ConstantSizeView<'a> {
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(
                self.try_get_s_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size =
                Struct_Array_Field_UnsizedElement_ConstantSize_View::try_parse(view)?
                    .try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_UnsizedElement_ConstantSize_View<'a>, ParseError> {
        Struct_Array_Field_UnsizedElement_ConstantSize_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_UnsizedElement_ConstantSize_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_UnsizedElement_ConstantSizeBuilder {
    s: Struct_Array_Field_UnsizedElement_ConstantSize_Builder,
}
impl Serializable for Struct_Array_Field_UnsizedElement_ConstantSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_UnsizedElement_ConstantSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_UnsizedElement_ConstantSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_UnsizedElement_VariableSize_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_UnsizedElement_VariableSize_View<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        let mut cnt = 0;
        let mut view = self.buf.offset(self.try_get_custom_offset_2()?)?;
        let mut remaining_size = self.try_get_array_size()?;
        while remaining_size > 0 {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            if next_struct_size > remaining_size {
                return Err(ParseError::OutOfBoundsAccess);
            }
            remaining_size -= next_struct_size;
            view = view.offset(next_struct_size * 8)?;
            cnt += 1;
        }
        Ok(cnt)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<UnsizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let mut view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        Ok((0..count).map(move |i| {
            let parsed = UnsizedStructView::try_parse(view.into())?;
            view = view.offset(parsed.try_get_size()? * 8)?;
            Ok(parsed)
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = UnsizedStructView<'a>> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_UnsizedElement_VariableSize_Builder {
    array: Box<[UnsizedStructBuilder]>,
}
impl Serializable for Struct_Array_Field_UnsizedElement_VariableSize_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            self.array.iter().map(|elem| elem.size_in_bits()).fold(Ok(0), |total, next| {
                let total = total?;
                let next =
                    u64::try_from(next?).or(Err(SerializeError::IntegerConversionFailure))?;
                Ok(total + next)
            })
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Struct_Array_Field_UnsizedElement_VariableSize_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_UnsizedElement_VariableSize_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_UnsizedElement_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_UnsizedElement_VariableSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(
                self.try_get_s_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size =
                Struct_Array_Field_UnsizedElement_VariableSize_View::try_parse(view)?
                    .try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_UnsizedElement_VariableSize_View<'a>, ParseError> {
        Struct_Array_Field_UnsizedElement_VariableSize_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_UnsizedElement_VariableSize_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_UnsizedElement_VariableSizeBuilder {
    s: Struct_Array_Field_UnsizedElement_VariableSize_Builder,
}
impl Serializable for Struct_Array_Field_UnsizedElement_VariableSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_UnsizedElement_VariableSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_UnsizedElement_VariableSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_UnsizedElement_VariableCount_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_UnsizedElement_VariableCount_View<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_custom_offset_2()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_array_count()? {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<UnsizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let mut view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        Ok((0..count).map(move |i| {
            let parsed = UnsizedStructView::try_parse(view.into())?;
            view = view.offset(parsed.try_get_size()? * 8)?;
            Ok(parsed)
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = UnsizedStructView<'a>> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_UnsizedElement_VariableCount_Builder {
    array: Box<[UnsizedStructBuilder]>,
}
impl Serializable for Struct_Array_Field_UnsizedElement_VariableCount_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            self.array.len().try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Struct_Array_Field_UnsizedElement_VariableCount_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_UnsizedElement_VariableCount_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_UnsizedElement_VariableCountView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_UnsizedElement_VariableCountView<'a> {
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(
                self.try_get_s_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size =
                Struct_Array_Field_UnsizedElement_VariableCount_View::try_parse(view)?
                    .try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_UnsizedElement_VariableCount_View<'a>, ParseError> {
        Struct_Array_Field_UnsizedElement_VariableCount_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_UnsizedElement_VariableCount_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_UnsizedElement_VariableCountBuilder {
    s: Struct_Array_Field_UnsizedElement_VariableCount_Builder,
}
impl Serializable for Struct_Array_Field_UnsizedElement_VariableCountBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_UnsizedElement_VariableCountChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_UnsizedElement_VariableCountChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_UnsizedElement_UnknownSize_View<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> Struct_Array_Field_UnsizedElement_UnknownSize_View<'a> {
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
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        let mut cnt = 0;
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut remaining_size = self.try_get_array_size()?;
        while remaining_size > 0 {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            if next_struct_size > remaining_size {
                return Err(ParseError::OutOfBoundsAccess);
            }
            remaining_size -= next_struct_size;
            view = view.offset(next_struct_size * 8)?;
            cnt += 1;
        }
        Ok(cnt)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_header_start_offset()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<UnsizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let mut view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        Ok((0..count).map(move |i| {
            let parsed = UnsizedStructView::try_parse(view.into())?;
            view = view.offset(parsed.try_get_size()? * 8)?;
            Ok(parsed)
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = UnsizedStructView<'a>> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_UnsizedElement_UnknownSize_Builder {
    array: Box<[UnsizedStructBuilder]>,
}
impl Serializable for Struct_Array_Field_UnsizedElement_UnknownSize_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        Ok(())
    }
}
enum Struct_Array_Field_UnsizedElement_UnknownSize_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_UnsizedElement_UnknownSize_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_UnsizedElement_UnknownSizeView<'a> {
    buf: SizedBitSlice<'a>,
}
impl<'a> Struct_Array_Field_UnsizedElement_UnknownSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.buf.get_size_in_bits())
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_s_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
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
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let bit_difference = self
            .try_get_trailer_start_offset()?
            .checked_sub(self.try_get_header_start_offset()?)
            .ok_or(ParseError::ArithmeticOverflow)?;
        if bit_difference % 8 != 0 {
            return Err(ParseError::DivisionFailure);
        }
        Ok(bit_difference / 8)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_UnsizedElement_UnknownSize_View<'a>, ParseError> {
        Struct_Array_Field_UnsizedElement_UnknownSize_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.slice(self.try_get_s_end_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_UnsizedElement_UnknownSize_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_UnsizedElement_UnknownSizeBuilder {
    s: Struct_Array_Field_UnsizedElement_UnknownSize_Builder,
}
impl Serializable for Struct_Array_Field_UnsizedElement_UnknownSizeBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_UnsizedElement_UnknownSizeChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_UnsizedElement_UnknownSizeChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_SizedElement_VariableSize_Padded_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_SizedElement_VariableSize_Padded_View<'a> {
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_4(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(24i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(4usize)?.try_parse()?
            as usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        if self.try_get_array_element_size()? == 0
            || self.try_get_array_size()? % self.try_get_array_element_size()? != 0
        {
            return Err(ParseError::DivisionFailure);
        }
        Ok(self.try_get_array_size()? / self.try_get_array_element_size()?)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i * 8).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(element_size.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?)?;
            curr_view.try_parse()
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = u64> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_SizedElement_VariableSize_Padded_Builder {
    array: Box<[u64]>,
}
impl Serializable for Struct_Array_Field_SizedElement_VariableSize_Padded_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(4usize, || {
            (self.array.len() * 2usize).try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        writer.write_bits(4usize, || Ok(0u64))?;
        for elem in self.array.iter() {
            writer.write_bits(16usize, || {
                (*elem).try_into().or(Err(SerializeError::IntegerConversionFailure))
            })?;
        }
        let most_recent_array_len = 16usize * self.array.len();
        if (most_recent_array_len > 16usize) {
            return Err(SerializeError::NegativePadding);
        }
        for _ in 0..(16usize - most_recent_array_len) {
            writer.write_bits(8, || Ok(0u64))?;
        }
        Ok(())
    }
}
enum Struct_Array_Field_SizedElement_VariableSize_Padded_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_SizedElement_VariableSize_Padded_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_SizedElement_VariableSize_PaddedView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_SizedElement_VariableSize_PaddedView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(24i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(24i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_SizedElement_VariableSize_Padded_View<'a>, ParseError> {
        Struct_Array_Field_SizedElement_VariableSize_Padded_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_SizedElement_VariableSize_Padded_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_SizedElement_VariableSize_PaddedBuilder {
    s: Struct_Array_Field_SizedElement_VariableSize_Padded_Builder,
}
impl Serializable for Struct_Array_Field_SizedElement_VariableSize_PaddedBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_SizedElement_VariableSize_PaddedChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_SizedElement_VariableSize_PaddedChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_UnsizedElement_VariableCount_Padded_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_UnsizedElement_VariableCount_Padded_View<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add(
                self.try_get_array_size()?.checked_mul(8).ok_or(ParseError::ArithmeticOverflow)?,
            )
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(24i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_custom_offset_1()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_array_count()? {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size * 8)?;
        }
        Ok(size)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(8usize)?.try_parse()?
            as usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<UnsizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let mut view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        Ok((0..count).map(move |i| {
            let parsed = UnsizedStructView::try_parse(view.into())?;
            view = view.offset(parsed.try_get_size()? * 8)?;
            Ok(parsed)
        }))
    }
    #[inline]
    pub fn get_array_iter(&self) -> impl Iterator<Item = UnsizedStructView<'a>> + 'a {
        self.try_get_array_iter().unwrap().map(|x| x.unwrap())
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
        for elem in self.try_get_array_iter()? {
            elem?;
        }
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_UnsizedElement_VariableCount_Padded_Builder {
    array: Box<[UnsizedStructBuilder]>,
}
impl Serializable for Struct_Array_Field_UnsizedElement_VariableCount_Padded_Builder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        writer.write_bits(8usize, || {
            self.array.len().try_into().or(Err(SerializeError::IntegerConversionFailure))
        })?;
        let mut most_recent_array_len = 0;
        for elem in self.array.iter() {
            most_recent_array_len += elem.size_in_bits()?;
            elem.serialize(writer)?;
        }
        if (most_recent_array_len > 16usize) {
            return Err(SerializeError::NegativePadding);
        }
        for _ in 0..(16usize - most_recent_array_len) {
            writer.write_bits(8, || Ok(0u64))?;
        }
        Ok(())
    }
}
enum Struct_Array_Field_UnsizedElement_VariableCount_Padded_Child {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_UnsizedElement_VariableCount_Padded_Child {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_UnsizedElement_VariableCount_PaddedView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_UnsizedElement_VariableCount_PaddedView<'a> {
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(24i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(24i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_s(
        &self,
    ) -> Result<Struct_Array_Field_UnsizedElement_VariableCount_Padded_View<'a>, ParseError> {
        Struct_Array_Field_UnsizedElement_VariableCount_Padded_View::try_parse(
            self.buf.offset(self.try_get_s_offset()?)?.into(),
        )
    }
    #[inline]
    pub fn get_s(&self) -> Struct_Array_Field_UnsizedElement_VariableCount_Padded_View<'a> {
        self.try_get_s().unwrap()
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
        self.try_get_s()?;
        Ok(())
    }
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
    }
}
pub struct Struct_Array_Field_UnsizedElement_VariableCount_PaddedBuilder {
    s: Struct_Array_Field_UnsizedElement_VariableCount_Padded_Builder,
}
impl Serializable for Struct_Array_Field_UnsizedElement_VariableCount_PaddedBuilder {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        self.s.serialize(writer)?;
        Ok(())
    }
}
enum Struct_Array_Field_UnsizedElement_VariableCount_PaddedChild {
    RawData(Box<[u8]>),
}
impl Serializable for Struct_Array_Field_UnsizedElement_VariableCount_PaddedChild {
    fn serialize(&self, writer: &mut impl BitWriter) -> Result<(), SerializeError> {
        match self {
            Self::RawData(data) => {
                for byte in data.iter() {
                    writer.write_bits(8, || Ok(*byte as u64))?;
                }
            }
        }
        Ok(())
    }
}
