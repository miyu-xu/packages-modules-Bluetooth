/*
 * Copyright 2023 The Android Open Source Project
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

#include "le_connectlist_manager.h"

#include <base/strings/stringprintf.h>

#include <map>
#include <string>
#include <unordered_map>

#include "common/callback.h"
#include "common/contextual_callback.h"
#include "hci/acl_manager/le_connectlist_manager.h"
#include "hci/address_with_type.h"
#include "hci/le_address_manager.h"
#include "os/alarm.h"
#include "os/metrics.h"
#include "os/system_properties.h"

namespace bluetooth {
namespace hci {
namespace acl_manager {

namespace {

constexpr uint32_t kCreateConnectionTimeoutMs = 30 * 1000;

static const std::string kPropertyDirectConnTimeout = "bluetooth.core.le.direct_connection_timeout";

}  // namespace

LeConnectlistManager::LeConnectlistManager(
    LeAddressManager* le_address_manager,
    LeConnectHciManager* le_connect_hci_manager,
    common::ContextualCallback<void(AddressWithType, ErrorCode)> on_timeout,
    os::Handler* handler)
    : le_address_manager_(le_address_manager),
      le_connect_hci_manager_(le_connect_hci_manager),
      on_timeout_(on_timeout),
      handler_(handler) {}

LeConnectlistManager::~LeConnectlistManager() {
  if (address_manager_registered) {
    le_address_manager_->UnregisterSync(this);
  }
}

#define CASE_RETURN_TEXT(code) \
  case code:                   \
    return #code

std::string LeConnectlistManager::ConnectabilityStateMachineText(const ConnectabilityState& state) {
  switch (state) {
    CASE_RETURN_TEXT(ConnectabilityState::DISARMED);
    CASE_RETURN_TEXT(ConnectabilityState::ARMING);
    CASE_RETURN_TEXT(ConnectabilityState::ARMED);
    CASE_RETURN_TEXT(ConnectabilityState::DISARMING);
    default:
      return base::StringPrintf("UNKNOWN[%d]", state);
  }
}
#undef CASE_RETURN_TEXT

// connection canceled by LeAddressManager.OnPause(), will auto reconnect by
// LeAddressManager.OnResume()
void LeConnectlistManager::OnLeConnectionCancelledOnPause() {
  ASSERT_LOG(pause_connection, "Connection must be paused to ack the le address manager");
  arm_on_resume_ = true;
  connectability_state_ = ConnectabilityState::DISARMED;
  le_address_manager_->AckPause(this);
}

void LeConnectlistManager::OnCommonLeConnectionComplete(AddressWithType address_with_type) {
  auto connecting_addr_with_type = connecting_le_.find(address_with_type);
  if (connecting_addr_with_type == connecting_le_.end()) {
    LOG_WARN("No prior connection request for %s", ADDRESS_TO_LOGGABLE_CSTR(address_with_type));
  }
  connecting_le_.clear();

  if (create_connection_timeout_alarms_.find(address_with_type) !=
      create_connection_timeout_alarms_.end()) {
    create_connection_timeout_alarms_.at(address_with_type).Cancel();
    create_connection_timeout_alarms_.erase(address_with_type);
  }
}

void LeConnectlistManager::OnCreateConnectionTimeout(AddressWithType address_with_type) {
  LOG_INFO(
      "on_create_connection_timeout, address: %s", ADDRESS_TO_LOGGABLE_CSTR(address_with_type));
  if (create_connection_timeout_alarms_.find(address_with_type) !=
      create_connection_timeout_alarms_.end()) {
    create_connection_timeout_alarms_.at(address_with_type).Cancel();
    create_connection_timeout_alarms_.erase(address_with_type);
    auto argument_list = std::vector<std::pair<os::ArgumentType, int>>();
    argument_list.push_back(std::make_pair(
        os::ArgumentType::ACL_STATUS_CODE,
        static_cast<int>(android::bluetooth::hci::StatusEnum::STATUS_CONNECTION_TOUT)));
    bluetooth::os::LogMetricBluetoothLEConnectionMetricEvent(
        address_with_type.GetAddress(),
        android::bluetooth::le::LeConnectionOriginType::ORIGIN_NATIVE,
        android::bluetooth::le::LeConnectionType::CONNECTION_TYPE_LE_ACL,
        android::bluetooth::le::LeConnectionState::STATE_LE_ACL_TIMEOUT,
        argument_list);

    if (background_connections_.find(address_with_type) != background_connections_.end()) {
      direct_connections_.erase(address_with_type);
      DisarmConnectability();
    } else {
      CancelConnect(address_with_type);
    }
    on_timeout_.Invoke(address_with_type, ErrorCode::CONNECTION_ACCEPT_TIMEOUT);
  }
}

void LeConnectlistManager::CreateLeConnection(
    AddressWithType address_with_type, bool add_to_connect_list, bool is_direct) {
  // TODO: Configure default LE connection parameters?
  if (add_to_connect_list) {
    AddDeviceToConnectList(address_with_type);
    if (is_direct) {
      direct_connections_.insert(address_with_type);
      if (create_connection_timeout_alarms_.find(address_with_type) ==
          create_connection_timeout_alarms_.end()) {
        create_connection_timeout_alarms_.emplace(
            std::piecewise_construct,
            std::forward_as_tuple(
                address_with_type.GetAddress(), address_with_type.GetAddressType()),
            std::forward_as_tuple(handler_));
        uint32_t connection_timeout =
            os::GetSystemPropertyUint32(kPropertyDirectConnTimeout, kCreateConnectionTimeoutMs);
        create_connection_timeout_alarms_.at(address_with_type)
            .Schedule(
                common::BindOnce(
                    &LeConnectlistManager::OnCreateConnectionTimeout,
                    common::Unretained(this),
                    address_with_type),
                std::chrono::milliseconds(connection_timeout));
      }
    }
  }

  if (!address_manager_registered) {
    auto policy = le_address_manager_->Register(this);
    address_manager_registered = true;

    // Pause connection, wait for set random address complete
    if (policy == LeAddressManager::AddressPolicy::USE_RESOLVABLE_ADDRESS ||
        policy == LeAddressManager::AddressPolicy::USE_NON_RESOLVABLE_ADDRESS) {
      pause_connection = true;
    }
  }

  if (pause_connection) {
    arm_on_resume_ = true;
    return;
  }

  switch (connectability_state_) {
    case ConnectabilityState::ARMED:
    case ConnectabilityState::ARMING:
      // Ignored, if we add new device to the filter accept list, create connection command will be
      // sent by OnResume.
      LOG_DEBUG(
          "Deferred until filter accept list updated create connection state %s",
          ConnectabilityStateMachineText(connectability_state_).c_str());
      break;
    default:
      // If we added to filter accept list then the arming of the le state machine
      // must wait until the filter accept list command as completed
      if (add_to_connect_list) {
        arm_on_resume_ = true;
        LOG_DEBUG("Deferred until filter accept list has completed");
      } else {
        handler_->CallOn(this, &LeConnectlistManager::ArmConnectability);
      }
      break;
  }
}

void LeConnectlistManager::CancelConnect(AddressWithType address_with_type) {
  // Remove any alarms for this peer, if any
  if (create_connection_timeout_alarms_.find(address_with_type) !=
      create_connection_timeout_alarms_.end()) {
    create_connection_timeout_alarms_.at(address_with_type).Cancel();
    create_connection_timeout_alarms_.erase(address_with_type);
  }
  // the connection will be canceled by LeAddressManager.OnPause()
  RemoveDeviceFromConnectList(address_with_type);
}

ConnectionResult LeConnectlistManager::OnConnectionComplete(
    AddressWithType remote_address, ErrorCode status, Role role) {
  const bool in_filter_accept_list = IsDeviceInConnectList(remote_address);

  auto argument_list = std::vector<std::pair<bluetooth::os::ArgumentType, int>>();
  argument_list.push_back(
      std::make_pair(os::ArgumentType::ACL_STATUS_CODE, static_cast<int>(status)));

  bluetooth::os::LogMetricBluetoothLEConnectionMetricEvent(
      remote_address.GetAddress(),
      android::bluetooth::le::LeConnectionOriginType::ORIGIN_NATIVE,
      android::bluetooth::le::LeConnectionType::CONNECTION_TYPE_LE_ACL,
      android::bluetooth::le::LeConnectionState::STATE_LE_ACL_END,
      argument_list);

  if (role == hci::Role::CENTRAL) {
    connectability_state_ = ConnectabilityState::DISARMED;
    if (status == ErrorCode::UNKNOWN_CONNECTION && pause_connection) {
      OnLeConnectionCancelledOnPause();
      return ConnectionResult::Ignore;
    }
    OnCommonLeConnectionComplete(remote_address);
    if (status == ErrorCode::UNKNOWN_CONNECTION) {
      if (remote_address.GetAddress() != Address::kEmpty) {
        LOG_INFO(
            "Controller send non-empty address field:%s",
            ADDRESS_TO_LOGGABLE_CSTR(remote_address.GetAddress()));
      }
      // direct connect canceled due to connection timeout, start background connect
      CreateLeConnection(remote_address, false, false);
      return ConnectionResult::Ignore;
    }

    arm_on_resume_ = false;
    ready_to_unregister = true;
    RemoveDeviceFromConnectList(remote_address);

    if (!connect_list.empty()) {
      AddressWithType empty(Address::kEmpty, AddressType::RANDOM_DEVICE_ADDRESS);
      handler_->Post(common::BindOnce(
          &LeConnectlistManager::CreateLeConnection,
          common::Unretained(this),
          empty,
          false,
          false));
    }

    if (status != ErrorCode::SUCCESS) {
      return ConnectionResult::Failure;
    }
  } else {
    LOG_INFO("Received connection complete with Peripheral role");

    if (status != ErrorCode::SUCCESS) {
      std::string error_code = ErrorCodeText(status);
      LOG_WARN("Received on_le_connection_complete with error code %s", error_code.c_str());
      return ConnectionResult::Failure;
    }

    if (in_filter_accept_list) {
      LOG_INFO(
          "Received incoming connection of device in filter accept_list, %s",
          ADDRESS_TO_LOGGABLE_CSTR(remote_address));
      RemoveDeviceFromConnectList(remote_address);
      if (create_connection_timeout_alarms_.find(remote_address) !=
          create_connection_timeout_alarms_.end()) {
        create_connection_timeout_alarms_.at(remote_address).Cancel();
        create_connection_timeout_alarms_.erase(remote_address);
      }
    }
  }

  return in_filter_accept_list ? ConnectionResult::Success
                               : ConnectionResult::SuccessButNotInFilterAcceptList;
}

void LeConnectlistManager::OnLeDisconnect(AddressWithType remote_address) {
  if (background_connections_.count(remote_address) == 1) {
    LOG_INFO("re-add device to connect list");
    arm_on_resume_ = true;
    AddDeviceToConnectList(remote_address);
  }
}

void LeConnectlistManager::ArmConnectability() {
  if (connectability_state_ != ConnectabilityState::DISARMED) {
    LOG_ERROR(
        "Attempting to re-arm le connection state machine in unexpected state:%s",
        ConnectabilityStateMachineText(connectability_state_).c_str());
    return;
  }
  if (connect_list.empty()) {
    LOG_INFO(
        "Ignored request to re-arm le connection state machine when filter accept list is empty");
    return;
  }
  AddressWithType empty(Address::kEmpty, AddressType::RANDOM_DEVICE_ADDRESS);
  connectability_state_ = ConnectabilityState::ARMING;
  connecting_le_ = connect_list;

  le_connect_hci_manager_->LeCreateConnection(
      /* use_fast =*/!direct_connections_.empty(),
      handler_->BindOnce(
          &LeConnectlistManager::UpdateConnectabilityStateAfterArmed, common::Unretained(this)));
}

