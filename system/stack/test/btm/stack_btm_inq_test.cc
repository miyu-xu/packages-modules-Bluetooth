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
#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include <future>
#include <memory>

#include "common/contextual_callback.h"
#include "hci/address.h"
#include "hci/class_of_device.h"
#include "hci/hci_layer_fake.h"
#include "hci/hci_packets.h"
#include "packet/raw_builder.h"
#include "stack/btm/btm_int_types.h"
#include "stack/include/btm_inq.h"
#include "stack/include/hci_error_code.h"
#include "stack/include/inq_hci_link_interface.h"
#include "stack/include/main_thread.h"
#include "stack/test/btm/btm_test_fixtures.h"
#include "test/common/mock_functions.h"
#include "test/mock/mock_main_shim_entry.h"
#include "types/raw_address.h"

extern tBTM_CB btm_cb;

using bluetooth::common::BindOnce;
using bluetooth::common::ContextualCallback;
using bluetooth::common::ContextualOnceCallback;
using bluetooth::hci::Address;
using bluetooth::hci::CommandCompleteView;
using bluetooth::hci::CommandStatusView;
using bluetooth::hci::EventBuilder;
using bluetooth::hci::EventCode;
using bluetooth::hci::EventView;
using bluetooth::hci::ExtendedInquiryResultBuilder;
using bluetooth::hci::ExtendedInquiryResultRawBuilder;
using bluetooth::hci::ExtendedInquiryResultView;
using bluetooth::hci::InquiryResponse;
using bluetooth::hci::InquiryResponseWithRssi;
using bluetooth::hci::InquiryResultBuilder;
using bluetooth::hci::InquiryResultView;
using bluetooth::hci::InquiryResultWithRssiBuilder;
using bluetooth::hci::InquiryResultWithRssiView;
using bluetooth::hci::InquiryStatusBuilder;
using bluetooth::hci::InquiryView;
using bluetooth::hci::OpCode;
using bluetooth::hci::EventCode::EXTENDED_INQUIRY_RESULT;
using bluetooth::hci::EventCode::INQUIRY_COMPLETE;
using bluetooth::hci::EventCode::INQUIRY_RESULT;
using bluetooth::hci::EventCode::INQUIRY_RESULT_WITH_RSSI;
using bluetooth::packet::RawBuilder;
using testing::_;
using testing::A;
using testing::Matcher;
using testing::NiceMock;
using testing::Return;
using testing::SaveArg;

namespace {
const Address kAddress = Address({0x11, 0x22, 0x33, 0x44, 0x55, 0x66});
const Address kAddress2 = Address({0x77, 0x88, 0x99, 0xaa, 0xbb, 0xcc});
const RawAddress kRawAddress = RawAddress({0x11, 0x22, 0x33, 0x44, 0x55, 0x66});
const RawAddress kRawAddress2 =
    RawAddress({0x77, 0x88, 0x99, 0xaa, 0xbb, 0xcc});
const BD_NAME kBdName = {'A', ' ', 'B', 'd', ' ', 'N', 'a', 'm', 'e', '\0'};
const BD_NAME kEmptyName = "";

tBTM_REMOTE_DEV_NAME gBTM_REMOTE_DEV_NAME{};
bool gBTM_REMOTE_DEV_NAME_sent{false};

static constexpr uint8_t kNumCommandPackets = 1;

}  // namespace

class BtmInqTest : public BtmWithMocksTest {
 protected:
  void SetUp() override {
    BtmWithMocksTest::SetUp();
    btm_cb = {};
  }

  void TearDown() override { BtmWithMocksTest::TearDown(); }
};

class BtmInqActiveTest : public BtmInqTest {
 protected:
  void SetUp() override {
    BtmInqTest::SetUp();
    gBTM_REMOTE_DEV_NAME = {};
    gBTM_REMOTE_DEV_NAME_sent = false;

    btm_cb.rnr.remname_active = true;
    btm_cb.rnr.remname_bda = kRawAddress;
    btm_cb.rnr.remname_dev_type = BT_DEVICE_TYPE_BREDR;
    btm_cb.rnr.p_remname_cmpl_cb = [](const tBTM_REMOTE_DEV_NAME* name) {
      gBTM_REMOTE_DEV_NAME = *name;
      gBTM_REMOTE_DEV_NAME_sent = true;
    };
  }

  void TearDown() override { BtmInqTest::TearDown(); }
};

