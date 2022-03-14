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
#include "os/log.h"

struct bluetooth::os::Reactor::impl : public ReactorApi {};

namespace bluetooth {
namespace os {
ReactorApi* NewEventfdReactor();
ReactorApi* NewRustReactor();
}  // namespace os
}  // namespace bluetooth

// TODO Connect to devflags
static bool dev_init_flag_rust_reactor = false;

namespace {
bluetooth::os::ReactorApi* ReactorFactory() {
  if (!dev_init_flag_rust_reactor) {
    return bluetooth::os::NewEventfdReactor();
  } else {
    ASSERT_LOG(false, "Rust reactor is unimplemented");
    return bluetooth::os::NewRustReactor();
  }
}
}  // namespace

namespace bluetooth {
namespace os {

// Construct a reactor on the current thread
Reactor::Reactor() {
  pimpl_ = static_cast<bluetooth::os::Reactor::impl*>(ReactorFactory());
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
