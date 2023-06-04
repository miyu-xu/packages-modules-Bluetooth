/*
 * Copyright 2021 HIMSA II K/S - www.himsa.com. Represented by EHIMA -
 * www.ehima.com
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

#include "audio_hal_interface/le_audio_software_host.h"

#include <base/logging.h>
#include <errno.h>
#include <grp.h>
#include <sys/stat.h>

#include "audio_hal_interface/le_audio_software.h"
#include "audio_hal_interface/le_audio_software_host_transport.h"
#include "bta/include/bta_le_audio_api.h"
#include "bta/le_audio/codec_manager.h"
#include "osi/include/log.h"
#include "udrv/include/uipc.h"

#define LEA_DATA_READ_POLL_MS 10
#define LEA_HOST_DATA_PATH "/var/run/bluetooth/audio/.lea_data"
// TODO(b/198260375): Make LEA data owner group configurable.
#define LEA_HOST_DATA_GROUP "bluetooth-audio"

namespace {

std::unique_ptr<tUIPC_STATE> lea_uipc = nullptr;

void lea_data_cb(tUIPC_CH_ID, tUIPC_EVENT event) {
  switch (event) {
    case UIPC_OPEN_EVT:
      LOG_INFO("%s: UIPC_OPEN_EVT", __func__);
      /*
       * Read directly from media task from here on (keep callback for
       * connection events.
       */
      UIPC_Ioctl(*lea_uipc, UIPC_CH_ID_AV_AUDIO, UIPC_REG_REMOVE_ACTIVE_READSET,
                 NULL);
      UIPC_Ioctl(*lea_uipc, UIPC_CH_ID_AV_AUDIO, UIPC_SET_READ_POLL_TMO,
                 reinterpret_cast<void*>(LEA_DATA_READ_POLL_MS));
      break;
    case UIPC_CLOSE_EVT:
      LOG_INFO("%s: UIPC_CLOSE_EVT", __func__);
      break;
    default:
      break;
  }
}

static void lea_data_path_open() {
  UIPC_Open(*lea_uipc, UIPC_CH_ID_AV_AUDIO, lea_data_cb, LEA_HOST_DATA_PATH);
  struct group* grp = getgrnam(LEA_HOST_DATA_GROUP);
  chmod(LEA_HOST_DATA_PATH, 0770);
  if (grp) {
    int res = chown(LEA_HOST_DATA_PATH, -1, grp->gr_gid);
    if (res == -1) {
      LOG_ERROR("%s failed: %s", __func__, strerror(errno));
    }
  }
}

}  // namespace

