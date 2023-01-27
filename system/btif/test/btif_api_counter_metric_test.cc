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

#include "btif/include/btif_api_counter_metric.h"

#include <gtest/gtest.h>

#include "btif/include/btif_common.h"

class BtifApiMetricTest : public ::testing::Test {
 protected:
  void SetUp() override { api_counter_metric_init(); }
  void TearDown() override {}
};

TEST_F(BtifApiMetricTest, api_counter_metric_entry_inc) {
  api_counter_metric_entry_inc("TestApi");
  auto counters = api_counter_metric_entry_dump();
  ASSERT_EQ(0UL, counters.size());

  api_counter_metric_enable();
  api_counter_metric_entry_inc("TestApi");
  counters = api_counter_metric_entry_dump();
  ASSERT_EQ(1UL, counters.size());

  api_counter_metric_disable();
  api_counter_metric_entry_inc("TestApi");
  counters = api_counter_metric_entry_dump();
  ASSERT_EQ(1UL, counters.size());
}

class BtifApiMetricEnabledTest : public BtifApiMetricTest {
 protected:
  void SetUp() override {
    BtifApiMetricTest::SetUp();
    api_counter_metric_enable();
  }

  void TearDown() override {
    api_counter_metric_disable();
    BtifApiMetricTest::TearDown();
  }
};

TEST_F(BtifApiMetricEnabledTest, api_counter_metric_entry_inc) {
  api_counter_metric_entry_inc("eee");
  api_counter_metric_entry_inc("ccc");
  api_counter_metric_entry_inc("aaa");
  api_counter_metric_entry_inc("bbb");
  api_counter_metric_entry_inc("ddd");

  auto counters = api_counter_metric_entry_dump();
  ASSERT_EQ(5UL, counters.size());

  ASSERT_EQ(1UL, counters[0].second);
  ASSERT_STREQ("aaa", counters[0].first.data());
}

TEST_F(BtifApiMetricEnabledTest, api_counter_metric_callback_inc) {
  api_counter_metric_callback_inc("bbb");
  api_counter_metric_callback_inc("ccc");
  api_counter_metric_callback_inc("ddd");
  api_counter_metric_callback_inc("ggg");
  api_counter_metric_callback_inc("eee");
  api_counter_metric_callback_inc("aaa");
  api_counter_metric_callback_inc("fff");

  auto counters = api_counter_metric_callback_dump();
  ASSERT_EQ(7UL, counters.size());

  ASSERT_EQ(1UL, counters[0].second);
  ASSERT_STREQ("aaa", counters[0].first.data());
}

struct {
  void (*test00)();
} test_callback_;

TEST_F(BtifApiMetricEnabledTest, HAL_CBACK__one) {
  test_callback_.test00 = []() {};

  HAL_CBACK(&test_callback_, test00);

  auto counters = api_counter_metric_callback_dump();
  ASSERT_EQ(1UL, counters.size());

  ASSERT_EQ(1UL, counters[0].second);
  ASSERT_STREQ("test00", counters[0].first.data());

  test_callback_.test00 = {};
}

TEST_F(BtifApiMetricEnabledTest, HAL_CBACK__many) {
  const size_t kMany = 1000;
  test_callback_.test00 = []() {};

  for (size_t i = 0; i < kMany; i++) {
    HAL_CBACK(&test_callback_, test00);
  }

  auto counters = api_counter_metric_callback_dump();
  ASSERT_EQ(1UL, counters.size());

  ASSERT_EQ(kMany, counters[0].second);
  ASSERT_STREQ("test00", counters[0].first.data());

  test_callback_.test00 = {};
}
