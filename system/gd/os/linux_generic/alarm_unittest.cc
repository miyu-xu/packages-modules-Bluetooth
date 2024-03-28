/*
 * Copyright 2019 The Android Open Source Project
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

#include "os/alarm.h"

#include <cstddef>
#include <future>

#include "common/bind.h"
#include "gtest/gtest.h"
#include "os/fake_timer/fake_timerfd.h"

namespace bluetooth {
namespace os {
namespace {

using common::BindOnce;
using fake_timer::fake_timerfd_advance;
using fake_timer::fake_timerfd_reset;

class AlarmTest : public ::testing::Test {
 protected:
  void SetUp() override {
    thread_ = new Thread("test_thread", Thread::Priority::NORMAL);
    handler_ = new Handler(thread_);
    alarm_ = new Alarm(handler_);
  }

  void TearDown() override {
    delete alarm_;
    handler_->Clear();
    delete handler_;
    delete thread_;
    fake_timerfd_reset();
  }

  void fake_timer_advance(uint64_t ms) {
    handler_->Post(common::BindOnce(fake_timerfd_advance, ms));
  }

  Alarm* get_new_alarm() {
    return new Alarm(handler_);
  }

  Alarm* alarm_;

 private:
  Handler* handler_;
  Thread* thread_;
};

TEST_F(AlarmTest, cancel_while_not_armed) {
  alarm_->Cancel();
}

TEST_F(AlarmTest, schedule) {
  std::promise<void> promise;
  auto future = promise.get_future();
  int delay_ms = 10;
  alarm_->Schedule(
      BindOnce(&std::promise<void>::set_value, common::Unretained(&promise)), std::chrono::milliseconds(delay_ms));
  fake_timer_advance(10);
  future.get();
  ASSERT_FALSE(future.valid());
}

TEST_F(AlarmTest, cancel_alarm) {
  alarm_->Schedule(BindOnce([]() { ASSERT_TRUE(false) << "Should not happen"; }), std::chrono::milliseconds(3));
  alarm_->Cancel();
  std::this_thread::sleep_for(std::chrono::milliseconds(5));
}

TEST_F(AlarmTest, cancel_alarm_from_callback) {
  alarm_->Schedule(BindOnce(&Alarm::Cancel, common::Unretained(alarm_)), std::chrono::milliseconds(1));
  std::this_thread::sleep_for(std::chrono::milliseconds(5));
}

TEST_F(AlarmTest, schedule_while_alarm_armed) {
  alarm_->Schedule(BindOnce([]() { ASSERT_TRUE(false) << "Should not happen"; }), std::chrono::milliseconds(1));
  std::promise<void> promise;
  auto future = promise.get_future();
  alarm_->Schedule(
      BindOnce(&std::promise<void>::set_value, common::Unretained(&promise)), std::chrono::milliseconds(10));
  fake_timer_advance(10);
  future.get();
}

TEST_F(AlarmTest, delete_while_alarm_armed) {
  alarm_->Schedule(BindOnce([]() { ASSERT_TRUE(false) << "Should not happen"; }), std::chrono::milliseconds(1));
  delete alarm_;
  alarm_ = nullptr;
  std::this_thread::sleep_for(std::chrono::milliseconds(10));
}

class TwoAlarmTest : public AlarmTest {
 protected:
  void SetUp() override {
    AlarmTest::SetUp();
    alarm2 = get_new_alarm();
  }

  void TearDown() override {
    delete alarm2;
    AlarmTest::TearDown();
  }

  Alarm* alarm2;
};

TEST_F(TwoAlarmTest, schedule_from_alarm_long) {
  std::promise<void> promise;
  auto future = promise.get_future();
  std::promise<void> promise2;
  auto future2 = promise2.get_future();
  alarm_->Schedule(
      BindOnce(
          [](Alarm* alarm2, std::promise<void>* promise, std::promise<void>* promise2) {
            promise->set_value();
            if (promise == nullptr) {
              FAIL();
            }
            alarm2->Schedule(
                BindOnce(&std::promise<void>::set_value, common::Unretained(promise2)),
                std::chrono::milliseconds(10));
          },
          common::Unretained(alarm2),
          common::Unretained(&promise),
          common::Unretained(&promise2)),
      std::chrono::milliseconds(1));
  fake_timer_advance(10);
  EXPECT_EQ(std::future_status::ready, future.wait_for(std::chrono::milliseconds(20)));
  fake_timer_advance(10);
  EXPECT_EQ(std::future_status::ready, future2.wait_for(std::chrono::milliseconds(20)));
}

}  // namespace
}  // namespace os
}  // namespace bluetooth
