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

#include <fmt/core.h>
#include <fmt/format.h>
#include <fmt/std.h>

#ifndef LOG_TAG
#define LOG_TAG "bluetooth"
#endif  // LOG_TAG

namespace logger {

/// Android framework log priority levels.
/// They are defined in system/core/base/include/android-base/logging.h in
/// the Android Framework code.
enum Level {
    kVerbose = 0,
    kDebug = 1,
    kInfo = 2,
    kWarn = 3,
    kError = 4,
    kFatalWithoutAbort = 5,
    kFatal = 6,
};

/// Write a single log line.
/// The implementation of this function is dependent on the backend.
void vlog(Level level, char const *tag, char const *file_name, int line,
          std::string_view fmt, fmt::format_args vargs);

#if (__cplusplus >= 202002L && defined(__GNUC__) && !defined(__clang__))

template <Level level, typename... T>
struct log {
  log(fmt::format_string<T...> fmt, T&&... args,
      char const* file_name = __builtin_FILE(),
      int line = __builtin_LINE()) {
    vlog(level, LOG_TAG, file_name, line, fmt.get(), fmt::make_format_args(args...));
  }
};

template <int level, typename... T>
log(fmt::format_string<T...>, T&&...) -> log<level, T...>;

template <typename... T> using fatal = log<kFatal, T...>;
template <typename... T> using error = log<kError, T...>;
template <typename... T> using warning = log<kWarning, T...>;
template <typename... T> using info = log<kInfo, T...>;
template <typename... T> using debug = log<kDebug, T...>;
template <typename... T> using verbose = log<kVerbose, T...>;

#else

#define define_log__(level, name) \
    template <typename... T> \
    struct name { \
        name(fmt::format_string<T...> fmt, T&&... args, \
            char const* file_name = __builtin_FILE(), \
            int line = __builtin_LINE()) { \
            vlog(level, LOG_TAG, file_name, line, fmt.get(), fmt::make_format_args(args...)); \
        } \
    }; \
    template <typename... T> name(fmt::format_string<T...>, T&&...) -> name<T...>

define_log__(kFatal, fatal);
define_log__(kError, error);
define_log__(kWarn, warn);
define_log__(kInfo, info);
define_log__(kDebug, debug);
define_log__(kVerbose, verbose);

#undef define_log__

#endif  // GCC / C++20

}  // namespace logger
