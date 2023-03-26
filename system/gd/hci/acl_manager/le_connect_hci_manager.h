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

#include "common/contextual_callback.h"
#include "hci/controller.h"
#include "hci/hci_layer.h"
#include "hci/le_address_manager.h"
#include "os/handler.h"

namespace bluetooth {
namespace hci {
namespace acl_manager {

class LeConnectHciManager {
 public:
  LeConnectHciManager(
      Controller* controller,
      LeAddressManager* le_address_manager,
      LeAclConnectionInterface* le_acl_connection_interface,
      os::Handler* handler);

  void LeCreateConnection(
      bool use_fast_parameters, common::ContextualOnceCallback<void(ErrorCode)> on_complete);

  void LeCancelConnection(common::ContextualOnceCallback<void(ErrorCode)> on_complete);

  void AddToFilterAcceptList(AddressWithType address);

  void RemoveFromFilterAcceptList(AddressWithType address);

  void SetSystemSuspendState(bool suspended);

 private:
  bool system_suspend_ = false;
  Controller* controller_;
  LeAddressManager* le_address_manager_;
  LeAclConnectionInterface* le_acl_connection_interface_;
  os::Handler* handler_;
};

}  // namespace acl_manager
}  // namespace hci
}  // namespace bluetooth