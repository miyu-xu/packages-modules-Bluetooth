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
#define LOG_TAG "BTAudioA2dpEncodingTransportAIDL"

#include "a2dp_encoding_aidl.h"

#include <bluetooth/log.h>
#include <com_android_bluetooth_flags.h>

#include <vector>

#include "a2dp_provider_info.h"
#include "audio_aidl_interfaces.h"
#include "client_interface_aidl.h"
#include "transport_instance.h"

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
template <>
struct formatter<audio_usage_t> : enum_formatter<audio_usage_t> {};
template <>
struct formatter<audio_content_type_t> : enum_formatter<audio_content_type_t> {};
}  // namespace std

namespace bluetooth {
namespace audio {
namespace aidl {
namespace a2dp {

using ::aidl::android::hardware::bluetooth::audio::AudioConfiguration;
using ::aidl::android::hardware::bluetooth::audio::ChannelMode;
using ::aidl::android::hardware::bluetooth::audio::SessionType;

using ::bluetooth::audio::a2dp::Status;
using ::bluetooth::audio::a2dp::StreamCallbacks;

/********************************************************************************
* A2DP TRANSPORT INTERFACE
********************************************************************************/
A2dpEncodingTransport::A2dpEncodingTransport(SessionType session_type, std::shared_ptr<StreamCallbacks> stream_callbacks)
    : IBluetoothTransportInstance(session_type, (AudioConfiguration){}),
      a2dp_pending_cmd_(A2DP_CTRL_CMD_NONE),
      remote_delay_report_(0),
      total_bytes_read_(0),
      data_position_({}),
      stream_callbacks_(std::move(stream_callbacks)) {}

Status A2dpEncodingTransport::StartRequest(bool is_low_latency) {
  Status status = Status::FAILURE;

  // Check if a previous Start request is ongoing.
  if (a2dp_pending_cmd_ == A2DP_CTRL_CMD_START) {
    log::warn("unable to start stream: already pending");
    return Status::PENDING;
  }

  // Check if a different request is ongoing.
  if (a2dp_pending_cmd_ != A2DP_CTRL_CMD_NONE) {
    log::warn("unable to start stream: busy with pending command {}", a2dp_pending_cmd_);
    return Status::FAILURE;
  }

  log::info("is_low_latency={}", is_low_latency);

  if (stream_callbacks_) {
    status = stream_callbacks_->StartStream(is_low_latency);
  } else {
    log::error("stream_callbacks_ is null");
  }

  a2dp_pending_cmd_ = status == Status::PENDING ? A2DP_CTRL_CMD_START : A2DP_CTRL_CMD_NONE;

  return status;
}

Status A2dpEncodingTransport::SuspendRequest() {
  Status status = Status::FAILURE;

  // Check if a previous Suspend request is ongoing.
  if (a2dp_pending_cmd_ == A2DP_CTRL_CMD_SUSPEND) {
    log::warn("unable to suspend stream: already pending");
    return Status::PENDING;
  }

  // Check if a different request is ongoing.
  if (a2dp_pending_cmd_ != A2DP_CTRL_CMD_NONE) {
    log::warn("unable to suspend stream: busy with pending command {}", a2dp_pending_cmd_);
    return Status::FAILURE;
  }

  log::info("");

  if (stream_callbacks_) {
    status = stream_callbacks_->SuspendStream();
  } else {
    log::error("stream_callbacks_ is null");
  }

  a2dp_pending_cmd_ = status == Status::PENDING ? A2DP_CTRL_CMD_SUSPEND : A2DP_CTRL_CMD_NONE;

  return status;
}

void A2dpEncodingTransport::StopRequest() {
  Status status = Status::FAILURE;

  log::info("");

  if (stream_callbacks_) {
    status = stream_callbacks_->StopStream();
  } else {
    log::error("stream_callbacks_ is null");
  }

  a2dp_pending_cmd_ = status == Status::PENDING ? A2DP_CTRL_CMD_STOP : A2DP_CTRL_CMD_NONE;
}

void A2dpEncodingTransport::SetLatencyMode(LatencyMode latency_mode) {
  log::info("latency_mode={}",
            ::aidl::android::hardware::bluetooth::audio::toString(latency_mode));

  if (stream_callbacks_) {
    stream_callbacks_->SetLatencyMode(latency_mode == LatencyMode::LOW_LATENCY);
  } else {
    log::error("stream_callbacks_ is null");
  }
}

bool A2dpEncodingTransport::GetPresentationPosition(uint64_t* remote_delay_report_ns,
                                            uint64_t* total_bytes_read, timespec* data_position) {
  *remote_delay_report_ns = remote_delay_report_ * 100000u;
  *total_bytes_read = total_bytes_read_;
  *data_position = data_position_;
  log::verbose("delay={}/10ms, data={} byte(s), timestamp={}.{}s", remote_delay_report_,
               total_bytes_read_, data_position_.tv_sec, data_position_.tv_nsec);
  return true;
}

void A2dpEncodingTransport::ResetPresentationPosition() {
  remote_delay_report_ = 0;
  total_bytes_read_ = 0;
  data_position_ = {};
}

void A2dpEncodingTransport::LogBytesRead(size_t bytes_read) {
  if (bytes_read != 0) {
    total_bytes_read_ += bytes_read;
    clock_gettime(CLOCK_MONOTONIC, &data_position_);
  }
}

tA2DP_CTRL_CMD A2dpEncodingTransport::GetPendingCmd() const { return a2dp_pending_cmd_; }

void A2dpEncodingTransport::ResetPendingCmd() { a2dp_pending_cmd_ = A2DP_CTRL_CMD_NONE; }

// delay reports from AVDTP is based on 1/10 ms (100us)
void A2dpEncodingTransport::SetRemoteDelay(uint16_t delay_report) { remote_delay_report_ = delay_report; }

}  // namespace a2dp
}  // namespace aidl
}  // namespace audio
}  // namespace bluetooth
