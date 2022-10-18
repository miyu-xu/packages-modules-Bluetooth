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

#include <base/bind.h>
#include <base/location.h>
#include <base/logging.h>

#include "btif/include/btif_common.h"
#include "btif_config.h"
#include "btif_storage.h"
#include "include/bta_has_api.h"
#include "stack/include/btu.h"  // do_in_main_thread

namespace le_audio {
namespace has {
namespace storage {

constexpr char HAS_IS_ACCEPTLISTED[] = "LeAudioHasIsAcceptlisted";
constexpr char HAS_FEATURES[] = "LeAudioHasFlags";
constexpr char HAS_ACTIVE_PRESET[] = "LeAudioHasActivePreset";
constexpr char HAS_SERIALIZED_PRESETS[] = "LeAudioHasSerializedPresets";

void AddDevice(const RawAddress& address, std::vector<uint8_t> presets_bin,
               uint8_t features, uint8_t active_preset) {
  do_in_jni_thread(
      FROM_HERE,
      base::Bind(
          [](const RawAddress& address, std::vector<uint8_t> presets_bin,
             uint8_t features, uint8_t active_preset) {
            const std::string& name = address.ToString();

            btif_config_set_int(name, HAS_FEATURES, features);
            btif_config_set_int(name, HAS_ACTIVE_PRESET, active_preset);
            btif_config_set_bin(name, HAS_SERIALIZED_PRESETS,
                                presets_bin.data(), presets_bin.size());

            btif_config_set_int(name, HAS_IS_ACCEPTLISTED, true);
            btif_config_save();
          },
          address, std::move(presets_bin), features, active_preset));
}

void SetHasActivePreset(const RawAddress& address, uint8_t active_preset) {
  do_in_jni_thread(FROM_HERE,
                   base::Bind(
                       [](const RawAddress& address, uint8_t active_preset) {
                         const std::string& name = address.ToString();

                         btif_config_set_int(name, HAS_ACTIVE_PRESET,
                                             active_preset);
                         btif_config_save();
                       },
                       address, active_preset));
}

bool GetHasFeatures(const RawAddress& address, uint8_t& features) {
  std::string name = address.ToString();

  int value;
  if (!btif_config_get_int(name, HAS_FEATURES, &value)) return false;

  features = value;
  return true;
}

void SetHasFeatures(const RawAddress& address, uint8_t features) {
  do_in_jni_thread(FROM_HERE,
                   base::Bind(
                       [](const RawAddress& address, uint8_t features) {
                         const std::string& name = address.ToString();

                         btif_config_set_int(name, HAS_FEATURES, features);
                         btif_config_save();
                       },
                       address, features));
}

void AddBondedHasDevices() {
  for (const auto& bd_addr : btif_config_get_paired_devices()) {
    const std::string& name = bd_addr.ToString();

    if (!btif_config_exist(name, HAS_IS_ACCEPTLISTED) &&
        !btif_config_exist(name, HAS_FEATURES))
      continue;

#ifndef TARGET_FLOSS
    int value;
    uint16_t is_acceptlisted = 0;
    if (btif_config_get_int(name, HAS_IS_ACCEPTLISTED, &value))
      is_acceptlisted = value;

    uint8_t features = 0;
    if (btif_config_get_int(name, HAS_FEATURES, &value)) features = value;

    do_in_main_thread(FROM_HERE,
                      base::Bind(&le_audio::has::HasClient::AddFromStorage,
                                 bd_addr, features, is_acceptlisted));
#else
    ASSERT_LOG(false, "TODO - Fix LE audio build.");
#endif
  }
}

void RemoveHasDevice(const RawAddress& address) {
  std::string addrstr = address.ToString();
  btif_config_remove(addrstr, HAS_IS_ACCEPTLISTED);
  btif_config_remove(addrstr, HAS_FEATURES);
  btif_config_remove(addrstr, HAS_ACTIVE_PRESET);
  btif_config_remove(addrstr, HAS_SERIALIZED_PRESETS);
  btif_config_save();
}

void SetHasAcceptlist(const RawAddress& address, bool add_to_acceptlist) {
  std::string addrstr = address.ToString();

  btif_config_set_int(addrstr, HAS_IS_ACCEPTLISTED, add_to_acceptlist);
  btif_config_save();
}

void SetHasPresets(const RawAddress& address,
                   std::vector<uint8_t> presets_bin) {
  do_in_jni_thread(
      FROM_HERE,
      base::Bind(
          [](const RawAddress& address, std::vector<uint8_t> presets_bin) {
            const std::string& name = address.ToString();

            btif_config_set_bin(name, HAS_SERIALIZED_PRESETS,
                                presets_bin.data(), presets_bin.size());
            btif_config_save();
          },
          address, std::move(presets_bin)));
}

bool GetHasPresets(const RawAddress& address, std::vector<uint8_t>& presets_bin,
                   uint8_t& active_preset) {
  std::string name = address.ToString();

  int value;
  if (!btif_config_get_int(name, HAS_ACTIVE_PRESET, &value)) return false;
  active_preset = value;

  auto bin_sz = btif_config_get_bin_length(name, HAS_SERIALIZED_PRESETS);
  presets_bin.resize(bin_sz);
  if (!btif_config_get_bin(name, HAS_SERIALIZED_PRESETS, presets_bin.data(),
                           &bin_sz))
    return false;

  return true;
}
}  // namespace storage
}  // namespace has
}  // namespace le_audio