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

#ifndef BTM_ISO_ADP_INTERFACE_H
#define BTM_ISO_ADP_INTERFACE_H

#include "btm_iso_api.h"

typedef struct {
  /** set to sizeof(iso_adp_interface_t) */
  size_t size;
  void (*create_cig)(uint8_t cig_id,
                     struct bluetooth::hci::iso_manager::cig_create_params cig_params);
  void (*reconfigure_cig)(uint8_t cig_id,
                          struct bluetooth::hci::iso_manager::cig_create_params cig_params);
  void (*remove_cig)(uint8_t cig_id);
  void (*establish_cis)(
      struct bluetooth::hci::iso_manager::cis_establish_params conn_params);
  void (*disconnect_cis)(uint16_t iso_handle, uint8_t reason);
  void (*setup_iso_data_path)(uint16_t iso_handle,
                    struct bluetooth::hci::iso_manager::iso_data_path_params path_params);
  void (*remove_iso_data_path)(uint16_t iso_handle, uint8_t data_path_dir);
  void (*send_iso_data)(uint16_t iso_handle, const uint8_t* data, uint16_t data_len);
  void (*create_big)(uint8_t big_id, bluetooth::hci::iso_manager::big_create_params big_params);
  void (*terminate_big)(uint8_t big_id, uint8_t reason);
  void (*start)(void);
  void (*stop)(void);
} iso_adp_interface_t;

#endif  // BTM_ISO_ADP_INTERFACE_H
