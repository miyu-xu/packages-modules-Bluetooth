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

#define LOG_TAG "bt_bta_dm"

#include <android_bluetooth_flags.h>
#include <base/functional/bind.h>
#include <base/strings/stringprintf.h>
#include <bluetooth/log.h>

#include <cstddef>
#include <cstdint>
#include <string>
#include <variant>
#include <vector>

#include "android_bluetooth_flags.h"
#include "bta/dm/bta_dm_disc.h"
#include "bta/dm/bta_dm_disc_int.h"
#include "bta/include/bta_sdp_api.h"
#include "btif/include/btif_config.h"
#include "common/init_flags.h"
#include "common/strings.h"
#include "main/shim/dumpsys.h"
#include "os/logging/log_adapter.h"
#include "osi/include/allocator.h"
#include "stack/include/bt_name.h"
#include "stack/include/bt_uuid16.h"
#include "stack/include/btm_client_interface.h"
#include "stack/include/btm_log_history.h"
#include "stack/include/hidh_api.h"
#include "stack/include/main_thread.h"
#include "stack/include/sdp_status.h"
#include "stack/sdp/sdpint.h"  // is_sdp_pbap_pce_disabled
#include "storage/config_keys.h"
#include "types/raw_address.h"

#define MAX_DISC_RAW_DATA_BUF (4096)
static uint8_t g_disc_raw_data_buf[MAX_DISC_RAW_DATA_BUF];

using namespace bluetooth::legacy::stack::sdp;
using namespace bluetooth;

namespace {
constexpr char kBtmLogTag[] = "SDP";
}  // namespace

static void bta_dm_sdp_result(tSDP_STATUS sdp_result);
void bta_dm_find_services(const RawAddress& bd_addr);
tBTA_DM_SERVICE_DISCOVERY_STATE bta_dm_discovery_get_state();
void bta_dm_disc_sm_execute(tBTA_DM_DISC_EVT event,
                            std::unique_ptr<tBTA_DM_MSG> msg);

extern tBTA_DM_SERVICE_DISCOVERY_CB bta_dm_discovery_cb;

/* bta_dm_free_sdp_db */
static void bta_dm_free_sdp_db() {
  osi_free_and_reset((void**)&bta_dm_discovery_cb.p_sdp_db);
}

static void store_avrcp_profile_feature(tSDP_DISC_REC* sdp_rec) {
  tSDP_DISC_ATTR* p_attr =
      get_legacy_stack_sdp_api()->record.SDP_FindAttributeInRec(
          sdp_rec, ATTR_ID_SUPPORTED_FEATURES);
  if (p_attr == NULL) {
    return;
  }

  uint16_t avrcp_features = p_attr->attr_value.v.u16;
  if (avrcp_features == 0) {
    return;
  }

  if (btif_config_set_bin(sdp_rec->remote_bd_addr.ToString().c_str(),
                          BTIF_STORAGE_KEY_AV_REM_CTRL_FEATURES,
                          (const uint8_t*)&avrcp_features,
                          sizeof(avrcp_features))) {
    log::info("Saving avrcp_features: 0x{:x}", avrcp_features);
  } else {
    log::info("Failed to store avrcp_features 0x{:x} for {}", avrcp_features,
              sdp_rec->remote_bd_addr);
  }
}

static void bta_dm_store_audio_profiles_version() {
  struct AudioProfile {
    const uint16_t servclass_uuid;
    const uint16_t btprofile_uuid;
    const char* profile_key;
    void (*store_audio_profile_feature)(tSDP_DISC_REC*);
  };

  std::array<AudioProfile, 1> audio_profiles = {{
      {
          .servclass_uuid = UUID_SERVCLASS_AV_REMOTE_CONTROL,
          .btprofile_uuid = UUID_SERVCLASS_AV_REMOTE_CONTROL,
          .profile_key = BTIF_STORAGE_KEY_AVRCP_CONTROLLER_VERSION,
          .store_audio_profile_feature = store_avrcp_profile_feature,
      },
  }};

  for (const auto& audio_profile : audio_profiles) {
    tSDP_DISC_REC* sdp_rec = get_legacy_stack_sdp_api()->db.SDP_FindServiceInDb(
        bta_dm_discovery_cb.p_sdp_db, audio_profile.servclass_uuid, NULL);
    if (sdp_rec == NULL) continue;

    if (get_legacy_stack_sdp_api()->record.SDP_FindAttributeInRec(
            sdp_rec, ATTR_ID_BT_PROFILE_DESC_LIST) == NULL)
      continue;

    uint16_t profile_version = 0;
    /* get profile version (if failure, version parameter is not updated) */
    get_legacy_stack_sdp_api()->record.SDP_FindProfileVersionInRec(
        sdp_rec, audio_profile.btprofile_uuid, &profile_version);
    if (profile_version != 0) {
      if (btif_config_set_bin(sdp_rec->remote_bd_addr.ToString().c_str(),
                              audio_profile.profile_key,
                              (const uint8_t*)&profile_version,
                              sizeof(profile_version))) {
      } else {
        log::info("Failed to store peer profile version for {}",
                  sdp_rec->remote_bd_addr);
      }
    }
    audio_profile.store_audio_profile_feature(sdp_rec);
  }
}

