/*
 * Copyright 2021 The Android Open Source Project
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

#include "os/log.h"

#include <stdarg.h>
#include <stdio.h>
#include <string.h>

#define TC256_MIN (17)
#define TC256_MAX (230)
/// Compute a unique 256 term color based on the module name
static unsigned tag2color(const char* tag) {
  unsigned long color = 0x5;
  while (*tag) color = ((color << 4) + color) + (unsigned long)*tag++;
  return (color % (TC256_MAX - TC256_MIN)) + TC256_MIN;
}

namespace bluetooth::os {
static const char* const level_colors[] = {
    [LOG_LEVEL_VERBOSE] = "\x1b[100m\x1b[37m",
    [LOG_LEVEL_DEBUG] = "\x1b[100m\x1b[94m\x1b[1m",
    [LOG_LEVEL_INFO] = "\x1b[100m\x1b[92m\x1b[1m",
    [LOG_LEVEL_WARN] = "\x1b[103m\x1b[30m",
    [LOG_LEVEL_ERROR] = "\x1b[101m\x1b[30m",
    [LOG_LEVEL_FATAL] = "\x1b[101m\x1b[30m",
};

static const char* const line_colors[] = {
    [LOG_LEVEL_VERBOSE] = "\x1b[2m",
    [LOG_LEVEL_DEBUG] = "\x1b[2m",
    [LOG_LEVEL_INFO] = "",
    [LOG_LEVEL_WARN] = "\x1b[1m\x1b[93m",
    [LOG_LEVEL_ERROR] = "\x1b[1m\x1b[91m",
    [LOG_LEVEL_FATAL] = "\x1b[1m\x1b[91m",
};

static const char* const levels[] = {
    [LOG_LEVEL_VERBOSE] = " V ",
    [LOG_LEVEL_DEBUG] = " D ",
    [LOG_LEVEL_INFO] = " I ",
    [LOG_LEVEL_WARN] = " W ",
    [LOG_LEVEL_ERROR] = " E ",
    [LOG_LEVEL_FATAL] = " F ",
};

void log(enum log_level level, char const* tag, char const* format, ...) {
  char logline[1024] = {
      0,
  };
  va_list ap;
  va_start(ap, format);
  int len = vsnprintf(logline, sizeof(logline), format, ap);
  va_end(ap);

  if ((unsigned)len >= sizeof(logline) - 1) return;

  logline[len] = '\n';

  unsigned color = tag2color(tag);

  char* eol = strchr(logline, '\n');
  *eol = '\0';

  printf("\x1b[2m%10.10u\x1b[0m ", 0);

  printf(
      "\x1b[1;38;5;%um%20.20s\x1b[0m "
      "%s%s\x1b[0m %s%s\x1b[0m\n",
      color,
      tag,
      level_colors[level],
      levels[level],
      line_colors[level],
      logline);
};
}  // namespace bluetooth::os
