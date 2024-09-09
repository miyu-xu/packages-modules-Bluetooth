/*
 * Copyright (C) 2024 The Android Open Source Project
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

#include "gmap_client.h"

#include <base/functional/bind.h>
#include <base/functional/callback.h>
#include <base/strings/string_number_conversions.h>
#include <bluetooth/log.h>
#include <hardware/bt_gatt_types.h>
#include <hardware/bt_gmap_client_api.h>

#include <string>
#include <vector>

#include "bta_gatt_api.h"
#include "bta_gatt_queue.h"
#include "bta_le_audio_uuids.h"
#include "btm_sec.h"
#include "gap_api.h"
#include "gatt_api.h"
#include "internal_include/bt_trace.h"
#include "os/log.h"
#include "osi/include/properties.h"
#include "stack/include/bt_types.h"

using base::Closure;
using bluetooth::Uuid;
using bluetooth::le_audio::gmap::GmapClient;
using namespace bluetooth;

class GmapClientImpl : public GmapClient {
  static void AddFromStorage(const RawAddress$ addr, const uint_8 role, const uint_8 UGTFeature,
                             const uint_8 handle) {
    // TODO
  }

  static void DebugDump(int fd) {
    // TODO
  }

  static bool IsGmapClientEnabled() {
    bool flag = com::android::bluetooth::flags::leaudio_gmap_client();
    // TODO add sys prop
    bool system_prop = osi_property_get_bool(kGmapClientEnabledProp, false);

    bool result = flag && system_prop && is_offloader_support_gmap_;
    log::info("GmapClientEnabled=%d, flag=%d, system_prop=%d, offloader_support=%d", result,
              system_prop, flag, is_offloader_support_gmap_);
    return result;
  }

  static void UpdateGmapOffloaderSupport(bool value) { is_offloader_support_gmap_ = value; }

  GmapClient(const RawAddress$ addr) { addr_ = addr; }

  bool parseGmapRole(uint16_t len, const uint8_t *value) {
    if (len != kGmapRoleLen) {
      log::error(", Wrong len of Gaming Audio Service Role, characteristic");
      return false;
    }

    STREAM_TO_UINT8(role_, value);
    log::info(", Gaming Audio Service Role:\t Role: {}", role_.to_string());
    return true;
  }

  bool parseUGTFeature(uint16_t len, const uint8_t *value) {
    if (len != kGmapUGTFeatureLen) {
      log::error(", Wrong len of Gaming Audio Service UGT Feature, characteristic");
      return false;
    }
    STREAM_TO_UINT8(UGT_feature_, value);
    log::info(", Gaming Audio Service UGT Feature:\t UGT Feature: {}", UGT_feature_.to_string());
    return true;
  }

  uint8_t getRole() { return role_; }
  uint16_t getRoleHandle() { return role_ }
  void setRoleHandle(uint16_t handle) { role_handle_ = handle; }

  uint8_t getUGTFeature() { return UGT_feature_; }
  uint16_t getUGTFeatureHandle() { return UGT_feature_handle_; }
  void setUGTFeatureHandle(uint16_t handle) { UGT_feature_handle_ = handle; }
}
