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

#include "connection_manager.h"

#include "hci/acl_manager.h"
#include "hci/controller.h"
#include "hci/hci_layer.h"

namespace bluetooth {
namespace connection {

const ModuleFactory ConnectionManager::Factory =
    ModuleFactory([]() { return new ConnectionManager(); });
ConnectionManager::ConnectionManager() = default;
ConnectionManager::~ConnectionManager() = default;

void ConnectionManager::ListDependencies(ModuleList* list) const {
  list->add<hci::HciLayer>();
  list->add<hci::AclManager>();
  list->add<hci::Controller>();
}

void ConnectionManager::Start() {
  hci_layer_ = GetDependency<hci::HciLayer>();
  acl_manager_ = GetDependency<hci::AclManager>();
  address_manager_ = acl_manager_->GetLeAddressManager();
  acl_manager_->Register(this);
}

void ConnectionManager::Stop() { acl_manager_->Unregister(this); }

void ConnectionManager::Pause() { pause_(); }
void ConnectionManager::Resume() { resume_(); }

void ConnectionManager::AckPause() { address_manager_->AckPause(this); }
void ConnectionManager::AckResume() { address_manager_->AckResume(this); }

}  // namespace connection
}  // namespace bluetooth