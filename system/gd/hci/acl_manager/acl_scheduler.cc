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

#include "acl_scheduler.h"

#include <optional>
#include <queue>
#include <unordered_set>
#include <variant>

namespace bluetooth {
namespace hci {

namespace acl_manager {

struct AclCreateConnectionQueueEntry {
  Address address;
  common::ContextualOnceCallback<void()> callback;
};

struct RemoteNameRequestQueueEntry {
  Address address;
  common::ContextualOnceCallback<void()> callback;
};

using QueueEntry = std::variant<AclCreateConnectionQueueEntry, RemoteNameRequestQueueEntry>;

struct AclScheduler::impl {
 public:
  void EnqueueOutgoingAclConnection(Address address, common::ContextualOnceCallback<void()> start_connection) {
    pending_outgoing_connections_.push(AclCreateConnectionQueueEntry{address, std::move(start_connection)});
    try_dequeue_next_connection();
  }

  void RegisterPendingIncomingConnection(Address address) {
    incoming_connecting_address_set_.insert(address);
  }

  void ReportAclConnectionCompletion(
      Address address,
      common::ContextualOnceCallback<void()> handle_outgoing_connection,
      common::ContextualOnceCallback<void()> handle_incoming_connection,
      common::ContextualOnceCallback<void(std::string)> handle_unknown_connection) {
    if (outgoing_entry_.has_value()) {
      auto entry = std::get_if<AclCreateConnectionQueueEntry>(&outgoing_entry_.value());
      if (entry != nullptr && entry->address == address) {
        outgoing_entry_.reset();
        handle_outgoing_connection.InvokeIfNotEmpty();
        try_dequeue_next_connection();
      }
    } else if (incoming_connecting_address_set_.find(address) != incoming_connecting_address_set_.end()) {
      incoming_connecting_address_set_.erase(address);
      handle_incoming_connection.InvokeIfNotEmpty();
    } else {
      handle_unknown_connection.InvokeIfNotEmpty(set_of_incoming_connecting_addresses());
    }
  }

  void CancelAclConnection(
      Address address,
      common::ContextualOnceCallback<void()> cancel_connection,
      common::ContextualOnceCallback<void()> cancel_connection_completed) {
    cancel_connection.Invoke();
  }

  void EnqueueRemoteNameRequest(Address address, common::ContextualOnceCallback<void()> start_request) {
    pending_outgoing_connections_.push(RemoteNameRequestQueueEntry{address, std::move(start_request)});
  }

  void ReportRemoteNameRequestCompletion(Address address) {
    outgoing_entry_.reset();
    try_dequeue_next_connection();
  }

  void CancelRemoteNameRequest(
      Address address,
      common::ContextualOnceCallback<void()> cancel_request,
      common::ContextualOnceCallback<void()> cancel_request_completed){
      // TODO
  };

  void Stop() {
    stopped_ = true;
  }

 private:
  void try_dequeue_next_connection() {
    if (stopped_) {
      return;
    }
    if (incoming_connecting_address_set_.empty() && !outgoing_entry_.has_value() &&
        !pending_outgoing_connections_.empty()) {
      LOG_INFO("Pending connections is not empty; so sending next connection");
      auto entry = std::move(pending_outgoing_connections_.front());
      pending_outgoing_connections_.pop();
      std::visit([](auto&& variant) { variant.callback.Invoke(); }, entry);
      outgoing_entry_ = std::move(entry);
    }
  }

  const std::string set_of_incoming_connecting_addresses() const {
    std::stringstream buffer;
    for (const auto& c : incoming_connecting_address_set_) buffer << " " << c;
    return buffer.str();
  }

  std::optional<QueueEntry> outgoing_entry_;
  std::queue<QueueEntry> pending_outgoing_connections_;
  std::unordered_set<Address> incoming_connecting_address_set_;
  bool stopped_ = false;
};

AclScheduler::AclScheduler() = default;
AclScheduler::~AclScheduler() = default;

void AclScheduler::EnqueueOutgoingAclConnection(
    Address address, common::ContextualOnceCallback<void()> start_connection) {
  GetHandler()->Post(common::BindOnce(
      &impl::EnqueueOutgoingAclConnection, common::Unretained(pimpl_.get()), address, std::move(start_connection)));
}

void AclScheduler::RegisterPendingIncomingConnection(Address address) {
  GetHandler()->Post(
      common::BindOnce(&impl::RegisterPendingIncomingConnection, common::Unretained(pimpl_.get()), address));
}

void AclScheduler::ReportAclConnectionCompletion(
    Address address,
    common::ContextualOnceCallback<void()> handle_outgoing_connection,
    common::ContextualOnceCallback<void()> handle_incoming_connection,
    common::ContextualOnceCallback<void(std::string)> handle_unknown_connection) {
  GetHandler()->Post(common::BindOnce(
      &impl::ReportAclConnectionCompletion,
      common::Unretained(pimpl_.get()),
      address,
      std::move(handle_outgoing_connection),
      std::move(handle_incoming_connection),
      std::move(handle_unknown_connection)));
}

void AclScheduler::ReportAclConnectionCompletion(Address address) {
  ReportAclConnectionCompletion(address, {}, {}, {});
}

void AclScheduler::CancelAclConnection(
    Address address,
    common::ContextualOnceCallback<void()> cancel_connection,
    common::ContextualOnceCallback<void()> cancel_connection_completed) {
  GetHandler()->Post(common::BindOnce(
      &impl::CancelAclConnection,
      common::Unretained(pimpl_.get()),
      address,
      std::move(cancel_connection),
      std::move(cancel_connection_completed)));
}

void AclScheduler::EnqueueRemoteNameRequest(Address address, common::ContextualOnceCallback<void()> start_request) {
  GetHandler()->Post(common::BindOnce(
      &impl::EnqueueRemoteNameRequest, common::Unretained(pimpl_.get()), address, std::move(start_request)));
}

void AclScheduler::ReportRemoteNameRequestCompletion(Address address) {
  GetHandler()->Post(
      common::BindOnce(&impl::ReportRemoteNameRequestCompletion, common::Unretained(pimpl_.get()), address));
}

void AclScheduler::CancelRemoteNameRequest(
    Address address,
    common::ContextualOnceCallback<void()> cancel_request,
    common::ContextualOnceCallback<void()> cancel_request_completed) {
  GetHandler()->Post(common::BindOnce(
      &impl::CancelRemoteNameRequest,
      common::Unretained(pimpl_.get()),
      address,
      std::move(cancel_request),
      std::move(std::move(cancel_request_completed))));
}

void AclScheduler::ListDependencies(ModuleList* list) const {}

void AclScheduler::Start() {}

void AclScheduler::Stop() {
  pimpl_->Stop();
}

}  // namespace acl_manager
}  // namespace hci
}  // namespace bluetooth