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

#pragma once

#include <iomanip>
#include <sstream>
#include <vector>

#include "a2dp_codec_api.h"
#include "a2dp_constants.h"
#include "aidl/a2dp/a2dp_provider_info.h"
#include "hardware/bt_av.h"

namespace bluetooth {
namespace audio {
namespace a2dp {

//=============================================================================
// IA2dpEncoding
//=============================================================================

// Abstract interface for A2DP BluetoothAudio HAL communication.
class IA2dpEncoding {
public:
  virtual ~IA2dpEncoding() = default;

  // Update A2DP delay report to BluetoothAudio HAL
  virtual void SetRemoteDelay(uint16_t delay_report) = 0;

  // Start session in BluetoothAudio HAL
  virtual void StartSession() = 0;

  // Stop session in BluetoothAudio HAL
  virtual void StopSession() = 0;

  // Confirm that the stream started to BluetoothAudio HAL
  virtual void ConfirmStreamStarted(Status status) = 0;

  // Confirm that the stream suspended to BluetoothAudio HAL
  virtual void ConfirmStreamSuspended(Status status) = 0;

  // Update the audio codec configuration to BluetoothAudio HAL
  virtual bool UpdateAudioConfigToHal(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                                      int preferred_encoding_interval_us) = 0;

  // Check if codec is supported by the hardware offloader
  virtual bool IsCodecSupportedByHardwareOffload(A2dpCodecConfig* a2dp_config,
                                                 uint16_t peer_mtu) = 0;

  // Update A2DP Low Latency Mode to BluetoothAudio HAL
  virtual void SetLowLatencyMode([[maybe_unused]] bool allowed) {}

  // Read from the FMQ of BluetoothAudio HAL
  virtual size_t Read([[maybe_unused]] uint8_t* p_buf, [[maybe_unused]] uint32_t len) { return 0; }

  // Lookup the codec info in the list of supported offloaded sink codecs.
  virtual std::optional<btav_a2dp_codec_index_t> SinkCodecIndex(
          [[maybe_unused]] const uint8_t* p_codec_info) {
    return std::nullopt;
  }

  // Lookup the codec info in the list of supported offloaded source codecs.
  virtual std::optional<btav_a2dp_codec_index_t> SourceCodecIndex(
          [[maybe_unused]] const uint8_t* p_codec_info) {
    return std::nullopt;
  }

  // Return the name of the codec which is assigned to the input index.
  // The codec index must be in the ranges
  // BTAV_A2DP_CODEC_INDEX_SINK_EXT_MIN..BTAV_A2DP_CODEC_INDEX_SINK_EXT_MAX or
  // BTAV_A2DP_CODEC_INDEX_SOURCE_EXT_MIN..BTAV_A2DP_CODEC_INDEX_SOURCE_EXT_MAX.
  // Returns nullopt if the codec_index is not assigned or codec extensibility
  // is not supported or enabled.
  virtual std::optional<const char*> CodecIndexStr(
          [[maybe_unused]] btav_a2dp_codec_index_t codec_index) {
    return std::nullopt;
  }

  // Return true if the codec is supported for the session type
  // A2DP_HARDWARE_ENCODING_DATAPATH or A2DP_HARDWARE_DECODING_DATAPATH.
  virtual bool SupportsCodec([[maybe_unused]] btav_a2dp_codec_index_t codec_index) { return false; }

  // Return the A2DP capabilities for the selected codec.
  // `codec_info` returns the OTA codec capabilities, `codec_config`
  // returns the supported capabilities in a generic format.
  virtual bool CodecInfo([[maybe_unused]] btav_a2dp_codec_index_t codec_index,
                         [[maybe_unused]] bluetooth::a2dp::CodecId* codec_id,
                         [[maybe_unused]] uint8_t* codec_info,
                         [[maybe_unused]] btav_a2dp_codec_config_t* codec_config) {
    return false;
  }

  // Query the codec selection fromt the audio HAL.
  // The HAL is expected to pick the best audio configuration based on the
  // discovered remote SEPs.
  virtual std::optional<::bluetooth::audio::a2dp::provider::a2dp_configuration>
  GetA2dpConfiguration(
          [[maybe_unused]] RawAddress peer_address,
          [[maybe_unused]] std::vector<
                  ::bluetooth::audio::a2dp::provider::a2dp_remote_capabilities> const& remote_seps,
          [[maybe_unused]] btav_a2dp_codec_config_t const& user_preferences) {
    return std::nullopt;
  }

  // Query the codec parameters from the audio HAL.
  // The HAL is expected to parse the codec configuration
  // received from the peer and decide whether accept
  // the it or not.
  virtual tA2DP_STATUS ParseA2dpConfiguration(
          [[maybe_unused]] btav_a2dp_codec_index_t codec_index,
          [[maybe_unused]] const uint8_t* codec_info,
          [[maybe_unused]] btav_a2dp_codec_config_t* codec_parameters,
          [[maybe_unused]] std::vector<uint8_t>* vendor_specific_parameters) {
    return A2DP_FAIL;
  }
};

}  // namespace a2dp
}  // namespace audio
}  // namespace bluetooth