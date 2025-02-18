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
#include "a2dp_common_encoding_interface.h"
#include "a2dp_constants.h"
#include "aidl/a2dp/a2dp_provider_info.h"
#include "avdt_api.h"
#include "common/message_loop_thread.h"
#include "hardware/bt_av.h"

namespace bluetooth {
namespace audio {
namespace a2dp {

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
  virtual ~StreamCallbacks() = default;
  virtual Status StartStream(bool /*low_latency*/) const { return Status::FAILURE; }
  virtual Status SuspendStream() const { return Status::FAILURE; }
  virtual Status StopStream() const { return SuspendStream(); }
  virtual Status SetLatencyMode(bool /*low_latency*/) const { return Status::FAILURE; }
};

// Check if new bluetooth_audio is enabled
bool is_hal_enabled();

// Check if new bluetooth_audio is running with offloading encoders
bool is_hal_offloading();

// Initialize BluetoothAudio HAL: openProvider
bool init(bluetooth::common::MessageLoopThread* message_loop,
          StreamCallbacks const* stream_callbacks, bool offload_enabled,
          std::unique_ptr<::bluetooth::audio::aidl::a2dp::ProviderInfo> provider_info);

// Clean up BluetoothAudio HAL
void cleanup();

// Set up the codec into BluetoothAudio HAL
bool setup_codec(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                 int preferred_encoding_interval_us);

// Set low latency buffer mode allowed or disallowed
void set_audio_low_latency_mode_allowed(bool allowed);

// Send command to the BluetoothAudio HAL: StartSession, EndSession,
// StreamStarted, StreamSuspended
void start_session();
void end_session();
void ack_stream_started(Status status);
void ack_stream_suspended(Status status);

// Read from the FMQ of BluetoothAudio HAL
size_t read(uint8_t* p_buf, uint32_t len);

// Update A2DP delay report to BluetoothAudio HAL
void set_remote_delay(uint16_t delay_report);

// Definitions for A2DP hardware offload codec extensibility.
namespace provider {

// Lookup the codec info in the list of supported offloaded sink codecs.
std::optional<btav_a2dp_codec_index_t> sink_codec_index(const uint8_t* p_codec_info);

// Lookup the codec info in the list of supported offloaded source codecs.
std::optional<btav_a2dp_codec_index_t> source_codec_index(const uint8_t* p_codec_info);

// Return the name of the codec which is assigned to the input index.
// The codec index must be in the ranges
// BTAV_A2DP_CODEC_INDEX_SINK_EXT_MIN..BTAV_A2DP_CODEC_INDEX_SINK_EXT_MAX or
// BTAV_A2DP_CODEC_INDEX_SOURCE_EXT_MIN..BTAV_A2DP_CODEC_INDEX_SOURCE_EXT_MAX.
// Returns nullopt if the codec_index is not assigned or codec extensibility
// is not supported or enabled.
std::optional<const char*> codec_index_str(btav_a2dp_codec_index_t codec_index);

// Return true if the codec is supported for the session type
// A2DP_HARDWARE_ENCODING_DATAPATH or A2DP_HARDWARE_DECODING_DATAPATH.
bool supports_codec(btav_a2dp_codec_index_t codec_index);

// Return the A2DP capabilities for the selected codec.
// `codec_info` returns the OTA codec capabilities, `codec_config`
// returns the supported capabilities in a generic format.
bool codec_info(btav_a2dp_codec_index_t codec_index, bluetooth::a2dp::CodecId* codec_id,
                uint8_t* codec_info, btav_a2dp_codec_config_t* codec_config);

// Query the codec selection fromt the audio HAL.
// The HAL is expected to pick the best audio configuration based on the
// discovered remote SEPs.
std::optional<a2dp_configuration> get_a2dp_configuration(
        RawAddress peer_address, std::vector<a2dp_remote_capabilities> const& remote_seps,
        btav_a2dp_codec_config_t const& user_preferences);

// Query the codec parameters from the audio HAL.
// The HAL is expected to parse the codec configuration
// received from the peer and decide whether accept
// the it or not.
tA2DP_STATUS parse_a2dp_configuration(btav_a2dp_codec_index_t codec_index,
                                      const uint8_t* codec_info,
                                      btav_a2dp_codec_config_t* codec_parameters,
                                      std::vector<uint8_t>* vendor_specific_parameters);

}  // namespace provider

// Interface for A2DP BluetoothAudio HAL communication.
class A2dpClientInterface {
public:
  A2dpClientInterface(std::unique_ptr<IA2dpEncoding> software_encoding,
                      std::unique_ptr<IA2dpEncoding> offload_encoding);

  // Cleanup interface
  void Cleanup(bool update_only);

