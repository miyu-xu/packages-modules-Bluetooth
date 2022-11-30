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

#include "btif/include/core_callbacks.h"
#include "include/hardware/bluetooth.h"
#include "test/common/mock_functions.h"
#include "test/mock/mock_osi_allocator.h"

using namespace std::chrono_literals;

namespace bluetooth {
namespace testing {

void set_interface_to_profiles(bluetooth::core::CoreInterface* interface);
void clear_interface_to_profiles();

bt_bond_state_t get_pairing_cb_state();
void set_pairing_cb_bd_addr(const RawAddress&);
void set_pairing_cb_sdp_attempts(uint8_t);
void set_pairing_cb_state(const bt_bond_state_t&);

void bond_state_changed(bt_status_t status, const RawAddress& bd_addr,
                        bt_bond_state_t state);

}  // namespace testing
}  // namespace bluetooth

bluetooth::core::EventCallbacks eventCallbacks{
    .invoke_adapter_state_changed_cb = [](bt_state_t state) {},
    .invoke_adapter_properties_cb = [](bt_status_t status, int num_properties,
                                       bt_property_t* properties) {},
    .invoke_remote_device_properties_cb =
        [](bt_status_t status, RawAddress bd_addr, int num_properties,
           bt_property_t* properties) {},
    .invoke_device_found_cb = [](int num_properties,
                                 bt_property_t* properties) {},
    .invoke_discovery_state_changed_cb = [](bt_discovery_state_t state) {},
    .invoke_pin_request_cb = [](RawAddress bd_addr, bt_bdname_t bd_name,
                                uint32_t cod, bool min_16_digit) {},
    .invoke_ssp_request_cb = [](RawAddress bd_addr, bt_bdname_t bd_name,
                                uint32_t cod, bt_ssp_variant_t pairing_variant,
                                uint32_t pass_key) {},
    .invoke_oob_data_request_cb = [](tBT_TRANSPORT t, bool valid, Octet16 c,
                                     Octet16 r, RawAddress raw_address,
                                     uint8_t address_type) {},
    .invoke_bond_state_changed_cb = [](bt_status_t status, RawAddress bd_addr,
                                       bt_bond_state_t state,
                                       int fail_reason) {},
    .invoke_address_consolidate_cb = [](RawAddress main_bd_addr,
                                        RawAddress secondary_bd_addr) {},
    .invoke_le_address_associate_cb = [](RawAddress main_bd_addr,
                                         RawAddress secondary_bd_addr) {},
    .invoke_acl_state_changed_cb =
        [](bt_status_t status, RawAddress bd_addr, bt_acl_state_t state,
           int transport_link_type, bt_hci_error_code_t hci_reason,
           bt_conn_direction_t direction) {},
    .invoke_thread_evt_cb = [](bt_cb_thread_evt event) {},
    .invoke_le_test_mode_cb = [](bt_status_t status, uint16_t count) {},
    .invoke_energy_info_cb = [](bt_activity_energy_info energy_info,
                                bt_uid_traffic_t* uid_data) {},
    .invoke_link_quality_report_cb =
        [](uint64_t timestamp, int report_id, int rssi, int snr,
           int retransmission_count, int packets_not_receive_count,
           int negative_acknowledgement_count) {},
};

struct TestConfigInterface : public bluetooth::core::ConfigInterface {
  bool isA2DPOffloadEnabled() override { return false; }
  bool isAndroidTVDevice() override { return false; }
  bool isRestrictedMode() override { return false; }
} configInterface;

struct TestCodecInterface : public bluetooth::core::CodecInterface {
  void initialize() override{};
  void cleanup() override{};

  uint32_t encodePacket(int16_t* input, uint8_t* output) override { return 0; }
  bool decodePacket(const uint8_t* i_buf, int16_t* o_buf,
                    size_t out_len) override {
    return false;
  }

} codecInterface;

bluetooth::core::HACK_ProfileInterface testHACK_ProfileInterface = {};

struct MockCoreInterface : public bluetooth::core::CoreInterface {
  MockCoreInterface(
      bluetooth::core::EventCallbacks* eventCallbacks,
      bluetooth::core::ConfigInterface* configInterface,
      bluetooth::core::CodecInterface* msbcCodec,
      bluetooth::core::HACK_ProfileInterface* profileSpecific_HACK)
      : CoreInterface(eventCallbacks, configInterface, msbcCodec,
                      profileSpecific_HACK) {}

  virtual void onBluetoothEnabled() override{};
  virtual bt_status_t toggleProfile(tBTA_SERVICE_ID service_id,
                                    bool enable) override {
    return BT_STATUS_SUCCESS;
  }
  virtual void removeDeviceFromProfiles(const RawAddress& bd_addr) override {}
  virtual void onLinkDown(const RawAddress& bd_addr) override {}
};

namespace {
const RawAddress kRawAddress({0x11, 0x22, 0x33, 0x44, 0x55, 0x66});
const RawAddress kRawAddress2({0x12, 0x23, 0x34, 0x45, 0x56, 0x67});

}  // namespace

namespace test {
namespace mock {
extern bool bluetooth_shim_is_gd_stack_started_up;
}  // namespace mock
}  // namespace test

class BtifDmWithMockTest : public ::testing::Test {
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
    bluetooth::testing::set_interface_to_profiles(&mock_core_interface_);
  }

  void TearDown() override {
    bluetooth::testing::clear_interface_to_profiles();

    test::mock::osi_allocator::osi_malloc = {};
    test::mock::osi_allocator::osi_calloc = {};
    test::mock::osi_allocator::osi_free = {};
    test::mock::osi_allocator::osi_free_and_reset = {};
  }

  MockCoreInterface mock_core_interface_ =
      MockCoreInterface(&eventCallbacks, &configInterface, &codecInterface,
                        &testHACK_ProfileInterface);
};

class BtifDmTest : public BtifDmWithMockTest {
 protected:
  void SetUp() override {
    BtifDmWithMockTest::SetUp();
    test::mock::bluetooth_shim_is_gd_stack_started_up = true;
  }

  void TearDown() override {
    BtifDmWithMockTest::TearDown();
    test::mock::bluetooth_shim_is_gd_stack_started_up = false;
  }
};

TEST_F(BtifDmTest, lifecycle) {}

TEST_F(BtifDmTest, DISABLED_bond_state_changed) {
  bluetooth::testing::set_pairing_cb_bd_addr(kRawAddress);

  {
    bluetooth::testing::set_pairing_cb_state(BT_BOND_STATE_BONDING);
    bluetooth::testing::set_pairing_cb_sdp_attempts(0);

    bluetooth::testing::bond_state_changed(BT_STATUS_SUCCESS, kRawAddress2,
                                           BT_BOND_STATE_NONE);
    // CHECK that a bond state change does not affect the device currently in
    // the bond process
    ASSERT_EQ(BT_BOND_STATE_BONDING,
              bluetooth::testing::get_pairing_cb_state());
  }

  {
    bluetooth::testing::set_pairing_cb_state(BT_BOND_STATE_BONDED);
    bluetooth::testing::set_pairing_cb_sdp_attempts(1);

    bluetooth::testing::bond_state_changed(BT_STATUS_SUCCESS, kRawAddress2,
                                           BT_BOND_STATE_NONE);

    // CHECK that a bond state change does not affect the device currently in
    // the bond process
    ASSERT_EQ(BT_BOND_STATE_BONDED, bluetooth::testing::get_pairing_cb_state());
  }
}
