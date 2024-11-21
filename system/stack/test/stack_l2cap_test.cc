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

#include <bluetooth/log.h>
#include <fcntl.h>
#include <gmock/gmock.h>
#include <gtest/gtest.h>
#include <sys/socket.h>

#include "hci/controller_interface_mock.h"
#include "osi/include/allocator.h"
#include "stack/btm/btm_int_types.h"
#include "stack/include/bt_psm_types.h"
#include "stack/include/l2cap_controller_interface.h"
#include "stack/include/l2cap_hci_link_interface.h"
#include "stack/include/l2cap_module.h"
#include "stack/include/l2cdefs.h"
#include "stack/l2cap/l2c_int.h"
#include "test/mock/mock_main_shim_entry.h"

tBTM_CB btm_cb;
extern tL2C_CB l2cb;

void l2c_link_send_to_lower_br_edr(tL2C_LCB* p_lcb, BT_HDR* p_buf);
void l2c_link_send_to_lower_ble(tL2C_LCB* p_lcb, BT_HDR* p_buf);

using testing::Return;

namespace {
constexpr uint16_t kAclBufferCountClassic = 123;
constexpr uint16_t kAclBufferCountBle = 45;
constexpr uint16_t kAclBufferSizeBle = 45;

}  // namespace

class StackL2capTest : public ::testing::Test {
protected:
  void SetUp() override {
    bluetooth::hci::testing::mock_controller_ = &controller_interface_;
    ON_CALL(controller_interface_, GetNumAclPacketBuffers)
            .WillByDefault(Return(kAclBufferCountClassic));
    bluetooth::hci::LeBufferSize le_sizes;
    le_sizes.total_num_le_packets_ = kAclBufferCountBle;
    le_sizes.le_data_packet_length_ = kAclBufferSizeBle;
    ON_CALL(controller_interface_, GetLeBufferSize).WillByDefault(Return(le_sizes));
    ON_CALL(controller_interface_, SupportsBle).WillByDefault(Return(true));
    l2c_init();
  }

  void TearDown() override {
    l2c_free();
    bluetooth::hci::testing::mock_controller_ = nullptr;
  }

  bluetooth::hci::testing::MockControllerInterface controller_interface_;
};

TEST_F(StackL2capTest, l2cble_process_data_length_change_event) {
  l2cb.lcb_pool[0].tx_data_len = 0xdead;

  // ACL unknown and legal inputs
  l2cble_process_data_length_change_event(0x1234, 0x001b, 0x001b);
  ASSERT_EQ(0xdead, l2cb.lcb_pool[0].tx_data_len);

  l2cb.lcb_pool[0].in_use = true;
  l2cu_set_lcb_handle(l2cb.lcb_pool[0], 0x1234);
  ASSERT_EQ(0x1234, l2cb.lcb_pool[0].Handle());

  // ACL known and illegal inputs
  l2cble_process_data_length_change_event(0x1234, 1, 1);
  ASSERT_EQ(0xdead, l2cb.lcb_pool[0].tx_data_len);

  // ACL known and legal inputs
  l2cble_process_data_length_change_event(0x1234, 0x001b, 0x001b);
  ASSERT_EQ(0x001b, l2cb.lcb_pool[0].tx_data_len);
}

class StackL2capChannelTest : public StackL2capTest {
protected:
  void SetUp() override { StackL2capTest::SetUp(); }

  void TearDown() override { StackL2capTest::TearDown(); }

  tL2C_CCB ccb_ = {
          .in_use = true,
          .chnl_state = CST_OPEN,
          .local_conn_cfg = {.result = tL2CAP_CFG_RESULT::L2CAP_CFG_OK,
                             .mtu = 100,
                             .mps = 100,
                             .credits = L2CA_LeCreditDefault(),
                             .number_of_channels = L2CAP_CREDIT_BASED_MAX_CIDS},
          .peer_conn_cfg = {.result = tL2CAP_CFG_RESULT::L2CAP_CFG_OK,
                            .mtu = 100,
                            .mps = 100,
                            .credits = L2CA_LeCreditDefault(),
                            .number_of_channels = L2CAP_CREDIT_BASED_MAX_CIDS},
          .local_cid = 40,
          .remote_cid = 80,
          .remote_config_rsp_result =
                  tL2CAP_CFG_RESULT::L2CAP_CFG_OK,   // The config rsp result from remote
          .local_id = 12,                            // Transaction ID for local trans
          .remote_id = 22,                           // Transaction ID for local
          .ccb_priority = L2CAP_CHNL_PRIORITY_HIGH,  // tL2CAP_CHNL_PRIORITY Channel priority
  };
};

