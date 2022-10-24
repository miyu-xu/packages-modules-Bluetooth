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

#include "test/mock/mock_system_properties.h"

#include <map>
#include <optional>
#include <string>

extern std::map<std::string, int> mock_function_count_map;

namespace test {
namespace mock {
namespace system_properties {

struct GetSystemProperty GetSystemProperty;
struct GetSystemPropertyUint32 GetSystemPropertyUint32;
struct GetSystemPropertyBool GetSystemPropertyBool;
struct SetSystemProperty SetSystemProperty;
struct ClearSystemPropertiesForHost ClearSystemPropertiesForHost;
struct IsRootCanalEnabled IsRootCanalEnabled;
struct GetAndroidVendorReleaseVersion GetAndroidVendorReleaseVersion;

}  // namespace system_properties
}  // namespace mock
}  // namespace test

namespace bluetooth {
namespace os {

std::optional<std::string> GetSystemProperty(const std::string& property) {
  mock_function_count_map[__func__]++;
  return test::mock::system_properties::GetSystemProperty(property);
}

uint32_t GetSystemPropertyUint32(const std::string& property,
                                 uint32_t default_value) {
  mock_function_count_map[__func__]++;
  return test::mock::system_properties::GetSystemPropertyUint32(property,
                                                                default_value);
}

bool GetSystemPropertyBool(const std::string& property, bool default_value) {
  mock_function_count_map[__func__]++;
  return test::mock::system_properties::GetSystemPropertyBool(property,
                                                              default_value);
}

bool SetSystemProperty(const std::string& property, const std::string& value) {
  mock_function_count_map[__func__]++;
  return test::mock::system_properties::SetSystemProperty(property, value);
}

void ClearSystemPropertiesForHost() { mock_function_count_map[__func__]++; }

bool IsRootCanalEnabled() {
  mock_function_count_map[__func__]++;
  return test::mock::system_properties::IsRootCanalEnabled();
}

int GetAndroidVendorReleaseVersion() {
  mock_function_count_map[__func__]++;
  return test::mock::system_properties::GetAndroidVendorReleaseVersion();
}

}  // namespace os
}  // namespace bluetooth
