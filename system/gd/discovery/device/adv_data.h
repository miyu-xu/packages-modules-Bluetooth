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

#pragma once

#include <cstdint>
#include <vector>

#include "discovery/device/eir_data.h"

namespace bluetooth {
namespace discovery {
namespace device {

struct service_uuid16_t {
  uint16_t uuid;
  std::vector<uint8_t> data;
};

struct service_uuid32_t {
  uint32_t uuid;
  std::vector<uint8_t> data;
};

struct slave_connection_interval_range_t {
  uint16_t conn_interval_min;
  uint16_t conn_interval_max;
} __attribute__((packed));

//  Supplement to Bluetooth Core Specification | CSS v9, Part A
//  DATA TYPES DEFINITIONS AND FORMATS
class AdvData : public EirData {
 public:
  AdvData(const std::vector<uint8_t>& data);

  // Slave Connection Interval Range
  bool GetSlaveConnectionIntervalRange(std::vector<slave_connection_interval_range_t>&) const;

  // Service Solicitation
  bool GetServiceSolicitation16(std::vector<std::uint16_t> uuids) const;
  bool GetServiceSolicitation32(std::vector<std::uint32_t> uuids) const;
  bool GetServiceSolicitation128(std::vector<hci::Uuid> uuids) const;

  // Service Data
  bool GetServiceUuids16(std::vector<service_uuid16_t>&) const;
  bool GetServiceUuids32(std::vector<service_uuid32_t>&) const;

  // Appearance
  bool GetAppearance(std::vector<uint16_t>&) const;

  // Advertising Interval
  bool GetAdvertisingInterval(std::vector<uint16_t>&) const;

  // LE Supported Features
  bool GetLeSupportedFeatures(std::vector<std::vector<uint8_t>>&) const;
};

}  // namespace device
}  // namespace discovery
}  // namespace bluetooth
