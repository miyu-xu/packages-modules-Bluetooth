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
  virtual std::vector<bluetooth::hci::SocketProperties> GetSocketProperties() = 0;

  /**
   * Register a callback function to receive events from socket HAL
   *
   * @param callback a socket hal callback
   */
  virtual void RegisterCallback(bluetooth::hci::SocketHalCallback* callback) = 0;

  /**
   * Notify to offload stack and end point when socket connection state is changed
   *
   * @param SocketContext channel info, connection state, data path, and end point info
   * @return true if successfully notified to the offload end point
   */
  virtual bool NotifySocketConnectionStateChange(const bluetooth::hci::SocketContext& context) = 0;
};

}  // namespace hal
}  // namespace bluetooth
