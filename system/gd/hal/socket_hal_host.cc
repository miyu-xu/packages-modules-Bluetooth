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

#include "socket_hal.h"

namespace bluetooth {
namespace hal {

class SocketHalHost : public SocketHal {
public:
  bool IsBound() override { return false; }

protected:
  void ListDependencies(ModuleList* /*list*/) const {}

  void Start() override {}

  void Stop() override {}

  std::string ToString() const override { return std::string("SocketHalHost"); }

  SocketProperties GetSocketProperties() override { return socket_properties_; }

  void RegisterCallback(SocketHalCallback* /*callback*/) override {}

  void SetAclCredits(int /*link*/, int /*credit*/) override {}

  void NotifyAclConnectionStateChange(uint16_t /*handle*/, int /*link*/, int /*state*/) override {}

  void NotifyAclLeDataLengthChange(uint16_t /*handle*/, int /*txDataLen*/, int /*rxDataLen*/) override {
  }

  void NotifyAclPowerModeChange(uint16_t /*handle*/, int /*mode*/, uint16_t /*interval*/) override {
  }

  void NotifySocketConnectionStateChange(SocketContext& /*context*/, int /*state*/) override {}

private:
  SocketProperties socket_properties_;
  SocketHalCallback* socket_hal_cb_;
};

const ModuleFactory SocketHal::Factory = ModuleFactory([]() { return new SocketHalHost(); });

}  // namespace hal
}  // namespace bluetooth