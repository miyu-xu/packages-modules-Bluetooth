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

#include <base/callback.h>
#include "ble_advertiser_adp_interface.h"

using GetAddressCallback =
    base::Callback<void(uint8_t /* address_type*/, RawAddress /*address*/)>;
using IdTxPowerStatusCb = base::Callback<void(
    uint8_t /* inst_id */, int8_t /* tx_power */, uint8_t /* status */)>;
using RegisterCb =
    base::Callback<void(uint8_t /* inst_id */, uint8_t /* status */)>;

static bool is_initialized() {
  return BleAdvertisingManager::IsInitialized();
}

static void register_advertiser(base::Callback<void(uint8_t /* advertiser_id */, uint8_t /* status */)> cb) {
  if (BleAdvertisingManager::IsInitialized()) {
    BleAdvertisingManager::Get()->RegisterAdvertiser(std::move(cb));
  }
}

static void enable(uint8_t inst_id, bool enable, MultiAdvCb cb,
          uint16_t duration, uint8_t maxExtAdvEvents,
          MultiAdvCb timeout_cb) {
  if (BleAdvertisingManager::IsInitialized()) {
    BleAdvertisingManager::Get()->Enable(
      inst_id, enable, std::move(cb), duration, maxExtAdvEvents, std::move(timeout_cb));
  }
}

static void set_data(uint8_t inst_id, bool is_scan_rsp,
                       std::vector<uint8_t> data, MultiAdvCb cb) {
  if (BleAdvertisingManager::IsInitialized()) {
    BleAdvertisingManager::Get()->SetData(inst_id, is_scan_rsp, data, std::move(cb));
  }
}

static void set_periodic_advertising_data(uint8_t inst_id,
                              std::vector<uint8_t> data,
                              MultiAdvCb cb) {
  if (BleAdvertisingManager::IsInitialized()) {
    BleAdvertisingManager::Get()->SetPeriodicAdvertisingData(inst_id, data, std::move(cb));
  }
}

static void unregister(uint8_t inst_id) {
  if (BleAdvertisingManager::IsInitialized()) {
    BleAdvertisingManager::Get()->Unregister(inst_id);
  }
}

static void get_own_address(uint8_t inst_id, GetAddressCallback cb) {
  if (BleAdvertisingManager::IsInitialized()) {
    BleAdvertisingManager::Get()->GetOwnAddress(inst_id, std::move(cb));
  }
}

static void start_advertising(uint8_t advertiser_id, MultiAdvCb cb,
                        tBTM_BLE_ADV_PARAMS* params,
                        std::vector<uint8_t> advertise_data,
                        std::vector<uint8_t> scan_response_data,
                        int duration, MultiAdvCb timeout_cb) {
  if (BleAdvertisingManager::IsInitialized()) {
    BleAdvertisingManager::Get()->StartAdvertising(advertiser_id, std::move(cb),
                        params, advertise_data, scan_response_data, duration, std::move(timeout_cb));
  }
}

extern "C" EXPORT_SYMBOL ble_advertiser_adp_interface_t bleAdvertisterAdpInterface;

ble_advertiser_adp_interface_t bleAdvertisterAdpInterface = {
  sizeof(bleAdvertisterAdpInterface),
  is_initialized,
  register_advertiser,
  enable,
  set_data,
  set_periodic_advertising_data,
  unregister,
  get_own_address,
  start_advertising,
};
