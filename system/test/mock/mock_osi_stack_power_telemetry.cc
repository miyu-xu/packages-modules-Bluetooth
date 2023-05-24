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
/*
 * Generated mock file from original source file
 *   Functions generated:5
 *
 *  mockcify.pl ver 0.6.1
 */

#include <cstdint>
#include <functional>
#include <map>
#include <string>

// Mock include file to share data between tests and mock
#include "test/mock/mock_osi_stack_power_telemetry.h"

// Original usings

// Mocked internal structures, if any

namespace test {
namespace mock {
namespace osi_stack_power_telemetry {

// Function state capture and return values, if needed
struct GetCurrentTimeSec GetCurrentTimeSec;
struct GetCurrentTimeString GetCurrentTimeString;
struct GetTimeString GetTimeString;
struct GetTimeStringFromSec GetTimeStringFromSec;
struct LogTxPower_cb LogTxPower_cb;

}  // namespace osi_stack_power_telemetry
}  // namespace mock
}  // namespace test

// Mocked function return values, if any
namespace test {
namespace mock {
namespace osi_stack_power_telemetry {

int64_t GetCurrentTimeSec::return_value = 0;
std::string GetCurrentTimeString::return_value = std::string();
std::string GetTimeString::return_value = std::string();
std::string GetTimeStringFromSec::return_value = std::string();

}  // namespace osi_stack_power_telemetry
}  // namespace mock
}  // namespace test

// Mocked functions, if any
int64_t GetCurrentTimeSec() {
  inc_func_call_count(__func__);
  return test::mock::osi_stack_power_telemetry::GetCurrentTimeSec();
}
std::string GetCurrentTimeString() {
  inc_func_call_count(__func__);
  return test::mock::osi_stack_power_telemetry::GetCurrentTimeString();
}
std::string GetTimeString(time_t tstamp) {
  inc_func_call_count(__func__);
  return test::mock::osi_stack_power_telemetry::GetTimeString(tstamp);
}
std::string GetTimeStringFromSec(int64_t timeStampSec) {
  inc_func_call_count(__func__);
  return test::mock::osi_stack_power_telemetry::GetTimeStringFromSec(
      timeStampSec);
}
void LogTxPower_cb(void* res) {
  inc_func_call_count(__func__);
  test::mock::osi_stack_power_telemetry::LogTxPower_cb(res);
}
power_telemetry::LogDataContainer&
power_telemetry::PowerTelemetry::GetCurrentLogDataContainer() {
  inc_func_call_count(__func__);
  return test::mock::osi_stack_power_telemetry::fake_container;
}
void power_telemetry::PowerTelemetry::RecordLogDataContainer() {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogScanStarted() {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogScanEnded() {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogLeScanStarted() {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogChannelConnected(int32_t channel_type,
                                                          int32_t src_id,
                                                          int32_t dst_id,
                                                          RawAddress bd_addr,
                                                          int32_t psm) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogChannelDisconnected(
    int32_t channel_type, int32_t src_id, int32_t dst_id, RawAddress bd_addr) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogTxBytes(int32_t channel_type,
                                                 int32_t src_id, int32_t dst_id,
                                                 RawAddress bd_addr,
                                                 int32_t num_bytes) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogRxBytes(int32_t channel_type,
                                                 int32_t src_id, int32_t dst_id,
                                                 RawAddress bd_addr,
                                                 int32_t num_bytes) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::PowerTelemetryDump(int32_t fd) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogAclPktDetails(int32_t type,
                                                       uint16_t len) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogAclLinkDetails(
    uint16_t handle, const RawAddress* bdaddr, bool isConnected) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogScoLinkDetails(uint16_t handle,
                                                        RawAddress bdaddr,
                                                        bool isConnected) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogAclTxPowerLevel(uint16_t handle,
                                                         uint8_t txPower) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogInqScanDetails(bool started) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogBleAdvDetails(bool started) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogHciCmdEvtDetails(int32_t type) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogTxPower(void* res) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogTrafficData() {
  inc_func_call_count(__func__);
}
power_telemetry::PowerTelemetry* power_telemetry::GetInstance() {
  return &test::mock::osi_stack_power_telemetry::fake_power_telemetry;
}
// Mocked functions complete
// END mockcify generation
