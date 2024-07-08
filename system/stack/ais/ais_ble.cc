/******************************************************************************
 *
 *  Copyright 2024 The Android Open Source Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

#include <bluetooth/log.h>
#include <com_android_bluetooth_flags.h>
#include <string.h>

#include <array>

#include "ais_api.h"
#include "gatt_api.h"
#include "os/system_properties.h"
#include "stack/include/bt_types.h"
#include "types/bluetooth/uuid.h"

using bluetooth::Uuid;
using namespace bluetooth;

static const std::string kPropertyAndroidAPILevel = "ro.build.version.sdk";
static const uint32_t kPropertyAndroidAPILevelDefault = 0;

constexpr int AIS_MAX_CHAR_NUM = 1;

typedef struct {
  uint16_t handle;
} tAIS_ATTR;

/* LE AIS attribute database */
std::array<tAIS_ATTR, AIS_MAX_CHAR_NUM> gatt_attr;
tGATT_IF gatt_if;

void ais_request_cback(uint16_t, uint32_t, tGATTS_REQ_TYPE, tGATTS_DATA*);

static tGATT_CBACK ais_cback = {
        .p_conn_cb = nullptr,
        .p_cmpl_cb = nullptr,
        .p_disc_res_cb = nullptr,
        .p_disc_cmpl_cb = nullptr,
        .p_req_cb = ais_request_cback,
        .p_enc_cmpl_cb = nullptr,
        .p_congestion_cb = nullptr,
        .p_phy_update_cb = nullptr,
        .p_conn_update_cb = nullptr,
        .p_subrate_chg_cb = nullptr,
};

/** GAP Attributes Database Request callback */
tGATT_STATUS read_attr_value(uint16_t handle, tGATT_VALUE* p_value, bool is_long) {
  uint8_t* p = p_value->value;

  if (com::android::bluetooth::flags::android_os_identifier() && handle == gatt_attr[0].handle) {
    if (is_long) {
      return GATT_NOT_LONG;
    }

    UINT32_TO_STREAM(p, os::GetSystemPropertyUint32(kPropertyAndroidAPILevel,
                                                    kPropertyAndroidAPILevelDefault));
    p_value->len = 4;
    return GATT_SUCCESS;
  }

  return GATT_NOT_FOUND;
}

/** AIS Attributes Database Read/Read Blob Request process */
tGATT_STATUS proc_read(tGATTS_REQ_TYPE, tGATT_READ_REQ* p_data, tGATTS_RSP* p_rsp) {
  if (p_data->is_long) {
    p_rsp->attr_value.offset = p_data->offset;
  }

  p_rsp->attr_value.handle = p_data->handle;

  return read_attr_value(p_data->handle, &p_rsp->attr_value, p_data->is_long);
}

/** AIS ATT server attribute access request callback */
void ais_request_cback(uint16_t conn_id, uint32_t trans_id, tGATTS_REQ_TYPE type,
                       tGATTS_DATA* p_data) {
  tGATT_STATUS status = GATT_INVALID_PDU;
  bool ignore = false;

  tGATTS_RSP rsp_msg;
  memset(&rsp_msg, 0, sizeof(tGATTS_RSP));

  switch (type) {
    case GATTS_REQ_TYPE_READ_CHARACTERISTIC:
    case GATTS_REQ_TYPE_READ_DESCRIPTOR:
      status = proc_read(type, &p_data->read_req, &rsp_msg);
      break;

    default:
      log::verbose("Unknown/unexpected LE AIS ATT request: 0x{:02x}", type);
      break;
  }

  if (!ignore) {
    if (GATTS_SendRsp(conn_id, trans_id, status, &rsp_msg) != GATT_SUCCESS) {
      log::warn("Unable to send GATT ervier response conn_id:{}", conn_id);
    }
  }
}

/*******************************************************************************
 *
 * Function         ais_attr_db_init
 *
 * Description      AIS ATT database initialization.
 *
 * Returns          void.
 *
 ******************************************************************************/
void ais_attr_db_init(void) {
  // Add Android OS identifier if API level is defined.
  if (com::android::bluetooth::flags::android_os_identifier() &&
      os::GetSystemPropertyUint32(kPropertyAndroidAPILevel, kPropertyAndroidAPILevelDefault)) {
    std::array<uint8_t, Uuid::kNumBytes128> tmp;
    tmp.fill(0xc5);  // any number is fine here
    Uuid app_uuid = Uuid::From128BitBE(tmp);

    gatt_if = GATT_Register(app_uuid, "Ais", &ais_cback, false);

    GATT_StartIf(gatt_if);

    const Uuid ANDROID_INFORMATION_SERVICE_UUID =
            Uuid::FromString(ANDROID_INFORMATION_SERVICE_UUID_STRING);
    const Uuid GATT_UUID_AIS_API_LEVEL = Uuid::FromString(GATT_UUID_AIS_API_LEVEL_STRING);

    btgatt_db_element_t android_information_service[] = {
            {
                    .uuid = ANDROID_INFORMATION_SERVICE_UUID,
                    .type = BTGATT_DB_PRIMARY_SERVICE,
            },
            {
                    .uuid = GATT_UUID_AIS_API_LEVEL,
                    .type = BTGATT_DB_CHARACTERISTIC,
                    .properties = GATT_CHAR_PROP_BIT_READ,
                    .permissions = GATT_PERM_READ_IF_ENCRYPTED_OR_DISCOVERABLE,
            }};
    if (GATTS_AddService(gatt_if, android_information_service,
                         sizeof(android_information_service) / sizeof(btgatt_db_element_t)) !=
        GATT_SERVICE_STARTED) {
      log::warn("Unable to add Android Information Server gatt_if:{}", gatt_if);
    }

    gatt_attr[0].handle = android_information_service[1].attribute_handle;
  }
}

/*
 * This routine should not be called except once per stack invocation.
 */
void AIS_Init(void) { ais_attr_db_init(); }
