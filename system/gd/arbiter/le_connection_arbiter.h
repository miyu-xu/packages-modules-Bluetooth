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

#include "hci/acl_manager.h"
#include "module.h"

namespace bluetooth {
namespace arbiter {

struct DefaultFilter {};
struct AdvertisingSetFilter {
  uint8_t advertising_set_id;
};

using ConnectionFilter = std::variant<DefaultFilter, AdvertisingSetFilter>;

// The LeConnectionArbiter allows us to dispatch incoming LE connection to one of multiple BLE stacks (Fluoride,
// GMSCore, etc). Each client can register a set of callbacks, as well as an advertising set ID, and the Arbiter will
// dispatch the callbacks depending on the filter provided. If multiple filters match, the latest one added will take
// priority.
class LeConnectionArbiterModule : public bluetooth::Module {
 public:
  // Register callbacks to be invoked when a connection arrives. For now, this is just a pass-through into the
  // acl_manager, so the advertising set ID is not needed here.
  void RegisterLeCallbacks(
      hci::acl_manager::LeConnectionCallbacks* callbacks, os::Handler* handler, ConnectionFilter filter);

  // When removing filters, if multiple filters are enrolled for the same callback that compare with equality, the MOST
  // RECENTLY ADDED will be removed. This behavior is kept for simplicity of implementation, but it is not ideal.
  void UnregisterLeCallbacks(
      hci::acl_manager::LeConnectionCallbacks* callbacks, ConnectionFilter filter, std::promise<void> promise);

 private:
  struct impl;
  std::unique_ptr<impl> pimpl_;

 protected:
  void ListDependencies(ModuleList* list) const override;
  void Start() override;
  void Stop() override;
  std::string ToString() const override {
    return std::string("LeConnectionArbiterModule");
  }

 public:
  static const ModuleFactory Factory;
  LeConnectionArbiterModule();
  ~LeConnectionArbiterModule();
};

}  // namespace arbiter
}  // namespace bluetooth
