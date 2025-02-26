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

namespace bluetooth {
namespace audio {
namespace aidl {
namespace a2dp {

namespace {

using A2DPAudioContext = ::aidl::android::hardware::bluetooth::audio::AudioContext;

enum AudioContextPriority { SONIFICATION = 0, MEDIA, GAME, CONVERSATIONAL };

static int32_t audioUsageToAudioContext(audio_usage_t usage) {
  switch (usage) {
    case AUDIO_USAGE_MEDIA:
      return A2DPAudioContext::MEDIA;
    case AUDIO_USAGE_VOICE_COMMUNICATION:
      return A2DPAudioContext::CONVERSATIONAL;
    case AUDIO_USAGE_CALL_ASSISTANT:
      return A2DPAudioContext::CONVERSATIONAL;
    case AUDIO_USAGE_VOICE_COMMUNICATION_SIGNALLING:
      return A2DPAudioContext::VOICE_ASSISTANTS;
    case AUDIO_USAGE_ASSISTANCE_SONIFICATION:
      return A2DPAudioContext::SOUND_EFFECTS;
    case AUDIO_USAGE_GAME:
      return A2DPAudioContext::GAME;
    case AUDIO_USAGE_NOTIFICATION:
      return A2DPAudioContext::NOTIFICATIONS;
    case AUDIO_USAGE_NOTIFICATION_TELEPHONY_RINGTONE:
      return A2DPAudioContext::CONVERSATIONAL;
    case AUDIO_USAGE_ALARM:
      return A2DPAudioContext::ALERTS;
    case AUDIO_USAGE_EMERGENCY:
      return A2DPAudioContext::EMERGENCY_ALARM;
    case AUDIO_USAGE_ASSISTANCE_NAVIGATION_GUIDANCE:
      return A2DPAudioContext::INSTRUCTIONAL;
    default:
      break;
  }

  LOG(INFO) << __func__ << ": Return Media when not in call by default.";
  return A2DPAudioContext::MEDIA;
}

static int audioContextPriority(int32_t context) {
  switch (context) {
    case A2DPAudioContext::MEDIA:
      return AudioContextPriority::MEDIA;
    case A2DPAudioContext::GAME:
      return AudioContextPriority::GAME;
    case A2DPAudioContext::CONVERSATIONAL:
      return AudioContextPriority::CONVERSATIONAL;
    case A2DPAudioContext::SOUND_EFFECTS:
      return AudioContextPriority::SONIFICATION;
    default:
      break;
  }
  return -1;
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

  int32_t current_context = AudioContext::MEDIA;
  int current_priority = AudioContextPriority::MEDIA;
  for (const auto& track: source_metadata.tracks) {
    audio_usage_t usage = static_cast<audio_usage_t>(track.usage);
    int32_t context = audioUsageToAudioContext(usage);
    int priority = audioContextPriority(context);

    if (priority > current_priority) {
      current_context = context;
      current_priority = priority;
    }
  }

  bool is_low_latency = (current_context == A2DPAudioContext::GAME);
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
