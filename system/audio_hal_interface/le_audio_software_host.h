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

#pragma once

#include "audio_hal_interface/le_audio_software.h"

namespace bluetooth {
namespace audio {
namespace le_audio {

using ::le_audio::set_configurations::AudioSetConfiguration;
using ::le_audio::set_configurations::CodecCapabilitySetting;

using ::bluetooth::audio::le_audio::StartRequestState;
using ::bluetooth::audio::le_audio::StreamCallbacks;

struct PcmParameters {
  uint32_t data_interval_us;
  uint32_t sample_rate;
  uint8_t bits_per_sample;
  uint8_t channels_count;
};

}  // namespace le_audio
}  // namespace audio
}  // namespace bluetooth
