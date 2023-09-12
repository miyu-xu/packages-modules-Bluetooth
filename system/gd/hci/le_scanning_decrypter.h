/*
 * Copyright 2023 The Android Open Source Project
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
#pragma once

#include <gtest/gtest_prod.h>

#include <cstdint>
#include <list>
#include <map>
#include <optional>
#include <vector>

#include "hci/address_with_type.h"
#include "hci/hci_packets.h"

/// The LE Scanning Decrypter is responsible for decrypting the any incoming
/// encrypted data over the air. We then reassemble the data after decryption takes place

namespace bluetooth::hci {

class LeScanningDecrypter {
    public:
    LeScanningDecrypter(){};
    LeScanningDecrypter(const LeScanningDecrypter&) = delete;

    bool ExtractEncryptedData(
        std::vector<uint8_t> &adv_data,
        std::vector<uint8_t> &enc_key_material,
        std::vector<uint8_t> *adv_data_decrypted,
        bool *encrypted_data);

    static std::map<int, int> GetEncAdvFieldsInfo(const uint8_t* ad, size_t ad_len);

    void DecryptEncryptedData(
        std::vector<uint8_t> &adv_data,
        std::map<int, int> &enc_adv_data_map,
        std::vector<uint8_t> &enc_key_material,
        std::map<int, std::vector<uint8_t>> *decrypted_data_map);
};

} //namespace bluetooth::hci