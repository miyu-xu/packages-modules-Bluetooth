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
#include "hci/le_scanning_defragmenter.h"

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
#include "storage/storage_module.h"

namespace bluetooth::hci {

std::optional<std::vector<uint8_t>> ProcessAdvertisingReport(
    uint16_t event_type,
    uint8_t address_type,
    Address address,
    uint8_t primary_phy,
    uint8_t secondary_phy,
    uint8_t advertising_sid,
    int8_t tx_power,
    int8_t rssi,
    uint16_t periodic_advertising_interval,
    const std::vector<uint8_t>& advertising_data) {

  bool is_scannable = event_type & (1 << kScannableBit);
  bool is_scan_response = event_type & (1 << kScanResponseBit);
  bool is_legacy = event_type & (1 << kLegacyBit);
  DataStatus data_status = DataStatus((event_type >> kDataStatusBits) & 0x3);

  if (address_type != (uint8_t)DirectAdvertisingAddressType::NO_ADDRESS_PROVIDED &&
      address == Address::kEmpty) {
    LOG_WARN("Ignoring non-anonymous advertising report with empty address");
    return;
  }

  AdvertisingCache::AdvertisingInfo info(
      address, DirectAdvertisingAddressType(address_type), advertising_sid);

  // XXX
  // When using the vendor command Le Set Extended Params to
  // configure a filter accept list based e.g. on the service UUIDs
  // found in the report, we ignore the scan responses as we cannot be
  // certain that they will not be dropped by the filter.
/*  bool using_vendor_scan_filter =
      filter_policy_ == LeScanningFilterPolicy::FILTER_ACCEPT_LIST_ONLY &&
      api_type_ == ScanApiType::ANDROID_HCI;
*/

  // Ignore scan responses received without a mathing advertising event.
  if (is_scan_response && (ignore_scan_responses_ || !ContainsFragment(info))) {
    LOG_INFO("Ignoring scan response received without advertising event");
    return;
  }

  // Legacy advertising is always complete, we can drop
  // the previous data as safety measure if the report is not a scan
  // response.
  if (is_legacy && !is_scan_response) {
    RemoveFragment(info);
  }

  // Concatenate the data with existing fragments.
  const std::vector<uint8_t>& complete_advertising_data =
    AppendFragment(info, advertising_data);

  bool expect_scan_response = is_scannable && !is_scan_response && !ignore_scan_responses_;

  // Check if we should wait for additional fragments:
  // - For legacy advertising, when a scan response is expected.
  if (is_legacy && expect_scan_response) {
    return;
  }

  // - For extended advertising, when the current data is marked
  //   incomplete OR when a scan response is expected.
  if (!is_legacy && (data_status == DataStatus::CONTINUING || expect_scan_response)) {
    return;
  }

  switch (address_type) {
    case (uint8_t)AddressType::PUBLIC_DEVICE_ADDRESS:
    case (uint8_t)AddressType::PUBLIC_IDENTITY_ADDRESS:
      address_type = (uint8_t)AddressType::PUBLIC_DEVICE_ADDRESS;
      break;
    case (uint8_t)AddressType::RANDOM_DEVICE_ADDRESS:
    case (uint8_t)AddressType::RANDOM_IDENTITY_ADDRESS:
      address_type = (uint8_t)AddressType::RANDOM_DEVICE_ADDRESS;
      break;
  }

  // Remove empty and overflowing entries from the advertising data.
  std::vector<uint8_t> significant_advertising_data;
  for (size_t offset = 0; offset < complete_advertising_data.size();) {
    size_t remaining_size = complete_advertising_data.size() - offset;
    uint8_t entry_size = complete_advertising_data[offset];

    if (entry_size != 0 && entry_size < remaining_size) {
      significant_advertising_data.push_back(entry_size);
      significant_advertising_data.insert(
          significant_advertising_data.end(),
          complete_advertising_data.begin() + offset + 1,
          complete_advertising_data.begin() + offset + 1 + entry_size);
    }

    offset += entry_size + 1;
  }

/*  scanning_callbacks_->OnScanResult(
      event_type,
      address_type,
      address,
      primary_phy,
      secondary_phy,
      advertising_sid,
      tx_power,
      rssi,
      periodic_advertising_interval,
      complete_advertising_data);
*/
  // Remove the complete advertising data from the cache to
  // clear the space for the next advertising event from the same advertiser.
  advertising_cache_.Remove(info);
}

LeScanningReassembler::AdvertisingInfo::AdvertisingInfo(Address address, DirectAdvertisingAddressType address_type, uint8_t sid)
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

bool LeScanningReassembler::AdvertisingInfo::operator==(const AdvertisingInfo& other) {
  return address == other.address && sid == other.sid;
}

/// Append to the current advertising data of the selected advertiser.
/// If the advertiser is unknown a knew entry is added, optionally by
/// dropping the oldest advertiser.
const std::vector<uint8_t>& LeScanningReassembler::AppendFragment(
    const AdvertisingInfo& info, const std::vector<uint8_t>& data) {
  auto it = FindFragment(info);
  if (it != cache_.end()) {
    it->data.insert(it->data.end(), data.cbegin(), data.cend());
    return it->data;
  }

  if (cache_.size() > cache_max) {
    cache_.pop_back();
  }

  cache_.emplace_front(info, data);
  return cache_.front().data;
}

void LeScanningReassembler::RemoveFragment(const AdvertisingInfo& info) {
  auto it = FindFragment(info);
  if (it != cache_.end()) {
    cache_.erase(it);
  }
}

bool LeScanningReassembler::ContainsFragment(const AdvertisingInfo& info) {
  return FindFragment(info) != cache_.end();
}

std::list<Item>::iterator LeScanningReassembler::FindFragment(const AdvertisingInfo& info) {
  for (auto it = cache_.begin(); it != cache_.end(); it++) {
    if (it->info == info) {
      return it;
    }
  }
  return cache_.end();
}

}  // namespace bluetooth::hci
