/*
 * Copyright 2021 The Android Open Source Project
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

/*
 * Generated mock file from original source file
 *   Functions generated:4
 *
 *  mockcify.pl ver 0.3.2
 */

#include <cstdint>
#include <functional>
#include <map>
#include <string>

extern std::map<std::string, int> mock_function_count_map;

// Original included files, if any
// NOTE: Since this is a mock file with mock definitions some number of
//       include files may not be required.  The include-what-you-use
//       still applies, but crafting proper inclusion is out of scope
//       for this effort.  This compilation unit may compile as-is, or
//       may need attention to prune from (or add to ) the inclusion set.
#include <string.h>

#include "osi/include/compat.h"
#include "stack/include/bt_name.h"

// Mocked compile conditionals, if any

namespace test {
namespace mock {
namespace stack_btm_bt_name {

// Shared state between mocked functions and tests
// Name: btm_loc_bd_name_is_set
// Params: const tBTM_LOC_BD_NAME& btm_loc_bd_name
// Return: bool
struct btm_loc_bd_name_is_set {
  static bool return_value;
  std::function<bool(const tBTM_LOC_BD_NAME& btm_loc_bd_name)> body{
      [](const tBTM_LOC_BD_NAME& btm_loc_bd_name) { return return_value; }};
  bool operator()(const tBTM_LOC_BD_NAME& btm_loc_bd_name) {
    return body(btm_loc_bd_name);
  };
};
extern struct btm_loc_bd_name_is_set btm_loc_bd_name_is_set;

// Name: btm_loc_bd_name_length
// Params: const tBTM_LOC_BD_NAME& btm_loc_bd_name
// Return: size_t
struct btm_loc_bd_name_length {
  static size_t return_value;
  std::function<size_t(const tBTM_LOC_BD_NAME& btm_loc_bd_name)> body{
      [](const tBTM_LOC_BD_NAME& btm_loc_bd_name) { return return_value; }};
  size_t operator()(const tBTM_LOC_BD_NAME& btm_loc_bd_name) {
    return body(btm_loc_bd_name);
  };
};
extern struct btm_loc_bd_name_length btm_loc_bd_name_length;

// Name: btm_loc_bd_name_set
// Params: tBTM_LOC_BD_NAME& btm_loc_bd_name, const char* name
// Return: size_t
struct btm_loc_bd_name_set {
  static size_t return_value;
  std::function<size_t(tBTM_LOC_BD_NAME& btm_loc_bd_name, const char* name)>
      body{[](tBTM_LOC_BD_NAME& btm_loc_bd_name, const char* name) {
        return return_value;
      }};
  size_t operator()(tBTM_LOC_BD_NAME& btm_loc_bd_name, const char* name) {
    return body(btm_loc_bd_name, name);
  };
};
extern struct btm_loc_bd_name_set btm_loc_bd_name_set;

// Name: btm_loc_bd_name_text
// Params: const tBTM_LOC_BD_NAME& btm_loc_bd_name
// Return: const char*
struct btm_loc_bd_name_text {
  static const char* return_value;
  std::function<const char*(const tBTM_LOC_BD_NAME& btm_loc_bd_name)> body{
      [](const tBTM_LOC_BD_NAME& btm_loc_bd_name) { return return_value; }};
  const char* operator()(const tBTM_LOC_BD_NAME& btm_loc_bd_name) {
    return body(btm_loc_bd_name);
  };
};
extern struct btm_loc_bd_name_text btm_loc_bd_name_text;

}  // namespace stack_btm_bt_name
}  // namespace mock
}  // namespace test

// END mockcify generation