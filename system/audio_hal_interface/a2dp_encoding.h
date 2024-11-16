/*
 * Copyright 2019 The Android Open Source Project
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

#include <iomanip>
#include <sstream>
#include <vector>

#include "a2dp_codec_api.h"
#include "a2dp_constants.h"
#include "avdt_api.h"
#include "common/message_loop_thread.h"
#include "hardware/bt_av.h"

namespace bluetooth {
namespace audio {
namespace a2dp {

/// Selects the A2DP session type.
enum class StreamDirection {
  INPUT,  ///< Sink decoding.
  OUTPUT, ///< Source enconding.
};

/// Loosely copied after the definition from the Bluetooth Audio interface:
/// audio/aidl/android/hardware/bluetooth/audio/BluetoothAudioStatus.aidl
enum class Status {
  SUCCESS = 0,
  UNKNOWN,
  UNSUPPORTED_CODEC_CONFIGURATION,
  FAILURE,
  PENDING,
};

/// Loosely copied after the definition from the Bluetooth Audio interface:
/// audio/aidl/android/hardware/bluetooth/audio/IBluetoothAudioPort.aidl
///
/// Implements callbacks for the BT Audio HAL to start, suspend and configure
/// the audio stream. Completion of the requested operation is indicated
/// by the methods ack_stream_started, ack_stream_suspended.
///
/// The callbacks are always invoked from one of the binder threads.
class StreamCallbacks {
public:
  virtual ~StreamCallbacks() {}
  virtual Status StartStream(bool /*low_latency*/) const {
    return Status::FAILURE;
  }
  virtual Status SuspendStream() const { return Status::FAILURE; }
  virtual Status StopStream() const { return SuspendStream(); }
  virtual Status SetLatencyMode(bool /*low_latency*/) const {
    return Status::FAILURE;
  }

  // Stream start confirmation. The host stack shall reject the start
  // request if the HAL returns an error.
  virtual void StreamStarted(Status /*status*/) const { }

  // Stream suspend confirmation. The host stack shall reject the suspend
  // request if the HAL returns an error.
  virtual void StreamSuspended(Status /*status*/) const { }
};

bool update_codec_offloading_capabilities(
        const std::vector<btav_a2dp_codec_config_t>& framework_preference,
        bool supports_a2dp_hw_offload_v2);

// Check if new bluetooth_audio is enabled
bool is_hal_enabled();

// Check if new bluetooth_audio is running with offloading encoders
bool is_hal_offloading();

// Initialize BluetoothAudio HAL: openProvider
bool init(bluetooth::common::MessageLoopThread* message_loop,
          StreamCallbacks const* strean_callbacks,
          bool offload_enabled);

// Clean up BluetoothAudio HAL
void cleanup();

// Set up the codec into BluetoothAudio HAL
bool setup_codec(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                 int preferred_encoding_interval_us);

// Set low latency buffer mode allowed or disallowed
void set_audio_low_latency_mode_allowed(bool allowed);

// Start an audio session with the BluetoothAudio HAL.
// The audio configuration is fixed for the duration of the session.
// Only one stream may be active at any time, and the stream direction
// becomes implicit for all stream operations for the duration of the session.
void start_session(StreamDirection direction);
void end_session();

// Send stream indications to the BluetoothAudio HAL.
// Stream confirmations are sent back through StreamCallbacks::StreamStarted,
// StreamCallbacks::StreamSuspended.
void start_stream(Status status);
void suspend_stream(Status status);

void stream_started(Status status);
void stream_suspended(Status status);

// Read from the FMQ of BluetoothAudio HAL
size_t read(uint8_t* p_buf, uint32_t len);

// Update A2DP delay report to BluetoothAudio HAL
void set_remote_delay(uint16_t delay_report);

// Check whether OPUS is supported
bool is_opus_supported();

/// Information about a codec supported through offload codec extensibility.
/// The encoding / decoding capacity is implicit in the parameter of
/// getSupportedCodecs().
struct CodecInfo {
  bluetooth::a2dp::CodecId id;
  std::string name;
  /// Media Codec Capabilities, including the Media Codec Type and
  /// optional Vendor Codec Identifier.
  uint8_t capabilities[AVDT_CODEC_SIZE];
  btav_a2dp_codec_config_t parameters;
};

/// Return the list of supported codecs for the selected stream direction.
std::vector<CodecInfo> getSupportedCodecs(StreamDirection direction);

// Query the codec selection fromt the audio HAL.
// The HAL is expected to pick the best audio configuration based on the
// discovered remote SEPs.
std::optional<a2dp_configuration> getA2dpConfiguration(
        StreamDirection direction,
        RawAddress peer_address, std::vector<a2dp_remote_capabilities> const& remote_seps,
        btav_a2dp_codec_config_t const& user_preferences);

struct a2dp_configuration {
  int remote_seid;
  uint8_t codec_config[AVDT_CODEC_SIZE];
  btav_a2dp_codec_config_t codec_parameters;
  std::vector<uint8_t> vendor_specific_parameters;

  inline std::string toString() const {
    std::ostringstream os;
    os << "A2dpConfiguration{";
    os << "remote_seid: " << remote_seid;
    os << ", codec_index: " << codec_parameters.codec_type;
    os << ", codec_config: {";
    for (int i = 0; i < AVDT_CODEC_SIZE; i++) {
      os << "0x" << std::hex << std::setw(2) << std::setfill('0')
         << static_cast<int>(codec_config[i]);
      if (i != AVDT_CODEC_SIZE - 1) {
        os << ",";
      }
    }
    os << "}";
    os << "}";
    return os.str();
  }
};

struct a2dp_remote_capabilities {
  int seid;
  uint8_t const* capabilities;

  inline std::string toString() const {
    std::ostringstream os;
    os << "A2dpRemoteCapabilities{";
    os << "seid: " << seid;
    os << ", capabilities: {";
    if (capabilities != nullptr) {
      for (int i = 0; i < AVDT_CODEC_SIZE; i++) {
        os << "0x" << std::hex << std::setw(2) << std::setfill('0')
           << static_cast<int>(capabilities[i]);
        if (i != AVDT_CODEC_SIZE - 1) {
          os << ",";
        }
      }
    }
    os << "}";
    os << "}";
    return os.str();
  }
};

// Query the codec parameters from the audio HAL.
// The HAL is expected to parse the codec configuration
// received from the peer and decide whether accept
// the it or not.
tA2DP_STATUS parseA2dpConfiguration(StreamDirection direction,
                                    bluetooth::a2dp::CodecId codec_id,
                                    const uint8_t* codec_info,
                                    btav_a2dp_codec_config_t* codec_parameters,
                                    std::vector<uint8_t>* vendor_specific_parameters);

}  // namespace a2dp
}  // namespace audio
}  // namespace bluetooth

namespace fmt {
template <>
struct formatter<::bluetooth::audio::a2dp::Status>
    : enum_formatter<::bluetooth::audio::a2dp::Status> {};
}  // namespace fmt
