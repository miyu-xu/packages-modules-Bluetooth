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

#include <log/log.h>

#include "bluetooth/logger.h"
#include "truncating_buffer.h"

namespace logger {

static constexpr size_t kBufferSize = 1024;

static android_LogPriority level_to_priority(Level level) {
  switch (level) {
    case kVerbose:
      return ANDROID_LOG_VERBOSE;
    case kDebug:
      return ANDROID_LOG_DEBUG;
    case kInfo:
      return ANDROID_LOG_INFO;
    case kWarn:
      return ANDROID_LOG_WARN;
    case kError:
      return ANDROID_LOG_ERROR;
    case kFatal:
      return ANDROID_LOG_FATAL;
    default:
      break;
  }
  return ANDROID_LOG_DEFAULT;
}

void vlog(Level level, char const* tag, char const* file_name, int line,
          char const* function_name, std::string_view fmt,
          fmt::format_args vargs) {
  // Check if log is enabled.
  if (!__android_log_is_loggable(level, tag, ANDROID_LOG_DEFAULT) &&
      !__android_log_is_loggable(level, "bluetooth", ANDROID_LOG_DEFAULT)) {
    return;
  }

  // Format to stack buffer.
  truncating_buffer<kBufferSize> buffer;
  fmt::format_to(std::back_insert_iterator(buffer), "{}: ", function_name);
  auto result = fmt::vformat_to(std::back_insert_iterator(buffer), fmt, vargs);
  buffer.buffer[buffer.len] = '\0';

  // Send message to liblog.
  struct __android_log_message message = {
      .struct_size = sizeof(__android_log_message),
      .buffer_id = LOG_ID_MAIN,
      .priority = level_to_priority(level),
      .tag = LOG_TAG,
      .file = file_name,
      .line = static_cast<uint32_t>(line),
      .message = buffer.buffer,
  };
  __android_log_write_log_message(&message);
}

}  // namespace logger