TEST_F(BtmInqActiveTest, btm_process_remote_name__typical) {
  btm_process_remote_name(&kRawAddress, kBdName, 0, HCI_SUCCESS);
  ASSERT_FALSE(btm_cb.rnr.p_remname_cmpl_cb);
  ASSERT_FALSE(btm_cb.rnr.remname_active);
  ASSERT_EQ(btm_cb.rnr.remname_bda, RawAddress::kEmpty);
  ASSERT_EQ(btm_cb.rnr.remname_dev_type, BT_DEVICE_TYPE_UNKNOWN);
  ASSERT_EQ(1, get_func_call_count("alarm_cancel"));

  ASSERT_TRUE(gBTM_REMOTE_DEV_NAME_sent);
  ASSERT_EQ(BTM_SUCCESS, gBTM_REMOTE_DEV_NAME.status);
  ASSERT_EQ(HCI_SUCCESS, gBTM_REMOTE_DEV_NAME.hci_status);
  ASSERT_EQ(kRawAddress, gBTM_REMOTE_DEV_NAME.bd_addr);
  ASSERT_STREQ((char*)kBdName, (char*)gBTM_REMOTE_DEV_NAME.remote_bd_name);
}

TEST_F(BtmInqActiveTest, btm_process_remote_name__no_name) {
  btm_process_remote_name(&kRawAddress, nullptr, 0, HCI_SUCCESS);
  ASSERT_FALSE(btm_cb.rnr.p_remname_cmpl_cb);
  ASSERT_FALSE(btm_cb.rnr.remname_active);
  ASSERT_EQ(btm_cb.rnr.remname_bda, RawAddress::kEmpty);
  ASSERT_EQ(btm_cb.rnr.remname_dev_type, BT_DEVICE_TYPE_UNKNOWN);
  ASSERT_EQ(1, get_func_call_count("alarm_cancel"));

  ASSERT_TRUE(gBTM_REMOTE_DEV_NAME_sent);
  ASSERT_EQ(BTM_SUCCESS, gBTM_REMOTE_DEV_NAME.status);
  ASSERT_EQ(HCI_SUCCESS, gBTM_REMOTE_DEV_NAME.hci_status);
  ASSERT_EQ(kRawAddress, gBTM_REMOTE_DEV_NAME.bd_addr);
  ASSERT_STREQ((char*)kEmptyName, (char*)gBTM_REMOTE_DEV_NAME.remote_bd_name);
}

TEST_F(BtmInqActiveTest, btm_process_remote_name__bad_status) {
  btm_process_remote_name(&kRawAddress, kBdName, 0, HCI_ERR_PAGE_TIMEOUT);
  ASSERT_FALSE(btm_cb.rnr.p_remname_cmpl_cb);
  ASSERT_FALSE(btm_cb.rnr.remname_active);
  ASSERT_EQ(btm_cb.rnr.remname_bda, RawAddress::kEmpty);
  ASSERT_EQ(btm_cb.rnr.remname_dev_type, BT_DEVICE_TYPE_UNKNOWN);
  ASSERT_EQ(1, get_func_call_count("alarm_cancel"));

  ASSERT_TRUE(gBTM_REMOTE_DEV_NAME_sent);
  ASSERT_EQ(BTM_BAD_VALUE_RET, gBTM_REMOTE_DEV_NAME.status);
  ASSERT_EQ(HCI_ERR_PAGE_TIMEOUT, gBTM_REMOTE_DEV_NAME.hci_status);
  ASSERT_EQ(kRawAddress, gBTM_REMOTE_DEV_NAME.bd_addr);
  ASSERT_STREQ((char*)kEmptyName, (char*)gBTM_REMOTE_DEV_NAME.remote_bd_name);
}

TEST_F(BtmInqActiveTest, btm_process_remote_name__no_address) {
  btm_process_remote_name(nullptr, kBdName, 0, HCI_SUCCESS);
  ASSERT_FALSE(btm_cb.rnr.p_remname_cmpl_cb);
  ASSERT_FALSE(btm_cb.rnr.remname_active);
  ASSERT_EQ(btm_cb.rnr.remname_bda, RawAddress::kEmpty);
  ASSERT_EQ(btm_cb.rnr.remname_dev_type, BT_DEVICE_TYPE_UNKNOWN);
  ASSERT_EQ(1, get_func_call_count("alarm_cancel"));

  ASSERT_TRUE(gBTM_REMOTE_DEV_NAME_sent);
  ASSERT_EQ(BTM_SUCCESS, gBTM_REMOTE_DEV_NAME.status);
  ASSERT_EQ(HCI_SUCCESS, gBTM_REMOTE_DEV_NAME.hci_status);
  ASSERT_EQ(RawAddress::kEmpty, gBTM_REMOTE_DEV_NAME.bd_addr);
  ASSERT_STREQ((char*)kBdName, (char*)gBTM_REMOTE_DEV_NAME.remote_bd_name);
}

