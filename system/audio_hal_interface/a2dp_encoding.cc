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
#define LOG_TAG "BTAudioA2dpEncodingAIDL"

#include "a2dp_encoding.h"

#include <vector>

#include "aidl/a2dp/a2dp_encoding_aidl.h"
#include "aidl/a2dp/audio_aidl_interfaces.h"
#include "hal_version_manager.h"
#include "hidl/a2dp_encoding_hidl.h"

namespace bluetooth {
namespace audio {
namespace a2dp {

using ::aidl::android::hardware::bluetooth::audio::A2dpStreamConfiguration;
using ::aidl::android::hardware::bluetooth::audio::AudioConfiguration;
using ::aidl::android::hardware::bluetooth::audio::ChannelMode;
using ::aidl::android::hardware::bluetooth::audio::CodecConfiguration;
using ::aidl::android::hardware::bluetooth::audio::PcmConfiguration;
using ::aidl::android::hardware::bluetooth::audio::SessionType;

using ::bluetooth::audio::a2dp::Status;
using ::bluetooth::audio::a2dp::StreamCallbacks;

using ::bluetooth::audio::aidl::a2dp::BluetoothAudioClientInterface;
using ::bluetooth::audio::aidl::a2dp::codec::A2dpAacToHalConfig;
using ::bluetooth::audio::aidl::a2dp::codec::A2dpAptxToHalConfig;
using ::bluetooth::audio::aidl::a2dp::codec::A2dpCodecToHalBitsPerSample;
using ::bluetooth::audio::aidl::a2dp::codec::A2dpCodecToHalChannelMode;
using ::bluetooth::audio::aidl::a2dp::codec::A2dpCodecToHalSampleRate;
using ::bluetooth::audio::aidl::a2dp::codec::A2dpLdacToHalConfig;
using ::bluetooth::audio::aidl::a2dp::codec::A2dpOpusToHalConfig;
using ::bluetooth::audio::aidl::a2dp::codec::A2dpSbcToHalConfig;

/********************************************************************************
* GLOBAL
********************************************************************************/

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

/********************************************************************************
* A2DP CLIENT INTERFACE
********************************************************************************/

A2dpClientInterface* A2dpClientInterface::interface = nullptr;
A2dpClientInterface* A2dpClientInterface::Get() {
  if (A2dpClientInterface::interface == nullptr) {
    A2dpClientInterface::interface = new A2dpClientInterface();
  }

  return A2dpClientInterface::interface;
}

void A2dpClientInterface::Encode::Cleanup() {

// // Clean up BluetoothAudio HAL
// void cleanup() {
  if (active_hal_interface == nullptr) {
    return;
  }
  StopSession();

  auto a2dp_transport = active_hal_interface->GetTransportInstance();
  static_cast<A2dpEncodingTransport*>(a2dp_transport)->ResetPendingCmd();
  static_cast<A2dpEncodingTransport*>(a2dp_transport)->ResetPresentationPosition();
  active_hal_interface = nullptr;

  a2dp_transport = software_hal_interface->GetTransportInstance();
  delete software_hal_interface;
  software_hal_interface = nullptr;
  delete a2dp_transport;
  if (offloading_hal_interface != nullptr) {
    a2dp_transport = offloading_hal_interface->GetTransportInstance();
    delete offloading_hal_interface;
    offloading_hal_interface = nullptr;
    delete a2dp_transport;
  }
}

void A2dpClientInterface::Encode::SetRemoteDelay(uint16_t delay_report) {
// Update A2DP delay report to BluetoothAudio HAL
// void set_remote_delay(uint16_t delay_report) {
  if (active_hal_interface == nullptr) {
    log::info("not ready for DelayReport {} ms", static_cast<float>(delay_report / 10.0));
    remote_delay = delay_report;
    return;
  }
  log::verbose("DELAY {} ms", static_cast<float>(delay_report / 10.0));
  static_cast<A2dpEncodingTransport*>(active_hal_interface->GetTransportInstance())
          ->SetRemoteDelay(delay_report);
}

void A2dpClientInterface::Encode::SetLowLatencyMode(bool allowed) {
// Set low latency buffer mode allowed or disallowed
// void set_low_latency_mode_allowed(bool allowed) {
  is_low_latency_mode_allowed = allowed;
  if (active_hal_interface == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return;
  }
  std::vector<LatencyMode> latency_modes = {LatencyMode::FREE};
  if (is_low_latency_mode_allowed) {
    latency_modes.push_back(LatencyMode::LOW_LATENCY);
  }
  active_hal_interface->SetAllowedLatencyModes(latency_modes);
}

size_t A2dpClientInterface::Encode::Read(uint8_t* p_buf, uint32_t len) {
// Read from the FMQ of BluetoothAudio HAL
// size_t read(uint8_t* p_buf, uint32_t len) {
  if (active_hal_interface == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return 0;
  }
  SessionType session_type = active_hal_interface->GetTransportInstance()->GetSessionType();
  if (session_type == SessionType::A2DP_HARDWARE_OFFLOAD_ENCODING_DATAPATH) {
    log::error("session_type={} is not A2DP_SOFTWARE_ENCODING_DATAPATH", toString(session_type);
    return 0;
  }
  return active_hal_interface->ReadAudioData(p_buf, len);
}

bool A2dpClientInterface::Encode::UpdateAudioConfigToHal(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                 int preferred_encoding_interval_us) {
// Set up the codec into BluetoothAudio HAL
// bool setup_codec(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
//                  int preferred_encoding_interval_us) {
  log::assert_that(a2dp_config != nullptr, "received invalid codec configuration");

  if (active_hal_interface == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return false;
  }

  SessionType session_type = active_hal_interface->GetTransportInstance()->GetSessionType();

  if (GetProvider()->(a2dp_config->codecIndex())) {
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

    if (session_type != SessionType::A2DP_HARDWARE_OFFLOAD_ENCODING_DATAPATH) {
      log::warn("Switching BluetoothAudio HAL to Hardware");
      StopSession();
      active_hal_interface = offloading_hal_interface;
    }

    return active_hal_interface->UpdateAudioConfig(AudioConfiguration(a2dp_stream_configuration));
  }

  // Fallback to legacy offloading path.
  CodecConfiguration codec_config{};

  if (!a2dp_get_selected_hal_codec_config(a2dp_config, peer_mtu, &codec_config)) {
    log::error("Failed to get CodecConfiguration");
    return false;
  }

  bool should_codec_offloading =
          bluetooth::audio::aidl::a2dp::codec::IsCodecOffloadingEnabled(codec_config);
  if (should_codec_offloading && session_type != SessionType::A2DP_HARDWARE_OFFLOAD_ENCODING_DATAPATH) {
    log::warn("Switching BluetoothAudio HAL to Hardware");
    StopSession();
    active_hal_interface = offloading_hal_interface;
  } else if (!should_codec_offloading && (session_type == SessionType::A2DP_HARDWARE_OFFLOAD_ENCODING_DATAPATH)) {
    log::warn("Switching BluetoothAudio HAL to Software");
    StopSession();
    active_hal_interface = software_hal_interface;
  }

  AudioConfiguration audio_config{};
  if (session_type == SessionType::A2DP_HARDWARE_OFFLOAD_ENCODING_DATAPATH) {
    audio_config.set<AudioConfiguration::a2dpConfig>(codec_config);
  } else {
    PcmConfiguration pcm_config{};
    if (!a2dp_get_selected_hal_pcm_config(a2dp_config, preferred_encoding_interval_us,
                                          &pcm_config)) {
      log::error("Failed to get PcmConfiguration");
      return false;
    }
    audio_config.set<AudioConfiguration::pcmConfig>(pcm_config);
  }

  return active_hal_interface->UpdateAudioConfig(audio_config);
}

void A2dpClientInterface::Encode::StartSession() {
// void start_session() {
  if (active_hal_interface == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return;
  }
  std::vector<LatencyMode> latency_modes = {LatencyMode::FREE};
  if (is_low_latency_mode_allowed) {
    latency_modes.push_back(LatencyMode::LOW_LATENCY);
  }
  active_hal_interface->SetAllowedLatencyModes(latency_modes);
  active_hal_interface->StartSession();
}

void A2dpClientInterface::Encode::StopSession() {
// void end_session() {
  if (active_hal_interface == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return;
  }
  active_hal_interface->EndSession();
  static_cast<A2dpEncodingTransport*>(active_hal_interface->GetTransportInstance())->ResetPendingCmd();
  static_cast<A2dpEncodingTransport*>(active_hal_interface->GetTransportInstance())
          ->ResetPresentationPosition();
}

void A2dpClientInterface::Encode::ConfirmStreamStartRequest(Status status) {
// void ack_stream_started(Status ack) {
  if (active_hal_interface == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return;
  }
  log::info("status={}", status);
  auto a2dp_transport = static_cast<A2dpEncodingTransport*>(active_hal_interface->GetTransportInstance());
  auto pending_cmd = a2dp_transport->GetPendingCmd();
  if (pending_cmd == A2DP_CTRL_CMD_START) {
    active_hal_interface->StreamStarted(status);
  } else {
    log::warn("pending={} ignore status={}", pending_cmd, status);
    return;
  }
  if (status != Status::PENDING) {
    a2dp_transport->ResetPendingCmd();
  }
}

void A2dpClientInterface::Encode::ConfirmStreamSuspendRequest(Status status) {
// void ack_stream_suspended(Status ack) {
  if (active_hal_interface == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return;
  }
  log::info("status={}", status);
  auto a2dp_transport = static_cast<A2dpEncodingTransport*>(active_hal_interface->GetTransportInstance());
  auto pending_cmd = a2dp_transport->GetPendingCmd();
  if (pending_cmd == A2DP_CTRL_CMD_SUSPEND) {
    active_hal_interface->StreamSuspended(status);
  } else if (pending_cmd == A2DP_CTRL_CMD_STOP) {
    log::info("A2DP_CTRL_CMD_STOP status={}", status);
  } else {
    log::warn("pending={} ignore status={}", pending_cmd, status);
    return;
  }
  if (status != Status::PENDING) {
    a2dp_transport->ResetPendingCmd();
  }
}

// Opens the HAL client interface of the specified session type and check
// that is is valid. Returns nullptr if the client interface did not open
// properly.
BluetoothAudioClientInterface* A2dpClientInterface::NewHalInterface(SessionType session_type, std::shared_ptr<StreamCallbacks> stream_callbacks) {
  auto a2dp_transport = new A2dpEncodingTransport(session_type, std::move(stream_callbacks));
  auto hal_interface = new BluetoothAudioClientInterface(a2dp_transport);
  if (hal_interface->IsValid()) {
    return hal_interface;
  } else {
    log::error("BluetoothAudio HAL for a2dp is invalid");
    delete a2dp_transport;
    delete hal_interface;
    return nullptr;
  }
}

/// Delete the selected HAL client interface.
void A2dpClientInterface::DeleteHalInterface(BluetoothAudioClientInterface* hal_interface) {
  if (hal_interface == nullptr) {
    return;
  }
  auto a2dp_transport = static_cast<A2dpEncodingTransport*>(hal_interface->GetTransportInstance());
  delete a2dp_transport;
  delete hal_interface;
}

A2dpClientInterface::Encode* A2dpClientInterface::GetEncoder(std::shared_ptr<StreamCallbacks> stream_callbacks,
                    bluetooth::common::MessageLoopThread* /*message_loop*/,
                    std::unique_ptr<::bluetooth::audio::aidl::a2dp::ProviderInfo> provider_info, 
                    bool offload_enabled) {
  log::info("");
  log::assert_that(stream_callbacks, "stream_callbacks == nullptr");

  if (!BluetoothAudioClientInterface::is_aidl_available()) {
    log::error("BluetoothAudio AIDL implementation does not exist");
    return false;
  }

  if (encode_ == nullptr) {
    encode_ = new Encode();
  } else {
    return encode_;
  }

  provider_info_ = std::move(provider_info);

  software_hal_interface = NewHalInterface(SessionType::A2DP_SOFTWARE_ENCODING_DATAPATH, stream_callbacks);
  if (software_hal_interface == nullptr) {
    return false;
  }

  if (offload_enabled && offloading_hal_interface == nullptr) {
    offloading_hal_interface =
            NewHalInterface(SessionType::A2DP_HARDWARE_OFFLOAD_ENCODING_DATAPATH, stream_callbacks);
    if (offloading_hal_interface == nullptr) {
      DeleteHalInterface(software_hal_interface);
      software_hal_interface = nullptr;
      return false;
    }
  }

  active_hal_interface =
          (offloading_hal_interface != nullptr ? offloading_hal_interface : software_hal_interface);

  if (remote_delay != 0) {
    log::info("restore remote_delay {} ms", static_cast<float>(remote_delay / 10.0));
    static_cast<A2dpEncodingTransport*>(active_hal_interface->GetTransportInstance())
            ->SetRemoteDelay(remote_delay);
    remote_delay = 0;
  }
  return true;
}

void A2dpClientInterface::ReleaseEncode() {
  encode_->Cleanup();
  delete encode_;
  encode_ = nullptr;

  provider_info_.reset();
}

/***
 * Lookup the codec info in the list of supported offloaded sink codecs.
 ***/
void A2dpClientInterface::Provider::SinkCodecIndex(const uint8_t* p_codec_info) {
  return provider_info_ ? provider_info_->SinkCodecIndex(p_codec_info) : std::nullopt;
}

/***
 * Lookup the codec info in the list of supported offloaded source codecs.
 ***/
void A2dpClientInterface::Provider::SourceCodecIndex(const uint8_t* p_codec_info) {
  return provider_info_ ? provider_info_->SourceCodecIndex(p_codec_info) : std::nullopt;
}

/***
 * Return the name of the codec which is assigned to the input index.
 * The codec index must be in the ranges
 * BTAV_A2DP_CODEC_INDEX_SINK_EXT_MIN..BTAV_A2DP_CODEC_INDEX_SINK_EXT_MAX or
 * BTAV_A2DP_CODEC_INDEX_SOURCE_EXT_MIN..BTAV_A2DP_CODEC_INDEX_SOURCE_EXT_MAX.
 * Returns nullopt if the codec_index is not assigned or codec extensibility
 * is not supported or enabled.
 ***/
std::optional<const char*> A2dpClientInterface::Provider::CodecIndexStr(btav_a2dp_codec_index_t codec_index){
  return provider_info_ ? provider_info_->CodecIndexStr(codec_index) : std::nullopt;
}

/***
 * Return true if the codec is supported for the session type
 * A2DP_HARDWARE_ENCODING_DATAPATH or A2DP_HARDWARE_DECODING_DATAPATH.
 ***/
bool A2dpClientInterface::Provider::SupportsCodec(btav_a2dp_codec_index_t codec_index) {
  return provider_info_ ? provider_info_->SupportsCodec(codec_index) : false;
}

/***
 * Return the A2DP capabilities for the selected codec.
 ***/
bool A2dpClientInterface::Provider::CodecInfo(btav_a2dp_codec_index_t codec_index, bluetooth::a2dp::CodecId* codec_id,
                          uint8_t* codec_info, btav_a2dp_codec_config_t* codec_config) {
  return provider_info_
                 ? provider_info_->CodecCapabilities(codec_index, codec_id, codec_info, codec_config)
                 : false;
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

/***
 * Query the codec selection fromt the audio HAL.
 * The HAL is expected to pick the best audio configuration based on the
 * discovered remote SEPs.
 ***/
std::optional<::bluetooth::audio::a2dp::provider::a2dp_configuration>
A2dpClientInterface::Provider::GetA2dpConfiguration(RawAddress peer_address,
        std::vector<::bluetooth::audio::a2dp::provider::a2dp_remote_capabilities> const&
                remote_seps,
        btav_a2dp_codec_config_t const& user_preferences) {
  if (!provider_info_) {
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

  if (offloading_hal_interface == nullptr &&
      (offloading_hal_interface = new_hal_interface(
               SessionType::A2DP_HARDWARE_OFFLOAD_ENCODING_DATAPATH)) == nullptr) {
    log::error("the offloading HAL interface cannot be opened");
    return std::nullopt;
  }

  // Invoke the HAL GetAdpCapabilities method with the
  // remote capabilities.
  auto result = offloading_hal_interface->GetA2dpConfiguration(a2dp_remote_capabilities, hint);

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

/***
 * Query the codec parameters from the audio HAL.
 * The HAL is expected to parse the codec configuration
 * received from the peer and decide whether accept
 * the it or not.
 ***/
tA2DP_STATUS A2dpClientInterface::Provider::ParseA2dpConfiguration(btav_a2dp_codec_index_t codec_index,
                                                const uint8_t* codec_info,
                                                btav_a2dp_codec_config_t* codec_parameters,
                                                std::vector<uint8_t>* vendor_specific_parameters) {
  std::vector<uint8_t> configuration;
  CodecParameters codec_parameters_aidl;

  if (!provider_info_) {
    log::error("provider_info_ is null");
    return A2DP_FAIL;
  }

  auto codec = provider_info_->GetCodec(codec_index);
  if (!codec.has_value()) {
    log::error("codec index not recognized by provider");
    return A2DP_FAIL;
  }

  std::copy(codec_info, codec_info + AVDT_CODEC_SIZE, std::back_inserter(configuration));

  auto a2dp_status = offloading_hal_interface->ParseA2dpConfiguration(
          codec.value()->id, configuration, &codec_parameters_aidl);

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

A2dpClientInterface::Provider* A2dpClientInterface::Provider::GetProvider() {
  if (provider_ == nullptr) {
    provider_ = new Provider();
  } else {
    return provider_;
  }
}

void A2dpClientInterface::Provider::ReleaseProvider() {
  provider_.reset();
}



bool update_codec_offloading_capabilities(
        const std::vector<btav_a2dp_codec_config_t>& framework_preference,
        bool supports_a2dp_hw_offload_v2) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    return hidl::a2dp::update_codec_offloading_capabilities(framework_preference);
  }
  return aidl::a2dp::update_codec_offloading_capabilities(framework_preference,
                                                          supports_a2dp_hw_offload_v2);
}

// Check if new bluetooth_audio is enabled
// bool is_hal_enabled() {
//   if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
//     return hidl::a2dp::is_hal_2_0_enabled();
//   }
//   return aidl::a2dp::is_hal_enabled();
// }

// Check if new bluetooth_audio is running with offloading encoders
bool is_hal_offloading() {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    return hidl::a2dp::is_hal_2_0_offloading();
  }
  return aidl::a2dp::is_hal_offloading();
}

// Initialize BluetoothAudio HAL: openProvider
bool init(bluetooth::common::MessageLoopThread* message_loop,
          bluetooth::audio::a2dp::StreamCallbacks const* stream_callbacks, bool offload_enabled) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    return hidl::a2dp::init(message_loop, stream_callbacks, offload_enabled);
  }
  return aidl::a2dp::init(message_loop, stream_callbacks, offload_enabled);
}

// Clean up BluetoothAudio HAL
void cleanup() {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    hidl::a2dp::cleanup();
    return;
  }
  aidl::a2dp::cleanup();
}

// Set up the codec into BluetoothAudio HAL
bool setup_codec(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                 int preferred_encoding_interval_us) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    return hidl::a2dp::setup_codec(a2dp_config, peer_mtu, preferred_encoding_interval_us);
  }
  return aidl::a2dp::setup_codec(a2dp_config, peer_mtu, preferred_encoding_interval_us);
}

