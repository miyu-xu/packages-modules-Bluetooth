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
#include "a2dp_provider_info.h"
#include "client_interface_aidl.h"
#include "common/message_loop_thread.h"
#include "hardware/bt_av.h"
#include "transport_instance.h"
#include "types/raw_address.h"

namespace bluetooth {
namespace audio {
namespace aidl {
namespace a2dp {

using ::bluetooth::audio::a2dp::Status;

bool update_codec_offloading_capabilities(
        const std::vector<btav_a2dp_codec_config_t>& framework_preference,
        bool supports_a2dp_hw_offload_v2);

/***
 * Check if new bluetooth_audio is enabled
 ***/
bool is_hal_enabled();

/***
 * Check if new bluetooth_audio is running with offloading encoders
 ***/
bool is_hal_offloading();

/***
 * Initialize BluetoothAudio HAL: openProvider
 ***/
bool init(bluetooth::common::MessageLoopThread* message_loop,
          ::bluetooth::audio::a2dp::StreamCallbacks const* stream_callbacks, bool offload_enabled);

/***
 * Clean up BluetoothAudio HAL
 ***/
void cleanup();

/***
 * Set up the codec into BluetoothAudio HAL
 ***/
bool setup_codec(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                 int preferred_encoding_interval_us);

/***
 * Send command to the BluetoothAudio HAL: StartSession, EndSession,
 * StreamStarted, StreamSuspended
 ***/
void start_session();
void end_session();
void ack_stream_started(::bluetooth::audio::a2dp::Status status);
void ack_stream_suspended(::bluetooth::audio::a2dp::Status status);

/***
 * Read from the FMQ of BluetoothAudio HAL
 ***/
size_t read(uint8_t* p_buf, uint32_t len);

/***
 * Update A2DP delay report to BluetoothAudio HAL
 ***/
void set_remote_delay(uint16_t delay_report);

/***
 * Set low latency buffer mode allowed or disallowed
 ***/
void set_low_latency_mode_allowed(bool allowed);

namespace provider {

/***
 * Lookup the codec info in the list of supported offloaded sink codecs.
 * Should not be called before update_codec_offloading_capabilities.
 ***/
std::optional<btav_a2dp_codec_index_t> sink_codec_index(const uint8_t* p_codec_info);

/***
 * Lookup the codec info in the list of supported offloaded source codecs.
 * Should not be called before update_codec_offloading_capabilities.
 ***/
std::optional<btav_a2dp_codec_index_t> source_codec_index(const uint8_t* p_codec_info);

/***
 * Return the name of the codec which is assigned to the input index.
 * The codec index must be in the ranges
 * BTAV_A2DP_CODEC_INDEX_SINK_EXT_MIN..BTAV_A2DP_CODEC_INDEX_SINK_EXT_MAX or
 * BTAV_A2DP_CODEC_INDEX_SOURCE_EXT_MIN..BTAV_A2DP_CODEC_INDEX_SOURCE_EXT_MAX.
 * Returns nullopt if the codec_index is not assigned or codec extensibility
 * is not supported or enabled.
 * Should not be called before update_codec_offloading_capabilities.
 ***/
std::optional<const char*> codec_index_str(btav_a2dp_codec_index_t codec_index);

/***
 * Return true if the codec is supported for the session type
 * A2DP_HARDWARE_ENCODING_DATAPATH or A2DP_HARDWARE_DECODING_DATAPATH.
 ***/
bool supports_codec(btav_a2dp_codec_index_t codec_index);

/***
 * Return the A2DP capabilities for the selected codec.
 ***/
bool codec_info(btav_a2dp_codec_index_t codec_index, bluetooth::a2dp::CodecId* codec_id,
                uint8_t* codec_info, btav_a2dp_codec_config_t* codec_config);

/***
 * Query the codec selection fromt the audio HAL.
 * The HAL is expected to pick the best audio configuration based on the
 * discovered remote SEPs.
 ***/
std::optional<::bluetooth::audio::a2dp::provider::a2dp_configuration> get_a2dp_configuration(
        RawAddress peer_address,
        std::vector<::bluetooth::audio::a2dp::provider::a2dp_remote_capabilities> const&
                remote_seps,
        btav_a2dp_codec_config_t const& user_preferences);

/***
 * Query the codec parameters from the audio HAL.
 * The HAL is expected to parse the codec configuration
 * received from the peer and decide whether accept
 * the it or not.
 ***/
tA2DP_STATUS parse_a2dp_configuration(btav_a2dp_codec_index_t codec_index,
                                      const uint8_t* codec_info,
                                      btav_a2dp_codec_config_t* codec_parameters,
                                      std::vector<uint8_t>* vendor_specific_parameters);
}  // namespace provider

class BaseEncoding {
public:
  BaseEncoding(::bluetooth::audio::aidl::a2dp::BluetoothAudioClientInterface* audio_interface);
  virtual ~BaseEncoding();
  virtual void SetRemoteDelay(uint16_t delay_report);
  virtual void SetLowLatencyMode(bool allowed);
  virtual void StartSession();
  virtual void StopSession();
  virtual void ConfirmStreamStarted(Status status);
  virtual void ConfirmStreamSuspended(Status status);
  virtual void Cleanup() {}
  virtual bool UpdateAudioConfigToHal([[maybe_unused]] A2dpCodecConfig* a2dp_config,
                                      [[maybe_unused]] uint16_t peer_mtu,
                                      [[maybe_unused]] int preferred_encoding_interval_us) {
    return false;
  }
  virtual size_t Read([[maybe_unused]] uint8_t* p_buf, [[maybe_unused]] uint32_t len) { return 0; }
  virtual std::optional<btav_a2dp_codec_index_t> SinkCodecIndex(
          [[maybe_unused]] const uint8_t* p_codec_info) {
    return std::nullopt;
  }
  virtual std::optional<btav_a2dp_codec_index_t> SourceCodecIndex(
          [[maybe_unused]] const uint8_t* p_codec_info) {
    return std::nullopt;
  }
  virtual std::optional<const char*> CodecIndexStr(
          [[maybe_unused]] btav_a2dp_codec_index_t codec_index) {
    return std::nullopt;
  }
  virtual bool SupportsCodec([[maybe_unused]] btav_a2dp_codec_index_t codec_index) { return false; }
  virtual bool CodecInfo([[maybe_unused]] btav_a2dp_codec_index_t codec_index,
                         [[maybe_unused]] bluetooth::a2dp::CodecId* codec_id,
                         [[maybe_unused]] uint8_t* codec_info,
                         [[maybe_unused]] btav_a2dp_codec_config_t* codec_config) {
    return false;
  }
  virtual std::optional<::bluetooth::audio::a2dp::provider::a2dp_configuration>
  GetA2dpConfiguration(
          [[maybe_unused]] RawAddress peer_address,
          [[maybe_unused]] std::vector<
                  ::bluetooth::audio::a2dp::provider::a2dp_remote_capabilities> const& remote_seps,
          [[maybe_unused]] btav_a2dp_codec_config_t const& user_preferences) {
    return std::nullopt;
  }
  virtual tA2DP_STATUS ParseA2dpConfiguration(
          [[maybe_unused]] btav_a2dp_codec_index_t codec_index,
          [[maybe_unused]] const uint8_t* codec_info,
          [[maybe_unused]] btav_a2dp_codec_config_t* codec_parameters,
          [[maybe_unused]] std::vector<uint8_t>* vendor_specific_parameters) {
    return A2DP_FAIL;
  }

protected:
  ::bluetooth::audio::aidl::a2dp::BluetoothAudioClientInterface* interface_ = nullptr;
};

class SoftwareEncoding : public BaseEncoding {
public:
  SoftwareEncoding(::bluetooth::audio::aidl::a2dp::BluetoothAudioClientInterface* audio_interface);
  virtual ~SoftwareEncoding() = default;
  bool UpdateAudioConfigToHal(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                              int preferred_encoding_interval_us) override;
  size_t Read(uint8_t* p_buf, uint32_t len) override;
};

class HardwareOffloadEncoding : public BaseEncoding {
public:
  HardwareOffloadEncoding(
          ::bluetooth::audio::aidl::a2dp::BluetoothAudioClientInterface* audio_interface,
          std::unique_ptr<::bluetooth::audio::aidl::a2dp::ProviderInfo> provider_info);
  virtual ~HardwareOffloadEncoding() = default;
  bool UpdateAudioConfigToHal(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                              int preferred_encoding_interval_us) override;
  std::optional<btav_a2dp_codec_index_t> SinkCodecIndex(const uint8_t* p_codec_info) override;
  std::optional<btav_a2dp_codec_index_t> SourceCodecIndex(const uint8_t* p_codec_info) override;
  std::optional<const char*> CodecIndexStr(btav_a2dp_codec_index_t codec_index);
  bool SupportsCodec(btav_a2dp_codec_index_t codec_index) override;
  bool CodecInfo(btav_a2dp_codec_index_t codec_index, bluetooth::a2dp::CodecId* codec_id,
                 uint8_t* codec_info, btav_a2dp_codec_config_t* codec_config) override;
  std::optional<::bluetooth::audio::a2dp::provider::a2dp_configuration> GetA2dpConfiguration(
          RawAddress peer_address,
          std::vector<::bluetooth::audio::a2dp::provider::a2dp_remote_capabilities> const&
                  remote_seps,
          btav_a2dp_codec_config_t const& user_preferences) override;
  tA2DP_STATUS ParseA2dpConfiguration(btav_a2dp_codec_index_t codec_index,
                                      const uint8_t* codec_info,
                                      btav_a2dp_codec_config_t* codec_parameters,
                                      std::vector<uint8_t>* vendor_specific_parameters) override;

private:
  // ProviderInfo for A2DP hardware offload encoding and decoding data paths,
  // if supported by the HAL and enabled. nullptr if not supported
  // or disabled.
  std::unique_ptr<::bluetooth::audio::aidl::a2dp::ProviderInfo> provider_info_;
};

class A2dpClientInterface {
public:
  A2dpClientInterface(std::unique_ptr<SoftwareEncoding> software_encoding,
                      std::unique_ptr<HardwareOffloadEncoding> offload_encoding);
  void Cleanup(bool update_only);
  void SetRemoteDelay(uint16_t delay_report);
  void SetLowLatencyMode(bool allowed);
  void StartSession();
  void StopSession();
  void ConfirmStreamStarted(Status status);
  void ConfirmStreamSuspended(Status status);
  bool UpdateAudioConfigToHal(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                              [[maybe_unused]] int preferred_encoding_interval_us);
  size_t Read(uint8_t* p_buf, uint32_t len);

