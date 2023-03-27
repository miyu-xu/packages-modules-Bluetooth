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

#pragma once

#include <gtest/gtest_prod.h>

#include <map>
#include <string>
#include <unordered_map>
#include <unordered_set>

#include "common/callback.h"
#include "hci/acl_manager/le_connect_hci_manager.h"
#include "hci/address_with_type.h"
#include "hci/le_address_manager.h"
#include "os/alarm.h"
#include "os/handler.h"

namespace bluetooth {
namespace hci {
namespace acl_manager {

enum class ConnectionResult {
  Success,
  SuccessButNotInFilterAcceptList,
  Failure,
  Ignore,
};

class LeConnectlistManager : public bluetooth::hci::LeAddressManagerCallback {
 public:
  LeConnectlistManager(
      LeAddressManager* le_address_manager,
      LeConnectHciManager* le_connect_hci_manager,
      os::Handler* handler);
  ~LeConnectlistManager();
  LeConnectlistManager(const LeConnectlistManager&) = delete;
  LeConnectlistManager& operator=(const LeConnectlistManager&) = delete;

  // API
  void RegisterTimeoutCallback(
      common::ContextualCallback<void(AddressWithType, ErrorCode)> on_timeout);

  void CreateLeConnection(
      AddressWithType address_with_type, bool add_to_connect_list, bool is_direct);
  void CancelConnect(AddressWithType address_with_type);
  void OnLeDisconnect(AddressWithType address_with_type);

  void AddDeviceToBackgroundConnectionList(AddressWithType address_with_type);
  void IsOnBackgroundConnectionList(AddressWithType address_with_type, std::promise<bool> promise);
  void RemoveDeviceFromBackgroundConnectionList(AddressWithType address_with_type);

  void ClearFilterAcceptList();

  // LeAddressManagerCallback
  void OnPause() override;
  void OnResume() override;

  // HCI events
  ConnectionResult OnConnectionComplete(
      AddressWithType remote_address, ErrorCode status, Role role);

 private:
  void OnCancelConnectionComplete(ErrorCode status);

  void ArmConnectability();
  void DisarmConnectability();
  void UpdateConnectabilityStateAfterArmed(ErrorCode status);

  void AddDeviceToConnectList(AddressWithType address_with_type);
  bool IsDeviceInConnectList(AddressWithType address_with_type);
  void RemoveDeviceFromConnectList(AddressWithType address_with_type);

  void RegisterWithAddressManager();
  void CheckForUnregister();

  void OnCreateConnectionTimeout(AddressWithType address_with_type);
  void OnLeConnectionCancelledOnPause();

  LeAddressManager* le_address_manager_ = nullptr;
  LeConnectHciManager* le_connect_hci_manager_ = nullptr;
  common::ContextualCallback<void(AddressWithType, ErrorCode)> on_timeout_;
  os::Handler* handler_;

  std::unordered_set<AddressWithType> connecting_le_{};
  bool arm_on_resume_{};
  std::unordered_set<AddressWithType> direct_connections_{};
  // Set of devices that will not be removed from connect list after direct connect timeout
  std::unordered_set<AddressWithType> background_connections_;
  std::unordered_set<AddressWithType> connect_list;
  bool address_manager_registered = false;
  bool ready_to_unregister = false;
  bool pause_connection = false;
  bool disarmed_while_arming_ = false;

  enum class ConnectabilityState {
    DISARMED = 0,
    ARMING = 1,
    ARMED = 2,
    DISARMING = 3,
  };
  static std::string ConnectabilityStateMachineText(const ConnectabilityState& state);

  LeConnectlistManager::ConnectabilityState connectability_state_{ConnectabilityState::DISARMED};
  std::map<AddressWithType, os::Alarm> create_connection_timeout_alarms_{};

  friend class LeImplTest;
  friend class LeImplRegisteredWithAddressManagerTest;
  FRIEND_TEST(LeImplTest, add_device_to_connect_list);
  FRIEND_TEST(LeImplTest, remove_device_from_connect_list);
  FRIEND_TEST(LeImplTest, connection_complete_with_periperal_role);
  FRIEND_TEST(LeImplTest, enhanced_connection_complete_with_periperal_role);
  FRIEND_TEST(LeImplTest, connection_complete_with_central_role);
  FRIEND_TEST(LeImplTest, enhanced_connection_complete_with_central_role);
  FRIEND_TEST(LeImplTest, DISABLED_register_with_address_manager__AddressPolicyNotSet);
  FRIEND_TEST(LeImplTest, DISABLED_disarm_connectability_DISARMED);
  FRIEND_TEST(LeImplTest, DISABLED_disarm_connectability_DISARMED_extended);
  FRIEND_TEST(LeImplTest, DISABLED_disarm_connectability_ARMING);
  FRIEND_TEST(LeImplTest, DISABLED_disarm_connectability_ARMING_extended);
  FRIEND_TEST(LeImplTest, DISABLED_disarm_connectability_ARMED);
  FRIEND_TEST(LeImplTest, DISABLED_disarm_connectability_ARMED_extended);
  FRIEND_TEST(LeImplTest, DISABLED_disarm_connectability_DISARMING);
  FRIEND_TEST(LeImplTest, DISABLED_disarm_connectability_DISARMING_extended);
  FRIEND_TEST(LeImplTest, DISABLED_register_with_address_manager__AddressPolicyPublicAddress);
  FRIEND_TEST(LeImplTest, DISABLED_register_with_address_manager__AddressPolicyStaticAddress);
  FRIEND_TEST(
      LeImplTest, DISABLED_register_with_address_manager__AddressPolicyNonResolvableAddress);
  FRIEND_TEST(LeImplTest, DISABLED_register_with_address_manager__AddressPolicyResolvableAddress);
  FRIEND_TEST(LeImplTest, connectability_state_machine_text);
  FRIEND_TEST(LeImplRegisteredWithAddressManagerTest, ignore_on_pause_on_resume_after_unregistered);
  FRIEND_TEST(LeImplTest, on_le_connection_canceled_on_pause);
  FRIEND_TEST(LeImplTest, on_create_connection_timeout);
  FRIEND_TEST(LeImplTest, DISABLED_on_common_le_connection_complete__NoPriorConnection);
  FRIEND_TEST(LeImplTest, cancel_connect);
};

}  // namespace acl_manager
}  // namespace hci
}  // namespace bluetooth