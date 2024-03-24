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

#include "discovery/device/manager.h"

#include <base/functional/bind.h>
#include <base/functional/callback.h>
#include <base/location.h>
#include <bluetooth/log.h>

#include <functional>
#include <memory>
#include <string>
#include <vector>

#include "discovery/device/bt_property.h"
#include "hardware/bluetooth.h"
#include "macros.h"
#include "main/shim/dumpsys.h"

namespace {
constexpr char kModuleName[] = "DeviceDiscovery";
constexpr char kBtmLogTag[] = "DVDISC";
constexpr char kDumpTag[] = "discovery::device";

enum class DeviceDiscoveryState {
  kIdle,
  kStarting,
  kActive,
  kCanceling,
};

std::string DeviceDiscoveryStateText(const DeviceDiscoveryState& state) {
  switch (state) {
    CASE_RETURN_TEXT(DeviceDiscoveryState::kIdle);
    CASE_RETURN_TEXT(DeviceDiscoveryState::kStarting);
    CASE_RETURN_TEXT(DeviceDiscoveryState::kActive);
    CASE_RETURN_TEXT(DeviceDiscoveryState::kCanceling);
  }
}

}  // namespace

namespace bluetooth::discovery::device {

struct Manager::impl : public ModuleMainloop {
  struct {
    DeviceDiscoveryState state_{DeviceDiscoveryState::kIdle};
  } classic_inquiry_, le_scan_;

  // @Mainloop
  void start_discovery(
      common::ContextualCallback<void(bt_discovery_state_t)> state_change,
      common::ContextualCallback<
          void(std::vector<std::shared_ptr<property::BtProperty>> properties)> device_found) {
    state_change_ = state_change;
    device_found_ = device_found;
    log::info("API start device discovery");
  }

  void cancel_discovery(common::ContextualCallback<void(bt_discovery_state_t)> state_change) {
    log::info("API cancel device discovery");
    state_change_ = state_change;
  }

 private:
  common::ContextualCallback<void(bt_discovery_state_t)> state_change_{};
  common::ContextualCallback<void(std::vector<std::shared_ptr<property::BtProperty>> properties)>
      device_found_;
};

void Manager::StartDiscovery(
    common::ContextualCallback<void(bt_discovery_state_t)> state_change,
    common::ContextualCallback<void(std::vector<std::shared_ptr<property::BtProperty>> properties)>
        device_found) {
  PostMethodOnMain(pimpl_, &impl::start_discovery, state_change, device_found);
}

void Manager::CancelDiscovery(common::ContextualCallback<void(bt_discovery_state_t)> state_change) {
  PostMethodOnMain(pimpl_, &impl::cancel_discovery, state_change);
}

std::string Manager::ToString() const {
  return std::string(kModuleName);
}

void Manager::ListDependencies(ModuleList* /* list */) const {};

Manager::Manager() {
  pimpl_ = std::make_shared<impl>();
}

void Manager::Start() {
  log::debug("Started device discovery manager");
}

void Manager::Stop() {
  log::debug("Stopping device discovery manager");
}

void Manager::GetDumpsysData(int fd) const {
  std::shared_ptr<impl> dumpsys_pimpl_ = pimpl_;
  if (!dumpsys_pimpl_) return;

  LOG_DUMPSYS_TITLE(fd, kDumpTag);
  dprintf(
      fd,
      "%s Current state classic_inquiry:%s le_scan:%s\n",
      kDumpTag,
      DeviceDiscoveryStateText(dumpsys_pimpl_->classic_inquiry_.state_).c_str(),
      DeviceDiscoveryStateText(dumpsys_pimpl_->le_scan_.state_).c_str());
  dprintf(fd, "%s  ---------\n", kDumpTag);
}

const ModuleFactory Manager::Factory = ModuleFactory([]() { return new Manager(); });

}  // namespace bluetooth::discovery::device
