/*
 * Copyright 2020 The Android Open Source Project
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

#define LOG_TAG "JournalTest"

#include "common/journal.h"

#include <bluetooth/log.h>
#include <gmock/gmock.h>
#include <gtest/gtest.h>
#include <unistd.h>

#include <limits>
#include <string>

#include "ble_address_with_type.h"

namespace testing {
constexpr char kTag[] = "JournalTest";
constexpr uint8_t kSize = 2;
constexpr size_t kMaxLineLength = 512;
constexpr size_t kMaxBufferLength = kMaxLineLength * kSize;

constexpr tAclLinkSpec kLinkSpec = {
        .addrt = {.type = kBleAddressPublicIdentity, .bda = RawAddress()},
        .transport = BT_TRANSPORT_LE};

TEST(JournalTest, simple) {
  bluetooth::common::Journal journal(kTag, kSize);

  for (int i = 0; i < kSize; ++i) {
    journal.record(kLinkSpec, base::StringPrintf("Event %d", i),
                   base::StringPrintf("Details %d", i));
  }

  int fd[2];
  pipe(fd);
  journal.dump(fd[1]);
  char buf[kMaxBufferLength] = {};
  read(fd[0], buf, sizeof(buf));
  std::string dump_string(buf);
  std::istringstream iss(dump_string);
  std::string line;
  uint8_t i = 0;
  while (getline(iss, line, '\n')) {
    bluetooth::log::info("Line: {}", line);
    EXPECT_THAT(line, HasSubstr(kLinkSpec.ToRedactedStringForLogging()));
    EXPECT_THAT(line, HasSubstr(base::StringPrintf("Event %d", i)));
    EXPECT_THAT(line, HasSubstr(base::StringPrintf("Details %d", i)));
    i++;
  }
  EXPECT_EQ(i, kSize);
}

TEST(JournalTest, overflow) {
  bluetooth::common::Journal journal(kTag, kSize);

  for (int i = 0; i < kSize + 1; ++i) {
    journal.record(kLinkSpec, base::StringPrintf("Event %d", i),
                   base::StringPrintf("Details %d", i));
  }

  int fd[2];
  pipe(fd);
  journal.dump(fd[1]);

  char buf[kMaxBufferLength] = {};
  read(fd[0], buf, sizeof(buf));
  std::string dump_string(buf);
  std::istringstream iss(dump_string);
  std::string line;
  uint8_t i = 1; // Skip the first line
  while (std::getline(iss, line, '\n')) {
    bluetooth::log::info("Line: {}", line);
    EXPECT_THAT(line, HasSubstr(kLinkSpec.ToRedactedStringForLogging()));
    EXPECT_THAT(line, HasSubstr(base::StringPrintf("Event %d", i)));
    EXPECT_THAT(line, HasSubstr(base::StringPrintf("Details %d", i)));
    i++;
  }
  EXPECT_EQ(i, kSize + 1);
}

}  // namespace testing