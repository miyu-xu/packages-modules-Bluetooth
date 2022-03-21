/*
 * Copyright 2016 The Android Open Source Project
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

#include "device.h"

#include <vector>

namespace rootcanal {

std::string Device::ToString() const {
  std::string dev = GetTypeString() + "@" + properties_.GetAddress().ToString();

  return dev;
}

void Device::RegisterPhyChannel(
    std::function<void(model::packets::LinkLayerPacketView, Phy::Type)> send) {
  send_callback_ = send;
}

bool Device::IsAdvertisementAvailable() const {
  return (advertising_interval_ms_ > std::chrono::milliseconds(0)) &&
         (std::chrono::steady_clock::now() >=
          last_advertisement_ + advertising_interval_ms_);
}

void Device::SendLinkLayerPacket(
    std::shared_ptr<model::packets::LinkLayerPacketBuilder> to_send,
    Phy::Type phy_type) {
  // Convert from a Builder to a View
  auto bytes = std::make_shared<std::vector<uint8_t>>();
  bluetooth::packet::BitInserter i(*bytes);
  bytes->reserve(to_send->size());
  to_send->Serialize(i);
  auto packet_view =
      bluetooth::packet::PacketView<bluetooth::packet::kLittleEndian>(bytes);
  auto link_layer_packet_view =
      model::packets::LinkLayerPacketView::Create(packet_view);
  ASSERT(link_layer_packet_view.IsValid());

  SendLinkLayerPacket(link_layer_packet_view, phy_type);
}

void Device::SendLinkLayerPacket(model::packets::LinkLayerPacketView to_send,
                                 Phy::Type phy_type) {
  send_callback_(to_send, phy_type);
}

void Device::Close() {
  if (close_callback_) {
    close_callback_();
  }
}

void Device::RegisterCloseCallback(std::function<void()> close_callback) {
  close_callback_ = close_callback;
}

void Device::SetAddress(Address) {
  LOG_INFO("%s does not implement %s", GetTypeString().c_str(), __func__);
}

}  // namespace rootcanal
