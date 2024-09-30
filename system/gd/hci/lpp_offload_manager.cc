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
    hw_socket_prop_ = {};
    sw_socket_prop_ = {};
  }

  void init() {
    is_lpp_offload_enabled_ =
            os::GetSystemPropertyBool("persist.bluetooth.lpp_offload.enabled", false);
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
    std::vector<SocketProperties> socket_props_ = socket_hal_->GetSocketProperties();
    for (const auto& socket_prop : socket_props_) {
      switch (static_cast<int>(socket_prop.dataPath)) {
        case SocketDataPath::OFFLOAD_SOFTWARE:
          features_.socket_sw_offload_supported =
                  check_protocol_properties_supported(socket_prop.protocolProperties);
          sw_socket_prop_ = socket_prop;
          log::info("dataPath {}, supported {}", static_cast<int>(socket_prop.dataPath),
                    features_.socket_sw_offload_supported);
          break;
        case SocketDataPath::OFFLOAD_HARDWARE:
          features_.socket_hw_offload_supported =
                  check_protocol_properties_supported(socket_prop.protocolProperties);
          hw_socket_prop_ = socket_prop;
          log::info("dataPath {}, supported {}", static_cast<int>(socket_prop.dataPath),
                    features_.socket_hw_offload_supported);
          break;
      }
    }
  }

  bool check_protocol_properties_supported(const std::vector<ProtocolProperties>& props) {
    for (const auto& prop : props) {
      if (prop.numOfSocketSupported > 0) {
        return true;
      }
    }
    return false;
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
    if (!is_lpp_offload_enabled_ || features == nullptr) {
      return;
    }
    log::info("");
    *features = features_;
  }

  bool get_socket_properties(SocketDataPath dataPath, SocketProperties* socketPros) {
    if (!is_lpp_offload_enabled_ || socketPros == nullptr) {
      return false;
    }
    if (features_.socket_sw_offload_supported && dataPath == SocketDataPath::OFFLOAD_SOFTWARE) {
      *socketPros = sw_socket_prop_;
      return true;
    } else if (features_.socket_hw_offload_supported &&
               dataPath == SocketDataPath::OFFLOAD_HARDWARE) {
      *socketPros = hw_socket_prop_;
      return true;
    }
    return false;
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
  void onReceiveSocketCloseRequest(const bluetooth::Uuid& socketId, RequestReason reason) {
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
  SocketProperties hw_socket_prop_, sw_socket_prop_;
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

bool LppOffloadManager::GetSocketProperties(SocketDataPath dataPath, SocketProperties* socketPros) {
  return pimpl_->get_socket_properties(dataPath, socketPros);
}

bool LppOffloadManager::NotifySocketConnectionStateChange(const SocketContext& context) {
  return pimpl_->notify_socket_connection_state_change(context);
}

}  // namespace hci
}  // namespace bluetooth
