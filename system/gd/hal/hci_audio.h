/*
 * Copyright 2024 The Android Open Source Project
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

#include <functional>
#include <memory>
#include <optional>

namespace bluetooth::hal {

// Aggregating a connection handle and an L2CAP Channel ID,
// identifying an A2DP Stream link.

struct A2dpLinkId {
  uint16_t conn_handle;
  uint16_t l2cap_cid;

  constexpr bool operator==(const A2dpLinkId& rhs) const {
    return conn_handle == rhs.conn_handle && l2cap_cid == rhs.l2cap_cid;
  }

  constexpr bool operator!=(const A2dpLinkId& rhs) const {
    return !(*this == rhs);
  }
};

// Direction of the audio Stream
//
enum A2dpStreamDirection {
  kOutput = 0,
  kInput = 1,
};

// Notifications
//
// `startA2dp()` and `stopA2dp()` are called on "Start/Stop A2DP Offload"
// Vendor Specific Command emitted.
//
struct A2dpNotification {
  virtual ~A2dpNotification() = default;
  virtual void startA2dp(A2dpStreamDirection, A2dpLinkId) = 0;
  virtual void stopA2dp(A2dpStreamDirection, A2dpLinkId) = 0;
};

// A2DP Buffers Configuration
//
// - `reserve_packets` indicates the number of buffers reserved in the controller.
//   No more than half of the controller buffers can be reserved.
//
// - `max_packets` indicates the maximum number of packets buffered.
//   When this limit is exceeded, packets are dropped.
//
struct A2dpBuffers {
  size_t packets_reserve = 3;
  size_t max_packets = 10;
};

// A2DP Audio Output
//
// When A2dpLinkId is not set, next start indication is taken into account.
// Once started (`startA2dp()` notification called), `SendA2dpPacket()` can
// be used to send packet; The packet size is limited by the controller buffer size
//
void setupA2dpOutput(
    std::optional<A2dpLinkId>,
    std::shared_ptr<A2dpNotification>,
    A2dpBuffers buffers = A2dpBuffers(),
    int* max_packet_size = nullptr);

void sendA2dpPacket(std::vector<uint8_t>);

void releaseA2dpOutput(std::optional<A2dpLinkId>);

// A2DP Audio Input
//
// When A2dpLinkId is not set, next start indication is taken into account.
// Once started (`startA2dp()` notification called), the callback `recv_cb()`
// is called for each A2DP packet received.
//
void setupA2dpInput(
    std::optional<A2dpLinkId>,
    std::shared_ptr<A2dpNotification>,
    std::function<void(std::vector<uint8_t>)> recv_cb);

void releaseA2dpInput(std::optional<A2dpLinkId>);

}  // namespace bluetooth::hal
