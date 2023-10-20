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

#include <syslog.h>

#include "bluetooth/logger.h"
#include "truncating_buffer.h"

namespace logger {

static constexpr size_t kBufferSize = 1024;

void vlog(Level level, char const *tag, char const *file_name, int line,
          std::string_view fmt, fmt::format_args vargs) {
    // Prepare bounded stack buffer.
    truncating_buffer<logger::kBufferSize> buffer;

    // Format file, line.
    fmt::format_to(
        std::back_insert_iterator(buffer),
        "{} {}:{}",
        tag,
        file_name,
        line);

    // Format message.
    fmt::vformat_to(std::back_insert_iterator(buffer), fmt, vargs);

    // Print to vsyslog.
    buffer.buffer[buffer.len] = '\0';
    syslog(static_cast<int>(level), "%s", buffer.buffer);
}

}  // logger
