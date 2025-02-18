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

#define LOG_TAG "bluetooth-a2dp-ahal"

#include "a2dp_encoding.h"

#include <vector>

#include "a2dp_common_encoding_interface.h"
#include "a2dp_constants.h"
#include "aidl/a2dp/a2dp_encoding_aidl.h"
#include "aidl/a2dp/a2dp_encoding_aidl_transport.h"
#include "aidl/a2dp/a2dp_provider_info.h"
#include "hal_version_manager.h"
#include "hidl/a2dp_encoding_hidl.h"
#include "hidl/a2dp_encoding_hidl_transport.h"

namespace bluetooth {
namespace audio {
namespace a2dp {

using ::aidl::android::hardware::bluetooth::audio::CodecConfiguration;

uint16_t A2dpClientInterface::remote_delay = 0;
bool A2dpClientInterface::is_low_latency_mode_allowed = false;

std::unique_ptr<A2dpClientInterface> a2dp_client_inteface;

A2dpClientInterface::A2dpClientInterface(std::unique_ptr<IA2dpEncoding> software_encoding,
                                         std::unique_ptr<IA2dpEncoding> offload_encoding)
    : software_encoding_(std::move(software_encoding)),
      offload_encoding_(std::move(offload_encoding)) {
  if (offload_encoding_) {
    log::info("current interface: HardwareOffloadEncoding");
    current_encoding_interface_ = offload_encoding_.get();
  } else {
    log::info("current interface: SoftwareEncoding");
    current_encoding_interface_ = software_encoding_.get();
  }
}

void A2dpClientInterface::Cleanup(bool update_only = false) {
  if (!current_encoding_interface_) {
    log::error("no available Bluetooth Audio HAL interface");
    return;
  }
  current_encoding_interface_->StopSession();

  if (software_encoding_) {
    software_encoding_.reset();
  }

  if (offload_encoding_) {
    offload_encoding_.reset();
  }
  current_encoding_interface_ = nullptr;
  if (!update_only) {
    remote_delay = 0;
    is_low_latency_mode_allowed = false;
  }
}

void A2dpClientInterface::SetRemoteDelay(uint16_t delay_report) {
  if (!current_encoding_interface_) {
    log::error("no available Bluetooth Audio HAL interface");
    return;
  }
  current_encoding_interface_->SetRemoteDelay(delay_report);
}

void A2dpClientInterface::SetLowLatencyMode(bool allowed) {
  if (!current_encoding_interface_) {
    log::error("no available Bluetooth Audio HAL interface");
    return;
  }
  current_encoding_interface_->SetLowLatencyMode(allowed);
}

void A2dpClientInterface::StartSession() {
  if (!current_encoding_interface_) {
    log::error("no available Bluetooth Audio HAL interface");
    return;
  }
  current_encoding_interface_->SetLowLatencyMode(A2dpClientInterface::is_low_latency_mode_allowed);
  current_encoding_interface_->StartSession();
}

void A2dpClientInterface::StopSession() {
  if (!current_encoding_interface_) {
    log::error("no available Bluetooth Audio HAL interface");
    return;
  }
  current_encoding_interface_->StopSession();
}

void A2dpClientInterface::ConfirmStreamStarted(Status status) {
  if (!current_encoding_interface_) {
    log::error("no available Bluetooth Audio HAL interface");
    return;
  }
  current_encoding_interface_->ConfirmStreamStarted(status);
}

void A2dpClientInterface::ConfirmStreamSuspended(Status status) {
  if (!current_encoding_interface_) {
    log::error("no available Bluetooth Audio HAL interface");
    return;
  }
  current_encoding_interface_->ConfirmStreamSuspended(status);
}

bool A2dpClientInterface::UpdateAudioConfigToHal(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                                                 int preferred_encoding_interval_us) {
  if (!current_encoding_interface_) {
    log::error("no available Bluetooth Audio HAL interface");
    return false;
  }

  bool is_offloaded_codec = IsCodecSupportedByHardwareOffload(a2dp_config, peer_mtu);
  if (IsOffloadAvailable() && is_offloaded_codec && !IsOffloadEnabled()) {
    log::info("Switching BluetoothAudio HAL to HardwareOffload");
    StopSession();
    SwitchToHardwareOffloadEncoding();
  } else if (!is_offloaded_codec && IsOffloadEnabled()) {
    log::info("Switching BluetoothAudio HAL to Software");
    StopSession();
    SwitchToSoftwareEncoding();
  }

  return current_encoding_interface_->UpdateAudioConfigToHal(a2dp_config, peer_mtu,
                                                             preferred_encoding_interval_us);
}

size_t A2dpClientInterface::Read(uint8_t* p_buf, uint32_t len) {
  if (!current_encoding_interface_) {
    log::error("no available Bluetooth Audio HAL interface");
    return 0;
  }
  return current_encoding_interface_->Read(p_buf, len);
}

std::optional<btav_a2dp_codec_index_t> A2dpClientInterface::SinkCodecIndex(
        const uint8_t* p_codec_info) {
  if (!current_encoding_interface_) {
    log::error("no available Bluetooth Audio HAL interface");
    return std::nullopt;
  }
  return current_encoding_interface_->SinkCodecIndex(p_codec_info);
}

std::optional<btav_a2dp_codec_index_t> A2dpClientInterface::SourceCodecIndex(
        const uint8_t* p_codec_info) {
  if (!current_encoding_interface_) {
    log::error("no available Bluetooth Audio HAL interface");
    return std::nullopt;
  }
  return current_encoding_interface_->SourceCodecIndex(p_codec_info);
}

std::optional<const char*> A2dpClientInterface::CodecIndexStr(btav_a2dp_codec_index_t codec_index) {
  if (!current_encoding_interface_) {
    log::error("no available Bluetooth Audio HAL interface");
    return std::nullopt;
  }
  return current_encoding_interface_->CodecIndexStr(codec_index);
}

bool A2dpClientInterface::SupportsCodec(btav_a2dp_codec_index_t codec_index) {
  if (!current_encoding_interface_) {
    log::error("no available Bluetooth Audio HAL interface");
    return false;
  }
  return current_encoding_interface_->SupportsCodec(codec_index);
}

bool A2dpClientInterface::CodecInfo(btav_a2dp_codec_index_t codec_index,
                                    bluetooth::a2dp::CodecId* codec_id, uint8_t* codec_info,
                                    btav_a2dp_codec_config_t* codec_config) {
  if (!current_encoding_interface_) {
    log::error("no available Bluetooth Audio HAL interface");
    return false;
  }
  return current_encoding_interface_->CodecInfo(codec_index, codec_id, codec_info, codec_config);
}

std::optional<::bluetooth::audio::a2dp::provider::a2dp_configuration>
A2dpClientInterface::GetA2dpConfiguration(
        RawAddress peer_address,
        std::vector<::bluetooth::audio::a2dp::provider::a2dp_remote_capabilities> const&
                remote_seps,
        btav_a2dp_codec_config_t const& user_preferences) {
  if (!current_encoding_interface_) {
    log::error("no available Bluetooth Audio HAL interface");
    return std::nullopt;
  }
  return current_encoding_interface_->GetA2dpConfiguration(peer_address, remote_seps,
                                                           user_preferences);
}

tA2DP_STATUS A2dpClientInterface::ParseA2dpConfiguration(
        btav_a2dp_codec_index_t codec_index, const uint8_t* codec_info,
        btav_a2dp_codec_config_t* codec_parameters,
        std::vector<uint8_t>* vendor_specific_parameters) {
  if (!current_encoding_interface_) {
    log::error("no available Bluetooth Audio HAL interface");
    return A2DP_FAIL;
  }
  return current_encoding_interface_->ParseA2dpConfiguration(
          codec_index, codec_info, codec_parameters, vendor_specific_parameters);
}

bool A2dpClientInterface::IsOffloadAvailable() {
  bool status = offload_encoding_ != nullptr;
  log::verbose(": {}", status);
  return status;
}

bool A2dpClientInterface::IsOffloadEnabled() {
  bool status = IsOffloadAvailable() && offload_encoding_.get() == current_encoding_interface_;
  log::verbose(": {}", status);
  return status;
}

bool A2dpClientInterface::IsEnabled() {
  bool status = current_encoding_interface_ != nullptr;
  log::verbose(": {}", status);
  return status;
}

void A2dpClientInterface::SwitchToSoftwareEncoding() {
  if (!current_encoding_interface_) {
    log::error("no available Bluetooth Audio HAL interface");
    return;
  }
  if (offload_encoding_ && current_encoding_interface_ == offload_encoding_.get()) {
    log::info("current interface: SoftwareEncoding");
    current_encoding_interface_ = software_encoding_.get();
  }
}

void A2dpClientInterface::SwitchToHardwareOffloadEncoding() {
  if (!current_encoding_interface_) {
    log::error("no available Bluetooth Audio HAL interface");
    return;
  }
  if (software_encoding_ && current_encoding_interface_ == software_encoding_.get()) {
    log::info("current interface: HardwareOffloadEncoding");
    current_encoding_interface_ = offload_encoding_.get();
  }
}

bool A2dpClientInterface::IsCodecSupportedByHardwareOffload(A2dpCodecConfig* a2dp_config,
                                                            uint16_t peer_mtu) {
  if (!current_encoding_interface_) {
    log::error("no available Bluetooth Audio HAL interface");
    return false;
  }
  return current_encoding_interface_->IsCodecSupportedByHardwareOffload(a2dp_config, peer_mtu);
}

// Check if new bluetooth_audio is enabled
bool is_hal_enabled() {
  if (a2dp_client_inteface) {
    return a2dp_client_inteface->IsEnabled();
  }
  return false;
}

// Check if new bluetooth_audio is running with offloading encoders
bool is_hal_offloading() {
  if (a2dp_client_inteface) {
    return a2dp_client_inteface->IsOffloadEnabled();
  }
  return false;
}

// Initialize BluetoothAudio HAL: openProvider
bool init(bluetooth::common::MessageLoopThread* message_loop,
          bluetooth::audio::a2dp::StreamCallbacks const* stream_callbacks, bool offload_enabled,
          std::unique_ptr<::bluetooth::audio::aidl::a2dp::ProviderInfo> provider_info) {
  log::info("offload_enabled={}", offload_enabled);
  log::assert_that(stream_callbacks != nullptr, "stream_callbacks != nullptr");

  if (a2dp_client_inteface != nullptr && offload_enabled &&
      !a2dp_client_inteface->IsOffloadAvailable()) {
    log::info("Update a2dp_client_inteface with HardwareOffload. Reinitializing HAL.",
              offload_enabled);
    a2dp_client_inteface->Cleanup(true);
    a2dp_client_inteface.reset();
  }

  if (a2dp_client_inteface != nullptr && a2dp_client_inteface->IsEnabled()) {
    log::debug("BluetoothAudio HAL is already enabled");
    return true;
  }

  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    std::unique_ptr<hidl::a2dp::HardwareOffloadEncoding> hardware_offload_encoding = nullptr;
    std::unique_ptr<hidl::a2dp::SoftwareEncoding> software_encoding = nullptr;

    hidl::a2dp::A2dpTransport* software_transport = new hidl::a2dp::A2dpTransport(
            ::bluetooth::audio::hidl::SessionType::A2DP_SOFTWARE_ENCODING_DATAPATH,
            stream_callbacks);
    hidl::BluetoothAudioSinkClientInterface* software_audio_interface =
            new hidl::BluetoothAudioSinkClientInterface(software_transport, message_loop);
    if (!software_audio_interface->IsValid()) {
      log::error("software_audio_interface is invalid");
      delete software_transport;
      delete software_audio_interface;
      return false;
    }

    software_encoding = std::make_unique<hidl::a2dp::SoftwareEncoding>(software_audio_interface);

    if (offload_enabled) {
      hidl::a2dp::A2dpTransport* offload_transport = new hidl::a2dp::A2dpTransport(
              ::bluetooth::audio::hidl::SessionType::A2DP_HARDWARE_OFFLOAD_DATAPATH,
              stream_callbacks);
      hidl::BluetoothAudioSinkClientInterface* hardware_audio_interface =
              new hidl::BluetoothAudioSinkClientInterface(offload_transport, message_loop);
      if (!hardware_audio_interface->IsValid()) {
        log::error("hardware_audio_interface is invalid");
        delete offload_transport;
        delete hardware_audio_interface;
      } else {
        hardware_offload_encoding =
                std::make_unique<hidl::a2dp::HardwareOffloadEncoding>(hardware_audio_interface);
      }
    }

    a2dp_client_inteface = std::make_unique<A2dpClientInterface>(
            std::move(software_encoding), std::move(hardware_offload_encoding));

  } else if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::AIDL) {
    std::unique_ptr<aidl::a2dp::HardwareOffloadEncoding> hardware_offload_encoding = nullptr;
    std::unique_ptr<aidl::a2dp::SoftwareEncoding> software_encoding = nullptr;

    if (!aidl::a2dp::BluetoothAudioClientInterface::is_aidl_available()) {
      log::error("BluetoothAudio AIDL implementation does not exist");
      return false;
    }

    aidl::a2dp::A2dpTransport* software_transport =
            new aidl::a2dp::A2dpTransport(::aidl::android::hardware::bluetooth::audio::SessionType::
                                                  A2DP_SOFTWARE_ENCODING_DATAPATH,
                                          stream_callbacks);
    aidl::a2dp::BluetoothAudioClientInterface* software_audio_interface =
            new aidl::a2dp::BluetoothAudioClientInterface(software_transport);
    if (!software_audio_interface->IsValid()) {
      log::error("software_audio_interface is invalid");
      delete software_transport;
      delete software_audio_interface;
      return false;
    }

    software_encoding = std::make_unique<aidl::a2dp::SoftwareEncoding>(software_audio_interface);

    if (offload_enabled) {
      aidl::a2dp::A2dpTransport* offload_transport = new aidl::a2dp::A2dpTransport(
              ::aidl::android::hardware::bluetooth::audio::SessionType::
                      A2DP_HARDWARE_OFFLOAD_ENCODING_DATAPATH,
              stream_callbacks);
      aidl::a2dp::BluetoothAudioClientInterface* hardware_audio_interface =
              new aidl::a2dp::BluetoothAudioClientInterface(offload_transport);
      if (!hardware_audio_interface->IsValid()) {
        log::error("hardware_audio_interface is invalid");
        delete offload_transport;
        delete hardware_audio_interface;
      } else {
        hardware_offload_encoding = std::make_unique<aidl::a2dp::HardwareOffloadEncoding>(
                hardware_audio_interface, std::move(provider_info));
      }
    }

    a2dp_client_inteface = std::make_unique<A2dpClientInterface>(
            std::move(software_encoding), std::move(hardware_offload_encoding));
  }

