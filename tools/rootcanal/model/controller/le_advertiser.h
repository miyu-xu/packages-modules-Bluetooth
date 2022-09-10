/*
 * Copyright 2020 The Android Open Source Project
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

#include <chrono>
#include <cstdint>
#include <memory>
#include <optional>

#include "hci/address_with_type.h"
#include "hci/hci_packets.h"
#include "packets/link_layer_packets.h"

namespace rootcanal {

using namespace bluetooth::hci;

// Implement the unique legacy advertising instance.
// For extended advertising check the ExtendedAdvertiser class.
class LegacyAdvertiser {
 public:
  LegacyAdvertiser() = default;
  ~LegacyAdvertiser() = default;

  bool IsEnabled() const { return advertising_enable; };

  // Generate HCI Connection Complete or Enhanced HCI Connection Complete
  // events with Advertising Timeout error code when the advertising
  // type is ADV_DIRECT_IND and the connection failed to be established.
  std::unique_ptr<bluetooth::hci::EventBuilder> AdvertisingTimeout(
      std::chrono::steady_clock::time_point now, bool enhanced);

  // Generate Link Layer Advertising events when advertising is enabled
  // and a full interval has passed since the last event.
  std::unique_ptr<model::packets::LeAdvertisementBuilder> AdvertisingEvent(
      std::chrono::steady_clock::time_point now);

  // Time keeping.
  std::chrono::steady_clock::duration interval{};
  std::chrono::steady_clock::time_point last_event{};
  std::optional<std::chrono::steady_clock::time_point> timeout{};

  // Host configuration parameters. Gather the configuration from the
  // legacy advertising HCI commands. The initial configuration
  // matches the default values of the parameters of the HCI command
  // LE Set Advertising Parameters.
  bool advertising_enable{false};
  uint16_t advertising_interval{0x0800};
  AdvertisingType advertising_type{AdvertisingType::ADV_IND};
  Address own_address{};
  OwnAddressType own_address_type{OwnAddressType::PUBLIC_DEVICE_ADDRESS};
  PeerAddressType peer_address_type{
      PeerAddressType::PUBLIC_DEVICE_OR_IDENTITY_ADDRESS};
  Address peer_address{};
  uint8_t advertising_channel_map{0x07};
  AdvertisingFilterPolicy advertising_filter_policy{
      AdvertisingFilterPolicy::ALL_DEVICES};
  std::vector<uint8_t> advertising_data{};
  std::vector<uint8_t> scan_response_data{};
};

// Implement a single extended advertising set.
// The configuration is set by the extended advertising commands;
// for the legacy advertiser check the LegacyAdvertiser class.
class ExtendedAdvertiser {
 public:
  ExtendedAdvertiser(uint8_t advertising_handle = 0)
      : advertising_handle(advertising_handle) {}
  ~ExtendedAdvertiser() = default;

  // Time keeping.
  std::chrono::steady_clock::duration interval{};
  std::chrono::steady_clock::time_point last_event{};
  std::optional<std::chrono::steady_clock::time_point> timeout{};

  // Host configuration parameters. Gather the configuration from the
  // extended advertising HCI commands.
  uint8_t advertising_handle;
  bool advertising_enable{false};
  bool periodic_advertising_enable{false};
  AdvertisingEventProperties advertising_event_properties{};
  uint16_t primary_advertising_interval{};
  uint8_t primary_advertising_channel_map{};
  OwnAddressType own_address_type{};
  PeerAddressType peer_address_type{};
  Address peer_address{};
  std::optional<Address> random_address{};
  AdvertisingFilterPolicy advertising_filter_policy{};
  uint8_t advertising_tx_power{};
  PrimaryPhyType primary_advertising_phy{};
  uint8_t secondary_max_skip{};
  SecondaryPhyType secondary_advertising_phy{};
  uint8_t advertising_sid{};
  bool scan_request_notification_enable{};
  std::vector<uint8_t> advertising_data{};
  std::vector<uint8_t> scan_response_data{};
  bool partial_advertising_data{false};
  bool partial_scan_response_data{false};

  // Not implemented at the moment.
  bool constant_tone_extensions{false};

  // Compute the maximum advertising data payload size for the selected
  // advertising event properties. The advertising data is not present if
  // 0 is returned.
  static uint16_t GetMaxAdvertisingDataLength(
      const AdvertisingEventProperties& properties);

  // Compute the maximum scan response data payload size for the selected
  // advertising event properties. The scan response data is not present if
  // 0 is returned.
  static uint16_t GetMaxScanResponseDataLength(
      const AdvertisingEventProperties& properties);

  // Reconstitute the raw Advertising_Event_Properties bitmask.
  static uint16_t GetRawAdvertisingEventProperties(
      const AdvertisingEventProperties& properties);
};

}  // namespace rootcanal
