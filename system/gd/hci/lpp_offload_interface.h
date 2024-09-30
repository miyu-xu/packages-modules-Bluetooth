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
  bool is_socket_hw_offload_supported;
};

/**
 * Callbacks to notify upper layers of events or requests originating from the offload HAL.
 *
 * This class can inherit from multiple offload HAL callback interfaces, allowing higher layers to
 * consolidate callback handlers in a single location.
 */
class LppOffloadCallbacks : public SocketHalCallback {
public:
  virtual ~LppOffloadCallbacks() = default;

  /**
   * Invoked when LppOffloadInterface.SocketOpened() has been completed.
   *
   * @param socketId Identifier assigned to the socket by the host stack
   * @param status Status indicating success or failure
   */
  virtual void SocketOpenedComplete(uint64_t socketId, SocketStatus status) = 0;

  /**
   * Invoked when socket HAL requests host stack to close the socket.
   *
   * @param socketId Identifier assigned to the socket by the host stack
   */
  virtual void SocketClose(uint64_t socketId) = 0;
};

/**
 * Interface to low-power processors (LPPs) for supporting LPP offload features.
 *
 * This interface allows inheritance from multiple offload HAL interfaces, enabling a unified
 * offload function management approach through a single interface accessible from the upper layer.
 */
class LppOffloadInterface : public SocketHalInterface {
public:
  LppOffloadInterface() = default;

  virtual ~LppOffloadInterface() = default;

  LppOffloadInterface(const LppOffloadInterface&) = delete;

  LppOffloadInterface& operator=(const LppOffloadInterface&) = delete;

  /**
   * Registers a callback function to receive asynchronous events from LPP offload HALs.
   *
   * @param callback LPP offload hal callback
   */
  virtual void RegisterLppOffloadCallbacks(LppOffloadCallbacks* callbacks) = 0;

  /**
   * Retrieves the offload features available from LPP offload HALs.
   *
   * @param features pointer to get LPP offload features
   */
  virtual void GetOffloadFeaturesSupported(LppOffloadFeatures* features) = 0;

  /**
   * Retrieves the supported offload socket capabilities.
   *
   * @param socketCapabilities Pointer to retrieve the socket capabilities
   */
  virtual void GetSocketCapabilities(SocketCapabilities* socketCapabilities) = 0;

  /**
   * Notifies the socket HAL that the socket has been opened.
   *
   * If this method returns true, SocketHalCallback.SocketOpenedComplete() must be called to
   * indicate the result of this operation.
   *
   * @param context Socket context including socket ID, channel, hub, and endpoint info
   * @return Result of calling this method
   */
  virtual bool SocketOpened(const SocketContext& context) = 0;

  /**
   * Notifies the socket HAL that the socket has been closed.
   *
   * @param socketId Identifier assigned to the socket by the host stack
   */
  virtual void SocketClosed(uint64_t socketId) = 0;
};

}  // namespace hci
}  // namespace bluetooth