  std::optional<btav_a2dp_codec_index_t> SinkCodecIndex(const uint8_t* p_codec_info);
  std::optional<btav_a2dp_codec_index_t> SourceCodecIndex(const uint8_t* p_codec_info);
  std::optional<const char*> CodecIndexStr(btav_a2dp_codec_index_t codec_index);
  bool SupportsCodec(btav_a2dp_codec_index_t codec_index);
  bool CodecInfo(btav_a2dp_codec_index_t codec_index, bluetooth::a2dp::CodecId* codec_id,
                 uint8_t* codec_info, btav_a2dp_codec_config_t* codec_config);
  std::optional<::bluetooth::audio::a2dp::provider::a2dp_configuration> GetA2dpConfiguration(
          RawAddress peer_address,
          std::vector<::bluetooth::audio::a2dp::provider::a2dp_remote_capabilities> const&
                  remote_seps,
          btav_a2dp_codec_config_t const& user_preferences);
  tA2DP_STATUS ParseA2dpConfiguration(btav_a2dp_codec_index_t codec_index,
                                      const uint8_t* codec_info,
                                      btav_a2dp_codec_config_t* codec_parameters,
                                      std::vector<uint8_t>* vendor_specific_parameters);
  bool IsOffloadEnabled();
  bool IsOffloadActive();
  bool IsEnabled();

  // Save the value if the remote reports its delay before this interface is
  // initialized
  static uint16_t remote_delay;
  // Save the value if the stack reports low latency before this interface is
  // initialized
  static bool is_low_latency_mode_allowed;

private:
  void SwitchToSoftwareInterface();
  void SwitchToHardwareOffloadInterface();

  std::unique_ptr<SoftwareEncoding> software_encoding_ = nullptr;
  std::unique_ptr<HardwareOffloadEncoding> offload_encoding_ = nullptr;
  BaseEncoding* current_encoding_interface_ = nullptr;
};

}  // namespace a2dp
}  // namespace aidl
}  // namespace audio
}  // namespace bluetooth
