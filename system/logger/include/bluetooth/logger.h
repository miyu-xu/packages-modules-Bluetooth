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
void vlog(Level level, char const* tag, char const* file_name, int line,
          char const* function_name, fmt::string_view fmt,
          fmt::format_args vargs);

template <Level level, typename... T>
struct log {
  log(fmt::format_string<T...> fmt, T&&... args,
      char const* file_name = __builtin_FILE(), int line = __builtin_LINE(),
      char const* function_name = __builtin_FUNCTION()) {
    vlog(level, LOG_TAG, file_name, line, function_name, fmt.get(),
         fmt::make_format_args(args...));
  }
};

#if (__cplusplus >= 202002L && defined(__GNUC__) && !defined(__clang__))

template <int level, typename... T>
log(fmt::format_string<T...>, T&&...) -> log<level, T...>;

template <typename... T>
using fatal = log<kFatal, T...>;
template <typename... T>
using error = log<kError, T...>;
template <typename... T>
using warning = log<kWarning, T...>;
template <typename... T>
using info = log<kInfo, T...>;
template <typename... T>
using debug = log<kDebug, T...>;
template <typename... T>
using verbose = log<kVerbose, T...>;

#else

template <typename... T>
struct fatal : log<kFatal, T...> {
  using log<kFatal, T...>::log;
};
template <typename... T>
struct error : log<kError, T...> {
  using log<kError, T...>::log;
};
template <typename... T>
struct warn : log<kWarn, T...> {
  using log<kWarn, T...>::log;
};
template <typename... T>
struct info : log<kInfo, T...> {
  using log<kInfo, T...>::log;
};
template <typename... T>
struct debug : log<kDebug, T...> {
  using log<kDebug, T...>::log;
};
template <typename... T>
struct verbose : log<kVerbose, T...> {
  using log<kVerbose, T...>::log;
};

template <typename... T>
fatal(fmt::format_string<T...>, T&&...) -> fatal<T...>;
template <typename... T>
error(fmt::format_string<T...>, T&&...) -> error<T...>;
template <typename... T>
warn(fmt::format_string<T...>, T&&...) -> warn<T...>;
template <typename... T>
info(fmt::format_string<T...>, T&&...) -> info<T...>;
template <typename... T>
debug(fmt::format_string<T...>, T&&...) -> debug<T...>;
template <typename... T>
verbose(fmt::format_string<T...>, T&&...) -> verbose<T...>;

#endif  // GCC / C++20

}  // namespace logger
