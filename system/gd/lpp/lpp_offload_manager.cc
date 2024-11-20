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
#include "lpp_offload_manager.h"

#include <bluetooth/log.h>

#include <string>

#include "hal/socket_hal.h"
#include "module.h"
#include "os/handler.h"
#include "os/system_properties.h"

namespace bluetooth::lpp {

const ModuleFactory LppOffloadManager::Factory =
        ModuleFactory([]() { return new LppOffloadManager(); });

struct LppOffloadManager::impl {
  ~impl() {}

  void start(os::Handler* handler, hal::SocketHal* socket_hal) {
    log::info("");
    handler_ = handler;
    socket_hal_ = socket_hal;

    is_lpp_offload_enabled_ = os::GetSystemPropertyBool("bluetooth.lpp_offload.enabled", false);
    log::info("is_lpp_offload_enabled_ {}", is_lpp_offload_enabled_);
    if (!is_lpp_offload_enabled_) {
      return;
    }

    features_ = {};
    socket_capabilities_ = socket_hal_->GetSocketCapabilities();
    features_.is_socket_hw_offload_supported =
            (socket_capabilities_.le_coc_capabilities.number_of_supported_sockets > 0);
  }

  void stop() {
    log::info("");
    features_ = {};
    socket_capabilities_ = {};
  }

  bool register_socket_hal_callbacks(hal::SocketHalCallback* callbacks) {
    if (!is_lpp_offload_enabled_) {
      return false;
    }
    log::info("");
    return socket_hal_->RegisterCallback(callbacks);
  }

  LppOffloadFeatures get_offload_features_supported() const {
    if (!is_lpp_offload_enabled_) {
      return {};
    }
    log::info("");
    return features_;
  }

  hal::SocketCapabilities get_socket_capabilities() const {
    if (!is_lpp_offload_enabled_) {
      return {};
    }
    log::info("");
    return socket_capabilities_;
  }

  bool socket_opened(const hal::SocketContext& context) {
    if (!is_lpp_offload_enabled_) {
      return false;
    }
    log::info("socket_id: {}", context.socket_id);
    return socket_hal_->Opened(context);
  }

  void socket_closed(uint64_t socket_id) {
    if (!is_lpp_offload_enabled_) {
      return;
    }
    log::info("socket_id: {}", socket_id);
    return socket_hal_->Closed(socket_id);
  }

  os::Handler* handler_;
  hal::SocketHal* socket_hal_;
  hal::SocketCapabilities socket_capabilities_;
  LppOffloadFeatures features_;
  bool is_lpp_offload_enabled_;
};

LppOffloadManager::LppOffloadManager() { pimpl_ = std::make_unique<impl>(); }

LppOffloadManager::~LppOffloadManager() = default;

void LppOffloadManager::ListDependencies(ModuleList* list) const { list->add<hal::SocketHal>(); }

void LppOffloadManager::Start() { pimpl_->start(GetHandler(), GetDependency<hal::SocketHal>()); }

void LppOffloadManager::Stop() { pimpl_->stop(); }

std::string LppOffloadManager::ToString() const { return "Low Power Processor Offload Manager"; }

bool LppOffloadManager::RegisterSocketHalCallbacks(hal::SocketHalCallback* callbacks) {
  return pimpl_->register_socket_hal_callbacks(callbacks);
}

LppOffloadFeatures LppOffloadManager::GetOffloadFeaturesSupported() const {
  return pimpl_->get_offload_features_supported();
}

hal::SocketCapabilities LppOffloadManager::GetSocketCapabilities() const {
  return pimpl_->get_socket_capabilities();
}

bool LppOffloadManager::SocketOpened(const hal::SocketContext& context) {
  return pimpl_->socket_opened(context);
}

void LppOffloadManager::SocketClosed(uint64_t socket_id) {
  CallOn(pimpl_.get(), &impl::socket_closed, socket_id);
}

}  // namespace bluetooth::lpp
