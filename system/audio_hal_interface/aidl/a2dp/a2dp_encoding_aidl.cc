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
#define LOG_TAG "bluetooth-a2dp-ahal-aidl"

#include "a2dp_encoding_aidl.h"

#include <bluetooth/log.h>
#include <com_android_bluetooth_flags.h>

#include <vector>

#include "a2dp_encoding_aidl_transport.h"
#include "a2dp_provider_info.h"
#include "audio_aidl_interfaces.h"
#include "client_interface_aidl.h"
#include "codec_status_aidl.h"
#include "transport_instance.h"

namespace bluetooth {
namespace audio {
namespace aidl {
namespace a2dp {

using ::bluetooth::audio::a2dp::Status;
using ::bluetooth::audio::a2dp::StreamCallbacks;

using ::aidl::android::hardware::bluetooth::audio::A2dpStreamConfiguration;
using ::aidl::android::hardware::bluetooth::audio::AudioConfiguration;
using ::aidl::android::hardware::bluetooth::audio::ChannelMode;
using ::aidl::android::hardware::bluetooth::audio::CodecConfiguration;
using ::aidl::android::hardware::bluetooth::audio::PcmConfiguration;
using ::aidl::android::hardware::bluetooth::audio::SessionType;

using ::bluetooth::audio::aidl::a2dp::codec::A2dpAacToHalConfig;
using ::bluetooth::audio::aidl::a2dp::codec::A2dpAptxToHalConfig;
using ::bluetooth::audio::aidl::a2dp::codec::A2dpCodecToHalBitsPerSample;
using ::bluetooth::audio::aidl::a2dp::codec::A2dpCodecToHalChannelMode;
using ::bluetooth::audio::aidl::a2dp::codec::A2dpCodecToHalSampleRate;
using ::bluetooth::audio::aidl::a2dp::codec::A2dpLdacToHalConfig;
using ::bluetooth::audio::aidl::a2dp::codec::A2dpOpusToHalConfig;
using ::bluetooth::audio::aidl::a2dp::codec::A2dpSbcToHalConfig;

static bool a2dp_get_selected_hal_codec_config(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                                               CodecConfiguration* codec_config) {
  btav_a2dp_codec_config_t current_codec = a2dp_config->getCodecConfig();
  switch (current_codec.codec_type) {
    case BTAV_A2DP_CODEC_INDEX_SOURCE_SBC:
      [[fallthrough]];
    case BTAV_A2DP_CODEC_INDEX_SINK_SBC: {
      if (!A2dpSbcToHalConfig(codec_config, a2dp_config)) {
        return false;
      }
      break;
    }
    case BTAV_A2DP_CODEC_INDEX_SOURCE_AAC:
      [[fallthrough]];
    case BTAV_A2DP_CODEC_INDEX_SINK_AAC: {
      if (!A2dpAacToHalConfig(codec_config, a2dp_config)) {
        return false;
      }
      break;
    }
    case BTAV_A2DP_CODEC_INDEX_SOURCE_APTX:
      [[fallthrough]];
    case BTAV_A2DP_CODEC_INDEX_SOURCE_APTX_HD: {
      if (!A2dpAptxToHalConfig(codec_config, a2dp_config)) {
        return false;
      }
      break;
    }
    case BTAV_A2DP_CODEC_INDEX_SOURCE_LDAC: {
      if (!A2dpLdacToHalConfig(codec_config, a2dp_config)) {
        return false;
      }
      break;
    }
    case BTAV_A2DP_CODEC_INDEX_SOURCE_OPUS: {
      if (!A2dpOpusToHalConfig(codec_config, a2dp_config)) {
        return false;
      }
      break;
    }
    case BTAV_A2DP_CODEC_INDEX_MAX:
      [[fallthrough]];
    default:
      log::error("Unknown codec_type={}", current_codec.codec_type);
      return false;
  }
  codec_config->encodedAudioBitrate = a2dp_config->getTrackBitRate();
  codec_config->peerMtu = peer_mtu;
  log::info("CodecConfiguration={}", codec_config->toString());
  return true;
}

static bool a2dp_get_selected_hal_pcm_config(A2dpCodecConfig* a2dp_codec_configs,
                                             int preferred_encoding_interval_us,
                                             PcmConfiguration* pcm_config) {
  if (pcm_config == nullptr) {
    return false;
  }

  btav_a2dp_codec_config_t current_codec = a2dp_codec_configs->getCodecConfig();
  pcm_config->sampleRateHz = A2dpCodecToHalSampleRate(current_codec);
  pcm_config->bitsPerSample = A2dpCodecToHalBitsPerSample(current_codec);
  pcm_config->channelMode = A2dpCodecToHalChannelMode(current_codec);
  pcm_config->dataIntervalUs = preferred_encoding_interval_us;

  return pcm_config->sampleRateHz > 0 && pcm_config->bitsPerSample > 0 &&
         pcm_config->channelMode != ChannelMode::UNKNOWN;
}

static btav_a2dp_codec_channel_mode_t convert_channel_mode(ChannelMode channel_mode) {
  switch (channel_mode) {
    case ChannelMode::MONO:
      return BTAV_A2DP_CODEC_CHANNEL_MODE_MONO;
    case ChannelMode::STEREO:
      return BTAV_A2DP_CODEC_CHANNEL_MODE_STEREO;
    default:
      log::error("unknown channel mode");
      break;
  }
  return BTAV_A2DP_CODEC_CHANNEL_MODE_NONE;
}

static btav_a2dp_codec_sample_rate_t convert_sampling_frequency_hz(int sampling_frequency_hz) {
  switch (sampling_frequency_hz) {
    case 44100:
      return BTAV_A2DP_CODEC_SAMPLE_RATE_44100;
    case 48000:
      return BTAV_A2DP_CODEC_SAMPLE_RATE_48000;
    case 88200:
      return BTAV_A2DP_CODEC_SAMPLE_RATE_88200;
    case 96000:
      return BTAV_A2DP_CODEC_SAMPLE_RATE_96000;
    case 176400:
      return BTAV_A2DP_CODEC_SAMPLE_RATE_176400;
    case 192000:
      return BTAV_A2DP_CODEC_SAMPLE_RATE_192000;
    case 16000:
      return BTAV_A2DP_CODEC_SAMPLE_RATE_16000;
    case 24000:
      return BTAV_A2DP_CODEC_SAMPLE_RATE_24000;
    default:
      log::error("unknown sampling frequency {}", sampling_frequency_hz);
      break;
  }
  return BTAV_A2DP_CODEC_SAMPLE_RATE_NONE;
}

static btav_a2dp_codec_bits_per_sample_t convert_bitdepth(int bitdepth) {
  switch (bitdepth) {
    case 16:
      return BTAV_A2DP_CODEC_BITS_PER_SAMPLE_16;
    case 24:
      return BTAV_A2DP_CODEC_BITS_PER_SAMPLE_24;
    case 32:
      return BTAV_A2DP_CODEC_BITS_PER_SAMPLE_32;
    default:
      log::error("unknown bit depth {}", bitdepth);
      break;
  }
  return BTAV_A2DP_CODEC_BITS_PER_SAMPLE_NONE;
}

static A2dpTransport* get_a2dp_transport(
        ::bluetooth::audio::aidl::a2dp::BluetoothAudioClientInterface* interface) {
  return static_cast<A2dpTransport*>(interface->GetTransportInstance());
}

//=============================================================================
// SoftwareEncoding : AIDL
//=============================================================================

SoftwareEncoding::SoftwareEncoding(
        ::bluetooth::audio::aidl::a2dp::BluetoothAudioClientInterface* audio_interface)
    : interface_(audio_interface) {}

SoftwareEncoding::~SoftwareEncoding() {
  if (interface_) {
    log::verbose("Removing transport and HAL interface");
    auto transport = get_a2dp_transport(interface_);
    delete transport;
    delete interface_;
  }
}

void SoftwareEncoding::StopSession() {
  log::info("");
  if (interface_ == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return;
  }
  interface_->EndSession();
  get_a2dp_transport(interface_)->ResetPendingCmd();
  get_a2dp_transport(interface_)->ResetPresentationPosition();
}

void SoftwareEncoding::StartSession() {
  log::info("");
  if (interface_ == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return;
  }
  interface_->StartSession();
}

void SoftwareEncoding::ConfirmStreamStarted(Status status) {
  if (interface_ == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return;
  }
  log::info("status={}", status);
  auto pending_cmd = get_a2dp_transport(interface_)->GetPendingCmd();
  if (pending_cmd == A2DP_CTRL_CMD_START) {
    interface_->StreamStarted(status);
  } else {
    log::warn("pending={} ignore status={}", pending_cmd, status);
    return;
  }
  if (status != Status::PENDING) {
    get_a2dp_transport(interface_)->ResetPendingCmd();
  }
}

void SoftwareEncoding::ConfirmStreamSuspended(Status status) {
  if (interface_ == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return;
  }
  log::info("status={}", status);
  auto pending_cmd = get_a2dp_transport(interface_)->GetPendingCmd();
  if (pending_cmd == A2DP_CTRL_CMD_SUSPEND) {
    interface_->StreamSuspended(status);
  } else if (pending_cmd == A2DP_CTRL_CMD_STOP) {
    log::info("A2DP_CTRL_CMD_STOP status={}", status);
  } else {
    log::warn("pending={} ignore status={}", pending_cmd, status);
    return;
  }
  if (status != Status::PENDING) {
    get_a2dp_transport(interface_)->ResetPendingCmd();
  }
}

void SoftwareEncoding::SetRemoteDelay(uint16_t delay_report) {
  if (interface_ == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return;
  }
  log::verbose("Delay: {} ms", static_cast<float>(delay_report / 10.0));
  get_a2dp_transport(interface_)->SetRemoteDelay(delay_report);
}

void SoftwareEncoding::SetLowLatencyMode(bool allowed) {
  if (interface_ == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return;
  }
  std::vector<LatencyMode> latency_modes = {LatencyMode::FREE};
  if (allowed) {
    latency_modes.push_back(LatencyMode::LOW_LATENCY);
  }
  log::verbose("Low Latency: {}", allowed ? "allowed" : "prohibited");
  interface_->SetAllowedLatencyModes(latency_modes);
}

size_t SoftwareEncoding::Read(uint8_t* p_buf, uint32_t len) {
  if (interface_ == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return 0;
  }
  return interface_->ReadAudioData(p_buf, len);
}

bool SoftwareEncoding::UpdateAudioConfigToHal(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                                              int preferred_encoding_interval_us) {
  log::info("");

  log::assert_that(a2dp_config != nullptr, "received invalid codec configuration");
  if (interface_ == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return false;
  }

  // Fallback to legacy offloading path.
  CodecConfiguration codec_config{};

  if (!a2dp_get_selected_hal_codec_config(a2dp_config, peer_mtu, &codec_config)) {
    log::error("Failed to get CodecConfiguration");
    return false;
  }

  AudioConfiguration audio_config{};
  PcmConfiguration pcm_config{};
  if (!a2dp_get_selected_hal_pcm_config(a2dp_config, preferred_encoding_interval_us, &pcm_config)) {
    log::error("Failed to get PcmConfiguration");
    return false;
  }
  audio_config.set<AudioConfiguration::pcmConfig>(pcm_config);

  return interface_->UpdateAudioConfig(audio_config);
}

bool SoftwareEncoding::IsCodecSupportedByHardwareOffload(A2dpCodecConfig* a2dp_config,
                                                         uint16_t peer_mtu) {
  CodecConfiguration codec_config{};
  if (!a2dp_get_selected_hal_codec_config(a2dp_config, peer_mtu, &codec_config)) {
    log::error("Failed to get CodecConfiguration");
    return false;
  }
  return bluetooth::audio::aidl::a2dp::codec::IsCodecOffloadingEnabled(codec_config);
}

//=============================================================================
// HardwareOffloadEncoding : AIDL
//=============================================================================

HardwareOffloadEncoding::HardwareOffloadEncoding(
        ::bluetooth::audio::aidl::a2dp::BluetoothAudioClientInterface* audio_interface,
        std::unique_ptr<::bluetooth::audio::aidl::a2dp::ProviderInfo> provider_info)
    : provider_info_(std::move(provider_info)), interface_(audio_interface) {}

HardwareOffloadEncoding::~HardwareOffloadEncoding() {
  if (interface_) {
    log::verbose("Removing transport and HAL interface");
    auto transport = get_a2dp_transport(interface_);
    delete transport;
    delete interface_;
  }
}

void HardwareOffloadEncoding::StopSession() {
  log::info("");
  if (interface_ == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return;
  }
  interface_->EndSession();
  get_a2dp_transport(interface_)->ResetPendingCmd();
  get_a2dp_transport(interface_)->ResetPresentationPosition();
}

void HardwareOffloadEncoding::StartSession() {
  log::info("");
  if (interface_ == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return;
  }
  interface_->StartSession();
}

void HardwareOffloadEncoding::ConfirmStreamStarted(Status status) {
  if (interface_ == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return;
  }
  log::info("status={}", status);
  auto pending_cmd = get_a2dp_transport(interface_)->GetPendingCmd();
  if (pending_cmd == A2DP_CTRL_CMD_START) {
    interface_->StreamStarted(status);
  } else {
    log::warn("pending={} ignore status={}", pending_cmd, status);
    return;
  }
  if (status != Status::PENDING) {
    get_a2dp_transport(interface_)->ResetPendingCmd();
  }
}

void HardwareOffloadEncoding::ConfirmStreamSuspended(Status status) {
  if (interface_ == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return;
  }
  log::info("status={}", status);
  auto pending_cmd = get_a2dp_transport(interface_)->GetPendingCmd();
  if (pending_cmd == A2DP_CTRL_CMD_SUSPEND) {
    interface_->StreamSuspended(status);
  } else if (pending_cmd == A2DP_CTRL_CMD_STOP) {
    log::info("A2DP_CTRL_CMD_STOP status={}", status);
  } else {
    log::warn("pending={} ignore status={}", pending_cmd, status);
    return;
  }
  if (status != Status::PENDING) {
    get_a2dp_transport(interface_)->ResetPendingCmd();
  }
}

void HardwareOffloadEncoding::SetRemoteDelay(uint16_t delay_report) {
  if (interface_ == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return;
  }
  log::verbose("Delay: {} ms", static_cast<float>(delay_report / 10.0));
  get_a2dp_transport(interface_)->SetRemoteDelay(delay_report);
}

void HardwareOffloadEncoding::SetLowLatencyMode(bool allowed) {
  if (interface_ == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return;
  }
  std::vector<LatencyMode> latency_modes = {LatencyMode::FREE};
  if (allowed) {
    latency_modes.push_back(LatencyMode::LOW_LATENCY);
  }
  log::verbose("Low Latency: {}", allowed ? "allowed" : "prohibited");
  interface_->SetAllowedLatencyModes(latency_modes);
}

bool HardwareOffloadEncoding::UpdateAudioConfigToHal(
        A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
        [[maybe_unused]] int preferred_encoding_interval_us) {
  log::info("");
  log::assert_that(a2dp_config != nullptr, "received invalid codec configuration");
  if (interface_ == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return false;
  }

  if (SupportsCodec(a2dp_config->codecIndex())) {
    // The codec is supported in the provider info (AIDL v4).
    // In this case, the codec is offloaded, and the configuration passed
    // as A2dpStreamConfiguration to the UpdateAudioConfig() interface
    // method.
    uint8_t codec_info[AVDT_CODEC_SIZE];
    A2dpStreamConfiguration a2dp_stream_configuration;

    a2dp_config->copyOutOtaCodecConfig(codec_info);
    a2dp_stream_configuration.peerMtu = peer_mtu;
    a2dp_stream_configuration.codecId =
            provider_info_->GetCodec(a2dp_config->codecIndex()).value()->id;

    size_t parameters_start = 0;
    size_t parameters_end = 0;
    switch (a2dp_config->codecIndex()) {
      case BTAV_A2DP_CODEC_INDEX_SOURCE_SBC:
      case BTAV_A2DP_CODEC_INDEX_SOURCE_AAC:
        parameters_start = 3;
        parameters_end = 1 + codec_info[0];
        break;
      default:
        parameters_start = 9;
        parameters_end = 1 + codec_info[0];
        break;
    }

    a2dp_stream_configuration.configuration.insert(a2dp_stream_configuration.configuration.end(),
                                                   codec_info + parameters_start,
                                                   codec_info + parameters_end);

    return interface_->UpdateAudioConfig(AudioConfiguration(a2dp_stream_configuration));
  }

  // Fallback to legacy offloading path.
  CodecConfiguration codec_config{};
  if (!a2dp_get_selected_hal_codec_config(a2dp_config, peer_mtu, &codec_config)) {
    log::error("Failed to get CodecConfiguration");
    return false;
  }

  AudioConfiguration audio_config{};
  audio_config.set<AudioConfiguration::a2dpConfig>(codec_config);

  return interface_->UpdateAudioConfig(audio_config);
}

std::optional<btav_a2dp_codec_index_t> HardwareOffloadEncoding::SinkCodecIndex(
        const uint8_t* p_codec_info) {
  return provider_info_ ? provider_info_->SinkCodecIndex(p_codec_info) : std::nullopt;
}

std::optional<btav_a2dp_codec_index_t> HardwareOffloadEncoding::SourceCodecIndex(
        const uint8_t* p_codec_info) {
  return provider_info_ ? provider_info_->SourceCodecIndex(p_codec_info) : std::nullopt;
}

std::optional<const char*> HardwareOffloadEncoding::CodecIndexStr(
        btav_a2dp_codec_index_t codec_index) {
  return provider_info_ ? provider_info_->CodecIndexStr(codec_index) : std::nullopt;
}

bool HardwareOffloadEncoding::SupportsCodec(btav_a2dp_codec_index_t codec_index) {
  return provider_info_ ? provider_info_->SupportsCodec(codec_index) : false;
}

bool HardwareOffloadEncoding::CodecInfo(btav_a2dp_codec_index_t codec_index,
                                        bluetooth::a2dp::CodecId* codec_id, uint8_t* codec_info,
                                        btav_a2dp_codec_config_t* codec_config) {
  return provider_info_ ? provider_info_->CodecCapabilities(codec_index, codec_id, codec_info,
                                                            codec_config)
                        : false;
}

std::optional<::bluetooth::audio::a2dp::provider::a2dp_configuration>
HardwareOffloadEncoding::GetA2dpConfiguration(
        RawAddress peer_address,
        std::vector<::bluetooth::audio::a2dp::provider::a2dp_remote_capabilities> const&
                remote_seps,
        btav_a2dp_codec_config_t const& user_preferences) {
  if (provider_info_ == nullptr) {
    return std::nullopt;
  }

  if (interface_ == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return std::nullopt;
  }

  using ::aidl::android::hardware::bluetooth::audio::A2dpRemoteCapabilities;
  using ::aidl::android::hardware::bluetooth::audio::CodecId;

  // Convert the remote audio capabilities to the exchange format used
  // by the HAL.
  std::vector<A2dpRemoteCapabilities> a2dp_remote_capabilities;
  for (auto const& sep : remote_seps) {
    size_t capabilities_start = 0;
    size_t capabilities_end = 0;
    CodecId id;
    switch (sep.capabilities[2]) {
      case A2DP_MEDIA_CT_SBC:
      case A2DP_MEDIA_CT_AAC: {
        id = CodecId::make<CodecId::a2dp>(static_cast<CodecId::A2dp>(sep.capabilities[2]));
        capabilities_start = 3;
        capabilities_end = 1 + sep.capabilities[0];
        break;
      }
      case A2DP_MEDIA_CT_NON_A2DP: {
        uint32_t vendor_id = (static_cast<uint32_t>(sep.capabilities[3]) << 0) |
                             (static_cast<uint32_t>(sep.capabilities[4]) << 8) |
                             (static_cast<uint32_t>(sep.capabilities[5]) << 16) |
                             (static_cast<uint32_t>(sep.capabilities[6]) << 24);
        uint16_t codec_id = (static_cast<uint16_t>(sep.capabilities[7]) << 0) |
                            (static_cast<uint16_t>(sep.capabilities[8]) << 8);
        id = CodecId::make<CodecId::vendor>(
                CodecId::Vendor({.id = (int32_t)vendor_id, .codecId = codec_id}));
        capabilities_start = 9;
        capabilities_end = 1 + sep.capabilities[0];
        break;
      }
      default:
        continue;
    }
    A2dpRemoteCapabilities& capabilities = a2dp_remote_capabilities.emplace_back();
    capabilities.seid = sep.seid;
    capabilities.id = id;
    capabilities.capabilities.insert(capabilities.capabilities.end(),
                                     sep.capabilities + capabilities_start,
                                     sep.capabilities + capabilities_end);
  }

  // Convert the user preferences into a configuration hint.
  A2dpConfigurationHint hint;
  hint.bdAddr = peer_address.ToArray();
  auto& codecParameters = hint.codecParameters.emplace();
  switch (user_preferences.channel_mode) {
    case BTAV_A2DP_CODEC_CHANNEL_MODE_MONO:
      codecParameters.channelMode = ChannelMode::MONO;
      break;
    case BTAV_A2DP_CODEC_CHANNEL_MODE_STEREO:
      codecParameters.channelMode = ChannelMode::STEREO;
      break;
    default:
      break;
  }
  switch (user_preferences.sample_rate) {
    case BTAV_A2DP_CODEC_SAMPLE_RATE_44100:
      codecParameters.samplingFrequencyHz = 44100;
      break;
    case BTAV_A2DP_CODEC_SAMPLE_RATE_48000:
      codecParameters.samplingFrequencyHz = 48000;
      break;
    case BTAV_A2DP_CODEC_SAMPLE_RATE_88200:
      codecParameters.samplingFrequencyHz = 88200;
      break;
    case BTAV_A2DP_CODEC_SAMPLE_RATE_96000:
      codecParameters.samplingFrequencyHz = 96000;
      break;
    case BTAV_A2DP_CODEC_SAMPLE_RATE_176400:
      codecParameters.samplingFrequencyHz = 176400;
      break;
    case BTAV_A2DP_CODEC_SAMPLE_RATE_192000:
      codecParameters.samplingFrequencyHz = 192000;
      break;
    case BTAV_A2DP_CODEC_SAMPLE_RATE_16000:
      codecParameters.samplingFrequencyHz = 16000;
      break;
    case BTAV_A2DP_CODEC_SAMPLE_RATE_24000:
      codecParameters.samplingFrequencyHz = 24000;
      break;
    default:
      break;
  }
  switch (user_preferences.bits_per_sample) {
    case BTAV_A2DP_CODEC_BITS_PER_SAMPLE_16:
      codecParameters.bitdepth = 16;
      break;
    case BTAV_A2DP_CODEC_BITS_PER_SAMPLE_24:
      codecParameters.bitdepth = 24;
      break;
    case BTAV_A2DP_CODEC_BITS_PER_SAMPLE_32:
      codecParameters.bitdepth = 32;
      break;
    default:
      break;
  }

  log::info("remote capabilities:");
  for (auto const& sep : a2dp_remote_capabilities) {
    log::info("- {}", sep.toString());
  }
  log::info("hint: {}", hint.toString());

  // Invoke the HAL GetAdpCapabilities method with the
  // remote capabilities.
  auto result = interface_->GetA2dpConfiguration(a2dp_remote_capabilities, hint);

  // Convert the result configuration back to the stack's format.
  if (!result.has_value()) {
    log::info("provider cannot resolve the a2dp configuration");
    return std::nullopt;
  }

  log::info("provider selected {}", result->toString());

  ::bluetooth::audio::a2dp::provider::a2dp_configuration a2dp_configuration;
  a2dp_configuration.remote_seid = result->remoteSeid;
  a2dp_configuration.vendor_specific_parameters = result->parameters.vendorSpecificParameters;
  ProviderInfo::BuildCodecCapabilities(result->id, result->configuration,
                                       a2dp_configuration.codec_config);
  a2dp_configuration.codec_parameters.codec_type =
          provider_info_->SourceCodecIndex(result->id).value();
  a2dp_configuration.codec_parameters.channel_mode =
          convert_channel_mode(result->parameters.channelMode);
  a2dp_configuration.codec_parameters.sample_rate =
          convert_sampling_frequency_hz(result->parameters.samplingFrequencyHz);
  a2dp_configuration.codec_parameters.bits_per_sample =
          convert_bitdepth(result->parameters.bitdepth);

  return std::make_optional(a2dp_configuration);
}

tA2DP_STATUS HardwareOffloadEncoding::ParseA2dpConfiguration(
        btav_a2dp_codec_index_t codec_index, const uint8_t* codec_info,
        btav_a2dp_codec_config_t* codec_parameters,
        std::vector<uint8_t>* vendor_specific_parameters) {
  std::vector<uint8_t> configuration;
  CodecParameters codec_parameters_aidl;

  if (provider_info_ == nullptr) {
    log::error("provider_info_ is null");
    return A2DP_FAIL;
  }

  if (interface_ == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return A2DP_FAIL;
  }

  auto codec = provider_info_->GetCodec(codec_index);
  if (!codec.has_value()) {
    log::error("codec index not recognized by provider");
    return A2DP_FAIL;
  }

  std::copy(codec_info, codec_info + AVDT_CODEC_SIZE, std::back_inserter(configuration));

  auto a2dp_status = interface_->ParseA2dpConfiguration(codec.value()->id, configuration,
                                                        &codec_parameters_aidl);

  if (!a2dp_status.has_value()) {
    log::error("provider failed to parse configuration");
    return A2DP_FAIL;
  }

  if (codec_parameters != nullptr) {
    codec_parameters->channel_mode = convert_channel_mode(codec_parameters_aidl.channelMode);
    codec_parameters->sample_rate =
            convert_sampling_frequency_hz(codec_parameters_aidl.samplingFrequencyHz);
    codec_parameters->bits_per_sample = convert_bitdepth(codec_parameters_aidl.bitdepth);
  }

  if (vendor_specific_parameters != nullptr) {
    *vendor_specific_parameters = codec_parameters_aidl.vendorSpecificParameters;
  }

  return static_cast<tA2DP_STATUS>(a2dp_status.value());
}

bool HardwareOffloadEncoding::IsCodecSupportedByHardwareOffload(A2dpCodecConfig* a2dp_config,
                                                                uint16_t peer_mtu) {
  CodecConfiguration codec_config{};
  if (!a2dp_get_selected_hal_codec_config(a2dp_config, peer_mtu, &codec_config)) {
    log::error("Failed to get CodecConfiguration");
    return false;
  }
  return bluetooth::audio::aidl::a2dp::codec::IsCodecOffloadingEnabled(codec_config);
}

}  // namespace a2dp
}  // namespace aidl
}  // namespace audio
}  // namespace bluetooth
