/*
 * Copyright 2025 The Android Open Source Project
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

#include <string>
#include <vector>

namespace bluetooth {
namespace common {

class ComponentDisabler {
 public:
  // Disables the specified components of the given package.
  // Returns true on success, false on any failure.
  static bool DisableComponents(const std::string& package_name);
  static bool DisableComponents(); // use default package name

 private:
    static const std::string default_bt_package_name_;
};

}  // namespace common
}  // namespace bluetooth