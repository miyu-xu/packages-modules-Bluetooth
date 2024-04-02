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

#include "os/alarm.h"

#include <cctype>
#include <chrono>
#include <future>
#include <memory>

#include "common/bind.h"
#include "gtest/gtest.h"


namespace bluetooth::common {

struct IsSpace {
  bool operator()(std::string::value_type v) {
    return isspace(static_cast<int>(v));
  }
};

std::string StringTrim(std::string str) {
  str.erase(str.begin(), std::find_if_not(str.begin(), str.end(), IsSpace{}));
  str.erase(std::find_if_not(str.rbegin(), str.rend(), IsSpace{}).base(), str.end());
  return str;
}
}  // namespace bluetooth::common

namespace bluetooth::os {

using common::BindOnce;
using std::chrono::milliseconds;
using std::chrono::seconds;

class AlarmOnTimerFdTest : public ::testing::Test {
 protected:
  void SetUp() override {
    thread_ = new Thread("test_thread", Thread::Priority::NORMAL);
    handler_ = new Handler(thread_);
    alarm_ = std::make_shared<Alarm>(handler_);
  }

  void TearDown() override {
    alarm_.reset();
    handler_->Clear();
    delete handler_;
    delete thread_;
  }

  std::shared_ptr<Alarm> get_new_alarm() {
    return std::make_shared<Alarm>(handler_);
  }

  std::shared_ptr<Alarm> alarm_;

 private:
  Handler* handler_;
  Thread* thread_;
};

TEST_F(AlarmOnTimerFdTest, cancel_while_not_armed) {
  alarm_->Cancel();
}

TEST_F(AlarmOnTimerFdTest, schedule) {
  std::promise<void> promise;
  auto future = promise.get_future();
  int delay_ms = 10;
  alarm_->Schedule(
      BindOnce(&std::promise<void>::set_value, common::Unretained(&promise)),
      milliseconds(delay_ms));
  ASSERT_EQ(std::future_status::ready, future.wait_for(seconds(1)));
}

TEST_F(AlarmOnTimerFdTest, cancel_alarm) {
  alarm_->Schedule(BindOnce([]() { FAIL(); }), milliseconds(3));
  alarm_->Cancel();
  std::this_thread::sleep_for(milliseconds(5));
}

TEST_F(AlarmOnTimerFdTest, cancel_alarm_from_callback) {
  alarm_->Schedule(BindOnce(&Alarm::Cancel, alarm_), milliseconds(1));
  std::this_thread::sleep_for(milliseconds(5));
}

TEST_F(AlarmOnTimerFdTest, schedule_while_alarm_armed) {
  alarm_->Schedule(BindOnce([]() { FAIL(); }), milliseconds(1));
  std::promise<void> promise;
  auto future = promise.get_future();
  alarm_->Schedule(
      BindOnce(&std::promise<void>::set_value, common::Unretained(&promise)), milliseconds(10));
  future.get();
}

TEST_F(AlarmOnTimerFdTest, delete_while_alarm_armed) {
  alarm_->Schedule(BindOnce([]() { FAIL(); }), milliseconds(1));
  alarm_.reset();
  std::this_thread::sleep_for(milliseconds(10));
}

class TwoAlarmOnTimerFdTest : public AlarmOnTimerFdTest {
 protected:
  void SetUp() override {
    AlarmOnTimerFdTest::SetUp();
    alarm2 = get_new_alarm();
  }

  void TearDown() override {
    alarm2.reset();
    AlarmOnTimerFdTest::TearDown();
  }

  std::shared_ptr<Alarm> alarm2;
};

TEST_F(TwoAlarmOnTimerFdTest, schedule_from_alarm) {
  auto promise = std::make_unique<std::promise<void>>();
  auto future = promise->get_future();
  alarm_->Schedule(
      BindOnce(
          [](std::shared_ptr<Alarm> alarm2, std::unique_ptr<std::promise<void>> promise) {
            promise->set_value();
            alarm2->Schedule(
                BindOnce(&std::promise<void>::set_value, std::move(promise)), milliseconds(10));
          },
          alarm2,
          std::move(promise)),
      milliseconds(10));
  EXPECT_EQ(std::future_status::ready, future.wait_for(seconds(1)));
}

}  // namespace bluetooth::os
