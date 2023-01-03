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

#include "le_connection_packet_router.h"

#include <base/bind.h>

#include "hci/acl_manager.h"
#include "module.h"
#include "packet/raw_builder.h"

namespace bluetooth {
namespace arbiter {
namespace internal {

// NOTES ON HOW THE LIFETIMES OF EVERYTHING WORKS: READ ME TO AVOID USE-AFTER-FREE!
//
// The lifecycle of QueueEnds is tied to their associated connection. After a connection is dropped,
// we may no longer dequeue or enqueue from its queue. IN ADDITION, we MUST NOT be registered to a
// connection queue (either Enqueue or Dequeue) when a connection is destructed. Thus, in the
// invalidate_callback (which is invoked SYNCHRONOUSLY), we must unregister ourselves.
//
// A consequence is that the invalidate_callback is invoked on an UNKNOWN THREAD - thus, locking is
// REQUIRED for any fields that it touches (thus we choose to just lock everything in this class).
//
// HOWEVER, a queue may be owned on a SEPARATE THREAD from where we are running. Therefore, when
// interacting with callbacks registered on queues, we MUST (1) acquire the lock, (2) check that the
// queue is still alive, and only finally (3) doing whatever operations. The reason this is safe is
// because the destructor invokes invalidate_callback, which also acquires the lock. So after lock
// acquisition, our queue cannot be destructed from underneath us.
//
// The lifetime of the base connection exceeds that of all child connections. Thus, when the last
// child is destroyed, we must unregister ourselves from the base connection queue (both Enqueue and
// Dequeue).
//
// In addition, once a connection destructs, we MUST NOT invoke any ConnectionCallbacks upon it. We
// get the callbacks to invoke from GetEventCallbacks(), so we can use the invalidate_callback to
// ensure that this is the case.
//
// To control packet flow, we are always EITHER registered as an EnqueueCallback on one side of the
// multiplexer, OR a DequeueCallback on the either. Once we dequeue, we unregister ourselves as a
// DequeueCallback, register ourselves as an EnqueueCallback, and store the dequeued packet as an
// instance variable. This means that backpressure automatically works.

LeConnectionPacketRouter::LeConnectionPacketRouter(
    std::unique_ptr<hci::acl_manager::LeAclConnection> connection,
    std::vector<std::tuple<
        ArbiterClient,
        hci::acl_manager::LeAclConnection*,
        hci::acl_manager::LeAclConnection::QueueDownEnd*>>& clients,
    os::Handler* handler,
    common::ContextualOnceCallback<void(LeConnectionPacketRouter*)> on_release)
    : connection_{std::move(connection)},
      handler_(handler),
      on_release_{std::move(on_release)},
      callback_dispatcher_{
          common::Bind(
              &LeConnectionPacketRouter::GetValidClientsInPriorityOrder, common::Unretained(this)),
          lock_} {
  num_active_clients_ = clients.size();
  // store clients in descending order of priority
  for (size_t i = clients.size() - 1; i >= 0; --i) {
    // we store the TRUE index of clients in clients_, DO NOT USE i!
    auto& [client, child_connection, child_queue_end] = clients[i];
    auto client_idx = clients.size() - i;

    auto callbacks = child_connection->GetEventCallbacks(
        [client_idx, this](uint16_t handle) { InvalidateClient(client_idx); });
    clients_.emplace_back(
        ArbiterClientWithConnection{client_idx, client, callbacks, child_queue_end});
  }

  connection_->RegisterCallbacks(&callback_dispatcher_, handler_);

  RegisterDequeueUpwards();
  RegisterDequeueDownwards();
}

// Queue Callbacks
void LeConnectionPacketRouter::OnUpwardsPacketReady() {
  std::scoped_lock lock{lock_};
  if (pending_up_packet_ != nullptr) {
    LOG_ALWAYS_FATAL("Unexpected upwards dequeue when packet already pending");
  }
  pending_up_packet_ = connection_->GetAclQueueEnd()->TryDequeue();
  for (auto client : GetValidClientsInPriorityOrder()) {
    auto matches = std::visit(
        [this](auto&& filter) {
          using T = std::decay_t<decltype(filter)>;
          if constexpr (std::is_same_v<T, UnconditionalPacketFilter>) {
            return true;
          } else if constexpr (std::is_same_v<T, ExclusiveCallbackPacketFilter>) {
            return filter.callback.Run(pending_up_packet_.get());
          } else {
            static_assert(!sizeof(T*), "missed filter type");
          }
        },
        client->client.packet_filter);
    if (matches) {
      UnregisterDequeueUpwards();
      RegisterEnqueueUpwards(client);
      return;
    }
  }
  LOG_WARN("Incoming packet did not match any PacketFilters, dropping it");
  pending_up_packet_ = nullptr;
}

std::unique_ptr<packet::PacketView<packet::kLittleEndian>>
LeConnectionPacketRouter::ForwardUpwardsPacketToClient(size_t client_idx) {
  std::scoped_lock lock{lock_};
  if (!clients_[client_idx].has_value()) {
    LOG_WARN("Dropping incoming packet to client since it is about to destruct");
    return nullptr;
  }
  UnregisterEnqueueUpwards(&clients_[client_idx].value());
  RegisterDequeueUpwards();
  return std::move(pending_up_packet_);
}

void LeConnectionPacketRouter::OnDownwardsPacketReady(size_t client_idx) {
  std::scoped_lock lock{lock_};
  if (!clients_[client_idx].has_value()) {
    LOG_WARN("Dropping outgoing packet from client since it destructed");
    return;
  }
  // we know the client is valid here, since if it was not, we would already have Deregistered
  if (pending_down_packet_ != nullptr) {
    LOG_ALWAYS_FATAL("Unexpected downwards dequeue when packet already pending");
  }
  pending_down_packet_ = clients_[client_idx].value().queue_end->TryDequeue();

  UnregisterDequeueDownwards();
  RegisterEnqueueDownwards();
}

std::unique_ptr<packet::BasePacketBuilder> LeConnectionPacketRouter::ForwardDownwardsPacket() {
  std::scoped_lock lock{lock_};
  UnregisterEnqueueDownwards();
  RegisterDequeueDownwards();
  return std::move(pending_down_packet_);
}

// Client management
void LeConnectionPacketRouter::InvalidateClient(size_t client_idx) {
  std::scoped_lock lock{lock_};
  if (!clients_[client_idx].has_value()) {
    LOG_ALWAYS_FATAL("Trying to double-invalidate a client");
  }
  if (clients_[client_idx].value().is_upwards_packet_pending) {
    // if we have a pending_up_packet, then we must be waiting to enqueue it, so we should
    // deregister here and return to dequeueing from the downwards channel
    LOG_WARN("Dropping incoming packet to client since client is de-registering");
    pending_up_packet_ = nullptr;
    UnregisterEnqueueUpwards(&clients_[client_idx].value());
    RegisterDequeueUpwards();
  }
  if (pending_down_packet_ == nullptr) {
    // if we don't have a staged down packet, we are waiting for one from each active client,
    // so we should deregister here for this client in particular
    clients_[client_idx].value().queue_end->UnregisterDequeue();
  }

  clients_[client_idx] = std::nullopt;
  num_active_clients_ -= 1;

  if (num_active_clients_ == 0) {
    connection_ = nullptr;
    if (pending_down_packet_ != nullptr) {
      LOG_WARN("Dropping outgoing packet since connection is being released");
      UnregisterEnqueueDownwards();
    }
    if (pending_up_packet_ == nullptr) {
      UnregisterDequeueUpwards();
    }
    on_release_.Invoke(this);
  }
}

// Internal, called while holding the lock already
std::vector<ArbiterClientWithConnection*>
LeConnectionPacketRouter::GetValidClientsInPriorityOrder() {
  auto out = std::vector<ArbiterClientWithConnection*>{};
  for (auto& client : clients_) {
    if (client.has_value()) {
      out.push_back(&client.value());
    }
  }
  return out;
}

// Enqueue / Dequeue boilerplate (requires lock to be held by caller)
// Also requires the caller to verify that the client is valid
void LeConnectionPacketRouter::RegisterEnqueueUpwards(ArbiterClientWithConnection* client) {
  client->is_upwards_packet_pending = true;
  connection_->GetAclQueueEnd()->UnregisterDequeue();
  client->queue_end->RegisterEnqueue(
      handler_,
      common::Bind(
          &LeConnectionPacketRouter::ForwardUpwardsPacketToClient,
          common::Unretained(this),
          client->index));
}

void LeConnectionPacketRouter::UnregisterEnqueueUpwards(ArbiterClientWithConnection* client) {
  client->is_upwards_packet_pending = false;
  client->queue_end->UnregisterEnqueue();
}

void LeConnectionPacketRouter::RegisterEnqueueDownwards() {
  connection_->GetAclQueueEnd()->RegisterEnqueue(
      handler_,
      base::Bind(&LeConnectionPacketRouter::ForwardDownwardsPacket, common::Unretained(this)));
}

void LeConnectionPacketRouter::UnregisterEnqueueDownwards() {
  connection_->GetAclQueueEnd()->UnregisterEnqueue();
}

void LeConnectionPacketRouter::RegisterDequeueUpwards() {
  connection_->GetAclQueueEnd()->RegisterDequeue(
      handler_,
      base::Bind(&LeConnectionPacketRouter::OnUpwardsPacketReady, common::Unretained(this)));
}

void LeConnectionPacketRouter::UnregisterDequeueUpwards() {
  connection_->GetAclQueueEnd()->UnregisterDequeue();
}

void LeConnectionPacketRouter::RegisterDequeueDownwards() {
  for (auto client : GetValidClientsInPriorityOrder()) {
    client->queue_end->RegisterDequeue(
        handler_,
        common::Bind(
            &LeConnectionPacketRouter::OnDownwardsPacketReady,
            common::Unretained(this),
            client->index));
  }
}

void LeConnectionPacketRouter::UnregisterDequeueDownwards() {
  for (auto client : GetValidClientsInPriorityOrder()) {
    client->queue_end->UnregisterDequeue();
  }
}

}  // namespace internal
}  // namespace arbiter
}  // namespace bluetooth