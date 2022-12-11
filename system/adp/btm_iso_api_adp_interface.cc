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

#include "btm_iso_api_adp_interface.h"

using bluetooth::hci::IsoManager;

void create_cig(uint8_t cig_id,
                     struct bluetooth::hci::iso_manager::cig_create_params cig_params) {
  IsoManager::GetInstance()->CreateCig(cig_id, cig_params);
}

void reconfigure_cig(uint8_t cig_id,
                      struct bluetooth::hci::iso_manager::cig_create_params cig_params) {
  IsoManager::GetInstance()->ReconfigureCig(cig_id, cig_params);
}

void remove_cig(uint8_t cig_id) {
  IsoManager::GetInstance()->RemoveCig(cig_id);
}

void establish_cis(
  struct bluetooth::hci::iso_manager::cis_establish_params conn_params) {
  IsoManager::GetInstance()->EstablishCis(conn_params);
}

void disconnect_cis(uint16_t iso_handle, uint8_t reason) {
  IsoManager::GetInstance()->DisconnectCis(iso_handle, reason);
}

static void setup_iso_data_path(uint16_t iso_handle,
                    struct bluetooth::hci::iso_manager::iso_data_path_params path_params) {
  IsoManager::GetInstance()->SetupIsoDataPath(iso_handle, path_params);
}

static void remove_iso_data_path(uint16_t iso_handle, uint8_t data_path_dir) {
  IsoManager::GetInstance()->RemoveIsoDataPath(iso_handle, data_path_dir);
}

static void send_iso_data(uint16_t iso_handle, const uint8_t* data, uint16_t data_len) {
  IsoManager::GetInstance()->SendIsoData(iso_handle, data, data_len);
}

static void create_big(uint8_t big_id, bluetooth::hci::iso_manager::big_create_params big_params) {
  IsoManager::GetInstance()->CreateBig(big_id, big_params);
}

static void terminate_big(uint8_t big_id, uint8_t reason) {
  IsoManager::GetInstance()->TerminateBig(big_id, reason);
}

static void start() {
  IsoManager::GetInstance()->Start();
}

static void stop() {
  IsoManager::GetInstance()->Stop();
}

extern "C" EXPORT_SYMBOL iso_adp_interface_t isoAdpInterface;

iso_adp_interface_t isoAdpInterface = {
  sizeof(isoAdpInterface),
  create_cig,
  reconfigure_cig,
  remove_cig,
  establish_cis,
  disconnect_cis,
  setup_iso_data_path,
  remove_iso_data_path,
  send_iso_data,
  create_big,
  terminate_big,
  start,
  stop,
};
