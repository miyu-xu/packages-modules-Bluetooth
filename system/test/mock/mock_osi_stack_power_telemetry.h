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

/*
 * Generated mock file from original source file
 *   Functions generated:5
 *
 *  mockcify.pl ver 0.6.1
 */

#include <cstdint>
#include <functional>
#include <map>
#include <string>

#include "test/common/mock_functions.h"

// Original included files, if any
// NOTE: Since this is a mock file with mock definitions some number of
//       include files may not be required.  The include-what-you-use
//       still applies, but crafting proper inclusion is out of scope
//       for this effort.  This compilation unit may compile as-is, or
//       may need attention to prune from (or add to ) the inclusion set.
#include <sys/stat.h>

#include <cstdio>
#include <filesystem>
#include <sstream>

#include "bt_trace.h"
#include "osi/include/alarm.h"
#include "osi/include/stack_power_telemetry.h"
#include "stack/btm/btm_dev.h"
#include "stack/btm/btm_int_types.h"
#include "stack/btm/btm_sec.h"

// Original usings

// Mocked compile conditionals, if any

namespace test {
namespace mock {
namespace osi_stack_power_telemetry {

// Shared state between mocked functions and tests
// Name: GetCurrentTimeSec
// Params:
// Return: int64_t
struct GetCurrentTimeSec {
  static int64_t return_value;
  std::function<int64_t()> body{[]() { return return_value; }};
  int64_t operator()() { return body(); };
};
extern struct GetCurrentTimeSec GetCurrentTimeSec;

// Name: GetCurrentTimeString
// Params:
// Return: std::string
struct GetCurrentTimeString {
  static std::string return_value;
  std::function<std::string()> body{[]() { return return_value; }};
  std::string operator()() { return body(); };
};
extern struct GetCurrentTimeString GetCurrentTimeString;

// Name: GetTimeString
// Params: time_t tstamp
// Return: std::string
struct GetTimeString {
  static std::string return_value;
  std::function<std::string(time_t tstamp)> body{
      [](time_t tstamp) { return return_value; }};
  std::string operator()(time_t tstamp) { return body(tstamp); };
};
extern struct GetTimeString GetTimeString;

// Name: GetTimeStringFromSec
// Params: int64_t timeStampSec
// Return: std::string
struct GetTimeStringFromSec {
  static std::string return_value;
  std::function<std::string(int64_t timeStampSec)> body{
      [](int64_t timeStampSec) { return return_value; }};
  std::string operator()(int64_t timeStampSec) { return body(timeStampSec); };
};
extern struct GetTimeStringFromSec GetTimeStringFromSec;

// Name: LogTxPower_cb
// Params: void* res
// Return: void
struct LogTxPower_cb {
  std::function<void(void* res)> body{[](void* res) {}};
  void operator()(void* res) { body(res); };
};
extern struct LogTxPower_cb LogTxPower_cb;

static power_telemetry::LogDataContainer fake_container =
    power_telemetry::LogDataContainer();
static power_telemetry::PowerTelemetry fake_power_telemetry;
}  // namespace osi_stack_power_telemetry
}  // namespace mock
}  // namespace test

// END mockcify generation