  if (a2dp_client_inteface && A2dpClientInterface::remote_delay != 0) {
    log::info("restore DELAY {} ms", static_cast<float>(A2dpClientInterface::remote_delay / 10.0));
    a2dp_client_inteface->SetRemoteDelay(A2dpClientInterface::remote_delay);
    A2dpClientInterface::remote_delay = 0;
  }
  return true;
}

// Clean up BluetoothAudio HAL
void cleanup() {
  log::info("");
  if (a2dp_client_inteface) {
    a2dp_client_inteface->Cleanup(false);
  }
  a2dp_client_inteface.reset();
}

// Set up the codec into BluetoothAudio HAL
bool setup_codec(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                 int preferred_encoding_interval_us) {
  log::info("");
  if (a2dp_client_inteface) {
    return a2dp_client_inteface->UpdateAudioConfigToHal(a2dp_config, peer_mtu,
                                                        preferred_encoding_interval_us);
  }
  return false;
}

// Send command to the BluetoothAudio HAL: StartSession, EndSession,
// StreamStarted, StreamSuspended
void start_session() {
  log::info("");
  if (a2dp_client_inteface) {
    a2dp_client_inteface->StartSession();
  }
}

void end_session() {
  log::info("");
  if (a2dp_client_inteface) {
    a2dp_client_inteface->StopSession();
  }
}

void ack_stream_started(Status status) {
  log::info("");
  if (a2dp_client_inteface) {
    a2dp_client_inteface->ConfirmStreamStarted(status);
  }
}

void ack_stream_suspended(Status status) {
  log::info("");
  if (a2dp_client_inteface) {
    a2dp_client_inteface->ConfirmStreamSuspended(status);
  }
}

// Read from the FMQ of BluetoothAudio HAL
size_t read(uint8_t* p_buf, uint32_t len) {
  if (a2dp_client_inteface) {
    return a2dp_client_inteface->Read(p_buf, len);
  }
  return 0;
}

// Update A2DP delay report to BluetoothAudio HAL
void set_remote_delay(uint16_t delay_report) {
  A2dpClientInterface::remote_delay = delay_report;
  if (a2dp_client_inteface) {
    a2dp_client_inteface->SetRemoteDelay(A2dpClientInterface::remote_delay);
  } else {
    log::info("DelayReport: {} ms saved. Waiting for interface initialization.",
              static_cast<float>(delay_report / 10.0));
  }
}

// Set low latency buffer mode allowed or disallowed
void set_audio_low_latency_mode_allowed(bool allowed) {
  A2dpClientInterface::is_low_latency_mode_allowed = allowed;
  if (a2dp_client_inteface) {
    a2dp_client_inteface->SetLowLatencyMode(A2dpClientInterface::is_low_latency_mode_allowed);
  } else {
    log::info("Low Latency Buffer Mode: {} saved. Waiting for interface initialization",
              allowed ? "allowed" : "prohibited");
  }
}

namespace provider {

// Lookup the codec info in the list of supported offloaded sink codecs.
std::optional<btav_a2dp_codec_index_t> sink_codec_index(const uint8_t* p_codec_info) {
  if (a2dp_client_inteface) {
    return a2dp_client_inteface->SinkCodecIndex(p_codec_info);
  }
  return std::nullopt;
}

// Lookup the codec info in the list of supported offloaded source codecs.
std::optional<btav_a2dp_codec_index_t> source_codec_index(const uint8_t* p_codec_info) {
  if (a2dp_client_inteface) {
    return a2dp_client_inteface->SourceCodecIndex(p_codec_info);
  }
  return std::nullopt;
}

// Return the name of the codec which is assigned to the input index.
// The codec index must be in the ranges
// BTAV_A2DP_CODEC_INDEX_SINK_EXT_MIN..BTAV_A2DP_CODEC_INDEX_SINK_EXT_MAX or
// BTAV_A2DP_CODEC_INDEX_SOURCE_EXT_MIN..BTAV_A2DP_CODEC_INDEX_SOURCE_EXT_MAX.
// Returns nullopt if the codec_index is not assigned or codec extensibility
// is not supported or enabled.
std::optional<const char*> codec_index_str(btav_a2dp_codec_index_t codec_index) {
  if (a2dp_client_inteface) {
    return a2dp_client_inteface->CodecIndexStr(codec_index);
  }
  return std::nullopt;
}

// Return true if the codec is supported for the session type
// A2DP_HARDWARE_ENCODING_DATAPATH or A2DP_HARDWARE_DECODING_DATAPATH.
bool supports_codec(btav_a2dp_codec_index_t codec_index) {
  if (a2dp_client_inteface) {
    return a2dp_client_inteface->SupportsCodec(codec_index);
  }
  return false;
}

// Return the A2DP capabilities for the selected codec.
bool codec_info(btav_a2dp_codec_index_t codec_index, bluetooth::a2dp::CodecId* codec_id,
                uint8_t* codec_info, btav_a2dp_codec_config_t* codec_config) {
  if (a2dp_client_inteface) {
    return a2dp_client_inteface->CodecInfo(codec_index, codec_id, codec_info, codec_config);
  }
  return false;
}

// Query the codec selection fromt the audio HAL.
// The HAL is expected to pick the best audio configuration based on the
// discovered remote SEPs.
std::optional<a2dp_configuration> get_a2dp_configuration(
        RawAddress peer_address,
        std::vector<::bluetooth::audio::a2dp::provider::a2dp_remote_capabilities> const&
                remote_seps,
        btav_a2dp_codec_config_t const& user_preferences) {
  if (a2dp_client_inteface) {
    return a2dp_client_inteface->GetA2dpConfiguration(peer_address, remote_seps, user_preferences);
  }
  return std::nullopt;
}

// Query the codec parameters from the audio HAL.
// The HAL performs a two part validation:
//  - check if the configuration is valid
//  - check if the configuration is supported by the audio provider
// In case any of these checks fails, the corresponding A2DP
// status is returned. If the configuration is valid and supported,
// A2DP_OK is returned.
tA2DP_STATUS parse_a2dp_configuration(btav_a2dp_codec_index_t codec_index,
                                      const uint8_t* codec_info,
                                      btav_a2dp_codec_config_t* codec_parameters,
                                      std::vector<uint8_t>* vendor_specific_parameters) {
  if (a2dp_client_inteface) {
    return a2dp_client_inteface->ParseA2dpConfiguration(codec_index, codec_info, codec_parameters,
                                                        vendor_specific_parameters);
  }
  return A2DP_FAIL;
}

}  // namespace provider
}  // namespace a2dp
}  // namespace audio
}  // namespace bluetooth
