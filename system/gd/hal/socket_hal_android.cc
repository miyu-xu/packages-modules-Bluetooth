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

// syslog.h conflicts with libchrome/base/logging.h
#undef LOG_DEBUG
#undef LOG_INFO
#undef LOG_WARNING

#include "hal/socket_hal.h"

using ::aidl::android::hardware::bluetooth::socket::BnBluetoothSocketCallback;
using ::aidl::android::hardware::bluetooth::socket::IBluetoothSocket;

namespace bluetooth::hal {

const hal::SocketCapabilities kEmptySocketCapabilities = {};

class SocketAidlCallback : public BnBluetoothSocketCallback {
  class : public hal::SocketHalCallback {
  public:
    void SocketOpenedComplete(uint64_t /* socket_id */, hal::SocketStatus /* status */) override {
      log::warn("Dropping SocketOpenedComplete event, since callback is not set");
    }

    void SocketClose(uint64_t /* socket_id */) override {
      log::warn("Dropping SocketClose event, since callback is not set");
    }
  } kNullCallbacks;

public:
  SocketAidlCallback() = default;

  void SetCallback(hal::SocketHalCallback* callback) {
    log::assert_that(callback != nullptr, "callback != nullptr");
    socket_hal_cb_ = callback;
  }

  ::ndk::ScopedAStatus openedComplete(int64_t socket_id,
                                      ::aidl::android::hardware::bluetooth::socket::Status status,
                                      const std::string& reason) override {
    log::info("socket_id: {} status: {} reason: {}", socket_id, static_cast<int>(status), reason);
    if (status == ::aidl::android::hardware::bluetooth::socket::Status::SUCCESS) {
      socket_hal_cb_->SocketOpenedComplete(socket_id, hal::SocketStatus::SUCCESS);
    } else {
      socket_hal_cb_->SocketOpenedComplete(socket_id, hal::SocketStatus::FAILURE);
    }
    return ::ndk::ScopedAStatus::ok();
  }

  ::ndk::ScopedAStatus close(int64_t socket_id, const std::string& reason) override {
    log::info("socket_id: {} reason: {}", socket_id, reason);
    socket_hal_cb_->SocketClose(socket_id);
    return ::ndk::ScopedAStatus::ok();
  }

private:
  hal::SocketHalCallback* socket_hal_cb_ = &kNullCallbacks;
};

class SocketHalAndroid : public SocketHal {
public:
  bool IsBound() override { return socket_hal_instance_ != nullptr; }

protected:
  void ListDependencies(ModuleList* /*list*/) const {}

  void Start() override {
    std::string instance = std::string() + IBluetoothSocket::descriptor + "/default";
    if (AServiceManager_isDeclared(instance.c_str())) {
      ::ndk::SpAIBinder binder(AServiceManager_waitForService(instance.c_str()));
      socket_hal_instance_ = IBluetoothSocket::fromBinder(binder);
    }
    log::info("Bind IBluetoothSocket: {}", IsBound() ? "Success" : "Fail");
    if (!IsBound()) {
      return;
    }

    socket_aidl_cb_ = ndk::SharedRefBase::make<SocketAidlCallback>();
    ::ndk::ScopedAStatus status = socket_hal_instance_->registerCallback(socket_aidl_cb_);
    if (!status.isOk()) {
      log::error("registerCallback failure: {}", status.getDescription());
      Stop();
      return;
    }

    death_recipient_ =
            ::ndk::ScopedAIBinder_DeathRecipient(AIBinder_DeathRecipient_new([](void* /* cookie*/) {
              log::error("The Socket HAL service died.");
              // At shutdown, sometimes the HAL service gets killed before Bluetooth.
              std::this_thread::sleep_for(std::chrono::seconds(1));
              log::fatal("The Socket HAL died.");
            }));

    auto death_link = AIBinder_linkToDeath(socket_hal_instance_->asBinder().get(),
                                           death_recipient_.get(), this);
    log::assert_that(death_link == STATUS_OK,
                     "Unable to set the death recipient for the Socket HAL");
  }

  void Stop() override {
    if (IsBound()) {
      auto death_unlink = AIBinder_unlinkToDeath(socket_hal_instance_->asBinder().get(),
                                                 death_recipient_.get(), this);
      if (death_unlink != STATUS_OK) {
        log::error("Error unlinking death recipient from the Socket HAL");
      }
      socket_hal_instance_ = nullptr;
    }
  }

  std::string ToString() const override { return std::string("SocketHalAndroid"); }

