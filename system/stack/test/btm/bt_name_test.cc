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

#include "stack/include/bt_name.h"

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include "os/log.h"

void verify_integrity(const BD_NAME bd_name) {
  for (int i = 0; i < BD_NAME_LEN; i++) ASSERT_EQ((uint8_t)i, bd_name[i]);
}

inline void verify_range(const uint8_t* const p, uint8_t val, int start,
                         int end) {
  for (int i = start; i < end; i++) ASSERT_EQ(val, p[i]);
}

TEST(BtNameTest, simple) {
  uint8_t mem[BD_NAME_LEN * 2] = {};

  for (int offset = 0; offset < BD_NAME_LEN; offset++) {
    memset(mem, 0xff, sizeof(mem));
    verify_range(mem, 0xff, 0, BD_NAME_LEN * 2);
    for (int i = 0; i < BD_NAME_LEN; i++) mem[i + offset] = (uint8_t)i;

    uint8_t* p = &mem[offset];

    BD_NAME_IN_STREAM bdn = bd_name_in_stream(*p);

    verify_integrity(bdn);
    verify_range(mem, 0xff, 0, offset);
    verify_range(mem, 0xff, offset + BD_NAME_LEN + 1, BD_NAME_LEN * 2);
  }
}
