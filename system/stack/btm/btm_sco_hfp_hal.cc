/*
 * Copyright 2022 The Android Open Source Project
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

#include "btm_sco_hfp_hal.h"

namespace hfp_hal_interface {

bool get_wbs_supported() {
  // implement me using ioctl
  return false;
}

int get_packet_size() {
  // implement me using ioctl
  return 0xff;
}

void notify_sco_connection_change(RawAddress device, bool is_connected) {
}

}


// bool hfp_hal_interface::get_wbs_supported() {
//   return DISABLE_WBS == false;
// }

// int hfp_hal_interface::get_packet_size() {
//   // for hardware encoding, let's assume we can use the maximum SCO packet size (0xff)
//   return 0xff;
// }

// // Notify the lower layer about SCO connection change
// void hfp_hal_interface::notify_sco_connection_change(RawAddress, bool) {
//   // for hardware encoding, we don't need to notify the lower layer; it should be notified in hardware path
// }
