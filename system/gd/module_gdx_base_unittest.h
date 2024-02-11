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

#include <gtest/gtest.h>

#include "main/shim/stack.h"
#include "module.h"
#include "os/thread.h"
#include "stack/include/main_thread.h"

constexpr int sync_timeout_in_ms = 3000;
constexpr char kTestStackThreadName[] = "test_stack_thread";

class MainThreadUnitTest : public ::testing::Test {
 protected:
  void SetUp() override {
    main_thread_start_up();
  }

  void TearDown() override {
    sync_main_handler();
    main_thread_shut_down();
  }

 private:
  void sync_main_handler() {
    std::promise promise = std::promise<void>();
    std::future future = promise.get_future();
    post_on_bt_main([&promise]() { promise.set_value(); });
    future.wait_for(std::chrono::milliseconds(sync_timeout_in_ms));
  }
};

class ModuleStackUnitTest : public MainThreadUnitTest {
 protected:
  void SetUp() override {
    MainThreadUnitTest::SetUp();
  }

  void TearDown() override {
    if (stack_started_) StopStack();
    MainThreadUnitTest::TearDown();
  }

  template <typename T>
  void AddModule() {
    modules_.add<T>();
  }

  virtual void StartStack() {
    if (stack_started_) return;
    stack_started_ = true;
    bluetooth::os::Thread* stack_thread =
        new bluetooth::os::Thread(kTestStackThreadName, bluetooth::os::Thread::Priority::NORMAL);
    bluetooth::shim::Stack::GetInstance()->StartModuleStack(&modules_, stack_thread);
  }

  virtual void StopStack() {
    if (!stack_started_) return;
    stack_started_ = false;
    bluetooth::shim::Stack::GetInstance()->Stop();
  }

 private:
  bluetooth::ModuleList modules_;
  bool stack_started_{false};
};
