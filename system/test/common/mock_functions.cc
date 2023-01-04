/*
 * Copyright 2021 The Android Open Source Project
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

#include <map>

#include "osi/include/log.h"
#include "test/common/mock_functions.h"

std::map<std::string, int>& increment_mock_function_call_count_get() {
  static std::map<std::string, int> mock_function_count_map;
  return mock_function_count_map;
}

void increment_mock_function_call_count(const char* fn) {
  increment_mock_function_call_count_get()[fn]++;
}

void reset_mock_function_count_map() {
  increment_mock_function_call_count_get().clear();
}

void dump_mock_function_count_map() {
  LOG_INFO("Mock function count map size:%zu",
           increment_mock_function_call_count_get().size());

  for (auto it : increment_mock_function_call_count_get()) {
    LOG_INFO("function:%s: call_count:%d", it.first.c_str(), it.second);
  }
}