TEST_F(BtmInqActiveTest, btm_process_remote_name__different_address) {
  btm_cb.rnr.remname_bda = kRawAddress2;
  btm_process_remote_name(&kRawAddress, kBdName, 0, HCI_SUCCESS);
  ASSERT_TRUE(btm_cb.rnr.p_remname_cmpl_cb);
  ASSERT_TRUE(btm_cb.rnr.remname_active);
  ASSERT_NE(btm_cb.rnr.remname_bda, RawAddress::kEmpty);
  ASSERT_NE(btm_cb.rnr.remname_dev_type, BT_DEVICE_TYPE_UNKNOWN);
  ASSERT_EQ(0, get_func_call_count("alarm_cancel"));

  ASSERT_FALSE(gBTM_REMOTE_DEV_NAME_sent);
}

class BtmInquiryCallbacks {
 public:
  virtual ~BtmInquiryCallbacks() = default;
  virtual void btm_inq_results_cb(tBTM_INQ_RESULTS*, const uint8_t*,
                                  uint16_t) = 0;
  virtual void btm_inq_cmpl_cb(void*) = 0;
};

class MockBtmInquiryCallbacks : public BtmInquiryCallbacks {
 public:
  MOCK_METHOD(void, btm_inq_results_cb,
              (tBTM_INQ_RESULTS * p_inq_results, const uint8_t* p_eir,
               uint16_t eir_len),
              (override));
  MOCK_METHOD(void, btm_inq_cmpl_cb, (void*), (override));
};

MockBtmInquiryCallbacks* inquiry_callback_ptr = nullptr;

void btm_inq_results_cb(tBTM_INQ_RESULTS* p_inq_results, const uint8_t* p_eir,
                        uint16_t eir_len) {
  inquiry_callback_ptr->btm_inq_results_cb(p_inq_results, p_eir, eir_len);
}

void btm_inq_cmpl_cb(void* p1) { inquiry_callback_ptr->btm_inq_cmpl_cb(p1); }

class BtmDeviceInquiryTest : public BtmInqTest {
 protected:
  void SetUp() override {
    BtmInqTest::SetUp();
    inquiry_callback_ptr = &callbacks_;
    main_thread_start_up();
    bluetooth::hci::testing::mock_controller_ = &controller_;
    ON_CALL(controller_, SupportsBle()).WillByDefault(Return(true));
    bluetooth::hci::testing::mock_hci_layer_ = &hci_layer_;

    // silence warnings about log history
    btm_cb.history_ = std::make_shared<TimestampedStringCircularBuffer>(10);

    get_main()->Post(BindOnce([]() { btm_cb.btm_inq_vars.Init(); }));

    // Start Inquiry (Synchronized with GetCommand)
    get_main()->Post(BindOnce([]() {
      EXPECT_EQ(BTM_CMD_STARTED, BTM_StartInquiry(btm_inq_results_cb, btm_inq_cmpl_cb));
    }));
    auto view = hci_layer_.GetCommand(OpCode::INQUIRY);
    hci_layer_.IncomingEvent(InquiryStatusBuilder::Create(
        bluetooth::hci::ErrorCode::SUCCESS, kNumCommandPackets));

    // Send one response to synchronize
    std::promise<void> first_result_promise;
    auto first_result = first_result_promise.get_future();
    EXPECT_CALL(*inquiry_callback_ptr, btm_inq_results_cb(_, _, _))
            .WillOnce([&first_result_promise]() { first_result_promise.set_value(); });

    InquiryResponse one_device(kAddress,
                               bluetooth::hci::PageScanRepetitionMode::R0,
                               bluetooth::hci::ClassOfDevice(), 0x1234);
    hci_layer_.IncomingEvent(InquiryResultBuilder::Create({one_device}));

    EXPECT_EQ(std::future_status::ready,
              first_result.wait_for(std::chrono::seconds(1)));
  }

