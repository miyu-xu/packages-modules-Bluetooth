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

#include <cstdint>

#include "os/system_properties.h"

using namespace bluetooth;

uint16_t L2CA_LeCreditDefault() {
  static const uint16_t sL2CAP_LE_CREDIT_DEFAULT =
      bluetooth::os::GetSystemPropertyUint32Base(
          "bluetooth.l2cap.le.credit_default.value", 0xffff);
  return sL2CAP_LE_CREDIT_DEFAULT;
}

uint16_t L2CA_LeCreditThreshold() {
  static const uint16_t sL2CAP_LE_CREDIT_THRESHOLD =
      bluetooth::os::GetSystemPropertyUint32Base(
          "bluetooth.l2cap.le.credit_threshold.value", 0x0040);
  return sL2CAP_LE_CREDIT_THRESHOLD;
}

static bool check_l2cap_credit() {
  log::assert_that(L2CA_LeCreditThreshold() < L2CA_LeCreditDefault(),
                   "Threshold must be smaller than default credits");
  return true;
}

// Replace static assert with startup assert depending of the config
static const bool enforce_assert = check_l2cap_credit();
