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
#include "hci/le_scanning_reassembler.h"

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
std::list<LeScanningReassembler::PeriodicAdvertisingFragment> periodic_cache_;

std::optional<std::vector<uint8_t>> LeScanningReassembler::ProcessPeriodicAdvertisingReport(
    uint16_t sync_handle,
    AddressWithType Address_with_type,
    DataStatus data_status,
    const std::vector<uint8_t>& periodic_advertising_data) {
  AdvertisingKey key(
      Address_with_type.GetAddress(),
      DirectAdvertisingAddressType(Address_with_type.GetAddressType()),
      (uint8_t)sync_handle);
  std::list<PeriodicAdvertisingFragment>::iterator advertising_fragment =
      AppendPeriodicFragment(sync_handle, periodic_advertising_data);

  if (data_status != DataStatus::CONTINUING) {
    LOG_INFO("DATA COMPLETE");
    advertising_fragment->data = TrimAdvertisingData(advertising_fragment->data);
  } else {
    LOG_INFO("DATA INCOMPLETE");
    return {};
  }
  std::vector<uint8_t> complete_advertising_data = std::move(advertising_fragment->data);
  periodic_cache_.erase(advertising_fragment);
  return complete_advertising_data;
}
std::optional<std::vector<uint8_t>> LeScanningReassembler::ProcessAdvertisingReport(
    uint16_t event_type,
    uint8_t address_type,
    Address address,
    uint8_t advertising_sid,
    const std::vector<uint8_t>& advertising_data,
    std::vector<uint8_t> enc_key_material) {
  bool is_scannable = event_type & (1 << kScannableBit);
  bool is_scan_response = event_type & (1 << kScanResponseBit);
  bool is_legacy = event_type & (1 << kLegacyBit);
  DataStatus data_status = DataStatus((event_type >> kDataStatusBits) & 0x3);
  if (address_type != (uint8_t)DirectAdvertisingAddressType::NO_ADDRESS_PROVIDED &&
      address == Address::kEmpty) {
    LOG_WARN("Ignoring non-anonymous advertising report with empty address");
    return {};
  }
  LOG(INFO) << __func__ << " " << address << " " << (uint16_t)address_type << " "
            << (uint16_t)advertising_sid;
  AdvertisingKey key(address, DirectAdvertisingAddressType(address_type), advertising_sid);

  // Ignore scan responses received without a matching advertising event.
  if (is_scan_response && (ignore_scan_responses_ || !ContainsFragment(key))) {
    LOG_INFO("Ignoring scan response received without advertising event");
    return {};
  }

  // Legacy advertising is always complete, we can drop
  // the previous data as safety measure if the report is not a scan
  // response.
  if (is_legacy && !is_scan_response) {
    LOG_DEBUG("Dropping repeated legacy advertising data");
    RemoveFragment(key);
  }

  // Concatenate the data with existing fragments.
  std::list<AdvertisingFragment>::iterator advertising_fragment =
      AppendFragment(key, advertising_data);

  // Trim the advertising data when the complete payload is received.
  if (data_status != DataStatus::CONTINUING) {
    advertising_fragment->data = TrimAdvertisingData(advertising_fragment->data);
  }

  // TODO(b/272120114) waiting for a scan response here is prone to failure as the
  // SCAN_REQ PDUs can be rejected by the advertiser according to the
  // advertising filter parameter.
  bool expect_scan_response = is_scannable && !is_scan_response && !ignore_scan_responses_;

  // Check if we should wait for additional fragments:
  // - For legacy advertising, when a scan response is expected.
  // - For extended advertising, when the current data is marked
  //   incomplete OR when a scan response is expected.
  if (data_status == DataStatus::CONTINUING || expect_scan_response) {
    return {};
  }

  // Otherwise the full advertising report has been reassembled,
  // removed the cache entry and return the complete advertising data.
  std::vector<uint8_t> complete_advertising_data = std::move(advertising_fragment->data);

  if (bluetooth::common::init_flags::encrypted_advertising_is_enabled()) {
    bool encrypted_data = false;
    bool is_decrypt_success = false;
    std::map<int, int> enc_adv_data_map;
    std::map<int, std::vector<uint8_t>> decrypted_data_map;

    for(int i = 0; i < (int)complete_advertising_data.size(); i++) {
      if (complete_advertising_data[i] == (uint8_t)GapDataType::ENCRYPTED_ADVERTISING_DATA &&
         (int)complete_advertising_data[i-1] + (i) <= (int)complete_advertising_data.size()) {
        encrypted_data = true;
        if (bluetooth::common::init_flags::encrypted_advertising_log_is_enabled() &&
            !enc_key_material.empty()) {
          LOG_INFO(
              "BDA: %s Found Encrypted Data Index %d ",
              ADDRESS_TO_LOGGABLE_CSTR(address),
              i);
        }
      }
    }
    if (bluetooth::common::init_flags::encrypted_advertising_log_is_enabled()) {
      LOG_INFO(
          "BDA: %s Data  %s ",
          ADDRESS_TO_LOGGABLE_CSTR(address),
          base::HexEncode(complete_advertising_data.data(), complete_advertising_data.size())
              .c_str());
    }
    if (encrypted_data) {
      enc_adv_data_map =
          GetEncAdvFieldsInfo(complete_advertising_data.data(), complete_advertising_data.size());
      if (bluetooth::common::init_flags::encrypted_advertising_log_is_enabled()) {
        LOG_INFO(
            " Advertising Data Before Decryption: %s ",
            base::HexEncode(complete_advertising_data.data(), complete_advertising_data.size())
                .c_str());
      }
      std::vector<uint8_t> decrypted_data = ProcessEncryptedData(
          complete_advertising_data, &is_decrypt_success, enc_adv_data_map, enc_key_material);
      if (!is_decrypt_success &&
          bluetooth::common::init_flags::encrypted_advertising_log_is_enabled()) {
        LOG_INFO("Decryption FAILED");
      } else if (!decrypted_data.empty() && is_decrypt_success) {
        if (bluetooth::common::init_flags::encrypted_advertising_log_is_enabled()) {
          LOG_INFO("Decryption PASSED");
        }
        complete_advertising_data.clear();
        complete_advertising_data.insert(
            complete_advertising_data.begin(), decrypted_data.begin(), decrypted_data.end());
      }
    }
  }

  cache_.erase(advertising_fragment);
  return complete_advertising_data;
}