  hal::SocketCapabilities GetSocketCapabilities() override {
    if (!IsBound()) {
      return kEmptySocketCapabilities;
    }
    ::aidl::android::hardware::bluetooth::socket::SocketCapabilities socket_capabilities;
    ::ndk::ScopedAStatus status = socket_hal_instance_->getSocketCapabilities(&socket_capabilities);
    if (!status.isOk()) {
      log::info("Failed to get socket capabilities");
      return kEmptySocketCapabilities;
    }
    log::info("le_coc_capabilities number_of_supported_sockets: {}, mtu: {}",
              socket_capabilities.leCocCapabilities.numberOfSupportedSockets,
              socket_capabilities.leCocCapabilities.mtu);
    return hal::SocketCapabilities{
            .le_coc_capabilities.number_of_supported_sockets =
                    socket_capabilities.leCocCapabilities.numberOfSupportedSockets,
            .le_coc_capabilities.mtu =
                    static_cast<uint16_t>(socket_capabilities.leCocCapabilities.mtu)};
  }

  bool RegisterCallback(hal::SocketHalCallback* callback) override {
    if (!IsBound()) {
      return false;
    }
    socket_aidl_cb_->SetCallback(callback);
    return true;
  }

  bool Opened(const hal::SocketContext& context) override {
    if (!IsBound()) {
      return false;
    }
    log::info("socket_id: {}, name: {}, acl_connection_handle: {}, hub_id: {}, endpoint_id: {}",
              context.socket_id, context.name, context.acl_connection_handle,
              context.endpoint_info.hub_id, context.endpoint_info.endpoint_id);
    ::aidl::android::hardware::bluetooth::socket::SocketContext hal_context = {
            .socketId = static_cast<int64_t>(context.socket_id),
            .name = context.name,
            .aclConnectionHandle = context.acl_connection_handle,
            .hubId = static_cast<int64_t>(context.endpoint_info.hub_id),
            .endpointId = static_cast<int64_t>(context.endpoint_info.endpoint_id),
    };
    if (context.protocol == hal::SocketProtocol::LE_COC) {
      auto& le_coc_context = context.channel_info.le_coc_channel_info;
      auto& le_coc_hal_context = hal_context.channelInfo.get<
              ::aidl::android::hardware::bluetooth::socket::ChannelInfo::leCocChannelInfo>();
      le_coc_hal_context.localCid = le_coc_context.local_cid;
      le_coc_hal_context.remoteCid = le_coc_context.remote_cid;
      le_coc_hal_context.psm = le_coc_context.psm;
      le_coc_hal_context.localMtu = le_coc_context.local_mtu;
      le_coc_hal_context.remoteMtu = le_coc_context.remote_mtu;
      le_coc_hal_context.localMps = le_coc_context.local_mps;
      le_coc_hal_context.remoteMps = le_coc_context.remote_mps;
      le_coc_hal_context.initialRxCredits = le_coc_context.initial_rx_credits;
      le_coc_hal_context.initialTxCredits = le_coc_context.initial_tx_credits;
      log::info(
              "le_coc local_cid: {}, remote_cid: {}, psm: {}, local_mtu: {}, remote_mtu: {}, "
              "local_mps: {}, remote_mps: {}, initial_rx_credits: {}, initial_tx_credits: {}",
              le_coc_context.local_cid, le_coc_context.remote_cid, le_coc_context.psm,
              le_coc_context.local_mtu, le_coc_context.remote_mtu, le_coc_context.local_mps,
              le_coc_context.remote_mps, le_coc_context.initial_rx_credits,
              le_coc_context.initial_tx_credits);
    } else {
      log::error("Unsupported protocol: {}", static_cast<int>(context.protocol));
      return false;
    }
    ::ndk::ScopedAStatus status = socket_hal_instance_->opened(hal_context);
    if (!status.isOk()) {
      log::error("Opened failure: {}", status.getDescription());
      return false;
    }
    return true;
  }

  void Closed(uint64_t socket_id) override {
    if (!IsBound()) {
      return;
    }
    log::info("socket_id: {}", socket_id);
    ::ndk::ScopedAStatus status = socket_hal_instance_->closed(socket_id);
    if (!status.isOk()) {
      log::info("Closed failure: {}", status.getDescription());
    }
  }

private:
  std::shared_ptr<IBluetoothSocket> socket_hal_instance_;
  std::shared_ptr<SocketAidlCallback> socket_aidl_cb_;
  ::ndk::ScopedAIBinder_DeathRecipient death_recipient_;
};

const ModuleFactory SocketHal::Factory = ModuleFactory([]() { return new SocketHalAndroid(); });

}  // namespace bluetooth::hal
