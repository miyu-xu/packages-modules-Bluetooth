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

BluetoothAudioCtrlAck hfp_ack_to_bt_audio_ctrl_ack(tHFP_CTRL_ACK ack) {
  switch (ack) {
    case HFP_CTRL_ACK_SUCCESS:
      return BluetoothAudioCtrlAck::SUCCESS_FINISHED;
    case HFP_CTRL_ACK_PENDING:
      return BluetoothAudioCtrlAck::PENDING;
    case HFP_CTRL_ACK_FAILURE:
      return BluetoothAudioCtrlAck::FAILURE;
    default:
      return BluetoothAudioCtrlAck::FAILURE;
  }
}

HfpTransport::HfpTransport(SessionType sessionType) {
  hfp_pending_cmd_ = HFP_CTRL_CMD_NONE;
}

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

BluetoothAudioCtrlAck HfpTransport::StartRequest(bool is_low_latency) {
  if (hfp_pending_cmd_ == HFP_CTRL_CMD_START) {
    LOG(INFO) << __func__ << ": HFP_CTRL_CMD_START in progress";
    return hfp_ack_to_bt_audio_ctrl_ack(HFP_CTRL_ACK_PENDING);
  } else if (hfp_pending_cmd_ != HFP_CTRL_CMD_NONE) {
    LOG(WARNING) << __func__ << ": busy in pending_cmd=" << hfp_pending_cmd_;
    return hfp_ack_to_bt_audio_ctrl_ack(HFP_CTRL_ACK_FAILURE);
  }

  auto cb = get_hfp_active_device_callback();
  if (cb == nullptr) return hfp_ack_to_bt_audio_ctrl_ack(HFP_CTRL_ACK_FAILURE);

  if (bta_ag_sco_is_open(cb)) {
    // Already started, ACK back immediately.
    return hfp_ack_to_bt_audio_ctrl_ack(HFP_CTRL_ACK_SUCCESS);
  }

  /* Post start SCO event and wait for sco to open */
  hfp_pending_cmd_ = HFP_CTRL_CMD_START;
  bluetooth::headset::StartSco();
  hfp_pending_cmd_ = HFP_CTRL_CMD_NONE;

  return hfp_ack_to_bt_audio_ctrl_ack(HFP_CTRL_ACK_SUCCESS);
}

void HfpTransport::StopRequest() {
  LOG(INFO) << __func__ << ": handling";
  hfp_pending_cmd_ = HFP_CTRL_CMD_STOP;
  bluetooth::headset::StopSco();
}

void HfpTransport::ResetPendingCmd() { hfp_pending_cmd_ = HFP_CTRL_CMD_NONE; }

uint8_t HfpTransport::GetPendingCmd() const { return hfp_pending_cmd_; }

// Unimplemented functions
void HfpTransport::SetLowLatency(bool is_low_latency) {}

void HfpTransport::LogBytesProcessed(size_t bytes_read) {}

bool HfpTransport::GetPresentationPosition(uint64_t* remote_delay_report_ns,
                                           uint64_t* total_bytes_read,
                                           timespec* data_position) {
  return false;
}

void HfpTransport::SourceMetadataChanged(
    const source_metadata_v7_t& source_metadata) {}

void HfpTransport::SinkMetadataChanged(const sink_metadata_v7_t&) {}

void HfpTransport::ResetPresentationPosition() {}

BluetoothAudioCtrlAck HfpTransport::SuspendRequest() {
  return hfp_ack_to_bt_audio_ctrl_ack(HFP_CTRL_ACK_UNSUPPORTED);
}

// Source / sink methods
HfpSinkTransport::HfpSinkTransport(SessionType session_type)
    : IBluetoothSinkTransportInstance(session_type, (AudioConfiguration){}) {
  transport_ = new HfpTransport(session_type);
};

HfpSinkTransport::~HfpSinkTransport() { delete transport_; }

BluetoothAudioCtrlAck HfpSinkTransport::StartRequest(bool is_low_latency) {
  return transport_->StartRequest(is_low_latency);
}

BluetoothAudioCtrlAck HfpSinkTransport::SuspendRequest() {
  return transport_->SuspendRequest();
}

void HfpSinkTransport::SetLowLatency(bool is_low_latency) {
  transport_->SetLowLatency(is_low_latency);
}

bool HfpSinkTransport::GetPresentationPosition(uint64_t* remote_delay_report_ns,
                                               uint64_t* total_bytes_written,
                                               timespec* data_position) {
  return transport_->GetPresentationPosition(
      remote_delay_report_ns, total_bytes_written, data_position);
}

void HfpSinkTransport::SourceMetadataChanged(
    const source_metadata_v7_t& source_metadata) {
  transport_->SourceMetadataChanged(source_metadata);
}

void HfpSinkTransport::SinkMetadataChanged(
    const sink_metadata_v7_t& sink_metadata) {
  transport_->SinkMetadataChanged(sink_metadata);
}

void HfpSinkTransport::ResetPresentationPosition() {
  transport_->ResetPresentationPosition();
}

void HfpSinkTransport::LogBytesRead(size_t bytes_written) {
  transport_->LogBytesProcessed(bytes_written);
}

uint8_t HfpSinkTransport::GetPendingCmd() const {
  return transport_->GetPendingCmd();
}

void HfpSinkTransport::ResetPendingCmd() { transport_->ResetPendingCmd(); }

HfpSourceTransport::HfpSourceTransport(SessionType session_type)
    : IBluetoothSourceTransportInstance(session_type, (AudioConfiguration){}) {
  transport_ = new HfpTransport(session_type);
};

HfpSourceTransport::~HfpSourceTransport() { delete transport_; }

BluetoothAudioCtrlAck HfpSourceTransport::StartRequest(bool is_low_latency) {
  return transport_->StartRequest(is_low_latency);
}

BluetoothAudioCtrlAck HfpSourceTransport::SuspendRequest() {
  return transport_->SuspendRequest();
}

void HfpSourceTransport::SetLowLatency(bool is_low_latency) {
  transport_->SetLowLatency(is_low_latency);
}

bool HfpSourceTransport::GetPresentationPosition(
    uint64_t* remote_delay_report_ns, uint64_t* total_bytes_written,
    timespec* data_position) {
  return transport_->GetPresentationPosition(
      remote_delay_report_ns, total_bytes_written, data_position);
}

void HfpSourceTransport::SourceMetadataChanged(
    const source_metadata_v7_t& source_metadata) {
  transport_->SourceMetadataChanged(source_metadata);
}

void HfpSourceTransport::SinkMetadataChanged(
    const sink_metadata_v7_t& sink_metadata) {
  transport_->SinkMetadataChanged(sink_metadata);
}

void HfpSourceTransport::ResetPresentationPosition() {
  transport_->ResetPresentationPosition();
}

void HfpSourceTransport::LogBytesWritten(size_t bytes_written) {
  transport_->LogBytesProcessed(bytes_written);
}

uint8_t HfpSourceTransport::GetPendingCmd() const {
  return transport_->GetPendingCmd();
}

void HfpSourceTransport::ResetPendingCmd() { transport_->ResetPendingCmd(); }

}  // namespace hfp
}  // namespace aidl
}  // namespace audio
}  // namespace bluetooth
