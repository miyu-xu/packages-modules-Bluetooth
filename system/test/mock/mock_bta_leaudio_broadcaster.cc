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

#include <map>
#include <string>

#include "test/common/mock_functions.h"

#include <base/bind.h>
#include <base/bind_helpers.h>
#include <hardware/bt_le_audio.h>

#include "bta/include/bta_le_audio_broadcaster_api.h"

#ifndef UNUSED_ATTR
#define UNUSED_ATTR
#endif

void LeAudioBroadcaster::DebugDump(int) { increment_mock_function_call_count(__func__); }
void LeAudioBroadcaster::Initialize(
    bluetooth::le_audio::LeAudioBroadcasterCallbacks*,
    base::RepeatingCallback<bool()>) {
  increment_mock_function_call_count(__func__);
}
void LeAudioBroadcaster::Stop() { increment_mock_function_call_count(__func__); }
void LeAudioBroadcaster::Cleanup() { increment_mock_function_call_count(__func__); }
LeAudioBroadcaster* LeAudioBroadcaster::Get() {
  increment_mock_function_call_count(__func__);
  return nullptr;
}
