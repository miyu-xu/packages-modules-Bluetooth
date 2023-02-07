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

#include "btif/include/btif_api_access_metric.h"

#include <gtest/gtest.h>

#include "btif/include/btif_common.h"

class BtifApiMetricTest : public ::testing::Test {
 protected:
  void SetUp() override { api_access_metric_reset(); }

  void TearDown() override { api_access_metric_reset(); }
};

TEST_F(BtifApiMetricTest, api_access_metric_enable_disable) {
  api_access_metric_add_api("TestApi");
  api_access_metric_add_cb("TestCb");
  auto counters = api_access_metric_data();
  ASSERT_EQ(0UL, counters.size());

  api_access_metric_enable();
  api_access_metric_add_api("TestApi");
  api_access_metric_add_cb("TestCb");
  counters = api_access_metric_data();
  ASSERT_EQ(2UL, counters.size());

  api_access_metric_disable();
  api_access_metric_add_api("TestApi");
  counters = api_access_metric_data();
  ASSERT_EQ(2UL, counters.size());
}

class BtifApiMetricEnabledTest : public BtifApiMetricTest {
 protected:
  void SetUp() override {
    BtifApiMetricTest::SetUp();
    api_access_metric_enable();
  }

  void TearDown() override {
    api_access_metric_disable();
    BtifApiMetricTest::TearDown();
  }
};

TEST_F(BtifApiMetricEnabledTest, api_access_metric_add) {
  api_access_metric_add_api("aaa");
  api_access_metric_add_cb("bbb");
  api_access_metric_add_api("ccc");
  api_access_metric_add_cb("ddd");
  api_access_metric_add_api("eee");
  api_access_metric_add_cb("fff");
  api_access_metric_add_api("ggg");

  auto counters = api_access_metric_data();
  ASSERT_EQ(7UL, counters.size());

  ASSERT_EQ(1UL, counters[0].entry.count);
  ASSERT_STREQ("aaa", counters[0].entry.name.data());
}