/* Callback from sdp with discovery status */
static void bta_dm_sdp_callback(const RawAddress& /* bd_addr */,
                                tSDP_STATUS sdp_status) {
  log::info("{}", bta_dm_state_text(bta_dm_discovery_get_state()));

  if (bta_dm_discovery_get_state() == BTA_DM_DISCOVER_IDLE) {
    bta_dm_free_sdp_db();
    return;
  }

  do_in_main_thread(FROM_HERE, base::BindOnce(&bta_dm_sdp_result, sdp_status));
}

/* Process the discovery result from sdp */
static void bta_dm_sdp_result(tSDP_STATUS sdp_result) {
  tSDP_DISC_REC* p_sdp_rec = NULL;
  bool scn_found = false;
  uint16_t service = 0xFFFF;
  tSDP_PROTOCOL_ELEM pe;

  std::vector<Uuid> uuid_list;

  if ((sdp_result == SDP_SUCCESS) || (sdp_result == SDP_NO_RECS_MATCH) ||
      (sdp_result == SDP_DB_FULL)) {
    log::verbose("sdp_result::0x{:x}", sdp_result);
    do {
      p_sdp_rec = NULL;
      if (bta_dm_discovery_cb.service_index == (BTA_USER_SERVICE_ID + 1)) {
        if (p_sdp_rec &&
            get_legacy_stack_sdp_api()->record.SDP_FindProtocolListElemInRec(
                p_sdp_rec, UUID_PROTOCOL_RFCOMM, &pe)) {
          bta_dm_discovery_cb.peer_scn = (uint8_t)pe.params[0];
          scn_found = true;
        }
      } else {
        service =
            bta_service_id_to_uuid_lkup_tbl[bta_dm_discovery_cb.service_index -
                                            1];
        p_sdp_rec = get_legacy_stack_sdp_api()->db.SDP_FindServiceInDb(
            bta_dm_discovery_cb.p_sdp_db, service, p_sdp_rec);
      }
      /* finished with BR/EDR services, now we check the result for GATT based
       * service UUID */
      if (bta_dm_discovery_cb.service_index == BTA_MAX_SERVICE_ID) {
        /* all GATT based services */

        std::vector<Uuid> gatt_uuids;

        do {
          /* find a service record, report it */
          p_sdp_rec = get_legacy_stack_sdp_api()->db.SDP_FindServiceInDb(
              bta_dm_discovery_cb.p_sdp_db, 0, p_sdp_rec);
          if (p_sdp_rec) {
            Uuid service_uuid;
            if (get_legacy_stack_sdp_api()->record.SDP_FindServiceUUIDInRec(
                    p_sdp_rec, &service_uuid)) {
              gatt_uuids.push_back(service_uuid);
            }
          }
        } while (p_sdp_rec);

        if (!gatt_uuids.empty()) {
          log::info("GATT services discovered using SDP");

          // send all result back to app
          bta_dm_discovery_cb.service_search_cbacks.on_gatt_results(
              bta_dm_discovery_cb.peer_bdaddr, BD_NAME{}, gatt_uuids,
              /* transport_le */ false);
        }
      } else {
        if ((p_sdp_rec != NULL)) {
          if (service != UUID_SERVCLASS_PNP_INFORMATION) {
            bta_dm_discovery_cb.services_found |=
                (tBTA_SERVICE_MASK)(BTA_SERVICE_ID_TO_SERVICE_MASK(
                    bta_dm_discovery_cb.service_index - 1));
            uint16_t tmp_svc = bta_service_id_to_uuid_lkup_tbl
                [bta_dm_discovery_cb.service_index - 1];
            /* Add to the list of UUIDs */
            uuid_list.push_back(Uuid::From16Bit(tmp_svc));
          }
        }
      }

      if (bta_dm_discovery_cb.services_to_search == 0) {
        bta_dm_discovery_cb.service_index++;
      } else /* regular one service per search or PNP search */
        break;

    } while (bta_dm_discovery_cb.service_index <= BTA_MAX_SERVICE_ID);

    log::verbose("services_found = {:04x}", bta_dm_discovery_cb.services_found);

    /* Collect the 128-bit services here and put them into the list */
    p_sdp_rec = NULL;
    do {
      /* find a service record, report it */
      p_sdp_rec = get_legacy_stack_sdp_api()->db.SDP_FindServiceInDb_128bit(
          bta_dm_discovery_cb.p_sdp_db, p_sdp_rec);
      if (p_sdp_rec) {
        // SDP_FindServiceUUIDInRec_128bit is used only once, refactor?
        Uuid temp_uuid;
        if (get_legacy_stack_sdp_api()->record.SDP_FindServiceUUIDInRec_128bit(
                p_sdp_rec, &temp_uuid)) {
          uuid_list.push_back(temp_uuid);
        }
      }
    } while (p_sdp_rec);

    if (bluetooth::common::init_flags::
            dynamic_avrcp_version_enhancement_is_enabled() &&
        bta_dm_discovery_cb.services_to_search == 0) {
      bta_dm_store_audio_profiles_version();
    }

#if TARGET_FLOSS
    tSDP_DI_GET_RECORD di_record;
    if (get_legacy_stack_sdp_api()->device_id.SDP_GetDiRecord(
            1, &di_record, bta_dm_discovery_cb.p_sdp_db) == SDP_SUCCESS) {
      bta_dm_discovery_cb.service_search_cbacks.on_did_received(
          bta_dm_discovery_cb.peer_bdaddr, di_record.rec.vendor_id_source,
          di_record.rec.vendor, di_record.rec.product, di_record.rec.version);
    }
#endif

    /* if there are more services to search for */
    if (bta_dm_discovery_cb.services_to_search) {
      /* Free up the p_sdp_db before checking the next one */
      bta_dm_free_sdp_db();
      bta_dm_find_services(bta_dm_discovery_cb.peer_bdaddr);
    } else {
      /* callbacks */
      /* start next bd_addr if necessary */
      BTM_LogHistory(
          kBtmLogTag, bta_dm_discovery_cb.peer_bdaddr, "Discovery completed",
          base::StringPrintf("Result:%s services_found:0x%x service_index:0x%d",
                             sdp_result_text(sdp_result).c_str(),
                             bta_dm_discovery_cb.services_found,
                             bta_dm_discovery_cb.service_index));

      auto msg = std::make_unique<tBTA_DM_MSG>(tBTA_DM_SVC_RES{});
      auto& disc_result = std::get<tBTA_DM_SVC_RES>(*msg);

      disc_result.result = BTA_SUCCESS;
      disc_result.uuids = std::move(uuid_list);
      // Copy the raw_data to the discovery result structure
      if (bta_dm_discovery_cb.p_sdp_db != NULL &&
          bta_dm_discovery_cb.p_sdp_db->raw_used != 0 &&
          bta_dm_discovery_cb.p_sdp_db->raw_data != NULL) {
        log::verbose("raw_data used = 0x{:x} raw_data_ptr = 0x{}",
                     bta_dm_discovery_cb.p_sdp_db->raw_used,
                     fmt::ptr(bta_dm_discovery_cb.p_sdp_db->raw_data));

        bta_dm_discovery_cb.p_sdp_db->raw_data =
            NULL;  // no need to free this - it is a global assigned.
        bta_dm_discovery_cb.p_sdp_db->raw_used = 0;
        bta_dm_discovery_cb.p_sdp_db->raw_size = 0;
      } else {
        log::verbose("raw data size is 0 or raw_data is null!!");
      }
      /* Done with p_sdp_db. Free it */
      bta_dm_free_sdp_db();
      disc_result.services = bta_dm_discovery_cb.services_found;

      // Piggy back the SCN over result field
      if (scn_found) {
        disc_result.result =
            static_cast<tBTA_STATUS>((3 + bta_dm_discovery_cb.peer_scn));
        disc_result.services |= BTA_USER_SERVICE_MASK;

        log::verbose("Piggy back the SCN over result field  SCN={}",
                     bta_dm_discovery_cb.peer_scn);
      }
      disc_result.bd_addr = bta_dm_discovery_cb.peer_bdaddr;

      bta_dm_disc_sm_execute(BTA_DM_DISCOVERY_RESULT_EVT, std::move(msg));
    }
  } else {
    BTM_LogHistory(
        kBtmLogTag, bta_dm_discovery_cb.peer_bdaddr, "Discovery failed",
        base::StringPrintf("Result:%s", sdp_result_text(sdp_result).c_str()));
    log::error("SDP connection failed {}", sdp_status_text(sdp_result));
    if (sdp_result == SDP_CONN_FAILED)
      bta_dm_discovery_cb.wait_disc = false;

    /* not able to connect go to next device */
    if (bta_dm_discovery_cb.p_sdp_db)
      osi_free_and_reset((void**)&bta_dm_discovery_cb.p_sdp_db);

    auto msg = std::make_unique<tBTA_DM_MSG>(tBTA_DM_SVC_RES{});
    auto& disc_result = std::get<tBTA_DM_SVC_RES>(*msg);

    disc_result.result = BTA_FAILURE;
    disc_result.services = bta_dm_discovery_cb.services_found;
    disc_result.bd_addr = bta_dm_discovery_cb.peer_bdaddr;

    bta_dm_disc_sm_execute(BTA_DM_DISCOVERY_RESULT_EVT, std::move(msg));
  }
}

