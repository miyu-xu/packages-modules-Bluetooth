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

#include "a2dp_encoding.h"

#include <vector>

#include "aidl/a2dp/a2dp_encoding_aidl_controller.h"
#include "aidl/a2dp/a2dp_encoding_aidl_transport.h"
#include "aidl/a2dp/a2dp_provider_info.h"
#include "hal_version_manager.h"
#include "hidl/a2dp_encoding_hidl.h"

namespace bluetooth {
namespace audio {
namespace a2dp {

using aidl::a2dp::A2dpAidlClientInterface;
using aidl::a2dp::HardwareOffloadEncoding;
using aidl::a2dp::SoftwareEncoding;
using ::aidl::android::hardware::bluetooth::audio::SessionType;

std::unique_ptr<A2dpAidlClientInterface> aidl_client;

bool update_codec_offloading_capabilities(
        const std::vector<btav_a2dp_codec_config_t>& framework_preference) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    return hidl::a2dp::update_codec_offloading_capabilities(framework_preference);
  }
  return true;
}

// Check if new bluetooth_audio is enabled
bool is_hal_enabled() {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    return hidl::a2dp::is_hal_2_0_enabled();
  }

  if (aidl_client) {
    return aidl_client->IsEnabled();
  }
  return false;
}

// Check if new bluetooth_audio is running with offloading encoders
bool is_hal_offloading() {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    return hidl::a2dp::is_hal_2_0_offloading();
  }

  if (aidl_client) {
    return aidl_client->IsOffloadEnabled();
  }
  return false;
}

// Initialize BluetoothAudio HAL: openProvider
bool init(bluetooth::common::MessageLoopThread* message_loop,
          bluetooth::audio::a2dp::StreamCallbacks const* stream_callbacks, bool offload_enabled,
          std::unique_ptr<::bluetooth::audio::aidl::a2dp::ProviderInfo> provider_info) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    return hidl::a2dp::init(message_loop, stream_callbacks, offload_enabled);
  }

  log::info("offload_enabled={}", offload_enabled);
  log::assert_that(stream_callbacks != nullptr, "stream_callbacks != nullptr");

  std::unique_ptr<HardwareOffloadEncoding> hardware_offload_encoding = nullptr;
  std::unique_ptr<SoftwareEncoding> software_encoding = nullptr;

  if (aidl_client != nullptr && offload_enabled && !aidl_client->IsOffloadAvailable()) {
    log::info("Update aidl_client with HardwareOffload. Reinitializing HAL.", offload_enabled);
    aidl_client->Cleanup(true);
    aidl_client.reset();
  }

  if (aidl_client != nullptr && aidl_client->IsEnabled()) {
    log::debug("BluetoothAudio HAL is already enabled");
    return true;
  }

  if (!aidl::a2dp::BluetoothAudioClientInterface::is_aidl_available()) {
    log::error("BluetoothAudio AIDL implementation does not exist");
    return false;
  }

  aidl::a2dp::A2dpEncodingTransport* software_transport = new aidl::a2dp::A2dpEncodingTransport(
          SessionType::A2DP_SOFTWARE_ENCODING_DATAPATH, stream_callbacks);
  aidl::a2dp::BluetoothAudioClientInterface* software_audio_interface =
          new aidl::a2dp::BluetoothAudioClientInterface(software_transport);
  if (!software_audio_interface->IsValid()) {
    log::error("software_audio_interface is invalid");
    delete software_transport;
    delete software_audio_interface;
    return false;
  }

  software_encoding = std::make_unique<SoftwareEncoding>(software_audio_interface);

  if (offload_enabled) {
    aidl::a2dp::A2dpEncodingTransport* offload_transport = new aidl::a2dp::A2dpEncodingTransport(
            SessionType::A2DP_HARDWARE_OFFLOAD_ENCODING_DATAPATH, stream_callbacks);
    aidl::a2dp::BluetoothAudioClientInterface* hardware_audio_interface =
            new aidl::a2dp::BluetoothAudioClientInterface(offload_transport);
    if (!hardware_audio_interface->IsValid()) {
      log::error("hardware_audio_interface is invalid");
      delete offload_transport;
      delete hardware_audio_interface;
    } else {
      hardware_offload_encoding = std::make_unique<HardwareOffloadEncoding>(
              hardware_audio_interface, std::move(provider_info));
    }
  }

  aidl_client = std::make_unique<A2dpAidlClientInterface>(std::move(software_encoding),
                                                          std::move(hardware_offload_encoding));

  if (A2dpAidlClientInterface::remote_delay != 0) {
    log::info("restore DELAY {} ms",
              static_cast<float>(A2dpAidlClientInterface::remote_delay / 10.0));
    aidl_client->SetRemoteDelay(A2dpAidlClientInterface::remote_delay);
    A2dpAidlClientInterface::remote_delay = 0;
  }
  return true;
}

// Clean up BluetoothAudio HAL
void cleanup() {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    hidl::a2dp::cleanup();
    return;
  }

  log::info("");
  if (aidl_client) {
    aidl_client->Cleanup(false);
  }
  aidl_client.reset();
}

