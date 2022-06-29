/*
 * Copyright 2020 The Android Open Source Project
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

#include "sysprops/sysprops_module.h"

#include <filesystem>

#include "os/handler.h"
#include "os/log.h"
#include "os/system_properties.h"
#include "storage/legacy_config_file.h"

namespace bluetooth {
namespace sysprops {

static const size_t kDefaultCapacity = 10000;

const std::string kLePrivacy = "bluetooth.core.gap.le.privacy.enabled";

const ModuleFactory SyspropsModule::Factory = ModuleFactory([]() { return new SyspropsModule(); });

struct SyspropsModule::impl {
  impl(os::Handler* sysprops_handler) : sysprops_handler_(sysprops_handler) {}

  os::Handler* sysprops_handler_;
};

void SyspropsModule::ListDependencies(ModuleList* list) const {
}

void SyspropsModule::Start() {  
  // TODO: Need to make a os::ParameterProvider::SyspropsFilePath 
  // For most OS's it will be empty, Linux will have the define below
  std::string file_path = "/etc/bluetooth/sysprops.conf";
  parse_config(file_path);  
  for (const auto & entry : std::filesystem::directory_iterator(file_path + ".d")) {
    parse_config(entry.path());   
  }

  pimpl_ = std::make_unique<impl>(GetHandler());
}

void SyspropsModule::Stop() {
  pimpl_.reset();
}

std::string SyspropsModule::ToString() const {
  return "Sysprops Module";
}

void SyspropsModule::parse_config(std::string file_path) {
  auto config = storage::LegacyConfigFile::FromPath(file_path).Read(kDefaultCapacity);
  if (!config) {
    return;
  }

  // TODO: Instead of re-listing out all the sysprops, just get a list of properties from config and iterate
  // Will need to update storage/config_cache
  auto str = config->GetProperty("Sysprops", kLePrivacy);
  if (str) {
    bluetooth::os::SetSystemProperty(kLePrivacy, *str);
  }
}

}  // namespace sysprops
}  // namespace bluetooth
