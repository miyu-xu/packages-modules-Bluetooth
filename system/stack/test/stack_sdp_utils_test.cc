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
#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include "bt_types.h"
#include "device/include/interop.h"
#include "mock_btif_config.h"
#include "stack/include/avrc_defs.h"
#include "stack/include/sdp_api.h"
#include "stack/sdp/sdpint.h"

using testing::_;
using testing::DoAll;
using testing::Return;
using testing::SetArrayArgument;

// Global trace level referred in the code under test
uint8_t appl_trace_level = BT_TRACE_LEVEL_VERBOSE;

extern "C" void LogMsg(uint32_t trace_set_mask, const char* fmt_str, ...) {}

namespace {
// convenience mock
class IopMock {
 public:
  MOCK_METHOD2(InteropMatchAddr,
               bool(const interop_feature_t, const RawAddress*));
};

std::unique_ptr<IopMock> localIopMock;
}  // namespace

bool interop_match_addr(const interop_feature_t feature,
                        const RawAddress* addr) {
  return localIopMock->InteropMatchAddr(feature, addr);
}

uint8_t value[8] = {
    ((DATA_ELE_SEQ_DESC_TYPE << 3) | SIZE_IN_NEXT_BYTE),  // data_element
    6,                                                    // data_len
    ((UUID_DESC_TYPE << 3) | SIZE_TWO_BYTES),             // uuid_element
    0,                                                    // uuid
    0,                                                    // uuid
    ((UINT_DESC_TYPE << 3) | SIZE_TWO_BYTES),             // version_element
    0,                                                    // version
    0                                                     // version
};
tSDP_ATTRIBUTE attr = {
    .len = 0,
    .value_ptr = (uint8_t*)(&value),
    .id = 0,
    .type = 0,
};

void set_sdp_attr(uint32_t len, uint16_t id, uint16_t uuid, uint16_t version) {
  UINT16_TO_BE_FIELD(value + 3, uuid);
  UINT16_TO_BE_FIELD(value + 6, version);
  attr.len = len;
  attr.id = id;
}

uint16_t get_version(tSDP_ATTRIBUTE* p_attr) {
  uint8_t* p_version = p_attr->value_ptr + 6;
  uint16_t version =
      (((uint16_t)(*(p_version))) << 8) + ((uint16_t)(*((p_version) + 1)));
  return version;
}

class StackSdpUtilsTest : public ::testing::Test {
 protected:
  void SetUp() override {
    bluetooth::manager::SetMockBtifConfigInterface(&btif_config_interface_);
    localIopMock = std::make_unique<IopMock>();
  }

  void TearDown() override {
    bluetooth::manager::SetMockBtifConfigInterface(nullptr);
    localIopMock.reset();
  }
  bluetooth::manager::MockBtifConfigInterface btif_config_interface_;
};

