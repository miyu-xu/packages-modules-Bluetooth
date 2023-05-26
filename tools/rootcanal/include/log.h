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

#pragma once

#include <fmt/format.h>
#include <fmt/printf.h>

#include <optional>

namespace rootcanal::log {

enum Verbosity {
  kDebug,
  kInfo,
  kWarning,
  kError,
  kFatal,
};

void SetLogColorEnable(bool);
// void SetLogSink(std::function<void(Verbosity, std::string));

void Log(Verbosity verb, std::optional<int> instance, char const* format,
         fmt::format_args args);

template <typename... Args>
static void debug(int instance, const char* format, const Args&... args) {
  Log(Verbosity::kDebug, instance, format, fmt::make_format_args(args...));
}

template <typename... Args>
static void debug(const char* format, const Args&... args) {
  Log(Verbosity::kDebug, {}, format, fmt::make_format_args(args...));
}

template <typename... Args>
static void info(int instance, const char* format, const Args&... args) {
  Log(Verbosity::kInfo, instance, format, fmt::make_format_args(args...));
}

template <typename... Args>
static void info(const char* format, const Args&... args) {
  Log(Verbosity::kInfo, {}, format, fmt::make_format_args(args...));
}

template <typename... Args>
static void warning(int instance, const char* format, const Args&... args) {
  Log(Verbosity::kWarning, instance, format, fmt::make_format_args(args...));
}

template <typename... Args>
static void warning(const char* format, const Args&... args) {
  Log(Verbosity::kWarning, {}, format, fmt::make_format_args(args...));
}

template <typename... Args>
static void error(int instance, const char* format, const Args&... args) {
  Log(Verbosity::kError, instance, format, fmt::make_format_args(args...));
}

template <typename... Args>
static void error(const char* format, const Args&... args) {
  Log(Verbosity::kError, {}, format, fmt::make_format_args(args...));
}

template <typename... Args>
static void fatal(int instance, const char* format, const Args&... args) {
  Log(Verbosity::kFatal, instance, format, fmt::make_format_args(args...));
  ::abort();
}

template <typename... Args>
static void fatal(const char* format, const Args&... args) {
  Log(Verbosity::kFatal, {}, format, fmt::make_format_args(args...));
  ::abort();
}

// TODO: still required by the generated HCI parser and serializer backend.
#define LOG_INFO(...) rootcanal::log::info("{}", fmt::sprintf(__VA_ARGS__))
#define LOG_WARN(...) rootcanal::log::warning("{}", fmt::sprintf(__VA_ARGS__))
#define LOG_ERROR(...) rootcanal::log::error("{}", fmt::sprintf(__VA_ARGS__))
#define LOG_ALWAYS_FATAL(...) \
  rootcanal::log::fatal("{}", fmt::sprintf(__VA_ARGS__))

#define ASSERT(x)                                                          \
  __builtin_expect((x) != 0, true) ||                                      \
      (rootcanal::log::fatal("{}:{} Check failed: {}", __FILE__, __LINE__, \
                             #x),                                          \
       false)

#define ASSERT_LOG(x, ...)                                                     \
  __builtin_expect((x) != 0, true) ||                                          \
      (rootcanal::log::fatal("{}:{} Check failed: {}, {}", __FILE__, __LINE__, \
                             #x, fmt::sprintf(__VA_ARGS__)),                   \
       false)

}  // namespace rootcanal::log
