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

// Mock include file to share data between tests and mock
#include "test/mock/mock_stack_btm_bt_name.h"

// Mocked internal structures, if any

namespace test {
namespace mock {
namespace stack_btm_bt_name {

// Function state capture and return values, if needed
struct btm_loc_bd_name_is_set btm_loc_bd_name_is_set;
struct btm_loc_bd_name_length btm_loc_bd_name_length;
struct btm_loc_bd_name_set btm_loc_bd_name_set;
struct btm_loc_bd_name_text btm_loc_bd_name_text;

}  // namespace stack_btm_bt_name
}  // namespace mock
}  // namespace test

// Mocked function return values, if any
namespace test {
namespace mock {
namespace stack_btm_bt_name {

bool btm_loc_bd_name_is_set::return_value = false;
size_t btm_loc_bd_name_length::return_value = 0;
size_t btm_loc_bd_name_set::return_value = 0;
const char* btm_loc_bd_name_text::return_value = nullptr;

}  // namespace stack_btm_bt_name
}  // namespace mock
}  // namespace test

// Mocked functions, if any
bool btm_loc_bd_name_is_set(const tBTM_LOC_BD_NAME& btm_loc_bd_name) {
  mock_function_count_map[__func__]++;
  return test::mock::stack_btm_bt_name::btm_loc_bd_name_is_set(btm_loc_bd_name);
}
size_t btm_loc_bd_name_length(const tBTM_LOC_BD_NAME& btm_loc_bd_name) {
  mock_function_count_map[__func__]++;
  return test::mock::stack_btm_bt_name::btm_loc_bd_name_length(btm_loc_bd_name);
}
size_t btm_loc_bd_name_set(tBTM_LOC_BD_NAME& btm_loc_bd_name,
                           const char* name) {
  mock_function_count_map[__func__]++;
  return test::mock::stack_btm_bt_name::btm_loc_bd_name_set(btm_loc_bd_name,
                                                            name);
}
const char* btm_loc_bd_name_text(const tBTM_LOC_BD_NAME& btm_loc_bd_name) {
  mock_function_count_map[__func__]++;
  return test::mock::stack_btm_bt_name::btm_loc_bd_name_text(btm_loc_bd_name);
}
// Mocked functions complete
// END mockcify generation
