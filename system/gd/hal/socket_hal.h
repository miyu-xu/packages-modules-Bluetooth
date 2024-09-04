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

#include "hci/lpp_offload_socket_interface.h"
#include "module.h"

using bluetooth::hci::AclLinkType;
using bluetooth::hci::AclPowerMode;
using bluetooth::hci::ConnectionState;
using bluetooth::hci::SocketContext;
using bluetooth::hci::SocketHalCallback;
using bluetooth::hci::SocketProperties;

namespace bluetooth {
namespace hal {

/**
 * SocketHal provides an interface to the low-power processors to support Bluetooth Offload Socket.
 * Bluetooth Offload Socket is a method to provide channel information of an already connected
 * BluetoothSocket to the low-power processors so that offload stack can receive, process, and
 * transmit packets on the channel on behalf of host stack without waking up the application
 * processor host.
 */
class SocketHal : public ::bluetooth::Module {
public:
  static const ModuleFactory Factory;

  virtual ~SocketHal() = default;
  virtual bool IsBound() = 0;

  /**
   * Get the supported socket properties like number of sockets and protocol specs
   *
   * @return supported socket properties
   */
  virtual SocketProperties GetSocketProperties() = 0;

  /**
   * Register a callback function to receive events from socket HAL
   *
   * @param callback a socket hal callback
   */
  virtual void RegisterCallback(SocketHalCallback* callback) = 0;

  /**
   * Set ACL credits to offload stack for Bluetooth Offload Sockets
   *
   * @param linkType ACL link type between Classic and LE
   * @param credit number of credits to be shared
   * @return true if successfully configured at the offload end point
   */
  virtual bool SetAclCredits(AclLinkType linkType, int credit) = 0;

  /**
   * Notify to offload stack when ACL connection state is changed.
   *
   * @param aclHandle ACL connection handle
   * @param linkType ACL link type between Classic and LE
   * @param state connection state
   * @return true if successfully notified to the offload end point
   */
  virtual bool NotifyAclConnectionStateChange(int aclHandle, AclLinkType linkType,
                                              ConnectionState state) = 0;

  /**
   * Notify to offload stack when LE data length is changed.
   *
   * @param aclHandle ACL connection handle
   * @param txDataLen The maximum number of payload octets that the local Controller will send
   * @param rxDataLen The maximum number of payload octets that the local Controller expects to
   * receive
   * @return true if successfully notified to the offload end point
   */
  virtual bool NotifyAclLeDataLengthChange(int aclHandle, int txDataLen, int rxDataLen) = 0;

  /**
   * Notify to offload stack when ACL power mode is changed
   *
   * @param aclHandle ACL connection handle
   * @param powerMode power mode between active and low power mode
   * @param interval number of baseband slots in low power mode or zero for active power mode
   * @return true if successfully notified to the offload end point
   */
  virtual bool NotifyAclPowerModeChange(int aclHandle, AclPowerMode powerMode, int interval) = 0;

  /**
   * Notify to offload stack adn end point when socket connection state is changed
   *
   * @param SocketContext channel info, connection state, and end point info
   * @return true if successfully notified to the offload end point
   */
  virtual bool NotifySocketConnectionStateChange(const SocketContext& context) = 0;
};

}  // namespace hal
}  // namespace bluetooth
