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
 *   Functions generated:14
 *
 *  mockcify.pl ver 0.5.0
 */

#include <cstdint>
#include <functional>
#include <map>
#include <string>

extern std::map<std::string, int> mock_function_count_map;

// Mock include file to share data between tests and mock
#include "test/mock/mock_main_shim_acl_api.h"

// Original usings

// Mocked internal structures, if any

namespace test {
namespace mock {
namespace main_shim_acl_api {

// Function state capture and return values, if needed
struct ACL_AcceptLeConnectionFrom ACL_AcceptLeConnectionFrom;
struct ACL_AddToAddressResolution ACL_AddToAddressResolution;
struct ACL_CancelClassicConnection ACL_CancelClassicConnection;
struct ACL_ClearAddressResolution ACL_ClearAddressResolution;
struct ACL_ClearFilterAcceptList ACL_ClearFilterAcceptList;
struct ACL_ConfigureLePrivacy ACL_ConfigureLePrivacy;
struct ACL_CreateClassicConnection ACL_CreateClassicConnection;
struct ACL_Disconnect ACL_Disconnect;
struct ACL_IgnoreAllLeConnections ACL_IgnoreAllLeConnections;
struct ACL_IgnoreLeConnectionFrom ACL_IgnoreLeConnectionFrom;
struct ACL_ReadConnectionAddress ACL_ReadConnectionAddress;
struct ACL_RemoveFromAddressResolution ACL_RemoveFromAddressResolution;
struct ACL_Shutdown ACL_Shutdown;
struct ACL_WriteData ACL_WriteData;

}  // namespace main_shim_acl_api
}  // namespace mock
}  // namespace test

// Mocked function return values, if any
namespace test {
namespace mock {
namespace main_shim_acl_api {

bool ACL_AcceptLeConnectionFrom::return_value = false;

}  // namespace main_shim_acl_api
}  // namespace mock
}  // namespace test

// Mocked functions, if any
bool bluetooth::shim::ACL_AcceptLeConnectionFrom(
    const tBLE_BD_ADDR& legacy_address_with_type, bool is_direct) {
  mock_function_count_map[__func__]++;
  return test::mock::main_shim_acl_api::ACL_AcceptLeConnectionFrom(
      legacy_address_with_type, is_direct);
}
void bluetooth::shim::ACL_AddToAddressResolution(
    const tBLE_BD_ADDR& legacy_address_with_type, const Octet16& peer_irk,
    const Octet16& local_irk) {
  mock_function_count_map[__func__]++;
  test::mock::main_shim_acl_api::ACL_AddToAddressResolution(
      legacy_address_with_type, peer_irk, local_irk);
}
void bluetooth::shim::ACL_CancelClassicConnection(
    const RawAddress& raw_address) {
  mock_function_count_map[__func__]++;
  test::mock::main_shim_acl_api::ACL_CancelClassicConnection(raw_address);
}
void bluetooth::shim::ACL_ClearAddressResolution() {
  mock_function_count_map[__func__]++;
  test::mock::main_shim_acl_api::ACL_ClearAddressResolution();
}
void bluetooth::shim::ACL_ClearFilterAcceptList() {
  mock_function_count_map[__func__]++;
  test::mock::main_shim_acl_api::ACL_ClearFilterAcceptList();
}
void bluetooth::shim::ACL_ConfigureLePrivacy(bool is_le_privacy_enabled) {
  mock_function_count_map[__func__]++;
  test::mock::main_shim_acl_api::ACL_ConfigureLePrivacy(is_le_privacy_enabled);
}
void bluetooth::shim::ACL_CreateClassicConnection(
    const RawAddress& raw_address) {
  mock_function_count_map[__func__]++;
  test::mock::main_shim_acl_api::ACL_CreateClassicConnection(raw_address);
}
void bluetooth::shim::ACL_Disconnect(uint16_t handle, bool is_classic,
                                     tHCI_STATUS reason, std::string comment) {
  mock_function_count_map[__func__]++;
  test::mock::main_shim_acl_api::ACL_Disconnect(handle, is_classic, reason,
                                                comment);
}
void bluetooth::shim::ACL_IgnoreAllLeConnections() {
  mock_function_count_map[__func__]++;
  test::mock::main_shim_acl_api::ACL_IgnoreAllLeConnections();
}
void bluetooth::shim::ACL_IgnoreLeConnectionFrom(
    const tBLE_BD_ADDR& legacy_address_with_type) {
  mock_function_count_map[__func__]++;
  test::mock::main_shim_acl_api::ACL_IgnoreLeConnectionFrom(
      legacy_address_with_type);
}
void bluetooth::shim::ACL_ReadConnectionAddress(const RawAddress& pseudo_addr,
                                                RawAddress& conn_addr,
                                                tBLE_ADDR_TYPE* p_addr_type) {
  mock_function_count_map[__func__]++;
  test::mock::main_shim_acl_api::ACL_ReadConnectionAddress(
      pseudo_addr, conn_addr, p_addr_type);
}
void bluetooth::shim::ACL_RemoveFromAddressResolution(
    const tBLE_BD_ADDR& legacy_address_with_type) {
  mock_function_count_map[__func__]++;
  test::mock::main_shim_acl_api::ACL_RemoveFromAddressResolution(
      legacy_address_with_type);
}
void bluetooth::shim::ACL_Shutdown() {
  mock_function_count_map[__func__]++;
  test::mock::main_shim_acl_api::ACL_Shutdown();
}
void bluetooth::shim::ACL_WriteData(uint16_t handle, BT_HDR* p_buf) {
  mock_function_count_map[__func__]++;
  test::mock::main_shim_acl_api::ACL_WriteData(handle, p_buf);
}
// Mocked functions complete
// END mockcify generation
