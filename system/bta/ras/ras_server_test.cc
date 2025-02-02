/*
 * Copyright 2025 The Android Open Source Project
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

#include "bta/include/bta_ras_api.h"
#include "bta/ras/ras_types.h"
#include "bta/test/common/bta_gatt_api_mock.h"
#include "include/hardware/bluetooth.h"
#include "internal_include/stack_config.h"
#include "log/include/bluetooth/log.h"
#include "stack/include/main_thread.h"
#include "test/mock/mock_main_shim_entry.h"

using testing::_;
using testing::AnyNumber;
using testing::AtLeast;
using testing::AtMost;
using testing::DoAll;
using testing::Expectation;
using testing::InSequence;
using testing::Invoke;
using testing::Matcher;
using testing::Mock;
using testing::MockFunction;
using testing::NiceMock;
using testing::NotNull;
using testing::Return;
using testing::SaveArg;
using testing::SetArgPointee;
using testing::Test;
using testing::WithArg;

using namespace bluetooth::ras;
using namespace bluetooth;

bt_status_t do_in_main_thread(base::OnceClosure task) {
  if (task.is_null()) {
    bluetooth::log::error("Task is null!");
    return BT_STATUS_FAIL;
  }
  std::move(task).Run();
  return BT_STATUS_SUCCESS;
}

namespace bluetooth::ras {

class MockRasServerCallbacks : public RasServerCallbacks {
public:
  MOCK_METHOD(void, OnVendorSpecificReply,
              (const RawAddress& address,
               const std::vector<VendorSpecificCharacteristic>& vendor_specific_reply),
              (override));
  MOCK_METHOD(void, OnRasServerConnected, (const RawAddress& identity_address), (override));
  MOCK_METHOD(void, OnMtuChangedFromServer, (const RawAddress& address, uint16_t mtu), (override));
  MOCK_METHOD(void, OnRasServerDisconnected, (const RawAddress& identity_address), (override));
};

class RasServerTestNoInit : public ::testing::Test {
protected:
  void SetUp() override {
    gatt::SetMockBtaGattServerInterface(&mock_gatt_server_interface_);
    RawAddress::FromString("11:22:33:44:55:66", test_address_);
  }
  RawAddress test_address_;
  tBTA_GATTS_CBACK* captured_gatt_callback_ = nullptr;
  gatt::MockBtaGattServerInterface mock_gatt_server_interface_;
  MockRasServerCallbacks mock_ras_server_callbacks_;
};

class RasServerTest : public RasServerTestNoInit {
protected:
  void SetUp() override {
    RasServerTestNoInit::SetUp();
    EXPECT_CALL(mock_gatt_server_interface_, AppRegister(_, _, _))
            .WillOnce(testing::SaveArg<1>(&captured_gatt_callback_));
    GetRasServer()->Initialize();
    ASSERT_NE(captured_gatt_callback_, nullptr);
    GetRasServer()->RegisterCallbacks(&mock_ras_server_callbacks_);
  }
};

TEST_F(RasServerTestNoInit, InitializationSuccessful) {
  // AppRegister should be triggered when Initialize
  EXPECT_CALL(mock_gatt_server_interface_, AppRegister(_, _, _))
          .WillOnce(testing::SaveArg<1>(&captured_gatt_callback_));
  GetRasServer()->Initialize();
  ASSERT_NE(captured_gatt_callback_, nullptr);

  // AddService should be triggered after receiving BTA_GATTS_REG_EVT
  tGATT_IF captured_server_if;
  std::vector<btgatt_db_element_t> captured_service;
  BTA_GATTS_AddServiceCb captured_cb;
  EXPECT_CALL(mock_gatt_server_interface_, AddService(_, _, _))
          .WillOnce(testing::DoAll(
                  testing::SaveArg<0>(&captured_server_if), testing::SaveArg<1>(&captured_service),
                  testing::SaveArg<2>(&captured_cb),
                  testing::Return()));  // You might need to return something appropriate

  // Mock BTA_GATTS_REG_EVT
  tBTA_GATTS gatts_cb_data;
  gatts_cb_data.reg_oper.status = GATT_SUCCESS;
  captured_gatt_callback_(BTA_GATTS_REG_EVT, &gatts_cb_data);

  // Run BTA_GATTS_AddServiceCb
  captured_cb.Run(GATT_SUCCESS, captured_server_if, std::move(captured_service));
}

TEST_F(RasServerTestNoInit, RegisterCallback) {
  // AppRegister should be triggered when Initialize
  EXPECT_CALL(mock_gatt_server_interface_, AppRegister(_, _, _))
          .WillOnce(testing::SaveArg<1>(&captured_gatt_callback_));
  GetRasServer()->Initialize();
  ASSERT_NE(captured_gatt_callback_, nullptr);

  // RegisterCallback
  GetRasServer()->RegisterCallbacks(&mock_ras_server_callbacks_);

  // OnRasServerConnected should be triggered after receiving BTA_GATTS_CONNECT_EVT
  EXPECT_CALL(mock_ras_server_callbacks_, OnRasServerConnected(test_address_))
          .Times(1);  // Expect the method to be called once
  tBTA_GATTS p_data;
  p_data.conn.transport = BT_TRANSPORT_LE;
  p_data.conn.remote_bda = test_address_;
  captured_gatt_callback_(BTA_GATTS_CONNECT_EVT, &p_data);
}

}  // namespace bluetooth::ras
