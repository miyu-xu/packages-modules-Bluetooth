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

enum AclLinkType {
  LT_Classic,
  LT_LE,
};

enum AclPowerMode {
  PM_ACTIVE,
  PM_LOW_POWER,
};

enum SocketProtocol {
  RFCOMM,
  LE_COC,
};

enum ConnectionState {
  ST_DISCONNECTED,
  ST_CONNECTED,
};

enum SocketDataPath {
  OFFLOAD_OFF,
  OFFLOAD_SOFTWARE,
  OFFLOAD_HARDWARE,
};

enum AsyncEventType {
  RESET,
};

enum RequestReason {
  REASON_UNKNOWN,
  REASON_FAILURE,
  REASON_STACK_REQUEST,
  REASON_APP_REQUEST,
};

struct EndPointInfo {
  // The ID of the Context Hub to which the end point belongs.
  int hubId;
  // The ID of end point to which the socket context will be passed.
  int endpointId;
};

struct LeCocSpec {
  // Version number.
  int version;
  // Local maximum transmission unit.
  int mtu;
  // Local maximum PDU payload size.
  int mps;
  // Local initial credit for LE CoC.
  int credit;
};

struct RfcommSpec {
  // Version number.
  int version;
  // Local maximum transmission unit.
  int mtu;
  // Local initial credit for RFCOMM.
  int credit;
};

struct ProtocolProperties {
  union ProtocolSpec {
    LeCocSpec leCocSpec;
    RfcommSpec rfcommSpec;
  };
  SocketProtocol protocol;
  ProtocolSpec protocolSpec;
};

struct SocketProperties {
  SocketDataPath dataPath;
  int numOfLeCocSocketSupported_;
  int numOfRfcommSocketSupported_;
  std::vector<ProtocolProperties> protocolProperties_;
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

struct RfcommChannelInfo {
  // L2cap local channel ID for RFCOMM channel.
  int localL2capCid;
  // L2cap remote channel ID for RFCOMM channel.
  int remoteL2capCid;
  // DLCI for RFCOMM channel.
  int port;
  // Protocol local MTU size.
  int localMtu;
  // Protocol remote MTU size.
  int remoteMtu;
  // Protocol local credit.
  int localCredit;
  // Protocol remote credit.
  int remoteCredit;
  // RFCOMM CR bit
  int cr;
};

struct SocketContext {
  bluetooth::Uuid socketId;
  ConnectionState state;
  std::string name;
  int aclHandle;
  SocketDataPath dataPath;
  SocketProtocol protocol;
  union ProtocolChannelInfo {
    LeCocChannelInfo leCocChannelInfo;
    RfcommChannelInfo rfcommChannelInfo;
  };
  ProtocolChannelInfo channelInfo;
  EndPointInfo endpointInfo;
};

class SocketHalCallback {
public:
  virtual ~SocketHalCallback() = default;
  virtual void onReceiveAsyncEvent(AsyncEventType eventType) = 0;
  virtual void onReceiveSocketCloseRequest(const bluetooth::Uuid& socketId,
                                           RequestReason reason) = 0;
  virtual void onReceiveSocketData(const bluetooth::Uuid& socketId,
                                   const std::vector<uint8_t>& data) = 0;
};

class SocketHalInterface {
public:
  virtual ~SocketHalInterface() = default;
  virtual bool GetSocketProperties(SocketDataPath dataPath, SocketProperties* socketPros) = 0;
  virtual bool SetAclCreditsForSockets(AclLinkType linkType, int credit) = 0;
  virtual bool NotifyAclLeDataLengthChange(int aclHandle, int txDataLen, int rxDataLen) = 0;
  virtual bool NotifySocketConnectionStateChange(const SocketContext& context) = 0;
  virtual bool SendSocketData(const SocketContext& context, const std::vector<uint8_t>& data) = 0;
};

}  // namespace hci
}  // namespace bluetooth
