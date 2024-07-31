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

#include <functional>
#include <memory>
#include <mutex>

#include "common/callback.h"
#include "os/handler.h"
#include "os/thread.h"
#include "os/utils.h"

namespace bluetooth {
namespace os {

// A single-shot alarm for reactor-based thread, implemented by Linux timerfd.
// When it's constructed, it will register a reactable on the specified thread; when it's destroyed,
// it will unregister itself from the thread.
// This alarm tries to piggyback on CPU wakeups triggered by others in order to save battery.
// If there were no CPU wakeups until the deadline, this alarm wakes up CPU and runs the task.
class ConditionalWakeupAlarm {
public:
  // Create and register a single-shot alarm on a given handler
  explicit ConditionalWakeupAlarm(Handler* handler);

  ConditionalWakeupAlarm(const ConditionalWakeupAlarm&) = delete;
  ConditionalWakeupAlarm& operator=(const ConditionalWakeupAlarm&) = delete;

  // Unregister this alarm from the thread and release resource
  ~ConditionalWakeupAlarm();

  // Run the task after delay when CPU is awake.
  // If the CPU is awake after the delay, it runs the given task.
  // If the CPU is not awake from delay to deadline, it wakes up the CPU and runs the task.
  void Schedule(common::OnceClosure task, std::chrono::milliseconds delay,
                std::chrono::milliseconds deadline);

  // Cancel the alarm. No-op if it's not armed.
  void Cancel();

private:
  common::OnceClosure task_;
  Handler* handler_;
  mutable std::mutex mutex_;

  int fd_non_wake_ = 0;
  Reactor::Reactable* token_non_wake_;
  void on_fire_non_wake();

  int fd_wake_ = 0;
  Reactor::Reactable* token_wake_;
  void on_fire_wake();
};

}  // namespace os
}  // namespace bluetooth
