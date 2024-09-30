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
#include "hci/lpp_offload_manager.h"

#include <bluetooth/log.h>

#include <string>

#include "hal/socket_hal.h"
#include "module.h"
#include "os/handler.h"
#include "os/system_properties.h"

namespace bluetooth {
namespace hci {

const ModuleFactory LppOffloadManager::Factory =
        ModuleFactory([]() { return new LppOffloadManager(); });

struct LppOffloadManager::impl : SocketHalCallback {
  ~impl() {}

  void start(os::Handler* handler, hal::SocketHal* socket_hal) {
    log::info("");
    handler_ = handler;
    socket_hal_ = socket_hal;
    init();
  }

  void stop() { log::info(""); }

  void init() {
    is_lpp_offload_enabled_ =
            os::GetSystemPropertyBool("persist.bluetooth.lpp_offload.enabled", false);
    log::info("is_lpp_offload_enabled_ {}", is_lpp_offload_enabled_);
    if (!is_lpp_offload_enabled_) {
      return;
    }
    socket_prop_ = socket_hal_->GetSocketProperties();
    update_offload_features_supported();
  }

  void register_lpp_offload_callbacks(LppOffloadCallbacks* callbacks) {
    if (!is_lpp_offload_enabled_) {
      return;
    }
    log::info("");
    lpp_offload_callbacks_ = callbacks;
  }

  void get_offload_features_supported(LppOffloadFeatures* features) {
    if (!is_lpp_offload_enabled_ || features == nullptr) {
      return;
    }
    log::info("");
    *features = features_;
  }

  void update_offload_features_supported() {
    if (!is_lpp_offload_enabled_) {
      return;
    }
    log::info("");
    features_.socket_offload_supported = (socket_prop_.numOfLeCocSocketSupported_ > 0 ||
                                          socket_prop_.numOfRfcommSocketSupported_ > 0);
    features_.max_le_coc_socket_num = socket_prop_.numOfLeCocSocketSupported_;
    features_.max_rfcomm_socket_num = socket_prop_.numOfRfcommSocketSupported_;
  }

  bool set_acl_credits_for_sockets(AclLinkType linkType, int credit) {
    if (!is_lpp_offload_enabled_) {
      return false;
    }
    log::info("");
    return socket_hal_->SetAclCredits(linkType, credit);
  }

  bool notify_acl_connection_state_change(int aclHandle, AclLinkType linkType,
                                          ConnectionState state) {
    if (!is_lpp_offload_enabled_) {
      return false;
    }
    log::info("");
    return socket_hal_->NotifyAclConnectionStateChange(aclHandle, linkType, state);
  }

  bool notify_acl_le_data_length_change(int aclHandle, int txDataLen, int rxDataLen) {
    if (!is_lpp_offload_enabled_) {
      return false;
    }
    log::info("");
    return socket_hal_->NotifyAclLeDataLengthChange(aclHandle, txDataLen, rxDataLen);
  }

  bool notify_acl_pm_change(int aclHandle, AclPowerMode powerMode, int interval) {
    if (!is_lpp_offload_enabled_) {
      return false;
    }
    log::info("");
    return socket_hal_->NotifyAclPowerModeChange(aclHandle, powerMode, interval);
  }

  bool notify_socket_connection_state_change(const SocketContext& context) {
    if (!is_lpp_offload_enabled_) {
      return false;
    }
    log::info("");
    return socket_hal_->NotifySocketConnectionStateChange(context);
  }

  // Implements SocketHalCallback
  void onReceiveAsyncEvent(AsyncEventType eventType) {
    if (!is_lpp_offload_enabled_) {
      return;
    }
    log::info("");
    if (lpp_offload_callbacks_ != nullptr) {
      lpp_offload_callbacks_->onReceiveAsyncEvent(eventType);
    }
  }

  // Implements SocketHalCallback
  void onReceiveSocketCloseRequest(bluetooth::Uuid socketId, RequestReason reason) {
    if (!is_lpp_offload_enabled_) {
      return;
    }
    log::info("");
    if (lpp_offload_callbacks_ != nullptr) {
      lpp_offload_callbacks_->onReceiveSocketCloseRequest(socketId, reason);
    }
  }
  os::Handler* handler_;
  hal::SocketHal* socket_hal_;
  SocketProperties socket_prop_;
  LppOffloadFeatures features_;
  LppOffloadCallbacks* lpp_offload_callbacks_;
  bool is_lpp_offload_enabled_;
};

LppOffloadManager::LppOffloadManager() { pimpl_ = std::make_unique<impl>(); }

LppOffloadManager::~LppOffloadManager() = default;

void LppOffloadManager::ListDependencies(ModuleList* list) const { list->add<hal::SocketHal>(); }

void LppOffloadManager::Start() { pimpl_->start(GetHandler(), GetDependency<hal::SocketHal>()); }

void LppOffloadManager::Stop() { pimpl_->stop(); }

std::string LppOffloadManager::ToString() const { return "Low Power Processor Offload Manager"; }

void LppOffloadManager::RegisterLppOffloadCallbacks(LppOffloadCallbacks* callbacks) {
  CallOn(pimpl_.get(), &impl::register_lpp_offload_callbacks, callbacks);
}

void LppOffloadManager::GetOffloadFeaturesSupported(LppOffloadFeatures* features) {
  pimpl_->get_offload_features_supported(features);
}

bool LppOffloadManager::SetAclCreditsForSockets(AclLinkType linkType, int credit) {
  return pimpl_->set_acl_credits_for_sockets(linkType, credit);
}

bool LppOffloadManager::NotifyAclConnectionStateChange(int aclHandle, AclLinkType linkType,
                                                       ConnectionState state) {
  return pimpl_->notify_acl_connection_state_change(aclHandle, linkType, state);
}

bool LppOffloadManager::NotifyAclLeDataLengthChange(int aclHandle, int txDataLen, int rxDataLen) {
  return pimpl_->notify_acl_le_data_length_change(aclHandle, txDataLen, rxDataLen);
}

bool LppOffloadManager::NotifyAclPowerModeChange(int aclHandle, AclPowerMode powerMode,
                                                 int interval) {
  return pimpl_->notify_acl_pm_change(aclHandle, powerMode, interval);
}

bool LppOffloadManager::NotifySocketConnectionStateChange(const SocketContext& context) {
  return pimpl_->notify_socket_connection_state_change(context);
}

}  // namespace hci
}  // namespace bluetooth
