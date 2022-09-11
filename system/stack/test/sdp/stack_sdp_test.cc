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
#include <frameworks/proto_logging/stats/enums/bluetooth/enums.pb.h>
#include <gmock/gmock.h>
#include <gtest/gtest.h>
#include <stdlib.h>

#include <cstddef>

#include "bt_types.h"
#include "device/include/interop.h"
#include "mock_btif_config.h"
#include "osi/include/alarm.h"
#include "stack/include/avrc_defs.h"
#include "stack/include/sdp_api.h"
#include "stack/sdp/sdpint.h"

#ifndef BT_DEFAULT_BUFFER_SIZE
#define BT_DEFAULT_BUFFER_SIZE (4096 + 16)
#endif

using testing::_;
using testing::DoAll;
using testing::Return;
using testing::SetArrayArgument;

alarm_t* alarm_new(const char* name) { return nullptr; }
void alarm_cancel(alarm_t*) {}
void alarm_set_on_mloop(alarm_t* alarm, uint64_t interval_ms,
                        alarm_callback_t cb, void* data) {}

void* osi_malloc(size_t size) { return malloc(size); }

void osi_free(void* ptr) { free(ptr); }

void osi_free_and_reset(void** ptr) {
  free(*ptr);
  *ptr = nullptr;
}

uint16_t L2CA_Register2(uint16_t psm, const tL2CAP_APPL_INFO& p_cb_info,
                        bool enable_snoop, tL2CAP_ERTM_INFO* p_ertm_info,
                        uint16_t my_mtu, uint16_t required_remote_mtu,
                        uint16_t sec_level) {
  return 42;
}

static int L2CA_ConnectReq2_cid = 0x42;
uint16_t L2CA_ConnectReq2(uint16_t psm, const RawAddress& p_bd_addr,
                          uint16_t sec_level) {
  return ++L2CA_ConnectReq2_cid;
}
uint8_t L2CA_DataWrite(uint16_t cid, BT_HDR* p_data) {
  osi_free_and_reset((void**)&p_data);
  return 0;
}

bool L2CA_DisconnectReq(uint16_t cid) { return true; }
bool btif_config_set_int(const std::string& section, const std::string& key,
                         int value) {
  return true;
}
void log_sdp_attribute(const RawAddress& address, uint16_t protocol_uuid,
                       uint16_t attribute_id, size_t attribute_size,
                       const char* attribute_value) {}
void log_manufacturer_info(const RawAddress& address,
                           android::bluetooth::DeviceInfoSrcEnum source_type,
                           const std::string& source_name,
                           const std::string& manufacturer,
                           const std::string& model,
                           const std::string& hardware_version,
                           const std::string& software_version) {}
void log_manufacturer_info(const RawAddress& address,
                           android::bluetooth::AddressTypeEnum address_type,
                           android::bluetooth::DeviceInfoSrcEnum source_type,
                           const std::string& source_name,
                           const std::string& manufacturer,
                           const std::string& model,
                           const std::string& hardware_version,
                           const std::string& software_version) {}

class StackSdpMainTest : public ::testing::Test {
 protected:
  void SetUp() override {
    bluetooth::manager::SetMockBtifConfigInterface(&btif_config_interface_);
    sdp_init();
    // localIopMock = std::make_unique<IopMock>();
  }

  void TearDown() override {
    bluetooth::manager::SetMockBtifConfigInterface(nullptr);
    // localIopMock.reset();
  }
  bluetooth::manager::MockBtifConfigInterface btif_config_interface_;
};

tCONN_CB* find_ccb(uint16_t cid, uint8_t state) {
  uint16_t xx;
  tCONN_CB* p_ccb;

  /* Look through each connection control block */
  for (xx = 0, p_ccb = sdp_cb.ccb; xx < SDP_MAX_CONNECTIONS; xx++, p_ccb++) {
    if ((p_ccb->con_state == state) && (p_ccb->connection_id == cid)) {
      return (p_ccb);
    }
  }

  /* If here, not found */
  return (NULL);
}

TEST_F(StackSdpMainTest, sdp_service_search_request) {
  RawAddress addr = RawAddress({0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6});

  tSDP_DISCOVERY_DB* db =
      (tSDP_DISCOVERY_DB*)osi_malloc(BT_DEFAULT_BUFFER_SIZE);
  ASSERT_TRUE(SDP_ServiceSearchRequest(addr, db, nullptr));
  int cid = L2CA_ConnectReq2_cid;
  tCONN_CB* p_ccb = sdpu_find_ccb_by_cid(cid);
  ASSERT_NE(p_ccb, nullptr);
  ASSERT_EQ(p_ccb->con_state, SDP_STATE_CONN_SETUP);

  tL2CAP_CFG_INFO cfg;
  sdp_cb.reg_info.pL2CA_ConfigCfm_Cb(p_ccb->connection_id, 0, &cfg);

  ASSERT_EQ(p_ccb->con_state, SDP_STATE_CONNECTED);

  sdp_disconnect(p_ccb, SDP_SUCCESS);
  sdp_cb.reg_info.pL2CA_DisconnectCfm_Cb(p_ccb->connection_id, 0);

  ASSERT_EQ(p_ccb->con_state, SDP_STATE_IDLE);
}

TEST_F(StackSdpMainTest, sdp_service_search_request_queuing) {
  RawAddress addr = RawAddress({0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6});

  tSDP_DISCOVERY_DB* db =
      (tSDP_DISCOVERY_DB*)osi_malloc(BT_DEFAULT_BUFFER_SIZE);
  ASSERT_TRUE(SDP_ServiceSearchRequest(addr, db, nullptr));
  int cid = L2CA_ConnectReq2_cid;
  tCONN_CB* p_ccb1 = find_ccb(cid, SDP_STATE_CONN_SETUP);
  ASSERT_NE(p_ccb1, nullptr);
  ASSERT_EQ(p_ccb1->con_state, SDP_STATE_CONN_SETUP);

  ASSERT_TRUE(SDP_ServiceSearchRequest(addr, db, nullptr));
  tCONN_CB* p_ccb2 = find_ccb(cid, SDP_STATE_CONN_PEND);
  ASSERT_NE(p_ccb2, nullptr);
  ASSERT_NE(p_ccb2, p_ccb1);
  ASSERT_EQ(p_ccb2->con_state, SDP_STATE_CONN_PEND);

  tL2CAP_CFG_INFO cfg;
  sdp_cb.reg_info.pL2CA_ConfigCfm_Cb(p_ccb1->connection_id, 0, &cfg);

  ASSERT_EQ(p_ccb1->con_state, SDP_STATE_CONNECTED);
  ASSERT_EQ(p_ccb2->con_state, SDP_STATE_CONN_PEND);

  p_ccb1->disconnect_reason = SDP_SUCCESS;
  sdp_disconnect(p_ccb1, SDP_SUCCESS);

  ASSERT_EQ(p_ccb1->con_state, SDP_STATE_IDLE);
  ASSERT_EQ(p_ccb2->con_state, SDP_STATE_CONNECTED);

  sdp_disconnect(p_ccb2, SDP_SUCCESS);
  sdp_cb.reg_info.pL2CA_DisconnectCfm_Cb(p_ccb2->connection_id, 0);

  ASSERT_EQ(p_ccb1->con_state, SDP_STATE_IDLE);
  ASSERT_EQ(p_ccb2->con_state, SDP_STATE_IDLE);
}
