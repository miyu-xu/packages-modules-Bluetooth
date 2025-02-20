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
#include "a2dp_sbc_constants.h"
#include "common/message_loop_thread.h"
#include "hardware/bt_av.h"
#include "osi/include/properties.h"
#include "types/raw_address.h"

namespace bluetooth {
namespace audio {
namespace aidl {
namespace a2dp {


using ::bluetooth::audio::a2dp::Status;
using ::bluetooth::audio::a2dp::StreamCallbacks;
using ::bluetooth::audio::aidl::a2dp::LatencyMode;


// Provide call-in APIs for the Bluetooth Audio HAL
class A2dpEncodingTransport : public ::bluetooth::audio::aidl::a2dp::IBluetoothTransportInstance {
public:
  A2dpEncodingTransport(SessionType session_type, std::shared_ptr<StreamCallbacks> stream_callbacks);

  ~A2dpEncodingTransport();

  Status StartRequest(bool is_low_latency) override;

  Status SuspendRequest() override;

  void StopRequest() override;

  void SetLatencyMode(LatencyMode latency_mode) override;

  bool GetPresentationPosition(uint64_t* remote_delay_report_ns, uint64_t* total_bytes_read,
                               timespec* data_position) override;

  void ResetPresentationPosition() override;

  void LogBytesRead(size_t bytes_read) override;

  tA2DP_CTRL_CMD GetPendingCmd() const;

  void ResetPendingCmd();

  // delay reports from AVDTP is based on 1/10 ms (100us)
  void SetRemoteDelay(uint16_t delay_report);

private:
  tA2DP_CTRL_CMD a2dp_pending_cmd_;
  uint16_t remote_delay_report_;
  uint64_t total_bytes_read_;
  timespec data_position_;
  std::shared_ptr<StreamCallbacks> stream_callbacks_;
};

class A2dpClientInterface {
private:
  class IClientInterfaceEndpoint {
  public:
    virtual ~IClientInterfaceEndpoint() = default;
    virtual void Cleanup() = 0;
    virtual void SetRemoteDelay(uint16_t delay_report_ms) = 0;
    virtual void StartSession() = 0;
    virtual void StopSession() = 0;
    virtual void ConfirmStreamStartRequest(::bluetooth::audio::a2dp::Status status) = 0;
    virtual void ConfirmStreamSuspendRequest(::bluetooth::audio::a2dp::Status status) = 0;
    virtual bool UpdateAudioConfigToHal(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                 int preferred_encoding_interval_us) = 0;
    virtual void SetLowLatencyMode(bool allowed) = 0;
  };

public:
  class Encode : public IClientInterfaceEndpoint {
  public:
    virtual ~Encode() = default;
    void Cleanup() override;
    void SetRemoteDelay(uint16_t delay_report) override;
    void StartSession() override;
    void StopSession() override;
    void ConfirmStreamStartRequest(::bluetooth::audio::a2dp::Status status) override;
    void ConfirmStreamSuspendRequest(::bluetooth::audio::a2dp::Status status) override;
    bool UpdateAudioConfigToHal(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                 int preferred_encoding_interval_us) override;
    void SetLowLatencyMode(bool allowed) override;
    size_t Read(uint8_t* p_buf, uint32_t len);
  };

  class Provider {
  public:
    std::optional<btav_a2dp_codec_index_t> SinkCodecIndex(const uint8_t* p_codec_info);
    std::optional<btav_a2dp_codec_index_t> SourceCodecIndex(const uint8_t* p_codec_info);
    std::optional<const char*> CodecIndexStr(btav_a2dp_codec_index_t codec_index);
    bool CodecInfo(btav_a2dp_codec_index_t codec_index, bluetooth::a2dp::CodecId* codec_id, uint8_t* codec_info, btav_a2dp_codec_config_t* codec_config);
    std::optional<::bluetooth::audio::a2dp::provider::a2dp_configuration> GetA2dpConfiguration(RawAddress peer_address, std::vector<::bluetooth::audio::a2dp::provider::a2dp_remote_capabilities> const& remote_seps, btav_a2dp_codec_config_t const& user_preferences);
    tA2DP_STATUS ParseA2dpConfiguration(btav_a2dp_codec_index_t codec_index, const uint8_t* codec_info, btav_a2dp_codec_config_t* codec_parameters, std::vector<uint8_t>* vendor_specific_parameters);
  };

  Encode* GetEncoder(std::shared_ptr<StreamCallbacks> stream_callbacks,
                     bluetooth::common::MessageLoopThread* message_loop,
                     std::unique_ptr<::bluetooth::audio::aidl::a2dp::ProviderInfo> provider_info,
                     bool offload_enabled);
  void ReleaseEncode();

  Provider* GetProvider();
  void ReleaseProvider();

  static A2dpClientInterface* Get();

private:

  BluetoothAudioClientInterface* NewHalInterface(SessionType session_type, std::shared_ptr<StreamCallbacks> stream_callbacks);
  void DeleteHalInterface(BluetoothAudioClientInterface* hal_interface);

  static A2dpClientInterface* interface;
  Encode* encode_ = nullptr;
  Provider* provider_ = nullptr;

  BluetoothAudioClientInterface* software_hal_interface = nullptr;
  BluetoothAudioClientInterface* offloading_hal_interface = nullptr;
  BluetoothAudioClientInterface* active_hal_interface = nullptr;
  uint16_t remote_delay;
  bool is_low_latency_mode_allowed;
  // ProviderInfo for A2DP hardware offload encoding and decoding data paths,
  // if supported by the HAL and enabled. nullptr if not supported
  // or disabled.
  std::unique_ptr<::bluetooth::audio::aidl::a2dp::ProviderInfo> provider_info_;
}


bool update_codec_offloading_capabilities(
        const std::vector<btav_a2dp_codec_config_t>& framework_preference,
        bool supports_a2dp_hw_offload_v2);

// /***
//  * Check if new bluetooth_audio is enabled
//  ***/
// bool is_hal_enabled();

// /***
//  * Check if new bluetooth_audio is running with offloading encoders
//  ***/
// bool is_hal_offloading();

/***
 * Initialize BluetoothAudio HAL: openProvider
 ***/
bool init(bluetooth::common::MessageLoopThread* message_loop,
          bluetooth::audio::a2dp::StreamCallbacks const* stream_callbacks, bool offload_enabled);

// /***
//  * Clean up BluetoothAudio HAL
//  ***/
// void cleanup();

// /***
//  * Set up the codec into BluetoothAudio HAL
//  ***/
// bool setup_codec(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
//                  int preferred_encoding_interval_us);

/***
 * Send command to the BluetoothAudio HAL: StartSession, EndSession,
 * StreamStarted, StreamSuspended
 ***/
// void start_session();
// void end_session();
// void ack_stream_started(::bluetooth::audio::a2dp::Status status);
void ack_stream_suspended(::bluetooth::audio::a2dp::Status status);

// /***
//  * Read from the FMQ of BluetoothAudio HAL
//  ***/
// size_t read(uint8_t* p_buf, uint32_t len);

// /***
//  * Update A2DP delay report to BluetoothAudio HAL
//  ***/
// void set_remote_delay(uint16_t delay_report);

// /***
//  * Set low latency buffer mode allowed or disallowed
//  ***/
// void set_low_latency_mode_allowed(bool allowed);

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
}  // namespace a2dp
}  // namespace aidl
}  // namespace audio
}  // namespace bluetooth
