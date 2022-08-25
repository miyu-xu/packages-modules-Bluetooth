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

#include "log_capture.h"

#include <gtest/gtest.h>

#include <cstring>
#include <memory>
#include <string>

#include "common/init_flags.h"
#include "os/log.h"

namespace {
[[maybe_unused]] const char* test_flags[] = {
    "INIT_logging_debug_enabled_for_all=true",
    nullptr,
};
}  // namespace

namespace bluetooth {
namespace testing {

class LogCaptureTest : public ::testing::Test {
 protected:
  void SetUp() override {}

  void TearDown() override {}
};

TEST_F(LogCaptureTest, typical) {
  std::unique_ptr<LogCapture> log_capture = std::make_unique<LogCapture>();

  LOG_ERROR("LOG_ERROR");
  LOG_WARN("LOG_WARN");
  LOG_INFO("LOG_INFO");
  LOG_DEBUG("LOG_DEBUG");
  LOG_VERBOSE("LOG_VERBOSE");

  ASSERT_TRUE(log_capture->Rewind()->Find("LOG_ERROR"));
  ASSERT_TRUE(log_capture->Rewind()->Find("LOG_WARN"));
  ASSERT_TRUE(log_capture->Rewind()->Find("LOG_INFO"));
  ASSERT_FALSE(log_capture->Rewind()->Find("LOG_DEBUG"));
  ASSERT_FALSE(log_capture->Rewind()->Find("LOG_VERBOSE"));
}

TEST_F(LogCaptureTest, with_logging_debug_enabled_for_all) {
  bluetooth::common::InitFlags::Load(test_flags);
  std::unique_ptr<LogCapture> log_capture = std::make_unique<LogCapture>();

  LOG_ERROR("LOG_ERROR");
  LOG_WARN("LOG_WARN");
  LOG_INFO("LOG_INFO");
  LOG_DEBUG("LOG_DEBUG");
  LOG_VERBOSE("LOG_VERBOSE");

  ASSERT_TRUE(log_capture->Rewind()->Find("LOG_ERROR"));
  ASSERT_TRUE(log_capture->Rewind()->Find("LOG_WARN"));
  ASSERT_TRUE(log_capture->Rewind()->Find("LOG_INFO"));
  ASSERT_TRUE(log_capture->Rewind()->Find("LOG_DEBUG"));
  ASSERT_TRUE(log_capture->Rewind()->Find("LOG_VERBOSE"));
}
}  // namespace testing
}  // namespace bluetooth
