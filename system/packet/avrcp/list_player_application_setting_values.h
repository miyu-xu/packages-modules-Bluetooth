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

#pragma once

#include <vector>

#include "hardware/avrcp/avrcp_common.h"
#include "vendor_packet.h"

namespace bluetooth {
namespace avrcp {

class ListPlayerApplicationSettingValuesResponseBuilder
    : public VendorPacketBuilder {
 public:
  virtual ~ListPlayerApplicationSettingValuesResponseBuilder() = default;

  static std::unique_ptr<ListPlayerApplicationSettingValuesResponseBuilder>
  MakeBuilder(uint8_t num_of_values, std::vector<uint8_t> values);

  virtual size_t size() const override;
  virtual bool Serialize(
      const std::shared_ptr<::bluetooth::Packet>& pkt) override;

 protected:
  uint8_t num_of_values_;
  std::vector<uint8_t> values_;

  ListPlayerApplicationSettingValuesResponseBuilder(uint8_t num_of_values,
                                                    std::vector<uint8_t> values)
      : VendorPacketBuilder(CType::STABLE,
                            CommandPdu::LIST_PLAYER_APPLICATION_SETTING_VALUES,
                            PacketType::SINGLE),
        num_of_values_(num_of_values),
        values_(values){};
};

class ListPlayerApplicationSettingValuesRequest : public VendorPacket {
 public:
  virtual ~ListPlayerApplicationSettingValuesRequest() = default;

  /**
   *  List Player Application Setting Values
   *   AvrcpPacket:
   *     CType c_type_;
   *     uint8_t subunit_type_ : 5;
   *     uint8_t subunit_id_ : 3;
   *     Opcode opcode_;
   *   VendorPacket:
   *     uint8_t company_id[3];
   *     uint8_t command_pdu;
   *     uint8_t packet_type;
   *     uint16_t param_length;
   *   ListPlayerApplicationSettingValuesRequest:
   *     PlayerAttribute player_attribute;
   */
  static constexpr size_t kMinSize() { return VendorPacket::kMinSize() + 1; }

  PlayerAttribute GetPlayerAttribute() const;

  virtual bool IsValid() const override;
  virtual std::string ToString() const override;

 protected:
  using VendorPacket::VendorPacket;
};

}  // namespace avrcp
}  // namespace bluetooth