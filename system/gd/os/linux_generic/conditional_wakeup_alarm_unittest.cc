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

#include "os/conditional_wakeup_alarm.h"

#include <bluetooth/log.h>

#include <future>
#include <memory>

#include "common/bind.h"
#include "gtest/gtest.h"
#include "os/fake_timer/fake_timerfd.h"

namespace bluetooth {
namespace os {
namespace {

using common::BindOnce;
using fake_timer::fake_timerfd_advance;
using fake_timer::fake_timerfd_reset;
using std::chrono::milliseconds;
using std::chrono::seconds;

static constexpr milliseconds kShortWait = milliseconds(5);
static constexpr milliseconds kForever = milliseconds(1000);

static constexpr milliseconds kDelay = milliseconds(10);
static constexpr milliseconds kDeadline = milliseconds(50);

class ConditionalWakeupAlarmTest : public ::testing::Test {
protected:
  void SetUp() override {
    thread_ = new Thread("test_thread", Thread::Priority::NORMAL);
    handler_ = new Handler(thread_);
    alarm_ = std::make_shared<ConditionalWakeupAlarm>(handler_);
  }

  void TearDown() override {
    alarm_.reset();
    handler_->Clear();
    handler_->WaitUntilStopped(kForever);
    delete handler_;
    thread_->Stop();
    delete thread_;
    fake_timerfd_reset();
  }

  void fake_timer_advance(uint64_t ms) {
    handler_->Post(common::BindOnce(fake_timerfd_advance, ms));
  }

  std::shared_ptr<ConditionalWakeupAlarm> get_new_alarm() {
    return std::make_shared<ConditionalWakeupAlarm>(handler_);
  }

  std::shared_ptr<ConditionalWakeupAlarm> alarm_;

private:
  Handler* handler_;
  Thread* thread_;
};

TEST_F(ConditionalWakeupAlarmTest, cancel_while_not_armed) { alarm_->Cancel(); }

TEST_F(ConditionalWakeupAlarmTest, alarm_fired_after_delay) {
  std::promise<void> promise;
  auto future = promise.get_future();
  alarm_->Schedule(BindOnce(&std::promise<void>::set_value, common::Unretained(&promise)), kDelay,
                   kDeadline);
  fake_timer_advance(kDelay.count());
  future.get();
  ASSERT_FALSE(future.valid());
}

TEST_F(ConditionalWakeupAlarmTest, alarm_fired_after_deadline_only_once) {
  std::promise<void> promise;
  auto future = promise.get_future();
  alarm_->Schedule(BindOnce(&std::promise<void>::set_value, common::Unretained(&promise)), kDelay,
                   kDeadline);
  fake_timer_advance(kDeadline.count());
  future.get();
  ASSERT_FALSE(future.valid());
  // Check whether there is a duplicate call.
  std::this_thread::sleep_for(kShortWait);
}

TEST_F(ConditionalWakeupAlarmTest, cancel_alarm) {
  alarm_->Schedule(BindOnce([]() { FAIL() << "Should not happen"; }), kDelay, kDeadline);
  alarm_->Cancel();
  fake_timer_advance(kDeadline.count());
  // Check the alarm does not ring.
  std::this_thread::sleep_for(kShortWait);
}

TEST_F(ConditionalWakeupAlarmTest, cancel_alarm_from_callback) {
  std::promise<void> promise;
  auto future = promise.get_future();
  alarm_->Schedule(
          BindOnce(
                  [](std::shared_ptr<ConditionalWakeupAlarm> alarm, std::promise<void> promise) {
                    alarm->Cancel();
                    alarm.reset();  // Allow alarm to be freed by Teardown
                    promise.set_value();
                  },
                  alarm_, std::move(promise)),
          kDelay, kDeadline);
  fake_timer_advance(kDeadline.count());
  future.get();
  ASSERT_FALSE(future.valid());
}

TEST_F(ConditionalWakeupAlarmTest, schedule_while_alarm_armed) {
  alarm_->Schedule(BindOnce([]() { FAIL() << "Should not happen"; }), milliseconds(1), kDeadline);
  std::promise<void> promise;
  auto future = promise.get_future();
  alarm_->Schedule(BindOnce(&std::promise<void>::set_value, common::Unretained(&promise)), kDelay,
                   kDeadline);
  fake_timer_advance(kDeadline.count());
  future.get();
  ASSERT_FALSE(future.valid());
}

TEST_F(ConditionalWakeupAlarmTest, delete_while_alarm_armed) {
  alarm_->Schedule(BindOnce([]() { FAIL() << "Should not happen"; }), kDelay, kDeadline);
  alarm_.reset();
  fake_timer_advance(kDeadline.count());
  std::this_thread::sleep_for(kShortWait);
}

class TwoConditionalWakeupAlarmTest : public ConditionalWakeupAlarmTest {
protected:
  void SetUp() override {
    ConditionalWakeupAlarmTest::SetUp();
    alarm2_ = get_new_alarm();
  }

  void TearDown() override {
    alarm2_.reset();
    ConditionalWakeupAlarmTest::TearDown();
  }

  std::shared_ptr<ConditionalWakeupAlarm> alarm2_;
};

TEST_F(TwoConditionalWakeupAlarmTest, schedule_from_alarm_long) {
  auto promise = std::make_unique<std::promise<void>>();
  auto future = promise->get_future();
  auto promise2 = std::make_unique<std::promise<void>>();
  auto future2 = promise2->get_future();
  alarm_->Schedule(BindOnce(
                           [](std::shared_ptr<ConditionalWakeupAlarm> alarm2,
                              std::unique_ptr<std::promise<void>> promise,
                              std::unique_ptr<std::promise<void>> promise2) {
                             promise->set_value();
                             alarm2->Schedule(
                                     BindOnce(&std::promise<void>::set_value, std::move(promise2)),
                                     kDelay, kDeadline);
                           },
                           alarm2_, std::move(promise), std::move(promise2)),
                   kDelay, kDeadline);

  fake_timer_advance(kDelay.count());
  future.get();
  ASSERT_FALSE(future.valid());

  fake_timer_advance(kDelay.count());
  future2.get();
  ASSERT_FALSE(future2.valid());
}

}  // namespace
}  // namespace os
}  // namespace bluetooth
