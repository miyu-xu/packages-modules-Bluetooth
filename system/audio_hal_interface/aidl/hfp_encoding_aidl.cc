/*
 * Copyright 2023 The Android Open Source Project
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
#include "aidl/android/hardware/bluetooth/audio/AudioConfiguration.h"
#include "aidl/android/hardware/bluetooth/audio/ChannelMode.h"
#include "aidl/android/hardware/bluetooth/audio/CodecId.h"
#include "aidl/android/hardware/bluetooth/audio/HfpConfiguration.h"
#include "aidl/android/hardware/bluetooth/audio/PcmConfiguration.h"
#include "aidl/hfp_transport.h"
#include "aidl/transport_instance.h"
#include "hardware/bluetooth_headset_interface.h"
#define LOG_TAG "BTAudioA2dpAIDL"

#include "bta/ag/bta_ag_int.h"
#include "btm_api_types.h"
#include "hfp_encoding_aidl.h"
#include "types/raw_address.h"

namespace bluetooth {
namespace audio {
namespace aidl {
namespace hfp {

using ::aidl::android::hardware::bluetooth::audio::ChannelMode;
using ::aidl::android::hardware::bluetooth::audio::CodecId;
using ::aidl::android::hardware::bluetooth::audio::HfpConfiguration;

namespace {

tHFP_CTRL_CMD HfpTransport::hfp_pending_cmd_ = HFP_CTRL_CMD_NONE;

BluetoothAudioCtrlAck hfp_ack_to_bt_audio_ctrl_ack(tHFP_CTRL_ACK ack) {
  switch (ack) {
    case HFP_CTRL_ACK_SUCCESS:
      return BluetoothAudioCtrlAck::SUCCESS_FINISHED;
    case HFP_CTRL_ACK_PENDING:
      return BluetoothAudioCtrlAck::PENDING;
    case HFP_CTRL_ACK_FAILURE:
      return BluetoothAudioCtrlAck::FAILURE;
    default:
      return BluetoothAudioCtrlAck::FAILURE;
  }
}

HfpTransport::HfpTransport(SessionType sessionType)
    : IBluetoothSinkTransportInstance(sessionType, (AudioConfiguration){}),
      total_bytes_read_(0),
      data_position_({}) {
  hfp_pending_cmd_ = HFP_CTRL_CMD_NONE;
}

tBTA_AG_SCB* get_hfp_active_device_callback() {
  const RawAddress& addr = bta_ag_get_active_device();
  if (addr.IsEmpty()) {
    LOG(ERROR) << __func__ << ": No active device found";
    return nullptr;
  }
  auto idx = bta_ag_idx_by_bdaddr(&addr);
  if (idx == 0) {
    LOG(ERROR) << __func__ << ": No index found for active device";
    return nullptr;
  }
  auto cb = bta_ag_scb_by_idx(idx);
  if (cb == nullptr) {
    LOG(ERROR) << __func__ << ": No callback for the active device";
    return nullptr;
  }
  return cb;
}

BluetoothAudioCtrlAck HfpTransport::StartRequest(bool is_low_latency) {
  if (hfp_pending_cmd_ == HFP_CTRL_CMD_START) {
    LOG(INFO) << __func__ << ": HFP_CTRL_CMD_START in progress";
    return hfp_ack_to_bt_audio_ctrl_ack(HFP_CTRL_ACK_PENDING);
  } else if (hfp_pending_cmd_ != HFP_CTRL_CMD_NONE) {
    LOG(WARNING) << __func__ << ": busy in pending_cmd=" << hfp_pending_cmd_;
    return hfp_ack_to_bt_audio_ctrl_ack(HFP_CTRL_ACK_FAILURE);
  }

  auto cb = get_hfp_active_device_callback();
  if (cb == nullptr) return hfp_ack_to_bt_audio_ctrl_ack(HFP_CTRL_ACK_FAILURE);

  if (bta_ag_sco_is_open(cb)) {
    // Already started, ACK back immediately.
    return hfp_ack_to_bt_audio_ctrl_ack(HFP_CTRL_ACK_SUCCESS);
  }

  /* Post start SCO event and wait for sco to open */
  hfp_pending_cmd_ = HFP_CTRL_CMD_START;
  bluetooth::headset::StartSco();
  hfp_pending_cmd_ = HFP_CTRL_CMD_NONE;

  return hfp_ack_to_bt_audio_ctrl_ack(HFP_CTRL_ACK_SUCCESS);
}

