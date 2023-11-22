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

#include <cstdint>

#include "client_interface_aidl.h"

namespace bluetooth {
namespace audio {
namespace aidl {
namespace hfp {

typedef enum {
  HFP_CTRL_ACK_SUCCESS,
  HFP_CTRL_ACK_FAILURE,
  HFP_CTRL_ACK_INCALL_FAILURE, /* Failure when in Call*/
  HFP_CTRL_ACK_UNSUPPORTED,
  HFP_CTRL_ACK_PENDING,
  HFP_CTRL_ACK_DISCONNECT_IN_PROGRESS,
} tHFP_CTRL_ACK;

typedef enum {
  HFP_CTRL_CMD_NONE,
  HFP_CTRL_CMD_CHECK_READY,
  HFP_CTRL_CMD_START,
  HFP_CTRL_CMD_STOP,
  HFP_CTRL_CMD_SUSPEND,
  HFP_CTRL_GET_INPUT_AUDIO_CONFIG,
  HFP_CTRL_GET_OUTPUT_AUDIO_CONFIG,
  HFP_CTRL_SET_OUTPUT_AUDIO_CONFIG,
  HFP_CTRL_GET_PRESENTATION_POSITION,
} tHFP_CTRL_CMD;

namespace {

BluetoothAudioCtrlAck hfp_ack_to_bt_audio_ctrl_ack(tHFP_CTRL_ACK ack);

// Provide call-in APIs for the Bluetooth Audio HAL
class HfpTransport
    : public ::bluetooth::audio::aidl::IBluetoothSinkTransportInstance {
 public:
  HfpTransport(SessionType sessionType);

  BluetoothAudioCtrlAck StartRequest(bool is_low_latency) override;

  BluetoothAudioCtrlAck SuspendRequest() override;

  void StopRequest() override;

  void SetLowLatency(bool is_low_latency) override;

  bool GetPresentationPosition(uint64_t* remote_delay_report_ns,
                               uint64_t* total_bytes_read,
                               timespec* data_position) override;

  void SourceMetadataChanged(const source_metadata_v7_t& source_metadata);

  void SinkMetadataChanged(const sink_metadata_v7_t&) override;

  uint8_t GetPendingCmd() const;

  void ResetPendingCmd();

  void ResetPresentationPosition();

  void LogBytesRead(size_t bytes_read) override;

  // delay reports from AVDTP is based on 1/10 ms (100us)
  void SetRemoteDelay(uint16_t delay_report);

 private:
  static tHFP_CTRL_CMD hfp_pending_cmd_;
  uint64_t total_bytes_read_;
  timespec data_position_;
};
}  // namespace

}  // namespace hfp
}  // namespace aidl
}  // namespace audio
}  // namespace bluetooth
