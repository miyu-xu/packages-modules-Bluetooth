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

#pragma once

#include <atomic>
#include <mutex>

namespace bluetooth {
namespace common {
/**
 * @class SequentialIDGenerator
 * @brief A thread-safe generator for sequential unique IDs.
 *
 * This class provides a simple mechanism for generating unique IDs that
 * increment sequentially. It is designed to be thread-safe, ensuring that no
 * duplicate IDs are produced even in a multi-threaded environment.
 *
 * @tparam T The integral type to use for the generated IDs (e.g., uint32_t,
 * uint64_t).
 */
template <typename T>
class SequentialIDGenerator {
public:
  SequentialIDGenerator() : next_id_(1) {}

  /**
   * @brief Gets the next unique ID in the sequence.
   *
   * This function atomically increments the internal ID counter and returns the
   * new value. It handles potential counter overflows to ensure ID uniqueness.
   *
   * @return The next unique ID.
   */
  T getUniqueID() {
    // Atomic Fetch-Add: Fastest for most cases
    T id = next_id_.fetch_add(1, std::memory_order_relaxed);
    // Handle Potential Overflows
    if (id == 0) { // Wraparound
      std::lock_guard<std::mutex> lock(mutex_);
      if (next_id_ == 0) {
        next_id_ = 1; // Reset (consider logging this)
      }
      id = next_id_.fetch_add(1, std::memory_order_relaxed);
    }
    return id;
  }

private:
  std::atomic<T> next_id_;
  std::mutex mutex_; // Protects overflow handling
};

} // namespace common
} // namespace bluetooth
