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
#include "stack/include/bt_types.h"
#include "stack/include/main_thread.h"
#include "test/mock/mock_main_shim_entry.h"

using testing::_;
using testing::DoAll;
using testing::Mock;
using testing::MockFunction;
using testing::NiceMock;
using testing::NotNull;
using testing::Return;
using testing::SaveArg;
using testing::Test;
using testing::WithArg;

using namespace bluetooth::ras;
using namespace ::ras;
using namespace ::ras::uuid;
using namespace bluetooth;

static const uint16_t kVendorSpecificCharacteristic16Bit1 = 0x5566;
static const uint16_t kVendorSpecificCharacteristic16Bit2 = 0x5567;
static const bluetooth::Uuid kVendorSpecificCharacteristic1 =
        bluetooth::Uuid::From16Bit(kVendorSpecificCharacteristic16Bit1);
static const bluetooth::Uuid kVendorSpecificCharacteristic2 =
        bluetooth::Uuid::From16Bit(kVendorSpecificCharacteristic16Bit2);

// static uint16_t GetCharacteristicHandle(const bluetooth::Uuid& uuid) {
//   switch (uuid.As16Bit()) {
//     case kRasFeaturesCharacteristic16bit:
//       return 0x0001;
//     case kRasRealTimeRangingDataCharacteristic16bit:
//       return 0x0002;
//     case kRasOnDemandDataCharacteristic16bit:
//       return 0x0004;
//     case kRasControlPointCharacteristic16bit:
//       return 0x0006;
//     case kRasRangingDataReadyCharacteristic16bit:
//       return 0x0008;
//     case kRasRangingDataOverWrittenCharacteristic16bit:
//       return 0x000a;
//     case kVendorSpecificCharacteristic16Bit1:
//       return 0x000c;
//     case kVendorSpecificCharacteristic16Bit2:
//       return 0x000d;
//     default:
//       bluetooth::log::warn("Unknown uuid");
//       return 0xFFF0;
//   }
// }
//
// static uint16_t GetDescriptorHandle(const bluetooth::Uuid& uuid) {
//   return GetCharacteristicHandle(uuid) + 1;
// }
//
// static void UpdateTestServiceHandle(std::vector<btgatt_db_element_t>& service) {
//   for (uint16_t i = 0; i < service.size(); i++) {
//     service[i].attribute_handle = GetCharacteristicHandle(service[i].uuid);
//     // Check if descriptor exist
//     if (i < service.size() - 1 && service[i + 1].type == BTGATT_DB_DESCRIPTOR) {
//       service[i + 1].attribute_handle = GetDescriptorHandle(service[i].uuid);
//       i++;
//     }
//   }
// }

namespace bluetooth::ras {

// class MockRasServerCallbacks : public RasServerCallbacks {
// public:
//   MOCK_METHOD(void, OnVendorSpecificReply,
//               (const RawAddress& address,
//                const std::vector<VendorSpecificCharacteristic>& vendor_specific_reply),
//               (override));
//   MOCK_METHOD(void, OnRasServerConnected, (const RawAddress& identity_address), (override));
//   MOCK_METHOD(void, OnMtuChangedFromServer, (const RawAddress& address, uint16_t mtu),
//   (override)); MOCK_METHOD(void, OnRasServerDisconnected, (const RawAddress& identity_address),
//   (override));
// };

class RasClientTestNoInit : public ::testing::Test {
protected:
  void SetUp() override {
    // Init test data
    gatt::SetMockBtaGattInterface(&mock_gatt_interface_);
    RawAddress::FromString("11:22:33:44:55:66", test_address_);
    //     VendorSpecificCharacteristic vendor_specific_characteristic1,
    //     vendor_specific_characteristic2; vendor_specific_characteristic1.characteristicUuid_ =
    //     kVendorSpecificCharacteristic1; vendor_specific_characteristic1.value_ = {0x01, 0x02,
    //     0x03}; vendor_specific_characteristic2.characteristicUuid_ =
    //     kVendorSpecificCharacteristic2; vendor_specific_characteristic2.value_ = {0x04, 0x05,
    //     0x06}; vendor_specific_characteristics_.push_back(vendor_specific_characteristic1);
    //     vendor_specific_characteristics_.push_back(vendor_specific_characteristic2);
  }

  // std::vector<VendorSpecificCharacteristic> vendor_specific_characteristics_;
  RawAddress test_address_;
  // uint16_t test_conn_id_ = 0x0001;
  // tBTA_GATTS_CBACK* captured_gatt_callback_ = nullptr;
  gatt::MockBtaGattInterface mock_gatt_interface_;
  // MockRasServerCallbacks mock_ras_server_callbacks_;
};

TEST_F(RasClientTestNoInit, InitializationSuccessful) { GetRasClient()->Initialize(); }

}  // namespace bluetooth::ras
