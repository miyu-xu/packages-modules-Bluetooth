/*
 * Copyright 2019 The Android Open Source Project
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
#include <stdarg.h>

#include <chrono>
#include <string>

#include "bta/dm/bta_dm_int.h"
#include "bta/sys/bta_sys.h"
#include "common/init_flags.h"
#include "test/common/main_handler.h"
#include "test/mock/mock_osi_alarm.h"
#include "test/mock/mock_osi_allocator.h"
#include "test/mock/mock_stack_gatt_api.h"

using namespace std::chrono_literals;

void BTA_dm_on_hw_on();
void BTA_dm_on_hw_off();

struct alarm_t {
  alarm_t(const char* name){};
  int any_value;
};

class BtaSdpTest : public testing::Test {
 protected:
  void SetUp() override {
    test::mock::osi_allocator::osi_calloc.body = [](size_t size) -> void* {
      return calloc(1, size);
    };
    test::mock::osi_allocator::osi_malloc.body = [](size_t size) -> void* {
      return malloc(size);
    };
    test::mock::osi_allocator::osi_free.body = [](void* ptr) { free(ptr); };
    test::mock::osi_alarm::alarm_new.body = [](const char* name) -> alarm_t* {
      return new alarm_t(name);
    };
    test::mock::osi_alarm::alarm_free.body = [](alarm_t* alarm) {
      delete alarm;
    };

    id = 1;
    test::mock::stack_gatt_api::GATT_Register.body =
        [this](const Uuid& app_uuid128, std::string name,
               tGATT_CBACK* p_cb_info, bool eatt_support) { return id++; };

    main_thread_start_up();
    sync_main_handler();

    BTA_dm_on_hw_on();
  }

  void TearDown() override {
    BTA_dm_on_hw_off();

    sync_main_handler();
    main_thread_shut_down();

    test::mock::stack_gatt_api::GATT_Register = {};

    test::mock::osi_allocator::osi_calloc = {};
    test::mock::osi_allocator::osi_malloc = {};
    test::mock::osi_allocator::osi_free = {};
    test::mock::osi_alarm::alarm_new = {};
    test::mock::osi_alarm::alarm_free = {};
    mock_function_count_map.clear();
  }
  int id{0};
};

namespace {
constexpr bool kBufferNeedsToBeFreed = true;

tBTA_SYS_REG reg;
std::promise<const tBTA_DM_DISC_RESULT> promise;

}  // namespace

TEST_F(BtaSdpTest, NOP) {}

TEST_F(BtaSdpTest, bta_dm_sdp_result_SDP_SUCCESS) {
  bluetooth::common::InitFlags::SetAllForTesting();

  promise = std::promise<const tBTA_DM_DISC_RESULT>();
  auto future = promise.get_future();

  reg = {
      .evt_hdlr = [](BT_HDR_RIGID* p_msg) -> bool {
        const tBTA_DM_DISC_RESULT* result =
            reinterpret_cast<tBTA_DM_DISC_RESULT*>(p_msg);
        promise.set_value(*result);
        return kBufferNeedsToBeFreed;
      },
      .disable = []() {},
  };
  bta_sys_register(BTA_ID_DM_SEARCH, &reg);
  ASSERT_TRUE(bta_sys_is_register(BTA_ID_DM_SEARCH));

  // Search for all services
  bta_dm_search_cb.service_index = (BTA_USER_SERVICE_ID + 1);

  tBTA_DM_MSG msg = {
      .sdp_event =
          {
              .sdp_result = SDP_SUCCESS,
          },
  };
  bta_dm_sdp_result(&msg);

  ASSERT_TRUE(future.wait_for(2s) == std::future_status::ready);
  const tBTA_DM_DISC_RESULT result = future.get();
  ASSERT_EQ(BTA_DM_DISCOVERY_RESULT_EVT, result.hdr.event);
  ASSERT_EQ(0, result.hdr.len);
  ASSERT_EQ(0, result.hdr.offset);
  ASSERT_EQ(0, result.hdr.layer_specific);
  ASSERT_EQ(BTA_SUCCESS, result.result.disc_res.result);

  // NOTE: The flag is never checked after unregistering so we need to stick a
  // dummy function here.
  reg = {
      .evt_hdlr = [](BT_HDR_RIGID* p_msg) -> bool {
        return kBufferNeedsToBeFreed;
      },
      .disable = []() {},
  };
  bta_sys_register(BTA_ID_DM_SEARCH, &reg);
  bta_sys_deregister(BTA_ID_DM_SEARCH);
}

TEST_F(BtaSdpTest, bta_dm_sdp_result_SDP_NO_RECS_MATCH) {
  bluetooth::common::InitFlags::SetAllForTesting();

  promise = std::promise<const tBTA_DM_DISC_RESULT>();
  auto future = promise.get_future();

  reg = {
      .evt_hdlr = [](BT_HDR_RIGID* p_msg) -> bool {
        const tBTA_DM_DISC_RESULT* result =
            reinterpret_cast<tBTA_DM_DISC_RESULT*>(p_msg);
        promise.set_value(*result);
        return kBufferNeedsToBeFreed;
      },
      .disable = []() {},
  };
  bta_sys_register(BTA_ID_DM_SEARCH, &reg);
  ASSERT_TRUE(bta_sys_is_register(BTA_ID_DM_SEARCH));

  // Search for all services
  bta_dm_search_cb.service_index = (BTA_USER_SERVICE_ID + 1);

  tBTA_DM_MSG msg = {
      .sdp_event =
          {
              .sdp_result = SDP_NO_RECS_MATCH,
          },
  };
  bta_dm_sdp_result(&msg);

  ASSERT_TRUE(future.wait_for(2s) == std::future_status::ready);
  const tBTA_DM_DISC_RESULT result = future.get();
  ASSERT_EQ(BTA_DM_DISCOVERY_RESULT_EVT, result.hdr.event);
  ASSERT_EQ(0, result.hdr.len);
  ASSERT_EQ(0, result.hdr.offset);
  ASSERT_EQ(0, result.hdr.layer_specific);
  ASSERT_EQ(BTA_SUCCESS, result.result.disc_res.result);

  // NOTE: The flag is never checked after unregistering so we need to stick a
  // dummy function here.
  reg = {
      .evt_hdlr = [](BT_HDR_RIGID* p_msg) -> bool {
        return kBufferNeedsToBeFreed;
      },
      .disable = []() {},
  };
  bta_sys_register(BTA_ID_DM_SEARCH, &reg);
  bta_sys_deregister(BTA_ID_DM_SEARCH);
}

TEST_F(BtaSdpTest, bta_dm_sdp_result_SDP_DB_FULL) {
  bluetooth::common::InitFlags::SetAllForTesting();
  promise = std::promise<const tBTA_DM_DISC_RESULT>();
  auto future = promise.get_future();

  reg = {
      .evt_hdlr = [](BT_HDR_RIGID* p_msg) -> bool {
        const tBTA_DM_DISC_RESULT* result =
            reinterpret_cast<tBTA_DM_DISC_RESULT*>(p_msg);
        promise.set_value(*result);
        return kBufferNeedsToBeFreed;
      },
      .disable = []() {},
  };
  bta_sys_register(BTA_ID_DM_SEARCH, &reg);
  ASSERT_TRUE(bta_sys_is_register(BTA_ID_DM_SEARCH));

  // Search for all services
  bta_dm_search_cb.service_index = (BTA_USER_SERVICE_ID + 1);

  tBTA_DM_MSG msg = {
      .sdp_event =
          {
              .sdp_result = SDP_DB_FULL,
          },
  };
  bta_dm_sdp_result(&msg);

  ASSERT_TRUE(future.wait_for(2s) == std::future_status::ready);
  const tBTA_DM_DISC_RESULT result = future.get();
  ASSERT_EQ(BTA_DM_DISCOVERY_RESULT_EVT, result.hdr.event);
  ASSERT_EQ(0, result.hdr.len);
  ASSERT_EQ(0, result.hdr.offset);
  ASSERT_EQ(0, result.hdr.layer_specific);
  ASSERT_EQ(BTA_SUCCESS, result.result.disc_res.result);

  // NOTE: The flag is never checked after unregistering so we need to stick a
  // dummy function here.
  reg = {
      .evt_hdlr = [](BT_HDR_RIGID* p_msg) -> bool {
        return kBufferNeedsToBeFreed;
      },
      .disable = []() {},
  };
  bta_sys_register(BTA_ID_DM_SEARCH, &reg);
  bta_sys_deregister(BTA_ID_DM_SEARCH);
}

TEST_F(BtaSdpTest, bta_dm_sdp_result_SDP_NO_DI_RECORD_FOUND) {
  bluetooth::common::InitFlags::SetAllForTesting();

  promise = std::promise<const tBTA_DM_DISC_RESULT>();
  auto future = promise.get_future();

  reg = {
      .evt_hdlr = [](BT_HDR_RIGID* p_msg) -> bool {
        const tBTA_DM_DISC_RESULT* result =
            reinterpret_cast<tBTA_DM_DISC_RESULT*>(p_msg);
        promise.set_value(*result);
        return kBufferNeedsToBeFreed;
      },
      .disable = []() {},
  };
  bta_sys_register(BTA_ID_DM_SEARCH, &reg);
  ASSERT_TRUE(bta_sys_is_register(BTA_ID_DM_SEARCH));

  // Search for all services
  bta_dm_search_cb.service_index = (BTA_USER_SERVICE_ID + 1);

  tBTA_DM_MSG msg = {
      .sdp_event =
          {
              .sdp_result = SDP_NO_DI_RECORD_FOUND,
          },
  };
  bta_dm_sdp_result(&msg);

  ASSERT_TRUE(future.wait_for(2s) == std::future_status::ready);
  const tBTA_DM_DISC_RESULT result = future.get();
  ASSERT_EQ(BTA_DM_DISCOVERY_RESULT_EVT, result.hdr.event);
  ASSERT_EQ(0, result.hdr.len);
  ASSERT_EQ(0, result.hdr.offset);
  ASSERT_EQ(0, result.hdr.layer_specific);
  ASSERT_EQ(BTA_FAILURE, result.result.disc_res.result);

  // NOTE: The flag is never checked after unregistering so we need to stick a
  // dummy function here.
  reg = {
      .evt_hdlr = [](BT_HDR_RIGID* p_msg) -> bool {
        return kBufferNeedsToBeFreed;
      },
      .disable = []() {},
  };
  bta_sys_register(BTA_ID_DM_SEARCH, &reg);
  bta_sys_deregister(BTA_ID_DM_SEARCH);
}
