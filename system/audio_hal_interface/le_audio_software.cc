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

#include "le_audio_software.h"

#include "hal_version_manager.h"
#include "hidl/le_audio_software.h"

namespace bluetooth {
namespace audio {
namespace le_audio {

LeAudioClientInterface* LeAudioClientInterface::Get() {
  if (HalVersionManager::GetHalTransport() ==
      BluetoothAudioHalTransport::VERSION_HIDL) {
    return hidl::le_audio::LeAudioClientInterface::Get();
  }
  return nullptr;
}

/**
 *
 * These functions are just stub placeholders.
 *
 **/

std::vector<::le_audio::set_configurations::AudioSetConfiguration>
get_offload_capabilities() {
  return std::vector<::le_audio::set_configurations::AudioSetConfiguration>(0);
}

bool LeAudioClientInterface::IsSinkAcquired() { return false; }

bool LeAudioClientInterface::ReleaseSink(LeAudioClientInterface::Sink* sink) {
  return false;
}

LeAudioClientInterface::Sink* LeAudioClientInterface::GetSink(
    StreamCallbacks stream_cb,
    bluetooth::common::MessageLoopThread* message_loop) {
  return nullptr;
}

size_t LeAudioClientInterface::Sink::Read(uint8_t* p_buf, uint32_t len) {
  return 0;
}

LeAudioClientInterface::Source* LeAudioClientInterface::GetSource(
    StreamCallbacks stream_cb,
    bluetooth::common::MessageLoopThread* message_loop) {
  return nullptr;
}

size_t LeAudioClientInterface::Source::Write(const uint8_t* p_buf,
                                             uint32_t len) {
  return 0;
}

bool LeAudioClientInterface::IsSourceAcquired() { return false; }

bool LeAudioClientInterface::ReleaseSource(
    LeAudioClientInterface::Source* source) {
  return false;
}

}  // namespace le_audio
}  // namespace audio
}  // namespace bluetooth