void HfpTransport::StopRequest() {
  LOG(INFO) << __func__ << ": handling";
  hfp_pending_cmd_ = HFP_CTRL_CMD_STOP;
  bluetooth::headset::StopSco();
}

void HfpTransport::ResetPendingCmd() { hfp_pending_cmd_ = HFP_CTRL_CMD_NONE; }

uint8_t HfpTransport::GetPendingCmd() const { return hfp_pending_cmd_; }

// Unimplemented functions
void HfpTransport::SetLowLatency(bool is_low_latency) {}

void HfpTransport::LogBytesRead(size_t bytes_read) {}

bool HfpTransport::GetPresentationPosition(uint64_t* remote_delay_report_ns,
                                           uint64_t* total_bytes_read,
                                           timespec* data_position) {
  return false;
}

void HfpTransport::SourceMetadataChanged(
    const source_metadata_v7_t& source_metadata) {}

void HfpTransport::SinkMetadataChanged(const sink_metadata_v7_t&) {}

void HfpTransport::ResetPresentationPosition() {}

BluetoothAudioCtrlAck HfpTransport::SuspendRequest() {
  return hfp_ack_to_bt_audio_ctrl_ack(HFP_CTRL_ACK_UNSUPPORTED);
}

// Common interface to call-out into Bluetooth Audio HAL
BluetoothAudioSinkClientInterface* software_hal_interface = nullptr;
BluetoothAudioSinkClientInterface* offloading_hal_interface = nullptr;
BluetoothAudioSinkClientInterface* active_hal_interface = nullptr;

}  // namespace

bool is_hal_enabled() { return active_hal_interface != nullptr; }

// Check if new bluetooth_audio is running with offloading encoders
bool is_hal_offloading() {
  if (!is_hal_enabled()) {
    return false;
  }
  return active_hal_interface->GetTransportInstance()->GetSessionType() ==
         SessionType::HFP_HARDWARE_OFFLOAD_DATAPATH;
}

bool init(bluetooth::common::MessageLoopThread* message_loop) {
  LOG(INFO) << __func__;

  if (!BluetoothAudioClientInterface::is_aidl_available()) {
    LOG(ERROR) << __func__
               << ": BluetoothAudio AIDL implementation does not exist";
    return false;
  }

  auto hfp_sink = new HfpTransport(SessionType::HFP_SOFTWARE_ENCODING_DATAPATH);
  software_hal_interface =
      new BluetoothAudioSinkClientInterface(hfp_sink, message_loop);
  if (!software_hal_interface->IsValid()) {
    LOG(WARNING) << __func__ << ": BluetoothAudio HAL for A2DP is invalid?!";
    delete software_hal_interface;
    software_hal_interface = nullptr;
    delete hfp_sink;
    return false;
  }

  // Prepare offload hal interface.
  if (bta_ag_get_sco_offload_enabled()) {
    hfp_sink = new HfpTransport(SessionType::HFP_HARDWARE_OFFLOAD_DATAPATH);
    offloading_hal_interface =
        new BluetoothAudioSinkClientInterface(hfp_sink, message_loop);
    if (!offloading_hal_interface->IsValid()) {
      LOG(FATAL) << __func__
                 << ": BluetoothAudio HAL for A2DP offloading is invalid?!";
      delete offloading_hal_interface;
      offloading_hal_interface = nullptr;
      delete hfp_sink;
      hfp_sink = static_cast<HfpTransport*>(
          software_hal_interface->GetTransportInstance());
      delete software_hal_interface;
      software_hal_interface = nullptr;
      delete hfp_sink;
      return false;
    }
  }

  active_hal_interface =
      (offloading_hal_interface != nullptr ? offloading_hal_interface
                                           : software_hal_interface);

  return true;
}

// Clean up BluetoothAudio HAL
void cleanup() {
  if (!is_hal_enabled()) return;
  end_session();

  auto hfp_sink = active_hal_interface->GetTransportInstance();
  static_cast<HfpTransport*>(hfp_sink)->ResetPendingCmd();
  static_cast<HfpTransport*>(hfp_sink)->ResetPresentationPosition();
  active_hal_interface = nullptr;

  hfp_sink = software_hal_interface->GetTransportInstance();
  delete software_hal_interface;
  software_hal_interface = nullptr;
  delete hfp_sink;
  if (offloading_hal_interface != nullptr) {
    hfp_sink = offloading_hal_interface->GetTransportInstance();
    delete offloading_hal_interface;
    offloading_hal_interface = nullptr;
    delete hfp_sink;
  }
}

