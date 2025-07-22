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

#pragma once

#include "vendor_packet.h"
#include <base/logging.h>

namespace bluetooth {
namespace avrcp {

class AppSettingsAttributesText : public VendorPacket {
 public:
  virtual ~AppSettingsAttributesText() = default;

  /**
   * Avrcp Vendor Packet Layout
   *   AvrcpPacket:
   *     CType c_type_;
   *     uint8_t subunit_type_ : 5;
   *     uint8_t subunit_id_ : 3;
   *     Opcode opcode_;
   *   VendorPacket:
   *     uint8_t company_id[3];
   *     uint8_t command_pdu;
   *     uint8_t packet_type;
   *     uint16_t parameter_length;
   *     uint8_t num_of_attributes;
   *     uint8_t attributes[2];
   *   uint8_t[] payload;
   */
  static constexpr size_t kMinSize() { return VendorPacket::kMinSize() + 2; };

  // Getter Functions
  uint8_t GetAppSettingsAttrCnt() const;
  PlayerAttribute GetAppSettingsAttrRequested(uint8_t cnt) const;
  uint16_t GetLength() const;

 protected:
  using VendorPacket::VendorPacket;
};

class AppSettingsAttributesTextBuilder : public VendorPacketBuilder {
 public:
  virtual ~AppSettingsAttributesTextBuilder() = default;

  static std::unique_ptr<AppSettingsAttributesTextBuilder> MakeBuilder(uint8_t cnt);

  virtual AppSettingsAttributesTextBuilder* AddValue(uint8_t value);
  virtual size_t size() const override;
  virtual bool Serialize(
       const std::shared_ptr<::bluetooth::Packet>& pkt) override;

  private:
    uint8_t cnt_;
    std::vector<uint8_t> value_;

  AppSettingsAttributesTextBuilder(uint8_t cnt)
  : VendorPacketBuilder(CType::STABLE, CommandPdu::GET_PLAYER_APPLICATION_SETTING_ATTRIBUTE_TEXT,
                        PacketType::SINGLE),
    cnt_(cnt){};
};

class GetAppSettingValueText : public VendorPacket {
 public:
  virtual ~GetAppSettingValueText() = default;

  /**
   * Avrcp Vendor Packet Layout
   *   AvrcpPacket:
   *     CType c_type_;
   *     uint8_t subunit_type_ : 5;
   *     uint8_t subunit_id_ : 3;
   *     Opcode opcode_;
   *   VendorPacket:
   *     uint8_t company_id[3];
   *     uint8_t command_pdu;
   *     uint8_t packet_type;
   *     uint16_t parameter_length;
   *     uint8_t num_of_attributes;
   *   uint8_t[] payload;
   */
  static constexpr size_t kMinSize() { return VendorPacket::kMinSize() + 1; };

  // Getter Functions
  PlayerAttribute GetAppSettingsId() const;
  uint8_t GetNumAppSettingValue() const;
  uint8_t ListValueId(uint8_t cnt) const;
  uint16_t GetLength() const;

 protected:
  using VendorPacket::VendorPacket;
};

class GetAppSettingValueTextBuilder : public VendorPacketBuilder {
 public:
  virtual ~GetAppSettingValueTextBuilder() = default;

  static std::unique_ptr<GetAppSettingValueTextBuilder> MakeBuilder(uint8_t cnt);

  virtual GetAppSettingValueTextBuilder* AddValue(uint8_t value);
  virtual size_t size() const override;
  virtual bool Serialize(
       const std::shared_ptr<::bluetooth::Packet>& pkt) override;

  private:
    std::vector<uint8_t> value_;
    uint8_t cnt_;

  GetAppSettingValueTextBuilder(uint8_t cnt)
  : VendorPacketBuilder(CType::STABLE, CommandPdu::GET_PLAYER_APPLICATION_SETTING_VALUE_TEXT,
                        PacketType::SINGLE),
    cnt_(cnt){};
};

}  // namespace avrcp
}  // namespace bluetooth