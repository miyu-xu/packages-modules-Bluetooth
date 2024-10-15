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

#pragma once
#define LOG_TAG "bt_journal"

#include <base/strings/stringprintf.h>
#include <bluetooth/log.h>

#include <cstddef>
#include <memory>

#include "ble_address_with_type.h"
#include "common/circular_buffer.h"
#include "common/strings.h"
#include "main/shim/dumpsys.h"

namespace bluetooth {
namespace common {

using TimestampedStringCircularBuffer = bluetooth::common::TimestampedStringCircularBuffer;

#define JOURNAL_EVENT_STRING_MAX_LENGTH 32

class Journal {
public:
  static constexpr char kTimeFormat[] = "%Y-%m-%d %H:%M:%S";
  explicit Journal(const char* tag, uint8_t size)
      : DUMPSYS_TAG(tag), diary(std::make_unique<TimestampedStringCircularBuffer>(size)) {
    if (diary == nullptr) log::fatal("Diary not allocated");
  }

  void record(const tAclLinkSpec& link_spec, const std::string event, const std::string details) {
    diary->Push(base::StringPrintf("%s %-32s: %s", link_spec.ToRedactedStringForLogging().c_str(),
                                   event.substr(0, JOURNAL_EVENT_STRING_MAX_LENGTH).c_str(),
                                   details.c_str()));
  }

  void record(const tAclLinkSpec& link_spec, const char* event, const char* details) {
    record(link_spec, std::string(event), std::string(details));
  }

  void dump(int fd) {
    auto entries = diary->Pull();
    for (const auto& it : entries) {
      time_t then = it.timestamp / 1000;
      struct tm tm{};
      localtime_r(&then, &tm);
      auto s2 = common::StringFormatTime(kTimeFormat, tm);
      LOG_DUMPSYS(fd, " %s.%03u %s", s2.c_str(), static_cast<unsigned int>(it.timestamp % 1000),
                  it.entry.c_str());
    }
  }

private:
  const char* DUMPSYS_TAG;
  std::unique_ptr<TimestampedStringCircularBuffer> diary;
};

}  // namespace common
}  // namespace bluetooth
