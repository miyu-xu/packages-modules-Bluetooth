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

#include "socket_hal.h"

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

  SocketProperties GetSocketProperties() override {
    log::info("numOfLeCocSocket {}, numOfRfcommSocket {}, size {}",
              socket_properties_.numOfLeCocSocketSupported_,
              socket_properties_.numOfRfcommSocketSupported_,
              socket_properties_.protocolProperties_.size());
    return socket_properties_;
  }

  void RegisterCallback(SocketHalCallback* callback) override { socket_hal_cb_ = callback; }

  bool SetAclCredits(int linkType, int credit) override {
    log::info("linkType {}, credit {}", linkType, credit);
    return true;
  }

  bool NotifyAclConnectionStateChange(int aclHandle, int linkType, int state) override {
    log::info("aclHandle {}, linkType {}, state {}", aclHandle, linkType, state);
    return true;
  }

  bool NotifyAclLeDataLengthChange(int aclHandle, int txDataLen, int rxDataLen) override {
    log::info("aclHandle {}, txDataLen {}, rxDataLen {}", aclHandle, txDataLen, rxDataLen);
    return true;
  }

  bool NotifyAclPowerModeChange(int aclHandle, int powerMode, int interval) override {
    log::info("aclHandle {}, powerMode {}, interval {}", aclHandle, powerMode, interval);
    return true;
  }

  bool NotifySocketConnectionStateChange(SocketContext& context) override {
    log::info("socketId {}, state {}", context.socketId, context.state);
    return true;
  }

private:
  SocketProperties socket_properties_;
  SocketHalCallback* socket_hal_cb_;
};

const ModuleFactory SocketHal::Factory = ModuleFactory([]() { return new SocketHalAndroid(); });

}  // namespace hal
}  // namespace bluetooth