void LeConnectlistManager::DisarmConnectability() {
  auto argument_list = std::vector<std::pair<os::ArgumentType, int>>();
  bluetooth::os::LogMetricBluetoothLEConnectionMetricEvent(
      Address::kEmpty,
      os::LeConnectionOriginType::ORIGIN_UNSPECIFIED,
      os::LeConnectionType::CONNECTION_TYPE_LE_ACL,
      os::LeConnectionState::STATE_LE_ACL_CANCEL,
      argument_list);

  switch (connectability_state_) {
    case ConnectabilityState::ARMED:
      LOG_INFO("Disarming LE connection state machine with create connection cancel");
      connectability_state_ = ConnectabilityState::DISARMING;
      le_connect_hci_manager_->LeCancelConnection(
          handler_->BindOnceOn(this, &LeConnectlistManager::OnCancelConnectionComplete));
      break;

    case ConnectabilityState::ARMING:
      LOG_INFO("Queueing cancel connect until after connection state machine is armed");
      disarmed_while_arming_ = true;
      break;
    case ConnectabilityState::DISARMING:
    case ConnectabilityState::DISARMED:
      LOG_ERROR(
          "Attempting to disarm le connection state machine in unexpected state:%s",
          ConnectabilityStateMachineText(connectability_state_).c_str());
      break;
  }
}

