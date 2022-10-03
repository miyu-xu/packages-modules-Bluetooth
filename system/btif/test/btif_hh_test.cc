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

#include <gtest/gtest.h>

#include <future>
#include <map>

#include "bta/hh/bta_hh_int.h"
#include "bta/include/bta_ag_api.h"
#include "bta/include/bta_hh_api.h"
#include "btif/include/btif_api.h"
#include "btif/include/stack_manager.h"
#include "include/hardware/bt_hh.h"
#include "test/common/mock_functions.h"
#include "test/mock/mock_osi_allocator.h"

void set_hal_cbacks(bt_callbacks_t* callbacks);

uint8_t appl_trace_level = BT_TRACE_LEVEL_DEBUG;
uint8_t btif_trace_level = BT_TRACE_LEVEL_DEBUG;
uint8_t btu_trace_level = BT_TRACE_LEVEL_DEBUG;

stack_manager_t stack_manager = {
    .init_stack = []() {},
    .start_up_stack_async = []() {},
    .shut_down_stack_async = []() {},
    .clean_up_stack = []() {},
    .get_stack_is_running = []() -> bool { return true; },
};
const stack_manager_t* stack_manager_get_interface() { return &stack_manager; }

future_t* stack_manager_get_hack_future() { return nullptr; }

const tBTA_AG_RES_DATA tBTA_AG_RES_DATA::kEmpty = {};

extern void bte_hh_evt(tBTA_HH_EVT event, tBTA_HH* p_data);
extern const bthh_interface_t* btif_hh_get_interface();

#if __GLIBC__
size_t strlcpy(char* dst, const char* src, size_t siz) {
  char* d = dst;
  const char* s = src;
  size_t n = siz;

  /* Copy as many bytes as will fit */
  if (n != 0) {
    while (--n != 0) {
      if ((*d++ = *s++) == '\0') break;
    }
  }

  /* Not enough room in dst, add NUL and traverse rest of src */
  if (n == 0) {
    if (siz != 0) *d = '\0'; /* NUL-terminate dst */
    while (*s++)
      ;
  }

  return (s - src - 1); /* count does not include NUL */
}

pid_t gettid(void) throw() { return syscall(SYS_gettid); }
#endif

namespace {
[[maybe_unused]] std::array<uint8_t, 32> data32 = {
    0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b,
    0x0c, 0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16,
    0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x20,
};

[[maybe_unused]] std::promise<void> g_promise;
[[maybe_unused]] std::future<void> g_future;

}  // namespace

bt_callbacks_t bt_callbacks = {
    .size = sizeof(bt_callbacks_t),
    .adapter_state_changed_cb = nullptr,  // adapter_state_changed_callback
    .adapter_properties_cb = nullptr,     // adapter_properties_callback
    .remote_device_properties_cb =
        nullptr,                            // remote_device_properties_callback
    .device_found_cb = nullptr,             // device_found_callback
    .discovery_state_changed_cb = nullptr,  // discovery_state_changed_callback
    .pin_request_cb = nullptr,              // pin_request_callback
    .ssp_request_cb = nullptr,              // ssp_request_callback
    .bond_state_changed_cb = nullptr,       // bond_state_changed_callback
    .address_consolidate_cb = nullptr,      // address_consolidate_callback
    .le_address_associate_cb = nullptr,     // le_address_associate_callback
    .acl_state_changed_cb = nullptr,        // acl_state_changed_callback
    .thread_evt_cb = nullptr,               // callback_thread_event
    .dut_mode_recv_cb = nullptr,            // dut_mode_recv_callback
    .le_test_mode_cb = nullptr,             // le_test_mode_callback
    .energy_info_cb = nullptr,              // energy_info_callback
    .link_quality_report_cb = nullptr,      // link_quality_report_callback
    .generate_local_oob_data_cb = nullptr,  // generate_local_oob_data_callback
    .switch_buffer_size_cb = nullptr,       // switch_buffer_size_callback
    .switch_codec_cb = nullptr,             // switch_codec_callback
    .le_rand_cb = nullptr,                  // le_rand_callback
};

bthh_callbacks_t bthh_callbacks = {
    .size = sizeof(bthh_callbacks_t),
    .connection_state_cb = nullptr,  // bthh_connection_state_callback
    .hid_info_cb = nullptr,          // bthh_hid_info_callback
    .protocol_mode_cb = nullptr,     // bthh_protocol_mode_callback
    .idle_time_cb = nullptr,         // bthh_idle_time_callback
    .get_report_cb = nullptr,        // bthh_get_report_callback
    .virtual_unplug_cb = nullptr,    // bthh_virtual_unplug_callback
    .handshake_cb = nullptr,         // bthh_handshake_callback
};

std::atomic<int> allocations = 0;

