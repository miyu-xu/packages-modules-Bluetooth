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

// Mock include file to share data between tests and mock
#include "test/mock/mock_stack_rnr.h"

#include <cstdint>

#include "test/common/mock_functions.h"

namespace test {
namespace mock {
namespace stack_rnr {

// Function state capture and return values, if needed
struct BTM_CancelRemoteDeviceName BTM_CancelRemoteDeviceName;
struct BTM_IsRemoteNameKnown BTM_IsRemoteNameKnown;
struct BTM_ReadRemoteDeviceName BTM_ReadRemoteDeviceName;
struct btm_inq_remote_name_timer_timeout btm_inq_remote_name_timer_timeout;
struct btm_inq_rmt_name_failed_cancelled btm_inq_rmt_name_failed_cancelled;
struct btm_process_remote_name btm_process_remote_name;

}  // namespace stack_rnr
}  // namespace mock
}  // namespace test

// Mocked function return values, if any
namespace test {
namespace mock {
namespace stack_rnr {

tBTM_STATUS BTM_CancelRemoteDeviceName::return_value = 0;
tBTM_STATUS BTM_ReadRemoteDeviceName::return_value = 0;

}  // namespace stack_rnr
}  // namespace mock
}  // namespace test

// Mocked functions, if any
tBTM_STATUS BTM_CancelRemoteDeviceName(void) {
  inc_func_call_count(__func__);
  return test::mock::stack_rnr::BTM_CancelRemoteDeviceName();
}
bool BTM_IsRemoteNameKnown(const RawAddress& bd_addr, tBT_TRANSPORT transport) {
  inc_func_call_count(__func__);
  return test::mock::stack_rnr::BTM_IsRemoteNameKnown(bd_addr, transport);
}
tBTM_STATUS BTM_ReadRemoteDeviceName(const RawAddress& remote_bda, tBTM_NAME_CMPL_CB* p_cb,
                                     tBT_TRANSPORT transport) {
  inc_func_call_count(__func__);
  return test::mock::stack_rnr::BTM_ReadRemoteDeviceName(remote_bda, p_cb, transport);
}
void btm_inq_remote_name_timer_timeout(void* data) {
  inc_func_call_count(__func__);
  test::mock::stack_rnr::btm_inq_remote_name_timer_timeout(data);
}
void btm_inq_rmt_name_failed_cancelled(void) {
  inc_func_call_count(__func__);
  test::mock::stack_rnr::btm_inq_rmt_name_failed_cancelled();
}
void btm_process_remote_name(const RawAddress* bda, const BD_NAME bdn, uint16_t evt_len,
                             tHCI_STATUS hci_status) {
  inc_func_call_count(__func__);
  test::mock::stack_rnr::btm_process_remote_name(bda, bdn, evt_len, hci_status);
}
// Mocked functions complete
// END mockcify generation
