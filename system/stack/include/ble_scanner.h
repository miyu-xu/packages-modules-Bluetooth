/******************************************************************************

Copyright (c) 2021 Qualcomm Innovation Center, Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted (subject to the limitations in the
disclaimer below) provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

    * Neither the name of Qualcomm Innovation Center, Inc. nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE
GRANTED BY THIS LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT
HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

******************************************************************************/

#ifndef BLE_SCANNER_H
#define BLE_SCANNER_H

#include <base/bind.h>
#include <base/memory/weak_ptr.h>
#include <vector>
#include "btm_ble_api.h"

using status_cb = base::Callback<void(uint8_t /* status */)>;
using handle_cb =
    base::Callback<void(uint8_t /* status */, uint16_t /* adv_handle */)>;

// methods we expose to c code:
void btm_ble_scanner_cleanup(void);
void btm_ble_scanner_init();

class BleScannerHciInterface;

class BleScanningManager {
 public:
  virtual ~BleScanningManager() = default;

  static void Initialize(BleScannerHciInterface* interface);
  static void CleanUp();
  static bool IsInitialized();
  static base::WeakPtr<BleScanningManager> Get();

  virtual void PeriodicScanStart(uint8_t options, uint8_t set_id, uint8_t adv_addr_type,
                                 const RawAddress& adv_addr, uint16_t skip_num,
                                 uint16_t sync_timeout, uint8_t sync_cte_type) = 0;
  virtual void PeriodicScanCancelStart(/*status_cb command_complete*/) = 0;
  virtual void PeriodicScanTerminate(uint16_t sync_handle/*,
                                     status_cb command_complete*/) = 0;
  virtual void PeriodicAdvSyncTransfer(const RawAddress& bd_addr, uint16_t service_data,
                                       uint16_t sync_handle,
                                       handle_cb command_complete) = 0;
  virtual void PeriodicAdvSetInfoTransfer(const RawAddress& bd_addr,
                                         uint16_t service_data, uint8_t adv_handle,
                                         handle_cb command_complete) = 0;
  virtual void SetPeriodicAdvSyncTransferParams(const RawAddress& bd_addr, uint8_t mode,
                                                uint16_t skip, uint16_t sync_timeout,
                                                uint8_t cte_type, bool set_defaults,
                                                status_cb command_complete) = 0;

  virtual void OnPeriodicScanResult(uint16_t sync_handle, uint8_t tx_power,
                                    int8_t rssi, uint8_t cte_type,
                                    uint8_t pkt_data_status,
                                    uint8_t pkt_data_len,
                                    const uint8_t* pkt_data) = 0;
  virtual void OnPeriodicScanEstablished(
           uint8_t status, uint16_t sync_handle, uint8_t set_id,
           uint8_t adv_addr_type, const RawAddress& adv_addr, uint8_t adv_phy,
           uint16_t adv_interval, uint8_t adv_clock_accuracy) = 0;
  virtual void OnPeriodicScanLost(uint16_t sync_handle) = 0;
};

#endif  // BLE_SCANNER_H
