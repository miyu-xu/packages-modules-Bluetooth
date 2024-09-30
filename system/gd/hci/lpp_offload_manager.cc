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

  void stop() {
    log::info("");
    features_ = {};
    socket_capabilities_ = {};
  }

  void init() {
    is_lpp_offload_enabled_ = os::GetSystemPropertyBool("ro.bluetooth.lpp_offload.enabled", false);
    log::info("is_lpp_offload_enabled_ {}", is_lpp_offload_enabled_);
    if (!is_lpp_offload_enabled_) {
      return;
    }
    update_offload_features_supported();
  }

  void update_offload_features_supported() {
    features_ = {};
    update_socket_features_supported();
  }

  void update_socket_features_supported() {
    socket_capabilities_ = socket_hal_->GetSocketCapabilities();
    features_.is_socket_hw_offload_supported =
            (socket_capabilities_.leCocCapabilities.numberOfSupportedSockets > 0);
  }

  void register_lpp_offload_callbacks(LppOffloadCallbacks* callbacks) {
    if (!is_lpp_offload_enabled_) {
      return;
    }
    log::info("");
    lpp_offload_callbacks_ = callbacks;
    socket_hal_->RegisterCallback(this);
  }

  void get_offload_features_supported(LppOffloadFeatures* features) {
    if (!is_lpp_offload_enabled_) {
      return;
    }
    log::assert_that(features, "assert failed: features is null");
    log::info("");
    *features = features_;
  }

  void get_socket_capabilities(SocketCapabilities* capabilities) {
    log::assert_that(capabilities, "assert failed: capabilities is null");
    *capabilities = socket_capabilities_;
  }

  bool socket_opened(const SocketContext& context) {
    if (!is_lpp_offload_enabled_) {
      return false;
    }
    log::info("");
    return socket_hal_->Opened(context);
  }

  void socket_closed(uint64_t socketId) {
    if (!is_lpp_offload_enabled_) {
      return;
    }
    log::info("");
    return socket_hal_->Closed(socketId);
  }

  // Implements SocketHalCallback
  void SocketOpenedComplete(uint64_t socketId, SocketStatus status) {
    if (!is_lpp_offload_enabled_) {
      return;
    }
    log::info("");
    if (lpp_offload_callbacks_ != nullptr) {
      lpp_offload_callbacks_->SocketOpenedComplete(socketId, status);
    }
  }

  void SocketClose(uint64_t socketId) {
    if (!is_lpp_offload_enabled_) {
      return;
    }
    log::info("");
    if (lpp_offload_callbacks_ != nullptr) {
      lpp_offload_callbacks_->SocketClose(socketId);
    }
  }

  os::Handler* handler_;
  hal::SocketHal* socket_hal_;
  SocketCapabilities socket_capabilities_;
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
  CallOn(pimpl_.get(), &impl::get_offload_features_supported, features);
}

void LppOffloadManager::GetSocketCapabilities(SocketCapabilities* socketCapabilities) {
  CallOn(pimpl_.get(), &impl::get_socket_capabilities, socketCapabilities);
}

bool LppOffloadManager::SocketOpened(const SocketContext& context) {
  return pimpl_->socket_opened(context);
}

void LppOffloadManager::SocketClosed(uint64_t socketId) {
  CallOn(pimpl_.get(), &impl::socket_closed, socketId);
}

}  // namespace hci
}  // namespace bluetooth
