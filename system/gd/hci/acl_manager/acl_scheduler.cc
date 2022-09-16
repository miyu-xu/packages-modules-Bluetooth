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

#include <queue>
#include <unordered_set>

struct AclCreateConnectionQueueEntry {
  Address address;
  AclConnectionMessage message;
  common::ContextualOnceCallback<void(AclConnectionMessage)> callback;
}

struct AclScheduler::impl {
 public:
  void EnqueueAclCreateConnection(
      AclConnectionMessage entry, common::ContextualOnceCallback<void(AclConnectionMessage)> start_connection) {
    pending_outgoing_connections_.emplace(entry.address, std::move(entry), start_connection);
    try_dequeue_next_connection();
  }

  void RegisterPendingIncomingConnection(Address address) {
    incoming_connecting_address_set_.insert(address);
  }

  void ReportAclConnectionCompletion(
      Address address,
      common::ContextualOnceCallback<void> handle_outgoing_connection,
      common::ContextualOnceCallback<void> handle_incoming_connection,
      common::ContextualOnceCallback<void> handle_unknown_connection) {
    if (outgoing_connecting_address_ == address) {
      outgoing_connecting_address_ = Address::kEmpty;
      handle_outgoing_connection.Invoke();
    } else if (incoming_connecting_address_set_.find(address) != incoming_connecting_address_set_.end()) {
      incoming_connecting_address_set_.erase(address);
      handle_incoming_connection.Invoke();
    } else {
      handle_unknown_connection.Invoke(set_of_incoming_connecting_addresses());
    }
    try_dequeue_next_connection();
  }

  void CancelAclConnection(
      Address address,
      common::ContextualOnceCallback<void(Address)> cancel_connection,
      common::ContextualOnceCallback<void(Address)> cancel_connection_completed) {
    cancel_connection.Invoke(address);
  }

 private:
  void try_dequeue_next_connection() {
    if (incoming_connecting_address_set_.empty() && outgoing_connecting_address_.IsEmpty()) {
      while (!pending_outgoing_connections_.empty()) {
        LOG_INFO("Pending connections is not empty; so sending next connection");
        auto create_connection_packet_and_address = std::move(pending_outgoing_connections_.front());
        pending_outgoing_connections_.pop();
        if (!is_classic_link_already_connected(create_connection_packet_and_address.first)) {
          outgoing_connecting_address_ = create_connection_packet_and_address.first;
          acl_connection_interface_->EnqueueCommand(
              std::move(create_connection_packet_and_address.second),
              handler_->BindOnceOn(this, &classic_impl::on_create_connection_status));
          break;
        }
      }
    }
  }

  const std::string set_of_incoming_connecting_addresses() const {
    std::stringstream buffer;
    for (const auto& c : incoming_connecting_address_set_) buffer << " " << c;
    return buffer.str();
  }

  std::queue<AclCreateConnectionQueueEntry> pending_outgoing_connections_;
  std::unordered_set<Address> incoming_connecting_address_set_;
} 

void AclScheduler::EnqueueAclCreateConnection(
    AclConnectionMessage entry,
    common::ContextualOnceCallback<void(AclCreateConnectionQueueEntry)> start_connection) {
  pimpl_->EnqueueAclCreateConnection(std::move(entry), start_connection);
}

void AclScheduler::RegisterPendingIncomingConnection(Address address) {
  pimpl_->RegisterPendingIncomingConnection(address);
}

void AclScheduler::ReportAclConnectionCompletion(
    Address address,
    common::ContextualOnceCallback<void> handle_outgoing_connection,
    common::ContextualOnceCallback<void> handle_incoming_connection,
    common::ContextualOnceCallback<std::string> handle_unknown_connection) {
  pimpl_->ReportAclConnectionCompletion(
      address, handle_outgoing_connection, handle_incoming_connection, handle_unknown_connection);
}

void AclScheduler::CancelAclConnection(
    Address address,
    common::ContextualOnceCallback<void(Address)> cancel_connection,
    common::ContextualOnceCallback<void(Address)> cancel_connection_completed) {
  pimpl_->CancelAclConnection(address, cancel_connection, cancel_connection_completed);
}