TEST_F(StackSdpUtilsTest, sdpu_set_avrc_target_version) {
  set_sdp_attr(8, ATTR_ID_BT_PROFILE_DESC_LIST,
               UUID_SERVCLASS_AV_REMOTE_CONTROL, AVRC_REV_1_5);
  uint8_t* ptr = (uint8_t*)(&value);
  for (int i = 0; i < 8; i++, ptr++)
    std::cout << "ptr " << (int)(*ptr) << std::endl;
  RawAddress bdaddr;

  // device in IOP table
  set_sdp_attr(8, ATTR_ID_BT_PROFILE_DESC_LIST,
               UUID_SERVCLASS_AV_REMOTE_CONTROL, AVRC_REV_1_5);
  EXPECT_CALL(*localIopMock, InteropMatchAddr(INTEROP_AVRCP_1_4_ONLY, &bdaddr))
      .WillOnce(Return(true));
  sdpu_set_avrc_target_version(&attr, &bdaddr);
  EXPECT_EQ(get_version(&attr), AVRC_REV_1_4);

  // wrong len
  set_sdp_attr(5, ATTR_ID_BT_PROFILE_DESC_LIST,
               UUID_SERVCLASS_AV_REMOTE_CONTROL, AVRC_REV_1_5);
  sdpu_set_avrc_target_version(&attr, &bdaddr);
  EXPECT_EQ(get_version(&attr), AVRC_REV_1_5);

  // wrong attribute id
  set_sdp_attr(8, ATTR_ID_SERVICE_CLASS_ID_LIST,
               UUID_SERVCLASS_AV_REMOTE_CONTROL, AVRC_REV_1_5);
  sdpu_set_avrc_target_version(&attr, &bdaddr);
  EXPECT_EQ(get_version(&attr), AVRC_REV_1_5);

  // wrong UUID
  set_sdp_attr(8, ATTR_ID_BT_PROFILE_DESC_LIST, UUID_SERVCLASS_AUDIO_SOURCE,
               AVRC_REV_1_5);
  sdpu_set_avrc_target_version(&attr, &bdaddr);
  EXPECT_EQ(get_version(&attr), AVRC_REV_1_5);

  // device's controller version smaller than phone's target version
  set_sdp_attr(8, ATTR_ID_BT_PROFILE_DESC_LIST,
               UUID_SERVCLASS_AV_REMOTE_CONTROL, AVRC_REV_1_5);
  uint8_t config_0104[2] = {0x04, 0x01};
  EXPECT_CALL(*localIopMock, InteropMatchAddr(INTEROP_AVRCP_1_4_ONLY, &bdaddr))
      .WillOnce(Return(false));
  EXPECT_CALL(btif_config_interface_, GetBin(bdaddr.ToString(), _, _, _))
      .WillOnce(DoAll(SetArrayArgument<2>(config_0104, config_0104 + 2),
                      Return(true)));
  sdpu_set_avrc_target_version(&attr, &bdaddr);
  EXPECT_EQ(get_version(&attr), AVRC_REV_1_4);

  // device's controller version same as phone's target version
  set_sdp_attr(8, ATTR_ID_BT_PROFILE_DESC_LIST,
               UUID_SERVCLASS_AV_REMOTE_CONTROL, AVRC_REV_1_5);
  uint8_t config_0105[2] = {0x05, 0x01};
  EXPECT_CALL(*localIopMock, InteropMatchAddr(INTEROP_AVRCP_1_4_ONLY, &bdaddr))
      .WillOnce(Return(false));
  EXPECT_CALL(btif_config_interface_, GetBin(bdaddr.ToString(), _, _, _))
      .WillOnce(DoAll(SetArrayArgument<2>(config_0105, config_0105 + 2),
                      Return(true)));
  sdpu_set_avrc_target_version(&attr, &bdaddr);
  EXPECT_EQ(get_version(&attr), AVRC_REV_1_5);

  // device's controller version higher than phone's target version
  set_sdp_attr(8, ATTR_ID_BT_PROFILE_DESC_LIST,
               UUID_SERVCLASS_AV_REMOTE_CONTROL, AVRC_REV_1_5);
  uint8_t config_0106[2] = {0x06, 0x01};
  EXPECT_CALL(*localIopMock, InteropMatchAddr(INTEROP_AVRCP_1_4_ONLY, &bdaddr))
      .WillOnce(Return(false));
  EXPECT_CALL(btif_config_interface_, GetBin(bdaddr.ToString(), _, _, _))
      .WillOnce(DoAll(SetArrayArgument<2>(config_0106, config_0106 + 2),
                      Return(true)));
  sdpu_set_avrc_target_version(&attr, &bdaddr);
  EXPECT_EQ(get_version(&attr), AVRC_REV_1_5);

  // cannot read device's controller version from bt_config
  set_sdp_attr(8, ATTR_ID_BT_PROFILE_DESC_LIST,
               UUID_SERVCLASS_AV_REMOTE_CONTROL, AVRC_REV_1_5);
  EXPECT_CALL(*localIopMock, InteropMatchAddr(INTEROP_AVRCP_1_4_ONLY, &bdaddr))
      .WillOnce(Return(false));
  EXPECT_CALL(btif_config_interface_, GetBin(bdaddr.ToString(), _, _, _))
      .WillOnce(Return(false));
  sdpu_set_avrc_target_version(&attr, &bdaddr);
  EXPECT_EQ(get_version(&attr), AVRC_REV_1_5);
}
