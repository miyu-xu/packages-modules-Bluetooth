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

#include "btif/include/main_callbacks.h"

#include <bluetooth/log.h>

#include <memory>

#include "btif/include/btif_common.h"
#include "include/hardware/bluetooth.h"

namespace bluetooth::api::main {

namespace {

void print(std::string s) { log::error("Unable to send callback:{}", s); }

bt_callbacks_t bt_hal_empty_cbacks_ = {
    .size = sizeof(bt_callbacks_t),
    .adapter_state_changed_cb = [](bt_state_t /* state */) { print(__func__); },
    .adapter_properties_cb =
        [](bt_status_t /* status */, int /* num_properties */,
           bt_property_t* /* num_properties */) { print(__func__); },
    .remote_device_properties_cb =
        [](bt_status_t /* status */, RawAddress* /* bd_addr */,
           int /* num_properties */,
           bt_property_t* /* properties */) { print(__func__); },
    .device_found_cb = [](int num_properties,
                          bt_property_t* /* properties */) { print(__func__); },
    .discovery_state_changed_cb =
        [](bt_discovery_state_t /* state */) { print(__func__); },
    .pin_request_cb = [](RawAddress* /* remote_bd_addr */,
                         bt_bdname_t* /* bd_name */, uint32_t /* cod */,
                         bool /* min_16_digit */) { print(__func__); },
    .ssp_request_cb = [](RawAddress* /* remote_bd_addr */,
                         bt_bdname_t* /* bd_name */, uint32_t /* cod */,
                         bt_ssp_variant_t /* pairing_variant */,
                         uint32_t /* pass_key */) { print(__func__); },
    .bond_state_changed_cb = [](bt_status_t /* status */,
                                RawAddress* /* remote_bd_addr */,
                                bt_bond_state_t /* state */,
                                int /* fail_reason */) { print(__func__); },
    .address_consolidate_cb =
        [](RawAddress* /* main_bd_addr */,
           RawAddress* /* secondary_bd_addr */) { print(__func__); },
    .le_address_associate_cb =
        [](RawAddress* /* main_bd_addr */,
           RawAddress* /* secondary_bd_addr */) { print(__func__); },
    .acl_state_changed_cb =
        [](bt_status_t /* status */, RawAddress* /* remote_bd_addr */,
           bt_acl_state_t /* state */, int /* transport_link_type */,
           bt_hci_error_code_t /* hci_reason */,
           bt_conn_direction_t /* direction */,
           uint16_t /* acl_handle */) { print(__func__); },
    .thread_evt_cb = [](bt_cb_thread_evt /* evt */) { print(__func__); },
    .dut_mode_recv_cb = [](uint16_t /* opcode */, uint8_t* /* buf */,
                           uint8_t /* len */) { print(__func__); },
    .le_test_mode_cb = [](bt_status_t /* status */,
                          uint16_t /* num_packets */) { print(__func__); },
    .energy_info_cb = [](bt_activity_energy_info* /* energy_info */,
                         bt_uid_traffic_t* /* uid_data */) { print(__func__); },
    .link_quality_report_cb =
        [](uint64_t /* timestamp */, int /* report_id */, int /* rssi */,
           int /* snr */, int /* retransmission_count */,
           int /* packets_not_receive_count */,
           int /* negative_acknowledgement_count */) { print(__func__); },
    .generate_local_oob_data_cb =
        [](tBT_TRANSPORT /* transport */, bt_oob_data_t /* oob_data */) {
          print(__func__);
        },
    .switch_buffer_size_cb =
        [](bool /* is_low_latency_buffer_size */) { print(__func__); },
    .switch_codec_cb =
        [](bool /* is_low_latency_buffer_size */) { print(__func__); },
    .le_rand_cb = [](uint64_t /* random */) { print(__func__); },
    .key_missing_cb = [](const RawAddress /* bd_addr */) { print(__func__); },
};

}  // namespace

struct Callbacks::impl {
  bt_callbacks_t* bt_hal_cbacks_{nullptr};
};

Callbacks::Callbacks() { pimpl_ = std::make_unique<Callbacks::impl>(); }

Callbacks::~Callbacks() { pimpl_.reset(); }

// Callbacks are set up on an arbitrary thread before jni starts up
void Callbacks::set_callbacks(bt_callbacks_t* bt_hal_cbacks) {
  pimpl_->bt_hal_cbacks_ = bt_hal_cbacks;
}

void Callbacks::reset_callbacks() {
  log::assert_that(
      is_on_jni_thread(),
      "Callbacks may only be cleared synchronized on the jni thread");
  pimpl_->bt_hal_cbacks_ = nullptr;
}

// NOTE: This is unprotected
bool Callbacks::is_callbacks_set() const {
  //  log::assert_that(is_on_jni_thread(),
  //                   "Callbacks may only be synchronized on the jni thread");
  return (pimpl_->bt_hal_cbacks_ != nullptr);
}

bt_callbacks_t* Callbacks::operator()() const {
  log::assert_that(is_on_jni_thread(),
                   "Callbacks may only be called on the jni thread");
  return (pimpl_->bt_hal_cbacks_ == nullptr ? &bt_hal_empty_cbacks_
                                            : pimpl_->bt_hal_cbacks_);
}

}  // namespace bluetooth::api::main
