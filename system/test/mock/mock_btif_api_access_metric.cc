/*
 * Copyright 2023 The Android Open Source Project
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

#include "btif/include/btif_api_access_metric.h"
#include "test/common/mock_functions.h"

void api_access_metric_disable() { inc_func_call_count(__func__); }
void api_access_metric_enable() { inc_func_call_count(__func__); }

void api_access_metric_reset() { inc_func_call_count(__func__); }

void api_access_metric_add_api(const char* name) {
  inc_func_call_count(__func__);
}

void api_access_metric_add_cb(const char* name) {
  inc_func_call_count(__func__);
}

// Add internal entry or callback with given name.
void api_access_metric_add(uint32_t token, const char* name) {
  inc_func_call_count(__func__);
}

void api_access_metric_dump(int fd) { inc_func_call_count(__func__); }

size_t api_access_metric_buffer_size() {
  inc_func_call_count(__func__);
  return 0UL;
}
