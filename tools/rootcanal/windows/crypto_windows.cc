/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <bcrypt.h>
#include <windows.h>

#include <algorithm>
#include <array>
#include <stdexcept>
#include <vector>

#include "crypto/crypto.h"

namespace rootcanal::crypto {

namespace {

void CheckStatus(NTSTATUS status, const char* operation) {
  if (status < 0) {
    throw std::runtime_error(operation);
  }
}

}  // namespace

Octet16 aes_128(const Octet16& key, const Octet16& message) {
  Octet16 key_reversed;
  Octet16 message_reversed;
  Octet16 output;
  std::reverse_copy(key.begin(), key.end(), key_reversed.begin());
  std::reverse_copy(message.begin(), message.end(), message_reversed.begin());

  BCRYPT_ALG_HANDLE algorithm = nullptr;
  BCRYPT_KEY_HANDLE key_handle = nullptr;
  try {
    CheckStatus(BCryptOpenAlgorithmProvider(&algorithm, BCRYPT_AES_ALGORITHM, nullptr, 0),
                "BCryptOpenAlgorithmProvider failed");
    CheckStatus(
            BCryptSetProperty(algorithm, BCRYPT_CHAINING_MODE,
                              reinterpret_cast<PUCHAR>(const_cast<wchar_t*>(BCRYPT_CHAIN_MODE_ECB)),
                              sizeof(BCRYPT_CHAIN_MODE_ECB), 0),
            "BCryptSetProperty failed");
    ULONG object_size = 0;
    ULONG copied = 0;
    CheckStatus(BCryptGetProperty(algorithm, BCRYPT_OBJECT_LENGTH,
                                  reinterpret_cast<PUCHAR>(&object_size), sizeof(object_size),
                                  &copied, 0),
                "BCryptGetProperty failed");
    std::vector<uint8_t> key_object(object_size);
    CheckStatus(BCryptGenerateSymmetricKey(algorithm, &key_handle, key_object.data(), object_size,
                                           key_reversed.data(), key_reversed.size(), 0),
                "BCryptGenerateSymmetricKey failed");
    ULONG output_size = 0;
    CheckStatus(BCryptEncrypt(key_handle, message_reversed.data(), message_reversed.size(), nullptr,
                              nullptr, 0, output.data(), output.size(), &output_size, 0),
                "BCryptEncrypt failed");
    if (output_size != output.size()) {
      throw std::runtime_error("BCryptEncrypt returned an invalid AES block size");
    }
  } catch (...) {
    if (key_handle != nullptr) {
      BCryptDestroyKey(key_handle);
    }
    if (algorithm != nullptr) {
      BCryptCloseAlgorithmProvider(algorithm, 0);
    }
    throw;
  }
  BCryptDestroyKey(key_handle);
  BCryptCloseAlgorithmProvider(algorithm, 0);
  std::reverse(output.begin(), output.end());
  return output;
}

}  // namespace rootcanal::crypto
