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

using bluetooth::hci::AclLinkType;
using bluetooth::hci::AclPowerMode;
using bluetooth::hci::ConnectionState;
using bluetooth::hci::SocketContext;
using bluetooth::hci::SocketHalCallback;
using bluetooth::hci::SocketProperties;

namespace bluetooth {
namespace hal {

class SocketHalAndroid : public SocketHal {
public:
  bool IsBound() override { return false; }

protected:
  void ListDependencies(ModuleList* /*list*/) const {}

  void Start() override { initialize(); }

  void Stop() override { socket_properties_ = {}; }

  std::string ToString() const override { return std::string("SocketHalAndroid"); }

  void initialize() { log::info(""); }

  // TODO(b/342012881): Invoke socket offload HAL AIDL backend when the AIDL interface is merged.
  SocketProperties GetSocketProperties() override {
    log::info("numOfLeCocSocket {}, numOfRfcommSocket {}, size {}",
              socket_properties_.numOfLeCocSocketSupported_,
              socket_properties_.numOfRfcommSocketSupported_,
              socket_properties_.protocolProperties_.size());
    return socket_properties_;
  }

  // TODO(b/342012881): Invoke socket offload HAL AIDL backend when the AIDL interface is merged.
  void RegisterCallback(SocketHalCallback* callback) override { socket_hal_cb_ = callback; }
  bool SetAclCredits(AclLinkType linkType, int credit) override {
    log::info("linkType {}, credit {}", static_cast<int>(linkType), credit);
    return true;
  }

  // TODO(b/342012881): Invoke socket offload HAL AIDL backend when the AIDL interface is merged.
  bool NotifyAclConnectionStateChange(int aclHandle, AclLinkType linkType,
                                      ConnectionState state) override {
    log::info("aclHandle 0x{:04x}, linkType {}, state {}", static_cast<uint16_t>(aclHandle),
              static_cast<int>(linkType), static_cast<int>(state));
    return true;
  }

  // TODO(b/342012881): Invoke socket offload HAL AIDL backend when the AIDL interface is merged.
  bool NotifyAclLeDataLengthChange(int aclHandle, int txDataLen, int rxDataLen) override {
    log::info("aclHandle 0x{:04x}, txDataLen {}, rxDataLen {}", static_cast<uint16_t>(aclHandle),
              txDataLen, rxDataLen);
    return true;
  }

  // TODO(b/342012881): Invoke socket offload HAL AIDL backend when the AIDL interface is merged.
  bool NotifyAclPowerModeChange(int aclHandle, AclPowerMode powerMode, int interval) override {
    log::info("aclHandle 0x{:04x}, powerMode {}, interval {}", static_cast<uint16_t>(aclHandle),
              static_cast<int>(powerMode), interval);
    return true;
  }

  // TODO(b/342012881): Invoke socket offload HAL AIDL backend when the AIDL interface is merged.
  bool NotifySocketConnectionStateChange(const SocketContext& context) override {
    log::info("socketId {}, state {}", context.socketId, static_cast<int>(context.state));
    return true;
  }

private:
  SocketProperties socket_properties_;
  SocketHalCallback* socket_hal_cb_;
};

const ModuleFactory SocketHal::Factory = ModuleFactory([]() { return new SocketHalAndroid(); });

}  // namespace hal
}  // namespace bluetooth
