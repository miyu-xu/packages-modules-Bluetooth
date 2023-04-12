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

#include <any>
#include <map>
#include <string>

#ifdef OS_ANDROID
#include <android_autogen_include.h>
#endif

static std::map<std::string, std::any> properties_map;

#ifdef OS_ANDROID
#define FILL_PROPERTY(module, api_name, prop_name, default_value) \
  properties_map[prop_name] =                                     \
      android::sysprop::bluetooth::module::api_name().value_or(default_value);
#else
#define FILL_PROPERTY(namespace, api_name, prop_name, default_value) \
  properties_map[prop_name] = default_value;
#endif

#define GENERATE_PROPERTY_WRAPPER(module, api_name, prop_name, default_value, \
                                  type)                                       \
  type api_name() {                                                           \
    auto prop_entry = properties_map.find(prop_name);                         \
    if (prop_entry == properties_map.end()) {                                 \
      FILL_PROPERTY(module, api_name, prop_name, default_value)               \
      prop_entry = properties_map.find(prop_name);                            \
    }                                                                         \
    return std::any_cast<type>(prop_entry);                                   \
  }                                                                           \
  void api_name(type value) { properties_map[prop_name] = value; }

#include <sysprop_autogen_macro.h>
