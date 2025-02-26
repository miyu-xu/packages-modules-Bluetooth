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

#ifdef TARGET_FLOSS
#include <audio_hal_interface/audio_linux.h>
#else
#include <hardware/audio.h>
#endif

#include "bluetooth_audio_port_impl.h"

#include <bluetooth/log.h>
#include <com_android_bluetooth_flags.h>

#include <vector>

#include "android/binder_ibinder_platform.h"
#include "btif/include/btif_common.h"
#include "client_interface_aidl.h"
#include "common/stop_watch_legacy.h"

enum CONTEXT_PRIORITY { SONIFICATION = 0, MEDIA, GAME, CONVERSATIONAL };

namespace bluetooth {
namespace audio {
namespace aidl {
namespace a2dp {

AudioContext AudioContentToAudioContext([[maybe_unused]] audio_content_type_t content_type,
                                        audio_source_t source_type, audio_usage_t usage) {
  switch (usage) {
    case AUDIO_USAGE_MEDIA:
      return AudioContext::MEDIA;
    case AUDIO_USAGE_VOICE_COMMUNICATION:
      return AudioContext::CONVERSATIONAL;
    case AUDIO_USAGE_CALL_ASSISTANT:
      return AudioContext::CONVERSATIONAL;
    case AUDIO_USAGE_VOICE_COMMUNICATION_SIGNALLING:
      return AudioContext::VOICE_ASSISTANTS;
    case AUDIO_USAGE_ASSISTANCE_SONIFICATION:
      return AudioContext::SOUND_EFFECTS;
    case AUDIO_USAGE_GAME:
      return AudioContext::GAME;
    case AUDIO_USAGE_NOTIFICATION:
      return AudioContext::NOTIFICATIONS;
    case AUDIO_USAGE_NOTIFICATION_TELEPHONY_RINGTONE:
      return AudioContext::CONVERSATIONAL;
    case AUDIO_USAGE_ALARM:
      return AudioContext::ALERTS;
    case AUDIO_USAGE_EMERGENCY:
      return AudioContext::EMERGENCY_ALARM;
    case AUDIO_USAGE_ASSISTANCE_NAVIGATION_GUIDANCE:
      return AudioContext::INSTRUCTIONAL;
    default:
      break;
  }

  switch (source_type) {
    case AUDIO_SOURCE_MIC:
    case AUDIO_SOURCE_HOTWORD:
    case AUDIO_SOURCE_VOICE_CALL:
    case AUDIO_SOURCE_VOICE_COMMUNICATION:
      return AudioContext::CONVERSATIONAL;
    default:
      break;
  }

  LOG(INFO) << __func__ << ": Return Media when not in call by default.";
  return AudioContext::MEDIA;
}

static int getPriority(AudioContext context) {
  switch (context) {
    case AudioContext::MEDIA:
      return CONTEXT_PRIORITY::MEDIA;
    case AudioContext::GAME:
      return CONTEXT_PRIORITY::GAME;
    case AudioContext::CONVERSATIONAL:
      return CONTEXT_PRIORITY::CONVERSATIONAL;
    case AudioContext::SOUNDEFFECTS:
      return CONTEXT_PRIORITY::SONIFICATION;
    default:
      break;
  }
  return 0;
}

using ::bluetooth::common::StopWatchLegacy;

BluetoothAudioPortImpl::BluetoothAudioPortImpl(
        IBluetoothTransportInstance* transport_instance,
        const std::shared_ptr<IBluetoothAudioProvider>& provider)
    : transport_instance_(transport_instance), provider_(provider) {}

BluetoothAudioPortImpl::~BluetoothAudioPortImpl() {}

ndk::ScopedAStatus BluetoothAudioPortImpl::startStream(bool is_low_latency) {
  StopWatchLegacy stop_watch(__func__);
  Status ack = transport_instance_->StartRequest(is_low_latency);
  if (ack != Status::PENDING) {
    auto aidl_retval = provider_->streamStarted(StatusToHalStatus(ack));
    if (!aidl_retval.isOk()) {
      log::error("BluetoothAudioHal failure: {}", aidl_retval.getDescription());
    }
  }
  return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus BluetoothAudioPortImpl::suspendStream() {
  StopWatchLegacy stop_watch(__func__);
  Status ack = transport_instance_->SuspendRequest();
  if (ack != Status::PENDING) {
    auto aidl_retval = provider_->streamSuspended(StatusToHalStatus(ack));
    if (!aidl_retval.isOk()) {
      log::error("BluetoothAudioHal failure: {}", aidl_retval.getDescription());
    }
  }
  return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus BluetoothAudioPortImpl::stopStream() {
  StopWatchLegacy stop_watch(__func__);
  transport_instance_->StopRequest();
  return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus BluetoothAudioPortImpl::getPresentationPosition(
        PresentationPosition* _aidl_return) {
  StopWatchLegacy stop_watch(__func__);
  uint64_t remote_delay_report_ns;
  uint64_t total_bytes_read;
  timespec data_position;
  bool retval = transport_instance_->GetPresentationPosition(&remote_delay_report_ns,
                                                             &total_bytes_read, &data_position);

  PresentationPosition::TimeSpec transmittedOctetsTimeStamp;
  if (retval) {
    transmittedOctetsTimeStamp = timespec_convert_to_hal(data_position);
  } else {
    remote_delay_report_ns = 0;
    total_bytes_read = 0;
    transmittedOctetsTimeStamp = {};
  }
  log::verbose("result={}, delay={}, data={} byte(s), timestamp={}", retval, remote_delay_report_ns,
               total_bytes_read, transmittedOctetsTimeStamp.toString());
  _aidl_return->remoteDeviceAudioDelayNanos = static_cast<int64_t>(remote_delay_report_ns);
  _aidl_return->transmittedOctets = static_cast<int64_t>(total_bytes_read);
  _aidl_return->transmittedOctetsTimestamp = transmittedOctetsTimeStamp;
  return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus BluetoothAudioPortImpl::updateSourceMetadata(
        const SourceMetadata& source_metadata) {
  StopWatchLegacy stop_watch(__func__);
  log::info("{} track(s)", source_metadata.tracks.size());

  std::vector<playback_track_metadata_v7> tracks_vec;
  tracks_vec.reserve(source_metadata.tracks.size());
  for (const auto& track : source_metadata.tracks) {
    auto num_of_tags = track.tags.size();
    log::info("metadata tags size: {}", num_of_tags);

    playback_track_metadata_v7 desc_track = {
            .base = {.usage = static_cast<audio_usage_t>(track.usage),
                     .content_type = static_cast<audio_content_type_t>(track.contentType),
                     .gain = track.gain},
    };

    tracks_vec.push_back(desc_track);
  }

  const source_metadata_v7_t source_metadata = {.track_count = tracks_vec.size(),
                                                .tracks = tracks_vec.data()};
  AudioContext current_context = AudioContext::MEDIA;
  if (source_metadata.track_count > 0) {
    for (size_t i = 0; i < source_metadata.track_count; i++) {
    auto context_priority = 0;
    auto track = source_metadata.tracks[i].base;
    if (track.content_type == 0 && track.usage == 0) {
      log::info(" tracks count: {}", track_count);
      continue;
    }

    log::info("usage= {}, content_type= {}, gain= {}", track.usage, track.content_type, track.gain);
    AudioContext context_type = AudioContentToAudioContext(
            track.content_type, AUDIO_SOURCE_DEFAULT, track.usage);

    context_priority = getPriority(context_type);
    LOG(INFO) << __func__ << ": context_priority: " << context_priority;
    if (context_priority > current_priority) {
      current_priority = context_priority;
      current_context = context_type;
    }
  }

  bool is_low_latency = (current_context.bitmask == AudioContext::GAME);
  transport_instance_->SourceMetadataChanged(is_low_latency);
  return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus BluetoothAudioPortImpl::updateSinkMetadata(
        const SinkMetadata& /*sink_metadata*/) {
  return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus BluetoothAudioPortImpl::setLatencyMode(LatencyMode latency_mode) {
  bool is_low_latency = latency_mode == LatencyMode::LOW_LATENCY ? true : false;
  invoke_switch_buffer_size_cb(is_low_latency);
  transport_instance_->SetLatencyMode(latency_mode);
  return ndk::ScopedAStatus::ok();
}

PresentationPosition::TimeSpec BluetoothAudioPortImpl::timespec_convert_to_hal(const timespec& ts) {
  return {.tvSec = static_cast<int64_t>(ts.tv_sec), .tvNSec = static_cast<int64_t>(ts.tv_nsec)};
}

// Overriding create binder and inherit RT from caller.
// In our case, the caller is the AIDL session control, so we match the priority
// of the AIDL session / AudioFlinger writer thread.
ndk::SpAIBinder BluetoothAudioPortImpl::createBinder() {
  auto binder = BnBluetoothAudioPort::createBinder();
  AIBinder_setInheritRt(binder.get(), true);
  return binder;
}

}  // namespace a2dp
}  // namespace aidl
}  // namespace audio
}  // namespace bluetooth
