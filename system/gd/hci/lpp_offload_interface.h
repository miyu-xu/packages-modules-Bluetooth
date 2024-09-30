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

#include "lpp_offload_socket_interface.h"

namespace bluetooth {
namespace hci {

struct LppOffloadFeatures {
  bool socket_sw_offload_supported;
  bool socket_hw_offload_supported;
};

/**
 * Callbacks to notify upper layers of events or requests received from the offload domain. It can
 * inherit multiple offload HAL callbacks, allowing higher layers to implement callback handlers in
 * the same place.
 */
class LppOffloadCallbacks : public SocketHalCallback {
public:
  virtual ~LppOffloadCallbacks() = default;

  /**
   * This callback is to allow HAL to send an asynchronous event to host stack.
   *
   * @param eventType asynchronous event type
   */
  virtual void onReceiveAsyncEvent(AsyncEventType eventType) = 0;

  /**
   * This callback is to request host stack to close the socket.
   *
   * @param socketId socket identifier to be closed
   * @param reason request reason to close the socket
   */
  virtual void onReceiveSocketCloseRequest(const bluetooth::Uuid& socketId,
                                           RequestReason reason) = 0;

  /**
   * This callback is to send socket data from HAL to host stack. The socket should be configured
   * for OFFLOAD_SOFTWARE data path.
   *
   * @param socketId socket identifier to be closed
   * @param data socket data to be passed to host stack
   */
  virtual void onReceiveSocketData(const bluetooth::Uuid& socketId,
                                   const std::vector<uint8_t>& data) = 0;
};

/**
 * Interface to low-power processors to support LPP offload
 * features. This allows multiple offload HAL interfaces to be inherited and a single offload
 * interface to be called from the upper layer to support unified offload function management.
 */
class LppOffloadInterface : public SocketHalInterface {
public:
  LppOffloadInterface() = default;

  virtual ~LppOffloadInterface() = default;

  LppOffloadInterface(const LppOffloadInterface&) = delete;

  LppOffloadInterface& operator=(const LppOffloadInterface&) = delete;

  /**
   * Register a callback function to receive events from LPP offload HALs
   *
   * @param callback LPP offload hal callback
   */
  virtual void RegisterLppOffloadCallbacks(LppOffloadCallbacks* callbacks) = 0;

  /**
   * Get offload features available from LPP offload HALs
   *
   * @param features pointer to get LPP offload features
   */
  virtual void GetOffloadFeaturesSupported(LppOffloadFeatures* features) = 0;

  /**
   * Get the supported socket properties like number of sockets and protocol specs
   *
   * @param dataPath socket data path
   * @param socketPros pointer to get socket properties
   * @return true if successfully found socket properties for the data path and set it
   */
  virtual bool GetSocketProperties(SocketDataPath dataPath, SocketProperties* socketPros) = 0;

  /**
   * Set ACL credits to offload stack for Bluetooth Offload Sockets
   *
   * @param linkType ACL link type between Classic and LE
   * @param credit number of credits to provide to the offload stack
   * @return true if successfully configured at the offload end point
   */
  virtual bool SetAclCreditsForSockets(AclLinkType linkType, int credit) = 0;

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
   * Notify to offload stack and end point when socket connection state is changed
   *
   * @param SocketContext channel info, connection state, data path, and end point info
   * @return true if successfully notified to the offload end point
   */
  virtual bool NotifySocketConnectionStateChange(const SocketContext& context) = 0;

  /**
   * Send socket data from host stack to HAL. The socket should be configured for OFFLOAD_SOFTWARE
   * data path.
   *
   * @param SocketContext channel info, connection state, data path, and end point info
   * @param data socket data to be passed to HAL
   * @return true if successfully sent to HAL
   */
  virtual bool SendSocketData(const SocketContext& context, const std::vector<uint8_t>& data) = 0;
};

}  // namespace hci
}  // namespace bluetooth
