/*
 *
 *  Copyright 2022 The Android Open Source Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include "common/init_flags.h"
#include "stack/btm/btm_dev.h"
#include "stack/btm/btm_int_types.h"
#include "stack/btm/btm_sec.h"
#include "test/common/mock_functions.h"
#include "types/hci_role.h"
#include "types/raw_address.h"

extern tBTM_CB btm_cb;

void btm_init(void);
void btm_free(void);
tACL_CONN* acl_get_connection_from_address(const RawAddress& bd_addr,
                                           tBT_TRANSPORT transport);

namespace {
const char* test_flags[] = {
    "INIT_logging_debug_enabled_for_all=true",
    nullptr,
};

}  //  namespace

class StackBtmSecTest : public testing::Test {
 public:
 protected:
  void SetUp() override {
    mock_function_count_map.clear();
    bluetooth::common::InitFlags::Load(test_flags);
    btm_init();
  }
  void TearDown() override { btm_free(); }
};

TEST_F(StackBtmSecTest, btm_sec_connected_dedicated_bonding) {
  auto bd_addr = RawAddress({0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6});

  // Create a new device indicating we know the link key
  tBTM_SEC_DEV_REC* p_dev_rec = btm_sec_allocate_dev_rec();
  ASSERT_TRUE(p_dev_rec != nullptr);
  p_dev_rec->bd_addr = bd_addr;
  p_dev_rec->hci_handle = 0;
  p_dev_rec->sec_flags |= BTM_SEC_LINK_KEY_KNOWN;

  btm_cb.pairing_bda = bd_addr;
  btm_cb.pairing_state = BTM_PAIR_STATE_WAIT_AUTH_COMPLETE;

  btm_sec_connected(bd_addr, 123 /* handle */, HCI_SUCCESS, 0 /* enc_mode */,
                    HCI_ROLE_CENTRAL);

  ASSERT_EQ(123, p_dev_rec->hci_handle);
  ASSERT_EQ(BTM_PAIR_STATE_IDLE, btm_cb.pairing_state);
  ASSERT_EQ(nullptr,
            acl_get_connection_from_address(bd_addr, BT_TRANSPORT_BR_EDR));
}