  void TearDown() override {
    BTM_CancelInquiry();
    inquiry_callback_ptr = nullptr;

    // Synchronize with cleaning up.
    std::promise<void> free_promise;
    auto freed = free_promise.get_future();
    get_main()->Post(BindOnce(
            [](std::promise<void>* free_promise) {
              btm_cb.btm_inq_vars.Free();
              free_promise->set_value();
            },
            &free_promise));
    EXPECT_EQ(std::future_status::ready, freed.wait_for(std::chrono::seconds(1)));
    main_thread_shut_down();
    bluetooth::log::info("History");
    for (const auto& entry : btm_cb.history_->Pull()) {
      bluetooth::log::info("{}", entry.entry);
    }

    btm_cb.history_.reset();
    BtmInqTest::TearDown();
  }

  NiceMock<bluetooth::hci::testing::MockControllerInterface> controller_;
  bluetooth::hci::HciLayerFake hci_layer_;
  ContextualCallback<void(EventView)> on_exteneded_inq_result_;
  ContextualCallback<void(EventView)> on_inq_complete_;
  ContextualCallback<void(EventView)> on_inq_result_;
  ContextualCallback<void(EventView)> on_inq_result_with_rssi_;
  MockBtmInquiryCallbacks callbacks_;
};

TEST_F(BtmDeviceInquiryTest, bta_dm_disc_device_discovery_setup_teardown) {}

TEST_F(BtmDeviceInquiryTest, bta_dm_disc_device_discovery_single_result) {
  std::promise<void> one_result_promise;
  auto one_result = one_result_promise.get_future();
  EXPECT_CALL(*inquiry_callback_ptr, btm_inq_results_cb(_, _, _)).WillOnce([&one_result_promise]() {
    one_result_promise.set_value();
  });

  InquiryResponse one_device(kAddress2,
                             bluetooth::hci::PageScanRepetitionMode::R0,
                             bluetooth::hci::ClassOfDevice(), 0x2345);
  hci_layer_.IncomingEvent(InquiryResultBuilder::Create({one_device}));

  EXPECT_EQ(std::future_status::ready,
            one_result.wait_for(std::chrono::seconds(1)));
}

TEST_F(BtmDeviceInquiryTest, bta_dm_disc_device_discovery_single_extended_result) {
  std::promise<void> one_result_promise;
  auto one_result = one_result_promise.get_future();
  EXPECT_CALL(*inquiry_callback_ptr, btm_inq_results_cb(_, _, _)).WillOnce([&one_result_promise]() {
    one_result_promise.set_value();
  });

  uint8_t rssi = 0xe0;
  bluetooth::hci::GapData name{
          bluetooth::hci::GapDataType::COMPLETE_LOCAL_NAME,
          std::vector<uint8_t>({'t', 'e', 's', 't', '_', 'd', 'e', 'v', 'i', 'c', 'e'})};
  bluetooth::log::info("{} {}", name.ToString(), name.size());
  hci_layer_.IncomingEvent(ExtendedInquiryResultBuilder::Create(
          kAddress2, bluetooth::hci::PageScanRepetitionMode::R0, bluetooth::hci::ClassOfDevice(),
          0x2345, rssi, {name}));

  EXPECT_EQ(std::future_status::ready, one_result.wait_for(std::chrono::seconds(1)));
}

TEST_F(BtmDeviceInquiryTest, bta_dm_disc_device_discovery_single_extended_result_raw) {
  std::promise<void> one_result_promise;
  auto one_result = one_result_promise.get_future();
  EXPECT_CALL(*inquiry_callback_ptr, btm_inq_results_cb(_, _, _)).WillOnce([&one_result_promise]() {
    one_result_promise.set_value();
  });

  uint8_t rssi = 0xe0;
  std::array<uint8_t, 240> eir_data{0};

  std::vector<uint8_t> name{'t', 'e', 's', 't', '_', 'd', 'e', 'v', 'i', 'c', 'e'};
  eir_data[0] = 1 + name.size();
  eir_data[1] = static_cast<uint8_t>(bluetooth::hci::GapDataType::COMPLETE_LOCAL_NAME);
  std::copy(name.data(), name.data() + name.size(), eir_data.data() + 2);
  hci_layer_.IncomingEvent(ExtendedInquiryResultRawBuilder::Create(
          kAddress2, bluetooth::hci::PageScanRepetitionMode::R0, bluetooth::hci::ClassOfDevice(),
          0x2345, rssi, eir_data));

  EXPECT_EQ(std::future_status::ready, one_result.wait_for(std::chrono::seconds(1)));
}

