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

#include "common/sequential_id_generator.h"

#include <gtest/gtest.h>

#include <set>
#include <thread>
#include <vector>

using bluetooth::common::SequentialIDGenerator;

// Test Fixture for Parameterized Testing
class SequentialIDGeneratorTest : public ::testing::Test {};

// Test Case 1: Uniqueness
TEST_F(SequentialIDGeneratorTest, UniqueIDs) {
  SequentialIDGenerator<uint32_t> id_generator;

  std::set<uint32_t> generated_ids;
  const int num_ids = 1000;

  for (int i = 0; i < num_ids; ++i) {
    uint32_t id = id_generator.getUniqueID();
    EXPECT_TRUE(generated_ids.insert(id).second); // Ensure uniqueness
  }
}

// Test Case 2: Thread Safety
TEST_F(SequentialIDGeneratorTest, ThreadSafety) {
  SequentialIDGenerator<uint32_t> id_generator;
  std::set<uint32_t> generated_ids;
  std::mutex generated_ids_mutex;
  const int num_threads = 10;

  std::vector<std::thread> threads;
  for (int i = 0; i < num_threads; ++i) {
    threads.emplace_back([&id_generator, &generated_ids, &generated_ids_mutex]() {
      for (int j = 0; j < 1000; ++j) {
        uint32_t id = id_generator.getUniqueID();
        std::lock_guard<std::mutex> lock(generated_ids_mutex); // Protect the set
        EXPECT_TRUE(generated_ids.insert(id).second);
      }
    });
  }

  for (auto& thread : threads) {
    thread.join();
  }
}

// Test Case 3: Overflow
TEST_F(SequentialIDGeneratorTest, OverflowTest) {
  SequentialIDGenerator<uint8_t> id_generator;

  std::set<uint8_t> generated_ids;
  const int num_ids = 256;

  for (int i = 1; i <= num_ids; ++i) {
    uint8_t id = id_generator.getUniqueID();
    if (i != num_ids) {
      EXPECT_TRUE(generated_ids.insert(id).second); // Ensure uniqueness
    } else {
      // Overflow occurred and reset to 1.
      EXPECT_EQ(1, id);
    }
  }
}
