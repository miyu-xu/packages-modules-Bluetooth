/*
 * Copyright 2021 HIMSA II K/S - www.himsa.com.
 * Represented by EHIMA - www.ehima.com
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

#include <base/bind.h>

#include "bta_le_audio_broadcaster_api.h"

void LeAudioBroadcaster::Initialize(
    bluetooth::le_audio::LeAudioBroadcasterCallbacks* callbacks,
    base::Callback<bool()> hal_2_1_verifier) {
  mock_function_count_map[__func__]++;
}

bool LeAudioBroadcaster::IsLeAudioBroadcasterRunning() {
  mock_function_count_map[__func__]++;
  return false;
}

LeAudioBroadcaster* LeAudioBroadcaster::Get(void) {
  mock_function_count_map[__func__]++;
  return nullptr;
}

void LeAudioBroadcaster::Stop(void) { mock_function_count_map[__func__]++; }

void LeAudioBroadcaster::Cleanup(void) { mock_function_count_map[__func__]++; }

void LeAudioBroadcaster::DebugDump(int fd) {
  mock_function_count_map[__func__]++;
}
