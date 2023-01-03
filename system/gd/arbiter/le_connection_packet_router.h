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

#include <vector>

#include "arbiter_client_types.h"
#include "arbiter_filters.h"
#include "hci/acl_manager.h"
#include "le_connection_callback_dispatcher.h"

namespace bluetooth {
namespace arbiter {
namespace internal {

class LeConnectionPacketRouter {
 public:
  LeConnectionPacketRouter(
      std::unique_ptr<hci::acl_manager::LeAclConnection> connection,
      std::vector<std::tuple<
          ArbiterClient,
          hci::acl_manager::LeAclConnection*,
          hci::acl_manager::LeAclConnection::QueueDownEnd*>>& clients,
      os::Handler* handler,
      common::ContextualOnceCallback<void(LeConnectionPacketRouter*)> on_release);

 private:
  // Packet flow control
  void OnUpwardsPacketReady();
  std::unique_ptr<packet::PacketView<packet::kLittleEndian>> ForwardUpwardsPacketToClient(
      size_t client_idx);

  void OnDownwardsPacketReady(size_t client_idx);
  std::unique_ptr<packet::BasePacketBuilder> ForwardDownwardsPacket();

  // Client management
  void InvalidateClient(size_t client_idx);
  std::vector<ArbiterClientWithConnection*> GetValidClientsInPriorityOrder();

  // Enqueue / Dequeue utilities
  void RegisterEnqueueUpwards(ArbiterClientWithConnection* client);
  void UnregisterEnqueueUpwards(ArbiterClientWithConnection* client);
  void RegisterEnqueueDownwards();
  void UnregisterEnqueueDownwards();
  void RegisterDequeueUpwards();
  void UnregisterDequeueUpwards();
  void RegisterDequeueDownwards();
  void UnregisterDequeueDownwards();

  std::mutex lock_{};

  std::unique_ptr<hci::acl_manager::LeAclConnection> connection_;
  os::Handler* handler_;
  common::ContextualOnceCallback<void(LeConnectionPacketRouter*)> on_release_;
  LeConnectionCallbackDispatcher callback_dispatcher_;

  std::vector<std::optional<ArbiterClientWithConnection>> clients_{};
  size_t num_active_clients_{};

  std::unique_ptr<packet::PacketView<packet::kLittleEndian>> pending_up_packet_{};
  std::unique_ptr<packet::BasePacketBuilder> pending_down_packet_{};

  // we are not movable to preserve pointer stability inside bound callbacks
  LeConnectionPacketRouter(const LeConnectionPacketRouter&) = delete;
  LeConnectionPacketRouter& operator=(const LeConnectionPacketRouter&) = delete;
};

}  // namespace internal
}  // namespace arbiter
}  // namespace bluetooth
