/******************************************************************************
 *
 *  Copyright 2019 Google, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

#pragma once

#include <cstdlib>

#ifndef LOG_TAG
#define LOG_TAG "bluetooth"
#endif

static_assert(LOG_TAG != nullptr, "LOG_TAG should never be NULL");

#include "os/logging/log_adapter.h"

#if defined(OS_ANDROID)

#include <log/log.h>
#include <log/log_event_list.h>

#ifdef FUZZ_TARGET
// to improve fuzzing efficiency, log statements are
// silenced in fuzzers
#define _LOGWRAPPER(...)
#define LOG_VERBOSE_INT(...)
#define LOG_DEBUG_INT(...)
#define LOG_INFO_INT(...)
#define LOG_WARN_INT(...)
#else

// in some code where init flags APIs are not
// available, we can bypass it by adding
//
// #define IGNORE_LOG_TAG_FILTER 1
//
// before including this file

#define _LOGWRAPPER(ALOG_MACRO, fmt, ...)                                      \
  do {                                                                         \
      ALOG_MACRO(fmt, ##__VA_ARGS__);                                          \
  } while (false)

#ifdef IGNORE_LOG_TAG_FILTER

#define LOGWRAPPER(ALOG_MACRO, fmt, ...)                                       \
  _LOGWRAPPER(ALOG_MACRO, fmt, ##__VA_ARGS__)

#else

#include "common/init_flags.h"

#define LOGWRAPPER(ALOG_MACRO, fmt, ...)                                       \
  do {                                                                         \
    if (::bluetooth::common::InitFlags::IsDebugLoggingEnabledForTag(LOG_TAG)){ \
      ALOG_MACRO(fmt, ##__VA_ARGS__);                                          \
    }                                                                          \
  } while (false)

#endif

// MACROs ending with _INT and [_]LOGWRAPPER are internal and should not be
// used directly (use LOG_XXX without _INT suffix instead).
// the output of LOG_XXX_INT does not contain the source
// location of the log emitting statements, so far they are only used by
// LogMsg.
#define LOG_VERBOSE_INT(fmt, ...) LOGWRAPPER(ALOGV, fmt, ##__VA_ARGS__)
#define LOG_DEBUG_INT(fmt, ...) LOGWRAPPER(ALOGD, fmt, ##__VA_ARGS__)
#define LOG_INFO_INT(fmt, ...) LOGWRAPPER(ALOGI, fmt, ##__VA_ARGS__)
#define LOG_WARN_INT(fmt, ...) LOGWRAPPER(ALOGW, fmt, ##__VA_ARGS__)

#endif /* FUZZ_TARGET */

// always enable error logging
#define LOG_ERROR_INT(fmt, ...) _LOGWRAPPER(ALOGE, fmt, ##__VA_ARGS__)

#elif defined (ANDROID_EMULATOR)
// Log using android emulator logging mechanism
#include "android/utils/debug.h"

#define LOGWRAPPER(fmt, ...) VERBOSE_INFO(bluetooth,                          \
                                          "bluetooth: " fmt, ##__VA_ARGS__)

#define LOG_VERBOSE_INT(fmt, ...) LOGWRAPPER(fmt, ##__VA_ARGS__)
#define LOG_DEBUG_INT(fmt, ...)  LOGWRAPPER(fmt, ##__VA_ARGS__)
#define LOG_INFO_INT(fmt, ...)   LOGWRAPPER(fmt, ##__VA_ARGS__)
#define LOG_WARN_INT(fmt, ...)   LOGWRAPPER(fmt, ##__VA_ARGS__)
#define LOG_ERROR_INT(fmt, ...)  LOGWRAPPER(fmt, ##__VA_ARGS__)

