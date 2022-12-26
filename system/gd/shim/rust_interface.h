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

#include <memory>
#include <string>
#include <utility>

#include "common/contextual_callback.h"
#include "hci/hci_packets.h"
#include "module.h"
#include "storage/storage_module.h"

namespace bluetooth {
namespace rust_shim {

storage::StorageModule* GetStorage();

// The RustInterface is the interface between C++ GD and Rust Modules. Rust Modules are not managed
// by the GD StackManager - instead, they are started in a separate JNI call after GD C++ is
// started. The shims in entry.cc are not suitable, since they depend on Legacy C++ targets
// (libbluetooth-core), which is not available to Rust Modules (that only depend on
// libbluetooth_gd). So a separate shim is required that simply exposes static pointers to started
// C++ GD modules.
//
// On stack teardown, Rust Modules will shut down before C++ GD modules do. Therefore, they will
// never consume pointers to invalid GD modules via this interface.
class RustInterface : public bluetooth::Module {
 protected:
  void ListDependencies(ModuleList* list) const override;
  void Start() override;
  void Stop() override;
  std::string ToString() const override {
    return std::string("AclSchedulerModule");
  }

 public:
  static const ModuleFactory Factory;
  RustInterface();
  ~RustInterface();
};

}  // namespace rust_shim
}  // namespace bluetooth
