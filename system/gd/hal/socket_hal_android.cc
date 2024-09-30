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

#include <aidl/android/hardware/bluetooth/socket/BnBluetoothSocketCallback.h>
#include <aidl/android/hardware/bluetooth/socket/IBluetoothSocket.h>
#include <aidl/android/hardware/bluetooth/socket/IBluetoothSocketCallback.h>
#include <android/binder_manager.h>
#include <bluetooth/log.h>

// AIDL uses syslog.h, so these defines conflict with os/log.h
#undef LOG_DEBUG
#undef LOG_INFO
#undef LOG_WARNING

#include "hal/socket_hal.h"

using ::aidl::android::hardware::bluetooth::socket::BnBluetoothSocketCallback;
using ::aidl::android::hardware::bluetooth::socket::ConnectionState;
using ::aidl::android::hardware::bluetooth::socket::DataPath;
using ::aidl::android::hardware::bluetooth::socket::IBluetoothSocket;
using ::aidl::android::hardware::bluetooth::socket::ProtocolChannelInfo;
using ::aidl::android::hardware::bluetooth::socket::ProtocolProperties;
using ::aidl::android::hardware::bluetooth::socket::ProtocolSpec;
using ::aidl::android::hardware::bluetooth::socket::ProtocolType;
using ::aidl::android::hardware::bluetooth::socket::SocketContext;
using ::aidl::android::hardware::bluetooth::socket::SocketEvent;
using ::aidl::android::hardware::bluetooth::socket::SocketProperties;

namespace bluetooth {
namespace hal {

const std::vector<hci::SocketProperties> kEmptyOffloadSocketProperties = {
        {.dataPath = hci::SocketDataPath::OFFLOAD_SOFTWARE,
         .protocolProperties = {{
                 .protocol = hci::SocketProtocol::LE_COC,
                 .numOfSocketSupported = 0,
         }}},
        {.dataPath = hci::SocketDataPath::OFFLOAD_HARDWARE,
         .protocolProperties = {{
                 .protocol = hci::SocketProtocol::LE_COC,
                 .numOfSocketSupported = 0,
         }}}};

class BluetoothSocketCallback : public BnBluetoothSocketCallback {
public:
  BluetoothSocketCallback(hci::SocketHalCallback* socket_hal_cb) : socket_hal_cb_(socket_hal_cb) {}

  ::ndk::ScopedAStatus onSocketEventReceived(const SocketEvent& in_event) override {
    hci::SocketEvent event_return = {
            .event = static_cast<hci::SocketEventType>(in_event.event),
            .socketId = Uuid::From128BitBE(in_event.socketId.uuid),
            .reason = static_cast<hci::SocketEventReason>(in_event.reason),
    };
    socket_hal_cb_->onSocketEventReceived(event_return);
    return ::ndk::ScopedAStatus::ok();
  }

private:
  hci::SocketHalCallback* socket_hal_cb_;
};

class SocketHalAndroid : public SocketHal {
public:
  bool IsBound() override { return bluetooth_socket_ != nullptr; }

protected:
  void ListDependencies(ModuleList* /*list*/) const {}

  void Start() override {
    std::string instance = std::string() + IBluetoothSocket::descriptor + "/default";
    log::info("AServiceManager_isDeclared {}", AServiceManager_isDeclared(instance.c_str()));
    if (AServiceManager_isDeclared(instance.c_str())) {
      ::ndk::SpAIBinder binder(AServiceManager_waitForService(instance.c_str()));
      bluetooth_socket_ = IBluetoothSocket::fromBinder(binder);
      log::info("Bind IBluetoothSocket {}", IsBound() ? "Success" : "Fail");
    }
  }

  void Stop() override { bluetooth_socket_ = nullptr; }

  std::string ToString() const override { return std::string("SocketHalAndroid"); }

