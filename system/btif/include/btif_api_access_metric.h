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

#pragma once

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

#include "common/circular_buffer.h"

struct ApiEntry {
  uint32_t token;
  std::string name;
  uint64_t count;
};

// Enable or disable the collection of api counter metrics.
void api_access_metric_disable();
void api_access_metric_enable();

// Metric must be |disabled| in order to reset
void api_access_metric_reset();

// Add API main surface entry or callback with given |name|.
void api_access_metric_add_api(const char* name);
void api_access_metric_add_cb(const char* name);

// Add internal entry or callback with given |name| and |token|.
void api_access_metric_add(uint32_t token, const char* name);

// Return a copy |vector| of timestamped API calls
std::vector<bluetooth::common::TimestampedEntry<ApiEntry>>
api_access_metric_data();

// Dumpsys the API timestamp access and name to filedescriptor.
void api_access_metric_dump(int fd);

// Return the current API entry buffer size.
size_t api_access_metric_buffer_size();
