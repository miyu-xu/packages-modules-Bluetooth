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

#include <cstdint>
#include <functional>
#include <optional>
#include <string>

#include "os/system_properties.h"

namespace test {
namespace mock {
namespace system_properties {

// Shared state between mocked functions and tests
// Name: GetSystemProperty
// Params: const std::string& property
// Return: std::optional<std::string>
struct GetSystemProperty {
  std::optional<std::string> return_value;
  std::optional<std::string> operator()(const std::string& property) {
    return return_value;
  }
};
extern struct GetSystemProperty GetSystemProperty;

// Name: GetSystemPropertyUint32
// Params: const std::string& property, uint32_t default_value
// Return: uint32_t
struct GetSystemPropertyUint32 {
  uint32_t return_value;
  uint32_t operator()(const std::string& property, uint32_t default_value) {
    return return_value;
  }
};
extern struct GetSystemPropertyUint32 GetSystemPropertyUint32;

// Name: GetSystemPropertyBool
// Params: const std::string& property, bool default_value
// Return: bool
struct GetSystemPropertyBool {
  bool return_value;
  bool operator()(const std::string& property, bool default_value) {
    return return_value;
  }
};
extern struct GetSystemPropertyBool GetSystemPropertyBool;

// Name: SetSystemProperty
// Params: const std::string& property, const std::string& value
// Return: bool
struct SetSystemProperty {
  bool return_value;
  bool operator()(const std::string& property, const std::string& value) {
    return return_value;
  }
};
extern struct SetSystemProperty SetSystemProperty;

// Name: ClearSystemPropertiesForHost
// Params:
// Return: void
struct ClearSystemPropertiesForHost {
  void operator()() {}
};
extern struct ClearSystemPropertiesForHost ClearSystemPropertiesForHost;

// Name: IsRootCanalEnabled
// Params:
// Return: bool
struct IsRootCanalEnabled {
  bool return_value;
  bool operator()() { return return_value; }
};
extern struct IsRootCanalEnabled IsRootCanalEnabled;

// Name: GetAndroidVendorReleaseVersion
// Params:
// Return: int
struct GetAndroidVendorReleaseVersion {
  int return_value;
  int operator()() { return return_value; }
};
extern struct GetAndroidVendorReleaseVersion GetAndroidVendorReleaseVersion;

}  // namespace system_properties
}  // namespace mock
}  // namespace test
