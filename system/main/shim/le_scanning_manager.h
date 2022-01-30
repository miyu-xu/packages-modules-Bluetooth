/*
 * Copyright 2020 The Android Open Source Project
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

/**
 * Gd shim layer to legacy le scanner
 */
#pragma once

#include "btif/include/btif_dm.h"
#include "gd/hci/le_scanning_callback.h"
#include "gd/storage/device.h"
#include "gd/storage/le_device.h"
// #include "gd/storage/storage_module.h"
#include "include/hardware/ble_scanner.h"
#include "main/shim/helpers.h"
#include "main/shim/shim.h"
#include "stack/include/advertise_data_parser.h"
#include "stack/include/btm_ble_api_types.h"
#include "types/bluetooth/uuid.h"
#include "types/raw_address.h"

namespace bluetooth {
namespace shim {

extern ::ScanningCallbacks* default_scanning_callback;

BleScannerInterface* get_ble_scanner_instance();
void init_scanning_manager();
void set_empty_filter(bool enable);

class BleScannerInterfaceImpl : public ::BleScannerInterface,
                                public bluetooth::hci::ScanningCallback {
 public:
  ~BleScannerInterfaceImpl() override{};

  void Init();

  // ::BleScannerInterface
  void RegisterScanner(const bluetooth::Uuid& uuid, RegisterCallback) override;
  void Unregister(int scanner_id) override;
  void Scan(bool start) override;
  void ScanFilterParamSetup(
      uint8_t client_if, uint8_t action, uint8_t filter_index,
      std::unique_ptr<btgatt_filt_param_setup_t> filt_param,
      FilterParamSetupCallback cb) override;
  void ScanFilterAdd(int filter_index, std::vector<ApcfCommand> filters,
                     FilterConfigCallback cb) override;
  void ScanFilterClear(int filter_index, FilterConfigCallback cb) override;
  void ScanFilterEnable(bool enable, EnableCallback cb) override;
  void SetScanParameters(int scanner_id, int scan_interval, int scan_window,
                         Callback cb) override;
  void BatchscanConfigStorage(int client_if, int batch_scan_full_max,
                              int batch_scan_trunc_max,
                              int batch_scan_notify_threshold,
                              Callback cb) override;
  void BatchscanEnable(int scan_mode, int scan_interval, int scan_window,
                       int addr_type, int discard_rule, Callback cb) override;
  void BatchscanDisable(Callback cb) override;
  void BatchscanReadReports(int client_if, int scan_mode) override;
  void StartSync(uint8_t sid, RawAddress address, uint16_t skip,
                 uint16_t timeout, StartSyncCb start_cb, SyncReportCb report_cb,
                 SyncLostCb lost_cb) override;
  void StopSync(uint16_t handle) override;
  void CancelCreateSync(uint8_t sid, RawAddress address) override;
  void TransferSync(RawAddress address, uint16_t service_data,
                    uint16_t sync_handle, SyncTransferCb cb) override;
  void TransferSetInfo(RawAddress address, uint16_t service_data,
                       uint8_t adv_handle, SyncTransferCb cb) override;
  void SyncTxParameters(RawAddress addr, uint8_t mode, uint16_t skip,
                        uint16_t timeout, StartSyncCb start_cb) override;
  void RegisterCallbacks(ScanningCallbacks* callbacks);
  void OnScannerRegistered(const bluetooth::hci::Uuid app_uuid,
                           bluetooth::hci::ScannerId scanner_id,
                           ScanningStatus status) override;
  void OnSetScannerParameterComplete(bluetooth::hci::ScannerId scanner_id,
                                     ScanningStatus status) override;
  void OnScanResult(uint16_t event_type, uint8_t address_type,
                    bluetooth::hci::Address address, uint8_t primary_phy,
                    uint8_t secondary_phy, uint8_t advertising_sid,
                    int8_t tx_power, int8_t rssi,
                    uint16_t periodic_advertising_interval,
                    std::vector<uint8_t> advertising_data) override;
  void OnTrackAdvFoundLost(bluetooth::hci::AdvertisingFilterOnFoundOnLostInfo
                               on_found_on_lost_info) override;
  void OnBatchScanReports(int client_if, int status, int report_format,
                          int num_records, std::vector<uint8_t> data) override;
  void OnBatchScanThresholdCrossed(int client_if) override;
  void OnTimeout() override;
  void OnFilterEnable(bluetooth::hci::Enable enable, uint8_t status) override;
  void OnFilterParamSetup(uint8_t available_spaces,
                          bluetooth::hci::ApcfAction action,
                          uint8_t status) override;
  void OnFilterConfigCallback(bluetooth::hci::ApcfFilterType filter_type,
                              uint8_t available_spaces,
                              bluetooth::hci::ApcfAction action,
                              uint8_t status) override;
  ::ScanningCallbacks* scanning_callbacks_ = default_scanning_callback;

 private:
  bool parse_filter_command(
      bluetooth::hci::AdvertisingPacketContentFilterCommand&
          advertising_packet_content_filter_command,
      ApcfCommand apcf_command);
  void handle_remote_properties(RawAddress bd_addr, tBLE_ADDR_TYPE addr_type,
                                std::vector<uint8_t> advertising_data);

  void add_address_cache(const RawAddress& p_bda) {
    // Remove the oldest entries
    while (remote_bdaddr_cache_.size() >= remote_bdaddr_cache_max_size_) {
      const RawAddress& raw_address = remote_bdaddr_cache_ordered_.front();
      remote_bdaddr_cache_.erase(raw_address);
      remote_bdaddr_cache_ordered_.pop();
    }
    remote_bdaddr_cache_.insert(p_bda);
    remote_bdaddr_cache_ordered_.push(p_bda);
  }

  bool find_address_cache(const RawAddress& p_bda) {
    return (remote_bdaddr_cache_.find(p_bda) != remote_bdaddr_cache_.end());
  }

  void init_address_cache(void) {
    remote_bdaddr_cache_.clear();
    remote_bdaddr_cache_ordered_ = {};
  }

  // all access to this variable should be done on the jni thread
  std::set<RawAddress> remote_bdaddr_cache_;
  std::queue<RawAddress> remote_bdaddr_cache_ordered_;
  const size_t remote_bdaddr_cache_max_size_ = 1024;
};

}  // namespace shim
}  // namespace bluetooth
