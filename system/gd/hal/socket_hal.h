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
 * SocketHal provides an interface to low-power processors, enabling Bluetooth Offload Socket
 * functionality.
 *
 * Bluetooth Offload Socket allows the transfer of channel information from an established
 * BluetoothSocket to a low-power processor. This enables the offload stack on the low-power
 * processor to handle packet reception, processing, and transmission independently. This offloading
 * process prevents the need to wake the main application processor, improving power efficiency.
 */
class SocketHal : public ::bluetooth::Module {
public:
  static const ModuleFactory Factory;

  virtual ~SocketHal() = default;

  virtual bool IsBound() = 0;

  /**
   * Register a callback function to receive asynchronous events from socket HAL.
   *
   * @param callback a socket hal callback
   */
  virtual void RegisterCallback(bluetooth::hci::SocketHalCallback* callback) = 0;

  /**
   * Get the supported socket capabilities.
   *
   * @return supported socket capabilities
   */
  virtual bluetooth::hci::SocketCapabilities GetSocketCapabilities() = 0;

  /**
   * Notify the socket HAL that the socket is opened.
   *
   * @param context Socket context including socket id, channel, hub, and endpoint info
   * @return true if successfully notified to the offload endpoint
   */
  virtual bool Opened(const bluetooth::hci::SocketContext& context) = 0;

  /**
   * Notify the socket HAL that the socket is closed.
   *
   * @param socketId Identifier assigned to the socket by the host stack
   */
  virtual void Closed(uint64_t socketId) = 0;
};

}  // namespace hal
}  // namespace bluetooth
