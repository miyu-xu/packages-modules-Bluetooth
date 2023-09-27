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

#include "hci/address.h"
#include "hci/hci_packets.h"

namespace bluetooth {
namespace l2cap {
namespace classic {

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
      [[maybe_unused]] hci::Address remote, [[maybe_unused]] uint16_t handle) {}

  /**
   * Invoked when an ACL link is disconnected.
   */
  virtual void OnLinkDisconnected([[maybe_unused]] hci::Address remote) {}

  /**
   * Invoked when received remote version information for a given link
   */
  virtual void OnReadRemoteVersionInformation(
      [[maybe_unused]] hci::ErrorCode hci_status,
      [[maybe_unused]] hci::Address remote,
      [[maybe_unused]] uint8_t lmp_version,
      [[maybe_unused]] uint16_t manufacturer_name,
      [[maybe_unused]] uint16_t sub_version) {}

  /**
   * Invoked when received remote features and remote supported features for a
   * given link
   */
  virtual void OnReadRemoteSupportedFeatures(
      [[maybe_unused]] hci::Address remote, [[maybe_unused]] uint64_t features) {}

  /**
   * Invoked when received remote features and remote extended features for a
   * given link
   */
  virtual void OnReadRemoteExtendedFeatures(
      [[maybe_unused]] hci::Address remote,
      [[maybe_unused]] uint8_t page_number,
      [[maybe_unused]] uint8_t max_page_number,
      [[maybe_unused]] uint64_t features) {}

  /**
   * Invoked when received role change
   */
  virtual void OnRoleChange(
      [[maybe_unused]] hci::ErrorCode hci_status,
      [[maybe_unused]] hci::Address remote,
      [[maybe_unused]] hci::Role role) {}

  /**
   * Invoked when received clock offset
   */
  virtual void OnReadClockOffset(
      [[maybe_unused]] hci::Address remote, [[maybe_unused]] uint16_t clock_offset) {}

  /**
   * Invoked when received mode change
   */
  virtual void OnModeChange(
      [[maybe_unused]] hci::ErrorCode hci_status,
      [[maybe_unused]] hci::Address remote,
      [[maybe_unused]] hci::Mode mode,
      [[maybe_unused]] uint16_t interval) {}

  /**
   * Invoked when received sniff subrating
   */
  virtual void OnSniffSubrating(
      [[maybe_unused]] hci::ErrorCode hci_status,
      [[maybe_unused]] hci::Address remote,
      [[maybe_unused]] uint16_t max_tx_lat,
      [[maybe_unused]] uint16_t max_rx_lat,
      [[maybe_unused]] uint16_t min_remote_timeout,
      [[maybe_unused]] uint16_t min_local_timeout) {}
};

}  // namespace classic
}  // namespace l2cap
}  // namespace bluetooth
