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

#include <map>

#include "stack/gatt/connection_manager.h"
#include "stack/include/btm_ble_api.h"
#include "types/raw_address.h"

namespace targeted_announcements {

using tAPP_ID = connection_manager::tAPP_ID;

class TargetedAnnouncementsManager {
 public:
  TargetedAnnouncementsManager(const TargetedAnnouncementsManager&) = delete;
  TargetedAnnouncementsManager& operator=(const TargetedAnnouncementsManager&) =
      delete;

  bool Connect(tAPP_ID app_id, const RawAddress& address);
  bool CancelConnect(tAPP_ID app_id, const RawAddress& address);

  static TargetedAnnouncementsManager& Get();

 private:
  TargetedAnnouncementsManager() = default;
  void SetTargetedAnnouncementsFilter(bool enable);
  void OnScanResult(tBTM_INQ_RESULTS* p_inq, const uint8_t* p_eir,
                    uint16_t eir_len);

  enum class ConnectionState {
    SCANNING,
    INITIATING,
  };

  struct PendingConnection {
    std::set<tAPP_ID> clients;
    ConnectionState state;
  };

  std::map<RawAddress, PendingConnection> pending_connections_{};
};

}  // namespace targeted_announcements