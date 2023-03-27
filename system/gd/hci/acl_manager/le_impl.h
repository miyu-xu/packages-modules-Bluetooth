/*
 * Copyright 2020 The Android Open Source Project
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

#include <base/strings/stringprintf.h>

#include <atomic>
#include <cstddef>
#include <cstdint>
#include <memory>
#include <optional>
#include <string>
#include <unordered_set>

#include "common/bind.h"
#include "common/init_flags.h"
#include "crypto_toolbox/crypto_toolbox.h"
#include "hci/acl_manager/assembler.h"
#include "hci/acl_manager/le_connect_hci_manager.h"
#include "hci/acl_manager/le_connection_management_callbacks.h"
#include "hci/acl_manager/le_connectlist_manager.h"
#include "hci/acl_manager/round_robin_scheduler.h"
#include "hci/controller.h"
#include "hci/hci_layer.h"
#include "hci/hci_packets.h"
#include "hci/le_address_manager.h"
#include "os/alarm.h"
#include "os/handler.h"
#include "os/metrics.h"
#include "os/system_properties.h"
#include "packet/packet_view.h"

using bluetooth::crypto_toolbox::Octet16;

namespace bluetooth {
namespace hci {
namespace acl_manager {

namespace {
static const std::string kPropertyEnableBlePrivacy = "bluetooth.core.gap.le.privacy.enabled";
constexpr bool kEnableBlePrivacy = true;
}  // namespace

using common::BindOnce;

struct le_acl_connection {
  le_acl_connection(
      AddressWithType remote_address,
      std::unique_ptr<LeAclConnection> pending_connection,
      AclConnection::QueueDownEnd* queue_down_end,
      os::Handler* handler)
      : remote_address_(remote_address),
        pending_connection_(std::move(pending_connection)),
        assembler_(new acl_manager::assembler(remote_address, queue_down_end, handler)) {}
  ~le_acl_connection() {
    delete assembler_;
  }
  AddressWithType remote_address_;
  std::unique_ptr<LeAclConnection> pending_connection_;
  acl_manager::assembler* assembler_;
  LeConnectionManagementCallbacks* le_connection_management_callbacks_ = nullptr;
};

struct le_impl {
  le_impl(
      HciLayer* hci_layer,
      Controller* controller,
      os::Handler* handler,
      RoundRobinScheduler* round_robin_scheduler,
      bool crash_on_unknown_handle)
      : hci_layer_(hci_layer),
        controller_(controller),
        round_robin_scheduler_(round_robin_scheduler) {
    hci_layer_ = hci_layer;
    controller_ = controller;
    handler_ = handler;
    connections.crash_on_unknown_handle_ = crash_on_unknown_handle;
    le_acl_connection_interface_ = hci_layer_->GetLeAclConnectionInterface(
        handler_->BindOn(this, &le_impl::on_le_event),
        handler_->BindOn(this, &le_impl::on_le_disconnect),
        handler_->BindOn(this, &le_impl::on_le_read_remote_version_information));
    le_address_manager_ = new LeAddressManager(
        common::Bind(&le_impl::enqueue_command, common::Unretained(this)),
        handler_,
        controller->GetMacAddress(),
        controller->GetLeFilterAcceptListSize(),
        controller->GetLeResolvingListSize());
    le_connect_hci_manager_ = new LeConnectHciManager(
        controller_, le_address_manager_, le_acl_connection_interface_, handler_);
    le_connectlist_manager_ =
        new LeConnectlistManager(le_address_manager_, le_connect_hci_manager_, handler);
  }

  ~le_impl() {
    delete le_connectlist_manager_;
    delete le_connect_hci_manager_;
    delete le_address_manager_;
    hci_layer_->PutLeAclConnectionInterface();
    connections.reset();
  }

  void on_le_event(LeMetaEventView event_packet) {
    SubeventCode code = event_packet.GetSubeventCode();
    switch (code) {
      case SubeventCode::CONNECTION_COMPLETE:
        on_le_connection_complete(event_packet);
        break;
      case SubeventCode::ENHANCED_CONNECTION_COMPLETE:
        on_le_enhanced_connection_complete(event_packet);
        break;
      case SubeventCode::CONNECTION_UPDATE_COMPLETE:
        on_le_connection_update_complete(event_packet);
        break;
      case SubeventCode::PHY_UPDATE_COMPLETE:
        on_le_phy_update_complete(event_packet);
        break;
      case SubeventCode::DATA_LENGTH_CHANGE:
        on_data_length_change(event_packet);
        break;
      case SubeventCode::REMOTE_CONNECTION_PARAMETER_REQUEST:
        on_remote_connection_parameter_request(event_packet);
        break;
      case SubeventCode::LE_SUBRATE_CHANGE:
        on_le_subrate_change(event_packet);
        break;
      default:
        LOG_ALWAYS_FATAL("Unhandled event code %s", SubeventCodeText(code).c_str());
    }
  }

 private:
  static constexpr uint16_t kIllegalConnectionHandle = 0xffff;
  struct {
   private:
    std::map<uint16_t, le_acl_connection> le_acl_connections_;
    mutable std::mutex le_acl_connections_guard_;
    LeConnectionManagementCallbacks* find_callbacks(uint16_t handle) {
      auto connection = le_acl_connections_.find(handle);
      if (connection == le_acl_connections_.end()) return nullptr;
      return connection->second.le_connection_management_callbacks_;
    }
    void remove(uint16_t handle) {
      auto connection = le_acl_connections_.find(handle);
      if (connection != le_acl_connections_.end()) {
        connection->second.le_connection_management_callbacks_ = nullptr;
        le_acl_connections_.erase(handle);
      }
    }

   public:
    bool crash_on_unknown_handle_ = false;
    bool is_empty() const {
      std::unique_lock<std::mutex> lock(le_acl_connections_guard_);
      return le_acl_connections_.empty();
    }
    void reset() {
      std::map<uint16_t, le_acl_connection> le_acl_connections{};
      {
        std::unique_lock<std::mutex> lock(le_acl_connections_guard_);
        le_acl_connections = std::move(le_acl_connections_);
      }
      le_acl_connections.clear();
    }
    void invalidate(uint16_t handle) {
      std::unique_lock<std::mutex> lock(le_acl_connections_guard_);
      remove(handle);
    }
    void execute(
        uint16_t handle,
        std::function<void(LeConnectionManagementCallbacks* callbacks)> execute,
        bool remove_afterwards = false) {
      std::unique_lock<std::mutex> lock(le_acl_connections_guard_);
      auto callbacks = find_callbacks(handle);
      if (callbacks != nullptr)
        execute(callbacks);
      else
        ASSERT_LOG(!crash_on_unknown_handle_, "Received command for unknown handle:0x%x", handle);
      if (remove_afterwards) remove(handle);
    }
    bool send_packet_upward(uint16_t handle, std::function<void(struct acl_manager::assembler* assembler)> cb) {
      std::unique_lock<std::mutex> lock(le_acl_connections_guard_);
      auto connection = le_acl_connections_.find(handle);
      if (connection != le_acl_connections_.end()) cb(connection->second.assembler_);
      return connection != le_acl_connections_.end();
    }
    void add(
        uint16_t handle,
        const AddressWithType& remote_address,
        std::unique_ptr<LeAclConnection> pending_connection,
        AclConnection::QueueDownEnd* queue_end,
        os::Handler* handler,
        LeConnectionManagementCallbacks* le_connection_management_callbacks) {
      std::unique_lock<std::mutex> lock(le_acl_connections_guard_);
      auto emplace_pair = le_acl_connections_.emplace(
          std::piecewise_construct,
          std::forward_as_tuple(handle),
          std::forward_as_tuple(remote_address, std::move(pending_connection), queue_end, handler));
      ASSERT(emplace_pair.second);  // Make sure the connection is unique
      emplace_pair.first->second.le_connection_management_callbacks_ = le_connection_management_callbacks;
    }

    std::unique_ptr<LeAclConnection> record_peripheral_data_and_extract_pending_connection(
        uint16_t handle, DataAsPeripheral data) {
      std::unique_lock<std::mutex> lock(le_acl_connections_guard_);
      auto connection = le_acl_connections_.find(handle);
      if (connection != le_acl_connections_.end() && connection->second.pending_connection_.get()) {
        connection->second.pending_connection_->UpdateRoleSpecificData(data);
        return std::move(connection->second.pending_connection_);
      } else {
        return nullptr;
      }
    }

    uint16_t HACK_get_handle(Address address) const {
      std::unique_lock<std::mutex> lock(le_acl_connections_guard_);
      for (auto it = le_acl_connections_.begin(); it != le_acl_connections_.end(); it++) {
        if (it->second.remote_address_.GetAddress() == address) {
          return it->first;
        }
      }
      return kIllegalConnectionHandle;
    }

    AddressWithType getAddressWithType(uint16_t handle) {
      std::unique_lock<std::mutex> lock(le_acl_connections_guard_);
      auto it = le_acl_connections_.find(handle);
      if (it != le_acl_connections_.end()) {
        return it->second.remote_address_;
      }
      AddressWithType empty(Address::kEmpty, AddressType::RANDOM_DEVICE_ADDRESS);
      return empty;
    }

    bool alreadyConnected(AddressWithType address_with_type) {
      for (auto it = le_acl_connections_.begin(); it != le_acl_connections_.end(); it++) {
        if (it->second.remote_address_ == address_with_type) {
          return true;
        }
      }
      return false;
    }

  } connections;

 public:
  void enqueue_command(std::unique_ptr<CommandBuilder> command_packet) {
    hci_layer_->EnqueueCommand(
        std::move(command_packet),
        handler_->BindOnce(&LeAddressManager::OnCommandComplete, common::Unretained(le_address_manager_)));
  }

  bool send_packet_upward(uint16_t handle, std::function<void(struct acl_manager::assembler* assembler)> cb) {
    return connections.send_packet_upward(handle, cb);
  }

  void on_le_connection_complete(LeMetaEventView packet) {
    LeConnectionCompleteView connection_complete = LeConnectionCompleteView::Create(packet);
    ASSERT(connection_complete.IsValid());
    auto status = connection_complete.GetStatus();
    auto address = connection_complete.GetPeerAddress();
    auto peer_address_type = connection_complete.GetPeerAddressType();
    auto role = connection_complete.GetRole();
    AddressWithType remote_address(address, peer_address_type);

    ConnectionResult result =
        le_connectlist_manager_->OnConnectionComplete(remote_address, status, role);

    if (le_client_handler_ == nullptr) {
      LOG_ERROR("No callbacks to call");
      return;
    }

    bool in_filter_accept_list = true;
    switch (result) {
      case ConnectionResult::Failure:
        le_client_handler_->Post(common::BindOnce(
            &LeConnectionCallbacks::OnLeConnectFail,
            common::Unretained(le_client_callbacks_),
            remote_address,
            status));
        [[fallthrough]];
      case ConnectionResult::Ignore:
        return;

      case ConnectionResult::SuccessButNotInFilterAcceptList:
        in_filter_accept_list = false;
        [[fallthrough]];
      case ConnectionResult::Success: {
        // no-op, continue below
      }
    }

    uint16_t conn_interval = connection_complete.GetConnInterval();
    uint16_t conn_latency = connection_complete.GetConnLatency();
    uint16_t supervision_timeout = connection_complete.GetSupervisionTimeout();

    uint16_t handle = connection_complete.GetConnectionHandle();
    auto role_specific_data = initialize_role_specific_data(role);
    auto queue = std::make_shared<AclConnection::Queue>(10);
    auto queue_down_end = queue->GetDownEnd();
    round_robin_scheduler_->Register(RoundRobinScheduler::ConnectionType::LE, handle, queue);
    std::unique_ptr<LeAclConnection> connection(new LeAclConnection(
        std::move(queue),
        le_acl_connection_interface_,
        handle,
        role_specific_data,
        remote_address));
    connection->peer_address_with_type_ = AddressWithType(address, peer_address_type);
    connection->interval_ = conn_interval;
    connection->latency_ = conn_latency;
    connection->supervision_timeout_ = supervision_timeout;
    connection->in_filter_accept_list_ = in_filter_accept_list;
    connection->locally_initiated_ = (role == hci::Role::CENTRAL);
    auto connection_callbacks = connection->GetEventCallbacks(
        [this](uint16_t handle) { this->connections.invalidate(handle); });
    if (std::holds_alternative<DataAsUninitializedPeripheral>(role_specific_data)) {
      // the OnLeConnectSuccess event will be sent after receiving the On Advertising Set Terminated
      // event, since we need it to know what local_address / advertising set the peer connected to.
      // In the meantime, we store it as a pending_connection.
      connections.add(
          handle,
          remote_address,
          std::move(connection),
          queue_down_end,
          handler_,
          connection_callbacks);
    } else {
      connections.add(
          handle, remote_address, nullptr, queue_down_end, handler_, connection_callbacks);
      le_client_handler_->Post(common::BindOnce(
          &LeConnectionCallbacks::OnLeConnectSuccess,
          common::Unretained(le_client_callbacks_),
          remote_address,
          std::move(connection)));
    }
  }

  void on_le_enhanced_connection_complete(LeMetaEventView packet) {
    LeEnhancedConnectionCompleteView connection_complete = LeEnhancedConnectionCompleteView::Create(packet);
    ASSERT(connection_complete.IsValid());
    auto status = connection_complete.GetStatus();
    auto address = connection_complete.GetPeerAddress();
    auto peer_address_type = connection_complete.GetPeerAddressType();
    auto peer_resolvable_address = connection_complete.GetPeerResolvablePrivateAddress();
    auto role = connection_complete.GetRole();

    AddressType remote_address_type;
    switch (peer_address_type) {
      case AddressType::PUBLIC_DEVICE_ADDRESS:
      case AddressType::PUBLIC_IDENTITY_ADDRESS:
        remote_address_type = AddressType::PUBLIC_DEVICE_ADDRESS;
        break;
      case AddressType::RANDOM_DEVICE_ADDRESS:
      case AddressType::RANDOM_IDENTITY_ADDRESS:
        remote_address_type = AddressType::RANDOM_DEVICE_ADDRESS;
        break;
    }
    AddressWithType remote_address(address, remote_address_type);

    ConnectionResult result =
        le_connectlist_manager_->OnConnectionComplete(remote_address, status, role);

    if (le_client_handler_ == nullptr) {
      LOG_ERROR("No callbacks to call");
      return;
    }

    bool in_filter_accept_list = true;
    switch (result) {
      case ConnectionResult::Failure:
        le_client_handler_->Post(common::BindOnce(
            &LeConnectionCallbacks::OnLeConnectFail,
            common::Unretained(le_client_callbacks_),
            remote_address,
            status));
        [[fallthrough]];
      case ConnectionResult::Ignore:
        return;

      case ConnectionResult::SuccessButNotInFilterAcceptList:
        in_filter_accept_list = false;
        [[fallthrough]];
      case ConnectionResult::Success: {
        // no-op, continue below
      }
    }

    auto role_specific_data = initialize_role_specific_data(role);
    uint16_t conn_interval = connection_complete.GetConnInterval();
    uint16_t conn_latency = connection_complete.GetConnLatency();
    uint16_t supervision_timeout = connection_complete.GetSupervisionTimeout();

    uint16_t handle = connection_complete.GetConnectionHandle();
    auto queue = std::make_shared<AclConnection::Queue>(10);
    auto queue_down_end = queue->GetDownEnd();
    round_robin_scheduler_->Register(RoundRobinScheduler::ConnectionType::LE, handle, queue);
    std::unique_ptr<LeAclConnection> connection(new LeAclConnection(
        std::move(queue),
        le_acl_connection_interface_,
        handle,
        role_specific_data,
        remote_address));
    connection->peer_address_with_type_ = AddressWithType(address, peer_address_type);
    connection->interval_ = conn_interval;
    connection->latency_ = conn_latency;
    connection->supervision_timeout_ = supervision_timeout;
    connection->local_resolvable_private_address_ = connection_complete.GetLocalResolvablePrivateAddress();
    connection->peer_resolvable_private_address_ = connection_complete.GetPeerResolvablePrivateAddress();
    connection->in_filter_accept_list_ = in_filter_accept_list;
    connection->locally_initiated_ = (role == hci::Role::CENTRAL);

    auto connection_callbacks = connection->GetEventCallbacks(
        [this](uint16_t handle) { this->connections.invalidate(handle); });

    if (std::holds_alternative<DataAsUninitializedPeripheral>(role_specific_data)) {
      // the OnLeConnectSuccess event will be sent after receiving the On Advertising Set Terminated
      // event, since we need it to know what local_address / advertising set the peer connected to.
      // In the meantime, we store it as a pending_connection.
      connections.add(
          handle,
          remote_address,
          std::move(connection),
          queue_down_end,
          handler_,
          connection_callbacks);
    } else {
      connections.add(
          handle, remote_address, nullptr, queue_down_end, handler_, connection_callbacks);
      le_client_handler_->Post(common::BindOnce(
          &LeConnectionCallbacks::OnLeConnectSuccess,
          common::Unretained(le_client_callbacks_),
          remote_address,
          std::move(connection)));
    }
  }

  RoleSpecificData initialize_role_specific_data(Role role) {
    if (role == hci::Role::CENTRAL) {
      return DataAsCentral{le_address_manager_->GetInitiatorAddress()};
    } else if (
        controller_->SupportsBleExtendedAdvertising() ||
        controller_->IsSupported(hci::OpCode::LE_MULTI_ADVT)) {
      // when accepting connection, we must obtain the address from the advertiser.
      // When we receive "set terminated event", we associate connection handle with advertiser
      // address
      return DataAsUninitializedPeripheral{};
    } else {
      // the exception is if we only support legacy advertising - here, our current address is also
      // our advertised address
      return DataAsPeripheral{
          le_address_manager_->GetInitiatorAddress(),
          {},
          true /* For now, ignore non-discoverable legacy advertising TODO(b/254314964) */};
    }
  }

  static constexpr bool kRemoveConnectionAfterwards = true;
  void on_le_disconnect(uint16_t handle, ErrorCode reason) {
    AddressWithType remote_address = connections.getAddressWithType(handle);
    bool event_also_routes_to_other_receivers = connections.crash_on_unknown_handle_;
    connections.crash_on_unknown_handle_ = false;
    connections.execute(
        handle,
        [=](LeConnectionManagementCallbacks* callbacks) {
          round_robin_scheduler_->Unregister(handle);
          callbacks->OnDisconnection(reason);
        },
        kRemoveConnectionAfterwards);
    connections.crash_on_unknown_handle_ = event_also_routes_to_other_receivers;

    le_connectlist_manager_->OnLeDisconnect(remote_address);
  }

  void on_le_connection_update_complete(LeMetaEventView view) {
    auto complete_view = LeConnectionUpdateCompleteView::Create(view);
    if (!complete_view.IsValid()) {
      LOG_ERROR("Received on_le_connection_update_complete with invalid packet");
      return;
    }
    auto handle = complete_view.GetConnectionHandle();
    connections.execute(handle, [=](LeConnectionManagementCallbacks* callbacks) {
      callbacks->OnConnectionUpdate(
          complete_view.GetStatus(),
          complete_view.GetConnInterval(),
          complete_view.GetConnLatency(),
          complete_view.GetSupervisionTimeout());
    });
  }

  void on_le_phy_update_complete(LeMetaEventView view) {
    auto complete_view = LePhyUpdateCompleteView::Create(view);
    if (!complete_view.IsValid()) {
      LOG_ERROR("Received on_le_phy_update_complete with invalid packet");
      return;
    }
    auto handle = complete_view.GetConnectionHandle();
    connections.execute(handle, [=](LeConnectionManagementCallbacks* callbacks) {
      callbacks->OnPhyUpdate(complete_view.GetStatus(), complete_view.GetTxPhy(), complete_view.GetRxPhy());
    });
  }

  void on_le_read_remote_version_information(
      hci::ErrorCode hci_status, uint16_t handle, uint8_t version, uint16_t manufacturer_name, uint16_t sub_version) {
    connections.execute(handle, [=](LeConnectionManagementCallbacks* callbacks) {
      callbacks->OnReadRemoteVersionInformationComplete(hci_status, version, manufacturer_name, sub_version);
    });
  }

  void on_data_length_change(LeMetaEventView view) {
    auto data_length_view = LeDataLengthChangeView::Create(view);
    if (!data_length_view.IsValid()) {
      LOG_ERROR("Invalid packet");
      return;
    }
    auto handle = data_length_view.GetConnectionHandle();
    connections.execute(handle, [=](LeConnectionManagementCallbacks* callbacks) {
      callbacks->OnDataLengthChange(
          data_length_view.GetMaxTxOctets(),
          data_length_view.GetMaxTxTime(),
          data_length_view.GetMaxRxOctets(),
          data_length_view.GetMaxRxTime());
    });
  }

  void on_remote_connection_parameter_request(LeMetaEventView view) {
    auto request_view = LeRemoteConnectionParameterRequestView::Create(view);
    if (!request_view.IsValid()) {
      LOG_ERROR("Invalid packet");
      return;
    }

    auto handle = request_view.GetConnectionHandle();
    connections.execute(handle, [=](LeConnectionManagementCallbacks* callbacks) {
      // TODO: this is blindly accepting any parameters, just so we don't hang connection
      // have proper parameter negotiation
      le_acl_connection_interface_->EnqueueCommand(
          LeRemoteConnectionParameterRequestReplyBuilder::Create(
              handle,
              request_view.GetIntervalMin(),
              request_view.GetIntervalMax(),
              request_view.GetLatency(),
              request_view.GetTimeout(),
              0,
              0),
          handler_->BindOnce([](CommandCompleteView status) {}));
    });
  }

  void on_le_subrate_change(LeMetaEventView view) {
    auto subrate_change_view = LeSubrateChangeView::Create(view);
    if (!subrate_change_view.IsValid()) {
      LOG_ERROR("Invalid packet");
      return;
    }
    auto handle = subrate_change_view.GetConnectionHandle();
    connections.execute(handle, [=](LeConnectionManagementCallbacks* callbacks) {
      callbacks->OnLeSubrateChange(
          subrate_change_view.GetStatus(),
          subrate_change_view.GetSubrateFactor(),
          subrate_change_view.GetPeripheralLatency(),
          subrate_change_view.GetContinuationNumber(),
          subrate_change_view.GetSupervisionTimeout());
    });
  }

  uint16_t HACK_get_handle(Address address) {
    return connections.HACK_get_handle(address);
  }

  void OnAdvertisingSetTerminated(
      uint16_t conn_handle,
      uint8_t adv_set_id,
      hci::AddressWithType adv_set_address,
      bool is_discoverable) {
    auto connection = connections.record_peripheral_data_and_extract_pending_connection(
        conn_handle, DataAsPeripheral{adv_set_address, adv_set_id, is_discoverable});

    if (connection != nullptr) {
      le_client_handler_->Post(common::BindOnce(
          &LeConnectionCallbacks::OnLeConnectSuccess,
          common::Unretained(le_client_callbacks_),
          connection->GetRemoteAddress(),
          std::move(connection)));
    }
  }

  void create_le_connection(AddressWithType address_with_type, bool is_direct) {
    if (le_client_callbacks_ == nullptr) {
      LOG_ERROR("No callbacks to call");
      return;
    }

    if (connections.alreadyConnected(address_with_type)) {
      LOG_INFO("Device already connected, return");
      return;
    }

    le_connectlist_manager_->CreateLeConnection(address_with_type, true, is_direct);
  }

  void add_device_to_resolving_list(
      AddressWithType address_with_type,
      const std::array<uint8_t, 16>& peer_irk,
      const std::array<uint8_t, 16>& local_irk) {
    le_address_manager_->AddDeviceToResolvingList(
        address_with_type.ToPeerAddressType(), address_with_type.GetAddress(), peer_irk, local_irk);
  }

  void remove_device_from_resolving_list(AddressWithType address_with_type) {
    le_address_manager_->RemoveDeviceFromResolvingList(
        address_with_type.ToPeerAddressType(), address_with_type.GetAddress());
  }

  void set_le_suggested_default_data_parameters(uint16_t length, uint16_t time) {
    auto packet = LeWriteSuggestedDefaultDataLengthBuilder::Create(length, time);
    le_acl_connection_interface_->EnqueueCommand(
        std::move(packet), handler_->BindOnce([](CommandCompleteView complete) {}));
  }

  void LeSetDefaultSubrate(
      uint16_t subrate_min, uint16_t subrate_max, uint16_t max_latency, uint16_t cont_num, uint16_t sup_tout) {
    le_acl_connection_interface_->EnqueueCommand(
        LeSetDefaultSubrateBuilder::Create(subrate_min, subrate_max, max_latency, cont_num, sup_tout),
        handler_->BindOnce([](CommandCompleteView complete) {
          auto complete_view = LeSetDefaultSubrateCompleteView::Create(complete);
          ASSERT(complete_view.IsValid());
          ErrorCode status = complete_view.GetStatus();
          ASSERT_LOG(status == ErrorCode::SUCCESS, "Status 0x%02hhx, %s", status, ErrorCodeText(status).c_str());
        }));
  }

  void clear_resolving_list() {
    le_address_manager_->ClearResolvingList();
  }

  void set_privacy_policy_for_initiator_address(
      LeAddressManager::AddressPolicy address_policy,
      AddressWithType fixed_address,
      crypto_toolbox::Octet16 rotation_irk,
      std::chrono::milliseconds minimum_rotation_time,
      std::chrono::milliseconds maximum_rotation_time) {
    le_address_manager_->SetPrivacyPolicyForInitiatorAddress(
        address_policy,
        fixed_address,
        rotation_irk,
        controller_->SupportsBlePrivacy() && os::GetSystemPropertyBool(kPropertyEnableBlePrivacy, kEnableBlePrivacy),
        minimum_rotation_time,
        maximum_rotation_time);
  }

  // TODO(jpawlowski): remove once we have config file abstraction in cert tests
  void set_privacy_policy_for_initiator_address_for_test(
      LeAddressManager::AddressPolicy address_policy,
      AddressWithType fixed_address,
      crypto_toolbox::Octet16 rotation_irk,
      std::chrono::milliseconds minimum_rotation_time,
      std::chrono::milliseconds maximum_rotation_time) {
    le_address_manager_->SetPrivacyPolicyForInitiatorAddressForTest(
        address_policy, fixed_address, rotation_irk, minimum_rotation_time, maximum_rotation_time);
  }

  void handle_register_le_callbacks(LeConnectionCallbacks* callbacks, os::Handler* handler) {
    ASSERT(le_client_callbacks_ == nullptr);
    ASSERT(le_client_handler_ == nullptr);
    le_client_callbacks_ = callbacks;
    le_client_handler_ = handler;
    le_connectlist_manager_->RegisterTimeoutCallback(le_client_handler_->Bind(
        &LeConnectionCallbacks::OnLeConnectFail, common::Unretained(le_client_callbacks_)));
  }

  void handle_unregister_le_callbacks(LeConnectionCallbacks* callbacks, std::promise<void> promise) {
    ASSERT_LOG(le_client_callbacks_ == callbacks, "Registered le callback entity is different then unregister request");
    ASSERT_LOG(
        le_client_callbacks_ == callbacks,
        "Registered le callback entity is different then unregister request");
    le_client_callbacks_ = nullptr;
    le_client_handler_ = nullptr;
    le_connectlist_manager_->UnregisterTimeoutCallback();
    promise.set_value();
  }

  void set_system_suspend_state(bool suspended) {
    le_connect_hci_manager_->SetSystemSuspendState(suspended);
  }

  HciLayer* hci_layer_ = nullptr;
  Controller* controller_ = nullptr;
  os::Handler* handler_ = nullptr;
  RoundRobinScheduler* round_robin_scheduler_ = nullptr;
  LeAddressManager* le_address_manager_ = nullptr;
  LeAclConnectionInterface* le_acl_connection_interface_ = nullptr;
  LeConnectionCallbacks* le_client_callbacks_ = nullptr;
  LeConnectHciManager* le_connect_hci_manager_ = nullptr;
  LeConnectlistManager* le_connectlist_manager_ = nullptr;
  os::Handler* le_client_handler_ = nullptr;
};

}  // namespace acl_manager
}  // namespace hci
}  // namespace bluetooth
