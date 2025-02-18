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
#define LOG_TAG "bluetooth-a2dp-ahal-hidl"

#include "a2dp_encoding_hidl.h"

#include <bluetooth/log.h>

#include <vector>

#include "a2dp_encoding_hidl_transport.h"
#include "client_interface_hidl.h"
#include "codec_status_hidl.h"
#include "osi/include/properties.h"
#include "types/raw_address.h"

namespace bluetooth {
namespace audio {
namespace hidl {
namespace a2dp {

using ::bluetooth::audio::hidl::AudioConfiguration;
using ::bluetooth::audio::hidl::BitsPerSample;
using ::bluetooth::audio::hidl::ChannelMode;
using ::bluetooth::audio::hidl::PcmParameters;
using ::bluetooth::audio::hidl::SampleRate;
using ::bluetooth::audio::hidl::SessionType;

using ::bluetooth::audio::hidl::codec::A2dpAacToHalConfig;
using ::bluetooth::audio::hidl::codec::A2dpAptxToHalConfig;
using ::bluetooth::audio::hidl::codec::A2dpCodecToHalBitsPerSample;
using ::bluetooth::audio::hidl::codec::A2dpCodecToHalChannelMode;
using ::bluetooth::audio::hidl::codec::A2dpCodecToHalSampleRate;
using ::bluetooth::audio::hidl::codec::A2dpLdacToHalConfig;
using ::bluetooth::audio::hidl::codec::A2dpSbcToHalConfig;
using ::bluetooth::audio::hidl::codec::CodecConfiguration;

using ::bluetooth::audio::a2dp::Status;
using ::bluetooth::audio::a2dp::StreamCallbacks;

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
    case BTAV_A2DP_CODEC_INDEX_MAX:
      [[fallthrough]];
    default:
      log::error("Unknown codec_type={}", current_codec.codec_type);
      *codec_config = ::bluetooth::audio::hidl::codec::kInvalidCodecConfiguration;
      return false;
  }
  codec_config->encodedAudioBitrate = a2dp_config->getTrackBitRate();
  codec_config->peerMtu = peer_mtu;
  log::info("CodecConfiguration={}", toString(*codec_config));
  return true;
}

static bool a2dp_get_selected_hal_pcm_config(A2dpCodecConfig* a2dp_codec_configs,
                                             PcmParameters* pcm_config) {
  if (pcm_config == nullptr) {
    return false;
  }

  btav_a2dp_codec_config_t current_codec = a2dp_codec_configs->getCodecConfig();
  pcm_config->sampleRate = A2dpCodecToHalSampleRate(current_codec);
  pcm_config->bitsPerSample = A2dpCodecToHalBitsPerSample(current_codec);
  pcm_config->channelMode = A2dpCodecToHalChannelMode(current_codec);
  return pcm_config->sampleRate != SampleRate::RATE_UNKNOWN &&
         pcm_config->bitsPerSample != BitsPerSample::BITS_UNKNOWN &&
         pcm_config->channelMode != ChannelMode::UNKNOWN;
}

static A2dpTransport* get_a2dp_transport(
        ::bluetooth::audio::hidl::BluetoothAudioSinkClientInterface* interface) {
  return static_cast<A2dpTransport*>(interface->GetTransportInstance());
}

//=============================================================================
// SoftwareEncoding : HIDL
//=============================================================================

SoftwareEncoding::SoftwareEncoding(
        ::bluetooth::audio::hidl::BluetoothAudioSinkClientInterface* audio_interface)
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
    interface_->StreamStarted(a2dp_ack_to_bt_audio_ctrl_ack(status));
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
    interface_->StreamSuspended(a2dp_ack_to_bt_audio_ctrl_ack(status));
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

size_t SoftwareEncoding::Read(uint8_t* p_buf, uint32_t len) {
  if (interface_ == nullptr) {
    log::error("BluetoothAudio HAL is not enabled");
    return 0;
  }
  return interface_->ReadAudioData(p_buf, len);
}

bool SoftwareEncoding::UpdateAudioConfigToHal(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                                              [[maybe_unused]] int preferred_encoding_interval_us) {
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
  PcmParameters pcm_config{};
  if (!a2dp_get_selected_hal_pcm_config(a2dp_config, &pcm_config)) {
    log::error("Failed to get PcmConfiguration");
    return false;
  }
  audio_config.pcmConfig(pcm_config);
  log::info("");
  return interface_->UpdateAudioConfig(audio_config);
}

bool SoftwareEncoding::IsCodecSupportedByHardwareOffload(A2dpCodecConfig* a2dp_config,
                                                         uint16_t peer_mtu) {
  CodecConfiguration codec_config{};
  if (!a2dp_get_selected_hal_codec_config(a2dp_config, peer_mtu, &codec_config)) {
    log::error("Failed to get CodecConfiguration");
    return false;
  }
  return bluetooth::audio::hidl::codec::IsCodecOffloadingEnabled(codec_config);
}

//=============================================================================
// HardwareOffloadEncoding : HIDL
//=============================================================================

HardwareOffloadEncoding::HardwareOffloadEncoding(
        ::bluetooth::audio::hidl::BluetoothAudioSinkClientInterface* audio_interface)
    : interface_(audio_interface) {}

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
    interface_->StreamStarted(a2dp_ack_to_bt_audio_ctrl_ack(status));
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
    interface_->StreamSuspended(a2dp_ack_to_bt_audio_ctrl_ack(status));
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

bool HardwareOffloadEncoding::UpdateAudioConfigToHal(
        A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
        [[maybe_unused]] int preferred_encoding_interval_us) {
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
  audio_config.codecConfig(codec_config);
  log::info("");
  return interface_->UpdateAudioConfig(audio_config);
}

bool HardwareOffloadEncoding::IsCodecSupportedByHardwareOffload(A2dpCodecConfig* a2dp_config,
                                                                uint16_t peer_mtu) {
  CodecConfiguration codec_config{};
  if (!a2dp_get_selected_hal_codec_config(a2dp_config, peer_mtu, &codec_config)) {
    log::error("Failed to get CodecConfiguration");
    return false;
  }
  return bluetooth::audio::hidl::codec::IsCodecOffloadingEnabled(codec_config);
}

}  // namespace a2dp
}  // namespace hidl
}  // namespace audio
}  // namespace bluetooth
