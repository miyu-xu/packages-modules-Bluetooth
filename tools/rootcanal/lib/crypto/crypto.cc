
#include "crypto/crypto.h"

#include <algorithm>
#include <cstdint>

#include "aes.h"

namespace rootcanal::crypto {

/* This function computes AES_128(key, message) */
Octet16 aes_128(const Octet16& key, const Octet16& message) {
  Octet16 key_reversed;
  Octet16 message_reversed;
  Octet16 output;

  std::reverse_copy(key.begin(), key.end(), key_reversed.begin());
  std::reverse_copy(message.begin(), message.end(), message_reversed.begin());

  aes_context ctx;
  aes_set_key(key_reversed.data(), key_reversed.size(), &ctx);
  aes_encrypt(message_reversed.data(), output.data(), &ctx);

  std::reverse(output.begin(), output.end());
  return output;
}

}  // namespace rootcanal::crypto
