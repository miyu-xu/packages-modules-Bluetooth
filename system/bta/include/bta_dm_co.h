/******************************************************************************
 *
 *  Copyright 2006-2012 Broadcom Corporation
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

/******************************************************************************
 *
 *  This is the interface file for device mananger callout functions.
 *
 ******************************************************************************/
#ifndef BTA_DM_CO_H
#define BTA_DM_CO_H

#include "bta/include/bta_api.h"
#include "stack/include/bt_hdr.h"
#include "stack/include/bt_types.h"
#include "stack/include/btm_api_types.h"
#include "types/raw_address.h"

#ifndef BTA_SCO_OUT_PKT_SIZE
#define BTA_SCO_OUT_PKT_SIZE BTM_SCO_DATA_SIZE_MAX
#endif

/*****************************************************************************
 *  Function Declarations
 ****************************************************************************/

/*******************************************************************************
 *
 * Function         bta_dm_co_ble_io_req
 *
 * Description      This callout function is executed by DM to get BLE IO
 *                  capabilities before SMP pairing gets going.
 *
 * Parameters       bd_addr  - The peer device
 *                  *p_io_cap - The local Input/Output capabilities
 *                  *p_oob_data - true, if OOB data is available for the peer
 *                                device.
 *                  *p_auth_req -  Auth request setting (Bonding and MITM
 *                                                       required or not)
 *                  *p_max_key_size - max key size local device supported.
 *                  *p_init_key - initiator keys.
 *                  *p_resp_key - responder keys.
 *
 * Returns          void.
 *
 ******************************************************************************/
void bta_dm_co_ble_io_req(const RawAddress& bd_addr, tBTM_IO_CAP* p_io_cap,
                          tBTM_OOB_DATA* p_oob_data,
                          tBTM_LE_AUTH_REQ* p_auth_req, uint8_t* p_max_key_size,
                          tBTM_LE_KEY_TYPE* p_init_key,
                          tBTM_LE_KEY_TYPE* p_resp_key);

/*******************************************************************************
 *
 * Function         bta_dm_co_ble_local_key_reload
 *
 * Description      This callout function is to load the local BLE keys if
 *                  available on the device.
 *
 * Parameters       none
 *
 * Returns          void.
 *
 ******************************************************************************/
void bta_dm_co_ble_load_local_keys(tBTA_DM_BLE_LOCAL_KEY_MASK* p_key_mask,
                                   Octet16* p_er,
                                   tBTA_BLE_LOCAL_ID_KEYS* p_id_keys);

#endif /* BTA_DM_CO_H */
