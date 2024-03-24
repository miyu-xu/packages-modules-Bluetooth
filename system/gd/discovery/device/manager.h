/*
 * Copyright 2023 The Android Open Source Project
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

#include <memory>
#include <vector>

#include "common/contextual_callback.h"
#include "discovery/device/bt_property.h"
#include "include/hardware/bluetooth.h"
#include "module.h"
#include "module_mainloop.h"

namespace bluetooth::discovery::device {

class Manager : public Module, public ModuleMainloop {
 public:
  Manager();
  virtual ~Manager() = default;

  // @Mainloop Android APIs
  virtual void StartDiscovery(
      common::ContextualCallback<void(bt_discovery_state_t)> state_change,
      common::ContextualCallback<
          void(std::vector<std::shared_ptr<property::BtProperty>> properties)> device_found);
  virtual void CancelDiscovery(common::ContextualCallback<void(bt_discovery_state_t)> state_change);

  // @StackManager
  virtual void Start() override;
  virtual void Stop() override;
  virtual std::string ToString() const override;
  virtual void ListDependencies(ModuleList* list) const override;

  // @ModuleStateDumper
  virtual void GetDumpsysData(int fd) const override;

  static const ModuleFactory Factory;

 private:
  struct impl;
  std::shared_ptr<impl> pimpl_;
};

}  // namespace bluetooth::discovery::device
