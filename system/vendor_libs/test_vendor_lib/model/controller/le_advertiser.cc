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

#include "le_advertiser.h"

using namespace bluetooth::hci;

namespace test_vendor_lib {
void LeAdvertiser::Initialize(AddressWithType address,
                              AddressWithType peer_address,
                              LeScanningFilterPolicy filter_policy,
                              model::packets::AdvertisementType type,
                              const std::vector<uint8_t>& advertisement,
                              const std::vector<uint8_t>& scan_response,
                              std::chrono::steady_clock::duration interval) {
  address_ = address;
  peer_address_ = peer_address;
  filter_policy_ = filter_policy;
  type_ = type;
  advertisement_ = advertisement;
  scan_response_ = scan_response;
  interval_ = interval;
}

void LeAdvertiser::InitializeExtended(
    AddressType address_type, AddressWithType peer_address,
    LeScanningFilterPolicy filter_policy,
    model::packets::AdvertisementType type,
    std::chrono::steady_clock::duration interval) {
  address_ = AddressWithType(address_.GetAddress(), address_type);
  peer_address_ = peer_address;
  filter_policy_ = filter_policy;
  type_ = type;
  interval_ = interval;
  LOG_INFO("%s -> %s type = %hhx interval = %d ms", address_.ToString().c_str(),
           peer_address.ToString().c_str(), type_,
           static_cast<int>(interval_.count()));
}

void LeAdvertiser::Clear() {
  address_ = AddressWithType{};
  peer_address_ = AddressWithType{};
  filter_policy_ = LeScanningFilterPolicy::ACCEPT_ALL;
  type_ = model::packets::AdvertisementType::ADV_IND;
  advertisement_.clear();
  scan_response_.clear();
  interval_ = std::chrono::milliseconds(0);
  enabled_ = false;
}

void LeAdvertiser::SetAddress(Address address) {
  LOG_INFO("set address %s", address_.ToString().c_str());
  address_ = AddressWithType(address, address_.GetAddressType());
}

AddressWithType LeAdvertiser::GetAddress() const { return address_; }

void LeAdvertiser::SetData(const std::vector<uint8_t>& data) {
  advertisement_ = data;
}

void LeAdvertiser::SetScanResponse(const std::vector<uint8_t>& data) {
  scan_response_ = data;
}

void LeAdvertiser::Enable() {
  enabled_ = true;
  extended_ = false;
  num_events_ = 0;
  ending_time_ = std::chrono::steady_clock::now() - interval_;
  last_le_advertisement_ = std::chrono::steady_clock::now() - interval_;

  LOG_INFO("%s -> %s type = %hhx ad length %zu, scan length %zu",
           address_.ToString().c_str(), peer_address_.ToString().c_str(), type_,
           advertisement_.size(), scan_response_.size());
}

void LeAdvertiser::EnableExtended(unsigned advertising_handle,
                                  std::chrono::milliseconds duration) {

  enabled_ = true;
  extended_ = true;
  num_events_ = 0;
  advertising_handle_ = advertising_handle;
  ending_time_ = std::chrono::steady_clock::now() - interval_;
  last_le_advertisement_ = std::chrono::steady_clock::now() - interval_;

  if (duration == std::chrono::milliseconds(0) && IsDirected()) {
    duration = std::chrono::milliseconds(10000);
  }

  if (duration != std::chrono::milliseconds(0)) {
    ending_time_ = std::chrono::steady_clock::now() + duration;
  }

  LOG_INFO("%s -> %s type = %hhx ad length %zu, scan length %zu",
           address_.ToString().c_str(), peer_address_.ToString().c_str(), type_,
           advertisement_.size(), scan_response_.size());
}

void LeAdvertiser::Disable() { enabled_ = false; }

bool LeAdvertiser::IsEnabled() const { return enabled_; }

bool LeAdvertiser::IsExtended() const { return extended_; }

bool LeAdvertiser::IsConnectable() const {
  return type_ != model::packets::AdvertisementType::ADV_NONCONN_IND &&
         type_ != model::packets::AdvertisementType::ADV_SCAN_IND;
}

bool LeAdvertiser::IsDirected() const {
  return type_ == model::packets::AdvertisementType::ADV_DIRECT_IND ||
         type_ == model::packets::AdvertisementType::SCAN_RESPONSE;
}

uint8_t LeAdvertiser::GetNumAdvertisingEvents() const { return num_events_; }

std::unique_ptr<bluetooth::hci::EventBuilder>
LeAdvertiser::GetEvent(std::chrono::steady_clock::time_point now) {
  // Disabled or no timeout set for this advertiser.
  if (!enabled_) {
    return nullptr;
  }

  // [Vol 4] Part E 7.8.9   LE Set Advertising Enable command
  // [Vol 4] Part E 7.8.56  LE Set Extended Advertising Enable command
  if (IsDirected() && now >= ending_time_) {
    enabled_ = false;
    return bluetooth::hci::LeConnectionCompleteBuilder::Create(
        ErrorCode::ADVERTISING_TIMEOUT, 0,
        bluetooth::hci::Role::CENTRAL,
        bluetooth::hci::AddressType::PUBLIC_DEVICE_ADDRESS,
        bluetooth::hci::Address(), 0, 0, 0,
        bluetooth::hci::ClockAccuracy::PPM_500);
  }

  // [Vol 4] Part E 7.8.56  LE Set Extended Advertising Enable command
  if (extended_ && now >= ending_time_) {
    enabled_ = false;
    return bluetooth::hci::LeAdvertisingSetTerminatedBuilder::Create(
        ErrorCode::SUCCESS, advertising_handle_, 0, num_events_);
  }

  return nullptr;
}

std::unique_ptr<model::packets::LeAdvertisementBuilder>
LeAdvertiser::GetAdvertisement(std::chrono::steady_clock::time_point now) {
  if (!enabled_) {
    return nullptr;
  }

  if (now - last_le_advertisement_ < interval_) {
    return nullptr;
  }

  last_le_advertisement_ = now;
  num_events_ += (num_events_ < 255 ? 1 : 0);
  return model::packets::LeAdvertisementBuilder::Create(
      address_.GetAddress(), peer_address_.GetAddress(),
      static_cast<model::packets::AddressType>(address_.GetAddressType()),
      type_, advertisement_);
}

std::unique_ptr<model::packets::LeScanResponseBuilder>
LeAdvertiser::GetScanResponse(bluetooth::hci::Address scanned,
                              bluetooth::hci::Address scanner) {
  if (scanned != address_.GetAddress() || !enabled_) {
    return nullptr;
  }
  switch (filter_policy_) {
    case bluetooth::hci::LeScanningFilterPolicy::
        CONNECT_LIST_AND_INITIATORS_IDENTITY:
    case bluetooth::hci::LeScanningFilterPolicy::CONNECT_LIST_ONLY:
      LOG_WARN("ScanResponses don't handle connect list filters");
      return nullptr;
    case bluetooth::hci::LeScanningFilterPolicy::CHECK_INITIATORS_IDENTITY:
      if (scanner != peer_address_.GetAddress()) {
        return nullptr;
      }
      break;
    case bluetooth::hci::LeScanningFilterPolicy::ACCEPT_ALL:
      break;
  }
  return model::packets::LeScanResponseBuilder::Create(
      address_.GetAddress(), peer_address_.GetAddress(),
      static_cast<model::packets::AddressType>(address_.GetAddressType()),
      model::packets::AdvertisementType::SCAN_RESPONSE, scan_response_);
}

}  // namespace test_vendor_lib
