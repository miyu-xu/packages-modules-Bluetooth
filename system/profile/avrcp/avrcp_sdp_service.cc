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

#include "avrcp_sdp_service.h"

#include "stack/include/avrc_api.h"
#include "stack/include/bt_uuid16.h"

namespace bluetooth {
namespace avrcp {
std::shared_ptr<AvrcpSdpService> AvrcpSdpService::instance_ =
    nullptr;  // Static member definition

std::shared_ptr<AvrcpSdpService> AvrcpSdpService::Get() {
  static std::once_flag onceFlag;
  std::call_once(onceFlag,
                 []() { instance_ = std::make_shared<AvrcpSdpService>(); });
  return instance_;
}

uint16_t AvrcpSdpService::AddRecord(
    const AddSdpRecordRequest& add_sdp_record_request) {
  if (add_sdp_record_request.service_uuid ==
      UUID_SERVCLASS_AV_REM_CTRL_TARGET) {
    return target_sdp_record_helper_.AddRecord(add_sdp_record_request);
  } else if (add_sdp_record_request.service_uuid ==
             UUID_SERVCLASS_AV_REMOTE_CONTROL) {
    return control_sdp_record_helper_.AddRecord(add_sdp_record_request);
  }
  return AVRC_FAIL;
}

uint16_t AvrcpSdpService::EnableCovertArt(uint16_t service_uuid,
                                          uint16_t cover_art_psm) {
  if (service_uuid == UUID_SERVCLASS_AV_REM_CTRL_TARGET) {
    return target_sdp_record_helper_.EnableCovertArt(cover_art_psm);
  } else if (service_uuid == UUID_SERVCLASS_AV_REMOTE_CONTROL) {
    return control_sdp_record_helper_.EnableCovertArt(cover_art_psm);
  }
  return AVRC_FAIL;
}

uint16_t AvrcpSdpService::DisableCovertArt(uint16_t service_uuid) {
  if (service_uuid == UUID_SERVCLASS_AV_REM_CTRL_TARGET) {
    return target_sdp_record_helper_.DisableCovertArt();
  } else if (service_uuid == UUID_SERVCLASS_AV_REMOTE_CONTROL) {
    return control_sdp_record_helper_.DisableCovertArt();
  }
  return AVRC_FAIL;
}

uint16_t AvrcpSdpService::RemoveRecord(uint16_t service_uuid) {
  if (service_uuid == UUID_SERVCLASS_AV_REM_CTRL_TARGET) {
    return target_sdp_record_helper_.RemoveRecord();
  } else if (service_uuid == UUID_SERVCLASS_AV_REMOTE_CONTROL) {
    return control_sdp_record_helper_.RemoveRecord();
  }
  return AVRC_FAIL;
}

}  // namespace avrcp
}  // namespace bluetooth
