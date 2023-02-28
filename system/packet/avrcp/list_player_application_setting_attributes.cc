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

#include "list_player_application_setting_attributes.h"

namespace bluetooth {
namespace avrcp {

std::unique_ptr<ListPlayerApplicationSettingAttributesResponseBuilder>
ListPlayerApplicationSettingAttributesResponseBuilder::MakeBuilder(
    uint8_t num_of_attributes, std::vector<PlayerAttribute> player_attributes) {
  std::unique_ptr<ListPlayerApplicationSettingAttributesResponseBuilder>
      builder(new ListPlayerApplicationSettingAttributesResponseBuilder(
          num_of_attributes, player_attributes));

  return builder;
}

size_t ListPlayerApplicationSettingAttributesResponseBuilder::size() const {
  size_t len = VendorPacket::kMinSize();
  len += sizeof(num_of_attributes_);                    // Number of attributes
  len += num_of_attributes_ * sizeof(PlayerAttribute);  // Attributes size
  return len;
}

bool ListPlayerApplicationSettingAttributesResponseBuilder::Serialize(
    const std::shared_ptr<::bluetooth::Packet>& pkt) {
  ReserveSpace(pkt, size());

  PacketBuilder::PushHeader(pkt);

  VendorPacketBuilder::PushHeader(pkt, size() - VendorPacket::kMinSize());

  AddPayloadOctets1(pkt, (uint8_t)num_of_attributes_);
  for (auto player_attribute : player_attributes_) {
    AddPayloadOctets1(pkt, (uint8_t)player_attribute);
  }

  return true;
}

}  // namespace avrcp
}  // namespace bluetooth
