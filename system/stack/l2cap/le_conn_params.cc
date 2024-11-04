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

#define LOG_TAG "le_conn_params"

#include "stack/include/le_conn_params.h"

#include <bluetooth/log.h>

#include <cstdint>

#include "os/system_properties.h"
#include "stack/include/btm_ble_api_types.h"

namespace bluetooth {

void init_conn_params_with_system_properties() {
  __aggressive_conn_threshold =
          os::GetSystemPropertyUint32(kPropertyAggressiveConnThreshold, kAggressiveConnThreshold);

  __min_conn_interval_aggressive = os::GetSystemPropertyUint32(kPropertyMinConnIntervalAggressive,
                                                               kMinConnIntervalAggressive);
  __max_conn_interval_aggressive = os::GetSystemPropertyUint32(kPropertyMaxConnIntervalAggressive,
                                                               kMaxConnIntervalAggressive);
  __min_conn_interval_relaxed =
          os::GetSystemPropertyUint32(kPropertyMinConnIntervalRelaxed, kMinConnIntervalRelaxed);
  __max_conn_interval_relaxed =
          os::GetSystemPropertyUint32(kPropertyMaxConnIntervalRelaxed, kMaxConnIntervalRelaxed);
  log::debug("Before checking validity: threshold={}, aggressive={}/{}, relaxed={}/{}",
             __aggressive_conn_threshold, __min_conn_interval_aggressive,
             __max_conn_interval_aggressive, __min_conn_interval_relaxed,
             __max_conn_interval_relaxed);

  // Check validity of each values
  if (__aggressive_conn_threshold < 0) {
    log::warn("Invalid aggressive connection threshold. Using default value.",
              __aggressive_conn_threshold);
    __aggressive_conn_threshold = kAggressiveConnThreshold;
  }

  if (__min_conn_interval_aggressive < BTM_BLE_CONN_INT_MIN ||
      __min_conn_interval_aggressive > BTM_BLE_CONN_INT_MAX ||
      __max_conn_interval_aggressive < BTM_BLE_CONN_INT_MIN ||
      __max_conn_interval_aggressive > BTM_BLE_CONN_INT_MAX ||
      __max_conn_interval_aggressive < __min_conn_interval_aggressive) {
    log::warn("Invalid aggressive connection intervals. Using default values.");
    __min_conn_interval_aggressive = kMinConnIntervalAggressive;
    __max_conn_interval_aggressive = kMaxConnIntervalAggressive;
  }

  if (__min_conn_interval_relaxed < BTM_BLE_CONN_INT_MIN ||
      __min_conn_interval_relaxed > BTM_BLE_CONN_INT_MAX ||
      __max_conn_interval_relaxed < BTM_BLE_CONN_INT_MIN ||
      __max_conn_interval_relaxed > BTM_BLE_CONN_INT_MAX ||
      __max_conn_interval_relaxed < __min_conn_interval_relaxed) {
    log::warn("Invalid relaxed connection intervals. Using default values.");
    __min_conn_interval_relaxed = kMinConnIntervalRelaxed;
    __max_conn_interval_relaxed = kMaxConnIntervalRelaxed;
  }

  if ((__min_conn_interval_aggressive > __min_conn_interval_relaxed) &&
      (__max_conn_interval_aggressive > __max_conn_interval_relaxed)) {
    log::warn(
            "Relaxed connection intervals are more aggressive than aggressive ones."
            " Setting all intervals to default values.");
    __min_conn_interval_aggressive = kMinConnIntervalAggressive;
    __max_conn_interval_aggressive = kMaxConnIntervalAggressive;
    __min_conn_interval_relaxed = kMinConnIntervalRelaxed;
    __max_conn_interval_relaxed = kMaxConnIntervalRelaxed;
  }

  log::debug("After checking validity: threshold={}, aggressive={}/{}, relaxed={}/{}",
             __aggressive_conn_threshold, __min_conn_interval_aggressive,
             __max_conn_interval_aggressive, __min_conn_interval_relaxed,
             __max_conn_interval_relaxed);

  __initialized = true;
}

uint32_t get_aggressive_conn_threshold() {
  if (!__initialized) {
    init_conn_params_with_system_properties();
  }
  return __aggressive_conn_threshold;
}

uint32_t get_min_conn_interval_aggressive() {
  if (!__initialized) {
    init_conn_params_with_system_properties();
  }
  return __min_conn_interval_aggressive;
}

uint32_t get_max_conn_interval_aggressive() {
  if (!__initialized) {
    init_conn_params_with_system_properties();
  }
  return __max_conn_interval_aggressive;
}

uint32_t get_min_conn_interval_relaxed() {
  if (!__initialized) {
    init_conn_params_with_system_properties();
  }
  return __min_conn_interval_relaxed;
}

uint32_t get_max_conn_interval_relaxed() {
  if (!__initialized) {
    init_conn_params_with_system_properties();
  }
  return __max_conn_interval_relaxed;
}
}  // namespace bluetooth
