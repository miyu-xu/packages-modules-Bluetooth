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

#if defined(OS_ANDROID)

#include <log/log.h>

#include "common/init_flags.h"

#ifdef FUZZ_TARGET
#define LOG_VERBOSE(...)
#define LOG_DEBUG(...)
#define LOG_INFO(...)
#define LOG_WARN(...)
#else

static_assert(LOG_TAG != nullptr, "LOG_TAG is null after header inclusion");

#define LOG_VERBOSE(fmt, args...)                                             \
  do {                                                                        \
    if (bluetooth::common::InitFlags::IsDebugLoggingEnabledForTag(LOG_TAG)) { \
      ALOGV("%s:%d %s: " fmt, __FILE__, __LINE__, __func__, ##args);          \
    }                                                                         \
  } while (false)

#define LOG_DEBUG(fmt, args...)                                               \
  do {                                                                        \
    if (bluetooth::common::InitFlags::IsDebugLoggingEnabledForTag(LOG_TAG)) { \
      ALOGD("%s:%d %s: " fmt, __FILE__, __LINE__, __func__, ##args);          \
    }                                                                         \
  } while (false)

#define LOG_INFO(fmt, args...) ALOGI("%s:%d %s: " fmt, __FILE__, __LINE__, __func__, ##args)
#define LOG_WARN(fmt, args...) ALOGW("%s:%d %s: " fmt, __FILE__, __LINE__, __func__, ##args)
#endif /* FUZZ_TARGET */
#define LOG_ERROR(fmt, args...) ALOGE("%s:%d %s: " fmt, __FILE__, __LINE__, __func__, ##args)

#elif defined (ANDROID_EMULATOR)
// Log using android emulator logging mechanism
#include "android/utils/debug.h"

#define LOGWRAPPER(fmt, args...) VERBOSE_INFO(bluetooth, "bluetooth: %s:%d - %s: " fmt, \
                                              __FILE__, __LINE__, __func__, ##args)

#define LOG_VEBOSE(...) LOGWRAPPER(__VA_ARGS__)
#define LOG_DEBUG(...)  LOGWRAPPER(__VA_ARGS__)
#define LOG_INFO(...)   LOGWRAPPER(__VA_ARGS__)
#define LOG_WARN(...)   LOGWRAPPER(__VA_ARGS__)
#define LOG_ERROR(...)  LOGWRAPPER(__VA_ARGS__)
#define LOG_ALWAYS_FATAL(fmt, args...)                                              \
  do {                                                                              \
    fprintf(stderr, "%s:%d - %s: " fmt "\n", __FILE__, __LINE__, __func__, ##args); \
    abort();                                                                        \
  } while (false)
#elif defined(TARGET_FLOSS)
#include "gd/os/syslog.h"

// Prefix the log with tag, file, line and function
#define LOGWRAPPER(tag, fmt, args...) \
  write_syslog(tag, "%s:%s:%d - %s: " fmt, LOG_TAG, __FILE__, __LINE__, __func__, ##args)

#ifdef FUZZ_TARGET
#define LOG_VERBOSE(...)
#define LOG_DEBUG(...)
#define LOG_INFO(...)
#define LOG_WARN(...)
#else
#define LOG_VERBOSE(...)                                                      \
  do {                                                                        \
    if (bluetooth::common::InitFlags::IsDebugLoggingEnabledForTag(LOG_TAG)) { \
      LOGWRAPPER(LOG_TAG_VERBOSE, __VA_ARGS__);                               \
    }                                                                         \
  } while (false)
#define LOG_DEBUG(...)                                                        \
  do {                                                                        \
    if (bluetooth::common::InitFlags::IsDebugLoggingEnabledForTag(LOG_TAG)) { \
      LOGWRAPPER(LOG_TAG_DEBUG, __VA_ARGS__);                                 \
    }                                                                         \
  } while (false)
#define LOG_INFO(...) LOGWRAPPER(LOG_TAG_INFO, __VA_ARGS__)
#define LOG_WARN(...) LOGWRAPPER(LOG_TAG_WARN, __VA_ARGS__)
#endif /*FUZZ_TARGET*/
#define LOG_ERROR(...) LOGWRAPPER(LOG_TAG_ERROR, __VA_ARGS__)

#define LOG_ALWAYS_FATAL(...)               \
  do {                                      \
    LOGWRAPPER(LOG_TAG_FATAL, __VA_ARGS__); \
    abort();                                \
  } while (false)

#ifndef android_errorWriteLog
#define android_errorWriteLog(tag, subTag) LOG_ERROR("ERROR tag: 0x%x, sub_tag: %s", tag, subTag)
#endif

#ifndef android_errorWriteWithInfoLog
#define android_errorWriteWithInfoLog(tag, subTag, uid, data, dataLen) \
  LOG_ERROR("ERROR tag: 0x%x, sub_tag: %s", tag, subTag)
#endif

#ifndef LOG_EVENT_INT
#define LOG_EVENT_INT(...)
#endif

#else

namespace bluetooth::os {
enum log_level { LOG_LEVEL_VERBOSE, LOG_LEVEL_DEBUG, LOG_LEVEL_INFO, LOG_LEVEL_WARN, LOG_LEVEL_ERROR, LOG_LEVEL_FATAL };

void log(enum log_level level, char const* tag, char const* format, ...) __attribute__((format(printf, 3, 4)));
}  // namespace bluetooth::os

#define LOGWRAPPER(level, fmt, args...) \
  ::bluetooth::os::log(::bluetooth::os::log_level::LOG_LEVEL_##level, LOG_TAG, "%s: " fmt, __func__, ##args)

#ifdef FUZZ_TARGET
#define LOG_VERBOSE(...)
#define LOG_DEBUG(...)
#define LOG_INFO(...)
#define LOG_WARN(...)
#else
#define LOG_VERBOSE(fmt, args...)                                             \
  do {                                                                        \
    if (bluetooth::common::InitFlags::IsDebugLoggingEnabledForTag(LOG_TAG)) { \
      LOGWRAPPER(VERBOSE, fmt, ##args);                                       \
    }                                                                         \
  } while (false)
#define LOG_DEBUG(fmt, args...)                                               \
  do {                                                                        \
    if (bluetooth::common::InitFlags::IsDebugLoggingEnabledForTag(LOG_TAG)) { \
      LOGWRAPPER(DEBUG, fmt, ##args);                                         \
    }                                                                         \
  } while (false)
#define LOG_INFO(...) LOGWRAPPER(INFO, __VA_ARGS__)
#define LOG_WARN(...) LOGWRAPPER(WARN, __VA_ARGS__)
#endif /* FUZZ_TARGET */
#define LOG_ERROR(...) LOGWRAPPER(ERROR, __VA_ARGS__)

#ifndef LOG_ALWAYS_FATAL
#define LOG_ALWAYS_FATAL(...)       \
  do {                              \
    LOGWRAPPER(FATAL, __VA_ARGS__); \
    abort();                        \
  } while (false)
#endif

#ifndef android_errorWriteLog
#define android_errorWriteLog(tag, subTag) LOG_ERROR("ERROR tag: 0x%x, sub_tag: %s", tag, subTag)
#endif

#ifndef android_errorWriteWithInfoLog
#define android_errorWriteWithInfoLog(tag, subTag, uid, data, dataLen) \
  LOG_ERROR("ERROR tag: 0x%x, sub_tag: %s", tag, subTag)
#endif

#ifndef LOG_EVENT_INT
#define LOG_EVENT_INT(...)
#endif

#endif /* defined(OS_ANDROID) */

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
