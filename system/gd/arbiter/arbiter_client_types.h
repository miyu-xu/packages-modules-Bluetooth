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

#include "arbiter_filters.h"
#include "hci/acl_manager.h"

namespace bluetooth {
namespace arbiter {

enum ClientPriority { CLIENT_PRIORITY_FALLBACK = 0, CLIENT_PRIORITY_NORMAL = 1 };

// Opaque token representing a registered client.
struct Token {
  int val;
};

namespace internal {

struct ArbiterClient {
  Token token;
  hci::acl_manager::LeConnectionCallbacks* callbacks;
  ConnectionFilter connection_filter;
  PacketFilter packet_filter;
  os::Handler* handler;
  ClientPriority priority;

  friend bool operator<(const ArbiterClient& l, const ArbiterClient& r) {
    // sort by priority, then LIFO by recency (since token values are incrementing)
    return std::forward_as_tuple(l.priority, -l.token.val) <
           std::forward_as_tuple(r.priority, -r.token.val);
  }
};

struct ArbiterClientWithConnection {
  size_t index{};
  ArbiterClient client{};
  hci::acl_manager::LeConnectionManagementCallbacks* callbacks{};
  hci::acl_manager::LeAclConnection::QueueDownEnd* queue_end{};
  bool is_upwards_packet_pending{false};

  // since this holds state, it is move-only
  ArbiterClientWithConnection(ArbiterClientWithConnection&&) = default;
  ArbiterClientWithConnection& operator=(ArbiterClientWithConnection&&) = default;
};

}  // namespace internal

}  // namespace arbiter
}  // namespace bluetooth