TEST_F(BtmDeviceInquiryTest, bta_dm_disc_device_discovery_single_extended_result_wireshark) {
  std::promise<void> one_result_promise;
  auto one_result = one_result_promise.get_future();
  EXPECT_CALL(*inquiry_callback_ptr, btm_inq_results_cb(_, _, _)).WillOnce([&one_result_promise]() {
    one_result_promise.set_value();
  });

  std::vector<uint8_t> raw_extended_inquiry{
          0x01, 0x70, 0x4f, 0x1c, 0x0d, 0xd0, 0x14, 0x01, 0x00, 0x0c, 0x02, 0x7a, 0xc2, 0x09, 0xca,
          0x13, 0x09, 0x54, 0x6f, 0x6d, 0xe2, 0x80, 0x99, 0x73, 0x20, 0x44, 0x65, 0x76, 0x20, 0x69,
          0x50, 0x68, 0x6f, 0x6e, 0x65, 0x0f, 0x03, 0x00, 0x12, 0x1f, 0x11, 0x2f, 0x11, 0x0a, 0x11,
          0x0c, 0x11, 0x32, 0x11, 0x01, 0x18, 0x01, 0x05, 0x31, 0x07, 0xfe, 0xca, 0xca, 0xde, 0xaf,
          0xde, 0xca, 0xde, 0xde, 0xfa, 0xca, 0xde, 0x00, 0x00, 0x00, 0x00, 0x77, 0x0a, 0x6a, 0x10,
          0xa2, 0x22, 0xf2, 0x86, 0x5f, 0x41, 0x19, 0x1d, 0x02, 0x03, 0x03, 0x02, 0x1a, 0x29, 0xea,
          0xab, 0x01, 0x73, 0xbc, 0x88, 0x1c, 0x45, 0x4d, 0xe1, 0x66, 0x24, 0x8d, 0x2d, 0x27, 0xff,
          0x00, 0x4c, 0x02, 0x24, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
  };
  hci_layer_.IncomingEvent(EventBuilder::Create(
          EventCode::EXTENDED_INQUIRY_RESULT, std::make_unique<RawBuilder>(raw_extended_inquiry)));

  EXPECT_EQ(std::future_status::ready, one_result.wait_for(std::chrono::seconds(1)));
}

TEST_F(BtmDeviceInquiryTest, bta_dm_disc_device_discovery_single_result_rssi) {
  std::promise<void> one_result_promise;
  auto one_result = one_result_promise.get_future();
  EXPECT_CALL(*inquiry_callback_ptr, btm_inq_results_cb(_, _, _)).WillOnce([&one_result_promise]() {
    one_result_promise.set_value();
  });

  uint8_t rssi = 0xe0;
  InquiryResponseWithRssi one_device(kAddress2, bluetooth::hci::PageScanRepetitionMode::R0,
                                     bluetooth::hci::ClassOfDevice(), 0x2345, rssi);
  hci_layer_.IncomingEvent(InquiryResultWithRssiBuilder::Create({one_device}));

  EXPECT_EQ(std::future_status::ready, one_result.wait_for(std::chrono::seconds(1)));
}

TEST_F(BtmDeviceInquiryTest, bta_dm_disc_device_discovery_thousand_results) {
  EXPECT_CALL(*inquiry_callback_ptr, btm_inq_results_cb(_, _, _)).Times(1024);

  uint8_t address_bytes[Address::kLength];
  std::copy(kAddress2.data(), kAddress2.data() + Address::kLength, address_bytes);

  for (size_t i = 0; i < 10; i++) {
    Address address;
    address.FromOctets(address_bytes);
    address_bytes[0]++;
    if (i % 256 == 0) {
      address_bytes[1]++;
    }

    InquiryResponse one_device(address, bluetooth::hci::PageScanRepetitionMode::R0,
                               bluetooth::hci::ClassOfDevice(), 0x2345);
    hci_layer_.IncomingEvent(InquiryResultBuilder::Create({one_device}));
    std::this_thread::sleep_for(std::chrono::nanoseconds(1000000));
  }
}
