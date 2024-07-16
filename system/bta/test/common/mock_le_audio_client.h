/*
 * Copyright 2025 Cochlear Limited
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
#pragma once

#include <gmock/gmock.h>

#include "bta_le_audio_api.h"

class MockLeAudioClient : public LeAudioClient {
public:
  /* Overrides */
  MOCK_METHOD((void), RemoveDevice, (const RawAddress& address), (override));
  MOCK_METHOD((void), Connect, (const RawAddress& address), (override));
  MOCK_METHOD((void), Disconnect, (const RawAddress& address), (override));
  MOCK_METHOD((void), SetEnableState, (const RawAddress& address, bool enabled), (override));
  MOCK_METHOD((void), GroupAddNode, (int group_id, const RawAddress& addr), (override));
  MOCK_METHOD((void), GroupRemoveNode, (int group_id, const RawAddress& addr), (override));
  MOCK_METHOD((void), GroupStream, (int group_id, uint16_t content_type), (override));
  MOCK_METHOD((void), GroupSuspend, (int group_id), (override));
  MOCK_METHOD((void), GroupStop, (int group_id), (override));
  MOCK_METHOD((void), GroupDestroy, (int group_id), (override));
  MOCK_METHOD((void), GroupSetActive, (int group_id), (override));
  MOCK_METHOD((void), SetCodecConfigPreference,
              (int group_id, bluetooth::le_audio::btle_audio_codec_config_t input_codec_config,
               bluetooth::le_audio::btle_audio_codec_config_t output_codec_config),
              (override));
  MOCK_METHOD((bool), IsUsingPreferredCodecConfig, (int group_id, int context_type), (override));
  MOCK_METHOD((void), SetCcidInformation, (int ccid, int context_type), (override));
  MOCK_METHOD((void), SetInCall, (bool in_call), (override));
  MOCK_METHOD((bool), IsInCall, (), (override));
  MOCK_METHOD((void), SetInVoipCall, (bool in_call), (override));
  MOCK_METHOD((void), SetUnicastMonitorMode, (uint8_t direction, bool enable), (override));
  MOCK_METHOD((bool), IsInVoipCall, (), (override));
  MOCK_METHOD((bool), IsInStreaming, (), (override));
  MOCK_METHOD((void), SendAudioProfilePreferences,
              (int group_id, bool is_output_preference_le_audio,
               bool is_duplex_preference_le_audio),
              (override));
  MOCK_METHOD((void), SetGroupAllowedContextMask,
              (int group_id, int sink_context_types, int source_context_types),
              (override));
  MOCK_METHOD((bool), isOutputPreferenceLeAudio, (const RawAddress& address), (override));
  MOCK_METHOD((bool), isDuplexPreferenceLeAudio, (const RawAddress& address), (override));
  MOCK_METHOD(std::vector<RawAddress>, GetGroupDevices, (int group_id), (override));
  MOCK_METHOD((bool), IsDeviceActive, (const RawAddress& addr), (override));
  MOCK_METHOD((void), SetDesiredGroupSize, (int group_id, int size), (override));

  /* Called from static methods */
  MOCK_METHOD((LeAudioClient*), Get, ());
  MOCK_METHOD((bool), IsLeAudioClientRunning, ());
  MOCK_METHOD((bool), IsDcsEnabled, ());

  static void SetMockInstanceForTesting(MockLeAudioClient* mock);
};
