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

#define _LOGWRAPPER(ALOG_MACRO, fmt, args...)                                  \
  do {                                                                         \
      ALOG_MACRO(fmt, ##args);                                                 \
  } while (false)

#ifdef IGNORE_LOG_TAG_FILTER

#define LOGWRAPPER(ALOG_MACRO, fmt, args...)                                   \
  _LOGWRAPPER(ALOG_MACRO, fmt, ##args)

#else

#include "common/init_flags.h"

#define LOGWRAPPER(ALOG_MACRO, fmt, args...)                                   \
  do {                                                                         \
    if (::bluetooth::common::InitFlags::IsDebugLoggingEnabledForTag(LOG_TAG)){ \
      ALOG_MACRO(fmt, ##args);                                                 \
    }                                                                          \
  } while (false)

#endif

// MACROs ending with _INT and [_]LOGWRAPPER are internal and should not be
// used directly (use LOG_XXX instead).
// the output of LOG_XXX_INT does not contain the source
// location of the log emitting statement, so far they are only used by
// LogMsg ,where the source locations is passed in.
#define LOG_VERBOSE_INT(fmt, args...) LOGWRAPPER(ALOGV, fmt, ##args)
#define LOG_DEBUG_INT(fmt, args...) LOGWRAPPER(ALOGD, fmt, ##args)


#define LOG_INFO_INT(fmt, args...) _LOGWRAPPER(ALOGI, fmt, ##args)
#define LOG_WARN_INT(fmt, args...) _LOGWRAPPER(ALOGW, fmt, ##args)

#endif /* FUZZ_TARGET */

// always enable error logging
#define LOG_ERROR_INT(fmt, args...) _LOGWRAPPER(ALOGE, fmt, ##args)

#elif defined (ANDROID_EMULATOR)
// Log using android emulator logging mechanism
#include "android/utils/debug.h"

#define LOGWRAPPER(fmt, args...) VERBOSE_INFO(bluetooth,                      \
                                          "bluetooth: " fmt, ##args)

#define LOG_VERBOSE_INT(fmt, args...) LOGWRAPPER(fmt, ##args)
#define LOG_DEBUG_INT(fmt, args...)  LOGWRAPPER(fmt, ##args)
#define LOG_INFO_INT(fmt, args...)   LOGWRAPPER(fmt, ##args)
#define LOG_WARN_INT(fmt, args...)   LOGWRAPPER(fmt, ##args)
#define LOG_ERROR_INT(fmt, args...)  LOGWRAPPER(fmt, ##args)

#define LOG_ALWAYS_FATAL_INT(fmt, args...)                                    \
  do {                                                                        \
    fprintf(stderr, fmt "\n", ##args);                                        \
    abort();                                                                  \
  } while (false)

#elif defined(TARGET_FLOSS)
#include "gd/os/syslog.h"

#ifdef FUZZ_TARGET
#define _LOGWRAPPER(...)
#define LOG_VERBOSE_INT(...)
#define LOG_DEBUG_INT(...)
#define LOG_INFO_INT(...)
#define LOG_WARN_INT(...)
#else

#define _LOGWRAPPER(tag, fmt, args...)                                        \
  write_syslog(tag, "%s: " fmt, LOG_TAG, ##args)

#ifdef IGNORE_LOG_TAG_FILTER

#define LOGWRAPPER(TAG, fmt, args...)                                         \
    _LOGWRAPPER(TAG, fmt, ##args);

#else

#include "gd/common/init_flags.h"

#define LOGWRAPPER(TAG, fmt, args...)                                         \
do {                                                                          \
    if (::bluetooth::common::InitFlags::IsDebugLoggingEnabledForTag(TAG)) {   \
      _LOGWRAPPER(TAG, fmt, ##args);                                          \
    }                                                                         \
  } while (false)

#endif

#define LOG_VERBOSE_INT(fmt, args...)                                         \
  LOGWRAPPER(LOG_TAG_VERBOSE, fmt, ##args)

#define LOG_DEBUG_INT(fmt, args...)                                           \
  LOGWRAPPER(LOG_TAG_DEBUG, fmt, ##args)

#define LOG_INFO_INT(fmt, args...)                                            \
  _LOGWRAPPER(LOG_TAG_INFO, fmt, ##args)

#define LOG_WARN_INT(fmt, args...)                                            \
  _LOGWRAPPER(LOG_TAG_WARN, fmt, ##args)

#endif /*FUZZ_TARGET*/

#define LOG_ERROR_INT(fmt, args...)                                           \
  _LOGWRAPPER(LOG_TAG_ERROR, fmt, ##args)

#define LOG_ALWAYS_FATAL_INT(fmt, args...)                                    \
  do {                                                                        \
    _LOGWRAPPER(LOG_TAG_FATAL, fmt, ##args);                                  \
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
#define _LOGWRAPPER(...)
#define LOG_VERBOSE_INT(...)
#define LOG_DEBUG_INT(...)
#define LOG_INFO_INT(...)
#define LOG_WARN_INT(...)

#else

#define _LOGWRAPPER(fmt, args...)                                                                                   \
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
        "%s %7d %7ld %s " fmt "\n",                                                                                 \
        _buf,                                                                                                       \
        static_cast<int>(getpid()),                                                                                 \
        syscall(SYS_gettid),                                                                                        \
        LOG_TAG,                                                                                                    \
        ##args);                                                                                                    \
  } while (false)

#ifdef IGNORE_LOG_TAG_FILTER

#define LOGWRAPPER(fmt, args...)                                               \
do {                                                                           \
    _LOGWRAPPER(fmt, ##args);                                                  \
  } while (false)

#else

#include "common/init_flags.h"

#define LOGWRAPPER(fmt, args...)                                               \
  do {                                                                         \
    if (::bluetooth::common::InitFlags::IsDebugLoggingEnabledForTag(LOG_TAG)) {\
      _LOGWRAPPER(fmt, ##args);                                                \
    }                                                                          \
  } while (false)

#endif

#define LOG_VERBOSE_INT(fmt, args...)                                         \
  LOGWRAPPER(fmt, ##args)

#define LOG_DEBUG_INT(fmt, args...)                                           \
  LOGWRAPPER(fmt, ##args)

#define LOG_INFO_INT(fmt, args...)                                            \
  _LOGWRAPPER(fmt, ##args)

#define LOG_WARN_INT(fmt, args...)                                            \
  _LOGWRAPPER(fmt, ##args)

#endif /* FUZZ_TARGET */

#define LOG_ERROR_INT(fmt, args...)                                           \
  _LOGWRAPPER(fmt, ##args)

#ifndef LOG_ALWAYS_FATAL_INT
#define LOG_ALWAYS_FATAL_INT(fmt, args...)                                    \
  do {                                                                        \
    _LOGWRAPPER(fmt, ##args);                                                 \
    abort();                                                                  \
  } while (false)
#endif

#ifndef LOG_EVENT_INT
#define LOG_EVENT_INT(...)
#endif

#endif /* defined(OS_ANDROID) */

#define LOG_VERBOSE(fmt, args...)                                             \
  LOG_VERBOSE_INT("%s:%d - %s: " fmt, __FILE__, __LINE__, __func__, ##args)

#define LOG_DEBUG(fmt, args...)                                               \
  LOG_DEBUG_INT("%s:%d - %s: " fmt, __FILE__, __LINE__, __func__, ##args)

#define LOG_INFO(fmt, args...)                                                \
  LOG_INFO_INT("%s:%d - %s: " fmt, __FILE__, __LINE__, __func__, ##args)

#define LOG_WARN(fmt, args...)                                                \
  LOG_WARN_INT("%s:%d - %s: " fmt, __FILE__, __LINE__, __func__, ##args)

#define LOG_ERROR(fmt, args...)                                               \
  LOG_ERROR_INT("%s:%d %s: " fmt, __FILE__, __LINE__, __func__, ##args)

#ifndef LOG_ALWAYS_FATAL
#define LOG_ALWAYS_FATAL(fmt, args...)                                    \
  LOG_ALWAYS_FATAL_INT("%s:%d %s: " fmt, __FILE__, __LINE__, __func__, ##args)
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
