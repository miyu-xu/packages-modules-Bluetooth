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

#include <vector>

#include "a2dp_common_encoding_interface.h"
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

//=============================================================================
// SoftwareEncoding : AIDL
//=============================================================================

class SoftwareEncoding : public ::bluetooth::audio::a2dp::IA2dpEncoding {
public:
  SoftwareEncoding(::bluetooth::audio::aidl::a2dp::BluetoothAudioClientInterface* audio_interface);
  ~SoftwareEncoding() override;
  void SetRemoteDelay(uint16_t delay_report) override;
  void SetLowLatencyMode(bool allowed) override;
  void StartSession() override;
  void StopSession() override;
  void ConfirmStreamStarted(Status status) override;
  void ConfirmStreamSuspended(Status status) override;
  bool UpdateAudioConfigToHal(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                              int preferred_encoding_interval_us) override;
  bool IsCodecSupportedByHardwareOffload(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu) override;
  size_t Read(uint8_t* p_buf, uint32_t len) override;

private:
  // The client interface connects an IBluetoothTransportInstance to
  // IBluetoothAudioProvider and helps to route callbacks to
  // IBluetoothTransportInstance
  ::bluetooth::audio::aidl::a2dp::BluetoothAudioClientInterface* interface_ = nullptr;
};

//=============================================================================
// HardwareOffloadEncoding : AIDL
//=============================================================================

class HardwareOffloadEncoding : public ::bluetooth::audio::a2dp::IA2dpEncoding {
public:
  HardwareOffloadEncoding(
          ::bluetooth::audio::aidl::a2dp::BluetoothAudioClientInterface* audio_interface,
          std::unique_ptr<::bluetooth::audio::aidl::a2dp::ProviderInfo> provider_info);
  ~HardwareOffloadEncoding() override;
  void SetRemoteDelay(uint16_t delay_report) override;
  void SetLowLatencyMode(bool allowed) override;
  void StartSession() override;
  void StopSession() override;
  void ConfirmStreamStarted(Status status) override;
  void ConfirmStreamSuspended(Status status) override;
  bool UpdateAudioConfigToHal(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                              int preferred_encoding_interval_us) override;
  bool IsCodecSupportedByHardwareOffload(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu) override;
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

  // The client interface connects an IBluetoothTransportInstance to
  // IBluetoothAudioProvider and helps to route callbacks to
  // IBluetoothTransportInstance
  ::bluetooth::audio::aidl::a2dp::BluetoothAudioClientInterface* interface_ = nullptr;
};

}  // namespace a2dp
}  // namespace aidl
}  // namespace audio
}  // namespace bluetooth
