/*
 * Copyright 2018 The Android Open Source Project
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

#include "appsettings_packet.h"

namespace bluetooth {
namespace avrcp {

uint8_t AppSettingsAttributesText::GetAppSettingsAttrCnt() const {
  auto value = *(begin() + VendorPacket::kMinSize());
  return value;
}

PlayerAttribute AppSettingsAttributesText::GetAppSettingsAttrRequested(uint8_t cnt) const {
  auto value = *(begin() + VendorPacket::kMinSize() + 1 + cnt);
  return static_cast<PlayerAttribute>(value);
}

uint16_t AppSettingsAttributesText::GetLength() const {
  return GetParameterLength();
}

std::unique_ptr<AppSettingsAttributesTextBuilder>
AppSettingsAttributesTextBuilder::MakeBuilder(uint8_t cnt) {
  std::unique_ptr<AppSettingsAttributesTextBuilder> builder(
      new AppSettingsAttributesTextBuilder(cnt));

  return builder;
}

AppSettingsAttributesTextBuilder* AppSettingsAttributesTextBuilder::AddValue(
    uint8_t value) {
  CHECK_LT(value_.size(), size_t(0xFF))
      << __func__ << ": maximum capability count reached";
  value_.push_back(value);

  return this;
}

size_t AppSettingsAttributesTextBuilder::size() const {
  size_t size = 3; // uint16_t + uint8_t
  size += value_.size(); // check
  return AppSettingsAttributesText::kMinSize() + size;
}

bool AppSettingsAttributesTextBuilder::Serialize(
    const std::shared_ptr<::bluetooth::Packet>& pkt) {
  ReserveSpace(pkt, size());

  // Push the standard avrcp headers
  PacketBuilder::PushHeader(pkt);

  // Push the avrcp vendor command headers
  uint16_t parameter_count = size() - VendorPacket::kMinSize();
  VendorPacketBuilder::PushHeader(pkt, parameter_count);
  AddPayloadOctets1(pkt, cnt_);
  for (auto it = value_.begin(); it != value_.end(); it++) {
    AddPayloadOctets1(pkt, *it);
  }

  return true;
}

PlayerAttribute GetAppSettingValueText::GetAppSettingsId() const {
  auto value = *(begin() + VendorPacket::kMinSize());
  return static_cast<PlayerAttribute>(value);
}

uint8_t GetAppSettingValueText::GetNumAppSettingValue() const {
  auto value = *(begin() + VendorPacket::kMinSize() + 1);
  return static_cast<uint8_t>(value);
}

uint16_t GetAppSettingValueText::GetLength() const {
  return GetParameterLength();
}

uint8_t GetAppSettingValueText::ListValueId(uint8_t cnt) const {
  auto value = *(begin() + VendorPacket::kMinSize() + 2 + cnt);
  return static_cast<uint8_t>(value);
}

std::unique_ptr<GetAppSettingValueTextBuilder>
GetAppSettingValueTextBuilder::MakeBuilder(uint8_t cnt) {
  std::unique_ptr<GetAppSettingValueTextBuilder> builder(
      new GetAppSettingValueTextBuilder(cnt));

  return builder;
}

GetAppSettingValueTextBuilder* GetAppSettingValueTextBuilder::AddValue(
    uint8_t value) {
  CHECK_LT(value_.size(), size_t(0xFF))
      << __func__ << ": maximum capability count reached";
  value_.push_back(value);

  return this;
}

size_t GetAppSettingValueTextBuilder::size() const {
  size_t size = 2; // uint8_t + uint8_t
  size += value_.size();
  return GetAppSettingValueText::kMinSize() + size;
}

bool GetAppSettingValueTextBuilder::Serialize(
    const std::shared_ptr<::bluetooth::Packet>& pkt) {
  ReserveSpace(pkt, size());

  // Push the standard avrcp headers
  PacketBuilder::PushHeader(pkt);

  // Push the avrcp vendor command headers
  uint16_t parameter_count = size() - VendorPacket::kMinSize();
  VendorPacketBuilder::PushHeader(pkt, parameter_count);
  AddPayloadOctets1(pkt, cnt_);
  for (auto it = value_.begin(); it != value_.end(); it++) {
    AddPayloadOctets1(pkt, *it);
  }

  return true;
}

}  // namespace avrcp
}  // namespace bluetooth