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

// Original included files, if any
// NOTE: Since this is a mock file with mock definitions some number of
//       include files may not be required.  The include-what-you-use
//       still applies, but crafting proper inclusion is out of scope
//       for this effort.  This compilation unit may compile as-is, or
//       may need attention to prune from (or add to ) the inclusion set.
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "btm_api.h"
#include "device/include/controller.h"
#include "l2c_api.h"
#include "main/shim/btm_api.h"
#include "main/shim/dumpsys.h"
#include "main/shim/shim.h"
#include "osi/include/allocator.h"
#include "osi/include/compat.h"
#include "stack/btm/btm_dev.h"
#include "stack/include/acl_api.h"
#include "stack/include/bt_octets.h"
#include "types/raw_address.h"

// Original usings

// Mocked compile conditionals, if any

namespace test {
namespace mock {
namespace stack_btm_dev {

// Shared state between mocked functions and tests
// Name: BTM_SecAddDevice
// Params: const RawAddress& bd_addr, DEV_CLASS dev_class, const BD_NAME&
// bd_name, uint8_t* features, LinkKey* p_link_key, uint8_t key_type, uint8_t
// pin_length Return: bool
struct BTM_SecAddDevice {
  static bool return_value;
  std::function<bool(const RawAddress& bd_addr, DEV_CLASS dev_class,
                     const BD_NAME& bd_name, uint8_t* features,
                     LinkKey* p_link_key, uint8_t key_type, uint8_t pin_length)>
      body{[](const RawAddress& bd_addr, DEV_CLASS dev_class,
              const BD_NAME& bd_name, uint8_t* features, LinkKey* p_link_key,
              uint8_t key_type, uint8_t pin_length) { return return_value; }};
  bool operator()(const RawAddress& bd_addr, DEV_CLASS dev_class,
                  const BD_NAME& bd_name, uint8_t* features,
                  LinkKey* p_link_key, uint8_t key_type, uint8_t pin_length) {
    return body(bd_addr, dev_class, bd_name, features, p_link_key, key_type,
                pin_length);
  };
};
extern struct BTM_SecAddDevice BTM_SecAddDevice;

// Name: BTM_SecClearSecurityFlags
// Params: const RawAddress& bd_addr
// Return: void
struct BTM_SecClearSecurityFlags {
  std::function<void(const RawAddress& bd_addr)> body{
      [](const RawAddress& bd_addr) {}};
  void operator()(const RawAddress& bd_addr) { body(bd_addr); };
};
extern struct BTM_SecClearSecurityFlags BTM_SecClearSecurityFlags;

// Name: BTM_SecDeleteDevice
// Params: const RawAddress& bd_addr
// Return: bool
struct BTM_SecDeleteDevice {
  static bool return_value;
  std::function<bool(const RawAddress& bd_addr)> body{
      [](const RawAddress& bd_addr) { return return_value; }};
  bool operator()(const RawAddress& bd_addr) { return body(bd_addr); };
};
extern struct BTM_SecDeleteDevice BTM_SecDeleteDevice;

// Name: BTM_SecReadDevName
// Params: const RawAddress& bd_addr
// Return: char*
struct BTM_SecReadDevName {
  static char* return_value;
  std::function<char*(const RawAddress& bd_addr)> body{
      [](const RawAddress& bd_addr) { return return_value; }};
  char* operator()(const RawAddress& bd_addr) { return body(bd_addr); };
};
extern struct BTM_SecReadDevName BTM_SecReadDevName;

// Name: btm_consolidate_dev
// Params: tBTM_SEC_DEV_REC* p_target_rec
// Return: void
struct btm_consolidate_dev {
  std::function<void(tBTM_SEC_DEV_REC* p_target_rec)> body{
      [](tBTM_SEC_DEV_REC* p_target_rec) {}};
  void operator()(tBTM_SEC_DEV_REC* p_target_rec) { body(p_target_rec); };
};
extern struct btm_consolidate_dev btm_consolidate_dev;

// Name: btm_dev_support_role_switch
// Params: const RawAddress& bd_addr
// Return: bool
struct btm_dev_support_role_switch {
  static bool return_value;
  std::function<bool(const RawAddress& bd_addr)> body{
      [](const RawAddress& bd_addr) { return return_value; }};
  bool operator()(const RawAddress& bd_addr) { return body(bd_addr); };
};
extern struct btm_dev_support_role_switch btm_dev_support_role_switch;

// Name: btm_find_dev
// Params: const RawAddress& bd_addr
// Return: tBTM_SEC_DEV_REC*
struct btm_find_dev {
  static tBTM_SEC_DEV_REC* return_value;
  std::function<tBTM_SEC_DEV_REC*(const RawAddress& bd_addr)> body{
      [](const RawAddress& bd_addr) { return return_value; }};
  tBTM_SEC_DEV_REC* operator()(const RawAddress& bd_addr) {
    return body(bd_addr);
  };
};
extern struct btm_find_dev btm_find_dev;

// Name: btm_find_dev_by_handle
// Params: uint16_t handle
// Return: tBTM_SEC_DEV_REC*
struct btm_find_dev_by_handle {
  static tBTM_SEC_DEV_REC* return_value;
  std::function<tBTM_SEC_DEV_REC*(uint16_t handle)> body{
      [](uint16_t handle) { return return_value; }};
  tBTM_SEC_DEV_REC* operator()(uint16_t handle) { return body(handle); };
};
extern struct btm_find_dev_by_handle btm_find_dev_by_handle;

// Name: btm_find_or_alloc_dev
// Params: const RawAddress& bd_addr
// Return: tBTM_SEC_DEV_REC*
struct btm_find_or_alloc_dev {
  static tBTM_SEC_DEV_REC* return_value;
  std::function<tBTM_SEC_DEV_REC*(const RawAddress& bd_addr)> body{
      [](const RawAddress& bd_addr) { return return_value; }};
  tBTM_SEC_DEV_REC* operator()(const RawAddress& bd_addr) {
    return body(bd_addr);
  };
};
extern struct btm_find_or_alloc_dev btm_find_or_alloc_dev;

// Name: btm_get_bond_type_dev
// Params: const RawAddress& bd_addr
// Return: tBTM_SEC_DEV_REC::tBTM_BOND_TYPE
struct btm_get_bond_type_dev {
  static tBTM_SEC_DEV_REC::tBTM_BOND_TYPE return_value;
  std::function<tBTM_SEC_DEV_REC::tBTM_BOND_TYPE(const RawAddress& bd_addr)>
      body{[](const RawAddress& bd_addr) { return return_value; }};
  tBTM_SEC_DEV_REC::tBTM_BOND_TYPE operator()(const RawAddress& bd_addr) {
    return body(bd_addr);
  };
};
extern struct btm_get_bond_type_dev btm_get_bond_type_dev;

// Name: btm_sec_alloc_dev
// Params: const RawAddress& bd_addr
// Return: tBTM_SEC_DEV_REC*
struct btm_sec_alloc_dev {
  static tBTM_SEC_DEV_REC* return_value;
  std::function<tBTM_SEC_DEV_REC*(const RawAddress& bd_addr)> body{
      [](const RawAddress& bd_addr) { return return_value; }};
  tBTM_SEC_DEV_REC* operator()(const RawAddress& bd_addr) {
    return body(bd_addr);
  };
};
extern struct btm_sec_alloc_dev btm_sec_alloc_dev;

// Name: btm_sec_allocate_dev_rec
// Params: void
// Return: tBTM_SEC_DEV_REC*
struct btm_sec_allocate_dev_rec {
  static tBTM_SEC_DEV_REC* return_value;
  std::function<tBTM_SEC_DEV_REC*(void)> body{
      [](void) { return return_value; }};
  tBTM_SEC_DEV_REC* operator()(void) { return body(); };
};
extern struct btm_sec_allocate_dev_rec btm_sec_allocate_dev_rec;

// Name: btm_set_bond_type_dev
// Params: const RawAddress& bd_addr, tBTM_SEC_DEV_REC::tBTM_BOND_TYPE bond_type
// Return: bool
struct btm_set_bond_type_dev {
  static bool return_value;
  std::function<bool(const RawAddress& bd_addr,
                     tBTM_SEC_DEV_REC::tBTM_BOND_TYPE bond_type)>
      body{[](const RawAddress& bd_addr,
              tBTM_SEC_DEV_REC::tBTM_BOND_TYPE bond_type) {
        return return_value;
      }};
  bool operator()(const RawAddress& bd_addr,
                  tBTM_SEC_DEV_REC::tBTM_BOND_TYPE bond_type) {
    return body(bd_addr, bond_type);
  };
};
extern struct btm_set_bond_type_dev btm_set_bond_type_dev;

// Name: is_address_equal
// Params: void* data, void* context
// Return: bool
struct is_address_equal {
  static bool return_value;
  std::function<bool(void* data, void* context)> body{
      [](void* data, void* context) { return return_value; }};
  bool operator()(void* data, void* context) { return body(data, context); };
};
extern struct is_address_equal is_address_equal;

// Name: is_handle_equal
// Params: void* data, void* context
// Return: bool
struct is_handle_equal {
  static bool return_value;
  std::function<bool(void* data, void* context)> body{
      [](void* data, void* context) { return return_value; }};
  bool operator()(void* data, void* context) { return body(data, context); };
};
extern struct is_handle_equal is_handle_equal;

// Name: wipe_secrets_and_remove
// Params: tBTM_SEC_DEV_REC* p_dev_rec
// Return: void
struct wipe_secrets_and_remove {
  std::function<void(tBTM_SEC_DEV_REC* p_dev_rec)> body{
      [](tBTM_SEC_DEV_REC* p_dev_rec) {}};
  void operator()(tBTM_SEC_DEV_REC* p_dev_rec) { body(p_dev_rec); };
};
extern struct wipe_secrets_and_remove wipe_secrets_and_remove;

}  // namespace stack_btm_dev
}  // namespace mock
}  // namespace test

// END mockcify generation