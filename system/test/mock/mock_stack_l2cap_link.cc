/*
 * Copyright 2020 The Android Open Source Project
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
 */

#include <map>
#include <string>

#include "test/common/mock_functions.h"
#define UNUSED_ATTR

#include <cstdint>

#include "stack/include/bt_hdr.h"
#include "stack/l2cap/l2c_int.h"
#include "types/raw_address.h"

BT_HDR* l2cu_get_next_buffer_to_send(tL2C_LCB* p_lcb) {
  increment_mock_function_call_count(__func__);
  return nullptr;
}
bool l2c_link_hci_disc_comp(uint16_t handle, tHCI_REASON reason) {
  increment_mock_function_call_count(__func__);
  return false;
}
tBTM_STATUS l2cu_ConnectAclForSecurity(const RawAddress& bd_addr) {
  increment_mock_function_call_count(__func__);
  return BTM_SUCCESS;
}
tL2C_CCB* l2cu_get_next_channel_in_rr(tL2C_LCB* p_lcb) {
  increment_mock_function_call_count(__func__);
  return nullptr;
}
void l2c_OnHciModeChangeSendPendingPackets(RawAddress remote) {
  increment_mock_function_call_count(__func__);
}
void l2c_info_resp_timer_timeout(void* data) {
  increment_mock_function_call_count(__func__);
}
void l2c_link_adjust_allocation(void) {
  increment_mock_function_call_count(__func__);
}
void l2c_link_adjust_chnl_allocation(void) {
  increment_mock_function_call_count(__func__);
}
void l2c_link_check_send_pkts(tL2C_LCB* p_lcb, uint16_t local_cid,
                              BT_HDR* p_buf) {
  increment_mock_function_call_count(__func__);
}
void l2c_link_hci_conn_comp(tHCI_STATUS status, uint16_t handle,
                            const RawAddress& p_bda) {
  increment_mock_function_call_count(__func__);
}
void l2c_link_hci_conn_req(const RawAddress& bd_addr) {
  increment_mock_function_call_count(__func__);
}
void l2c_link_init(const uint16_t acl_buffer_count_classic) {
  increment_mock_function_call_count(__func__);
}
void l2c_link_role_changed(const RawAddress* bd_addr, uint8_t new_role,
                           uint8_t hci_status) {
  increment_mock_function_call_count(__func__);
}
void l2c_link_sec_comp(const RawAddress* p_bda,
                       UNUSED_ATTR tBT_TRANSPORT transport, void* p_ref_data,
                       uint8_t status) {
  increment_mock_function_call_count(__func__);
}
void l2c_link_sec_comp2(const RawAddress& p_bda,
                        UNUSED_ATTR tBT_TRANSPORT transport, void* p_ref_data,
                        uint8_t status) {
  increment_mock_function_call_count(__func__);
}
void l2c_link_segments_xmitted(BT_HDR* p_msg) {
  increment_mock_function_call_count(__func__);
}
void l2c_link_timeout(tL2C_LCB* p_lcb) {
  increment_mock_function_call_count(__func__);
}
void l2c_packets_completed(uint16_t handle, uint16_t num_sent) {
  increment_mock_function_call_count(__func__);
}
void l2c_pin_code_request(const RawAddress& bd_addr) {
  increment_mock_function_call_count(__func__);
}
void l2cble_update_sec_act(const RawAddress& bd_addr, uint16_t sec_act) {
  increment_mock_function_call_count(__func__);
}
