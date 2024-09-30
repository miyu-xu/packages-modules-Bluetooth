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

enum SocketConnectionState {
  ST_DISCONNECTED,
  ST_CONNECTED,
};

enum SocketDataPath {
  OFFLOAD_OFF,
  OFFLOAD_SOFTWARE,
  OFFLOAD_HARDWARE,
};

enum SocketEventType {
  OPEN_CONFIRM,
  CLOSE_REQUEST,
  RESET_NOTIFY,
};

enum SocketEventReason {
  REASON_UNKNOWN,
  REASON_FAILURE,
  REASON_UNSUPPORTED,
  REASON_STACK_REQUEST,
  REASON_APP_REQUEST,
};

struct SocketEvent {
  SocketEventType event;
  bluetooth::Uuid socketId;
  SocketEventReason reason;
};

struct EndPointInfo {
  // The ID of the Context Hub to which the end point belongs.
  long hubId;
  // The ID of end point to which the socket context will be passed.
  long endpointId;
};

struct LeCocSpec {
  // Local maximum transmission unit.
  int mtu;
  // Local maximum PDU payload size.
  int mps;
  // Local initial credit for LE CoC.
  int credit;
};

struct ProtocolProperties {
  SocketProtocol protocol;
  int numOfSocketSupported;
  union ProtocolSpec {
    LeCocSpec leCocSpec;
  };
  ProtocolSpec protocolSpec;
};

struct SocketProperties {
  SocketDataPath dataPath;
  std::vector<ProtocolProperties> protocolProperties;
};

struct LeCocChannelInfo {
  // L2cap local channel ID for LE COC channel.
  int localL2capCid;
  // L2cap remote channel ID for LE COC channel.
  int remoteL2capCid;
  // PSM for L2CAP LE CoC.
  int psm;
  // Protocol local MTU size.
  int localMtu;
  // Protocol remote MTU size.
  int remoteMtu;
  // Protocol local MPS size.
  int localMps;
  // Protocol remote MPS size.
  int remoteMps;
  // Protocol local credit.
  int localCredit;
  // Protocol remote credit.
  int remoteCredit;
};

struct SocketContext {
  bluetooth::Uuid socketId;
  SocketConnectionState state;
  std::string name;
  int aclHandle;
  SocketDataPath dataPath;
  SocketProtocol protocol;
  union ProtocolChannelInfo {
    LeCocChannelInfo leCocChannelInfo;
  };
  ProtocolChannelInfo channelInfo;
  EndPointInfo endpointInfo;
};

class SocketHalCallback {
public:
  virtual ~SocketHalCallback() = default;
  virtual void onSocketEventReceived(SocketEvent& socketEvent) = 0;
};

class SocketHalInterface {
public:
  virtual ~SocketHalInterface() = default;
  virtual bool GetSocketProperties(SocketDataPath dataPath, SocketProperties* socketPros) = 0;
  virtual bool NotifySocketConnectionStateChange(const SocketContext& context) = 0;
};

}  // namespace hci
}  // namespace bluetooth