void LeConnectlistManager::OnCancelConnectionComplete(ErrorCode status) {
  if (status != ErrorCode::SUCCESS) {
    std::string error_code = ErrorCodeText(status);
    LOG_WARN(
        "Received on_create_connection_cancel_complete with error code %s", error_code.c_str());
    if (pause_connection) {
      LOG_WARN("AckPause");
      le_address_manager_->AckPause(this);
      return;
    }
  }
  if (connectability_state_ != ConnectabilityState::DISARMING) {
    LOG_ERROR(
        "Attempting to disarm le connection state machine in unexpected state:%s",
        ConnectabilityStateMachineText(connectability_state_).c_str());
  }
}

void LeConnectlistManager::UpdateConnectabilityStateAfterArmed(ErrorCode status) {
  switch (connectability_state_) {
    case ConnectabilityState::DISARMED:
    case ConnectabilityState::ARMED:
    case ConnectabilityState::DISARMING:
      LOG_ERROR(
          "Received connectability arm notification for unexpected state:%s status:%s",
          ConnectabilityStateMachineText(connectability_state_).c_str(),
          ErrorCodeText(status).c_str());
      break;
    case ConnectabilityState::ARMING:
      if (status != ErrorCode::SUCCESS) {
        LOG_ERROR(
            "Le connection state machine armed failed status:%s", ErrorCodeText(status).c_str());
      }
      connectability_state_ = (status == ErrorCode::SUCCESS) ? ConnectabilityState::ARMED
                                                             : ConnectabilityState::DISARMED;
      LOG_INFO(
          "Le connection state machine armed state:%s status:%s",
          ConnectabilityStateMachineText(connectability_state_).c_str(),
          ErrorCodeText(status).c_str());
      if (disarmed_while_arming_) {
        disarmed_while_arming_ = false;
        DisarmConnectability();
      }
  }
}

