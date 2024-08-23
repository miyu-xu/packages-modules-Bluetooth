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

#include "os/nonwake_alarm.h"

#include <bluetooth/log.h>
#include <sys/timerfd.h>
#include <unistd.h>

#include <cstring>

#include "common/bind.h"
#include "os/linux_generic/linux.h"
#include "os/log.h"
#include "os/utils.h"

namespace bluetooth {
namespace os {
using common::Closure;
using common::OnceClosure;

NonwakeAlarm::NonwakeAlarm(Handler* handler) : handler_(handler) {
  fd_ = TIMERFD_CREATE(CLOCK_BOOTTIME, TFD_NONBLOCK);

  log::assert_that(fd_ != -1, "cannot create timerfd: {}", strerror(errno));

  token_ = handler_->thread_->GetReactor()->Register(
          fd_, common::Bind(&NonwakeAlarm::on_fire, common::Unretained(this)), Closure());
}

NonwakeAlarm::~NonwakeAlarm() {
  handler_->thread_->GetReactor()->Unregister(token_);

  int close_status;
  RUN_NO_INTR(close_status = TIMERFD_CLOSE(fd_));
  log::assert_that(close_status != -1, "assert failed: close_status != -1");
}

void NonwakeAlarm::Schedule(OnceClosure task, std::chrono::milliseconds delay) {
  std::lock_guard<std::mutex> lock(mutex_);
  long delay_ms = delay.count();
  itimerspec timer_itimerspec{{/* interval for periodic timer */},
                              {delay_ms / 1000, delay_ms % 1000 * 1000000}};
  int result = TIMERFD_SETTIME(fd_, 0, &timer_itimerspec, nullptr);
  log::assert_that(result == 0, "assert failed: result == 0");

  task_ = std::move(task);
}

void NonwakeAlarm::Cancel() {
  std::lock_guard<std::mutex> lock(mutex_);
  itimerspec disarm_itimerspec{/* disarm timer */};
  int result = TIMERFD_SETTIME(fd_, 0, &disarm_itimerspec, nullptr);
  log::assert_that(result == 0, "assert failed: result == 0");
}

void NonwakeAlarm::on_fire() {
  std::unique_lock<std::mutex> lock(mutex_);
  auto task = std::move(task_);
  uint64_t times_invoked;
  auto bytes_read = read(fd_, &times_invoked, sizeof(uint64_t));
  lock.unlock();

  if (bytes_read == -1) {
    log::info("No data to read.");
    if (errno == EAGAIN || errno == EWOULDBLOCK) {
      log::info("Alarm is already canceled or rescheduled.");
      return;
    }
  }

  log::assert_that(bytes_read == static_cast<ssize_t>(sizeof(uint64_t)),
                   "assert failed: bytes_read == static_cast<ssize_t>(sizeof(uint64_t))");
  log::assert_that(times_invoked == static_cast<uint64_t>(1), "Invoked number of times:{} fd:{}",
                   (unsigned long)times_invoked, fd_);
  std::move(task).Run();
}

}  // namespace os
}  // namespace bluetooth
