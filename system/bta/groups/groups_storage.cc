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

#include "bta/include/bta_groups.h"
#include "btif/include/btif_common.h"
#include "btif_config.h"
#include "btif_storage.h"
#include "stack/include/btu.h"  // do_in_main_thread

#define BTIF_STORAGE_DEVICE_GROUP_BIN "DeviceGroupBin"

using base::Bind;

namespace bluetooth {
namespace groups {
namespace storage {

void AddGroups(const RawAddress& addr) {
  std::vector<uint8_t> group_info;
  auto not_empty = DeviceGroups::GetForStorage(addr, group_info);

  if (not_empty)
    do_in_jni_thread(
        FROM_HERE,
        Bind(
            [](const RawAddress& bd_addr, std::vector<uint8_t> group_info) {
              auto bdstr = bd_addr.ToString();
              btif_config_set_bin(bdstr, BTIF_STORAGE_DEVICE_GROUP_BIN,
                                  group_info.data(), group_info.size());
              btif_config_save();
            },
            addr, std::move(group_info)));
}

void RemoveGroups(const RawAddress& address) {
  std::string addrstr = address.ToString();
  btif_config_remove(addrstr, BTIF_STORAGE_DEVICE_GROUP_BIN);
  btif_config_save();
}

void LoadBondedGroups(void) {
  for (const auto& bd_addr : btif_config_get_paired_devices()) {
    auto name = bd_addr.ToString();
    size_t buffer_size =
        btif_config_get_bin_length(name, BTIF_STORAGE_DEVICE_GROUP_BIN);
    if (buffer_size == 0) continue;

    BTIF_TRACE_DEBUG("Grouped device:%s", name.c_str());

    std::vector<uint8_t> in(buffer_size);
    if (btif_config_get_bin(name, BTIF_STORAGE_DEVICE_GROUP_BIN, in.data(),
                            &buffer_size)) {
      do_in_main_thread(FROM_HERE, Bind(&DeviceGroups::AddFromStorage, bd_addr,
                                        std::move(in)));
    }
  }
}
}  // namespace storage
}  // namespace groups
}  // namespace bluetooth