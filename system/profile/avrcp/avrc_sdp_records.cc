/*
 * Copyright 2024 The Android Open Source Project
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

#include "avrc_sdp_records.h"

#include <bluetooth/log.h>

#include "bta/sys/bta_sys.h"
#include "stack/include/avrc_api.h"
#include "stack/include/bt_types.h"
#include "stack/include/bt_uuid16.h"

using namespace bluetooth::legacy::stack::sdp;

namespace bluetooth {
namespace avrc {

uint16_t AvrcSdpRecordHelper::AddRecord(
    const AddSdpRecordRequest& add_sdp_record_request, const bool add_sys_uid) {
  if (sdp_record_handle_ == -1u) {
    cached_add_sdp_record_request_ = add_sdp_record_request;
    log::debug("Adding a new record for {} 0x{:x}",
               cached_add_sdp_record_request_.service_name,
               cached_add_sdp_record_request_.service_uuid);
    sdp_record_handle_ = get_legacy_stack_sdp_api()->handle.SDP_CreateRecord();
    if (add_sys_uid) {
      bta_sys_add_uuid(cached_add_sdp_record_request_.service_uuid);
    }
    return AVRC_AddRecord(cached_add_sdp_record_request_.service_uuid,
                          cached_add_sdp_record_request_.service_name.c_str(),
                          cached_add_sdp_record_request_.provider_name.c_str(),
                          cached_add_sdp_record_request_.categories,
                          sdp_record_handle_,
                          cached_add_sdp_record_request_.browse_supported,
                          cached_add_sdp_record_request_.profile_version,
                          cached_add_sdp_record_request_.cover_art_psm);
  } else {
    // SDP record is already present. Update the existing SDP record with the
    // new supported categories.
    return UpdateRecord(add_sdp_record_request.categories);
  }
}

uint16_t AvrcSdpRecordHelper::UpdateRecord(const uint16_t& new_categories) {
  // Set all supported categories to
  cached_add_sdp_record_request_.categories |= new_categories;
  log::debug(
      "Adding additional categories 0x{:x}. Final supported categories 0x{:x}",
      new_categories, cached_add_sdp_record_request_.categories);
  uint8_t temp[sizeof(uint16_t)], *p;
  p = temp;
  UINT16_TO_BE_STREAM(p, cached_add_sdp_record_request_.categories);
  return get_legacy_stack_sdp_api()->handle.SDP_AddAttribute(
             sdp_record_handle_, ATTR_ID_SUPPORTED_FEATURES, UINT_DESC_TYPE,
             sizeof(temp), (uint8_t*)temp)
             ? AVRC_SUCCESS
             : AVRC_FAIL;
}

uint16_t AvrcSdpRecordHelper::RemoveRecord() {
  if (sdp_record_handle_ != -1u) {
    bta_sys_remove_uuid(cached_add_sdp_record_request_.service_uuid);
    sdp_record_handle_ = -1;
    return AVRC_RemoveRecord(sdp_record_handle_);
  }
  // Nothing to remove.
  return AVRC_SUCCESS;
}

uint16_t TargetAvrcSdpRecordHelper::EnableCovertArt(uint16_t cover_art_psm) {
  log::debug("Adding cover art support");
  AVRC_RemoveRecord(sdp_record_handle_);
  sdp_record_handle_ = -1;
  cached_add_sdp_record_request_.cover_art_psm = cover_art_psm;
  cached_add_sdp_record_request_.AddToExistingCategories(
      AVRC_SUPF_TG_PLAYER_COVER_ART);
  return AddRecord(cached_add_sdp_record_request_, false);
}

uint16_t TargetAvrcSdpRecordHelper::DisableCovertArt() {
  log::debug("Disabling cover art support");
  AVRC_RemoveRecord(sdp_record_handle_);
  sdp_record_handle_ = -1;
  cached_add_sdp_record_request_.cover_art_psm = 0;
  cached_add_sdp_record_request_.RemoveCategory(AVRC_SUPF_TG_PLAYER_COVER_ART);
  return AddRecord(cached_add_sdp_record_request_, false);
}

uint16_t ControlAvrcSdpRecordHelper::AddRecord(
    const AddSdpRecordRequest& add_sdp_record_request, const bool add_sys_uid) {
  if (sdp_record_handle_ == -1u) {
    return AvrcSdpRecordHelper::AddRecord(add_sdp_record_request, add_sys_uid);
  } else {
    // Handle already exists, update records.
    bool result =
        AvrcSdpRecordHelper::UpdateRecord(add_sdp_record_request.categories)
            ? AVRC_SUCCESS
            : AVRC_FAIL;
    if (cached_add_sdp_record_request_.profile_version <
        add_sdp_record_request.profile_version) {
      if (add_sdp_record_request.profile_version > AVRC_REV_1_3 &&
          cached_add_sdp_record_request_.profile_version <= AVRC_REV_1_3) {
        uint16_t class_list[2], count = 1;
        class_list[0] = add_sdp_record_request.service_uuid;
        if (add_sdp_record_request.service_uuid ==
            UUID_SERVCLASS_AV_REMOTE_CONTROL) {
          class_list[1] = UUID_SERVCLASS_AV_REM_CTRL_CONTROL;
          count = 2;
        }
        result &= get_legacy_stack_sdp_api()->handle.SDP_AddServiceClassIdList(
            sdp_record_handle_, count, class_list);
      }
      cached_add_sdp_record_request_.profile_version =
          add_sdp_record_request.profile_version;
      result &= get_legacy_stack_sdp_api()->handle.SDP_AddProfileDescriptorList(
          sdp_record_handle_, add_sdp_record_request.service_uuid,
          add_sdp_record_request.profile_version);
    }
    return result ? AVRC_SUCCESS : AVRC_FAIL;
  }
}

uint16_t ControlAvrcSdpRecordHelper::EnableCovertArt(uint16_t cover_art_psm) {
  log::warn(
      "Enabling cover art support dynamically is not supported for service "
      "UUID {:x}",
      cached_add_sdp_record_request_.service_uuid);
  return AVRC_FAIL;
}

uint16_t ControlAvrcSdpRecordHelper::DisableCovertArt() {
  log::warn(
      "Disabling cover art support dynamically is not supported for service "
      "UUID {:x}",
      cached_add_sdp_record_request_.service_uuid);
  return AVRC_FAIL;
}

}  // namespace avrc
}  // namespace bluetooth
