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
   * This callback is to allow HAL to send an asynchronous socket event to host stack.
   *
   * @param socketEvent asynchronous socket event
   */
  virtual void onSocketEventReceived(SocketEvent& socketEvent) = 0;
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
   * Notify to offload stack and end point when socket connection state is changed
   *
   * @param SocketContext channel info, connection state, data path, and end point info
   * @return true if successfully notified to the offload end point
   */
  virtual bool NotifySocketConnectionStateChange(const SocketContext& context) = 0;
};

}  // namespace hci
}  // namespace bluetooth
