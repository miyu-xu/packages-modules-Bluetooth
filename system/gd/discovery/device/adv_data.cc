/*
 * Copyright 2024 The Android Open Source Project
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

#include "discovery/device/adv_data.h"

#include <iterator>
#include <vector>

#include "hci/hci_packets.h"
#include "hci/uuid.h"

using namespace bluetooth;

using namespace bluetooth::hci;
using namespace bluetooth::packet;

namespace bluetooth::discovery::device {

AdvData::AdvData(const std::vector<uint8_t>& data) : EirData(data) {}

bool AdvData::GetSlaveConnectionIntervalRange(
    std::vector<slave_connection_interval_range_t>& range) const {
  for (const auto& gap_data : gap_data_) {
    if (gap_data.data_type_ == hci::GapDataType::SLAVE_CONNECTION_INTERVAL_RANGE) {
      if (gap_data.data_.size() < sizeof(slave_connection_interval_range_t)) continue;
      auto it = gap_data.data_.begin();
      range.push_back({
          .conn_interval_min = (uint16_t)(*it | *(it + 1) << 8),
          .conn_interval_max = (uint16_t)(*(it + 2) | *(it + 3) << 8),
      });
    }
  }
  return !range.empty();
}

bool AdvData::GetServiceSolicitation16(std::vector<std::uint16_t> uuids) const {
  for (const auto& gap_data : gap_data_) {
    if (gap_data.data_type_ == hci::GapDataType::LIST_16BIT_SERVICE_SOLICITATION_UUIDS) {
      auto it = gap_data.data_.begin();
      while (std::distance(it, gap_data.data_.end()) >= (long)Uuid::kNumBytes16) {
        uuids.push_back(*it | *(it + 1) << 8);
        it += Uuid::kNumBytes16;
      }
    }
  }
  return !uuids.empty();
}

bool AdvData::GetServiceSolicitation32(std::vector<std::uint32_t> uuids) const {
  for (const auto& gap_data : gap_data_) {
    if (gap_data.data_type_ == hci::GapDataType::LIST_32BIT_SERVICE_SOLICITATION_UUIDS) {
      auto it = gap_data.data_.begin();
      while (std::distance(it, gap_data.data_.end()) >= (long)Uuid::kNumBytes16) {
        uuids.push_back(*it | *(it + 1) << 8);
        it += Uuid::kNumBytes16;
      }
    }
  }
  return !uuids.empty();
}

bool AdvData::GetServiceSolicitation128(std::vector<hci::Uuid> uuids) const {
  for (const auto& gap_data : gap_data_) {
    if (gap_data.data_type_ == hci::GapDataType::LIST_128BIT_SERVICE_SOLICITATION_UUIDS) {
      auto it = gap_data.data_.begin();
      while (std::distance(it, gap_data.data_.end()) >= (long)Uuid::kNumBytes128) {
        auto uuid = bluetooth::hci::Uuid::From128BitLE(&it[0]);
        uuids.push_back(uuid);
        it += Uuid::kNumBytes128;
      }
    }
  }
  return !uuids.empty();
}

bool AdvData::GetServiceUuids16(std::vector<service_uuid16_t>& uuids) const {
  for (const auto& gap_data : gap_data_) {
    if (gap_data.data_type_ == hci::GapDataType::SERVICE_DATA_16_BIT_UUIDS) {
      if (gap_data.data_.size() < Uuid::kNumBytes16) continue;
      auto it = gap_data.data_.begin();
      uuids.push_back({
          .uuid = (uint16_t)(*it | *(it + 1) << 8),
          .data = std::vector<uint8_t>(it + Uuid::kNumBytes16, gap_data.data_.end()),
      });
    }
  }
  return !uuids.empty();
}

bool AdvData::GetServiceUuids32(std::vector<service_uuid32_t>& uuids) const {
  for (const auto& gap_data : gap_data_) {
    if (gap_data.data_type_ == hci::GapDataType::SERVICE_DATA_32_BIT_UUIDS) {
      if (gap_data.data_.size() < Uuid::kNumBytes32) continue;
      auto it = gap_data.data_.begin();
      uuids.push_back({
          .uuid = (uint32_t)(*it | *(it + 1) << 8 | *(it + 2) << 16 | *(it + 3) << 24),
          .data = std::vector<uint8_t>(it + Uuid::kNumBytes32, gap_data.data_.end()),
      });
    }
  }
  return !uuids.empty();
}

bool AdvData::GetAppearance(std::vector<uint16_t>& appearance) const {
  for (const auto& gap_data : gap_data_) {
    if (gap_data.data_type_ == hci::GapDataType::APPEARANCE) {
      if (gap_data.data_.size() < sizeof(uint16_t)) continue;
      auto it = gap_data.data_.begin();
      appearance.push_back((uint16_t)(*it | *(it + 1) << 8));
    }
  }
  return !appearance.empty();
}

bool AdvData::GetAdvertisingInterval(std::vector<uint16_t>& interval) const {
  for (const auto& gap_data : gap_data_) {
    if (gap_data.data_type_ == hci::GapDataType::ADVERTISING_INTERVAL) {
      if (gap_data.data_.size() < sizeof(uint16_t)) continue;
      auto it = gap_data.data_.begin();
      interval.push_back((uint16_t)(*it | *(it + 1) << 8));
    }
  }
  return !interval.empty();
}

bool AdvData::GetLeSupportedFeatures(std::vector<std::vector<uint8_t>>& features) const {
  for (const auto& gap_data : gap_data_) {
    if (gap_data.data_type_ == hci::GapDataType::LE_SUPPORTED_FEATURES) {
      features.push_back({gap_data.data_.begin(), gap_data.data_.end()});
    }
  }
  return !features.empty();
}

}  // namespace bluetooth::discovery::device
