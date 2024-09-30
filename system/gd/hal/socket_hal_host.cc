/*
 * Copyright 2024 The Android Open Source Project
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
#include "hal/socket_hal.h"

using bluetooth::hci::AclLinkType;
using bluetooth::hci::AclPowerMode;
using bluetooth::hci::ConnectionState;
using bluetooth::hci::SocketContext;
using bluetooth::hci::SocketHalCallback;
using bluetooth::hci::SocketProperties;

namespace bluetooth {
namespace hal {

const std::vector<SocketProperties> kDefaultOffloadSocketProperties = {
        {.dataPath = hci::SocketDataPath::OFFLOAD_SOFTWARE,
         .numOfLeCocSocketSupported_ = 0,
         .numOfRfcommSocketSupported_ = 0,
         .protocolProperties_ = {}},
        {.dataPath = hci::SocketDataPath::OFFLOAD_HARDWARE,
         .numOfLeCocSocketSupported_ = 0,
         .numOfRfcommSocketSupported_ = 0,
         .protocolProperties_ = {}}};

class SocketHalHost : public SocketHal {
public:
  bool IsBound() override { return false; }

protected:
  void ListDependencies(ModuleList* /*list*/) const {}

  void Start() override {}

  void Stop() override {}

  std::string ToString() const override { return std::string("SocketHalHost"); }

  std::vector<SocketProperties> GetSocketProperties() override {
    return {kDefaultOffloadSocketProperties};
  }

  void RegisterCallback(SocketHalCallback* callback) override { socket_hal_cb_ = callback; }

  bool SetAclCredits(AclLinkType /*linkType*/, int /*credit*/) override { return true; }

  bool NotifyAclLeDataLengthChange(int /*aclHandle*/, int /*txDataLen*/,
                                   int /*rxDataLen*/) override {
    return true;
  }

  bool NotifySocketConnectionStateChange(const SocketContext& /*context*/) override { return true; }

  bool SendSocketData(const SocketContext& /*context*/, const std::vector<uint8_t>& /*data*/) {
    return true;
  }

private:
  SocketHalCallback* socket_hal_cb_;
};

const ModuleFactory SocketHal::Factory = ModuleFactory([]() { return new SocketHalHost(); });

}  // namespace hal
}  // namespace bluetooth
