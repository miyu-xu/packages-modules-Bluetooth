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
/*
 * Generated mock file from original source file
 *   Functions generated:16
 *
 *  mockcify.pl ver 0.5.0
 */

#include <cstdint>
#include <functional>
#include <map>
#include <string>

extern std::map<std::string, int> mock_function_count_map;

// Mock include file to share data between tests and mock
#include "test/mock/mock_stack_btm_dev.h"

// Original usings

// Mocked internal structures, if any

namespace test {
namespace mock {
namespace stack_btm_dev {

// Function state capture and return values, if needed
struct BTM_SecAddDevice BTM_SecAddDevice;
struct BTM_SecClearSecurityFlags BTM_SecClearSecurityFlags;
struct BTM_SecDeleteDevice BTM_SecDeleteDevice;
struct BTM_SecReadDevName BTM_SecReadDevName;
struct btm_consolidate_dev btm_consolidate_dev;
struct btm_dev_support_role_switch btm_dev_support_role_switch;
struct btm_find_dev btm_find_dev;
struct btm_find_dev_by_handle btm_find_dev_by_handle;
struct btm_find_or_alloc_dev btm_find_or_alloc_dev;
struct btm_get_bond_type_dev btm_get_bond_type_dev;
struct btm_sec_alloc_dev btm_sec_alloc_dev;
struct btm_sec_allocate_dev_rec btm_sec_allocate_dev_rec;
struct btm_set_bond_type_dev btm_set_bond_type_dev;
struct is_address_equal is_address_equal;
struct is_handle_equal is_handle_equal;
struct wipe_secrets_and_remove wipe_secrets_and_remove;

}  // namespace stack_btm_dev
}  // namespace mock
}  // namespace test

// Mocked function return values, if any
namespace test {
namespace mock {
namespace stack_btm_dev {

bool BTM_SecAddDevice::return_value = false;
bool BTM_SecDeleteDevice::return_value = false;
char* BTM_SecReadDevName::return_value = nullptr;
bool btm_dev_support_role_switch::return_value = false;
tBTM_SEC_DEV_REC* btm_find_dev::return_value = nullptr;
tBTM_SEC_DEV_REC* btm_find_dev_by_handle::return_value = nullptr;
tBTM_SEC_DEV_REC* btm_find_or_alloc_dev::return_value = nullptr;
tBTM_SEC_DEV_REC::tBTM_BOND_TYPE btm_get_bond_type_dev::return_value =
    tBTM_SEC_DEV_REC::BOND_TYPE_UNKNOWN;
tBTM_SEC_DEV_REC* btm_sec_alloc_dev::return_value = nullptr;
tBTM_SEC_DEV_REC* btm_sec_allocate_dev_rec::return_value = nullptr;
bool btm_set_bond_type_dev::return_value = false;
bool is_address_equal::return_value = false;
bool is_handle_equal::return_value = false;

}  // namespace stack_btm_dev
}  // namespace mock
}  // namespace test

// Mocked functions, if any
bool BTM_SecAddDevice(const RawAddress& bd_addr, DEV_CLASS dev_class,
                      const BD_NAME& bd_name, uint8_t* features,
                      LinkKey* p_link_key, uint8_t key_type,
                      uint8_t pin_length) {
  mock_function_count_map[__func__]++;
  return test::mock::stack_btm_dev::BTM_SecAddDevice(
      bd_addr, dev_class, bd_name, features, p_link_key, key_type, pin_length);
}
void BTM_SecClearSecurityFlags(const RawAddress& bd_addr) {
  mock_function_count_map[__func__]++;
  test::mock::stack_btm_dev::BTM_SecClearSecurityFlags(bd_addr);
}
bool BTM_SecDeleteDevice(const RawAddress& bd_addr) {
  mock_function_count_map[__func__]++;
  return test::mock::stack_btm_dev::BTM_SecDeleteDevice(bd_addr);
}
char* BTM_SecReadDevName(const RawAddress& bd_addr) {
  mock_function_count_map[__func__]++;
  return test::mock::stack_btm_dev::BTM_SecReadDevName(bd_addr);
}
void btm_consolidate_dev(tBTM_SEC_DEV_REC* p_target_rec) {
  mock_function_count_map[__func__]++;
  test::mock::stack_btm_dev::btm_consolidate_dev(p_target_rec);
}
bool btm_dev_support_role_switch(const RawAddress& bd_addr) {
  mock_function_count_map[__func__]++;
  return test::mock::stack_btm_dev::btm_dev_support_role_switch(bd_addr);
}
tBTM_SEC_DEV_REC* btm_find_dev(const RawAddress& bd_addr) {
  mock_function_count_map[__func__]++;
  return test::mock::stack_btm_dev::btm_find_dev(bd_addr);
}
tBTM_SEC_DEV_REC* btm_find_dev_by_handle(uint16_t handle) {
  mock_function_count_map[__func__]++;
  return test::mock::stack_btm_dev::btm_find_dev_by_handle(handle);
}
tBTM_SEC_DEV_REC* btm_find_or_alloc_dev(const RawAddress& bd_addr) {
  mock_function_count_map[__func__]++;
  return test::mock::stack_btm_dev::btm_find_or_alloc_dev(bd_addr);
}
tBTM_SEC_DEV_REC::tBTM_BOND_TYPE btm_get_bond_type_dev(
    const RawAddress& bd_addr) {
  mock_function_count_map[__func__]++;
  return test::mock::stack_btm_dev::btm_get_bond_type_dev(bd_addr);
}
tBTM_SEC_DEV_REC* btm_sec_alloc_dev(const RawAddress& bd_addr) {
  mock_function_count_map[__func__]++;
  return test::mock::stack_btm_dev::btm_sec_alloc_dev(bd_addr);
}
tBTM_SEC_DEV_REC* btm_sec_allocate_dev_rec(void) {
  mock_function_count_map[__func__]++;
  return test::mock::stack_btm_dev::btm_sec_allocate_dev_rec();
}
bool btm_set_bond_type_dev(const RawAddress& bd_addr,
                           tBTM_SEC_DEV_REC::tBTM_BOND_TYPE bond_type) {
  mock_function_count_map[__func__]++;
  return test::mock::stack_btm_dev::btm_set_bond_type_dev(bd_addr, bond_type);
}
bool is_address_equal(void* data, void* context) {
  mock_function_count_map[__func__]++;
  return test::mock::stack_btm_dev::is_address_equal(data, context);
}
bool is_handle_equal(void* data, void* context) {
  mock_function_count_map[__func__]++;
  return test::mock::stack_btm_dev::is_handle_equal(data, context);
}
void wipe_secrets_and_remove(tBTM_SEC_DEV_REC* p_dev_rec) {
  mock_function_count_map[__func__]++;
  test::mock::stack_btm_dev::wipe_secrets_and_remove(p_dev_rec);
}
// Mocked functions complete
// END mockcify generation
