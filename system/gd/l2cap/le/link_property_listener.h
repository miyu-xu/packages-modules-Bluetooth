/*
 * Copyright 2021 The Android Open Source Project
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

#include <memory>

#include "hci/address_with_type.h"
#include "hci/hci_packets.h"

namespace bluetooth {
namespace l2cap {
namespace le {

/**
 * This is the listener interface for link property callbacks.
 */
class LinkPropertyListener {
 public:
  virtual ~LinkPropertyListener() = default;

  /**
   * Invoked when an ACL link is connected.
   */
  virtual void OnLinkConnected(
      [[maybe_unused]] hci::AddressWithType remote,
      [[maybe_unused]] uint16_t handle,
      [[maybe_unused]] hci::Role my_role) {}

  /**
   * Invoked when an ACL link is disconnected.
   */
  virtual void OnLinkDisconnected([[maybe_unused]] hci::AddressWithType remote) {}

  /**
   * Invoked when received remote version information for a given link
   */
  virtual void OnReadRemoteVersionInformation(
      [[maybe_unused]] hci::ErrorCode hci_status,
      [[maybe_unused]] hci::AddressWithType remote,
      [[maybe_unused]] uint8_t lmp_version,
      [[maybe_unused]] uint16_t manufacturer_name,
      [[maybe_unused]] uint16_t sub_version) {}

  /**
   * Invoked when received connection update for a given link
   */
  virtual void OnConnectionUpdate(
      [[maybe_unused]] hci::AddressWithType remote,
      [[maybe_unused]] uint16_t connection_interval,
      [[maybe_unused]] uint16_t connection_latency,
      [[maybe_unused]] uint16_t supervision_timeout) {}

  /**
   * Invoked when received PHY update for a given link
   */
  virtual void OnPhyUpdate(
      [[maybe_unused]] hci::AddressWithType remote,
      [[maybe_unused]] uint8_t tx_phy,
      [[maybe_unused]] uint8_t rx_phy) {}

  /**
   * Invoked when received data length exchange for a given link
   */
  virtual void OnDataLengthChange(
      [[maybe_unused]] hci::AddressWithType remote,
      [[maybe_unused]] uint16_t tx_octets,
      [[maybe_unused]] uint16_t tx_time,
      [[maybe_unused]] uint16_t rx_octets,
      [[maybe_unused]] uint16_t rx_time) {}
};

}  // namespace le
}  // namespace l2cap
}  // namespace bluetooth
