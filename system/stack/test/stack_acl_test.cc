/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0(the "License");
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
#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include <cstdint>
#include <deque>

#include "common/init_flags.h"
#include "osi/include/log.h"
#include "stack/acl/acl.h"
#include "stack/acl/btm_acl.h"
#include "stack/btm/btm_int_types.h"
#include "stack/btm/security_device_record.h"
#include "stack/include/acl_hci_link_interface.h"
#include "stack/include/hci_error_code.h"
#include "test/common/mock_functions.h"
#include "test/mock/mock_main_shim_acl_api.h"
#include "test/mock/mock_stack_btm_dev.h"
#include "types/ble_address_with_type.h"
#include "types/hci_role.h"
#include "types/raw_address.h"

tBTM_CB btm_cb;

void LogMsg(uint32_t trace_set_mask, const char* fmt_str, ...) {}

namespace {
const char* test_flags[] = {
    "INIT_logging_debug_enabled_for_all=true",
    nullptr,
};

const RawAddress kRawAddress = RawAddress({0x11, 0x22, 0x33, 0x44, 0x55, 0x66});
const tBLE_BD_ADDR kPublicAddress = {BLE_ADDR_PUBLIC, kRawAddress};
const tBLE_BD_ADDR kRandomAddress = {BLE_ADDR_RANDOM, kRawAddress};
const tBLE_BD_ADDR kPublicIdentityAddress = {BLE_ADDR_PUBLIC_ID, kRawAddress};
const tBLE_BD_ADDR kRandomIdentityAddress = {BLE_ADDR_RANDOM_ID, kRawAddress};

const tBLE_BD_ADDR kEmptyIdentityAddress = {BLE_ADDR_PUBLIC_ID,
                                            RawAddress::kEmpty};
}  // namespace

namespace bluetooth {
namespace testing {

std::set<const RawAddress> copy_of_connected_with_both_public_and_random_set();

}  // namespace testing
}  // namespace bluetooth

void BTM_update_version_info(const RawAddress& bd_addr,
                             const remote_version_info& remote_version_info) {}

void btm_sec_role_changed(tHCI_STATUS hci_status, const RawAddress& bd_addr,
                          tHCI_ROLE new_role) {}

class StackAclTest : public testing::Test {
 protected:
  void SetUp() override {
    mock_function_count_map.clear();
    bluetooth::common::InitFlags::Load(test_flags);
  }
  void TearDown() override {}

  tBTM_SEC_DEV_REC device_record_;
};

TEST_F(StackAclTest, nop) {}

