#include <cassert>
#include <cinttypes>
#include <cstddef>
#include <optional>
#include <type_traits>

namespace packet {

struct BasePacket {
  const uint8_t* buf;
  uint64_t size;

  uint64_t get__payload__offset() const { return 0; }
  uint64_t get__payload__offset_end() const { return size * 8; }

  static BasePacket parse(const uint8_t* buf, uint64_t size) {
    return BasePacket{.buf = buf, .size = size};
  }
};

template <typename T>
inline T GetTypeFromBuffer(const uint8_t* buf, uint64_t start_offset,
                           uint64_t end_offset, uint64_t bit_offset,
                           uint64_t bits_to_extract, bool already_valid,
                           bool& is_corrupt) {
  static_assert(std::is_pod<T>::value, "T must be POD");
  T out{};
  if (!already_valid) {
    // check we are reading within the bounds of the buffer
    if (end_offset < start_offset || bit_offset < start_offset ||
        bit_offset >= end_offset) {
      is_corrupt = true;
      return out;
    }
    // careful to protect against overflow
    if (bits_to_extract > end_offset) {
      is_corrupt = true;
      return out;
    }
    // no overflow here because of the previous check
    if (bit_offset > end_offset - bits_to_extract) {
      is_corrupt = true;
      return out;
    }
  }

  // fields that fit into a uint64_t don't need to be byte-aligned
  if (bits_to_extract <= 64) {
    // loop over all relevant bytes and shift into accumulator
    uint64_t accumulator = 0;
    uint64_t curr_byte_index = bit_offset / 8;
    uint64_t curr_bit_offset = bit_offset % 8;
    uint64_t remaining_bits = bits_to_extract;
    while (remaining_bits) {
      // how many bits to take from the current byte?
      // check if this is the last byte
      if (curr_bit_offset + remaining_bits <= 8) {
        uint64_t tmp = (buf[curr_byte_index] >> curr_bit_offset) &
                       ((1U << remaining_bits) - 1);
        accumulator += tmp << (bits_to_extract - remaining_bits);
        break;
      } else {
        // this is not the last byte, so we have 8 - curr_bit_offset bits to
        // consume
        uint64_t bits_to_consume = 8 - curr_bit_offset;
        uint64_t tmp = buf[curr_byte_index] >> curr_bit_offset;
        accumulator += tmp << (bits_to_extract - remaining_bits);
        curr_bit_offset = 0;
        curr_byte_index += 1;
        remaining_bits -= bits_to_consume;
      }
    }
    // relies on us being little-endian
    memcpy(&out, &accumulator, sizeof(out));
  } else {
    if (!already_valid) {
      if (bit_offset % 8 || bits_to_extract % 8) {
        is_corrupt = true;
        return out;
      }
    }
    memcpy(&out, buf + bit_offset / 8, bits_to_extract / 8);
  }
  return out;
};

}  // namespace packet