// Set up the codec into BluetoothAudio HAL
bool setup_codec(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                 int preferred_encoding_interval_us) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    return hidl::a2dp::setup_codec(a2dp_config, peer_mtu, preferred_encoding_interval_us);
  }

  log::info("");
  if (aidl_client) {
    return aidl_client->UpdateAudioConfigToHal(a2dp_config, peer_mtu,
                                               preferred_encoding_interval_us);
  }
  return false;
}

// Send command to the BluetoothAudio HAL: StartSession, EndSession,
// StreamStarted, StreamSuspended
void start_session() {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    hidl::a2dp::start_session();
    return;
  }

  log::info("");
  if (aidl_client) {
    aidl_client->StartSession();
  }
}

void end_session() {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    hidl::a2dp::end_session();
    return;
  }

  log::info("");
  if (aidl_client) {
    aidl_client->StopSession();
  }
}

void ack_stream_started(Status status) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    hidl::a2dp::ack_stream_started(status);
    return;
  }

  log::info("");
  if (aidl_client) {
    aidl_client->ConfirmStreamStarted(status);
  }
}

void ack_stream_suspended(Status status) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    hidl::a2dp::ack_stream_suspended(status);
    return;
  }

  log::info("");
  if (aidl_client) {
    aidl_client->ConfirmStreamSuspended(status);
  }
}

// Read from the FMQ of BluetoothAudio HAL
size_t read(uint8_t* p_buf, uint32_t len) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    return hidl::a2dp::read(p_buf, len);
  }

  if (aidl_client) {
    return aidl_client->Read(p_buf, len);
  }
  return 0;
}

// Update A2DP delay report to BluetoothAudio HAL
void set_remote_delay(uint16_t delay_report) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    hidl::a2dp::set_remote_delay(delay_report);
    return;
  }

  A2dpAidlClientInterface::remote_delay = delay_report;
  if (aidl_client) {
    aidl_client->SetRemoteDelay(A2dpAidlClientInterface::remote_delay);
  } else {
    log::info("DelayReport: {} ms saved. Waiting for interface initalization.",
              static_cast<float>(delay_report / 10.0));
  }
}

// Set low latency buffer mode allowed or disallowed
void set_audio_low_latency_mode_allowed(bool allowed) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::AIDL) {
    A2dpAidlClientInterface::is_low_latency_mode_allowed = allowed;
    if (aidl_client) {
      aidl_client->SetLowLatencyMode(A2dpAidlClientInterface::is_low_latency_mode_allowed);
    } else {
      log::info("Low Latency Buffer Mode: {} saved. Waiting for interface initalization",
                allowed ? "allowed" : "prohibited");
    }
  }
}

// Check if OPUS codec is supported
bool is_opus_supported() {
  // OPUS codec was added after HIDL HAL was frozen
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::AIDL) {
    return true;
  }
  return false;
}

namespace provider {

// Lookup the codec info in the list of supported offloaded sink codecs.
std::optional<btav_a2dp_codec_index_t> sink_codec_index(const uint8_t* p_codec_info) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::AIDL) {
    if (aidl_client) {
      return aidl_client->SinkCodecIndex(p_codec_info);
    }
  }
  return std::nullopt;
}

// Lookup the codec info in the list of supported offloaded source codecs.
std::optional<btav_a2dp_codec_index_t> source_codec_index(const uint8_t* p_codec_info) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::AIDL) {
    if (aidl_client) {
      return aidl_client->SourceCodecIndex(p_codec_info);
    }
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
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::AIDL) {
    if (aidl_client) {
      return aidl_client->CodecIndexStr(codec_index);
    }
  }
  return std::nullopt;
}

// Return true if the codec is supported for the session type
// A2DP_HARDWARE_ENCODING_DATAPATH or A2DP_HARDWARE_DECODING_DATAPATH.
bool supports_codec(btav_a2dp_codec_index_t codec_index) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::AIDL) {
    if (aidl_client) {
      return aidl_client->SupportsCodec(codec_index);
    }
  }
  return false;
}

// Return the A2DP capabilities for the selected codec.
bool codec_info(btav_a2dp_codec_index_t codec_index, bluetooth::a2dp::CodecId* codec_id,
                uint8_t* codec_info, btav_a2dp_codec_config_t* codec_config) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::AIDL) {
    if (aidl_client) {
      return aidl_client->CodecInfo(codec_index, codec_id, codec_info, codec_config);
    }
  }
  return false;
}

// Query the codec selection fromt the audio HAL.
// The HAL is expected to pick the best audio configuration based on the
// discovered remote SEPs.
std::optional<a2dp_configuration> get_a2dp_configuration(
        RawAddress peer_address, std::vector<a2dp_remote_capabilities> const& remote_seps,
        btav_a2dp_codec_config_t const& user_preferences) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::AIDL) {
    if (aidl_client) {
      return aidl_client->GetA2dpConfiguration(peer_address, remote_seps, user_preferences);
    }
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
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::AIDL) {
    if (aidl_client) {
      return aidl_client->ParseA2dpConfiguration(codec_index, codec_info, codec_parameters,
                                                 vendor_specific_parameters);
    }
  }
  return A2DP_FAIL;
}

}  // namespace provider
}  // namespace a2dp
}  // namespace audio
}  // namespace bluetooth
