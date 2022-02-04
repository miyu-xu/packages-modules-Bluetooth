/*
 * Copyright 2021 The Android Open Source Project
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

#include "hci_socket_transport.h"

#include <type_traits>                                 // for remove_extent_t

#include "model/devices/h4_data_channel_packetizer.h"  // for H4DataChannelP...
#include "model/devices/hci_transport.h"               // for PacketCallback
#include "os/log.h"                                    // for LOG_INFO, LOG_...

namespace android {
namespace net {
class AsyncDataChannel;
}  // namespace net
}  // namespace android

namespace test_vendor_lib {

HciSocketTransport::HciSocketTransport(
    std::shared_ptr<AsyncDataChannel> datachannel)
    : datachannel_(datachannel) {}

void HciSocketTransport::RegisterCallbacks(PacketCallback command_callback,
                                           PacketCallback acl_callback,
                                           PacketCallback sco_callback,
                                           PacketCallback iso_callback,
                                           CloseCallback close_callback) {
  h4_ = H4DataChannelPacketizer(
      datachannel_, command_callback,
      [](const std::vector<uint8_t>&) {
        LOG_ALWAYS_FATAL("Unexpected Event in HciSocketTransport!");
      },
      acl_callback, sco_callback, iso_callback,
      [this, close_callback]() {
        LOG_INFO("HCI socket device disConnected");
        datachannel_->Close();
        close_callback();
      });
}

void HciSocketTransport::TimerTick() { h4_.OnDataReady(datachannel_); }

void HciSocketTransport::SendEvent(const std::vector<uint8_t>& packet) {
  if (!datachannel_->Connected()) {
    LOG_INFO("Closed socket. Dropping event packet");
    return;
  }
  h4_.Send(static_cast<uint8_t>(PacketType::EVENT), packet.data(),
           packet.size());
}

void HciSocketTransport::SendAcl(const std::vector<uint8_t>& packet) {
  if (!datachannel_->Connected()) {
    LOG_INFO("Closed socket. Dropping acl packet");
    return;
  }
  h4_.Send(static_cast<uint8_t>(PacketType::ACL), packet.data(), packet.size());
}

void HciSocketTransport::SendSco(const std::vector<uint8_t>& packet) {
  if (!datachannel_->Connected()) {
    LOG_INFO("Closed socket. Dropping sco packet");
    return;
  }
  h4_.Send(static_cast<uint8_t>(PacketType::SCO), packet.data(), packet.size());
}

void HciSocketTransport::SendIso(const std::vector<uint8_t>& packet) {
  if (!datachannel_->Connected()) {
    LOG_INFO("Closed socket. Dropping iso packet");
    return;
  }
  h4_.Send(static_cast<uint8_t>(PacketType::ISO), packet.data(), packet.size());
}

void HciSocketTransport::Close() { datachannel_->Close(); }
}  // namespace test_vendor_lib