void LeConnectlistManager::AddDeviceToConnectList(AddressWithType address_with_type) {
  if (connect_list.find(address_with_type) != connect_list.end()) {
    LOG_WARN(
        "Device already exists in acceptlist and cannot be added:%s",
        ADDRESS_TO_LOGGABLE_CSTR(address_with_type));
    return;
  }

  connect_list.insert(address_with_type);
  RegisterWithAddressManager();
  le_connect_hci_manager_->AddToFilterAcceptList(address_with_type);
}

bool LeConnectlistManager::IsDeviceInConnectList(AddressWithType address_with_type) {
  return (connect_list.find(address_with_type) != connect_list.end());
}

void LeConnectlistManager::RemoveDeviceFromConnectList(AddressWithType address_with_type) {
  if (connect_list.find(address_with_type) == connect_list.end()) {
    LOG_WARN(
        "Device not in acceptlist and cannot be removed:%s",
        ADDRESS_TO_LOGGABLE_CSTR(address_with_type));
    return;
  }
  connect_list.erase(address_with_type);
  connecting_le_.erase(address_with_type);
  direct_connections_.erase(address_with_type);
  RegisterWithAddressManager();
  le_connect_hci_manager_->RemoveFromFilterAcceptList(address_with_type);
}

void LeConnectlistManager::ClearFilterAcceptList() {
  connect_list.clear();
  RegisterWithAddressManager();
  le_address_manager_->ClearFilterAcceptList();
}

void LeConnectlistManager::RegisterWithAddressManager() {
  if (!address_manager_registered) {
    le_address_manager_->Register(this);
    address_manager_registered = true;
    pause_connection = true;
  }
}

void LeConnectlistManager::CheckForUnregister() {
  if (connecting_le_.empty() && address_manager_registered && ready_to_unregister) {
    le_address_manager_->Unregister(this);
    address_manager_registered = false;
    pause_connection = false;
    ready_to_unregister = false;
  }
}

void LeConnectlistManager::OnPause() {  // bluetooth::hci::LeAddressManagerCallback
  if (!address_manager_registered) {
    LOG_WARN("Unregistered!");
    return;
  }
  pause_connection = true;
  if (connectability_state_ == ConnectabilityState::DISARMED) {
    le_address_manager_->AckPause(this);
    return;
  }
  arm_on_resume_ = !connecting_le_.empty();
  DisarmConnectability();
}

void LeConnectlistManager::OnResume() {  // bluetooth::hci::LeAddressManagerCallback
  if (!address_manager_registered) {
    LOG_WARN("Unregistered!");
    return;
  }
  pause_connection = false;
  if (arm_on_resume_) {
    ArmConnectability();
  }
  arm_on_resume_ = false;
  le_address_manager_->AckResume(this);
  CheckForUnregister();
}

void LeConnectlistManager::AddDeviceToBackgroundConnectionList(AddressWithType address_with_type) {
  background_connections_.insert(address_with_type);
}

void LeConnectlistManager::RemoveDeviceFromBackgroundConnectionList(
    AddressWithType address_with_type) {
  background_connections_.erase(address_with_type);
}

void LeConnectlistManager::IsOnBackgroundConnectionList(
    AddressWithType address_with_type, std::promise<bool> promise) {
  promise.set_value(
      background_connections_.find(address_with_type) != background_connections_.end());
}

}  // namespace acl_manager
}  // namespace hci
}  // namespace bluetooth