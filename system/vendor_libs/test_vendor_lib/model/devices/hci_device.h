/*
 * Copyright 2022 The Android Open Source Project
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

#include <functional>                               // for function
#include <memory>                                   // for shared_ptr, make_...
#include <string>                                   // for string

#include "model/controller/dual_mode_controller.h"  // for DualModeController
#include "model/devices/hci_transport.h"

namespace test_vendor_lib {

class HciDevice : public DualModeController {
 public:
  HciDevice(std::shared_ptr<HciTransport> transport);
  ~HciDevice() = default;

  static std::shared_ptr<HciDevice> Create(
      std::shared_ptr<HciTransport> transport) {
    return std::make_shared<HciDevice>(transport);
  }

  virtual std::string GetTypeString() const override { return "hci_device"; }

  virtual void TimerTick() override;

  void RegisterCloseCallback(std::function<void()>);

 private:
  std::shared_ptr<HciTransport> transport_;
  std::function<void()> close_callback_;
};

}  // namespace test_vendor_lib
