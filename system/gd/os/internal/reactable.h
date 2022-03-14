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

#include <atomic>
#include <memory>
#include <mutex>

#include "common/callback.h"
#include "os/reactor.h"

namespace bluetooth {
namespace os {

class Reactor::Reactable {
 public:
  Reactable(int fd, common::Closure on_read_ready, common::Closure on_write_ready)
      : fd_(fd),
        on_read_ready_(std::move(on_read_ready)),
        on_write_ready_(std::move(on_write_ready)),
        is_executing_(false),
        removed_(false) {}
  const int fd_;
  common::Closure on_read_ready_;
  common::Closure on_write_ready_;
  bool is_executing_;
  bool removed_;
  std::mutex mutex_;
  std::unique_ptr<std::promise<void>> finished_promise_;
};

}  // namespace os
}  // namespace bluetooth
