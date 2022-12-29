/******************************************************************************
 *
 *  Copyright 2014 Broadcom Corporation
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
#include <base/bind.h>
#include <stddef.h>
#include <stdio.h>
#include <string.h>
#include <vector>
#include "bt_target.h"

#include "btm_ble_api.h"
#include "btu.h"
#include "device/include/controller.h"
#include "stack/btm/btm_int_types.h"
#include "utils/include/bt_utils.h"

extern tBTM_CB btm_cb;

using base::Bind;
using base::Callback;
using hci_cmd_cb = base::Callback<void(uint8_t* /* return_parameters */,
                                       uint16_t /* return_parameters_length*/)>;

tBTM_BLE_BATCH_SCAN_CB ble_batchscan_cb;
tBTM_BLE_ADV_TRACK_CB ble_advtrack_cb;

/* length of each batch scan command */
#define BTM_BLE_BATCH_SCAN_STORAGE_CFG_LEN 4
#define BTM_BLE_BATCH_SCAN_PARAM_CONFIG_LEN 12
#define BTM_BLE_BATCH_SCAN_ENB_DISB_LEN 2
#define BTM_BLE_BATCH_SCAN_READ_RESULTS_LEN 2

namespace {

/* VSE callback for batch scan, filter, and tracking events */
void btm_ble_batchscan_filter_track_adv_vse_cback(uint8_t len,
                                                  const uint8_t* p) {
  tBTM_BLE_TRACK_ADV_DATA adv_data;

  uint8_t sub_event = 0;
  tBTM_BLE_VSC_CB cmn_ble_vsc_cb;
  if (len == 0) return;
  STREAM_TO_UINT8(sub_event, p);

  BTM_TRACE_EVENT(
      "btm_ble_batchscan_filter_track_adv_vse_cback called with event:%x",
      sub_event);
  if (HCI_VSE_SUBCODE_BLE_THRESHOLD_SUB_EVT == sub_event &&
      NULL != ble_batchscan_cb.p_thres_cback) {
    ble_batchscan_cb.p_thres_cback(ble_batchscan_cb.ref_value);
    return;
  }

  if (HCI_VSE_SUBCODE_BLE_TRACKING_SUB_EVT == sub_event &&
      NULL != ble_advtrack_cb.p_track_cback) {
    if (len < 10) return;

    memset(&adv_data, 0, sizeof(tBTM_BLE_TRACK_ADV_DATA));
    BTM_BleGetVendorCapabilities(&cmn_ble_vsc_cb);
    adv_data.client_if = (uint8_t)ble_advtrack_cb.ref_value;
    if (cmn_ble_vsc_cb.version_supported > BTM_VSC_CHIP_CAPABILITY_L_VERSION) {
      STREAM_TO_UINT8(adv_data.filt_index, p);
      STREAM_TO_UINT8(adv_data.advertiser_state, p);
      STREAM_TO_UINT8(adv_data.advertiser_info_present, p);
      STREAM_TO_BDADDR(adv_data.bd_addr, p);
      STREAM_TO_UINT8(adv_data.addr_type, p);

      /* Extract the adv info details */
      if (ADV_INFO_PRESENT == adv_data.advertiser_info_present) {
        if (len < 15) return;
        STREAM_TO_UINT8(adv_data.tx_power, p);
        STREAM_TO_UINT8(adv_data.rssi_value, p);
        STREAM_TO_UINT16(adv_data.time_stamp, p);

        STREAM_TO_UINT8(adv_data.adv_pkt_len, p);
        if (adv_data.adv_pkt_len > 0) {
          adv_data.p_adv_pkt_data =
              static_cast<uint8_t*>(osi_malloc(adv_data.adv_pkt_len));
          memcpy(adv_data.p_adv_pkt_data, p, adv_data.adv_pkt_len);
          p += adv_data.adv_pkt_len;
        }

        STREAM_TO_UINT8(adv_data.scan_rsp_len, p);
        if (adv_data.scan_rsp_len > 0) {
          adv_data.p_scan_rsp_data =
              static_cast<uint8_t*>(osi_malloc(adv_data.scan_rsp_len));
          memcpy(adv_data.p_scan_rsp_data, p, adv_data.scan_rsp_len);
        }
      }
    } else {
      /* Based on L-release version */
      STREAM_TO_UINT8(adv_data.filt_index, p);
      STREAM_TO_UINT8(adv_data.addr_type, p);
      STREAM_TO_BDADDR(adv_data.bd_addr, p);
      STREAM_TO_UINT8(adv_data.advertiser_state, p);
    }

    BTM_TRACE_EVENT("track_adv_vse_cback called: %d, %d, %d",
                    adv_data.filt_index, adv_data.addr_type,
                    adv_data.advertiser_state);

    // Make sure the device is known
    BTM_SecAddBleDevice(adv_data.bd_addr, BT_DEVICE_TYPE_BLE,
                        to_ble_addr_type(adv_data.addr_type));

    ble_advtrack_cb.p_track_cback(&adv_data);
    return;
  }
}
}  // namespace

/**
 * This function initialize the batch scan control block.
 **/
void btm_ble_batchscan_init(void) {
  BTM_TRACE_EVENT(" btm_ble_batchscan_init");
  memset(&ble_batchscan_cb, 0, sizeof(tBTM_BLE_BATCH_SCAN_CB));
  memset(&ble_advtrack_cb, 0, sizeof(tBTM_BLE_ADV_TRACK_CB));
  BTM_RegisterForVSEvents(btm_ble_batchscan_filter_track_adv_vse_cback, true);
}