TEST_F(StackL2capChannelTest, l2c_lcc_proc_pdu__FirstSegment) {
  ccb_.is_first_seg = true;

  BT_HDR* p_buf = (BT_HDR*)osi_calloc(sizeof(BT_HDR) + 32);
  p_buf->len = 32;

  l2c_lcc_proc_pdu(&ccb_, p_buf);
}

TEST_F(StackL2capChannelTest, l2c_lcc_proc_pdu__NextSegment) {
  BT_HDR* p_buf = (BT_HDR*)osi_calloc(sizeof(BT_HDR) + 32);
  p_buf->len = 32;

  l2c_lcc_proc_pdu(&ccb_, p_buf);
}

TEST_F(StackL2capChannelTest, l2c_link_init) {
  l2cb.num_lm_acl_bufs = 0;
  l2cb.controller_xmit_window = 0;
  l2c_link_init(kAclBufferCountClassic);

  ASSERT_EQ(kAclBufferCountClassic, l2cb.num_lm_acl_bufs);
  ASSERT_EQ(kAclBufferCountClassic, l2cb.controller_xmit_window);
}

TEST_F(StackL2capTest, l2cap_result_code_text) {
  std::vector<std::pair<tL2CAP_CONN, std::string>> results = {
          std::make_pair(tL2CAP_CONN::L2CAP_CONN_OK, "tL2CAP_CONN::L2CAP_CONN_OK(0x0000)"),
          std::make_pair(tL2CAP_CONN::L2CAP_CONN_PENDING,
                         "tL2CAP_CONN::L2CAP_CONN_PENDING(0x0001)"),
          std::make_pair(tL2CAP_CONN::L2CAP_CONN_NO_PSM, "tL2CAP_CONN::L2CAP_CONN_NO_PSM(0x0002)"),
          std::make_pair(tL2CAP_CONN::L2CAP_CONN_SECURITY_BLOCK,
                         "tL2CAP_CONN::L2CAP_CONN_SECURITY_BLOCK(0x0003)"),
          std::make_pair(tL2CAP_CONN::L2CAP_CONN_NO_RESOURCES,
                         "tL2CAP_CONN::L2CAP_CONN_NO_RESOURCES(0x0004)"),
          std::make_pair(tL2CAP_CONN::L2CAP_CONN_TIMEOUT,
                         "tL2CAP_CONN::L2CAP_CONN_TIMEOUT(0xeeee)"),
          std::make_pair(tL2CAP_CONN::L2CAP_CONN_OTHER_ERROR,
                         "tL2CAP_CONN::L2CAP_CONN_OTHER_ERROR(0xf000)"),
          std::make_pair(tL2CAP_CONN::L2CAP_CONN_ACL_CONNECTION_FAILED,

                         "tL2CAP_CONN::L2CAP_CONN_ACL_CONNECTION_FAILED(0xf001)"),
          std::make_pair(tL2CAP_CONN::L2CAP_CONN_CLIENT_SECURITY_CLEARANCE_FAILED,
                         "tL2CAP_CONN::L2CAP_CONN_CLIENT_SECURITY_CLEARANCE_FAILED(0xf002)"),
          std::make_pair(tL2CAP_CONN::L2CAP_CONN_NO_LINK,
                         "tL2CAP_CONN::L2CAP_CONN_NO_LINK(0xf003)"),
          std::make_pair(tL2CAP_CONN::L2CAP_CONN_CANCEL, "tL2CAP_CONN::L2CAP_CONN_CANCEL(0xf004)"),
          std::make_pair(tL2CAP_CONN::L2CAP_CONN_INSUFFICIENT_AUTHENTICATION,
                         "tL2CAP_CONN::L2CAP_CONN_INSUFFICIENT_AUTHENTICATION(0xff05)"),
          std::make_pair(tL2CAP_CONN::L2CAP_CONN_INSUFFICIENT_AUTHORIZATION,
                         "tL2CAP_CONN::L2CAP_CONN_INSUFFICIENT_AUTHORIZATION(0xff06)"),
          std::make_pair(tL2CAP_CONN::L2CAP_CONN_INSUFFICIENT_ENCRYP_KEY_SIZE,
                         "tL2CAP_CONN::L2CAP_CONN_INSUFFICIENT_ENCRYP_KEY_SIZE(0xff07)"),
          std::make_pair(tL2CAP_CONN::L2CAP_CONN_INSUFFICIENT_ENCRYP,
                         "tL2CAP_CONN::L2CAP_CONN_INSUFFICIENT_ENCRYP(0xff08)"),
          std::make_pair(tL2CAP_CONN::L2CAP_CONN_INVALID_SOURCE_CID,
                         "tL2CAP_CONN::L2CAP_CONN_INVALID_SOURCE_CID(0xff09)"),
          std::make_pair(tL2CAP_CONN::L2CAP_CONN_SOURCE_CID_ALREADY_ALLOCATED,
                         "tL2CAP_CONN::L2CAP_CONN_SOURCE_CID_ALREADY_ALLOCATED(0xff0a)"),
          std::make_pair(tL2CAP_CONN::L2CAP_CONN_UNACCEPTABLE_PARAMETERS,

                         "tL2CAP_CONN::L2CAP_CONN_UNACCEPTABLE_PARAMETERS(0xff0b)"),
          std::make_pair(tL2CAP_CONN::L2CAP_CONN_INVALID_PARAMETERS,
                         "tL2CAP_CONN::L2CAP_CONN_INVALID_PARAMETERS(0xff0c)"),
  };
  for (const auto& result : results) {
    ASSERT_STREQ(result.second.c_str(), l2cap_result_code_text(result.first).c_str());
  }
  std::ostringstream oss;
  oss << "Unknown tL2CAP_CONN(" << std::hex << "0x" << std::numeric_limits<std::uint16_t>::max()
      << ")";
  ASSERT_STREQ(oss.str().c_str(),
               l2cap_result_code_text(
                       static_cast<tL2CAP_CONN>(std::numeric_limits<std::uint16_t>::max()))
                       .c_str());
}

