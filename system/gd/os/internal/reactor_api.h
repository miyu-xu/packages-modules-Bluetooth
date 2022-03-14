/*
 * Copyright 2022 The Android Open Source Project
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

#include <base/callback.h>

#include <atomic>
#include <future>
#include <mutex>

#include "os/reactor.h"

namespace bluetooth {
namespace os {

class ReactorApi {
 public:
  virtual ~ReactorApi() = default;
  virtual void Run() = 0;
  virtual void Stop() = 0;
  virtual Reactor::Reactable* Register(int fd, base::Closure on_read_ready, base::Closure on_write_ready) = 0;
  virtual void Unregister(Reactor::Reactable* reactable) = 0;
  virtual bool WaitForUnregisteredReactable(std::chrono::milliseconds timeout) = 0;
  virtual bool WaitForIdle(std::chrono::milliseconds timeout) = 0;
  virtual void ModifyRegistration(
      Reactor::Reactable* reactable, base::Closure on_read_ready, base::Closure on_write_ready) = 0;

 protected:
  std::mutex mutex_;
  std::atomic<bool> is_running_{false};
  std::list<Reactor::Reactable*> invalidation_list_;
  std::shared_ptr<std::promise<void>> idle_promise_;
  std::shared_ptr<std::future<void>> executing_reactable_finished_;
};

}  // namespace os
}  // namespace bluetooth
