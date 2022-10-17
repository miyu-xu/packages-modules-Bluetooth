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

#define BTIF_STORAGE_CSIS_AUTOCONNECT "CsisAutoconnect"
#define BTIF_STORAGE_CSIS_SET_INFO_BIN "CsisSetInfoBin"

#include <base/bind.h>
#include <base/location.h>
#include <base/logging.h>

#include "bta_csis_api.h"
#include "btif/include/btif_common.h"
#include "btif_config.h"
#include "btif_storage.h"
#include "stack/include/btu.h"  // do_in_main_thread

using base::Bind;

namespace bluetooth {
namespace csis {
namespace storage {
void SetAutoconnect(const RawAddress& addr, bool autoconnect) {
  do_in_jni_thread(FROM_HERE, Bind(
                                  [](const RawAddress& addr, bool autoconnect) {
                                    std::string bdstr = addr.ToString();
                                    VLOG(2) << "Storing CSIS device: " << bdstr;
                                    btif_config_set_int(
                                        bdstr, BTIF_STORAGE_CSIS_AUTOCONNECT,
                                        autoconnect);
                                    btif_config_save();
                                  },
                                  addr, autoconnect));
}

void UpdateInfo(const RawAddress& addr) {
  std::vector<uint8_t> set_info;
  auto not_empty = CsisClient::GetForStorage(addr, set_info);

  if (not_empty)
    do_in_jni_thread(
        FROM_HERE,
        Bind(
            [](const RawAddress& bd_addr, std::vector<uint8_t> set_info) {
              auto bdstr = bd_addr.ToString();
              btif_config_set_bin(bdstr, BTIF_STORAGE_CSIS_SET_INFO_BIN,
                                  set_info.data(), set_info.size());
              btif_config_save();
            },
            addr, std::move(set_info)));
}

void LoadBondedDevices(void) {
  for (const auto& bd_addr : btif_config_get_paired_devices()) {
    auto name = bd_addr.ToString();

    BTIF_TRACE_DEBUG("Loading CSIS device:%s", name.c_str());

    int value;
    bool autoconnect = false;
    if (btif_config_get_int(name, BTIF_STORAGE_CSIS_AUTOCONNECT, &value))
      autoconnect = !!value;

    size_t buffer_size =
        btif_config_get_bin_length(name, BTIF_STORAGE_CSIS_SET_INFO_BIN);
    std::vector<uint8_t> in(buffer_size);
    if (buffer_size != 0)
      btif_config_get_bin(name, BTIF_STORAGE_CSIS_SET_INFO_BIN, in.data(),
                          &buffer_size);

    if (buffer_size != 0 || autoconnect)
      do_in_main_thread(FROM_HERE, Bind(&CsisClient::AddFromStorage, bd_addr,
                                        std::move(in), autoconnect));
  }
}

void RemoveDevice(const RawAddress& address) {
  std::string addrstr = address.ToString();
  btif_config_remove(addrstr, BTIF_STORAGE_CSIS_AUTOCONNECT);
  btif_config_remove(addrstr, BTIF_STORAGE_CSIS_SET_INFO_BIN);
  btif_config_save();
}
}  // namespace storage
}  // namespace csis
}  // namespace bluetooth