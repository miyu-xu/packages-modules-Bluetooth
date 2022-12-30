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
            self.buf.push(self.curr_byte);
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

        println!("writing {val:b} into buffer of size {num_bits} bits");
        if num_bits < 64 && val >= 1 << num_bits {
            println!("num_bits={num_bits}, val={val}");
            return Err(SerializeError::ValueTooLarge);
        }

        let mut remaining_val = val;
        let mut remaining_bits = num_bits;
        while remaining_bits > 0 {
            let remaining_bits_in_curr_byte = (8 - self.curr_bit_offset) as usize;
            if remaining_bits < remaining_bits_in_curr_byte {
                // we cannot finish the last byte
                println!("writing final {remaining_bits} bits of {remaining_val:b} into byte currently holding {:b}", self.curr_byte);
                self.curr_byte = (self.curr_byte << remaining_bits) + (remaining_val as u8);
                self.curr_bit_offset += remaining_bits as u8;
                break;
            } else {
                // finish up our current byte and move on
                println!("writing {remaining_bits_in_curr_byte} bits of {remaining_val:b} to fill up byte currently holding {:b}", self.curr_byte);
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
