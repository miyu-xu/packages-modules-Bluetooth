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

#include "btif/include/btif_api_counter_metric.h"

#include <base/strings/stringprintf.h>

#include <atomic>
#include <cstddef>
#include <map>
#include <mutex>
#include <string>
#include <unordered_map>
#include <vector>

namespace {
std::atomic_bool is_enabled_;
// Synchronize access to the count maps
std::mutex mutex_;
std::unordered_map<std::string, size_t> api_entry_count_;
std::unordered_map<std::string, size_t> api_callback_count_;

void api_counter_metric_inc(std::unordered_map<std::string, size_t>& map,
                            const std::string& name) {
  std::lock_guard<std::mutex> lock(mutex_);
  auto it = map.find(name);
  if (it == map.end()) {
    map[name] = 1;
  } else {
    it->second++;
  }
}

std::map<std::string, size_t> get_count_ordered_by_name(
    const std::unordered_map<std::string, size_t>& map) {
  std::lock_guard<std::mutex> lock(mutex_);
  std::map<std::string, size_t> ordered_map(map.begin(), map.end());
  return ordered_map;
}

std::vector<std::pair<std::string, size_t>> get_ordered_api_counter_metric(
    std::unordered_map<std::string, size_t>& map) {
  std::vector<std::pair<std::string, size_t>> vector(map.size());
  auto it = vector.begin();
  for (auto& entry : get_count_ordered_by_name(map)) {
    *it++ = std::make_pair(entry.first.data(), entry.second);
  }
  return vector;
}

void api_counter_metric_enable(bool is_enabled) { is_enabled_ = is_enabled; }

}  // namespace

void api_counter_metric_init() {
  std::lock_guard<std::mutex> lock(mutex_);
  api_entry_count_.clear();
  api_callback_count_.clear();
}

// Enable the API counters
void api_counter_metric_enable() { api_counter_metric_enable(true); }

// Disable the API counters
void api_counter_metric_disable() { api_counter_metric_enable(false); }

// Increment associated API entry point name
void api_counter_metric_entry_inc(const char* name) {
  if (is_enabled_) api_counter_metric_inc(api_entry_count_, std::string(name));
}

// Increment associated API callback name
void api_counter_metric_callback_inc(const char* name) {
  if (is_enabled_)
    api_counter_metric_inc(api_callback_count_, std::string(name));
}

// Return an ordered vector of the API main interface entry counters.
std::vector<std::pair<std::string, size_t>> api_counter_metric_entry_dump() {
  return get_ordered_api_counter_metric(api_entry_count_);
}

// Return an ordered vector of the API main interface callback counters.
std::vector<std::pair<std::string, size_t>> api_counter_metric_callback_dump() {
  return get_ordered_api_counter_metric(api_callback_count_);
}

// Dump the counters in alphabetical order to a filedescriptor for dumpsys
void api_counter_metric_dump(int fd) {
  if (!is_enabled_) {
    dprintf(fd, "API counter metrics is disabled\n");
    return;
  }

  dprintf(fd, "API counter metrics:\n");
  dprintf(fd, "%20s: %40s : %s\n", "API Group", "Name", "Count");
  for (const auto& it : api_counter_metric_entry_dump()) {
    dprintf(fd, "%20s: %40s : %9zu\n", "API main entry", it.first.data(),
            it.second);
  }

  for (const auto& it : api_counter_metric_callback_dump()) {
    dprintf(fd, "%20s: %40s : %9zu\n", "API main callback", it.first.data(),
            it.second);
  }
}
