/*
 * Copyright 2025 The Android Open Source Project
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
#define LOG_TAG "bluetooth-a2dp-ahal-hidl"

#include "a2dp_encoding_hidl_transport.h"

#include <bluetooth/log.h>

#include <vector>

#include "client_interface_hidl.h"
#include "codec_status_hidl.h"
#include "osi/include/properties.h"
#include "types/raw_address.h"

namespace std {
template <>
struct formatter<audio_usage_t> : enum_formatter<audio_usage_t> {};
template <>
struct formatter<audio_content_type_t> : enum_formatter<audio_content_type_t> {};
}  // namespace std

namespace bluetooth {
namespace audio {
namespace hidl {
namespace a2dp {

using ::bluetooth::audio::a2dp::Status;
using ::bluetooth::audio::a2dp::StreamCallbacks;

using ::bluetooth::audio::hidl::AudioConfiguration;
using ::bluetooth::audio::hidl::BluetoothAudioCtrlAck;
using ::bluetooth::audio::hidl::SessionType;

tA2DP_CTRL_CMD A2dpTransport::a2dp_pending_cmd_ = A2DP_CTRL_CMD_NONE;
uint16_t A2dpTransport::remote_delay_report_ = 0;

BluetoothAudioCtrlAck a2dp_ack_to_bt_audio_ctrl_ack(Status ack) {
  switch (ack) {
    case Status::SUCCESS:
      return BluetoothAudioCtrlAck::SUCCESS_FINISHED;
    case Status::PENDING:
      return BluetoothAudioCtrlAck::PENDING;
    case Status::UNSUPPORTED_CODEC_CONFIGURATION:
      return BluetoothAudioCtrlAck::FAILURE_UNSUPPORTED;
    case Status::UNKNOWN:
    case Status::FAILURE:
    default:
      return BluetoothAudioCtrlAck::FAILURE;
  }
}

//=============================================================================
// A2dpTransport : HIDL
//=============================================================================

A2dpTransport::A2dpTransport(SessionType sessionType, StreamCallbacks const* stream_callbacks)
    : IBluetoothSinkTransportInstance(sessionType, (AudioConfiguration){}),
      total_bytes_read_(0),
      data_position_({}),
      stream_callbacks_(stream_callbacks) {
  a2dp_pending_cmd_ = A2DP_CTRL_CMD_NONE;
  remote_delay_report_ = 0;
}

BluetoothAudioCtrlAck A2dpTransport::StartRequest() {
  Status status = Status::FAILURE;
  // Check if a previous Start request is ongoing.
  if (a2dp_pending_cmd_ == A2DP_CTRL_CMD_START) {
    log::warn("unable to start stream: already pending");
    return BluetoothAudioCtrlAck::PENDING;
  }

  // Check if a different request is ongoing.
  if (a2dp_pending_cmd_ != A2DP_CTRL_CMD_NONE) {
    log::warn("unable to start stream: busy with pending command {}", a2dp_pending_cmd_);
    return BluetoothAudioCtrlAck::FAILURE;
  }

  log::info("");
  if (stream_callbacks_) {
    status = stream_callbacks_->StartStream(false);
  } else {
    log::error("stream_callbacks_ is null");
  }
  a2dp_pending_cmd_ = status == Status::PENDING ? A2DP_CTRL_CMD_START : A2DP_CTRL_CMD_NONE;

  return a2dp_ack_to_bt_audio_ctrl_ack(status);
}

BluetoothAudioCtrlAck A2dpTransport::SuspendRequest() {
  Status status = Status::FAILURE;
  // Check if a previous Suspend request is ongoing.
  if (a2dp_pending_cmd_ == A2DP_CTRL_CMD_SUSPEND) {
    log::warn("unable to suspend stream: already pending");
    return BluetoothAudioCtrlAck::PENDING;
  }

  // Check if a different request is ongoing.
  if (a2dp_pending_cmd_ != A2DP_CTRL_CMD_NONE) {
    log::warn("unable to suspend stream: busy with pending command {}", a2dp_pending_cmd_);
    return BluetoothAudioCtrlAck::FAILURE;
  }

  log::info("");
  if (stream_callbacks_) {
    status = stream_callbacks_->SuspendStream();
  } else {
    log::error("stream_callbacks_ is null");
  }
  a2dp_pending_cmd_ = status == Status::PENDING ? A2DP_CTRL_CMD_SUSPEND : A2DP_CTRL_CMD_NONE;

  return a2dp_ack_to_bt_audio_ctrl_ack(status);
}

void A2dpTransport::StopRequest() {
  Status status = Status::FAILURE;
  log::info("");
  if (stream_callbacks_) {
    status = stream_callbacks_->SuspendStream();
  } else {
    log::error("stream_callbacks_ is null");
  }
  a2dp_pending_cmd_ = status == Status::PENDING ? A2DP_CTRL_CMD_STOP : A2DP_CTRL_CMD_NONE;
}

bool A2dpTransport::GetPresentationPosition(uint64_t* remote_delay_report_ns,
                                            uint64_t* total_bytes_read, timespec* data_position) {
  *remote_delay_report_ns = remote_delay_report_ * 100000u;
  *total_bytes_read = total_bytes_read_;
  *data_position = data_position_;
  log::verbose("delay={}/10ms, data={} byte(s), timestamp={}.{}s", remote_delay_report_,
               total_bytes_read_, data_position_.tv_sec, data_position_.tv_nsec);
  return true;
}

void A2dpTransport::MetadataChanged(const source_metadata_t& source_metadata) {
  auto track_count = source_metadata.track_count;
  auto tracks = source_metadata.tracks;
  log::verbose("{} track(s) received", track_count);
  while (track_count) {
    log::verbose("usage={}, content_type={}, gain={}", tracks->usage, tracks->content_type,
                 tracks->gain);
    --track_count;
    ++tracks;
  }
}

tA2DP_CTRL_CMD A2dpTransport::GetPendingCmd() const { return a2dp_pending_cmd_; }

void A2dpTransport::ResetPendingCmd() { a2dp_pending_cmd_ = A2DP_CTRL_CMD_NONE; }

void A2dpTransport::ResetPresentationPosition() {
  remote_delay_report_ = 0;
  total_bytes_read_ = 0;
  data_position_ = {};
}

void A2dpTransport::LogBytesRead(size_t bytes_read) {
  if (bytes_read != 0) {
    total_bytes_read_ += bytes_read;
    clock_gettime(CLOCK_MONOTONIC, &data_position_);
  }
}

// delay reports from AVDTP is based on 1/10 ms (100us)
void A2dpTransport::SetRemoteDelay(uint16_t delay_report) { remote_delay_report_ = delay_report; }

}  // namespace a2dp
}  // namespace hidl
}  // namespace audio
}  // namespace bluetooth
