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

#include "os/reactor.h"

#include "common/callback.h"
#include "os/internal/reactable.h"
#include "os/internal/reactor_api.h"
#include "os/internal/reactor_event_api.h"
#include "os/log.h"

using bluetooth::common::Closure;

namespace bluetooth {
namespace os {

struct TokioEvent : public ReactorEventApi {
  TokioEvent() {}
  ~TokioEvent() {}
  bool Read() override {
    return false;
  }
  int Id() const override {
    return 0;
  }
  void Clear() override {}
  void Close() override {}
  void Notify() override {}
};

class RustReactor : public ReactorApi {
 public:
  RustReactor();
  ~RustReactor();
  void Run() override;
  void Stop() override;
  Reactor::Reactable* Register(int fd, Closure on_read_ready, Closure on_write_ready) override;
  void Unregister(Reactor::Reactable* reactable) override;
  bool WaitForUnregisteredReactable(std::chrono::milliseconds timeout) override;
  bool WaitForIdle(std::chrono::milliseconds timeout) override;
  void ModifyRegistration(Reactor::Reactable* reactable, Closure on_read_ready, Closure on_write_ready) override;
};

ReactorEventApi* NewReactorEventRust() {
  return new TokioEvent();
}

ReactorApi* NewRustReactor() {
  return new RustReactor();
}

RustReactor::RustReactor() {
  LOG_ERROR("UNIMPLEMENTED");
  // TODO: Create a reactor equivalent in the tokio space
  // bluetooth::shim::rust::new_reactor();
}

RustReactor::~RustReactor() {
  LOG_ERROR("UNIMPLEMENTED");
  // TODO: Clean up a reactor equivalent in the tokio space
  // bluetooth::shim::rust::free_reactor();
}

void RustReactor::Run() {
  LOG_ERROR("UNIMPLEMENTED");
  // bluetooth::shim::rust::run_reactor();
}

void RustReactor::Stop() {
  LOG_ERROR("UNIMPLEMENTED");
  // bluetooth::shim::rust::stop_reactor();
}

Reactor::Reactable* RustReactor::Register(int fd, Closure on_read_ready, Closure on_write_ready) {
  LOG_ERROR("UNIMPLEMENTED");
  // bluetooth::shim::rust::register_reactor();
  auto* reactable = new Reactor::Reactable(fd, on_read_ready, on_write_ready);
  return reactable;
}

void RustReactor::Unregister(Reactor::Reactable* reactable) {
  ASSERT(reactable != nullptr);
  LOG_ERROR("UNIMPLEMENTED");
  // bluetooth::shim::rust::unregister_reactor();
}

bool RustReactor::WaitForUnregisteredReactable(std::chrono::milliseconds timeout) {
  LOG_ERROR("UNIMPLEMENTED");
  // return bluetooth::shim::rust::wait_for_unregistered_reactable();
  return false;
}

bool RustReactor::WaitForIdle(std::chrono::milliseconds timeout) {
  LOG_ERROR("UNIMPLEMENTED");
  // return bluetooth::shim::rust::wait_for_idle();
  return false;
}

void RustReactor::ModifyRegistration(Reactor::Reactable* reactable, Closure on_read_ready, Closure on_write_ready) {
  ASSERT(reactable != nullptr);
  LOG_ERROR("UNIMPLEMENTED");
  //  bluetooth::shim::rust::modify_registration();
}

}  // namespace os
}  // namespace bluetooth
