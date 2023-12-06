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

#include <cstdint>

#define LOG_TAG "BTAudioClientHfpStub"

#include "aidl/client_interface_aidl.h"
#include "aidl/hfp_software_aidl.h"
#include "hal_version_manager.h"
#include "hfp_software.h"
#include "osi/include/log.h"
#include "osi/include/properties.h"

using ::bluetooth::audio::aidl::hfp::HfpSinkTransport;
using AudioConfiguration =
    ::aidl::android::hardware::bluetooth::audio::AudioConfiguration;
using ::aidl::android::hardware::bluetooth::audio::ChannelMode;
using ::aidl::android::hardware::bluetooth::audio::CodecId;
using ::aidl::android::hardware::bluetooth::audio::HfpConfiguration;
using ::aidl::android::hardware::bluetooth::audio::PcmConfiguration;

namespace bluetooth {
namespace audio {
namespace hfp {

// Helper functions
aidl::BluetoothAudioSinkClientInterface* get_aidl_sink_client_interface() {
  return HfpSinkTransport::active_hal_interface;
}

HfpSinkTransport* get_aidl_sink_transport_instance() {
  return HfpSinkTransport::instance_;
}

PcmConfiguration get_default_pcm_configuration() {
  PcmConfiguration pcm_config{
      .sampleRateHz = 8000,
      .channelMode = ChannelMode::MONO,
      .bitsPerSample = 16,
  };
  return pcm_config;
}

HfpConfiguration get_default_hfp_configuration() {
  HfpConfiguration hfp_config{
      .codecId = CodecId::Core::CVSD,
      .connectionHandle = 6,
      .nrec = false,
      .controllerCodec = true,
  };
  return hfp_config;
}

CodecId get_codec_id_by_peer_codec(tBTA_AG_PEER_CODEC sco_codec) {
  if (sco_codec & BTM_SCO_CODEC_LC3) return CodecId::Core::LC3;
  if (sco_codec & BTM_SCO_CODEC_MSBC) return CodecId::Core::MSBC;
  if (sco_codec & BTM_SCO_CODEC_CVSD) return CodecId::Core::CVSD;
  // Unknown vendor codec otherwise
  CodecId codec_id = CodecId::Vendor();
  return codec_id;
}

AudioConfiguration offload_config_to_hal_audio_config(
    const ::hfp::offload_config& offload_config) {
  HfpConfiguration hfp_config{
      .codecId = get_codec_id_by_peer_codec(offload_config.sco_codec),
      .connectionHandle = offload_config.connection_handle,
      .nrec = offload_config.is_nrec,
      .controllerCodec = offload_config.is_controller_codec,
  };
  return AudioConfiguration(hfp_config);
}

bool is_aidl_sink_offload_session() {
  return get_aidl_sink_transport_instance()->GetSessionType() ==
         aidl::SessionType::HFP_HARDWARE_OFFLOAD_DATAPATH;
}

void get_default_audio_configuration(AudioConfiguration* audio_config) {
  AudioConfiguration local_audio_config;
  if (is_aidl_sink_offload_session()) {
    // Populate default HfpConfiguration
    LOG(INFO) << __func__ << ": processing offload session";
    local_audio_config.set<AudioConfiguration::hfpConfig>(
        get_default_hfp_configuration());
  } else {
    // Populate default pcm configuration
    LOG(INFO) << __func__ << ": processing software session";
    local_audio_config.set<AudioConfiguration::pcmConfig>(
        get_default_pcm_configuration());
  }
  *audio_config = local_audio_config;
}

bool is_hal_enabled() {
  return !osi_property_get_bool(BLUETOOTH_AUDIO_HAL_PROP_DISABLED, false);
}

bool is_aidl_enabled() {
  return HalVersionManager::GetHalTransport() ==
             BluetoothAudioHalTransport::AIDL &&
         HalVersionManager::GetHalVersion() >=
             BluetoothAudioHalVersion::VERSION_AIDL_V4;
}

// Sink client implementation
void HfpClientInterface::Sink::Cleanup() {
  LOG(INFO) << __func__ << " sink";
  StopSession();
  if (HfpSinkTransport::instance_) {
    delete HfpSinkTransport::software_hal_interface;
    HfpSinkTransport::software_hal_interface = nullptr;
    if (HfpSinkTransport::offloading_hal_interface != nullptr) {
      delete HfpSinkTransport::offloading_hal_interface;
      HfpSinkTransport::offloading_hal_interface = nullptr;
    }

    delete HfpSinkTransport::instance_;
    HfpSinkTransport::instance_ = nullptr;
  }
}

void HfpClientInterface::Sink::StartSession() {
  if (!is_aidl_enabled()) {
    LOG(WARNING) << __func__ << ": Unsupported HIDL or AIDL version";
    return;
  }
  LOG(INFO) << __func__ << " sink";
  AudioConfiguration* audio_config = nullptr;
  get_default_audio_configuration(audio_config);
  if (!get_aidl_sink_client_interface()->UpdateAudioConfig(*audio_config)) {
    LOG(ERROR) << __func__ << ": cannot update audio config to HAL";
    return;
  }
  get_aidl_sink_client_interface()->StartSession();
}

void HfpClientInterface::Sink::StopSession() {
  if (!is_aidl_enabled()) {
    LOG(WARNING) << __func__ << ": Unsupported HIDL or AIDL version";
    return;
  }
  LOG(INFO) << __func__ << " sink";
  get_aidl_sink_client_interface()->EndSession();
  if (get_aidl_sink_transport_instance()) {
    get_aidl_sink_transport_instance()->ResetPendingCmd();
    get_aidl_sink_transport_instance()->ResetPresentationPosition();
  }
}

void HfpClientInterface::Sink::UpdateAudioConfigToHal(
    const ::hfp::offload_config& offload_config) {
  if (!is_aidl_enabled()) {
    LOG(WARNING) << __func__ << ": Unsupported HIDL or AIDL version";
    return;
  }

  LOG(INFO) << __func__ << " sink";
  get_aidl_sink_client_interface()->UpdateAudioConfig(
      offload_config_to_hal_audio_config(offload_config));
}

size_t HfpClientInterface::Sink::Read(uint8_t* p_buf, uint32_t len) {
  if (!is_aidl_enabled()) {
    LOG(WARNING) << __func__ << ": Unsupported HIDL or AIDL version";
    return 0;
  }
  LOG(INFO) << __func__ << " sink";
  return get_aidl_sink_client_interface()->ReadAudioData(p_buf, len);
}

HfpClientInterface::Sink* HfpClientInterface::GetSink(
    bluetooth::common::MessageLoopThread* message_loop) {
  if (!is_aidl_enabled()) {
    LOG(WARNING) << __func__ << ": Unsupported HIDL or AIDL version";
    return nullptr;
  }

  if (sink_ == nullptr) {
    sink_ = new Sink();
  } else {
    LOG(WARNING) << __func__ << ": Sink is already acquired";
    return nullptr;
  }

  LOG(INFO) << __func__ << " sink";

  HfpSinkTransport::instance_ =
      new HfpSinkTransport(aidl::SessionType::HFP_SOFTWARE_DECODING_DATAPATH);
  HfpSinkTransport::software_hal_interface =
      new aidl::BluetoothAudioSinkClientInterface(HfpSinkTransport::instance_,
                                                  message_loop);
  if (!HfpSinkTransport::software_hal_interface->IsValid()) {
    LOG(WARNING) << __func__ << ": BluetoothAudio HAL for HFP is invalid";
    delete HfpSinkTransport::software_hal_interface;
    HfpSinkTransport::software_hal_interface = nullptr;
    delete HfpSinkTransport::instance_;
    return nullptr;
  }

  // Prepare offload hal interface.
  if (bta_ag_get_sco_offload_enabled()) {
    HfpSinkTransport::instance_ =
        new HfpSinkTransport(aidl::SessionType::HFP_HARDWARE_OFFLOAD_DATAPATH);
    HfpSinkTransport::offloading_hal_interface =
        new aidl::BluetoothAudioSinkClientInterface(HfpSinkTransport::instance_,
                                                    message_loop);
    if (!HfpSinkTransport::offloading_hal_interface->IsValid()) {
      LOG(FATAL) << __func__
                 << ": BluetoothAudio HAL for HFP offloading is invalid";
      delete HfpSinkTransport::offloading_hal_interface;
      HfpSinkTransport::offloading_hal_interface = nullptr;
      delete HfpSinkTransport::instance_;
      HfpSinkTransport::instance_ = static_cast<HfpSinkTransport*>(
          HfpSinkTransport::software_hal_interface->GetTransportInstance());
      delete HfpSinkTransport::software_hal_interface;
      HfpSinkTransport::software_hal_interface = nullptr;
      delete HfpSinkTransport::instance_;
      return nullptr;
    }
  }

  HfpSinkTransport::active_hal_interface =
      (HfpSinkTransport::offloading_hal_interface != nullptr
           ? HfpSinkTransport::offloading_hal_interface
           : HfpSinkTransport::software_hal_interface);

  return sink_;
}

bool HfpClientInterface::ReleaseSink(HfpClientInterface::Sink* sink) {
  if (sink != sink_) {
    LOG(WARNING) << __func__ << ", can't release not acquired sink";
    return false;
  }

  LOG(INFO) << __func__ << " sink";
  if (get_aidl_sink_client_interface()) sink->Cleanup();

  delete sink_;
  sink_ = nullptr;

  return true;
}

}  // namespace hfp
}  // namespace audio
}  // namespace bluetooth
