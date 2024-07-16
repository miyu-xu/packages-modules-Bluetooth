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

#include "mock_le_audio_client.h"

static MockLeAudioClient* mock_le_audio_client;

void MockLeAudioClient::SetMockInstanceForTesting(MockLeAudioClient* mock) {
  mock_le_audio_client = mock;
}

LeAudioClient* LeAudioClient::Get() {
  bluetooth::log::assert_that(mock_le_audio_client, "Mock LeAudioClient interface not set!");
  return mock_le_audio_client->Get();
}

bool LeAudioClient::IsLeAudioClientRunning() {
  bluetooth::log::assert_that(mock_le_audio_client, "Mock LeAudioClient interface not set!");
  return mock_le_audio_client->IsLeAudioClientRunning();
}

bool LeAudioClient::IsDcsEnabled() {
  bluetooth::log::assert_that(mock_le_audio_client, "Mock LeAudioClient interface not set!");
  return mock_le_audio_client->IsDcsEnabled();
}