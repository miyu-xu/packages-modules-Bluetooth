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
  bool socket_offload_supported;
  uint8_t max_le_coc_socket_num;
  uint8_t max_rfcomm_socket_num;
};

/**
 * Callbacks to notify upper layers of events or requests received from the offload domain. It can
 * inherit multiple offload HAL callbacks, allowing higher layers to implement callback handlers in
 * the same place.
 */
class LppOffloadCallbacks : public SocketHalCallback {
public:
  virtual ~LppOffloadCallbacks() = default;
  virtual void onReceiveAsyncEvent(AsyncEventType eventType) = 0;
  virtual void onReceiveSocketCloseRequest(bluetooth::Uuid socketId, RequestReason reason) = 0;
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
   * Set ACL credits to offload stack for Bluetooth Offload Sockets
   *
   * @param linkType ACL link type between Classic and LE
   * @param credit number of credits to provide to the offload stack
   * @return true if successfully configured at the offload end point
   */
  virtual bool SetAclCreditsForSockets(AclLinkType linkType, int credit) = 0;

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
   * Notify to offload stack and end point when socket connection state is changed
   *
   * @param SocketContext channel info, connection state, and end point info
   * @return true if successfully notified to the offload end point
   */
  virtual bool NotifySocketConnectionStateChange(const SocketContext& context) = 0;
};

}  // namespace hci
}  // namespace bluetooth
