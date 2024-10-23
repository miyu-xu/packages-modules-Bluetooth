// Copyright (C) 2024 The Android Open Source Project
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

// http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include <bluetooth/constants/aics/MuteField.h>

#include <cstdint>

namespace bluetooth::aics {
using MuteField = bluetooth::constants::aics::MuteField;

/** Check if the data is a correct Mute value */
bool isAudioInputMuteField(uint8_t data);

/** Convert valid data into a Mute value. Abort if data is not valid */
MuteField parseMuteField(uint8_t data);
}  // namespace bluetooth::aics
