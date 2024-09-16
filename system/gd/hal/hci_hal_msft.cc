/*
 * Copyright 2024 The Android Open Source Project
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

#include <bluetooth/log.h>
#include <com_android_bluetooth_flags.h>

#include "hal/hci_hal.h"
#include "hal/mgmt.h"
#include "osi/include/properties.h"

static const char kPropertyMsftHciExtEnabled[] = "bluetooth.core.le.use_msft_hci_ext";

namespace bluetooth::hal {

class HciHalImpl : public HciHal {
public:
  uint16_t getMsftOpcode() override {
    return osi_property_get_bool(kPropertyMsftHciExtEnabled, false) &&
                           com::android::bluetooth::flags::le_scan_msft_support()
                   ? Mgmt().get_vs_opcode(MGMT_VS_OPCODE_MSFT)
                   : 0;
  }
};

}  // namespace bluetooth::hal
