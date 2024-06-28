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
#pragma once

#include <stddef.h>
#include <stdlib.h>

#include <cstdint>
#include <functional>

#include "stack/include/btm_status.h"
#include "stack/rnr/remote_name_request.h"
#include "types/bt_transport.h"
#include "types/raw_address.h"

namespace test {
namespace mock {
namespace stack_rnr {

// Name: BTM_CancelRemoteDeviceName
// Params: void
// Return: tBTM_STATUS
struct BTM_CancelRemoteDeviceName {
  static tBTM_STATUS return_value;
  std::function<tBTM_STATUS(void)> body{[](void) { return return_value; }};
  tBTM_STATUS operator()(void) { return body(); }
};
extern struct BTM_CancelRemoteDeviceName BTM_CancelRemoteDeviceName;

// Name: BTM_ReadRemoteDeviceName
// Params: const RawAddress& remote_bda, tBTM_NAME_CMPL_CB* p_cb, tBT_TRANSPORT
// transport Return: tBTM_STATUS
struct BTM_ReadRemoteDeviceName {
  static tBTM_STATUS return_value;
  std::function<tBTM_STATUS(const RawAddress& remote_bda, tBTM_NAME_CMPL_CB* p_cb,
                            tBT_TRANSPORT transport)>
          body{[](const RawAddress& /* remote_bda */, tBTM_NAME_CMPL_CB* /* p_cb */,
                  tBT_TRANSPORT /* transport */) { return return_value; }};
  tBTM_STATUS operator()(const RawAddress& remote_bda, tBTM_NAME_CMPL_CB* p_cb,
                         tBT_TRANSPORT transport) {
    return body(remote_bda, p_cb, transport);
  }
};
extern struct BTM_ReadRemoteDeviceName BTM_ReadRemoteDeviceName;

// Name: btm_inq_remote_name_timer_timeout
// Params:  void* data
// Return: void
struct btm_inq_remote_name_timer_timeout {
  std::function<void(void* data)> body{[](void* /* data */) {}};
  void operator()(void* data) { body(data); }
};
extern struct btm_inq_remote_name_timer_timeout btm_inq_remote_name_timer_timeout;

// Name: btm_inq_rmt_name_failed_cancelled
// Params: void
// Return: void
struct btm_inq_rmt_name_failed_cancelled {
  std::function<void(void)> body{[](void) {}};
  void operator()(void) { body(); }
};
extern struct btm_inq_rmt_name_failed_cancelled btm_inq_rmt_name_failed_cancelled;

// Name: btm_process_remote_name
// Params: const RawAddress* bda, const BD_NAME bdn, uint16_t evt_len,
// tHCI_STATUS hci_status Return: void
struct btm_process_remote_name {
  std::function<void(const RawAddress* bda, const BD_NAME bdn, uint16_t evt_len,
                     tHCI_STATUS hci_status)>
          body{[](const RawAddress* /* bda */, const BD_NAME /* bdn */, uint16_t /* evt_len */,
                  tHCI_STATUS /* hci_status */) {}};
  void operator()(const RawAddress* bda, const BD_NAME bdn, uint16_t evt_len,
                  tHCI_STATUS hci_status) {
    body(bda, bdn, evt_len, hci_status);
  }
};
extern struct btm_process_remote_name btm_process_remote_name;

// Name: BTM_IsRemoteNameKnown
// Params: const RawAddress& bd_addr, tBT_TRANSPORT transport
// Return: bool
struct BTM_IsRemoteNameKnown {
  std::function<bool(const RawAddress& bd_addr, tBT_TRANSPORT transport)> body{
          [](const RawAddress& /* bd_addr */, tBT_TRANSPORT /* transport */) { return false; }};
  bool operator()(const RawAddress& bd_addr, tBT_TRANSPORT transport) {
    return body(bd_addr, transport);
  }
};
extern struct BTM_IsRemoteNameKnown BTM_IsRemoteNameKnown;

}  // namespace stack_rnr
}  // namespace mock
}  // namespace test
