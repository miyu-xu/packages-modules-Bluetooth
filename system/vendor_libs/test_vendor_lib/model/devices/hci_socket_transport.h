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

#include <cstdint>                       // for uint8_t
#include <functional>                    // for __base
#include <memory>                        // for shared_ptr, make_shared
#include <vector>                        // for vector

#include "h4_data_channel_packetizer.h"  // for H4DataChannelPacketizer
#include "hci_transport.h"               // for PacketCallback, CloseCallback
#include "model/devices/h4_parser.h"     // for ClientDisconnectCallback
#include "model/devices/hci_protocol.h"  // for PacketReadCallback, AsyncDat...

namespace android {
namespace net {
class AsyncDataChannel;
}  // namespace net
}  // namespace android

namespace test_vendor_lib {

using android::net::AsyncDataChannel;

class HciSocketTransport : public HciTransport {
 public:
  HciSocketTransport(std::shared_ptr<AsyncDataChannel> datachannel);
  ~HciSocketTransport() = default;

  static std::shared_ptr<HciTransport> Create(
      std::shared_ptr<AsyncDataChannel> datachannel) {
    return std::make_shared<HciSocketTransport>(datachannel);
  }

  void SendEvent(const std::vector<uint8_t>& packet) override;
  void SendAcl(const std::vector<uint8_t>& packet) override;
  void SendSco(const std::vector<uint8_t>& packet) override;
  void SendIso(const std::vector<uint8_t>& packet) override;
  void RegisterCallbacks(PacketCallback command_callback,
                                 PacketCallback acl_callback,
                                 PacketCallback sco_callback,
                                 PacketCallback iso_callback,
                                 CloseCallback close_callback) override;
  void TimerTick() override;
  void Close() override;
 private:
  std::shared_ptr<AsyncDataChannel> datachannel_;
  H4DataChannelPacketizer h4_{datachannel_,
                              [](const std::vector<uint8_t>&) {},
                              [](const std::vector<uint8_t>&) {},
                              [](const std::vector<uint8_t>&) {},
                              [](const std::vector<uint8_t>&) {},
                              [](const std::vector<uint8_t>&) {},
                              [] {}};
};
}  // namespace test_vendor_lib