  // Update A2DP delay report to BluetoothAudio HAL
  void SetRemoteDelay(uint16_t delay_report);

  // Update A2DP Low Latency Mode to BluetoothAudio HAL
  void SetLowLatencyMode(bool allowed);

  // Start session in BluetoothAudio HAL
  void StartSession();

  // Stop session in BluetoothAudio HAL
  void StopSession();

  // Confirm that the stream started to BluetoothAudio HAL
  void ConfirmStreamStarted(Status status);

  // Confirm that the stream suspended to BluetoothAudio HAL
  void ConfirmStreamSuspended(Status status);

  // Update the audio codec configuration to BluetoothAudio HAL
  bool UpdateAudioConfigToHal(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                              int preferred_encoding_interval_us);

  // Read from the FMQ of BluetoothAudio HAL
  size_t Read(uint8_t* p_buf, uint32_t len);

  // Lookup the codec info in the list of supported offloaded sink codecs.
  std::optional<btav_a2dp_codec_index_t> SinkCodecIndex(const uint8_t* p_codec_info);

  // Lookup the codec info in the list of supported offloaded source codecs.
  std::optional<btav_a2dp_codec_index_t> SourceCodecIndex(const uint8_t* p_codec_info);

  // Return the name of the codec which is assigned to the input index.
  // The codec index must be in the ranges
  // BTAV_A2DP_CODEC_INDEX_SINK_EXT_MIN..BTAV_A2DP_CODEC_INDEX_SINK_EXT_MAX or
  // BTAV_A2DP_CODEC_INDEX_SOURCE_EXT_MIN..BTAV_A2DP_CODEC_INDEX_SOURCE_EXT_MAX.
  // Returns nullopt if the codec_index is not assigned or codec extensibility
  // is not supported or enabled.
  std::optional<const char*> CodecIndexStr(btav_a2dp_codec_index_t codec_index);

  // Return true if the codec is supported for the session type
  // A2DP_HARDWARE_ENCODING_DATAPATH or A2DP_HARDWARE_DECODING_DATAPATH.
  bool SupportsCodec(btav_a2dp_codec_index_t codec_index);

  // Return the A2DP capabilities for the selected codec.
  // `codec_info` returns the OTA codec capabilities, `codec_config`
  // returns the supported capabilities in a generic format.
  bool CodecInfo(btav_a2dp_codec_index_t codec_index, bluetooth::a2dp::CodecId* codec_id,
                 uint8_t* codec_info, btav_a2dp_codec_config_t* codec_config);

  // Query the codec selection fromt the audio HAL.
  // The HAL is expected to pick the best audio configuration based on the
  // discovered remote SEPs.
  std::optional<::bluetooth::audio::a2dp::provider::a2dp_configuration> GetA2dpConfiguration(
          RawAddress peer_address,
          std::vector<::bluetooth::audio::a2dp::provider::a2dp_remote_capabilities> const&
                  remote_seps,
          btav_a2dp_codec_config_t const& user_preferences);

  // Query the codec parameters from the audio HAL.
  // The HAL is expected to parse the codec configuration
  // received from the peer and decide whether accept
  // the it or not.
  tA2DP_STATUS ParseA2dpConfiguration(btav_a2dp_codec_index_t codec_index,
                                      const uint8_t* codec_info,
                                      btav_a2dp_codec_config_t* codec_parameters,
                                      std::vector<uint8_t>* vendor_specific_parameters);

  // Check if hardware offload is configured and available
  bool IsOffloadAvailable();

  // Check if hardware offload is currently enabled
  bool IsOffloadEnabled();

  // Check if BluetoothAudio HAL is currently and enabled
  bool IsEnabled();

  // Save the value if the remote reports its delay before this interface is
  // initialized
  static uint16_t remote_delay;
  // Save the value if the stack reports low latency before this interface is
  // initialized
  static bool is_low_latency_mode_allowed;

private:
  // Switch currently active interface to software encoding
  void SwitchToSoftwareEncoding();

  // Switch currently active interface to hardware offload encoding
  void SwitchToHardwareOffloadEncoding();

  // Check if codec is supported by the hardware offloader
  bool IsCodecSupportedByHardwareOffload(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu);

  std::unique_ptr<IA2dpEncoding> software_encoding_ = nullptr;
  std::unique_ptr<IA2dpEncoding> offload_encoding_ = nullptr;
  IA2dpEncoding* current_encoding_interface_ = nullptr;
};

}  // namespace a2dp
}  // namespace audio
}  // namespace bluetooth

namespace std {
template <>
struct formatter<::bluetooth::audio::a2dp::Status>
    : enum_formatter<::bluetooth::audio::a2dp::Status> {};
}  // namespace std
