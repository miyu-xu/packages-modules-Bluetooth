/******************************************************************************
 *
 *  Copyright 2016 The Android Open Source Project
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

#pragma once

#ifndef BLE_ADVERTISER_ADP_INTERFACE_H
#define BLE_ADVERTISER_ADP_INTERFACE_H

#include "ble_advertiser.h"

typedef struct {
  /** set to sizeof(ble_advertiser_adp_interface_t) */
  size_t size;
  bool (*is_initialized)(void);
  void (*register_advertiser)(base::Callback<void(uint8_t /* advertiser_id */, uint8_t /* status */)> cb);
  void (*enable)(uint8_t inst_id, bool enable, MultiAdvCb cb,
            uint16_t duration, uint8_t maxExtAdvEvents,
            MultiAdvCb timeout_cb);
  void (*set_data)(uint8_t inst_id, bool is_scan_rsp,
                       std::vector<uint8_t> data, MultiAdvCb cb);
  void (*set_periodic_advertising_data)(uint8_t inst_id,
                                std::vector<uint8_t> data,
                                MultiAdvCb cb);
  void (*unregister)(uint8_t inst_id);
  void (*get_own_address)(uint8_t inst_id,
    base::Callback<void(uint8_t /* address_type*/, RawAddress /*address*/)> cb);
  void (*start_advertising)(uint8_t advertiser_id, MultiAdvCb cb,
                        tBTM_BLE_ADV_PARAMS* params,
                        std::vector<uint8_t> advertise_data,
                        std::vector<uint8_t> scan_response_data,
                        int duration, MultiAdvCb timeout_cb);
} ble_advertiser_adp_interface_t;

#endif  // BLE_ADVERTISER_ADP_INTERFACE_H
