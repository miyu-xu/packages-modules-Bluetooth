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

#include <atomic>
#include <cstddef>
#include <ctime>
#include <map>
#include <mutex>
#include <string>
#include <unordered_map>
#include <vector>

#include "common/circular_buffer.h"
#include "common/strings.h"

using bluetooth::common::StringFormatTime;
using bluetooth::common::TimestampedEntry;

namespace {
constexpr size_t kApiBufferSize = 100UL;
const std::string kTimeFormat("%Y-%m-%d %H:%M:%S");

constexpr uint32_t kArrowLeft = 0x2190;
constexpr uint32_t kArrowUp = 0x2191;
constexpr uint32_t kArrowRight = 0x2192;
constexpr uint32_t kArrowDown = 0x2193;

std::atomic_bool is_enabled_;

std::unordered_map<std::string, uint64_t> api_access_count_;
std::mutex api_access_count_mutex_;

bluetooth::common::TimestampedCircularBuffer<ApiEntry> buffer_(kApiBufferSize);

void api_access_metric_enable(bool is_enabled) { is_enabled_ = is_enabled; }

uint64_t api_access_metric_inc(const std::string& name) {
  std::lock_guard<std::mutex> lock(api_access_count_mutex_);
  auto it = api_access_count_.find(name);
  if (it == api_access_count_.end()) {
    api_access_count_[name] = 1;
    it = api_access_count_.find(name);
  } else {
    it->second++;
  }
  return it->second;
}

void api_access_metric_add(uint32_t token, const std::string& name) {
  buffer_.Push(ApiEntry{token, name, api_access_metric_inc(name)});
}

std::map<std::string, size_t> get_count_ordered_by_name() {
  std::lock_guard<std::mutex> lock(api_access_count_mutex_);
  std::map<std::string, size_t> ordered_map(api_access_count_.begin(),
                                            api_access_count_.end());
  return ordered_map;
}

std::vector<std::pair<std::string, size_t>> get_ordered_api_counter_metric() {
  auto ordered_map = get_count_ordered_by_name();
  std::vector<std::pair<std::string, size_t>> vector(ordered_map.size());
  auto it = vector.begin();
  for (auto& entry : ordered_map) {
    *it++ = std::make_pair(entry.first.data(), entry.second);
  }
  return vector;
}

}  // namespace

// Enable the API access metric
void api_access_metric_enable() { api_access_metric_enable(true); }

// Disable the API access metric
void api_access_metric_disable() { api_access_metric_enable(false); }

// Reset the API access metric
void api_access_metric_reset() {
  // Prevent reset while access metrics are enabled
  if (is_enabled_) {
    return;
  }
  api_access_count_.clear();
  // Buffer queue has internal synchronization
  buffer_.Drain();
}

void api_access_metric_add(uint32_t token, const char* name) {
  if (is_enabled_) api_access_metric_add(token, std::string(name));
}

void api_access_metric_add_api(const char* name) {
  if (is_enabled_) api_access_metric_add(kArrowDown, std::string(name));
}

void api_access_metric_add_cb(const char* name) {
  if (is_enabled_) api_access_metric_add(kArrowUp, std::string(name));
}

void api_access_metric_add_api_internal(const char* name) {
  if (is_enabled_) api_access_metric_add(kArrowRight, std::string(name));
}

void api_access_metric_add_cb_internal(const char* name) {
  if (is_enabled_) api_access_metric_add(kArrowLeft, std::string(name));
}

std::vector<TimestampedEntry<ApiEntry>> api_access_metric_data() {
  return buffer_.Pull();
}

// On dumpsys thread
void api_access_metric_dump(int fd) {
  if (!is_enabled_) {
    dprintf(fd, "API access metrics is disabled\n");
    return;
  }

  dprintf(fd, "API access most recent %zu entries:\n", kApiBufferSize);
  for (auto& record : api_access_metric_data()) {
    time_t then = record.timestamp / 1000;
    struct tm tm;
    localtime_r(&then, &tm);
    auto s2 = StringFormatTime(kTimeFormat, tm);
    dprintf(fd, " %s.%03u %lc %-35s count:%lu\n", s2.c_str(),
            static_cast<unsigned int>(record.timestamp % 1000),
            record.entry.token, record.entry.name.c_str(), record.entry.count);
  }
  dprintf(fd, "\n");
  dprintf(fd, "API access count summary:\n");
  dprintf(fd, " %-35s : %s\n", "API Name", "Access Count");
  for (const auto& it : get_ordered_api_counter_metric()) {
    dprintf(fd, " %-35s : %9zu\n", it.first.data(), it.second);
  }
  dprintf(fd, "\n");
}
