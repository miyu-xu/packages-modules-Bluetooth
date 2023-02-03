/******************************************************************************
 *
 *  Copyright 2021 The Android Open Source Project
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
 ******************************************************************************/

#include <base/logging.h>
#include <gtest/gtest.h>

#include "bta/hf_client/bta_hf_client_sdp.cc"
#include "bta/include/bta_hf_client_api.h"
#include "btif/src/btif_hf_client.cc"
#include "types/bluetooth/uuid.h"

void sdp_init(void);
void sdp_free(void);

using SdpRecordHandle = uint32_t;

class BtaHfClientAddRecordTest : public ::testing::Test {
 protected:
  void SetUp() override {
    sdp_init();
    handle_ = SDP_CreateRecord();
  }

  void TearDown() override { sdp_free(); }

  SdpRecordHandle handle_;
  uint8_t scn_{0};
};

TEST_F(BtaHfClientAddRecordTest, test_hf_client_add_record) {
  tBTA_HF_CLIENT_FEAT features = BTIF_HF_CLIENT_FEATURES;

  ASSERT_TRUE(bta_hf_client_add_record("Handsfree", scn_, features, handle_));
}
