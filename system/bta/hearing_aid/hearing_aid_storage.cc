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

#include "hearing_aid_storage.h"

#include <base/bind.h>
#include <base/callback.h>
#include <base/location.h>

#include "btif/include/btif_common.h"
#include "btif/include/btif_config.h"
#include "btif/include/btif_storage.h"
#include "stack/include/btu.h"  // do_in_main_thread

constexpr char HEARING_AID_READ_PSM_HANDLE[] = "HearingAidReadPsmHandle";
constexpr char HEARING_AID_CAPABILITIES[] = "HearingAidCapabilities";
constexpr char HEARING_AID_CODECS[] = "HearingAidCodecs";
constexpr char HEARING_AID_AUDIO_CONTROL_POINT[] =
    "HearingAidAudioControlPoint";
constexpr char HEARING_AID_VOLUME_HANDLE[] = "HearingAidVolumeHandle";
constexpr char HEARING_AID_AUDIO_STATUS_HANDLE[] =
    "HearingAidAudioStatusHandle";
constexpr char HEARING_AID_AUDIO_STATUS_CCC_HANDLE[] =
    "HearingAidAudioStatusCccHandle";
constexpr char HEARING_AID_SERVICE_CHANGED_CCC_HANDLE[] =
    "HearingAidServiceChangedCccHandle";
constexpr char HEARING_AID_SYNC_ID[] = "HearingAidSyncId";
constexpr char HEARING_AID_RENDER_DELAY[] = "HearingAidRenderDelay";
constexpr char HEARING_AID_PREPARATION_DELAY[] = "HearingAidPreparationDelay";
constexpr char HEARING_AID_IS_ACCEPTLISTED[] = "HearingAidIsAcceptlisted";

