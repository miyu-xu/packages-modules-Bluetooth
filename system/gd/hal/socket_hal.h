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

#include "module.h"
#include "types/bluetooth/uuid.h"

namespace bluetooth {
namespace hal {

enum LinkType {
  LT_Classic,
  LT_LE,
};

enum LinkPowerMode {
  PM_ACTIVE,
  PM_LOW_POWER,
};

enum ConnectionState {
  ST_DISCONNECTED,
  ST_CONNECTED,
};

enum AsyncEventType {
  RESET,
};

struct EndPointInfo {
  // The ID of the Context Hub to which the end point belongs.
  int hubId;
  // The ID of end point to which the socket context will be passed.
  int endpointId;
};

struct LeCocSpec {
  // Version numer.
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
  int protocol;
  ProtocolSpec protocolSpec;
};

struct SocketProperties {
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
  std::string name;
  int protocol;
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
  virtual void onSocketHalEvent(int eventType) = 0;
  virtual void onSocketCloseRequest(bluetooth::Uuid socketId, int reason) = 0;
};

class SocketHal : public ::bluetooth::Module {
public:
  static const ModuleFactory Factory;

  virtual ~SocketHal() = default;
  virtual bool IsBound() = 0;
  virtual SocketProperties GetSocketProperties() = 0;
  virtual void RegisterCallback(SocketHalCallback* callback) = 0;
  virtual void SetAclCredits(int link, int credit) = 0;
  virtual void NotifyAclConnectionStateChange(uint16_t handle, int link, int state) = 0;
  virtual void NotifyAclLeDataLengthChange(uint16_t handle, int txDataLen, int rxDataLen) = 0;
  virtual void NotifyAclPowerModeChange(uint16_t handle, int mode, uint16_t interval) = 0;
  virtual void NotifySocketConnectionStateChange(SocketContext& context, int state) = 0;
};

}  // namespace hal
}  // namespace bluetooth