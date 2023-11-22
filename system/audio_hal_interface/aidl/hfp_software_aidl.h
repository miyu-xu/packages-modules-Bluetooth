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
#include "common/message_loop_thread.h"

namespace bluetooth {
namespace audio {
namespace aidl {
namespace hfp {

using ::aidl::android::hardware::bluetooth::audio::LatencyMode;

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

// Provide call-in APIs for the Bluetooth Audio HAL
class HfpTransport {
 public:
  HfpTransport();

  BluetoothAudioCtrlAck StartRequest();

  BluetoothAudioCtrlAck SuspendRequest();

  void StopRequest();

  void SetLatencyMode(LatencyMode latency_mode);

  bool GetPresentationPosition(uint64_t* remote_delay_report_ns,
                               uint64_t* total_bytes_read,
                               timespec* data_position);

  void SourceMetadataChanged(const source_metadata_v7_t& source_metadata);

  void SinkMetadataChanged(const sink_metadata_v7_t&);

  void ResetPresentationPosition();

  uint8_t GetPendingCmd() const;

  void ResetPendingCmd();

  void LogBytesProcessed(size_t bytes_read);

 private:
  tHFP_CTRL_CMD hfp_pending_cmd_;
};

// Sink transport implementation
class HfpSinkTransport
    : public ::bluetooth::audio::aidl::IBluetoothSinkTransportInstance,
      public HfpTransport {
 public:
  HfpSinkTransport(SessionType sessionType);

  ~HfpSinkTransport();

  BluetoothAudioCtrlAck StartRequest(bool is_low_latency) {
    return HfpTransport::StartRequest();
  }

  BluetoothAudioCtrlAck SuspendRequest() {
    return HfpTransport::SuspendRequest();
  }

  void StopRequest() { HfpTransport::StopRequest(); }

  void SetLatencyMode(LatencyMode latency_mode) {
    HfpTransport::SetLatencyMode(latency_mode);
  }

  bool GetPresentationPosition(uint64_t* remote_delay_report_ns,
                               uint64_t* total_bytes_read,
                               timespec* data_position) {
    return HfpTransport::GetPresentationPosition(
        remote_delay_report_ns, total_bytes_read, data_position);
  }

  void SourceMetadataChanged(const source_metadata_v7_t& source_metadata) {
    HfpTransport::SourceMetadataChanged(source_metadata);
  }

  void SinkMetadataChanged(const sink_metadata_v7_t& sink_metadata) {
    HfpTransport::SinkMetadataChanged(sink_metadata);
  }

  void ResetPresentationPosition() {
    HfpTransport::ResetPresentationPosition();
  }

  void LogBytesRead(size_t bytes_read) override;

  static inline HfpSinkTransport* instance_ = nullptr;
  static inline BluetoothAudioSinkClientInterface* software_hal_interface =
      nullptr;
  static inline BluetoothAudioSinkClientInterface* offloading_hal_interface =
      nullptr;
  static inline BluetoothAudioSinkClientInterface* active_hal_interface =
      nullptr;
};

class HfpSourceTransport
    : public ::bluetooth::audio::aidl::IBluetoothSourceTransportInstance,
      public HfpTransport {
 public:
  HfpSourceTransport(SessionType sessionType);

  ~HfpSourceTransport();

  BluetoothAudioCtrlAck StartRequest(bool is_low_latency) {
    return HfpTransport::StartRequest();
  }

  BluetoothAudioCtrlAck SuspendRequest() {
    return HfpTransport::SuspendRequest();
  }

  void StopRequest() { HfpTransport::StopRequest(); }

  void SetLatencyMode(LatencyMode latency_mode) {
    HfpTransport::SetLatencyMode(latency_mode);
  }

  bool GetPresentationPosition(uint64_t* remote_delay_report_ns,
                               uint64_t* total_bytes_read,
                               timespec* data_position) {
    return HfpTransport::GetPresentationPosition(
        remote_delay_report_ns, total_bytes_read, data_position);
  }

  void SourceMetadataChanged(const source_metadata_v7_t& source_metadata) {
    HfpTransport::SourceMetadataChanged(source_metadata);
  }

  void SinkMetadataChanged(const sink_metadata_v7_t& sink_metadata) {
    HfpTransport::SinkMetadataChanged(sink_metadata);
  }

  void ResetPresentationPosition() {
    HfpTransport::ResetPresentationPosition();
  }

  void LogBytesWritten(size_t bytes_written) override;

  static inline HfpSourceTransport* instance_ = nullptr;
  static inline BluetoothAudioSourceClientInterface* software_hal_interface =
      nullptr;
  static inline BluetoothAudioSourceClientInterface* offloading_hal_interface =
      nullptr;
  static inline BluetoothAudioSourceClientInterface* active_hal_interface =
      nullptr;
};

}  // namespace hfp
}  // namespace aidl
}  // namespace audio
}  // namespace bluetooth
