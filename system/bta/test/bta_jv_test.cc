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

#include <gtest/gtest.h>

#include <memory>

#include "bta/jv/bta_jv_int.h"
#include "osi/include/allocator.h"
#include "stack/include/sdp_status.h"
#include "test/common/mock_functions.h"
#include "test/fake/fake_osi.h"
#include "test/mock/mock_stack_sdp_legacy_api.h"
#include "types/bluetooth/uuid.h"
#include "types/raw_address.h"

namespace {
const RawAddress kRawAddress = RawAddress({0x11, 0x22, 0x33, 0x44, 0x55, 0x66});
}  // namespace

namespace bluetooth::legacy::testing {

void bta_jv_start_discovery_cback(const RawAddress& bd_addr, tSDP_RESULT result,
                                  const void* user_data);

}  // namespace bluetooth::legacy::testing

class FakeSdp {
 public:
  FakeSdp() {
    test::mock::stack_sdp_legacy::api_ = {
        .service = {
            .SDP_InitDiscoveryDb = [](tSDP_DISCOVERY_DB*, uint32_t, uint16_t,
                                      const bluetooth::Uuid*, uint16_t,
                                      const uint16_t*) -> bool { return true; },
            .SDP_CancelServiceSearch = nullptr,
            .SDP_ServiceSearchRequest = nullptr,
            .SDP_ServiceSearchAttributeRequest = nullptr,
            .SDP_ServiceSearchAttributeRequest2 =
                [](const RawAddress& /* p_bd_addr */,
                   tSDP_DISCOVERY_DB* /* p_db */,
                   tSDP_DISC_CMPL_CB2* /* p_cb2 */, const void* user_data) {
                  if (user_data) osi_free((void*)user_data);
                  return true;
                },
        },
        .db =
            {
                .SDP_FindServiceInDb = nullptr,
                .SDP_FindServiceUUIDInDb = nullptr,
                .SDP_FindServiceInDb_128bit = nullptr,
            },
        .record =
            {
                .SDP_FindAttributeInRec = nullptr,
                .SDP_FindServiceUUIDInRec_128bit = nullptr,
                .SDP_FindProtocolListElemInRec = nullptr,
                .SDP_FindProfileVersionInRec = nullptr,
                .SDP_FindServiceUUIDInRec = nullptr,
            },
        .handle =
            {
                .SDP_CreateRecord = nullptr,
                .SDP_DeleteRecord = nullptr,
                .SDP_AddAttribute = nullptr,
                .SDP_AddSequence = nullptr,
                .SDP_AddUuidSequence = nullptr,
                .SDP_AddProtocolList = nullptr,
                .SDP_AddAdditionProtoLists = nullptr,
                .SDP_AddProfileDescriptorList = nullptr,
                .SDP_AddLanguageBaseAttrIDList = nullptr,
                .SDP_AddServiceClassIdList = nullptr,
            },
        .device_id =
            {
                .SDP_SetLocalDiRecord = nullptr,
                .SDP_DiDiscover = nullptr,
                .SDP_GetNumDiRecords = nullptr,
                .SDP_GetDiRecord = nullptr,
            },
    };
  }

  ~FakeSdp() { test::mock::stack_sdp_legacy::api_ = {}; }
};

class BtaJvMockAndFakeTest : public ::testing::Test {
 protected:
  void SetUp() override {
    reset_mock_function_count_map();
    fake_osi_ = std::make_unique<test::fake::FakeOsi>();
    fake_sdp_ = std::make_unique<FakeSdp>();
  }

  void TearDown() override {}

  std::unique_ptr<test::fake::FakeOsi> fake_osi_;
  std::unique_ptr<FakeSdp> fake_sdp_;
};

class BtaJvTest : public BtaJvMockAndFakeTest {
 protected:
  void SetUp() override { BtaJvMockAndFakeTest::SetUp(); }

  void TearDown() override { BtaJvMockAndFakeTest::TearDown(); }
};

TEST_F(BtaJvTest, bta_jv_start_discovery_cback) {
  tSDP_RESULT result = SDP_SUCCESS;
  uint32_t* user_data = (uint32_t*)osi_malloc(sizeof(uint32_t));
  *user_data = 0x12345678;

  bluetooth::legacy::testing::bta_jv_start_discovery_cback(kRawAddress, result,
                                                           (void*)user_data);
}
TEST_F(BtaJvTest, bta_jv_start_discovery) {
  uint16_t num_uuid = 1;
  bluetooth::Uuid uuid_list[1] = {
      bluetooth::Uuid::GetRandom(),
  };
  uint32_t rfcomm_slot_id = 123;

  bta_jv_start_discovery(kRawAddress, num_uuid, uuid_list, rfcomm_slot_id);
}
