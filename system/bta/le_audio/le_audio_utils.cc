/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "le_audio_utils.h"

#include "gd/common/strings.h"
#include "le_audio_types.h"
#include "osi/include/log.h"

using bluetooth::common::ToString;
using le_audio::types::AudioContexts;
using le_audio::types::LeAudioContextType;

namespace le_audio {
namespace utils {
LeAudioContextType AudioContentToLeAudioContext(
    audio_content_type_t content_type, audio_usage_t usage) {
  /* Check audio attribute usage of stream */
  switch (usage) {
    case AUDIO_USAGE_MEDIA:
      return LeAudioContextType::MEDIA;
    case AUDIO_USAGE_VOICE_COMMUNICATION:
    case AUDIO_USAGE_CALL_ASSISTANT:
      return LeAudioContextType::CONVERSATIONAL;
    case AUDIO_USAGE_VOICE_COMMUNICATION_SIGNALLING:
      if (content_type == AUDIO_CONTENT_TYPE_SPEECH)
        return LeAudioContextType::CONVERSATIONAL;
      else
        return LeAudioContextType::MEDIA;
    case AUDIO_USAGE_GAME:
      return LeAudioContextType::GAME;
    case AUDIO_USAGE_NOTIFICATION:
      return LeAudioContextType::NOTIFICATIONS;
    case AUDIO_USAGE_NOTIFICATION_TELEPHONY_RINGTONE:
      return LeAudioContextType::RINGTONE;
    case AUDIO_USAGE_ALARM:
      return LeAudioContextType::ALERTS;
    case AUDIO_USAGE_EMERGENCY:
      return LeAudioContextType::EMERGENCYALARM;
    case AUDIO_USAGE_ASSISTANCE_NAVIGATION_GUIDANCE:
      return LeAudioContextType::INSTRUCTIONAL;
    default:
      break;
  }

  return LeAudioContextType::MEDIA;
}

AudioContexts GetAllowedAudioContextsFromSourceMetadata(
    const source_metadata_t& source_metadata, AudioContexts allowed_contexts) {
  AudioContexts track_contexts(0);
  for (auto idx = 0u; idx < source_metadata.track_count; ++idx) {
    auto& track = source_metadata.tracks[idx];
    if (track.content_type == 0 && track.usage == 0) continue;

    LOG_INFO("%s: usage=%d, content_type=%d, gain=%f", __func__, track.usage,
             track.content_type, track.gain);

    track_contexts |= AudioContexts(
        static_cast<std::underlying_type<LeAudioContextType>::type>(
            AudioContentToLeAudioContext(track.content_type, track.usage)));
  }
  track_contexts &= allowed_contexts;
  LOG_INFO("%s: allowed context=%lu", __func__, track_contexts.to_ulong());

  return track_contexts;
}

}  // namespace utils
}  // namespace le_audio
