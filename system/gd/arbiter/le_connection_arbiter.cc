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

#include <optional>
#include <tuple>
#include <unordered_set>

#include "arbiter_client_types.h"
#include "hci/hci_packets.h"
#include "le_connection_packet_router.h"

namespace bluetooth {
namespace arbiter {

struct TokenAllocator {
  Token AllocateToken() {
    std::scoped_lock lock{lock_};
    ++token_;
    return {token_};
  }

  // guarded by lock_
  std::mutex lock_{};
  int token_{};
};

struct LeConnectionArbiterModule::impl : hci::acl_manager::LeConnectionCallbacks {
  // Lifecycle
  impl(const LeConnectionArbiterModule& module) : module_(module) {}

  void Start() {
    acl_manager_ = module_.GetDependency<hci::AclManager>();
    hci_layer_ = module_.GetDependency<hci::HciLayer>();
    handler_ = module_.GetHandler();

    acl_manager_->RegisterLeCallbacks(this, handler_);
  }

  void Stop() {
    acl_manager_ = nullptr;
    handler_ = nullptr;

    acl_manager_->UnregisterLeCallbacks(this, {});
  }

  // Interface
  void RegisterClient(
      Token token,
      hci::acl_manager::LeConnectionCallbacks* callbacks,
      os::Handler* handler,
      ConnectionFilter connection_filter,
      PacketFilter packet_filter,
      ClientPriority priority) {
    callback_handlers_.push_back(
        {token, callbacks, connection_filter, packet_filter, handler, priority});

    // note: std::list::sort is stable and uses the operator<() defined above
    callback_handlers_.sort();
  }

  void UnregisterClient(Token token, std::promise<void> promise) {
    callback_handlers_.remove_if([&](auto elem) { return elem.token.val == token.val; });
    promise.set_value();
  }

 private:
  // Callbacks
  void OnLeConnectSuccess(
      hci::AddressWithType peer_address,
      std::unique_ptr<hci::acl_manager::LeAclConnection> connection) override {
    auto clients_for_router = std::vector<std::tuple<
        internal::ArbiterClient,
        hci::acl_manager::LeAclConnection*,
        hci::acl_manager::LeAclConnection::QueueDownEnd*>>{};
    auto participating_clients = std::vector<
        std::pair<internal::ArbiterClient, std::unique_ptr<hci::acl_manager::LeAclConnection>>>{};

    for (auto it = callback_handlers_.rbegin(); it != callback_handlers_.rend(); ++it) {
      if (ConnectionFilterMatches(connection.get(), it->connection_filter)) {
        // wrap the connection so we can intercept packets
        auto queue_after_filtering = std::make_unique<hci::acl_manager::AclConnection::Queue>(10);
        auto queue_after_filtering_down_end = queue_after_filtering->GetDownEnd();

        auto filtered_connection = std::make_unique<hci::acl_manager::LeAclConnection>(
            std::move(queue_after_filtering),
            hci_layer_->GetLeAclConnectionInterfaceWithoutRegisteringForEvents(),
            connection->GetHandle(),
            connection->GetRoleSpecificData(),
            connection->GetRemoteAddress());

        clients_for_router.push_back(
            {*it, filtered_connection.get(), queue_after_filtering_down_end});
        participating_clients.push_back({*it, std::move(filtered_connection)});
      }
    }

    if (clients_for_router.empty()) {
      LOG_ERROR(
          "no callback handler registered matching connection, disconnecting and dropping it!");
      connection->Disconnect(hci::DisconnectReason::REMOTE_USER_TERMINATED_CONNECTION);
      return;
    }

    packet_routers_.emplace_back(
        std::move(connection),
        clients_for_router,
        handler_,
        handler_->BindOnceOn(this, &LeConnectionArbiterModule::impl::RemovePacketRouter));

    for (auto& [client, connection] : participating_clients) {
      client.handler->CallOn(
          client.callbacks,
          &hci::acl_manager::LeConnectionCallbacks::OnLeConnectSuccess,
          peer_address,
          std::move(connection));
    }
  }

  void OnLeConnectFail(
      hci::AddressWithType peer_address, hci::ErrorCode reason, bool locally_initiated) override {
    if (callback_handlers_.size() == 0) {
      LOG_ERROR("no callback handlers registered, dropping event");
      return;
    }
    callback_handlers_.front().handler->CallOn(
        callback_handlers_.front().callbacks,
        &hci::acl_manager::LeConnectionCallbacks::OnLeConnectFail,
        peer_address,
        reason,
        locally_initiated);
  }

 private:
  // Internal
  bool ConnectionFilterMatches(
      hci::acl_manager::LeAclConnection* connection, ConnectionFilter filter) {
    return std::visit(
        [&](auto&& filter) {
          using T = std::decay_t<decltype(filter)>;
          if constexpr (std::is_same_v<T, AdvertisingSetConnectionFilter>) {
            auto peripheral_data = std::get_if<bluetooth::hci::acl_manager::DataAsPeripheral>(
                &connection->GetRoleSpecificData());
            return peripheral_data != nullptr &&
                   peripheral_data->advertising_set_id == filter.advertising_set_id;
          } else if constexpr (std::is_same_v<T, UnconditionalConnectionFilter>) {
            return true;
          } else {
            static_assert(!sizeof(T*), "missed filter type");
          }
        },
        filter);
  }

  void RemovePacketRouter(internal::LeConnectionPacketRouter* router) {
    packet_routers_.remove_if([&](auto& candidate) { return &candidate == router; });
  }

  // read-only outside of lifecycle methods
  const LeConnectionArbiterModule& module_;
  hci::AclManager* acl_manager_;
  hci::HciLayer* hci_layer_;
  os::Handler* handler_;

  std::list<internal::ArbiterClient> callback_handlers_{};
  std::list<internal::LeConnectionPacketRouter> packet_routers_{};

 public:
  // thread-safe, so public / can be read outside of the handler
  TokenAllocator token_allocator_{};
};

Token LeConnectionArbiterModule::RegisterClient(
    hci::acl_manager::LeConnectionCallbacks* callbacks,
    os::Handler* handler,
    ConnectionFilter connection_filter,
    PacketFilter packet_filter,
    ClientPriority priority) {
  // do this outside of the handler so we can return it synchronously
  auto token = pimpl_->token_allocator_.AllocateToken();
  CallOn(
      pimpl_.get(),
      &impl::RegisterClient,
      token,
      callbacks,
      handler,
      connection_filter,
      packet_filter,
      priority);
  return token;
}

void LeConnectionArbiterModule::UnregisterClient(Token token, std::promise<void> promise) {
  CallOn(pimpl_.get(), &impl::UnregisterClient, token, std::move(promise));
}

const ModuleFactory LeConnectionArbiterModule::Factory =
    ModuleFactory([]() { return new LeConnectionArbiterModule(); });

LeConnectionArbiterModule::LeConnectionArbiterModule() : pimpl_(std::make_unique<impl>(*this)){};
LeConnectionArbiterModule::~LeConnectionArbiterModule() = default;

void LeConnectionArbiterModule::ListDependencies(ModuleList* list) const {
  list->add<hci::AclManager>();
  list->add<hci::HciLayer>();
}

void LeConnectionArbiterModule::Start() {
  pimpl_->Start();
}

void LeConnectionArbiterModule::Stop() {
  pimpl_->Stop();
}

}  // namespace arbiter
}  // namespace bluetooth