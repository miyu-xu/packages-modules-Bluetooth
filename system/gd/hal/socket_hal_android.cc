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

#include <android/binder_manager.h>
#include <bluetooth/log.h>

#include "hal/socket_hal.h"

using bluetooth::hci::SocketConnectionState;
using bluetooth::hci::SocketContext;
using bluetooth::hci::SocketHalCallback;
using bluetooth::hci::SocketProperties;

namespace bluetooth {
namespace hal {

const std::vector<SocketProperties> kDefaultOffloadSocketProperties = {
        {.dataPath = hci::SocketDataPath::OFFLOAD_SOFTWARE,
         .protocolProperties = {{
                 .protocol = hci::SocketProtocol::LE_COC,
                 .numOfSocketSupported = 0,
         }}},
        {.dataPath = hci::SocketDataPath::OFFLOAD_HARDWARE,
         .protocolProperties = {{
                 .protocol = hci::SocketProtocol::LE_COC,
                 .numOfSocketSupported = 0,
         }}}};

class SocketHalAndroid : public SocketHal {
public:
  bool IsBound() override { return false; }

protected:
  void ListDependencies(ModuleList* /*list*/) const {}

  void Start() override { initialize(); }

  void Stop() override {}

  std::string ToString() const override { return std::string("SocketHalAndroid"); }

  void initialize() { log::info(""); }

  // TODO(b/342012881): Invoke socket offload HAL AIDL backend when the AIDL interface is merged.
  std::vector<SocketProperties> GetSocketProperties() override {
    return {kDefaultOffloadSocketProperties};
  }

  // TODO(b/342012881): Invoke socket offload HAL AIDL backend when the AIDL interface is merged.
  void RegisterCallback(SocketHalCallback* callback) override { socket_hal_cb_ = callback; }

  // TODO(b/342012881): Invoke socket offload HAL AIDL backend when the AIDL interface is merged.
  bool NotifySocketConnectionStateChange(const SocketContext& context) override {
    log::info("socketId {}, state {}", context.socketId, static_cast<int>(context.state));
    return true;
  }

private:
  SocketHalCallback* socket_hal_cb_;
};

const ModuleFactory SocketHal::Factory = ModuleFactory([]() { return new SocketHalAndroid(); });

}  // namespace hal
}  // namespace bluetooth
