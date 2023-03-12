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

#include "stack/gatt/targeted_announcements.h"

#include <stdint.h>

#include "main/shim/le_scanning_manager.h"
#include "osi/include/log.h"
#include "stack/include/advertise_data_parser.h"
#include "stack/include/btm_ble_api.h"

namespace targeted_announcements {

namespace {

constexpr char kBtmLogTag[] = "TA";

bool IsTargetedAnnouncement(const uint8_t* p_eir, uint16_t eir_len) {
  const uint8_t* p_service_data = p_eir;
  uint8_t service_data_len = 0;

  while ((p_service_data = AdvertiseDataParser::GetFieldByType(
              p_service_data + service_data_len,
              eir_len - (p_service_data - p_eir) - service_data_len,
              BTM_BLE_AD_TYPE_SERVICE_DATA_TYPE, &service_data_len))) {
    uint16_t uuid;
    uint8_t announcement_type;
    const uint8_t* p_tmp = p_service_data;

    if (service_data_len < 1) {
      continue;
    }

    STREAM_TO_UINT16(uuid, p_tmp);
    LOG_DEBUG("Found UUID 0x%04x", uuid);

    if (uuid != 0x184E && uuid != 0x1853) {
      continue;
    }

    STREAM_TO_UINT8(announcement_type, p_tmp);
    LOG_DEBUG("Found announcement_type 0x%02x", announcement_type);
    if (announcement_type == 0x01) {
      return true;
    }
  }
  return false;
}

}  // namespace

/** Add a device to the background connection list for targeted announcements.
 * Returns
 *   true if device added to the list, or already in list,
 *   false otherwise
 */
bool TargetedAnnouncementsManager::Connect(tAPP_ID app_id,
                                           const RawAddress& address) {
  LOG_INFO("app_id=%d, address=%s", static_cast<int>(app_id),
           ADDRESS_TO_LOGGABLE_CSTR(address));

  auto it = pending_connections_.find(address);
  if (it != pending_connections_.end()) {
    LOG_INFO(
        "app_id=%d, already doing targeted announcement filtering to "
        "address=%s",
        static_cast<int>(app_id), ADDRESS_TO_LOGGABLE_CSTR(address));
    it->second.clients.insert(app_id);
    return true;
  }

  pending_connections_[address].clients.insert(app_id);
  if (pending_connections_[address].clients.size() == 1) {
    BTM_LogHistory(kBtmLogTag, address, "Allow connection from");
  }

  if (pending_connections_[address].state == ConnectionState::SCANNING) {
    if (pending_connections_.size() == 1) {
      SetTargetedAnnouncementsFilter(true);
    }
  } else {
    // we are already initiating, let this client "piggyback" on the initiation
    connection_manager::direct_connect_add(app_id, address);
  }

  return true;
}

void TargetedAnnouncementsManager::SetTargetedAnnouncementsFilter(bool enable) {
  LOG_DEBUG("enable %d", enable);
  BTM_LogHistory(kBtmLogTag, RawAddress::kEmpty,
                 (enable ? "Start filtering" : "Stop filtering"));

  /* Safe to call as if there is no support for filtering, this call will be
   * ignored. */
  bluetooth::shim::set_target_announcements_filter(enable);
  BTM_BleTargetAnnouncementObserve(
      enable,
      [](tBTM_INQ_RESULTS* p_inq, const uint8_t* p_eir, uint16_t eir_len) {
        TargetedAnnouncementsManager::Get().OnScanResult(p_inq, p_eir, eir_len);
      });
}

void TargetedAnnouncementsManager::OnScanResult(tBTM_INQ_RESULTS* p_inq,
                                                const uint8_t* p_eir,
                                                uint16_t eir_len) {
  auto addr = p_inq->remote_bd_addr;
  auto it = pending_connections_.find(addr);
  if (it == pending_connections_.end()) {
    return;
  }

  if (!IsTargetedAnnouncement(p_eir, eir_len)) {
    LOG_DEBUG("Not a targeted announcement for device %s",
              ADDRESS_TO_LOGGABLE_CSTR(addr));
    return;
  }

  LOG_INFO("Found targeted announcement for device %s",
           ADDRESS_TO_LOGGABLE_CSTR(addr));

  if (it->second.state == ConnectionState::INITIATING) {
    LOG_INFO("Device %s is already connecting", ADDRESS_TO_LOGGABLE_CSTR(addr));
    return;
  }

  if (BTM_GetHCIConnHandle(addr, BT_TRANSPORT_LE) != 0xFFFF) {
    LOG_DEBUG("Device %s already connected", ADDRESS_TO_LOGGABLE_CSTR(addr));
    pending_connections_.erase(it);
    return;
  }

  BTM_LogHistory(kBtmLogTag, addr, "Found TA from");

  // since the device wants a connection, all our clients can begin a direct
  // connection, and we move to the INITIATING state
  it->second.state = ConnectionState::INITIATING;
  for (auto client : it->second.clients) {
    connection_manager::direct_connect_add(client, addr);
  }
}

/** Remove device from the background connection device list or listening to
 * advertising list.  Returns true if device was on the list and was
 * successfully removed */
bool TargetedAnnouncementsManager::CancelConnect(tAPP_ID app_id,
                                                 const RawAddress& address) {
  LOG_DEBUG("app_id=%d, address=%s", static_cast<int>(app_id),
            ADDRESS_TO_LOGGABLE_CSTR(address));
  auto it = pending_connections_.find(address);
  if (it == pending_connections_.end()) {
    LOG_WARN("address %s is not found", ADDRESS_TO_LOGGABLE_CSTR(address));
    return false;
  }

  bool client_removed = it->second.clients.erase(app_id) > 0;
  if (!client_removed) {
    LOG_WARN(
        "Failed to remove targeted announcement connection app %d for address "
        "%s",
        static_cast<int>(app_id), ADDRESS_TO_LOGGABLE_CSTR(address));
    return false;
  }

  if (it->second.state == ConnectionState::INITIATING) {
    // if we are initiating, this app needs to be de-registered from the
    // connection manager
    connection_manager::direct_connect_remove(app_id, address);
  }

  if (it->second.clients.size() > 0) {
    LOG_DEBUG("some client is still connecting, app_id=%d, address=%s",
              static_cast<int>(app_id), ADDRESS_TO_LOGGABLE_CSTR(address));
    return true;
  }

  BTM_LogHistory(kBtmLogTag, address, "Ignore connection from");

  if (pending_connections_.empty()) {
    SetTargetedAnnouncementsFilter(false);
  }

  return true;
}

TargetedAnnouncementsManager& TargetedAnnouncementsManager::Get() {
  static auto self = TargetedAnnouncementsManager();
  return self;
}

}  // namespace targeted_announcements