CodecId get_codec_id_by_peer_codec(tBTA_AG_PEER_CODEC sco_codec) {
  if (sco_codec & BTM_SCO_CODEC_LC3) return CodecId::Core::LC3;
  if (sco_codec & BTM_SCO_CODEC_MSBC) return CodecId::Core::MSBC;
  if (sco_codec & BTM_SCO_CODEC_CVSD) return CodecId::Core::CVSD;
  // Unknown vendor codec otherwise
  CodecId codec_id = CodecId::Vendor();
  return codec_id;
}

HfpConfiguration get_hfp_config_from_peer_codec(tBTA_AG_PEER_CODEC sco_codec,
                                                uint16_t conn_handle) {
  HfpConfiguration configuration;
  configuration.connectionHandle = conn_handle;
  configuration.codecId = get_codec_id_by_peer_codec(sco_codec);
  if (configuration.codecId.getTag() == CodecId::vendor)
    configuration.controllerCodec = true;
  return configuration;
}

// Set up the codec into BluetoothAudio HAL
bool setup_codec() {
  if (!is_hal_enabled()) {
    LOG(ERROR) << __func__ << ": BluetoothAudio HAL is not enabled";
    return false;
  }

  auto cb = get_hfp_active_device_callback();
  if (cb == nullptr) return false;

  AudioConfiguration audio_config{};
  if (active_hal_interface->GetTransportInstance()->GetSessionType() ==
      SessionType::HFP_HARDWARE_OFFLOAD_DATAPATH) {
    // Create hfp configuration with information from callback
    audio_config.set<AudioConfiguration::hfpConfig>(
        get_hfp_config_from_peer_codec(cb->sco_codec, cb->conn_handle));
  } else {
    // Populate default pcm configuration
    PcmConfiguration pcm_config;
    pcm_config.sampleRateHz = 16000;
    pcm_config.bitsPerSample = 16;
    pcm_config.channelMode = ChannelMode::STEREO;
    audio_config.set<AudioConfiguration::pcmConfig>(pcm_config);
  }
  // Update audio config, later will start session with config from transport
  if (!active_hal_interface->UpdateAudioConfig(audio_config)) {
    LOG(ERROR) << __func__ << ": Cannot update audio config";
    return false;
  }
  // After setup codec, start session.
  start_session();
  return true;
}

void start_session() {
  if (!is_hal_enabled()) {
    LOG(ERROR) << __func__ << ": BluetoothAudio HAL is not enabled";
    return;
  }
  active_hal_interface->StartSession();
}

void end_session() {
  if (!is_hal_enabled()) {
    LOG(ERROR) << __func__ << ": BluetoothAudio HAL is not enabled";
    return;
  }
  active_hal_interface->EndSession();
  static_cast<HfpTransport*>(active_hal_interface->GetTransportInstance())
      ->ResetPendingCmd();
  static_cast<HfpTransport*>(active_hal_interface->GetTransportInstance())
      ->ResetPresentationPosition();
}

void ack_stream_started(const tHFP_CTRL_ACK& ack) {
  auto ctrl_ack = hfp_ack_to_bt_audio_ctrl_ack(ack);
  LOG(INFO) << __func__ << ": result=" << ctrl_ack;
  auto hfp_sink =
      static_cast<HfpTransport*>(active_hal_interface->GetTransportInstance());
  auto pending_cmd = hfp_sink->GetPendingCmd();
  if (pending_cmd == HFP_CTRL_CMD_START) {
    active_hal_interface->StreamStarted(ctrl_ack);
  } else {
    LOG(WARNING) << __func__ << ": pending=" << pending_cmd
                 << " ignore result=" << ctrl_ack;
    return;
  }
  if (ctrl_ack != BluetoothAudioCtrlAck::PENDING) {
    hfp_sink->ResetPendingCmd();
  }
}

// Read from the FMQ of BluetoothAudio HAL
size_t read(uint8_t* p_buf, uint32_t len) {
  if (!is_hal_enabled()) {
    LOG(ERROR) << __func__ << ": BluetoothAudio HAL is not enabled";
    return 0;
  } else if (is_hal_offloading()) {
    LOG(ERROR) << __func__ << ": session_type="
               << toString(active_hal_interface->GetTransportInstance()
                               ->GetSessionType())
               << " is not HFP_SOFTWARE_ENCODING_DATAPATH";
    return 0;
  }
  return active_hal_interface->ReadAudioData(p_buf, len);
}

}  // namespace hfp
}  // namespace aidl
}  // namespace audio
}  // namespace bluetooth