TEST_F(StackL2capTest, L2CA_Dumpsys) {
  int sv[2];
  char buf[32];
  ASSERT_EQ(0, socketpair(AF_UNIX, SOCK_STREAM, 0, sv));
  ASSERT_EQ(0, fcntl(sv[1], F_SETFL, fcntl(sv[1], F_GETFL, 0) | O_NONBLOCK));

  L2CA_Dumpsys(sv[0]);
  while (read(sv[1], buf, sizeof(buf)) != -1) {
  }
}

TEST_F(StackL2capTest, bt_psm_text) {
  std::map<tBT_PSM, std::string> map = {
          {BT_PSM_SDP, "BT_PSM_SDP"},
          {BT_PSM_RFCOMM, "BT_PSM_RFCOMM"},
          {BT_PSM_TCS, "BT_PSM_TCS"},
          {BT_PSM_CTP, "BT_PSM_CTP"},
          {BT_PSM_BNEP, "BT_PSM_BNEP"},
          {BT_PSM_HIDC, "BT_PSM_HIDC"},
          {HID_PSM_CONTROL, "HID_PSM_CONTROL"},
          {BT_PSM_HIDI, "BT_PSM_HIDI"},
          {HID_PSM_INTERRUPT, "HID_PSM_INTERRUPT"},
          {BT_PSM_UPNP, "BT_PSM_UPNP"},
          {BT_PSM_AVCTP, "BT_PSM_AVCTP"},
          {BT_PSM_AVDTP, "BT_PSM_AVDTP"},
          {BT_PSM_AVCTP_BROWSE, "BT_PSM_AVCTP_BROWSE"},
          {BT_PSM_UDI_CP, "BT_PSM_UDI_CP"},
          {BT_PSM_ATT, "BT_PSM_ATT"},
          {BT_PSM_EATT, "BT_PSM_EATT"},
          {BRCM_RESERVED_PSM_START, "BRCM_RESERVED_PSM_START"},
          {BRCM_RESERVED_PSM_END, "BRCM_RESERVED_PSM_END"},
  };

  for (const auto& it : map) {
    bluetooth::log::info("{} {} ", bt_psm_text(it.first), it.second);
  }
}
