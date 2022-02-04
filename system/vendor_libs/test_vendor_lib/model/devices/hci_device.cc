/*
 * Copyright 2022 The Android Open Source Project
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

#include "hci_device.h"

#include <chrono>                             // for milliseconds
#include <cstdint>                            // for uint8_t
#include <type_traits>                        // for remove_extent_t
#include <vector>                             // for vector

#include "model/devices/device_properties.h"  // for DeviceProperties

namespace test_vendor_lib {
class HciTransport;

HciDevice::HciDevice(std::shared_ptr<HciTransport> transport)
    : transport_(transport) {
  advertising_interval_ms_ = std::chrono::milliseconds(1000);

  page_scan_delay_ms_ = std::chrono::milliseconds(600);

  properties_.SetPageScanRepetitionMode(0);
  properties_.SetClassOfDevice(0x600420);
  properties_.SetExtendedInquiryData({
      12,  // length
      9,   // Type: Device Name
      'g',
      'D',
      'e',
      'v',
      'i',
      'c',
      'e',
      '-',
      'h',
      'c',
      'i',

  });
  properties_.SetName({
      'g',
      'D',
      'e',
      'v',
      'i',
      'c',
      'e',
      '-',
      'H',
      'C',
      'I',
  });

  RegisterEventChannel([this](std::shared_ptr<std::vector<uint8_t>> packet) {
    transport_->SendEvent(*packet);
  });
  RegisterAclChannel([this](std::shared_ptr<std::vector<uint8_t>> packet) {
    transport_->SendAcl(*packet);
  });
  RegisterScoChannel([this](std::shared_ptr<std::vector<uint8_t>> packet) {
    transport_->SendSco(*packet);
  });
  RegisterIsoChannel([this](std::shared_ptr<std::vector<uint8_t>> packet) {
    transport_->SendIso(*packet);
  });

  transport_->RegisterCallbacks(
      [this](const std::vector<uint8_t>& raw_command) {
        std::shared_ptr<std::vector<uint8_t>> packet_copy =
            std::make_shared<std::vector<uint8_t>>(raw_command);
        HandleCommand(packet_copy);
      },
      [this](const std::vector<uint8_t>& raw_acl) {
        std::shared_ptr<std::vector<uint8_t>> packet_copy =
            std::make_shared<std::vector<uint8_t>>(raw_acl);
        HandleAcl(packet_copy);
      },
      [this](const std::vector<uint8_t>& raw_sco) {
        std::shared_ptr<std::vector<uint8_t>> packet_copy =
            std::make_shared<std::vector<uint8_t>>(raw_sco);
        HandleSco(packet_copy);
      },
      [this](const std::vector<uint8_t>& raw_iso) {
        std::shared_ptr<std::vector<uint8_t>> packet_copy =
            std::make_shared<std::vector<uint8_t>>(raw_iso);
        HandleIso(packet_copy);
      },
      [this]() { close_callback_(); });
}

void HciDevice::TimerTick() {
  transport_->TimerTick();
  DualModeController::TimerTick();
}

void HciDevice::RegisterCloseCallback(std::function<void()> close_callback) {
  close_callback_ = close_callback;
}

}  // namespace test_vendor_lib