#define LOG_ALWAYS_FATAL_INT(fmt, ...)                                        \
  do {                                                                        \
    fprintf(stderr, fmt "\n", ##__VA_ARGS__);                                 \
    abort();                                                                  \
  } while (false)

#elif defined(TARGET_FLOSS)
#include "gd/os/syslog.h"

#ifdef FUZZ_TARGET
#define _LOGWRAPPER(tag, fmt, ...)
#define LOG_VERBOSE_INT(...)
#define LOG_DEBUG_INT(...)
#define LOG_INFO_INT(...)
#define LOG_WARN_INT(...)
#else

#define _LOGWRAPPER(tag, fmt, ...)                                            \
  write_syslog(tag, "%s: " fmt, LOG_TAG, ##__VA_ARGS__)

#ifdef IGNORE_LOG_TAG_FILTER

#define LOGWRAPPER(TAG, fmt, ...)                                             \
    _LOGWRAPPER(TAG, fmt, ##__VA_ARGS__);

#else

#include "gd/common/init_flags.h"

#define LOGWRAPPER(TAG, fmt, ...)                                             \
do {                                                                          \
    if (::bluetooth::common::InitFlags::IsDebugLoggingEnabledForTag(TAG)) {   \
      _LOGWRAPPER(TAG, fmt, ##__VA_ARGS__);                                    \
    }                                                                         \
  } while (false)

#endif

#define LOG_VERBOSE_INT(fmt, ...)                                             \
  LOGWRAPPER(LOG_TAG_VERBOSE, fmt, ##__VA_ARGS__)

#define LOG_DEBUG_INT(fmt, ...)                                               \
  LOGWRAPPER(LOG_TAG_DEBUG, fmt, ##__VA_ARGS__)

#define LOG_INFO_INT(fmt, ...)                                                \
  LOGWRAPPER(LOG_TAG_INFO, fmt, ##__VA_ARGS__)

#define LOG_WARN_INT(fmt, ...)                                                \
  LOGWRAPPER(LOG_TAG_WARN, fmt, ##__VA_ARGS__)

#endif /*FUZZ_TARGET*/

#define LOG_ERROR_INT(fmt, ...)                                               \
  _LOGWRAPPER(LOG_TAG_ERROR, fmt, ##__VA_ARGS__)

#define LOG_ALWAYS_FATAL_INT(fmt, ...)                                        \
  do {                                                                        \
    _LOGWRAPPER(LOG_TAG_FATAL, fmt, ##__VA_ARGS__);                            \
    abort();                                                                  \
  } while (false)

#ifndef LOG_EVENT_INT
#define LOG_EVENT_INT(...)
#endif

#else
/* syslog didn't work well here since we would be redefining LOG_DEBUG. */
#include <sys/syscall.h>
#include <sys/types.h>
#include <unistd.h>

#include <chrono>
#include <cstdio>
#include <ctime>

#ifdef FUZZ_TARGET
#define _LOGWRAPPER(fmt, ...)
#define LOG_VERBOSE_INT(...)
#define LOG_DEBUG_INT(...)
#define LOG_INFO_INT(...)
#define LOG_WARN_INT(...)

#else

#define _LOGWRAPPER(fmt, ...)                                                                                       \
  do {                                                                                                              \
    auto _now = std::chrono::system_clock::now();                                                                   \
    auto _now_ms = std::chrono::time_point_cast<std::chrono::milliseconds>(_now);                                   \
    auto _now_t = std::chrono::system_clock::to_time_t(_now);                                                       \
    /* YYYY-MM-DD_HH:MM:SS.sss is 23 byte long, plus 1 for null terminator */                                       \
    char _buf[24];                                                                                                  \
    auto l = std::strftime(_buf, sizeof(_buf), "%Y-%m-%d %H:%M:%S", std::localtime(&_now_t));                       \
    snprintf(                                                                                                       \
        _buf + l, sizeof(_buf) - l, ".%03u", static_cast<unsigned int>(_now_ms.time_since_epoch().count() % 1000)); \
    /* pid max is 2^22 = 4194304 in 64-bit system, and 32768 by default, hence 7 digits are needed most */          \
    fprintf(                                                                                                        \
        stderr,                                                                                                     \
        "%s %7d %7ld %s - %s:%d - %s: " fmt "\n",                                                                   \
        _buf,                                                                                                       \
        static_cast<int>(getpid()),                                                                                 \
        syscall(SYS_gettid),                                                                                        \
        LOG_TAG,                                                                                                    \
        __FILE__,                                                                                                   \
        __LINE__,                                                                                                   \
        __func__,                                                                                                   \
        ##__VA_ARGS__);                                                                                             \
  } while (false)

#ifdef IGNORE_LOG_TAG_FILTER

#define LOGWRAPPER(fmt, ...)                                                   \
do {                                                                           \
    _LOGWRAPPER(fmt, ##__VA_ARGS__);                                           \
  } while (false)

#else

#include "common/init_flags.h"

#define LOGWRAPPER(fmt, ...)                                                   \
  do {                                                                         \
    if (::bluetooth::common::InitFlags::IsDebugLoggingEnabledForTag(LOG_TAG)) {\
      _LOGWRAPPER(fmt, ##__VA_ARGS__);                                         \
    }                                                                          \
  } while (false)

#endif

#define LOG_VERBOSE_INT(fmt, ...)                                             \
  LOGWRAPPER(fmt, ##__VA_ARGS__)

#define LOG_DEBUG_INT(fmt, ...)                                               \
  LOGWRAPPER(fmt, ##__VA_ARGS__)

#define LOG_INFO_INT(fmt, ...)                                                \
  LOGWRAPPER(fmt, ##__VA_ARGS__)

#define LOG_WARN_INT(fmt, ...)                                                \
  LOGWRAPPER(fmt, ##__VA_ARGS__)

#endif /* FUZZ_TARGET */

#define LOG_ERROR_INT(fmt, ...)                                               \
  _LOGWRAPPER(fmt, ##__VA_ARGS__)

#ifndef LOG_ALWAYS_FATAL_INT
#define LOG_ALWAYS_FATAL_INT(fmt, ...)                                        \
  do {                                                                        \
    _LOGWRAPPER(fmt, ##__VA_ARGS__);                                           \
    abort();                                                                  \
  } while (false)
#endif

#ifndef LOG_EVENT_INT
#define LOG_EVENT_INT(...)
#endif

#endif /* defined(OS_ANDROID) */

#define LOG_VERBOSE(fmt, ...)                                             \
  LOG_VERBOSE_INT("%s:%d - %s: " fmt, __FILE__, __LINE__, __func__, ##__VA_ARGS__)

#define LOG_DEBUG(fmt, ...)                                               \
  LOG_DEBUG_INT("%s:%d - %s: " fmt, __FILE__, __LINE__, __func__, ##__VA_ARGS__)

#define LOG_INFO(fmt, ...)                                                \
  LOG_INFO_INT("%s:%d - %s: " fmt, __FILE__, __LINE__, __func__, ##__VA_ARGS__)

#define LOG_WARN(fmt, ...)                                                \
  LOG_WARN_INT("%s:%d - %s: " fmt, __FILE__, __LINE__, __func__, ##__VA_ARGS__)

#define LOG_ERROR(fmt, ...)                                               \
  LOG_ERROR_INT("%s:%d %s: " fmt, __FILE__, __LINE__, __func__, ##__VA_ARGS__)

#ifndef LOG_ALWAYS_FATAL
#define LOG_ALWAYS_FATAL(fmt, ...)                                       \
  LOG_ALWAYS_FATAL_INT("%s:%d %s: " fmt, __FILE__, __LINE__, __func__, ##__VA_ARGS__)
#endif

#define ASSERT(condition)                                    \
  do {                                                       \
    if (!(condition)) {                                      \
      LOG_ALWAYS_FATAL("assertion '" #condition "' failed"); \
    }                                                        \
  } while (false)

#define ASSERT_LOG(condition, fmt, args...)                                 \
  do {                                                                      \
    if (!(condition)) {                                                     \
      LOG_ALWAYS_FATAL("assertion '" #condition "' failed - " fmt, ##args); \
    }                                                                       \
  } while (false)

#ifndef CASE_RETURN_TEXT
#define CASE_RETURN_TEXT(code) \
  case code:                   \
    return #code
#endif