std::vector<uint8_t> LeScanningReassembler::ProcessEncryptedData(
    std::vector<uint8_t> adv_data,
    bool* is_decryption_success,
    std::map<int, int> enc_adv_data_map,
    std::vector<uint8_t> enc_key_material) {
  if (bluetooth::common::init_flags::encrypted_advertising_log_is_enabled()) {
    LOG_INFO(
        "ENC_KEY_MATERIAL %s",
        base::HexEncode(enc_key_material.data(), enc_key_material.size()).c_str());
  }
  std::vector<uint8_t> iv;
  std::vector<uint8_t> key;

  std::vector<std::vector<uint8_t>> enc_key_material_vec;
  std::vector<uint8_t> adv_data_decrypted;

  static const std::vector<uint8_t> ad = {0xEA};
  *is_decryption_success = false;
  std::map<int, std::vector<uint8_t>> decrypted_data_map;
  std::vector<uint8_t> empty_vec;

  /* Split the string from bt_config.conf file into individual 24 byte Encrypted data
     key material char values and save it in enc_key_material_vec vector */
  std::vector<uint8_t> enc_key;
  for (size_t i = 0; i < enc_key_material.size(); i++) {
    enc_key.push_back(enc_key_material[i]);
    if ((i > 0) && (((i + 1) % ENC_KEY_MATERIAL_LEN) == 0)) {
      enc_key_material_vec.push_back(enc_key);
      enc_key.clear();
    }
  }

  if (bluetooth::common::init_flags::encrypted_advertising_log_is_enabled()) {
    // Print the split 24 byte Encrypted data key material char values
    for (size_t k = 0; k < enc_key_material_vec.size(); k++) {
      LOG_INFO(
          "Enc Data Key Vector %d: %s",
          (int)k,
          base::HexEncode(enc_key_material_vec[k].data(), enc_key_material_vec[k].size()).c_str());
    }
  }

  /*Iterate through the multiple enc data key char values to check
    and find the enc key which successfully decrypts the data */
  for (size_t k = 0; k < enc_key_material_vec.size(); k++) {
    key.clear();
    iv.clear();
    key.insert(key.begin(), enc_key_material_vec[k].begin(), enc_key_material_vec[k].begin() + 16);
    iv.insert(iv.begin(), enc_key_material_vec[k].begin() + 16, enc_key_material_vec[k].end());

    if (bluetooth::common::init_flags::encrypted_advertising_log_is_enabled()) {
      if (!key.empty()) {
        LOG_INFO("Session Key: %s", base::HexEncode(key.data(), key.size()).c_str());
      }
      if (!iv.empty()) {
        LOG_INFO("IV: %s", base::HexEncode(iv.data(), iv.size()).c_str());
      }
    }

    const EVP_AEAD_CTX* aeadCTX = EVP_AEAD_CTX_new(
        EVP_aead_aes_128_ccm_bluetooth(), key.data(), key.size(), EVP_AEAD_DEFAULT_TAG_LENGTH);
    if (aeadCTX == nullptr) return empty_vec;
    int pos_index = 0;
    int enc_data_part_len = 0;

    if (enc_adv_data_map.empty()) {
      LOG_INFO("enc_adv_data_map is empty");
      return empty_vec;
    }

    std::map<int, int>::iterator it;
    if (bluetooth::common::init_flags::encrypted_advertising_log_is_enabled()) {
      for (it = enc_adv_data_map.begin(); it != enc_adv_data_map.end(); it++) {
        LOG_INFO("enc_adv_data_map position: %d length: %d", +(it->first), +(it->second));
      }
    }

    for (it = enc_adv_data_map.begin(); it != enc_adv_data_map.end(); it++) {
      std::vector<uint8_t> nonce;
      std::vector<uint8_t> MIC;
      std::vector<uint8_t> payload;
      std::vector<uint8_t> randomizer;
      pos_index = it->first;
      enc_data_part_len = it->second;

      if (bluetooth::common::init_flags::encrypted_advertising_log_is_enabled()) {
        LOG_INFO("pos_index: %d enc_data_part_len: %d", +pos_index, +enc_data_part_len);
      }
      for (int i = pos_index; i < (pos_index + enc_data_part_len); i++) {
        if ((i >= (pos_index + 2)) && (i <= (pos_index + 6))) {
          randomizer.push_back(adv_data[i]);
        } else if ((i > (pos_index + 6)) && i < (pos_index + (enc_data_part_len - 4))) {
          payload.push_back(adv_data[i]);
        }
        if ((i >= (pos_index + (enc_data_part_len - 4))) && (i < (pos_index + enc_data_part_len))) {
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
        LOG_INFO(" Decryption successful ");
        std::vector<uint8_t> decrypted_data;

        // construct enc adv data part's (decrypted) vector
        decrypted_data.insert(decrypted_data.begin(), out.begin(), out.end());

        // Insert decrypted part vector and position
        decrypted_data_map.insert(std::pair<int, std::vector<uint8_t>>(pos_index, decrypted_data));
      } else {
        LOG_INFO(" Decryption NOT successful ");
        break;  // try next enc key char value
      }
    }

    if (bluetooth::common::init_flags::encrypted_advertising_log_is_enabled()) {
      // Print decrypted_data_map
      std::map<int, std::vector<uint8_t>>::iterator it1;
      for (it1 = decrypted_data_map.begin(); it1 != decrypted_data_map.end(); it1++) {
        LOG_INFO(" decrypted_data_map: position: %d ", +(it1->first));
        std::vector<uint8_t> vec_temp = it1->second;
        LOG_INFO(
            " decrypted_data_map vector: %s",
            base::HexEncode(vec_temp.data(), vec_temp.size()).c_str());
      }
    }

    if (!decrypted_data_map.empty() && !enc_adv_data_map.empty()) {
      std::map<int, std::vector<uint8_t>>::iterator it_decrypted_map;
      std::map<int, int>::iterator it_enc_map;
      *is_decryption_success = true;
      it_decrypted_map = decrypted_data_map.begin();
      int enc_data_index = it_decrypted_map->first;
      std::vector<uint8_t> decrypted_part = it_decrypted_map->second;

      it_enc_map = enc_adv_data_map.begin();
      int enc_data_part_len = it_enc_map->second;

      // Copy data from original adv_data to adv_data_decrypted vector
      for (int i = 0; i < (int)adv_data.size(); i++) {
        if (i < enc_data_index) {
          adv_data_decrypted.push_back(adv_data[i]);
        } else if (i == enc_data_index) {
          adv_data_decrypted.insert(
              adv_data_decrypted.end(), decrypted_part.begin(), decrypted_part.end());
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
      adv_data.clear();
      adv_data.insert(adv_data.begin(), adv_data_decrypted.begin(), adv_data_decrypted.end());
      break;  // break and no need to iterate through other enc key char values for decryption
    }
  }  // for loop for enc_key_value

  return adv_data;
}

std::map<int, int> LeScanningReassembler::GetEncAdvFieldsInfo(const uint8_t* ad, size_t ad_len) {
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

/// Trim the advertising data by removing empty or overflowing
/// GAP Data entries.
std::vector<uint8_t> LeScanningReassembler::TrimAdvertisingData(
    const std::vector<uint8_t>& advertising_data) {
  // Remove empty and overflowing entries from the advertising data.
  std::vector<uint8_t> significant_advertising_data;
  for (size_t offset = 0; offset < advertising_data.size();) {
    size_t remaining_size = advertising_data.size() - offset;
    uint8_t entry_size = advertising_data[offset];

    if (entry_size != 0 && entry_size < remaining_size) {
      significant_advertising_data.push_back(entry_size);
      significant_advertising_data.insert(
          significant_advertising_data.end(),
          advertising_data.begin() + offset + 1,
          advertising_data.begin() + offset + 1 + entry_size);
    }

    offset += entry_size + 1;
  }

  return significant_advertising_data;
}

LeScanningReassembler::AdvertisingKey::AdvertisingKey(
    Address address, DirectAdvertisingAddressType address_type, uint8_t sid)
    : address(), sid() {
  // The address type is NO_ADDRESS_PROVIDED for anonymous advertising.
  if (address_type != DirectAdvertisingAddressType::NO_ADDRESS_PROVIDED) {
    this->address = AddressWithType(address, AddressType(address_type));
  }
  // 0xff is reserved to indicate that the ADI field was not present
  // in the ADV_EXT_IND PDU.
  if (sid != 0xff) {
    this->sid = sid;
  }
}

bool LeScanningReassembler::AdvertisingKey::operator==(const AdvertisingKey& other) {
  return address == other.address && sid == other.sid;
}

/// Append to the current advertising data of the selected advertiser.
/// If the advertiser is unknown a new entry is added, optionally by
/// dropping the oldest advertiser.
std::list<LeScanningReassembler::AdvertisingFragment>::iterator
LeScanningReassembler::AppendFragment(const AdvertisingKey& key, const std::vector<uint8_t>& data) {
  auto it = FindFragment(key);
  if (it != cache_.end()) {
    it->data.insert(it->data.end(), data.cbegin(), data.cend());
    return it;
  }

  if (cache_.size() > kMaximumCacheSize) {
    cache_.pop_back();
  }

  cache_.emplace_front(key, data);
  return cache_.begin();
}

std::list<LeScanningReassembler::PeriodicAdvertisingFragment>::iterator
LeScanningReassembler::AppendPeriodicFragment(
    uint16_t sync_handle, const std::vector<uint8_t>& data) {
  auto it = FindPeriodicFragment(sync_handle);
  if (it != periodic_cache_.end()) {
    it->data.insert(it->data.end(), data.cbegin(), data.cend());
    return it;
  }
  if (periodic_cache_.size() > kMaximumCacheSize) {
    periodic_cache_.pop_back();
  }
  periodic_cache_.emplace_front(sync_handle, data);
  return periodic_cache_.begin();
}
std::list<LeScanningReassembler::PeriodicAdvertisingFragment>::iterator
LeScanningReassembler::FindPeriodicFragment(uint16_t sync_handle) {
  for (auto it = periodic_cache_.begin(); it != periodic_cache_.end(); it++) {
    if (it->sync_handle == sync_handle) {
      return it;
    }
  }
  return periodic_cache_.end();
}

void LeScanningReassembler::RemoveFragment(const AdvertisingKey& key) {
  auto it = FindFragment(key);
  if (it != cache_.end()) {
    cache_.erase(it);
  }
}

bool LeScanningReassembler::ContainsFragment(const AdvertisingKey& key) {
  return FindFragment(key) != cache_.end();
}

bool LeScanningReassembler::ContainsPeriodicFragment(uint16_t sync_handle) {
  return FindPeriodicFragment(sync_handle) != periodic_cache_.end();
}

std::list<LeScanningReassembler::AdvertisingFragment>::iterator LeScanningReassembler::FindFragment(
    const AdvertisingKey& key) {
  for (auto it = cache_.begin(); it != cache_.end(); it++) {
    if (it->key == key) {
      return it;
    }
  }
  return cache_.end();
}

}  // namespace bluetooth::hci
