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

#include <map>
#include <string>

extern std::map<std::string, int> mock_function_count_map;

// Mock include file to share data between tests and mock
#include "test/mock/mock_init_flags.h"

namespace test {
namespace mock {
namespace init_flags {

struct load load;
struct set_all_for_testing set_all_for_testing;
struct btaa_hci_is_enabled btaa_hci_is_enabled;
struct gatt_robust_caching_client_is_enabled
    gatt_robust_caching_client_is_enabled;
struct gatt_robust_caching_server_is_enabled
    gatt_robust_caching_server_is_enabled;
struct gd_core_is_enabled gd_core_is_enabled;
struct gd_l2cap_is_enabled gd_l2cap_is_enabled;
struct gd_link_policy_is_enabled gd_link_policy_is_enabled;
struct gd_remote_name_request_is_enabled gd_remote_name_request_is_enabled;
struct gd_rust_is_enabled gd_rust_is_enabled;
struct gd_security_is_enabled gd_security_is_enabled;
struct get_hci_adapter get_hci_adapter;
struct hide_address_in_log_is_enabled hide_address_in_log_is_enabled;
struct irk_rotation_is_enabled irk_rotation_is_enabled;
struct is_debug_logging_enabled_for_tag is_debug_logging_enabled_for_tag;
struct logging_debug_enabled_for_all_is_enabled
    logging_debug_enabled_for_all_is_enabled;
struct pass_phy_update_callback_is_enabled pass_phy_update_callback_is_enabled;
struct sdp_serialization_is_enabled sdp_serialization_is_enabled;

}  // namespace init_flags
}  // namespace mock
}  // namespace test

namespace bluetooth {
namespace common {
namespace init_flags {

void load(::rust::Vec<::rust::String> flags) noexcept {
  mock_function_count_map[__func__]++;
  return test::mock::init_flags::load(flags);
}

void set_all_for_testing() noexcept {
  mock_function_count_map[__func__]++;
  return test::mock::init_flags::set_all_for_testing();
}

bool btaa_hci_is_enabled() noexcept {
  mock_function_count_map[__func__]++;
  return test::mock::init_flags::btaa_hci_is_enabled();
}

bool gatt_robust_caching_client_is_enabled() noexcept {
  mock_function_count_map[__func__]++;
  return test::mock::init_flags::gatt_robust_caching_client_is_enabled();
}

bool gatt_robust_caching_server_is_enabled() noexcept {
  mock_function_count_map[__func__]++;
  return test::mock::init_flags::gatt_robust_caching_server_is_enabled();
}

bool gd_core_is_enabled() noexcept {
  mock_function_count_map[__func__]++;
  return test::mock::init_flags::gd_core_is_enabled();
}

bool gd_l2cap_is_enabled() noexcept {
  mock_function_count_map[__func__]++;
  return test::mock::init_flags::gd_l2cap_is_enabled();
}

bool gd_link_policy_is_enabled() noexcept {
  mock_function_count_map[__func__]++;
  return test::mock::init_flags::gd_link_policy_is_enabled();
}

bool gd_remote_name_request_is_enabled() noexcept {
  mock_function_count_map[__func__]++;
  return test::mock::init_flags::gd_remote_name_request_is_enabled();
}

bool gd_rust_is_enabled() noexcept {
  mock_function_count_map[__func__]++;
  return test::mock::init_flags::gd_rust_is_enabled();
}

bool gd_security_is_enabled() noexcept {
  mock_function_count_map[__func__]++;
  return test::mock::init_flags::gd_security_is_enabled();
}

::std::int32_t get_hci_adapter() noexcept {
  mock_function_count_map[__func__]++;
  return test::mock::init_flags::get_hci_adapter();
}

bool hide_address_in_log_is_enabled() noexcept {
  mock_function_count_map[__func__]++;
  return test::mock::init_flags::hide_address_in_log_is_enabled();
}

bool irk_rotation_is_enabled() noexcept {
  mock_function_count_map[__func__]++;
  return test::mock::init_flags::irk_rotation_is_enabled();
}

bool is_debug_logging_enabled_for_tag(::rust::Str tag) noexcept {
  mock_function_count_map[__func__]++;
  return test::mock::init_flags::is_debug_logging_enabled_for_tag(tag);
}

bool logging_debug_enabled_for_all_is_enabled() noexcept {
  mock_function_count_map[__func__]++;
  return test::mock::init_flags::logging_debug_enabled_for_all_is_enabled();
}

bool pass_phy_update_callback_is_enabled() noexcept {
  mock_function_count_map[__func__]++;
  return test::mock::init_flags::pass_phy_update_callback_is_enabled();
}

bool sdp_serialization_is_enabled() noexcept {
  mock_function_count_map[__func__]++;
  return test::mock::init_flags::sdp_serialization_is_enabled();
}

}  // namespace init_flags
}  // namespace common
}  // namespace bluetooth
