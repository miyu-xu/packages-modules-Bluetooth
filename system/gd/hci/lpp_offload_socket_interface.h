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
#include <string>
#include <vector>

#include "types/bluetooth/uuid.h"

namespace bluetooth {
namespace hci {

enum SocketProtocol {
  LE_COC,
};

enum SocketStatus {
  SUCCESS,
  FAILURE,
};

struct EndpointInfo {
  // The ID of the Hub to which the end point belongs for hardware offload data path.
  uint64_t hubId;

  //  The ID of the Hub endpoint for hardware offload data path.
  uint64_t endpointId;
};

struct LeCocCapabilities {
  // Maximum number of LE COC sockets supported. If not supported, the value must be zero.
  int numberOfSupportedSockets;

  // Local Maximum Transmission Unit size in octets.
  int mtu;
};

struct SocketCapabilities {
  LeCocCapabilities leCocCapabilities;
};

struct LeCocChannelInfo {
  // L2cap local channel ID.
  int localCid;

  // L2cap remote channel ID.
  int remoteCid;

  // PSM for L2CAP LE CoC.
  int psm;

  // Local Maximum Transmission Unit for LE COC specifying the maximum SDU size in bytes that the
  // local L2CAP layer can receive.
  int localMtu;

  // Remote Maximum Transmission Unit for LE COC specifying the maximum SDU size in bytes that the
  // remote L2CAP layer can receive.
  int remoteMtu;

  // Local Maximum PDU payload Size in bytes that the local L2CAP layer can receive.
  int localMps;

  // Remote Maximum PDU payload Size in bytes that the remote L2CAP layer can receive.
  int remoteMps;

  // Protocol initial credits at Rx path.
  int initialRxCredits;

  // Protocol initial credits at Tx path.
  int initialTxCredits;
};

struct SocketContext {
  // Identifier assigned to the socket by the host stack when the socket is connected.
  uint64_t socketId;

  // Descriptive socket name provided by the host app when it creates this socket.
  std::string name;

  // ACL connection handle for the socket.
  int aclConnectionHandle;

  // Protocol used for the socket.
  SocketProtocol protocol;

  // Used to specify the channel information of different protocol.
  union ChannelInfo {
    LeCocChannelInfo leCocChannelInfo;
  };
  ChannelInfo channelInfo;

  // Endpoint information.
  EndpointInfo endpointInfo;
};

class SocketHalCallback {
public:
  virtual ~SocketHalCallback() = default;

  /**
   * Invoked when IBluetoothSocket.opened() has been completed.
   *
   * @param socketId Identifier assigned to the socket by the host stack
   * @param status Status indicating success or failure
   */
  virtual void SocketOpenedComplete(uint64_t socketId, SocketStatus status) = 0;

  /**
   * Invoked when offload app or stack requests host stack to close the socket.
   *
   * @param socketId Identifier assigned to the socket by the host stack
   */
  virtual void SocketClose(uint64_t socketId) = 0;
};

class SocketHalInterface {
public:
  virtual ~SocketHalInterface() = default;

  /**
   * Retrieves the supported offload socket capabilities.
   *
   * @param socketCapabilities Pointer to retrieve the socket capabilities
   */
  virtual void GetSocketCapabilities(SocketCapabilities* socketCapabilities) = 0;

  /**
   * Notify the socket HAL that the socket is opened.
   *
   * If this method returns true, SocketHalCallback.SocketOpenedComplete() must be called to
   * indicate the result of this operation
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
