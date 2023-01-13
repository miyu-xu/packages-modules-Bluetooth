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

#include <cstdint>
#include <functional>

#define LOG_TAG "TEST"

#include "osi/include/log.h"

namespace {
constexpr size_t kTestBufferLogSize = 512;
}  // namespace

std::function<void(uint32_t, const char*)> test_common_log_msg =
    [](uint32_t trace_set_mask, const char* buffer) {};

extern "C" void LogMsg(uint32_t trace_set_mask, const char* fmt_str, ...) {
  char buffer[kTestBufferLogSize];

  va_list ap;
  va_start(ap, fmt_str);
  vsnprintf(buffer, kTestBufferLogSize, fmt_str, ap);
  va_end(ap);

  test_common_log_msg(trace_set_mask, buffer);
}
