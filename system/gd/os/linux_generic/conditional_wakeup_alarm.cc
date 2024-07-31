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
#include <sys/timerfd.h>
#include <unistd.h>

#include <cstring>

#include "common/bind.h"
#include "os/linux_generic/linux.h"
#include "os/log.h"
#include "os/utils.h"

#ifdef __ANDROID__
#define ALARM_CLOCK CLOCK_BOOTTIME_ALARM
#else
#define ALARM_CLOCK CLOCK_BOOTTIME
#endif

namespace bluetooth {
namespace os {
using common::Closure;
using common::OnceClosure;

ConditionalWakeupAlarm::ConditionalWakeupAlarm(Handler* handler) : handler_(handler) {
  fd_non_wake_ = TIMERFD_CREATE(CLOCK_BOOTTIME, TFD_NONBLOCK);
  log::assert_that(fd_non_wake_ != -1, "cannot create timerfd: {}", strerror(errno));
  fd_wake_ = TIMERFD_CREATE(ALARM_CLOCK, TFD_NONBLOCK);
  log::assert_that(fd_wake_ != -1, "cannot create timerfd: {}", strerror(errno));

  token_non_wake_ = handler_->thread_->GetReactor()->Register(
          fd_non_wake_,
          common::Bind(&ConditionalWakeupAlarm::on_fire_non_wake, common::Unretained(this)),
          Closure());
  token_wake_ = handler_->thread_->GetReactor()->Register(
          fd_wake_, common::Bind(&ConditionalWakeupAlarm::on_fire_wake, common::Unretained(this)),
          Closure());
}

ConditionalWakeupAlarm::~ConditionalWakeupAlarm() {
  handler_->thread_->GetReactor()->Unregister(token_non_wake_);
  handler_->thread_->GetReactor()->Unregister(token_wake_);

  int close_status;
  RUN_NO_INTR(close_status = TIMERFD_CLOSE(fd_non_wake_));
  log::assert_that(close_status != -1, "assert failed: close_status != -1");

  RUN_NO_INTR(close_status = TIMERFD_CLOSE(fd_wake_));
  log::assert_that(close_status != -1, "assert failed: close_status != -1");
}

void ConditionalWakeupAlarm::Schedule(OnceClosure task, std::chrono::milliseconds delay,
                                      std::chrono::milliseconds deadline) {
  std::lock_guard<std::mutex> lock(mutex_);
  long delay_ms = delay.count();
  itimerspec timer_itimerspec_delay{{/* interval for periodic timer */},
                                    {delay_ms / 1000, delay_ms % 1000 * 1000000}};
  int result = TIMERFD_SETTIME(fd_non_wake_, 0, &timer_itimerspec_delay, nullptr);
  log::assert_that(result == 0, "assert failed: result == 0");

  long deadline_ms = deadline.count();
  itimerspec timer_itimerspec_deadline{{/* interval for periodic timer */},
                                       {deadline_ms / 1000, deadline_ms % 1000 * 1000000}};
  result = TIMERFD_SETTIME(fd_wake_, 0, &timer_itimerspec_deadline, nullptr);
  log::assert_that(result == 0, "assert failed: result == 0");

  task_ = std::move(task);
}

void ConditionalWakeupAlarm::Cancel() {
  std::lock_guard<std::mutex> lock(mutex_);
  itimerspec disarm_itimerspec{/* disarm timer */};
  int result = TIMERFD_SETTIME(fd_non_wake_, 0, &disarm_itimerspec, nullptr);
  log::assert_that(result == 0, "assert failed: result == 0");
  result = TIMERFD_SETTIME(fd_wake_, 0, &disarm_itimerspec, nullptr);
  log::assert_that(result == 0, "assert failed: result == 0");
}

void ConditionalWakeupAlarm::on_fire_non_wake() {
  log::info("");
  std::unique_lock<std::mutex> lock(mutex_);
  uint64_t times_invoked;
  auto bytes_read = read(fd_non_wake_, &times_invoked, sizeof(uint64_t));
  lock.unlock();

  if (bytes_read == -1) {
    log::info("No data to read.");
    if (errno == EAGAIN) {
      log::info("alarm is already canceled or rescheduled.");
    }
    return;
  }

  log::assert_that(bytes_read == static_cast<ssize_t>(sizeof(uint64_t)),
                   "assert failed: bytes_read == static_cast<ssize_t>(sizeof(uint64_t))");
  log::assert_that(times_invoked == static_cast<uint64_t>(1),
                   "Invoked number of times:{} fd_non_wake_:{}", times_invoked, fd_non_wake_);

  log::info("Before running the task, resetting both alarms.");
  Cancel();

  log::info("Running scheduled task.");
  std::move(task_).Run();
}

void ConditionalWakeupAlarm::on_fire_wake() { log::info(""); }

}  // namespace os
}  // namespace bluetooth
