/*
 * Copyright 2024 The Android Open Source Project
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

#include <bluetooth/log.h>
#include <gtest/gtest.h>
#include <unistd.h>

#include "stack/include/avct_api.h"
#include "stack/include/bt_psm_types.h"
#include "test/mock/mock_stack_l2cap_interface.h"

using ::testing::_;
using ::testing::Args;
using ::testing::DoAll;
using ::testing::SaveArg;

namespace {
constexpr uint16_t kRemoteCid = 0x123;
const RawAddress kRawAddress = RawAddress({0x11, 0x22, 0x33, 0x44, 0x55, 0x66});
}  // namespace

class StackAvctpTest : public ::testing::Test {
protected:
  void SetUp() override {
    bluetooth::testing::stack::l2cap::set_interface(&mock_stack_l2cap_interface_);
    EXPECT_CALL(mock_stack_l2cap_interface_, L2CA_RegisterWithSecurity(_, _, _, _, _, _, _))
            .WillRepeatedly([this](unsigned short psm, const tL2CAP_APPL_INFO& cb, bool /* c */,
                                   tL2CAP_ERTM_INFO* /*d*/, unsigned short /* e */,
                                   unsigned short /* f */, unsigned short /* g */) {
              this->callback_map_.insert(std::make_tuple(psm, cb));
              return psm;
            });
    AVCT_Register();
    // Make sure we have a callback for both PSMs
    ASSERT_EQ(2U, callback_map_.size());
  }

  void TearDown() override {
    EXPECT_CALL(mock_stack_l2cap_interface_, L2CA_Deregister(BT_PSM_AVCTP));
    EXPECT_CALL(mock_stack_l2cap_interface_, L2CA_Deregister(BT_PSM_AVCTP_BROWSE));
    AVCT_Deregister();
  }

  std::map<uint16_t, tL2CAP_APPL_INFO> callback_map_;
  bluetooth::testing::stack::l2cap::Mock mock_stack_l2cap_interface_;
  int fd_{STDOUT_FILENO};
};

TEST_F(StackAvctpTest, AVCT_Dumpsys) { AVCT_Dumpsys(fd_); }

TEST_F(StackAvctpTest, AVCT_RemoteInitiates) {
  callback_map_[BT_PSM_AVCTP].pL2CA_ConnectInd_Cb(kRawAddress, kRemoteCid, BT_PSM_AVCTP, 0);
}
