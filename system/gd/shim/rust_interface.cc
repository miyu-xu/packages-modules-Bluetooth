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

#include "rust_interface.h"

#include <optional>
#include <queue>
#include <unordered_set>
#include <variant>

namespace bluetooth {
namespace rust_shim {
namespace {
storage::StorageModule* storage_module;
}

storage::StorageModule* GetStorage() {
  return storage_module;
}

const ModuleFactory RustInterface::Factory = ModuleFactory([]() { return new RustInterface(); });

RustInterface::RustInterface() = default;
RustInterface::~RustInterface() = default;
void RustInterface::ListDependencies(ModuleList* list) const {
  list->add<storage::StorageModule>();
}

void RustInterface::Start() {
  storage_module = GetDependency<storage::StorageModule>();
}

void RustInterface::Stop() {}

}  // namespace rust_shim
}  // namespace bluetooth
