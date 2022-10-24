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

#include <functional>

#include "common/init_flags.h"

namespace test {
namespace mock {
namespace init_flags {

struct load {
  void operator()(::rust::Vec<::rust::String>) {}
};
extern struct load load;

struct set_all_for_testing {
  void operator()() {}
};
extern struct set_all_for_testing set_all_for_testing;

struct btaa_hci_is_enabled {
  bool return_value;
  bool operator()() { return return_value; }
};
extern struct btaa_hci_is_enabled btaa_hci_is_enabled;

struct gatt_robust_caching_client_is_enabled {
  bool return_value;
  bool operator()() { return return_value; }
};
extern struct gatt_robust_caching_client_is_enabled
    gatt_robust_caching_client_is_enabled;

struct gatt_robust_caching_server_is_enabled {
  bool return_value;
  bool operator()() { return return_value; }
};
extern struct gatt_robust_caching_server_is_enabled
    gatt_robust_caching_server_is_enabled;

struct gd_core_is_enabled {
  bool return_value;
  bool operator()() { return return_value; }
};
extern struct gd_core_is_enabled gd_core_is_enabled;

struct gd_l2cap_is_enabled {
  bool return_value;
  bool operator()() { return return_value; }
};
extern struct gd_l2cap_is_enabled gd_l2cap_is_enabled;

struct gd_link_policy_is_enabled {
  bool return_value;
  bool operator()() { return return_value; }
};
extern struct gd_link_policy_is_enabled gd_link_policy_is_enabled;

struct gd_remote_name_request_is_enabled {
  bool return_value;
  bool operator()() { return return_value; }
};
extern struct gd_remote_name_request_is_enabled
    gd_remote_name_request_is_enabled;

struct gd_rust_is_enabled {
  bool return_value;
  bool operator()() { return return_value; }
};
extern struct gd_rust_is_enabled gd_rust_is_enabled;

struct gd_security_is_enabled {
  bool return_value;
  bool operator()() { return return_value; }
};
extern struct gd_security_is_enabled gd_security_is_enabled;

struct get_hci_adapter {
  ::std::int32_t return_value;
  ::std::int32_t operator()() { return return_value; }
};
extern struct get_hci_adapter get_hci_adapter;

struct hide_address_in_log_is_enabled {
  bool return_value;
  bool operator()() { return return_value; }
};
extern struct hide_address_in_log_is_enabled hide_address_in_log_is_enabled;

struct irk_rotation_is_enabled {
  bool return_value;
  bool operator()() { return return_value; }
};
extern struct irk_rotation_is_enabled irk_rotation_is_enabled;

struct is_debug_logging_enabled_for_tag {
  bool return_value;
  bool operator()(::rust::Str tag) { return return_value; }
};
extern struct is_debug_logging_enabled_for_tag is_debug_logging_enabled_for_tag;

struct logging_debug_enabled_for_all_is_enabled {
  bool return_value;
  bool operator()() { return return_value; }
};
extern struct logging_debug_enabled_for_all_is_enabled
    logging_debug_enabled_for_all_is_enabled;

struct pass_phy_update_callback_is_enabled {
  bool return_value;
  bool operator()() { return return_value; }
};
extern struct pass_phy_update_callback_is_enabled
    pass_phy_update_callback_is_enabled;

struct sdp_serialization_is_enabled {
  bool return_value;
  bool operator()() { return return_value; }
};
extern struct sdp_serialization_is_enabled sdp_serialization_is_enabled;

}  // namespace init_flags
}  // namespace mock
}  // namespace test
