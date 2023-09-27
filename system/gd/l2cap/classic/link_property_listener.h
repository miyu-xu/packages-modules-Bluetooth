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
  virtual void OnLinkConnected(hci::Address, uint16_t) {}

  /**
   * Invoked when an ACL link is disconnected.
   */
  virtual void OnLinkDisconnected([[maybe_unused]] hci::Address remote) {}

  /**
   * Invoked when received remote version information for a given link
   */
  virtual void OnReadRemoteVersionInformation(
      hci::ErrorCode, hci::Address, uint8_t, uint16_t, uint16_t) {}

  /**
   * Invoked when received remote features and remote supported features for a
   * given link
   */
  virtual void OnReadRemoteSupportedFeatures(hci::Address, uint64_t) {}

  /**
   * Invoked when received remote features and remote extended features for a
   * given link
   */
  virtual void OnReadRemoteExtendedFeatures(hci::Address, uint8_t, uint8_t, uint64_t) {}

  /**
   * Invoked when received role change
   */
  virtual void OnRoleChange(hci::ErrorCode, hci::Address, hci::Role) {}

  /**
   * Invoked when received clock offset
   */
  virtual void OnReadClockOffset(hci::Address, uint16_t) {}

  /**
   * Invoked when received mode change
   */
  virtual void OnModeChange(hci::ErrorCode, hci::Address, hci::Mode, uint16_t) {}

  /**
   * Invoked when received sniff subrating
   */
  virtual void OnSniffSubrating(
      hci::ErrorCode, hci::Address, uint16_t, uint16_t, uint16_t, uint16_t) {}
};

}  // namespace classic
}  // namespace l2cap
}  // namespace bluetooth
