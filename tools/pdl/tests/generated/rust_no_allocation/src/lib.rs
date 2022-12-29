#![allow(non_snake_case)]

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
        self.slice_to(self.start_bit_offset.checked_add(len).ok_or(ParseError::ArithmeticOverflow)?)
    }

    pub fn slice_to(&self, end_bit_offset: usize) -> Result<SizedBitSlice<'a>, ParseError> {
        if self.end_bit_offset - self.start_bit_offset < end_bit_offset {
            return Err(ParseError::OutOfBoundsAccess);
        }
        Ok(SizedBitSlice(Self {
            backing: self.backing,
            start_bit_offset: self.start_bit_offset,
            end_bit_offset: self
                .start_bit_offset
                .checked_add(end_bit_offset)
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
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
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

#[derive(Clone, Copy, Debug)]
pub struct UnsizedStructView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> UnsizedStructView<'a> {
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_array_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(6i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(2i64 as isize)
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
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(2usize)?.try_parse()?
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct ScalarParentView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> ScalarParentView<'a> {
    #[inline]
    fn try_get__payload__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get__payload__end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get__payload__size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
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
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get__payload__size()?)
            .ok_or(ParseError::ArithmeticOverflow)
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
        self.buf
            .offset(self.try_get__payload__offset()?)?
            .slice_to(self.try_get__payload__end_offset()?)
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

#[derive(Clone, Copy, Debug)]
pub struct EnumParentView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> EnumParentView<'a> {
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get__payload__end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get__payload__size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get__payload__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
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
            .checked_add(self.try_get__payload__size()?)
            .ok_or(ParseError::ArithmeticOverflow)
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
        self.buf
            .offset(self.try_get__payload__offset()?)?
            .slice_to(self.try_get__payload__end_offset()?)
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
    fn try_get__payload__offset(&self) -> Result<usize, ParseError> {
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
    fn try_get__payload__end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_payload(&self) -> Result<SizedBitSlice<'a>, ParseError> {
        self.buf
            .offset(self.try_get__payload__offset()?)?
            .slice_to(self.try_get__payload__end_offset()?)
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
    fn try_get__payload__end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
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
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(5i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
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
        self.buf.offset(self.try_get_a_offset()?)?.slice(5usize)?.try_parse()
    }
    #[inline]
    pub fn get_a(&self) -> u64 {
        self.try_get_a().unwrap()
    }
    fn try_get_payload(&self) -> Result<SizedBitSlice<'a>, ParseError> {
        self.buf
            .offset(self.try_get__payload__offset()?)?
            .slice_to(self.try_get__payload__end_offset()?)
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
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
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
    fn try_get__payload__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get__payload__end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(12i64 as isize)
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
        self.buf
            .offset(self.try_get__payload__offset()?)?
            .slice_to(self.try_get__payload__end_offset()?)
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Scalar_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Scalar_FieldView<'a> {
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
    fn try_get_c_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Enum_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Enum_FieldView<'a> {
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(57i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(7i64 as isize)
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Reserved_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Reserved_FieldView<'a> {
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
    fn try_get_c_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
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
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Size_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Size_FieldView<'a> {
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
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
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_b_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Count_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Count_FieldView<'a> {
    #[inline]
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
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
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_b_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(3i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_b_count(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(3usize)?.try_parse()?
            as usize)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_FixedScalar_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_FixedScalar_FieldView<'a> {
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
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Payload_Field_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Payload_Field_VariableSizeView<'a> {
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
    fn try_get__payload__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get__payload__size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get__payload__end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get__payload__size()?)
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
    fn try_get__payload__size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(3usize)?.try_parse()?
            as usize)
    }
    fn try_get_payload(&self) -> Result<SizedBitSlice<'a>, ParseError> {
        self.buf
            .offset(self.try_get__payload__offset()?)?
            .slice_to(self.try_get__payload__end_offset()?)
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
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
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
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(-16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_payload(&self) -> Result<SizedBitSlice<'a>, ParseError> {
        self.buf
            .offset(self.try_get__payload__offset()?)?
            .slice_to(self.try_get__payload__end_offset()?)
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
    fn try_get__payload__end_offset(&self) -> Result<usize, ParseError> {
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
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
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
        self.buf
            .offset(self.try_get__payload__offset()?)?
            .slice_to(self.try_get__payload__end_offset()?)
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Body_Field_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Body_Field_VariableSizeView<'a> {
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
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get__body__size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(5i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get__body__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get__body__end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get__body__size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get__body__size(&self) -> Result<usize, ParseError> {
        Ok(self.buf.offset(self.try_get_header_start_offset()?)?.slice(3usize)?.try_parse()?
            as usize)
    }
    fn try_get_payload(&self) -> Result<SizedBitSlice<'a>, ParseError> {
        self.buf.offset(self.try_get__body__offset()?)?.slice_to(self.try_get__body__end_offset()?)
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
    fn try_get__body__end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get__body__offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
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
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(-16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_payload(&self) -> Result<SizedBitSlice<'a>, ParseError> {
        self.buf.offset(self.try_get__body__offset()?)?.slice_to(self.try_get__body__end_offset()?)
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
    fn try_get__payload__end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
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
        self.buf
            .offset(self.try_get__payload__offset()?)?
            .slice_to(self.try_get__payload__end_offset()?)
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Struct_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Struct_FieldView<'a> {
    #[inline]
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add(self.try_get_b_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_b_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_custom_offset_1()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size)?;
        }
        Ok(size)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_ByteElement_ConstantSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_ByteElement_ConstantSizeView<'a> {
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
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_ByteElement_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_ByteElement_VariableSizeView<'a> {
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
            .checked_add(self.try_get_array_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_ByteElement_VariableCountView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_ByteElement_VariableCountView<'a> {
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_array_size()?)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
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
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()? - self.try_get_trailer_start_offset()?)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_ScalarElement_ConstantSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_ScalarElement_ConstantSizeView<'a> {
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
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(4usize)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_ScalarElement_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_ScalarElement_VariableSizeView<'a> {
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
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_array_size()?)
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
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_ScalarElement_VariableCountView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_ScalarElement_VariableCountView<'a> {
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
            .checked_add(self.try_get_array_size()?)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
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
        Ok(self.try_get_header_start_offset()? - self.try_get_trailer_start_offset()?)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_EnumElement_ConstantSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_EnumElement_ConstantSizeView<'a> {
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
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(8usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<Enum16, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_EnumElement_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_EnumElement_VariableSizeView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_array_size()?)
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
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
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
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
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
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<Enum16, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_EnumElement_VariableCountView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_EnumElement_VariableCountView<'a> {
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
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_array_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
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
    ) -> Result<impl Iterator<Item = Result<Enum16, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()? - self.try_get_trailer_start_offset()?)
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
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<Enum16, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_SizedElement_ConstantSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_SizedElement_ConstantSizeView<'a> {
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
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
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
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<SizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_SizedElement_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_SizedElement_VariableSizeView<'a> {
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
            .checked_add(self.try_get_array_size()?)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_SizedElement_VariableCountView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_SizedElement_VariableCountView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_array_size()?)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()? - self.try_get_trailer_start_offset()?)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_UnsizedElement_ConstantSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_UnsizedElement_ConstantSizeView<'a> {
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(self.try_get_array_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_array_count()? {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size)?;
        }
        Ok(size)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(4usize)
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_UnsizedElement_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_UnsizedElement_VariableSizeView<'a> {
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_array_size()?)
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
        let mut cnt = 0;
        let mut view = self.buf.offset(self.try_get_custom_offset_2()?)?;
        let mut remaining_size = self.try_get_array_size()?;
        while remaining_size > 0 {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            if next_struct_size > remaining_size {
                return Err(ParseError::OutOfBoundsAccess);
            }
            remaining_size -= next_struct_size;
            view = view.offset(next_struct_size)?;
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
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
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
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_array_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_custom_offset_2()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_array_count()? {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size)?;
        }
        Ok(size)
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
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
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
            view = view.offset(next_struct_size)?;
            cnt += 1;
        }
        Ok(cnt)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()? - self.try_get_trailer_start_offset()?)
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_SizedElement_VariableSize_PaddedView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_SizedElement_VariableSize_PaddedView<'a> {
    #[inline]
    fn try_get_custom_offset_4(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
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
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_array_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Packet_Array_Field_UnsizedElement_VariableCount_PaddedView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Packet_Array_Field_UnsizedElement_VariableCount_PaddedView<'a> {
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
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add(self.try_get_array_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_custom_offset_1()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_array_count()? {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size)?;
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
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(8i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
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
    fn try_get_c_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
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

#[derive(Clone, Copy, Debug)]
pub struct EnumChild_BView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> EnumChild_BView<'a> {
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
    pub fn try_parse<'b>(parent: EnumParentView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
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

#[derive(Clone, Copy, Debug)]
pub struct AliasedChild_BView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> AliasedChild_BView<'a> {
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
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
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
    pub fn try_parse<'b>(parent: EmptyParentView<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent.try_get_payload().unwrap().into() };
        out.validate()?;
        Ok(out)
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
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(11i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
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

#[derive(Clone, Copy, Debug)]
pub struct PartialChild5_BView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> PartialChild5_BView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(27i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
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

#[derive(Clone, Copy, Debug)]
pub struct PartialChild12_AView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> PartialChild12_AView<'a> {
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
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
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

#[derive(Clone, Copy, Debug)]
pub struct PartialChild12_BView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> PartialChild12_BView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(20i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Scalar_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Scalar_FieldView<'a> {
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
    fn try_get_c_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
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
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Enum_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Enum_FieldView<'a> {
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
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Reserved_Field_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Reserved_Field_View<'a> {
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
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_c_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Reserved_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Reserved_FieldView<'a> {
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Size_Field_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Size_Field_View<'a> {
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
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(3i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_a_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_b_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Size_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Size_FieldView<'a> {
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
            .checked_add(self.try_get_s_size()?)
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
            let next_struct_size = Struct_Size_Field_View::try_parse(view)?.try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size)?;
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
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(3i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_b_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_b_size(&self) -> Result<usize, ParseError> {
        self.try_get_b_count()?
            .checked_mul(self.try_get_b_element_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Count_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Count_FieldView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(self.try_get_s_size()?)
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
            view = view.offset(next_struct_size)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_FixedScalar_Field_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_FixedScalar_Field_View<'a> {
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(57i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
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
    fn try_get_b_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_FixedScalar_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_FixedScalar_FieldView<'a> {
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Struct_FieldView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Struct_FieldView<'a> {
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
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add(self.try_get_b_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
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
    fn try_get_b_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_custom_offset_1()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ByteElement_ConstantSize_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ByteElement_ConstantSize_View<'a> {
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
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
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
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ByteElement_ConstantSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ByteElement_ConstantSizeView<'a> {
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ByteElement_VariableSize_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ByteElement_VariableSize_View<'a> {
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
            .checked_add(self.try_get_array_size()?)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(self.try_get_s_size()?)
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
                Struct_Array_Field_ByteElement_VariableSize_View::try_parse(view)?
                    .try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ByteElement_VariableCount_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ByteElement_VariableCount_View<'a> {
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
            .checked_add(self.try_get_array_size()?)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ByteElement_VariableCountView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ByteElement_VariableCountView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(self.try_get_s_size()?)
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
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size =
                Struct_Array_Field_ByteElement_VariableCount_View::try_parse(view)?
                    .try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size)?;
        }
        Ok(size)
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
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
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
        Ok(self.try_get_header_start_offset()? - self.try_get_trailer_start_offset()?)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()? - self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ScalarElement_ConstantSize_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ScalarElement_ConstantSize_View<'a> {
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
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(2usize)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ScalarElement_VariableSize_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ScalarElement_VariableSize_View<'a> {
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
            .checked_add(self.try_get_array_size()?)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ScalarElement_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ScalarElement_VariableSizeView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(self.try_get_s_size()?)
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
                Struct_Array_Field_ScalarElement_VariableSize_View::try_parse(view)?
                    .try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size)?;
        }
        Ok(size)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ScalarElement_VariableCount_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ScalarElement_VariableCount_View<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
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
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_array_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_ScalarElement_VariableCountView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_ScalarElement_VariableCountView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(self.try_get_s_size()?)
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
                Struct_Array_Field_ScalarElement_VariableCount_View::try_parse(view)?
                    .try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size)?;
        }
        Ok(size)
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
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
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
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()? - self.try_get_trailer_start_offset()?)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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
        Ok(self.try_get_header_start_offset()? - self.try_get_trailer_start_offset()?)
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_EnumElement_ConstantSize_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_EnumElement_ConstantSize_View<'a> {
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
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(64i64 as isize)
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
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(8usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<Enum16, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_EnumElement_ConstantSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_EnumElement_ConstantSizeView<'a> {
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
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_array_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_EnumElement_VariableSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_EnumElement_VariableSizeView<'a> {
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
            .checked_add(self.try_get_s_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
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
            view = view.offset(next_struct_size)?;
        }
        Ok(size)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_EnumElement_VariableCount_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_EnumElement_VariableCount_View<'a> {
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
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_array_size()?)
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
        self.try_get_array_count()?
            .checked_mul(self.try_get_array_element_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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
            .checked_add(self.try_get_s_size()?)
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
            view = view.offset(next_struct_size)?;
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
        Ok(self.try_get_header_start_offset()? - self.try_get_trailer_start_offset()?)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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
    fn try_get_trailer_start_offset(&self) -> Result<usize, ParseError> {
        self.try_get_packet_end_offset()?
            .checked_add_signed(0i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
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
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()? - self.try_get_trailer_start_offset()?)
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
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
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
    ) -> Result<impl Iterator<Item = Result<SizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_SizedElement_ConstantSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_SizedElement_ConstantSizeView<'a> {
    #[inline]
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_SizedElement_VariableSize_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_SizedElement_VariableSize_View<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_array_size()?)
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
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_3()?)
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
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<SizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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
            .checked_add(self.try_get_s_size()?)
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
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size =
                Struct_Array_Field_SizedElement_VariableSize_View::try_parse(view)?
                    .try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size)?;
        }
        Ok(size)
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
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_array_size()?)
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
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        self.try_get_array_count()?
            .checked_mul(self.try_get_array_element_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_element_size(&self) -> Result<usize, ParseError> {
        Ok(1usize)
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
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<SizedStructView<'a>, ParseError>> + 'a, ParseError>
    {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(self.try_get_s_size()?)
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
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size =
                Struct_Array_Field_SizedElement_VariableCount_View::try_parse(view)?
                    .try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size)?;
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
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
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
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()? - self.try_get_trailer_start_offset()?)
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
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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
    fn try_get_s_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
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
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()? - self.try_get_trailer_start_offset()?)
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_UnsizedElement_ConstantSize_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_UnsizedElement_ConstantSize_View<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(self.try_get_array_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_count(&self) -> Result<usize, ParseError> {
        Ok(4usize)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_array_count()? {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_UnsizedElement_ConstantSizeView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_UnsizedElement_ConstantSizeView<'a> {
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(self.try_get_s_size()?)
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
                Struct_Array_Field_UnsizedElement_ConstantSize_View::try_parse(view)?
                    .try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_UnsizedElement_VariableSize_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_UnsizedElement_VariableSize_View<'a> {
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_array_size()?)
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
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
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
        let mut cnt = 0;
        let mut view = self.buf.offset(self.try_get_custom_offset_2()?)?;
        let mut remaining_size = self.try_get_array_size()?;
        while remaining_size > 0 {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            if next_struct_size > remaining_size {
                return Err(ParseError::OutOfBoundsAccess);
            }
            remaining_size -= next_struct_size;
            view = view.offset(next_struct_size)?;
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
    pub fn try_parse<'b>(parent: BitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
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
            .checked_add(self.try_get_s_size()?)
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
            view = view.offset(next_struct_size)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_UnsizedElement_VariableCount_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_UnsizedElement_VariableCount_View<'a> {
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_array_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
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
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_2()?)
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
            view = view.offset(next_struct_size)?;
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_UnsizedElement_VariableCountView<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_UnsizedElement_VariableCountView<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_custom_offset_1()?)
    }
    #[inline]
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add(self.try_get_s_size()?)
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
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_header_start_offset()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_custom_value_0()? {
            let next_struct_size =
                Struct_Array_Field_UnsizedElement_VariableCount_View::try_parse(view)?
                    .try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size)?;
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
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_trailer_start_offset()?)
    }
    #[inline]
    fn try_get_array_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
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
            view = view.offset(next_struct_size)?;
            cnt += 1;
        }
        Ok(cnt)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()? - self.try_get_trailer_start_offset()?)
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
    pub fn try_parse<'b>(parent: SizedBitSlice<'a>) -> Result<Self, ParseError> {
        let out = Self { buf: parent };
        out.validate()?;
        Ok(out)
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
    fn try_get_s_offset(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()?)
    }
    #[inline]
    fn try_get_custom_value_0(&self) -> Result<usize, ParseError> {
        Ok(1usize)
    }
    #[inline]
    fn try_get_s_size(&self) -> Result<usize, ParseError> {
        Ok(self.try_get_header_start_offset()? - self.try_get_trailer_start_offset()?)
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

#[derive(Clone, Copy, Debug)]
pub struct Struct_Array_Field_SizedElement_VariableSize_Padded_View<'a> {
    buf: BitSlice<'a>,
}
impl<'a> Struct_Array_Field_SizedElement_VariableSize_Padded_View<'a> {
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(24i64 as isize)
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
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(4i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add(self.try_get_array_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_4(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_2()?
            .checked_add_signed(16i64 as isize)
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
    fn try_get_array_iter(
        &self,
    ) -> Result<impl Iterator<Item = Result<u64, ParseError>> + 'a, ParseError> {
        let view = self.buf.offset(self.try_get_array_offset()?)?;
        let count = self.try_get_array_count()?;
        let element_size = self.try_get_array_element_size()?;
        Ok((0..count).map(move |i| {
            let curr_view = view
                .offset(element_size.checked_mul(i).ok_or(ParseError::ArithmeticOverflow)?)?
                .slice(i)?;
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
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(24i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_2(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add(self.try_get_array_size()?)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_custom_offset_3(&self) -> Result<usize, ParseError> {
        self.try_get_custom_offset_1()?
            .checked_add_signed(16i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_array_size(&self) -> Result<usize, ParseError> {
        let mut view = self.buf.offset(self.try_get_custom_offset_1()?)?;
        let mut size = 0;
        for _ in 0..self.try_get_array_count()? {
            let next_struct_size = UnsizedStructView::try_parse(view)?.try_get_size()?;
            size += next_struct_size;
            view = view.offset(next_struct_size)?;
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
    fn try_get_custom_offset_1(&self) -> Result<usize, ParseError> {
        self.try_get_header_start_offset()?
            .checked_add_signed(24i64 as isize)
            .ok_or(ParseError::ArithmeticOverflow)
    }
    #[inline]
    fn try_get_packet_end_offset(&self) -> Result<usize, ParseError> {
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
