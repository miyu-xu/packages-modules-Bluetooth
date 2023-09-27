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
  virtual void OnLinkConnected(hci::AddressWithType, uint16_t, hci::Role) {}

  /**
   * Invoked when an ACL link is disconnected.
   */
  virtual void OnLinkDisconnected(hci::AddressWithType) {}

  /**
   * Invoked when received remote version information for a given link
   */
  virtual void OnReadRemoteVersionInformation(
      hci::ErrorCode, hci::AddressWithType, uint8_t, uint16_t, uint16_t) {}

  /**
   * Invoked when received connection update for a given link
   */
  virtual void OnConnectionUpdate(hci::AddressWithType, uint16_t, uint16_t, uint16_t) {}

  /**
   * Invoked when received PHY update for a given link
   */
  virtual void OnPhyUpdate(hci::AddressWithType, uint8_t, uint8_t) {}

  /**
   * Invoked when received data length exchange for a given link
   */
  virtual void OnDataLengthChange(hci::AddressWithType, uint16_t, uint16_t, uint16_t, uint16_t) {}
};

}  // namespace le
}  // namespace l2cap
}  // namespace bluetooth
