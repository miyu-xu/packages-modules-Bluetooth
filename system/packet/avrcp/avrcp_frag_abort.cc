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

#include "avrcp_frag_abort.h"

namespace bluetooth {
namespace avrcp {
std::unique_ptr<FragAbortBuilder> FragAbortBuilder::MakeBuilder(CommandPdu pdu) {
  std::unique_ptr<FragAbortBuilder> builder =
      std::unique_ptr<FragAbortBuilder>(new FragAbortBuilder(pdu));

  return builder;
}
size_t FragAbortBuilder::size() const { return VendorPacket::kMinSize() + 1; }

bool FragAbortBuilder::Serialize(const std::shared_ptr<::bluetooth::Packet>& pkt) {
  ReserveSpace(pkt, size());

  // Push the standard avrcp headers
  PacketBuilder::PushHeader(pkt);

  // Push the avrcp vendor command headers
  VendorPacketBuilder::PushHeader(pkt, 0);

  return true;
}

}  // namespace avrcp
}  // namespace bluetooth