namespace bluetooth {
namespace hearing_aid {
namespace storage {

void AddHearingDeviceToStorage(const HearingDevice& dev_info) {
  do_in_jni_thread(
      FROM_HERE,
      base::Bind(
          [](const HearingDevice& dev_info) {
            std::string bdstr = dev_info.address.ToString();
            VLOG(2) << "saving hearing aid device: " << bdstr;
            btif_config_set_int(bdstr, HEARING_AID_SERVICE_CHANGED_CCC_HANDLE,
                                dev_info.service_changed_ccc_handle);
            btif_config_set_int(bdstr, HEARING_AID_READ_PSM_HANDLE,
                                dev_info.read_psm_handle);
            btif_config_set_int(bdstr, HEARING_AID_CAPABILITIES,
                                dev_info.capabilities);
            btif_config_set_int(bdstr, HEARING_AID_CODECS, dev_info.codecs);
            btif_config_set_int(bdstr, HEARING_AID_AUDIO_CONTROL_POINT,
                                dev_info.audio_control_point_handle);
            btif_config_set_int(bdstr, HEARING_AID_VOLUME_HANDLE,
                                dev_info.volume_handle);
            btif_config_set_int(bdstr, HEARING_AID_AUDIO_STATUS_HANDLE,
                                dev_info.audio_status_handle);
            btif_config_set_int(bdstr, HEARING_AID_AUDIO_STATUS_CCC_HANDLE,
                                dev_info.audio_status_ccc_handle);
            btif_config_set_uint64(bdstr, HEARING_AID_SYNC_ID,
                                   dev_info.hi_sync_id);
            btif_config_set_int(bdstr, HEARING_AID_RENDER_DELAY,
                                dev_info.render_delay);
            btif_config_set_int(bdstr, HEARING_AID_PREPARATION_DELAY,
                                dev_info.preparation_delay);
            btif_config_set_int(bdstr, HEARING_AID_IS_ACCEPTLISTED, true);
            btif_config_save();
          },
          dev_info));
}

/** Deletes the bonded hearing aid device info from NVRAM */
void RemoveHearingDeviceFromStorage(const RawAddress& address) {
  std::string addrstr = address.ToString();
  btif_config_remove(addrstr, HEARING_AID_READ_PSM_HANDLE);
  btif_config_remove(addrstr, HEARING_AID_CAPABILITIES);
  btif_config_remove(addrstr, HEARING_AID_CODECS);
  btif_config_remove(addrstr, HEARING_AID_AUDIO_CONTROL_POINT);
  btif_config_remove(addrstr, HEARING_AID_VOLUME_HANDLE);
  btif_config_remove(addrstr, HEARING_AID_AUDIO_STATUS_HANDLE);
  btif_config_remove(addrstr, HEARING_AID_AUDIO_STATUS_CCC_HANDLE);
  btif_config_remove(addrstr, HEARING_AID_SERVICE_CHANGED_CCC_HANDLE);
  btif_config_remove(addrstr, HEARING_AID_SYNC_ID);
  btif_config_remove(addrstr, HEARING_AID_RENDER_DELAY);
  btif_config_remove(addrstr, HEARING_AID_PREPARATION_DELAY);
  btif_config_remove(addrstr, HEARING_AID_IS_ACCEPTLISTED);
  btif_config_save();
}

/** Loads information about bonded hearing aid devices */
void LoadBondedHearingAidsFromStorage() {
  for (const auto& bd_addr : btif_config_get_paired_devices()) {
    const std::string& name = bd_addr.ToString();

    int size = STORAGE_UUID_STRING_SIZE * HEARINGAID_MAX_NUM_UUIDS;
    char uuid_str[size];
    bool isHearingaidDevice = false;
    if (btif_config_get_str(name, BTIF_STORAGE_PATH_REMOTE_SERVICE, uuid_str,
                            &size)) {
      Uuid p_uuid[HEARINGAID_MAX_NUM_UUIDS];
      size_t num_uuids =
          btif_split_uuids_string(uuid_str, p_uuid, HEARINGAID_MAX_NUM_UUIDS);
      for (size_t i = 0; i < num_uuids; i++) {
        if (p_uuid[i] == Uuid::FromString("FDF0")) {
          isHearingaidDevice = true;
          break;
        }
      }
    }
    if (!isHearingaidDevice) {
      continue;
    }

    BTIF_TRACE_DEBUG("Remote device:%s", name.c_str());

    if (btif_in_fetch_bonded_device(name) != BT_STATUS_SUCCESS) {
      RemoveHearingDeviceFromStorage(bd_addr);
      continue;
    }

    int value;
    uint8_t capabilities = 0;
    if (btif_config_get_int(name, HEARING_AID_CAPABILITIES, &value))
      capabilities = value;

    uint16_t codecs = 0;
    if (btif_config_get_int(name, HEARING_AID_CODECS, &value)) codecs = value;

    uint16_t audio_control_point_handle = 0;
    if (btif_config_get_int(name, HEARING_AID_AUDIO_CONTROL_POINT, &value))
      audio_control_point_handle = value;

    uint16_t audio_status_handle = 0;
    if (btif_config_get_int(name, HEARING_AID_AUDIO_STATUS_HANDLE, &value))
      audio_status_handle = value;

    uint16_t audio_status_ccc_handle = 0;
    if (btif_config_get_int(name, HEARING_AID_AUDIO_STATUS_CCC_HANDLE, &value))
      audio_status_ccc_handle = value;

    uint16_t service_changed_ccc_handle = 0;
    if (btif_config_get_int(name, HEARING_AID_SERVICE_CHANGED_CCC_HANDLE,
                            &value))
      service_changed_ccc_handle = value;

    uint16_t volume_handle = 0;
    if (btif_config_get_int(name, HEARING_AID_VOLUME_HANDLE, &value))
      volume_handle = value;

    uint16_t read_psm_handle = 0;
    if (btif_config_get_int(name, HEARING_AID_READ_PSM_HANDLE, &value))
      read_psm_handle = value;

    uint64_t lvalue;
    uint64_t hi_sync_id = 0;
    if (btif_config_get_uint64(name, HEARING_AID_SYNC_ID, &lvalue))
      hi_sync_id = lvalue;

    uint16_t render_delay = 0;
    if (btif_config_get_int(name, HEARING_AID_RENDER_DELAY, &value))
      render_delay = value;

    uint16_t preparation_delay = 0;
    if (btif_config_get_int(name, HEARING_AID_PREPARATION_DELAY, &value))
      preparation_delay = value;

    uint16_t is_acceptlisted = 0;
    if (btif_config_get_int(name, HEARING_AID_IS_ACCEPTLISTED, &value))
      is_acceptlisted = value;

    // add extracted information to BTA Hearing Aid
    do_in_main_thread(
        FROM_HERE,
        base::Bind(
            &HearingAid::AddFromStorage,
            HearingDevice(bd_addr, capabilities, codecs,
                          audio_control_point_handle, audio_status_handle,
                          audio_status_ccc_handle, service_changed_ccc_handle,
                          volume_handle, read_psm_handle, hi_sync_id,
                          render_delay, preparation_delay),
            is_acceptlisted));
  }
}

/** Set/Unset the hearing aid device HEARING_AID_IS_ACCEPTLISTED flag. */
void SetHearingDeviceAcceptlist(const RawAddress& address,
                                bool add_to_acceptlist) {
  std::string addrstr = address.ToString();

  btif_config_set_int(addrstr, HEARING_AID_IS_ACCEPTLISTED, add_to_acceptlist);
  btif_config_save();
}

/** Get the hearing aid device properties. */
bool GetHearingDeviceProperties(const RawAddress& address,
                                uint8_t* capabilities, uint64_t* hi_sync_id,
                                uint16_t* render_delay,
                                uint16_t* preparation_delay, uint16_t* codecs) {
  std::string addrstr = address.ToString();

  int value;
  if (btif_config_get_int(addrstr, HEARING_AID_CAPABILITIES, &value)) {
    *capabilities = value;
  } else {
    return false;
  }

  if (btif_config_get_int(addrstr, HEARING_AID_CODECS, &value)) {
    *codecs = value;
  } else {
    return false;
  }

  if (btif_config_get_int(addrstr, HEARING_AID_RENDER_DELAY, &value)) {
    *render_delay = value;
  } else {
    return false;
  }

  if (btif_config_get_int(addrstr, HEARING_AID_PREPARATION_DELAY, &value)) {
    *preparation_delay = value;
  } else {
    return false;
  }

  uint64_t lvalue;
  if (btif_config_get_uint64(addrstr, HEARING_AID_SYNC_ID, &lvalue)) {
    *hi_sync_id = lvalue;
  } else {
    return false;
  }

  return true;
}

}  // namespace storage
}  // namespace hearing_aid
}  // namespace bluetooth