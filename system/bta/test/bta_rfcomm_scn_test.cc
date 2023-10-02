/*
 *
 *  Copyright 2023 The Android Open Source Project
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

#include "bta_rfcomm_scn.h"

#include <gtest/gtest.h>

#include "bta/jv/bta_jv_int.h"      // tBTA_JV_CB
#include "stack/include/rfcdefs.h"  // RFCOMM_MAX_SCN

tBTA_JV_CB bta_jv_cb;

using testing::Test;

class BtaRfcommScnTest : public Test {
 public:
 protected:
  void SetUp() override {
    bta_jv_cb.scn_search_index = 1;
    for (int i = 0; i < RFCOMM_MAX_SCN; i++) {
      bta_jv_cb.scn_tracker[i] = false;
    }
  }

  void TearDown() override {}
};

TEST_F(BtaRfcommScnTest, scn_available_after_available_index) {
  bta_jv_cb.scn_search_index = 5;
  uint8_t occupied_idx[] = {1, 2, 3, 4, 5, 6, 7};
  for (uint8_t idx : occupied_idx) {
    bta_jv_cb.scn_tracker[idx] = true;
  }

  uint8_t scn = BTA_AllocateSCN();
  ASSERT_EQ(scn, 9);  // All indexes up to 7 are occupied; hence index 8 i.e.
                      // scn 9 should return
}

TEST_F(BtaRfcommScnTest, scn_available_before_available_index) {
  bta_jv_cb.scn_search_index = 28;
  uint8_t occupied_idx[] = {26, 27, 28, 29};
  for (uint8_t idx : occupied_idx) {
    bta_jv_cb.scn_tracker[idx] = true;
  }

  uint8_t scn = BTA_AllocateSCN();
  ASSERT_EQ(scn, 2);  // All SCN from available to 30 are occupied; hence cycle
                      // to beginning.
}

TEST_F(BtaRfcommScnTest, can_allocate_all_scns) {
  for (uint8_t scn = 2; scn <= RFCOMM_MAX_SCN; scn++) {
    EXPECT_EQ(BTA_AllocateSCN(), scn);
  }
}

TEST_F(BtaRfcommScnTest, only_last_scn_available) {
  // Fill all relevant SCN except the last
  for (uint8_t scn = 2; scn < RFCOMM_MAX_SCN; scn++) {
    bta_jv_cb.scn_tracker[scn - 1] = true;
  }
  EXPECT_EQ(BTA_AllocateSCN(), RFCOMM_MAX_SCN);
}

TEST_F(BtaRfcommScnTest, no_scn_available) {
  for (int i = 1; i < RFCOMM_MAX_SCN;
       i++) {  // Fill all relevant SCN indexes (1 to 29)
    bta_jv_cb.scn_tracker[i] = true;
  }

  uint8_t scn = BTA_AllocateSCN();
  EXPECT_EQ(scn, 0) << "scn = " << scn << "and not 0";
}