  std::vector<hci::SocketProperties> GetSocketProperties() override {
    std::optional<std::vector<std::optional<SocketProperties>>> socket_properties;
    ::ndk::ScopedAStatus status = bluetooth_socket_->getSocketProperties(&socket_properties);
    if (!status.isOk() || !socket_properties.has_value()) {
      return {kEmptyOffloadSocketProperties};
    }
    std::vector<hci::SocketProperties> props_return = {};
    for (auto& socket_entry : socket_properties.value()) {
      hci::SocketProperties socket_prop;
      socket_prop.dataPath = static_cast<hci::SocketDataPath>(socket_entry->dataPath);
      for (auto& protocol_entry : socket_entry->protocolProperties) {
        hci::ProtocolProperties protocol_prop;
        protocol_prop.protocol = static_cast<hci::SocketProtocol>(protocol_entry.protocol);
        protocol_prop.numOfSocketSupported = protocol_entry.numOfSocketSupported;
        if (protocol_prop.protocol == hci::SocketProtocol::LE_COC) {
          protocol_prop.protocolSpec.leCocSpec.mtu =
                  protocol_entry.protocolSpec.get<ProtocolSpec::leCocSpec>().mtu;
          protocol_prop.protocolSpec.leCocSpec.mps =
                  protocol_entry.protocolSpec.get<ProtocolSpec::leCocSpec>().mps;
          protocol_prop.protocolSpec.leCocSpec.credit =
                  protocol_entry.protocolSpec.get<ProtocolSpec::leCocSpec>().credit;
          socket_prop.protocolProperties.emplace_back(std::move(protocol_prop));
        }
      }
      props_return.emplace_back(std::move(socket_prop));
    }
    return props_return;
  }

  void RegisterCallback(hci::SocketHalCallback* in_callback) override {
    bluetooth_socket_cb_ = ndk::SharedRefBase::make<BluetoothSocketCallback>(in_callback);
    ::ndk::ScopedAStatus status = bluetooth_socket_->initialize(bluetooth_socket_cb_);
    if (!status.isOk()) {
      log::error("initialize failure: {}", status.getDescription());
    }
  }

  bool NotifySocketConnectionStateChange(const hci::SocketContext& in_context) override {
    log::info("socketId {}, state {}", in_context.socketId, static_cast<int>(in_context.state));
    SocketContext context;
    context.socketId = toAidlUuid(in_context.socketId.To128BitLE());
    context.state = static_cast<ConnectionState>(in_context.state);
    context.name = in_context.name;
    context.aclHandle = in_context.aclHandle;
    context.dataPath = static_cast<DataPath>(in_context.dataPath);
    context.protocol = static_cast<ProtocolType>(in_context.protocol);
    if (in_context.protocol == hci::SocketProtocol::LE_COC) {
      auto& in_l2cap_context = in_context.channelInfo.leCocChannelInfo;
      auto& l2cap_context = context.channelInfo.get<ProtocolChannelInfo::leCocChannelInfo>();
      l2cap_context.localL2capCid = in_l2cap_context.localL2capCid;
      l2cap_context.remoteL2capCid = in_l2cap_context.remoteL2capCid;
      l2cap_context.psm = in_l2cap_context.psm;
      l2cap_context.localMtu = in_l2cap_context.localMtu;
      l2cap_context.remoteMtu = in_l2cap_context.remoteMtu;
      l2cap_context.localMps = in_l2cap_context.localMps;
      l2cap_context.remoteMps = in_l2cap_context.remoteMps;
      l2cap_context.localCredit = in_l2cap_context.localCredit;
      l2cap_context.remoteCredit = in_l2cap_context.remoteCredit;
    }
    context.endPointInfo.hubId = in_context.endpointInfo.hubId;
    context.endPointInfo.endPointId = in_context.endpointInfo.endpointId;

    ::ndk::ScopedAStatus status = bluetooth_socket_->notifySocketConnectionStateChange(context);
    if (!status.isOk()) {
      log::error("notifySocketConnectionStateChange failure: {}", status.getDescription());
      return false;
    }
    return true;
  }

  ::aidl::android::hardware::bluetooth::socket::Uuid toAidlUuid(const Uuid::UUID128Bit& in_uuid) {
    std::array<uint8_t, Uuid::kNumBytes128> a;
    std::copy_n(in_uuid.begin(), a.size(), a.begin());
    return {a};
  }

private:
  std::shared_ptr<IBluetoothSocket> bluetooth_socket_;
  std::shared_ptr<BluetoothSocketCallback> bluetooth_socket_cb_;
};

const ModuleFactory SocketHal::Factory = ModuleFactory([]() { return new SocketHalAndroid(); });

}  // namespace hal
}  // namespace bluetooth
