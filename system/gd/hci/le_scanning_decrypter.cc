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
#include "hci/le_scanning_decrypter.h"

#include <base/strings/string_number_conversions.h>
#include <openssl/aead.h>
#include <openssl/base.h>
#include <openssl/rand.h>

#include <memory>
#include <unordered_map>

#include "hci/acl_manager.h"
#include "hci/controller.h"
#include "hci/hci_layer.h"
#include "hci/hci_packets.h"
#include "hci/le_periodic_sync_manager.h"
#include "hci/le_scanning_interface.h"
#include "hci/vendor_specific_event_manager.h"
#include "module.h"
#include "os/handler.h"
#include "os/log.h"
#include "os/system_properties.h"
#include "storage/storage_module.h"


namespace bluetooth::hci {

/// Iterate over the advertising data, and attempts to decrypt
/// Encrypted Data AD types using the provided key material.
/// Stores the completely decrypted data in adv_data_decrypted.
/// Returns true if all encrypted data was successfully decrypted,
/// false if any Encrypted Data could not be read.
bool LeScanningDecrypter::ExtractEncryptedData(
    std::vector<uint8_t> const &adv_data,
    std::vector<uint8_t> const &enc_key_material,
    std::vector<uint8_t> *adv_data_decrypted,
    bool *has_encrypted_data) {

  *has_encrypted_data = false;
  bool is_decryption_success = false;
  std::map<int, int> enc_adv_data_map;

  enc_adv_data_map =
          GetEncAdvFieldsInfo(adv_data.data(), adv_data.size());
  if(!enc_adv_data_map.empty()) {
    *has_encrypted_data = true;
    LOG_INFO("Found Encrypted Data: %s",
      base::HexEncode(adv_data.data(), adv_data.size()).c_str());
  }
  else {
    LOG_INFO("enc_adv_data_map is empty");
    return is_decryption_success;
  }

  if (*has_encrypted_data) {
    if (bluetooth::common::init_flags::encrypted_advertising_log_is_enabled()) {
      LOG_INFO(
          "Advertising Data Before Decryption: %s ",
          base::HexEncode(adv_data.data(), adv_data.size())
              .c_str());
    }
    if (bluetooth::common::init_flags::encrypted_advertising_log_is_enabled()) {
      LOG_INFO(
          "ENC_KEY_MATERIAL %s",
          base::HexEncode(enc_key_material.data(), enc_key_material.size()).c_str());
    }

    std::vector<std::pair<std::vector<uint8_t>, std::vector<uint8_t>>> key_and_ivs;

    std::map<int, std::vector<uint8_t>> decrypted_data_map;

    /* Split the string from bt_config.conf file into individual 24 byte Encrypted data
        key material char values and save it in enc_key_material_vec vector */
    std::vector<uint8_t> enc_key;
    for (size_t i = 0; i < enc_key_material.size(); i++) {
      enc_key.push_back(enc_key_material[i]);
      if ((i > 0) && (((i + 1) % ENC_KEY_MATERIAL_LEN) == 0)) {
        std::vector<uint8_t> key(enc_key.begin(), enc_key.begin() + 16);
        std::vector<uint8_t> iv(enc_key.begin() + 16, enc_key.end());
        key_and_ivs.push_back(std::pair(key,iv));
        enc_key.clear();
      }
    }

    if (bluetooth::common::init_flags::encrypted_advertising_log_is_enabled()) {
      // Print the split 24 byte Encrypted data key material char values
      int enc_key_material_count = 0;
      for (auto enc_key_material : key_and_ivs) {
        enc_key_material_count++;
        LOG_INFO(
          "Enc Data Key Vector %d: %s %s",
          enc_key_material_count,
          base::HexEncode(enc_key_material.first.data(), enc_key_material.first.size()).c_str(),
          base::HexEncode(enc_key_material.second.data(), enc_key_material.second.size()).c_str());
      }
    }

    /*Iterate through the multiple enc data key char values to check
        and find the enc key which successfully decrypts the data */
    for (auto enc_key_material : key_and_ivs) {
      std::vector<uint8_t> key = enc_key_material.first;
      std::vector<uint8_t> iv = enc_key_material.second;

      if (bluetooth::common::init_flags::encrypted_advertising_log_is_enabled()) {
        if (!key.empty()) {
          LOG_INFO("Session Key: %s", base::HexEncode(key.data(), key.size()).c_str());
        }
        if (!iv.empty()) {
          LOG_INFO("IV: %s", base::HexEncode(iv.data(), iv.size()).c_str());
        }
      }
      int pos_index = 0;
      int enc_data_part_len = 0;
      for (auto it = enc_adv_data_map.begin(); it != enc_adv_data_map.end(); it++) {
        std::vector<uint8_t> nonce;
        std::vector<uint8_t> MIC;
        std::vector<uint8_t> payload;
        std::vector<uint8_t> randomizer;
        pos_index = it->first;
        enc_data_part_len = it->second;

        if (bluetooth::common::init_flags::encrypted_advertising_log_is_enabled()) {
          LOG_INFO("pos_index: %d enc_data_part_len: %d", +pos_index, +enc_data_part_len);
        }
        std::vector<uint8_t> encrypted_data(
            adv_data.begin() + pos_index, adv_data.begin() + (pos_index + enc_data_part_len));

        std::vector<uint8_t> decrypted_data = DecryptEncryptedData(
                                                  encrypted_data,
                                                  key,
                                                  iv).value();

        // Insert decrypted part vector and position
        if(!decrypted_data.empty()) {
          decrypted_data_map.insert(std::pair<int, std::vector<uint8_t>>(pos_index, decrypted_data));
        }
      }
      if (bluetooth::common::init_flags::encrypted_advertising_log_is_enabled()) {
      // Print decrypted_data_map
        for (auto it1 = decrypted_data_map.begin(); it1 != decrypted_data_map.end(); it1++) {
          LOG_INFO("decrypted_data_map: position: %d ", +(it1->first));
          std::vector<uint8_t> vec_temp = it1->second;
          LOG_INFO(
              "decrypted_data_map vector: %s",
              base::HexEncode(vec_temp.data(), vec_temp.size()).c_str());
        }
      }

      if (!decrypted_data_map.empty() && !enc_adv_data_map.empty()) {
        std::map<int, std::vector<uint8_t>>::iterator it_decrypted_map;
        std::map<int, int>::iterator it_enc_map;
        is_decryption_success = true;
        it_decrypted_map = decrypted_data_map.begin();
        int enc_data_index = it_decrypted_map->first;
        std::vector<uint8_t> decrypted_part = it_decrypted_map->second;

        it_enc_map = enc_adv_data_map.begin();
        int enc_data_part_len = it_enc_map->second;

        // Copy data from original adv_data to adv_data_decrypted vector
        for (int i = 0; i < (int)adv_data.size(); i++) {
          if (i < enc_data_index) {
            adv_data_decrypted->push_back(adv_data[i]);
          } else if (i == enc_data_index) {
            adv_data_decrypted->insert(
                adv_data_decrypted->end(), decrypted_part.begin(), decrypted_part.end());
            i = i + enc_data_part_len - 1;
            it_decrypted_map++;
            if (it_decrypted_map != decrypted_data_map.end()) {
                enc_data_index = it_decrypted_map->first;
                decrypted_part = it_decrypted_map->second;
            } else {
                enc_data_index = (int)adv_data.size();
            }
            it_enc_map++;
            if (it_enc_map != enc_adv_data_map.end()) {
                enc_data_part_len = it_enc_map->second;
            } else {
                enc_data_part_len = 0;
            }
          }
        }  // end for loop
        break;  // break and no need to iterate through other enc key char values for decryption
      }
    } // for loop for enc_key_value
  }
  return is_decryption_success;
}

/// Identifies Encrypted Advertising Data in the advertising data
/// and stores the position and length of the data in a map.
std::map<int, int> LeScanningDecrypter::GetEncAdvFieldsInfo(const uint8_t* ad, size_t ad_len) {
  size_t position = 0;
  std::map<int, int> enc_adv_map;
  int enc_data_part_length = 0;

  while (position < ad_len) {
    uint8_t len = ad[position];

    if (len == 0) break;
    if (position + len >= ad_len) break;

    uint8_t adv_type = ad[position + 1];

    if (adv_type == (uint8_t)GapDataType::ENCRYPTED_ADVERTISING_DATA) {
      enc_data_part_length = len + 1; /* Length(1 byte) + len */
      enc_adv_map.insert(std::pair<int, int>((int)position, enc_data_part_length));
    }

    position += len + 1; /* skip the length of data */
  }

  return enc_adv_map;
}

/// This function receives the complete adv_data,
/// the map that stores the encrypted advertising data,
/// encrypted data key material, and the decrypted data map
/// that stores the decrypted data. This function initializes
/// the decryption and algorithm and performs decryption
std::optional<std::vector<uint8_t>> LeScanningDecrypter::DecryptEncryptedData(
    std::vector<uint8_t> const &adv_data,
    std::vector<uint8_t> const &key,
    std::vector<uint8_t> const &iv) {

  std::vector<uint8_t> decrypted_data;
  static const std::vector<uint8_t> ad = {0xEA};
  const EVP_AEAD_CTX* aeadCTX = EVP_AEAD_CTX_new(
      EVP_aead_aes_128_ccm_bluetooth(), key.data(), key.size(), EVP_AEAD_DEFAULT_TAG_LENGTH);
  if (aeadCTX == nullptr) return decrypted_data;
  std::vector<uint8_t> nonce;
  std::vector<uint8_t> MIC;
  std::vector<uint8_t> payload;
  std::vector<uint8_t> randomizer;

  for (size_t i = 0; i < adv_data.size(); i++) {
    if ((i >= 2) && (i <=  6)) {
    randomizer.push_back(adv_data[i]);
    } else if ((i >  6) && i < (adv_data.size() - 4)) {
    payload.push_back(adv_data[i]);
    }
    if ((i >= (adv_data.size() - 4)) && (i < adv_data.size())) {
    MIC.push_back(adv_data[i]);
    }
  }

  nonce.insert(nonce.end(), randomizer.begin(), randomizer.end());
  nonce.insert(nonce.end(), iv.rbegin(), iv.rend());

  std::vector<uint8_t> out(payload.size());
  if (bluetooth::common::init_flags::encrypted_advertising_log_is_enabled()) {
    if (!randomizer.empty()) {
      LOG_INFO("Randomizer: %s", base::HexEncode(randomizer.data(), randomizer.size()).c_str());
    }
    if (!nonce.empty()) {
      LOG_INFO("Nonce: %s", base::HexEncode(nonce.data(), nonce.size()).c_str());
    }
    if (!payload.empty()) {
      LOG_INFO("Payload: %s", base::HexEncode(payload.data(), payload.size()).c_str());
    }
    if (!MIC.empty()) {
      LOG_INFO("MIC: %s", base::HexEncode(MIC.data(), MIC.size()).c_str());
    }
  }

  EVP_AEAD_CTX_open_gather(
      aeadCTX,
      out.data(),
      nonce.data(),
      nonce.size(),
      payload.data(),
      payload.size(),
      MIC.data(),
      MIC.size(),
      ad.data(),
      ad.size());
  if (bluetooth::common::init_flags::encrypted_advertising_log_is_enabled()) {
    LOG_INFO("OUT: %s", base::HexEncode(out.data(), out.size()).c_str());
  }
  if (out.size() > 0 && (out[0] > 0)) {
    LOG_INFO("Decryption successful ");

    // construct enc adv data part's (decrypted) vector
    decrypted_data.insert(decrypted_data.begin(), out.begin(), out.end());

  } else {
    LOG_INFO("Decryption NOT successful ");
  }
  return decrypted_data;
}

} // namespace bluetooth::hci