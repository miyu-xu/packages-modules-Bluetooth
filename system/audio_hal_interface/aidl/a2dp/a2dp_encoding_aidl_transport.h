/*
 * Copyright 2022 The Android Open Source Project
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

#include <vector>

#include "a2dp_constants.h"
#include "a2dp_encoding.h"
#include "client_interface_aidl.h"
#include "common/message_loop_thread.h"
#include "hardware/bt_av.h"
#include "transport_instance.h"
#include "types/raw_address.h"

typedef enum {
  A2DP_CTRL_CMD_NONE,
  A2DP_CTRL_CMD_CHECK_READY,
  A2DP_CTRL_CMD_START,
  A2DP_CTRL_CMD_STOP,
  A2DP_CTRL_CMD_SUSPEND,
  A2DP_CTRL_GET_INPUT_AUDIO_CONFIG,
  A2DP_CTRL_GET_OUTPUT_AUDIO_CONFIG,
  A2DP_CTRL_SET_OUTPUT_AUDIO_CONFIG,
  A2DP_CTRL_GET_PRESENTATION_POSITION,
} tA2DP_CTRL_CMD;

namespace std {
template <>
struct formatter<tA2DP_CTRL_CMD> : enum_formatter<tA2DP_CTRL_CMD> {};
}  // namespace std

namespace bluetooth {
namespace audio {
namespace aidl {
namespace a2dp {

using ::bluetooth::audio::a2dp::Status;
using ::bluetooth::audio::aidl::a2dp::LatencyMode;

// Provide call-in APIs for the Bluetooth Audio HAL
class A2dpTransport {
public:
  A2dpTransport(bluetooth::audio::a2dp::StreamCallbacks const* stream_callbacks);

  Status StartRequest(bool is_low_latency);

  Status SuspendRequest();

  void StopRequest();

  void SetLatencyMode(LatencyMode latency_mode);

  bool GetPresentationPosition(uint64_t* remote_delay_report_ns, uint64_t* total_bytes_read,
                               timespec* data_position);

  tA2DP_CTRL_CMD GetPendingCmd() const;

  void ResetPendingCmd();

  void ResetPresentationPosition();

  void LogBytesRead(size_t bytes_read);

  // delay reports from AVDTP is based on 1/10 ms (100us)
  void SetRemoteDelay(uint16_t delay_report);

private:
  tA2DP_CTRL_CMD a2dp_pending_cmd_;
  uint16_t remote_delay_report_;
  uint64_t total_bytes_read_;
  timespec data_position_;
  ::bluetooth::audio::a2dp::StreamCallbacks const* stream_callbacks_;
};

class A2dpEncodingTransport : public ::bluetooth::audio::aidl::a2dp::IBluetoothTransportInstance {
public:
  A2dpEncodingTransport(SessionType sessionType,
                        bluetooth::audio::a2dp::StreamCallbacks const* stream_callbacks);

  ~A2dpEncodingTransport();

  Status StartRequest(bool is_low_latency) override;

  Status SuspendRequest() override;

  void StopRequest() override;

  void SetLatencyMode(LatencyMode latency_mode) override;

  bool GetPresentationPosition(uint64_t* remote_delay_report_ns, uint64_t* total_bytes_read,
                               timespec* data_position) override;

  tA2DP_CTRL_CMD GetPendingCmd() const;

  void ResetPendingCmd();

  void ResetPresentationPosition();

  void LogBytesRead(size_t bytes_read) override;

  // delay reports from AVDTP is based on 1/10 ms (100us)
  void SetRemoteDelay(uint16_t delay_report);

private:
  A2dpTransport* transport_;
};

}  // namespace a2dp
}  // namespace aidl
}  // namespace audio
}  // namespace bluetooth