namespace bluetooth {
namespace audio {

namespace le_audio {

// Invoked by audio server when it has audio data to stream.
bool HostStartRequest() {
  host::le_audio::LeAudioSinkTransport::instance->ResetPresentationPosition();
  return host::le_audio::LeAudioSinkTransport::instance->StartRequest();
}

void HostStopRequest() {
  host::le_audio::LeAudioSinkTransport::instance->StopRequest();
}

btle_pcm_parameters GetHostPcmConfig() {
  auto pcm_params = host::le_audio::LeAudioSinkTransport::instance
                        ->LeAudioGetSelectedHalPcmConfig();

  btle_pcm_parameters pcm_config = {
      .data_interval_us = pcm_params.data_interval_us,
      .sample_rate = pcm_params.sample_rate,
      .bits_per_sample = pcm_params.bits_per_sample,
      .channels_count = pcm_params.channels_count,
  };

  return pcm_config;
}

// Invoked by audio server to request audio data streamed from the peer.
bool PeerStartRequest() {
  host::le_audio::LeAudioSinkTransport::instance->ResetPresentationPosition();
  return host::le_audio::LeAudioSourceTransport::instance->StartRequest();
}

void PeerStopRequest() {
  host::le_audio::LeAudioSourceTransport::instance->StopRequest();
}

btle_pcm_parameters GetPeerPcmConfig() {
  auto pcm_params = host::le_audio::LeAudioSourceTransport::instance
                        ->LeAudioGetSelectedHalPcmConfig();

  btle_pcm_parameters pcm_config = {
      .data_interval_us = pcm_params.data_interval_us,
      .sample_rate = pcm_params.sample_rate,
      .bits_per_sample = pcm_params.bits_per_sample,
      .channels_count = pcm_params.channels_count,
  };

  return pcm_config;
}

bool GetHostStreamStarted() {
  return host::le_audio::LeAudioSinkTransport::stream_started;
}

bool GetPeerStreamStarted() {
  return host::le_audio::LeAudioSourceTransport::stream_started;
}

std::vector<::le_audio::set_configurations::AudioSetConfiguration>
get_offload_capabilities() {
  return std::vector<::le_audio::set_configurations::AudioSetConfiguration>(0);
}

int GetAidlInterfaceVersion() { return 0; }

void LeAudioClientInterface::Sink::Cleanup() {
  LOG(INFO) << __func__;

  StopSession();

  delete host::le_audio::LeAudioSinkTransport::instance;
  host::le_audio::LeAudioSinkTransport::instance = nullptr;
}

void LeAudioClientInterface::Sink::SetPcmParameters(
    const PcmParameters& params) {
  LOG(INFO) << __func__ << ", sample_rate: " << params.sample_rate
            << ", bits_per_sample: " << params.bits_per_sample
            << ", channels_count: " << params.channels_count
            << ", data_interval_us: " << params.data_interval_us;

  host::le_audio::LeAudioSinkTransport::instance
      ->LeAudioSetSelectedHalPcmConfig(
          params.sample_rate, params.bits_per_sample, params.channels_count,
          params.data_interval_us);
}

void LeAudioClientInterface::Sink::SetRemoteDelay(uint16_t delay_report_ms) {
  LOG(INFO) << __func__ << ": delay_report_ms=" << delay_report_ms << " ms";

  host::le_audio::LeAudioSinkTransport::instance->SetRemoteDelay(
      delay_report_ms);
}

void LeAudioClientInterface::Sink::StartSession() { LOG(INFO) << __func__; }

void LeAudioClientInterface::Sink::StopSession() {
  LOG(INFO) << __func__;

  host::le_audio::LeAudioSinkTransport::instance->ClearStartRequestState();
  host::le_audio::LeAudioSinkTransport::stream_started = false;
}

void LeAudioClientInterface::Sink::ConfirmStreamingRequest() {
  LOG(INFO) << __func__;

  auto instance = host::le_audio::LeAudioSinkTransport::instance;
  auto start_request_state = instance->GetStartRequestState();

  switch (start_request_state) {
    case StartRequestState::IDLE:
      LOG_WARN(", no pending start stream request");
      return;
    case StartRequestState::PENDING_BEFORE_RESUME:
      LOG_INFO("Response before sending PENDING to audio HAL");
      instance->SetStartRequestState(StartRequestState::CONFIRMED);
      lea_data_path_open();
      return;
    case StartRequestState::PENDING_AFTER_RESUME:
      LOG_INFO("Response after sending PENDING to audio HAL");
      instance->ClearStartRequestState();
      host::le_audio::LeAudioSinkTransport::stream_started = true;
      return;
    case StartRequestState::CONFIRMED:
    case StartRequestState::CANCELED:
      LOG_ERROR("Invalid state, start stream already confirmed");
      return;
  }
}

void LeAudioClientInterface::Sink::CancelStreamingRequest() {
  LOG(INFO) << __func__;

  auto instance = host::le_audio::LeAudioSinkTransport::instance;
  auto start_request_state = instance->GetStartRequestState();

  switch (start_request_state) {
    case StartRequestState::IDLE:
      LOG_WARN(", no pending start stream request");
      return;
    case StartRequestState::PENDING_BEFORE_RESUME:
      LOG_INFO("Response before sending PENDING to audio HAL");
      instance->SetStartRequestState(StartRequestState::CANCELED);
      return;
    case StartRequestState::PENDING_AFTER_RESUME:
      LOG_INFO("Response after sending PENDING to audio HAL");
      instance->ClearStartRequestState();
      host::le_audio::LeAudioSinkTransport::stream_started = false;
      return;
    case StartRequestState::CONFIRMED:
    case StartRequestState::CANCELED:
      LOG_ERROR("Invalid state, start stream already confirmed");
      break;
  }
}

void LeAudioClientInterface::Sink::UpdateAudioConfigToHal(
    const ::le_audio::offload_config& offload_config) {}

void LeAudioClientInterface::Sink::UpdateBroadcastAudioConfigToHal(
    ::le_audio::broadcast_offload_config const& config) {}

void LeAudioClientInterface::Sink::SuspendedForReconfiguration() {
  LOG(INFO) << __func__;
  // TODO
}

void LeAudioClientInterface::Sink::ReconfigurationComplete() {
  LOG(INFO) << __func__;
}

size_t LeAudioClientInterface::Sink::Read(uint8_t* p_buf, uint32_t len) {
  // return
  // hidl::le_audio::LeAudioSinkTransport::interface->ReadAudioData(p_buf, len);
  return 0;
}

void LeAudioClientInterface::Source::Cleanup() {
  LOG(INFO) << __func__;

  StopSession();

  delete host::le_audio::LeAudioSourceTransport::instance;
  host::le_audio::LeAudioSourceTransport::instance = nullptr;
}

void LeAudioClientInterface::Source::SetPcmParameters(
    const PcmParameters& params) {
  LOG(INFO) << __func__ << ", sample_rate: " << params.sample_rate
            << ", bits_per_sample: " << params.bits_per_sample
            << ", channels_count: " << params.channels_count
            << ", data_interval_us: " << params.data_interval_us;

  host::le_audio::LeAudioSourceTransport::instance
      ->LeAudioSetSelectedHalPcmConfig(
          params.sample_rate, params.bits_per_sample, params.channels_count,
          params.data_interval_us);
}

void LeAudioClientInterface::Source::SetRemoteDelay(uint16_t delay_report_ms) {
  LOG(INFO) << __func__ << ": delay_report_ms=" << delay_report_ms << " ms";

  host::le_audio::LeAudioSourceTransport::instance->SetRemoteDelay(
      delay_report_ms);
}

void LeAudioClientInterface::Source::StartSession() { LOG(INFO) << __func__; }

void LeAudioClientInterface::Source::StopSession() {
  LOG(INFO) << __func__;

  host::le_audio::LeAudioSourceTransport::instance->ClearStartRequestState();
  host::le_audio::LeAudioSourceTransport::stream_started = false;
}

void LeAudioClientInterface::Source::ConfirmStreamingRequest() {
  LOG(INFO) << __func__;

  auto instance = host::le_audio::LeAudioSourceTransport::instance;
  auto start_request_state = instance->GetStartRequestState();

  switch (start_request_state) {
    case StartRequestState::IDLE:
      LOG_WARN(", no pending start stream request");
      return;
    case StartRequestState::PENDING_BEFORE_RESUME:
      LOG_INFO("Response before sending PENDING to audio HAL");
      instance->SetStartRequestState(StartRequestState::CONFIRMED);
      lea_data_path_open();
      return;
    case StartRequestState::PENDING_AFTER_RESUME:
      LOG_INFO("Response after sending PENDING to audio HAL");
      instance->ClearStartRequestState();
      host::le_audio::LeAudioSourceTransport::stream_started = true;
      return;
    case StartRequestState::CONFIRMED:
    case StartRequestState::CANCELED:
      LOG_ERROR("Invalid state, start stream already confirmed");
      return;
  }
}

void LeAudioClientInterface::Source::CancelStreamingRequest() {
  LOG(INFO) << __func__;

  auto instance = host::le_audio::LeAudioSourceTransport::instance;
  auto start_request_state = instance->GetStartRequestState();

  switch (start_request_state) {
    case StartRequestState::IDLE:
      LOG_WARN(", no pending start stream request");
      return;
    case StartRequestState::PENDING_BEFORE_RESUME:
      LOG_INFO("Response before sending PENDING to audio HAL");
      instance->SetStartRequestState(StartRequestState::CANCELED);
      return;
    case StartRequestState::PENDING_AFTER_RESUME:
      LOG_INFO("Response after sending PENDING to audio HAL");
      instance->ClearStartRequestState();
      host::le_audio::LeAudioSourceTransport::stream_started = false;
      return;
    case StartRequestState::CANCELED:
    case StartRequestState::CONFIRMED:
      LOG_ERROR("Invalid state, start stream already confirmed");
      break;
  }
}

void LeAudioClientInterface::Source::UpdateAudioConfigToHal(
    const ::le_audio::offload_config& offload_config) {}

void LeAudioClientInterface::Source::SuspendedForReconfiguration() {
  LOG(INFO) << __func__;
  // TODO
}

void LeAudioClientInterface::Source::ReconfigurationComplete() {
  LOG(INFO) << __func__;
}

size_t LeAudioClientInterface::Source::Write(const uint8_t* p_buf,
                                             uint32_t len) {
  /* return
   * hidl::le_audio::LeAudioSourceTransport::interface->WriteAudioData(p_buf,
   * len); */
  return 0;
}

LeAudioClientInterface::Sink* LeAudioClientInterface::GetSink(
    StreamCallbacks stream_cb,
    bluetooth::common::MessageLoopThread* message_loop,
    bool is_broadcasting_session_type) {
  if (is_broadcasting_session_type &&
      !LeAudioHalVerifier::SupportsLeAudioBroadcast()) {
    LOG(WARNING) << __func__ << ", No support for broadcasting Le Audio";
    return nullptr;
  }

  Sink* sink = is_broadcasting_session_type ? broadcast_sink_ : unicast_sink_;
  if (sink == nullptr) {
    sink = new Sink(is_broadcasting_session_type);
  } else {
    LOG(WARNING) << __func__ << ", Sink is already acquired";
    return nullptr;
  }

  host::le_audio::LeAudioSinkTransport::instance =
      new host::le_audio::LeAudioSinkTransport(std::move(stream_cb));

  return sink;
}

bool LeAudioClientInterface::IsUnicastSinkAcquired() {
  return unicast_sink_ != nullptr;
}

bool LeAudioClientInterface::IsBroadcastSinkAcquired() {
  return broadcast_sink_ != nullptr;
}

bool LeAudioClientInterface::ReleaseSink(LeAudioClientInterface::Sink* sink) {
  if (sink != unicast_sink_ && sink != broadcast_sink_) {
    LOG(WARNING) << __func__ << ", can't release not acquired sink";
    return false;
  }

  sink->Cleanup();

  // TODO: when is this set?
  if (sink == unicast_sink_) {
    delete (unicast_sink_);
    unicast_sink_ = nullptr;
  } else if (sink == broadcast_sink_) {
    delete (broadcast_sink_);
    broadcast_sink_ = nullptr;
  }

  return true;
}

LeAudioClientInterface::Source* LeAudioClientInterface::GetSource(
    StreamCallbacks stream_cb,
    bluetooth::common::MessageLoopThread* message_loop) {
  if (source_ == nullptr) {
    source_ = new Source();
  } else {
    LOG(WARNING) << __func__ << ", Source is already acquired";
    return nullptr;
  }

  LOG(INFO) << __func__;

  host::le_audio::LeAudioSourceTransport::instance =
      new host::le_audio::LeAudioSourceTransport(std::move(stream_cb));

  return source_;
}

bool LeAudioClientInterface::IsSourceAcquired() { return source_ != nullptr; }

bool LeAudioClientInterface::ReleaseSource(
    LeAudioClientInterface::Source* source) {
  if (source != source_) {
    LOG(WARNING) << __func__ << ", can't release not acquired source";
    return false;
  }

  LOG(INFO) << __func__;

  if (host::le_audio::LeAudioSourceTransport::instance) source->Cleanup();

  delete (source_);
  source_ = nullptr;

  return true;
}

LeAudioClientInterface* LeAudioClientInterface::interface = nullptr;

LeAudioClientInterface* LeAudioClientInterface::Get() {
  // TODO: check flag

  if (LeAudioClientInterface::interface == nullptr) {
    ASSERT(lea_uipc == nullptr);
    lea_uipc = UIPC_Init();

    LeAudioClientInterface::interface = new LeAudioClientInterface();
  }

  return LeAudioClientInterface::interface;
}

}  // namespace le_audio
}  // namespace audio
}  // namespace bluetooth