/*******************************************************************************
 *
 * Function         bta_dm_find_services
 *
 * Description      Starts discovery on a device
 *
 * Returns          void
 *
 ******************************************************************************/
void bta_dm_find_services(const RawAddress& bd_addr) {
  while (bta_dm_discovery_cb.service_index < BTA_MAX_SERVICE_ID) {
    if (bta_dm_discovery_cb.services_to_search &
        (tBTA_SERVICE_MASK)(BTA_SERVICE_ID_TO_SERVICE_MASK(
            bta_dm_discovery_cb.service_index))) {
      break;
    }
    bta_dm_discovery_cb.service_index++;
  }

  /* no more services to be discovered */
  if (bta_dm_discovery_cb.service_index >= BTA_MAX_SERVICE_ID) {
    log::info("SDP - no more services to discover");
    bta_dm_disc_sm_execute(BTA_DM_DISCOVERY_RESULT_EVT,
                           std::make_unique<tBTA_DM_MSG>(tBTA_DM_SVC_RES{
                               .bd_addr = bta_dm_discovery_cb.peer_bdaddr,
                               .services = bta_dm_discovery_cb.services_found,
                               .result = BTA_SUCCESS}));
    return;
  }

  /* try to search all services by search based on L2CAP UUID */
  log::info("services_to_search={:08x}",
            bta_dm_discovery_cb.services_to_search);
  Uuid uuid = Uuid::kEmpty;
  if (bta_dm_discovery_cb.services_to_search & BTA_RES_SERVICE_MASK) {
    uuid = Uuid::From16Bit(bta_service_id_to_uuid_lkup_tbl[0]);
    bta_dm_discovery_cb.services_to_search &= ~BTA_RES_SERVICE_MASK;
  } else {
    uuid = Uuid::From16Bit(UUID_PROTOCOL_L2CAP);
    bta_dm_discovery_cb.services_to_search = 0;
  }

  bta_dm_discovery_cb.p_sdp_db =
      (tSDP_DISCOVERY_DB*)osi_malloc(BTA_DM_SDP_DB_SIZE);

  log::info("search UUID = {}", uuid.ToString());
  get_legacy_stack_sdp_api()->service.SDP_InitDiscoveryDb(
      bta_dm_discovery_cb.p_sdp_db, BTA_DM_SDP_DB_SIZE, 1, &uuid, 0, NULL);

  memset(g_disc_raw_data_buf, 0, sizeof(g_disc_raw_data_buf));
  bta_dm_discovery_cb.p_sdp_db->raw_data = g_disc_raw_data_buf;

  bta_dm_discovery_cb.p_sdp_db->raw_size = MAX_DISC_RAW_DATA_BUF;

  if (!get_legacy_stack_sdp_api()->service.SDP_ServiceSearchAttributeRequest(
          bd_addr, bta_dm_discovery_cb.p_sdp_db, &bta_dm_sdp_callback)) {
    /*
     * If discovery is not successful with this device, then
     * proceed with the next one.
     */
    osi_free_and_reset((void**)&bta_dm_discovery_cb.p_sdp_db);
    bta_dm_discovery_cb.service_index = BTA_MAX_SERVICE_ID;
    log::info("SDP not successful");
    bta_dm_disc_sm_execute(BTA_DM_DISCOVERY_RESULT_EVT,
                           std::make_unique<tBTA_DM_MSG>(tBTA_DM_SVC_RES{
                               .bd_addr = bta_dm_discovery_cb.peer_bdaddr,
                               .services = bta_dm_discovery_cb.services_found,
                               .result = BTA_SUCCESS}));
    return;
  }

  if (uuid == Uuid::From16Bit(UUID_PROTOCOL_L2CAP)) {
    if (!is_sdp_pbap_pce_disabled(bd_addr)) {
      log::debug("SDP search for PBAP Client");
      BTA_SdpSearch(bd_addr, Uuid::From16Bit(UUID_SERVCLASS_PBAP_PCE));
    }
  }
  bta_dm_discovery_cb.service_index++;
}

namespace bluetooth {
namespace legacy {
namespace testing {

void bta_dm_sdp_result(tSDP_STATUS sdp_status) {
  ::bta_dm_sdp_result(sdp_status);
}

}  // namespace testing
}  // namespace legacy
}  // namespace bluetooth