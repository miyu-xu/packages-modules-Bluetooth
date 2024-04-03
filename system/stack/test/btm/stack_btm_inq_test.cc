/*
 * Copyright 2024 The Android Open Source Project
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

#include <base/strings/stringprintf.h>
#include <gtest/gtest.h>
#include <stdlib.h>

#include "stack/btm/btm_int_types.h"
#include "stack/test/btm/btm_test_fixtures.h"
#include "test/fake/fake_looper.h"
#include "test/mock/mock_osi_allocator.h"
#include "test/mock/mock_osi_thread.h"

extern tBTM_CB btm_cb;

namespace {}  // namespace

class BtmInqTest : public BtmWithMocksTest {
 protected:
  void SetUp() override {}

  void TearDown() override {}
};

TEST_F(BtmInqTest, btm_process_remote_name) {}
