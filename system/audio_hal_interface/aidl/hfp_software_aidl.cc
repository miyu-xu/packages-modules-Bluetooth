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
#include <map>

#include "aidl/android/hardware/bluetooth/audio/AudioConfiguration.h"
#include "aidl/transport_instance.h"
#include "btif_hf.h"
#include "hardware/bluetooth_headset_interface.h"
#define LOG_TAG "BTAudioHfpAIDL"

#include "bta/ag/bta_ag_int.h"
#include "btm_api_types.h"
#include "hfp_software_aidl.h"
#include "types/raw_address.h"

namespace bluetooth {
namespace audio {
namespace aidl {
namespace hfp {

std::map<bt_status_t, BluetoothAudioCtrlAck> status_to_ack_map = {
    {BT_STATUS_SUCCESS, BluetoothAudioCtrlAck::SUCCESS_FINISHED},
    {BT_STATUS_DONE, BluetoothAudioCtrlAck::SUCCESS_FINISHED},
    {BT_STATUS_FAIL, BluetoothAudioCtrlAck::FAILURE},
    {BT_STATUS_NOT_READY, BluetoothAudioCtrlAck::FAILURE_BUSY},
    {BT_STATUS_BUSY, BluetoothAudioCtrlAck::FAILURE_BUSY},
    {BT_STATUS_UNSUPPORTED, BluetoothAudioCtrlAck::FAILURE_UNSUPPORTED},
};

tBTA_AG_SCB* get_hfp_active_device_callback() {
  const RawAddress& addr = bta_ag_get_active_device();
  if (addr.IsEmpty()) {
    LOG(ERROR) << __func__ << ": No active device found";
    return nullptr;
  }
  auto idx = bta_ag_idx_by_bdaddr(&addr);
  if (idx == 0) {
    LOG(ERROR) << __func__ << ": No index found for active device";
    return nullptr;
  }
  auto cb = bta_ag_scb_by_idx(idx);
  if (cb == nullptr) {
    LOG(ERROR) << __func__ << ": No callback for the active device";
    return nullptr;
  }
  return cb;
}

HfpTransport::HfpTransport() { hfp_pending_cmd_ = HFP_CTRL_CMD_NONE; }

BluetoothAudioCtrlAck HfpTransport::StartRequest() {
  if (hfp_pending_cmd_ == HFP_CTRL_CMD_START) {
    LOG(INFO) << __func__ << ": HFP_CTRL_CMD_START in progress";
    return BluetoothAudioCtrlAck::PENDING;
  } else if (hfp_pending_cmd_ != HFP_CTRL_CMD_NONE) {
    LOG(WARNING) << __func__ << ": busy in pending_cmd=" << hfp_pending_cmd_;
    return BluetoothAudioCtrlAck::FAILURE_BUSY;
  }

  auto cb = get_hfp_active_device_callback();
  if (cb == nullptr) return BluetoothAudioCtrlAck::FAILURE;

  if (bta_ag_sco_is_open(cb)) {
    // Already started, ACK back immediately.
    return BluetoothAudioCtrlAck::SUCCESS_FINISHED;
  }

  /* Post start SCO event and wait for sco to open */
  hfp_pending_cmd_ = HFP_CTRL_CMD_START;
  auto status =
      bluetooth::headset::GetInterface()->ConnectAudio(&cb->peer_addr, 0);
  hfp_pending_cmd_ = HFP_CTRL_CMD_NONE;
  LOG(INFO) << __func__ << ": ConnectAudio status = " << status;
  auto ctrl_ack = status_to_ack_map.find(status);
  if (ctrl_ack == status_to_ack_map.end())
    return BluetoothAudioCtrlAck::FAILURE;
  return ctrl_ack->second;
}

void HfpTransport::StopRequest() {
  LOG(INFO) << __func__ << ": handling";
  RawAddress addr = bta_ag_get_active_device();
  if (addr.IsEmpty()) {
    LOG(ERROR) << __func__ << ": No active device found";
    return;
  }
  hfp_pending_cmd_ = HFP_CTRL_CMD_STOP;
  auto status = bluetooth::headset::GetInterface()->DisconnectAudio(&addr);
  LOG(INFO) << __func__ << ": DisconnectAudio status = " << status;
  hfp_pending_cmd_ = HFP_CTRL_CMD_NONE;
  return;
}

void HfpTransport::ResetPendingCmd() { hfp_pending_cmd_ = HFP_CTRL_CMD_NONE; }

uint8_t HfpTransport::GetPendingCmd() const { return hfp_pending_cmd_; }

// Unimplemented functions
void HfpTransport::LogBytesProcessed(size_t bytes_read) {}

BluetoothAudioCtrlAck HfpTransport::SuspendRequest() {
  return BluetoothAudioCtrlAck::FAILURE_UNSUPPORTED;
}

void HfpTransport::SetLatencyMode(LatencyMode latency_mode) {}

bool GetPresentationPosition(uint64_t* remote_delay_report_ns,
                             uint64_t* total_bytes_read,
                             timespec* data_position) {
  return false;
}

void HfpTransport::SourceMetadataChanged(
    const source_metadata_v7_t& source_metadata) {}

void HfpTransport::SinkMetadataChanged(const sink_metadata_v7_t&) {}

void HfpTransport::ResetPresentationPosition() {}

// Source / sink functions
HfpSinkTransport::HfpSinkTransport(SessionType session_type)
    : IBluetoothSinkTransportInstance(session_type, (AudioConfiguration){}),
      HfpTransport(){};

void HfpSinkTransport::LogBytesRead(size_t bytes_read) {
  return HfpTransport::LogBytesProcessed(bytes_read);
}

HfpSourceTransport::HfpSourceTransport(SessionType session_type)
    : IBluetoothSourceTransportInstance(session_type, (AudioConfiguration){}),
      HfpTransport(){};

void HfpSourceTransport::LogBytesWritten(size_t bytes_written) {
  return HfpTransport::LogBytesProcessed(bytes_written);
}

}  // namespace hfp
}  // namespace aidl
}  // namespace audio
}  // namespace bluetooth