class BtifHhWithMockTest : public ::testing::Test {
 protected:
  void SetUp() override {
    reset_mock_function_count_map();
    test::mock::osi_allocator::osi_malloc.body = [](size_t size) {
      return malloc(size);
    };
    test::mock::osi_allocator::osi_calloc.body = [](size_t size) {
      return calloc(1UL, size);
    };
    test::mock::osi_allocator::osi_free.body = [](void* ptr) { free(ptr); };
    test::mock::osi_allocator::osi_free_and_reset.body = [](void** ptr) {
      free(*ptr);
      *ptr = nullptr;
    };
    bluetooth::common::InitFlags::SetAllForTesting();
  }

  void TearDown() override {
    test::mock::osi_allocator::osi_malloc = {};
    test::mock::osi_allocator::osi_calloc = {};
    test::mock::osi_allocator::osi_free = {};
    test::mock::osi_allocator::osi_free_and_reset = {};
  }
};

class BtifHhWithHalCallbacksTest : public BtifHhWithMockTest {
 protected:
  void SetUp() override {
    BtifHhWithMockTest::SetUp();
    g_promise = std::promise<void>();
    g_future = g_promise.get_future();
    bt_callbacks.thread_evt_cb = [](bt_cb_thread_evt evt) {
      g_promise.set_value();
      ASSERT_EQ(ASSOCIATE_JVM, evt);
    };
    set_hal_cbacks(&bt_callbacks);
    // Start the jni callback thread
    ASSERT_EQ(BT_STATUS_SUCCESS, btif_init_bluetooth());

    // Wait for event indicating startup of jni callback thread
    g_future.wait();
    bt_callbacks.thread_evt_cb = [](bt_cb_thread_evt evt) {};

    // Wait for adapter to be ready
    g_promise = std::promise<void>();
    g_future = g_promise.get_future();
    bt_callbacks.adapter_properties_cb =
        [](bt_status_t status, int num_properties, bt_property_t* properties) {
          g_promise.set_value();
        };
    btif_hh_get_interface()->init(&bthh_callbacks);
    g_future.wait();
    bt_callbacks.adapter_properties_cb = [](bt_status_t status,
                                            int num_properties,
                                            bt_property_t* properties) {};
  }

  void TearDown() override {
    g_promise = std::promise<void>();
    g_future = g_promise.get_future();
    bt_callbacks.thread_evt_cb = [](bt_cb_thread_evt evt) {
      g_promise.set_value();
      ASSERT_EQ(DISASSOCIATE_JVM, evt);
    };
    // Shutdown the jni callback thread
    ASSERT_EQ(BT_STATUS_SUCCESS, btif_cleanup_bluetooth());
    // Await for event indicating shutdown of jni callback thread
    g_future.wait();

    bt_callbacks.thread_evt_cb = [](bt_cb_thread_evt evt) {};
    bt_callbacks.adapter_properties_cb = [](bt_status_t status,
                                            int num_properties,
                                            bt_property_t* properties) {};
    BtifHhWithMockTest::TearDown();
  }
};

TEST_F(BtifHhWithMockTest, allocate) {
  auto* p = osi_calloc(256);
  osi_free(p);
  ASSERT_EQ(0, allocations);
}

TEST_F(BtifHhWithHalCallbacksTest, lifecycle) {}

TEST_F(BtifHhWithHalCallbacksTest, allocate) {
  ASSERT_EQ(1, allocations.load());

  int current_allocations = allocations.load();
  LOG_INFO("A. current allocations:%d, allocsations:%d\n", current_allocations,
           allocations.load());
  auto* p = osi_calloc(1024);
  int new2 = allocations.load();
  LOG_INFO(
      "B. current allocations:%d, allocsations:%d new:%d &allocations:%p\n",
      current_allocations, allocations.load(), new2, &allocations);
  ASSERT_EQ(current_allocations + 1, allocations.load());
  LOG_INFO("C. current allocations:%d, allocsations:%d\n", current_allocations,
           allocations.load());
  osi_free(p);
  LOG_INFO("D. current allocations:%d, allocsations:%d\n", current_allocations,
           allocations.load());
  ASSERT_EQ(current_allocations, allocations.load());
  LOG_INFO("E. current allocations:%d, allocsations:%d\n", current_allocations,
           allocations.load());
}

TEST_F(BtifHhWithHalCallbacksTest, BTA_HH_GET_RPT_EVT) {
  tBTA_HH data = {
      .hs_data =
          {
              .status = BTA_HH_OK,
              .handle = 123,
              .rsp_data =
                  {
                      .p_rpt_data =
                          static_cast<BT_HDR*>(osi_calloc(32 + sizeof(BT_HDR))),
                  },
          },
  };

  // Fill out the deep copy data
  data.hs_data.rsp_data.p_rpt_data->len = static_cast<uint16_t>(data32.size());
  memcpy((data.hs_data.rsp_data.p_rpt_data + 1), data32.begin(), data32.size());

  bte_hh_evt(BTA_HH_GET_RPT_EVT, &data);

  osi_free(data.hs_data.rsp_data.p_rpt_data);

  // Verify data was delivered
}