// Send command to the BluetoothAudio HAL: StartSession, EndSession,
// StreamStarted, StreamSuspended
void start_session() {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    hidl::a2dp::start_session();
    return;
  }
  aidl::a2dp::start_session();
}

void end_session() {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::AIDL) {
    return aidl::a2dp::end_session();
  }
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    hidl::a2dp::end_session();
    return;
  }
}

void ack_stream_started(Status status) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    hidl::a2dp::ack_stream_started(status);
    return;
  }
  return aidl::a2dp::ack_stream_started(status);
}

void ack_stream_suspended(Status status) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    hidl::a2dp::ack_stream_suspended(status);
    return;
  }
  aidl::a2dp::ack_stream_suspended(status);
}

// Read from the FMQ of BluetoothAudio HAL
size_t read(uint8_t* p_buf, uint32_t len) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    return hidl::a2dp::read(p_buf, len);
  }
  return aidl::a2dp::read(p_buf, len);
}

// Update A2DP delay report to BluetoothAudio HAL
void set_remote_delay(uint16_t delay_report) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::HIDL) {
    hidl::a2dp::set_remote_delay(delay_report);
    return;
  }
  aidl::a2dp::set_remote_delay(delay_report);
}

// Set low latency buffer mode allowed or disallowed
void set_audio_low_latency_mode_allowed(bool allowed) {
  if (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::AIDL) {
    aidl::a2dp::set_low_latency_mode_allowed(allowed);
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
  return (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::AIDL)
                 ? aidl::a2dp::provider::sink_codec_index(p_codec_info)
                 : std::nullopt;
}

// Lookup the codec info in the list of supported offloaded source codecs.
std::optional<btav_a2dp_codec_index_t> source_codec_index(const uint8_t* p_codec_info) {
  return (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::AIDL)
                 ? aidl::a2dp::provider::source_codec_index(p_codec_info)
                 : std::nullopt;
}

// Return the name of the codec which is assigned to the input index.
// The codec index must be in the ranges
// BTAV_A2DP_CODEC_INDEX_SINK_EXT_MIN..BTAV_A2DP_CODEC_INDEX_SINK_EXT_MAX or
// BTAV_A2DP_CODEC_INDEX_SOURCE_EXT_MIN..BTAV_A2DP_CODEC_INDEX_SOURCE_EXT_MAX.
// Returns nullopt if the codec_index is not assigned or codec extensibility
// is not supported or enabled.
std::optional<const char*> codec_index_str(btav_a2dp_codec_index_t codec_index) {
  return (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::AIDL)
                 ? aidl::a2dp::provider::codec_index_str(codec_index)
                 : std::nullopt;
}

// Return true if the codec is supported for the session type
// A2DP_HARDWARE_ENCODING_DATAPATH or A2DP_HARDWARE_DECODING_DATAPATH.
bool supports_codec(btav_a2dp_codec_index_t codec_index) {
  return (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::AIDL)
                 ? aidl::a2dp::provider::supports_codec(codec_index)
                 : false;
}

// Return the A2DP capabilities for the selected codec.
bool codec_info(btav_a2dp_codec_index_t codec_index, bluetooth::a2dp::CodecId* codec_id,
                uint8_t* codec_info, btav_a2dp_codec_config_t* codec_config) {
  return (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::AIDL)
                 ? aidl::a2dp::provider::codec_info(codec_index, codec_id, codec_info, codec_config)
                 : false;
}

// Query the codec selection fromt the audio HAL.
// The HAL is expected to pick the best audio configuration based on the
// discovered remote SEPs.
std::optional<a2dp_configuration> get_a2dp_configuration(
        RawAddress peer_address, std::vector<a2dp_remote_capabilities> const& remote_seps,
        btav_a2dp_codec_config_t const& user_preferences) {
  return (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::AIDL)
                 ? aidl::a2dp::provider::get_a2dp_configuration(peer_address, remote_seps,
                                                                user_preferences)
                 : std::nullopt;
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
  return (HalVersionManager::GetHalTransport() == BluetoothAudioHalTransport::AIDL)
                 ? aidl::a2dp::provider::parse_a2dp_configuration(
                           codec_index, codec_info, codec_parameters, vendor_specific_parameters)
                 : A2DP_FAIL;
}

}  // namespace provider
}  // namespace a2dp
}  // namespace audio
}  // namespace bluetooth
