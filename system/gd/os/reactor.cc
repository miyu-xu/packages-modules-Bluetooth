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

#include "os/internal/reactor_api.h"
#include "os/internal/reactor_event_api.h"
#include "os/log.h"

namespace bluetooth {
namespace os {

ReactorApi* NewEventfdReactor();
ReactorApi* NewRustReactor();
ReactorEventApi* NewReactorEventWithEventfd();
ReactorEventApi* NewReactorEventWithTokio();

// The implementation is a concrete object implementing the reactor API
// and is determined at runtime
struct Reactor::impl : public ReactorApi {};

}  // namespace os
}  // namespace bluetooth

// TODO Connect to devflags
static bool dev_init_rust_reactor_flag = false;

namespace {
bluetooth::os::ReactorApi* ReactorFactory() {
  if (!dev_init_rust_reactor_flag) {
    return bluetooth::os::NewEventfdReactor();
  } else {
    ASSERT_LOG(false, "Rust reactor is unimplemented");
    return bluetooth::os::NewRustReactor();
  }
}

bluetooth::os::ReactorEventApi* ReactorEventApiFactory() {
  if (!dev_init_rust_reactor_flag) {
    return bluetooth::os::NewReactorEventWithEventfd();
  } else {
    ASSERT_LOG(false, "Rust reactor is unimplemented");
    return bluetooth::os::NewReactorEventWithTokio();
  }
}
}  // namespace

namespace bluetooth {
namespace os {

struct Reactor::Event::impl final {
  impl(ReactorEventApi* reactor_event_api) : reactor_event_api_(reactor_event_api) {}
  ~impl() {
    delete reactor_event_api_;
  }

  ReactorEventApi* reactor_event_api_{nullptr};
};

Reactor::Event::Event() {
  pimpl_ = new Reactor::Event::impl(ReactorEventApiFactory());
}

Reactor::Event::~Event() {
  delete pimpl_;
  pimpl_ = nullptr;
}

bool Reactor::Event::Read() {
  return pimpl_->reactor_event_api_->Read();
}
int Reactor::Event::Id() const {
  return pimpl_->reactor_event_api_->Id();
}

void Reactor::Event::Clear() {
  pimpl_->reactor_event_api_->Clear();
}

void Reactor::Event::Close() {
  pimpl_->reactor_event_api_->Close();
}
void Reactor::Event::Notify() {
  pimpl_->reactor_event_api_->Notify();
}

// Construct a reactor on the current thread
Reactor::Reactor() {
  pimpl_ = static_cast<bluetooth::os::Reactor::impl*>(ReactorFactory());
  ASSERT(pimpl_ != nullptr);
}

// Destruct this reactor and release its resources
Reactor::~Reactor() {
  ASSERT(pimpl_ != nullptr);
  delete pimpl_;
}

// Start the reactor. The current thread will be blocked until Stop() is invoked and handled.
void Reactor::Run() {
  pimpl_->Run();
}

// Stop the reactor. Must be invoked from a different thread. Note: all registered reactables will not be unregistered
// by Stop(). If the reactor is not running, it will be stopped once it's started.
void Reactor::Stop() {
  pimpl_->Stop();
}

// Register a reactable fd to this reactor. Returns a pointer to a Reactable. Caller must use this object to
// unregister or modify registration. Ownership of the memory space is NOT transferred to user.
Reactor::Reactable* Reactor::Register(int fd, common::Closure on_read_ready, common::Closure on_write_ready) {
  return pimpl_->Register(fd, on_read_ready, on_write_ready);
}

// Unregister a reactable from this reactor
void Reactor::Unregister(Reactable* reactable) {
  pimpl_->Unregister(reactable);
}

// Wait for up to timeout milliseconds, and return true if the reactable finished executing.
bool Reactor::WaitForUnregisteredReactable(std::chrono::milliseconds timeout) {
  return pimpl_->WaitForUnregisteredReactable(timeout);
}

// Wait for up to timeout milliseconds, and return true if we reached idle.
bool Reactor::WaitForIdle(std::chrono::milliseconds timeout) {
  return pimpl_->WaitForIdle(timeout);
}

// Modify the registration for a reactable with given reactable
void Reactor::ModifyRegistration(Reactable* reactable, common::Closure on_read_ready, common::Closure on_write_ready) {
  return pimpl_->ModifyRegistration(reactable, on_read_ready, on_write_ready);
}

std::unique_ptr<Reactor::Event> Reactor::NewEvent() const {
  return std::make_unique<Reactor::Event>();
}

}  // namespace os
}  // namespace bluetooth
