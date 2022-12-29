#include <gtest/gtest.h>

#include "test_packets.h"

uint8_t hexCharToWord(char hex) {
  if ('0' <= hex && hex <= '9') {
    return hex - '0';
  } else if ('A' <= hex && hex <= 'F') {
    return hex - 'A' + 0xa;
  } else {
    return hex - 'a' + 0xa;
  }
}

std::vector<uint8_t> hexToByteString(const char* hex, size_t len) {
  auto out = std::vector<uint8_t>(len);
  for (size_t i = 0; i != len; i += 1) {
    uint8_t high = hexCharToWord(hex[2 * i]);
    uint8_t low = hexCharToWord(hex[2 * i + 1]);
    out[i] = low + (high << 4);
  }
  return out;
}
