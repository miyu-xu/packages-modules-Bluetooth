/*
 * Copyright (C) 2024 The Android Open Source Project
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
 * limitations under the License
 */

#include "gd/os/system_properties.h"
#include "server_configurable_flags/get_flags.h"

namespace server_configurable_flags {
static std::string MakeSystemPropertyName(
    const std::string& experiment_category_name,
    const std::string& experiment_flag_name) {
  constexpr char kSystemPropertyPrefix[] = "persist.device_config.";
  constexpr char kBtFlags[] = "com.android.bluetooth.flags";

  auto StripPackageName = [](const std::string& full_name,
                             const std::string& package_name) {
    auto pos = full_name.find(package_name);
    if (pos != std::string::npos) {
      return full_name.substr(pos + package_name.length() + 1);
    }
    return full_name;
  };

  auto flag_name = StripPackageName(experiment_flag_name, kBtFlags);
  return kSystemPropertyPrefix + experiment_category_name + "." + flag_name;
}

std::string GetServerConfigurableFlag(
    const std::string& experiment_category_name,
    const std::string& experiment_flag_name, const std::string& default_value) {
  auto name =
      MakeSystemPropertyName(experiment_category_name, experiment_flag_name);
  return bluetooth::os::GetSystemProperty(std::string(name))
      .value_or(default_value);
}
}  // namespace server_configurable_flags
