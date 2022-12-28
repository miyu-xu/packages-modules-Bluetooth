#include <cassert>
#include <cinttypes>
#include <cstddef>
#include <optional>
#include <type_traits>

namespace packet {

struct BasePacket {
  uint8_t* buf;
  uint64_t size;

  uint64_t get_payload_offset() { return 0; }
  uint64_t get_payload_size() { return size; }
};

template <typename T>
inline T GetTypeFromBuffer(uint8_t const* buf, uint64_t start_offset,
                           uint64_t end_offset, uint64_t bit_offset,
                           uint64_t bits_to_extract, bool check_valid,
                           bool& is_corrupt) {
  static_assert(std::is_pod<T>::value, "T must be POD");
  T out;
  if (check_valid) {
    // check we are reading within the bounds of the buffer
    if (bit_offset < start_offset || bit_offset >= end_offset) {
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

  // sub-byte fields don't have to be aligned, but they must live within a
  // single byte
  if (bits_to_extract < 8) {
    uint8_t tmp =
        (buf[bit_offset / 8] >> (bit_offset % 8)) & (1 << bits_to_extract - 1);
  } else {
    if (check_valid) {
      if (bit_offset % 8) {
        is_corrupt = true;
        return out;
      }
    }
    memcpy(&out, buf + offset / 8, sizeof(T));
  }
  return out;
};

}  // namespace packet