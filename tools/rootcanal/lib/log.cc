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

#include "log.h"

#include <fmt/color.h>
#include <fmt/core.h>

#include <array>
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <optional>

namespace rootcanal::log {

// Enable flag for log styling.
static bool enable_log_color = true;

void SetLogColorEnable(bool enable) { enable_log_color = enable; }

static std::array<char, 5> verbosity_tag = {'D', 'I', 'W', 'E', 'F'};

static std::array<std::text_style, 5> text_style = {
        std::fg(std::color::dim_gray),
        std::fg(std::color::floral_white),
        std::emphasis::bold | std::fg(std::color::yellow),
        std::emphasis::bold | std::fg(std::color::orange_red),
        std::emphasis::bold | std::fg(std::color::red),
};

static std::array<std::color, 16> text_color = {
        std::color::cadet_blue,  std::color::aquamarine,    std::color::indian_red,
        std::color::blue_violet, std::color::chartreuse,    std::color::medium_sea_green,
        std::color::deep_pink,   std::color::medium_orchid, std::color::green_yellow,
        std::color::dark_orange, std::color::golden_rod,    std::color::medium_slate_blue,
        std::color::coral,       std::color::lemon_chiffon, std::color::wheat,
        std::color::turquoise,
};

void VLog(Verbosity verb, char const* file, int line, std::optional<int> instance,
          char const* format, std::format_args args) {
  // Generate the time label.
  auto now = std::chrono::system_clock::now();
  auto now_ms = std::chrono::time_point_cast<std::chrono::milliseconds>(now);
  auto now_t = std::chrono::system_clock::to_time_t(now);
  char time_str[19];  // "mm-dd_HH:MM:SS.mmm\0" is 19 byte long
  auto n = std::strftime(time_str, sizeof(time_str), "%m-%d %H:%M:%S", std::localtime(&now_t));
  snprintf(time_str + n, sizeof(time_str) - n, ".%03u",
           static_cast<unsigned int>(now_ms.time_since_epoch().count() % 1000));

  // Generate the file label.
  char delimiter = '/';
  char const* file_name = ::strrchr(file, delimiter);
  file_name = file_name == nullptr ? file : file_name + 1;
  char file_str[40];  // file:line limited to 40 characters
  snprintf(file_str, sizeof(file_str), "%.35s:%d", file_name, line);

  std::print("root-canal {} {} {:<35.35} ", verbosity_tag[verb], time_str, file_str);

  if (instance.has_value() && enable_log_color) {
    std::color instance_color = text_color[*instance % text_color.size()];
    std::print(std::bg(instance_color) | std::fg(std::color::black), " {:>2} ", *instance);
    std::print(" ");
  } else if (instance.has_value()) {
    std::print(" {:>2}  ", *instance);
  } else {
    std::print("     ");
  }

  if (enable_log_color) {
    std::text_style style = text_style[verb];
    std::vprint(stdout, style, format, args);
  } else {
    std::vprint(stdout, format, args);
  }

  std::print("\n");

  if (verb == Verbosity::kFatal) {
    std::abort();
  }
}

}  // namespace rootcanal::log
