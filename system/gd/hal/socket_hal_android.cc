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
using ::aidl::android::hardware::bluetooth::socket::ChannelInfo;
using ::aidl::android::hardware::bluetooth::socket::IBluetoothSocket;
using ::aidl::android::hardware::bluetooth::socket::SocketCapabilities;
using ::aidl::android::hardware::bluetooth::socket::SocketContext;

namespace bluetooth {
namespace hal {

const hci::SocketCapabilities kEmptySocketCapabilities = {};

class BluetoothSocketCallback : public BnBluetoothSocketCallback {
public:
  BluetoothSocketCallback(hci::SocketHalCallback* in_callback) : socket_hal_cb_(in_callback) {}

  ::ndk::ScopedAStatus openedComplete(
          int64_t in_socketId, ::aidl::android::hardware::bluetooth::socket::Status in_status,
          const std::string& in_reason) override {
    log::info("socketId {} status {} reason {}", in_socketId, static_cast<int>(in_status),
              in_reason);
    socket_hal_cb_->SocketOpenedComplete(in_socketId, static_cast<hci::SocketStatus>(in_status));
    return ::ndk::ScopedAStatus::ok();
  }

  ::ndk::ScopedAStatus close(int64_t in_socketId, const std::string& in_reason) override {
    log::info("socketId {} reason {}", in_socketId, in_reason);
    socket_hal_cb_->SocketClose(in_socketId);
    return ::ndk::ScopedAStatus::ok();
  }

private:
  hci::SocketHalCallback* socket_hal_cb_;
};

class SocketHalAndroid : public SocketHal {
public:
  bool IsBound() override { return bluetooth_socket_hal_ != nullptr; }

protected:
  void ListDependencies(ModuleList* /*list*/) const {}

  void Start() override {
    std::string instance = std::string() + IBluetoothSocket::descriptor + "/default";
    log::info("AServiceManager_isDeclared {}", AServiceManager_isDeclared(instance.c_str()));
    if (AServiceManager_isDeclared(instance.c_str())) {
      ::ndk::SpAIBinder binder(AServiceManager_waitForService(instance.c_str()));
      bluetooth_socket_hal_ = IBluetoothSocket::fromBinder(binder);
      log::info("Bind IBluetoothSocket {}", IsBound() ? "Success" : "Fail");
    }
  }

  void Stop() override { bluetooth_socket_hal_ = nullptr; }

  std::string ToString() const override { return std::string("SocketHalAndroid"); }

  hci::SocketCapabilities GetSocketCapabilities() override {
    SocketCapabilities socket_capabilities;
    ::ndk::ScopedAStatus status =
            bluetooth_socket_hal_->getSocketCapabilities(&socket_capabilities);
    if (!status.isOk()) {
      return kEmptySocketCapabilities;
    }
    hci::SocketCapabilities capabilities_return = {};
    capabilities_return.leCocCapabilities.numberOfSupportedSockets =
            socket_capabilities.leCocCapabilities.numberOfSupportedSockets;
    capabilities_return.leCocCapabilities.mtu = socket_capabilities.leCocCapabilities.mtu;
    return capabilities_return;
  }

  void RegisterCallback(hci::SocketHalCallback* in_callback) override {
    bluetooth_socket_hal_callback_ = ndk::SharedRefBase::make<BluetoothSocketCallback>(in_callback);
    ::ndk::ScopedAStatus status =
            bluetooth_socket_hal_->registerCallback(bluetooth_socket_hal_callback_);
    if (!status.isOk()) {
      log::error("registerCallback failure: {}", status.getDescription());
    }
  }

  bool Opened(const hci::SocketContext& in_context) override {
    log::info("socketId {}, name {}, aclConnectionHandle {}, hubId {}, endpointId {}",
              in_context.socketId, in_context.name, in_context.aclConnectionHandle,
              in_context.endpointInfo.hubId, in_context.endpointInfo.endpointId);
    SocketContext hal_context;
    hal_context.socketId = in_context.socketId;
    hal_context.name = in_context.name;
    hal_context.aclConnectionHandle = in_context.aclConnectionHandle;
    hal_context.hubId = in_context.endpointInfo.hubId;
    hal_context.endpointId = in_context.endpointInfo.endpointId;
    if (in_context.protocol == hci::SocketProtocol::LE_COC) {
      auto& in_le_coc_context = in_context.channelInfo.leCocChannelInfo;
      auto& le_coc_hal_context = hal_context.channelInfo.get<ChannelInfo::leCocChannelInfo>();
      le_coc_hal_context.localCid = in_le_coc_context.localCid;
      le_coc_hal_context.remoteCid = in_le_coc_context.remoteCid;
      le_coc_hal_context.psm = in_le_coc_context.psm;
      le_coc_hal_context.localMtu = in_le_coc_context.localMtu;
      le_coc_hal_context.remoteMtu = in_le_coc_context.remoteMtu;
      le_coc_hal_context.localMps = in_le_coc_context.localMps;
      le_coc_hal_context.remoteMps = in_le_coc_context.remoteMps;
      le_coc_hal_context.initialRxCredits = in_le_coc_context.initialRxCredits;
      le_coc_hal_context.initialTxCredits = in_le_coc_context.initialTxCredits;
    } else {
      log::error("Unsupported protocol {}", static_cast<int>(in_context.protocol));
      return false;
    }
    ::ndk::ScopedAStatus status = bluetooth_socket_hal_->opened(hal_context);
    if (!status.isOk()) {
      log::error("Opened failure: {}", status.getDescription());
      return false;
    }
    return true;
  }

  void Closed(uint64_t socketId) {
    log::info("socketId {}", socketId);
    ::ndk::ScopedAStatus status = bluetooth_socket_hal_->closed(socketId);
    if (!status.isOk()) {
      log::info("Closed failure: {}", status.getDescription());
    }
  }

private:
  std::shared_ptr<IBluetoothSocket> bluetooth_socket_hal_;
  std::shared_ptr<BluetoothSocketCallback> bluetooth_socket_hal_callback_;
};

const ModuleFactory SocketHal::Factory = ModuleFactory([]() { return new SocketHalAndroid(); });

}  // namespace hal
}  // namespace bluetooth
