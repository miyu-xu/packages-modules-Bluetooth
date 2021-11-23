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

#include <map>
#include <string>

extern std::map<std::string, int> mock_function_count_map;

#include <base/bind.h>
#include <base/location.h>
#include <base/logging.h>
#include <base/memory/weak_ptr.h>
#include <base/strings/string_number_conversions.h>
#include <base/time/time.h>
#include <string.h>
#include <queue>
#include <vector>
#include "bind_helpers.h"
#include "ble_scanner.h"
#include "bt_target.h"
#include "device/include/controller.h"
#include "osi/include/alarm.h"
#include "stack/btm/ble_scanner_hci_interface.h"
#include "stack/btm/btm_ble_int.h"
#include "stack/btm/btm_int_types.h"

#ifndef UNUSED_ATTR
#define UNUSED_ATTR
#endif

void BleScanningManager::CleanUp() { mock_function_count_map[__func__]++; }
void btm_ble_scanner_init() { mock_function_count_map[__func__]++; }
base::WeakPtr<BleScanningManager> BleScanningManager::Get() {
  mock_function_count_map[__func__]++;
  return nullptr;
}
bool BleScanningManager::IsInitialized() {
  mock_function_count_map[__func__]++;
  return false;
}
void BleScanningManager::Initialize(BleScannerHciInterface* interface) {
  mock_function_count_map[__func__]++;
}
void btm_ble_scanner_cleanup(void) { mock_function_count_map[__func__]++; }