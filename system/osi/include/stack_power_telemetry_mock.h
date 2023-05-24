/*
 * Copyright 2023 The Android Open Source Project
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

#pragma once

#include <gmock/gmock.h>

#include "osi/include/stack_power_telemetry.h"
#include "types/raw_address.h"

namespace power_telemetry {

struct PowerTelemetryImpl;

class MockPowerTelemetry : public power_telemetry::PowerTelemetry {
 public:
  MOCK_METHOD(void, LogScanStarted, (), (override));
  MOCK_METHOD(void, LogHciCmdDetail, (), (override));
  MOCK_METHOD(void, LogHciEvtDetail, (), (override));
  MOCK_METHOD(void, LogLinkDetails,
              (uint16_t handle, const RawAddress& bdaddr, bool isConnected,
               bool is_acl_link),
              (override));
  MOCK_METHOD(void, LogRxAclPktData, (uint16_t len), (override));
  MOCK_METHOD(void, LogTxAclPktData, (uint16_t len), (override));
  MOCK_METHOD(void, LogChannelConnected,
              (uint16_t psm, int32_t src_id, int32_t dst_id,
               const RawAddress& bd_addr),
              (override));
  MOCK_METHOD(void, LogChannelDisconnected,
              (uint16_t psm, int32_t src_id, int32_t dst_id,
               const RawAddress& bd_addr),
              (override));
  MOCK_METHOD(void, LogRxBytes,
              (uint16_t psm, int32_t src_id, int32_t dst_id,
               const RawAddress& bd_addr, int32_t num_bytes),
              (override));
  MOCK_METHOD(void, LogTxBytes,
              (uint16_t psm, int32_t src_id, int32_t dst_id,
               const RawAddress& bd_addr, int32_t num_bytes),
              (override));
  MOCK_METHOD(void, LogSniffStarted,
              (uint16_t handle, const RawAddress& bdaddr), (override));
  MOCK_METHOD(void, LogSniffStopped,
              (uint16_t handle, const RawAddress& bdaddr), (override));
  MOCK_METHOD(void, LogInqScanStarted, (), (override));
  MOCK_METHOD(void, LogInqScanStopped, (), (override));
  MOCK_METHOD(void, LogBleAdvStarted, (), (override));
  MOCK_METHOD(void, LogBleAdvStopped, (), (override));
  MOCK_METHOD(void, LogTxPower, (void* res), (override));
  MOCK_METHOD(void, Dumpsys, (int32_t fd), (override));
};

PowerTelemetry& GetInstance();

}  // namespace power_telemetry
