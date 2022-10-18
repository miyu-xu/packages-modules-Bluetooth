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

namespace le_audio {
namespace has {
namespace storage {

void AddDevice(const RawAddress& address, std::vector<uint8_t> presets_bin,
               uint8_t features, uint8_t active_preset);

void SetHasActivePreset(const RawAddress& address, uint8_t active_preset);

bool GetHasFeatures(const RawAddress& address, uint8_t& features);

void SetHasFeatures(const RawAddress& address, uint8_t features);

void AddBondedHasDevices();

void RemoveHasDevice(const RawAddress& address);

void SetHasAcceptlist(const RawAddress& address, bool add_to_acceptlist);

void SetHasPresets(const RawAddress& address, std::vector<uint8_t> presets_bin);

bool GetHasPresets(const RawAddress& address, std::vector<uint8_t>& presets_bin,
                   uint8_t& active_preset);

}  // namespace storage
}  // namespace has
}  // namespace le_audio