TEST_F(StackAclTest, acl_create_le_connection_with_no_record) {
  std::unordered_set<tBLE_BD_ADDR> address_with_type_set;

  test::mock::main_shim_acl_api::ACL_AcceptLeConnectionFrom.body =
      [&address_with_type_set](const tBLE_BD_ADDR& legacy_address_with_type,
                               bool is_direct) -> bool {
    address_with_type_set.insert(legacy_address_with_type);
    return true;
  };
  test::mock::main_shim_acl_api::ACL_IgnoreLeConnectionFrom.body =
      [&address_with_type_set](const tBLE_BD_ADDR& legacy_address_with_type) {
        address_with_type_set.erase(legacy_address_with_type);
      };

  // Public Address Type
  {
    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    ASSERT_EQ(
        1UL,
        bluetooth::testing::copy_of_connected_with_both_public_and_random_set()
            .size());
    ASSERT_EQ(2UL, address_with_type_set.size());
    ASSERT_EQ(1UL, address_with_type_set.count(kPublicAddress));
    ASSERT_EQ(1UL, address_with_type_set.count(kRandomAddress));
    // Remote connected with BLE_ADDR_PUBLIC which is automatically removed
    // from filter accept list by acl manager.  Explicitly remove the
    // BLE_ADDR_RANDOM for this connection attempt.
    acl_ignore_other_public_and_random_direct_connect(kPublicAddress);
    ASSERT_EQ(
        0UL,
        bluetooth::testing::copy_of_connected_with_both_public_and_random_set()
            .size());

    ASSERT_EQ(2, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    ASSERT_EQ(1, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    mock_function_count_map.clear();
    address_with_type_set.clear();
  }

  // Random Address Type
  {
    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    ASSERT_EQ(
        1UL,
        bluetooth::testing::copy_of_connected_with_both_public_and_random_set()
            .size());
    ASSERT_EQ(2UL, address_with_type_set.size());
    ASSERT_EQ(1UL, address_with_type_set.count(kPublicAddress));
    ASSERT_EQ(1UL, address_with_type_set.count(kRandomAddress));
    // Remote connected with BLE_ADDR_RANDOM which is automatically removed
    // from filter accept list by acl manager.  Explicitly remove the
    // BLE_ADDR_PUBLIC for this connection attempt.
    acl_ignore_other_public_and_random_direct_connect(kRandomAddress);
    ASSERT_EQ(
        0UL,
        bluetooth::testing::copy_of_connected_with_both_public_and_random_set()
            .size());

    ASSERT_EQ(2, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    ASSERT_EQ(1, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    mock_function_count_map.clear();
    address_with_type_set.clear();
  }

  // Public Identity Address Type.  This scenario is not possible as
  // the Public Identity Address was not on the accept filter list.
  {
    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    ASSERT_EQ(
        1UL,
        bluetooth::testing::copy_of_connected_with_both_public_and_random_set()
            .size());
    // The Public Identity addresses are not removed from accept list
    acl_ignore_other_public_and_random_direct_connect(kPublicIdentityAddress);
    // Check and clear the public and random set size
    ASSERT_EQ(
        0UL,
        bluetooth::testing::copy_of_connected_with_both_public_and_random_set()
            .size());

    ASSERT_EQ(2, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    ASSERT_EQ(0, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    mock_function_count_map.clear();
    address_with_type_set.clear();
  }

  // Random Identity Address Type.  This scenario is not possible as
  // the Random Identity Address was not on the accept filter list.
  {
    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    ASSERT_EQ(
        1UL,
        bluetooth::testing::copy_of_connected_with_both_public_and_random_set()
            .size());
    // The Random Identity addresses are not removed from accept list
    acl_ignore_other_public_and_random_direct_connect(kRandomIdentityAddress);
    // Check and clear the public and random set size
    ASSERT_EQ(
        0UL,
        bluetooth::testing::copy_of_connected_with_both_public_and_random_set()
            .size());

    ASSERT_EQ(2, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    ASSERT_EQ(0, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    mock_function_count_map.clear();
    address_with_type_set.clear();
  }

  test::mock::main_shim_acl_api::ACL_AcceptLeConnectionFrom = {};
  test::mock::main_shim_acl_api::ACL_IgnoreLeConnectionFrom = {};
}

TEST_F(StackAclTest, acl_create_le_connection_with_record_no_identity_address) {
  device_record_.device_type = BT_DEVICE_TYPE_BLE;
  device_record_.ble.identity_address_with_type = kEmptyIdentityAddress;

  test::mock::stack_btm_dev::btm_find_dev.body =
      [this](const RawAddress& bd_addr) -> tBTM_SEC_DEV_REC* {
    return &this->device_record_;
  };

  tBLE_BD_ADDR address_with_type;
  test::mock::main_shim_acl_api::ACL_AcceptLeConnectionFrom.body =
      [&address_with_type](const tBLE_BD_ADDR& legacy_address_with_type,
                           bool is_direct) -> bool {
    address_with_type = legacy_address_with_type;
    return true;
  };

  // Public Address Type with no known Identity Address
  {
    device_record_.ble.SetAddressType(BLE_ADDR_PUBLIC);
    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    acl_ignore_other_public_and_random_direct_connect(kPublicAddress);
    ASSERT_EQ(1, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    ASSERT_EQ(address_with_type, kPublicAddress);
    // gd acl manager removes connected address from filter accept list
    ASSERT_EQ(0, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    mock_function_count_map.clear();
  }

  // Random Address Type with no known Identity Address
  {
    device_record_.ble.SetAddressType(BLE_ADDR_RANDOM);
    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    acl_ignore_other_public_and_random_direct_connect(kRandomAddress);
    ASSERT_EQ(1, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    ASSERT_EQ(address_with_type, kRandomAddress);
    // gd acl manager removes connected address from filter accept list
    ASSERT_EQ(0, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    mock_function_count_map.clear();
  }

  // Public Identity Address Type
  {
    device_record_.ble.SetAddressType(BLE_ADDR_PUBLIC_ID);

    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    acl_ignore_other_public_and_random_direct_connect(kPublicIdentityAddress);
    ASSERT_EQ(address_with_type, kPublicIdentityAddress);
    ASSERT_EQ(1, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    // gd acl manager removes connected address from filter accept list
    ASSERT_EQ(0, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    mock_function_count_map.clear();
  }

  // Random Identity Address Type
  {
    device_record_.ble.SetAddressType(BLE_ADDR_RANDOM_ID);

    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    acl_ignore_other_public_and_random_direct_connect(kRandomIdentityAddress);
    ASSERT_EQ(address_with_type, kRandomIdentityAddress);
    ASSERT_EQ(1, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    // gd acl manager removes connected address from filter accept list
    ASSERT_EQ(0, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    mock_function_count_map.clear();
  }

  test::mock::main_shim_acl_api::ACL_AcceptLeConnectionFrom = {};
  test::mock::stack_btm_dev::btm_find_dev = {};
}

TEST_F(StackAclTest,
       acl_create_le_connection_with_record_with_public_identity_address) {
  device_record_.device_type = BT_DEVICE_TYPE_BLE;
  device_record_.ble.SetAddressType(BLE_ADDR_RANDOM);
  device_record_.ble.identity_address_with_type = kPublicIdentityAddress;

  test::mock::stack_btm_dev::btm_find_dev.body =
      [this](const RawAddress& bd_addr) -> tBTM_SEC_DEV_REC* {
    return &this->device_record_;
  };

  tBLE_BD_ADDR address_with_type;
  test::mock::main_shim_acl_api::ACL_AcceptLeConnectionFrom.body =
      [&address_with_type](const tBLE_BD_ADDR& legacy_address_with_type,
                           bool is_direct) -> bool {
    address_with_type = legacy_address_with_type;
    return true;
  };

  // Anonymous address type is illegal
  {
    device_record_.ble.identity_address_with_type = {BLE_ADDR_ANONYMOUS,
                                                     RawAddress::kAny};

    ASSERT_FALSE(acl_create_le_connection(kRawAddress));
    ASSERT_EQ(0, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    ASSERT_EQ(0, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    mock_function_count_map.clear();

    device_record_.ble.identity_address_with_type = kPublicIdentityAddress;
  }

  // Public Address Type with Public Identity Address
  {
    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    acl_ignore_other_public_and_random_direct_connect(kPublicAddress);
    ASSERT_EQ(1, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    ASSERT_EQ(address_with_type, kPublicIdentityAddress);
    // gd acl manager removes connected address from filter accept list
    ASSERT_EQ(0, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    mock_function_count_map.clear();
  }

  // Random Address Type with Public Identity Address
  {
    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    acl_ignore_other_public_and_random_direct_connect(kRandomAddress);
    ASSERT_EQ(1, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    ASSERT_EQ(address_with_type, kPublicIdentityAddress);
    // gd acl manager removes connected address from filter accept list
    ASSERT_EQ(0, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    mock_function_count_map.clear();
  }

  // Public Identity Address Type with Public Identity Address
  {
    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    acl_ignore_other_public_and_random_direct_connect(kPublicIdentityAddress);
    ASSERT_EQ(address_with_type, kPublicIdentityAddress);
    ASSERT_EQ(1, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    // gd acl manager removes connected address from filter accept list
    ASSERT_EQ(0, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    mock_function_count_map.clear();
  }

  // Random Identity Address Type with Public Identity Address
  {
    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    acl_ignore_other_public_and_random_direct_connect(kRandomIdentityAddress);
    ASSERT_EQ(address_with_type, kPublicIdentityAddress);
    ASSERT_EQ(1, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    // gd acl manager removes connected address from filter accept list
    ASSERT_EQ(0, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    mock_function_count_map.clear();
  }

  test::mock::main_shim_acl_api::ACL_AcceptLeConnectionFrom = {};
  test::mock::stack_btm_dev::btm_find_dev = {};
}

TEST_F(StackAclTest,
       acl_create_le_connection_with_record_with_random_identity_address) {
  device_record_.device_type = BT_DEVICE_TYPE_BLE;
  device_record_.ble.SetAddressType(BLE_ADDR_RANDOM);
  device_record_.ble.identity_address_with_type = kRandomIdentityAddress;

  test::mock::stack_btm_dev::btm_find_dev.body =
      [this](const RawAddress& bd_addr) -> tBTM_SEC_DEV_REC* {
    return &this->device_record_;
  };

  tBLE_BD_ADDR address_with_type;
  test::mock::main_shim_acl_api::ACL_AcceptLeConnectionFrom.body =
      [&address_with_type](const tBLE_BD_ADDR& legacy_address_with_type,
                           bool is_direct) -> bool {
    address_with_type = legacy_address_with_type;
    return true;
  };

  // Anonymous address type is illegal
  {
    device_record_.ble.identity_address_with_type = {BLE_ADDR_ANONYMOUS,
                                                     RawAddress::kAny};

    ASSERT_FALSE(acl_create_le_connection(kRawAddress));
    ASSERT_EQ(0, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    ASSERT_EQ(0, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    mock_function_count_map.clear();

    device_record_.ble.identity_address_with_type = kRandomIdentityAddress;
  }

  {
    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    acl_ignore_other_public_and_random_direct_connect(kPublicAddress);
    ASSERT_EQ(address_with_type, kRandomIdentityAddress);
    ASSERT_EQ(1, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    // gd acl manager removes connected address from filter accept list
    ASSERT_EQ(0, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    mock_function_count_map.clear();
  }

  // Random Address Type with no known Identity Address
  {
    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    acl_ignore_other_public_and_random_direct_connect(kRandomAddress);
    ASSERT_EQ(address_with_type, kRandomIdentityAddress);
    ASSERT_EQ(1, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    // gd acl manager removes connected address from filter accept list
    ASSERT_EQ(0, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    mock_function_count_map.clear();
  }

  // Public Identity Address Type
  {
    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    acl_ignore_other_public_and_random_direct_connect(kPublicIdentityAddress);
    ASSERT_EQ(address_with_type, kRandomIdentityAddress);
    ASSERT_EQ(1, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    // gd acl manager removes connected address from filter accept list
    ASSERT_EQ(0, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    mock_function_count_map.clear();
  }

  // Random Identity Address Type
  {
    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    acl_ignore_other_public_and_random_direct_connect(kRandomIdentityAddress);
    ASSERT_EQ(address_with_type, kRandomIdentityAddress);
    ASSERT_EQ(1, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    // gd acl manager removes connected address from filter accept list
    ASSERT_EQ(0, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    mock_function_count_map.clear();
  }

  test::mock::main_shim_acl_api::ACL_AcceptLeConnectionFrom = {};
  test::mock::stack_btm_dev::btm_find_dev = {};
}

TEST_F(StackAclTest, acl_cancel_le_connection_with_no_record) {
  std::unordered_set<tBLE_BD_ADDR> address_with_type_set;
  test::mock::main_shim_acl_api::ACL_AcceptLeConnectionFrom.body =
      [&address_with_type_set](const tBLE_BD_ADDR& legacy_address_with_type,
                               bool is_direct) -> bool {
    address_with_type_set.insert(legacy_address_with_type);
    return true;
  };
  test::mock::main_shim_acl_api::ACL_IgnoreLeConnectionFrom.body =
      [&address_with_type_set](const tBLE_BD_ADDR& legacy_address_with_type) {
        address_with_type_set.erase(legacy_address_with_type);
      };

  {
    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    ASSERT_EQ(2UL, address_with_type_set.size());
    acl_cancel_le_connection(kPublicAddress);
    ASSERT_EQ(2, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    ASSERT_EQ(2, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    ASSERT_EQ(0UL, address_with_type_set.size());
    mock_function_count_map.clear();
  }

  {
    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    ASSERT_EQ(2UL, address_with_type_set.size());
    acl_cancel_le_connection(kRandomAddress);
    ASSERT_EQ(2, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    ASSERT_EQ(2, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);
    ASSERT_EQ(0UL, address_with_type_set.size());
    mock_function_count_map.clear();
  }

  test::mock::main_shim_acl_api::ACL_AcceptLeConnectionFrom = {};
  test::mock::main_shim_acl_api::ACL_IgnoreLeConnectionFrom = {};
}

TEST_F(StackAclTest, acl_cancel_le_connection_with_record) {
  device_record_.device_type = BT_DEVICE_TYPE_BLE;
  device_record_.ble.SetAddressType(BLE_ADDR_RANDOM);
  device_record_.ble.identity_address_with_type = kPublicIdentityAddress;

  test::mock::stack_btm_dev::btm_find_dev.body =
      [this](const RawAddress& bd_addr) -> tBTM_SEC_DEV_REC* {
    return &this->device_record_;
  };

  std::unordered_set<tBLE_BD_ADDR> address_with_type_set;

  test::mock::main_shim_acl_api::ACL_AcceptLeConnectionFrom.body =
      [&address_with_type_set](const tBLE_BD_ADDR& legacy_address_with_type,
                               bool is_direct) -> bool {
    address_with_type_set.insert(legacy_address_with_type);
    return true;
  };
  test::mock::main_shim_acl_api::ACL_IgnoreLeConnectionFrom.body =
      [&address_with_type_set](const tBLE_BD_ADDR& legacy_address_with_type) {
        address_with_type_set.erase(legacy_address_with_type);
      };

  // LE device with record and valid identity address.
  {
    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    ASSERT_EQ(1UL, address_with_type_set.size());
    ASSERT_EQ(1UL, address_with_type_set.count(kPublicIdentityAddress));
    acl_cancel_le_connection(kPublicIdentityAddress);
    ASSERT_EQ(0UL, address_with_type_set.size());
    ASSERT_EQ(1, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    ASSERT_EQ(1, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);

    mock_function_count_map.clear();
    address_with_type_set.clear();
  }

  // LE device with record and no identity address
  {
    device_record_.ble.identity_address_with_type = kEmptyIdentityAddress;

    ASSERT_TRUE(acl_create_le_connection(kRawAddress));
    ASSERT_EQ(1UL, address_with_type_set.size());
    ASSERT_EQ(1UL, address_with_type_set.count(kRandomAddress));
    acl_cancel_le_connection(kRandomAddress);
    ASSERT_EQ(1, mock_function_count_map["ACL_AcceptLeConnectionFrom"]);
    ASSERT_EQ(1, mock_function_count_map["ACL_IgnoreLeConnectionFrom"]);

    mock_function_count_map.clear();
    address_with_type_set.clear();
  }

  test::mock::main_shim_acl_api::ACL_AcceptLeConnectionFrom = {};
  test::mock::main_shim_acl_api::ACL_IgnoreLeConnectionFrom = {};
  test::mock::stack_btm_dev::btm_find_dev = {};
}

TEST_F(StackAclTest, acl_process_extended_features) {
  const uint16_t hci_handle = 0x123;
  const tBT_TRANSPORT transport = BT_TRANSPORT_LE;
  const tHCI_ROLE link_role = HCI_ROLE_CENTRAL;

  btm_acl_created(kRawAddress, hci_handle, link_role, transport);
  tACL_CONN* p_acl = btm_acl_for_bda(kRawAddress, transport);
  ASSERT_NE(nullptr, p_acl);

  // Handle typical case
  {
    const uint8_t max_page = 3;
    memset((void*)p_acl->peer_lmp_feature_valid, 0,
           HCI_EXT_FEATURES_PAGE_MAX + 1);
    acl_process_extended_features(hci_handle, 1, max_page, 0xf123456789abcde);
    acl_process_extended_features(hci_handle, 2, max_page, 0xef123456789abcd);
    acl_process_extended_features(hci_handle, 3, max_page, 0xdef123456789abc);

    /* page 0 is the standard feature set */
    ASSERT_FALSE(p_acl->peer_lmp_feature_valid[0]);
    ASSERT_TRUE(p_acl->peer_lmp_feature_valid[1]);
    ASSERT_TRUE(p_acl->peer_lmp_feature_valid[2]);
    ASSERT_TRUE(p_acl->peer_lmp_feature_valid[3]);
  }

  // Handle extreme case
  {
    const uint8_t max_page = 255;
    memset((void*)p_acl->peer_lmp_feature_valid, 0,
           HCI_EXT_FEATURES_PAGE_MAX + 1);
    for (int i = 1; i < HCI_EXT_FEATURES_PAGE_MAX + 1; i++) {
      acl_process_extended_features(hci_handle, static_cast<uint8_t>(i),
                                    max_page, 0x123456789abcdef);
    }
    /* page 0 is the standard feature set */
    ASSERT_FALSE(p_acl->peer_lmp_feature_valid[0]);
    ASSERT_TRUE(p_acl->peer_lmp_feature_valid[1]);
    ASSERT_TRUE(p_acl->peer_lmp_feature_valid[2]);
    ASSERT_TRUE(p_acl->peer_lmp_feature_valid[3]);
  }

  // Handle case where device returns max page of zero
  {
    memset((void*)p_acl->peer_lmp_feature_valid, 0,
           HCI_EXT_FEATURES_PAGE_MAX + 1);
    acl_process_extended_features(hci_handle, 1, 0, 0xdef123456789abc);
    ASSERT_FALSE(p_acl->peer_lmp_feature_valid[0]);
    ASSERT_TRUE(p_acl->peer_lmp_feature_valid[1]);
    ASSERT_FALSE(p_acl->peer_lmp_feature_valid[2]);
    ASSERT_FALSE(p_acl->peer_lmp_feature_valid[3]);
  }

  btm_acl_removed(hci_handle);
}
