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
public:
  gatt::MockBtaGattServerInterface mock_gatt_server_interface_;

protected:
  void SetUp() override {
    bluetooth::log::info("CYDBG SetUp 123");
    gatt::SetMockBtaGattServerInterface(&mock_gatt_server_interface_);
  }
};

TEST_F(RasServerTestNoInit, InitializationSuccessful) {
  tBTA_GATTS_CBACK* captured_callback = nullptr;

  bool temp = false;
  EXPECT_CALL(mock_gatt_server_interface_, AppRegister(_, _, _))
          .WillOnce(testing::SaveArg<1>(&captured_callback));

  GetRasServer()->Initialize();

  ASSERT_NE(captured_callback, nullptr);

  uint16_t handle = 10;
  tGATTS_DATA gatts_data;
  gatts_data.read_req.handle = handle;
  tBTA_GATTS gatts_cb_data;
  gatts_cb_data.req_data.p_data = &gatts_data;

  bluetooth::log::info("CYDBG captured_callback s");

  captured_callback(BTA_GATTS_READ_CHARACTERISTIC_EVT, &gatts_cb_data);

  bluetooth::log::info("CYDBG captured_callback e");

  captured_callback(BTA_GATTS_READ_CHARACTERISTIC_EVT, &gatts_cb_data);

  tBTA_GATTS gatts_cb_data2;
  uint8_t server_if = 10;
  gatts_cb_data2.reg_oper.status = GATT_SUCCESS;
  gatts_cb_data2.reg_oper.server_if = server_if;
  captured_callback(BTA_GATTS_REG_EVT, &gatts_cb_data2);
}
