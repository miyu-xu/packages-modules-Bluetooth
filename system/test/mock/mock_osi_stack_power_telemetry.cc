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
#include <string>

// Mock include file to share data between tests and mock
#include "test/mock/mock_osi_stack_power_telemetry.h"

// Original usings

// Mocked internal structures, if any

namespace test {
namespace mock {
namespace osi_stack_power_telemetry {

// Function state capture and return values, if needed
struct GetTimeString GetTimeString;

}  // namespace osi_stack_power_telemetry
}  // namespace mock
}  // namespace test

// Mocked function return values, if any
namespace test {
namespace mock {
namespace osi_stack_power_telemetry {

std::string GetTimeString::return_value = std::string();

}  // namespace osi_stack_power_telemetry
}  // namespace mock
}  // namespace test

power_telemetry::LogDataContainer&
power_telemetry::GetCurrentLogDataContainer() {
  inc_func_call_count(__func__);
  return test::mock::osi_stack_power_telemetry::fake_container;
}
void power_telemetry::PowerTelemetry::RecordLogDataContainer() {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogScanStarted() {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogChannelConnected(
    uint16_t psm, int32_t src_id, int32_t dst_id, const RawAddress& bd_addr) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogChannelDisconnected(
    uint16_t psm, int32_t src_id, int32_t dst_id, const RawAddress& bd_addr) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogTxBytes(uint16_t psm, int32_t src_id,
                                                 int32_t dst_id,
                                                 const RawAddress& bd_addr,
                                                 int32_t num_bytes) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogRxBytes(uint16_t psm, int32_t src_id,
                                                 int32_t dst_id,
                                                 const RawAddress& bd_addr,
                                                 int32_t num_bytes) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::Dumpsys(int32_t fd) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogRxAclPktData(uint16_t len) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogTxAclPktData(uint16_t len) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogLinkDetails(uint16_t handle,
                                                     const RawAddress& bdaddr,
                                                     bool isConnected,
                                                     bool is_acl_link) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogAclTxPowerLevel(uint16_t handle,
                                                         uint8_t txPower) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogInqScanStarted() {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogInqScanStopped() {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogBleAdvStarted() {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogBleAdvStopped() {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogHciCmdDetail() {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogHciEvtDetail() {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogSniffActivity(uint16_t handle,
                                                       const RawAddress& bdaddr,
                                                       bool sniff_entered) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogTxPower(void* res) {
  inc_func_call_count(__func__);
}
void power_telemetry::PowerTelemetry::LogTrafficData() {
  inc_func_call_count(__func__);
}
power_telemetry::PowerTelemetry& power_telemetry::GetInstance() {
  return test::mock::osi_stack_power_telemetry::fake_power_telemetry;
}
// Mocked functions complete
// END mockcify generation
