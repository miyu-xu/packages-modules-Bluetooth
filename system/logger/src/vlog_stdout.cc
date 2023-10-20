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

#include <fmt/chrono.h>

#include <cstdio>

#include "bluetooth/logger.h"
#include "truncating_buffer.h"

namespace logger {

static constexpr size_t kBufferSize = 1024;
static constexpr char const* kLevelNames[] = {"V", "D", "I", "W",
                                              "E", "F", "F"};

void vlog(Level level, char const* tag, char const* file_name, int line,
          char const* function_name, std::string_view fmt,
          fmt::format_args vargs) {
  // Prepare bounded stack buffer.
  truncating_buffer<logger::kBufferSize> buffer;

  // Format timestamp.
  auto now = std::chrono::system_clock::now();
  auto now_ms = std::chrono::time_point_cast<std::chrono::milliseconds>(now);
  auto now_t = std::chrono::system_clock::to_time_t(now);

  fmt::format_to(std::back_insert_iterator(buffer), "{:%m-%d %H:%M:%S}.{:03}",
                 fmt::localtime(now_t),
                 now_ms.time_since_epoch().count() % 1000);

  // Format file, line.
  fmt::format_to(std::back_insert_iterator(buffer),
                 "{} {} {}:{} {}: ", kLevelNames[level], tag, file_name, line,
                 function_name);

  // Format message.
  fmt::vformat_to(std::back_insert_iterator(buffer), fmt, vargs);

  // Print to stdout.
  buffer.buffer[buffer.len] = '\0';
  ::printf("%s\n", buffer.buffer);
}

}  // namespace logger
