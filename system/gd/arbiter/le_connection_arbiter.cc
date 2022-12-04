/*
 * Copyright 2022 The Android Open Source Project
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

#include "le_connection_arbiter.h"

#include <unordered_set>

namespace bluetooth {
namespace arbiter {

struct CallbacksAndHandler {
  hci::acl_manager::LeConnectionCallbacks* callbacks;
  ConnectionFilter filter;
  os::Handler* handler;
};

struct LeConnectionArbiterModule::impl : hci::acl_manager::LeConnectionCallbacks {
  // Lifecycle
  impl(const LeConnectionArbiterModule& module) : module_(module) {}

  void Start() {
    acl_manager_ = module_.GetDependency<hci::AclManager>();
    handler_ = module_.GetHandler();
  }

  void Stop() {
    acl_manager_ = nullptr;
    handler_ = nullptr;
  }

  // Interface
  void RegisterLeCallbacks(
      hci::acl_manager::LeConnectionCallbacks* callbacks, os::Handler* handler, ConnectionFilter filter) {
    acl_manager_->RegisterLeCallbacks(callbacks, handler);
  }

  void UnregisterLeCallbacks(
      hci::acl_manager::LeConnectionCallbacks* callbacks, ConnectionFilter filter, std::promise<void> promise) {
    acl_manager_->UnregisterLeCallbacks(callbacks, std::move(promise));
  }

 protected:
  // Callbacks
  void OnLeConnectSuccess(
      hci::AddressWithType peer_address, std::unique_ptr<hci::acl_manager::LeAclConnection> connection) override {
    for (auto it = callback_handlers_.rbegin(); it != callback_handlers_.rend(); ++it) {
      if (FilterMatches(connection.get(), it->filter)) {
        GetDefaultCallback().handler->CallOn(
            GetDefaultCallback().callbacks,
            &hci::acl_manager::LeConnectionCallbacks::OnLeConnectSuccess,
            peer_address,
            std::move(connection));
      }
      LOG_ALWAYS_FATAL("DefaultFilter was not registered");
    }
  }

  void OnLeConnectFail(hci::AddressWithType peer_address, hci::ErrorCode reason, bool locally_initiated) override {
    GetDefaultCallback().handler->CallOn(
        GetDefaultCallback().callbacks,
        &hci::acl_manager::LeConnectionCallbacks::OnLeConnectFail,
        peer_address,
        reason,
        locally_initiated);
  }

 private:
  // Internal
  bool FilterMatches(hci::acl_manager::LeAclConnection* connection, ConnectionFilter filter) {
    return std::visit(
        [&](auto&& filter) {
          using T = std::decay_t<decltype(filter)>;
          if constexpr (std::is_same_v<T, AdvertisingSetFilter>) {
            auto peripheral_data =
                std::get_if<bluetooth::hci::acl_manager::DataAsPeripheral>(&connection->GetRoleSpecificData());
            return peripheral_data != nullptr && peripheral_data->advertising_set_id == filter.advertising_set_id;
          } else if constexpr (std::is_same_v<T, DefaultFilter>) {
            return true;
          } else {
            static_assert(!sizeof(T*), "missed filter type");
          }
        },
        filter);
  }

  CallbacksAndHandler GetDefaultCallback() {
    if (callback_handlers_.empty()) {
      LOG_ALWAYS_FATAL("No callback handlers registered");
    }
    if (std::get_if<DefaultFilter>(&callback_handlers_.front().filter) == nullptr) {
      LOG_ALWAYS_FATAL("Default filter not registered as first callback");
    }
    return callback_handlers_.front();
  }

  const LeConnectionArbiterModule& module_;
  hci::AclManager* acl_manager_;
  os::Handler* handler_;
  std::list<CallbacksAndHandler> callback_handlers_;
};

void LeConnectionArbiterModule::RegisterLeCallbacks(
    hci::acl_manager::LeConnectionCallbacks* callbacks, os::Handler* handler, ConnectionFilter filter) {
  CallOn(pimpl_.get(), &impl::RegisterLeCallbacks, callbacks, handler, filter);
}

void LeConnectionArbiterModule::UnregisterLeCallbacks(
    hci::acl_manager::LeConnectionCallbacks* callbacks, ConnectionFilter filter, std::promise<void> promise) {
  CallOn(pimpl_.get(), &impl::UnregisterLeCallbacks, callbacks, filter, std::move(promise));
}

const ModuleFactory LeConnectionArbiterModule::Factory =
    ModuleFactory([]() { return new LeConnectionArbiterModule(); });

LeConnectionArbiterModule::LeConnectionArbiterModule() : pimpl_(std::make_unique<impl>(*this)){};
LeConnectionArbiterModule::~LeConnectionArbiterModule() = default;

void LeConnectionArbiterModule::ListDependencies(ModuleList* list) const {
  list->add<hci::AclManager>();
}

void LeConnectionArbiterModule::Start() {
  pimpl_->Start();
}

void LeConnectionArbiterModule::Stop() {
  pimpl_->Stop();
}

}  // namespace arbiter
}  // namespace bluetooth