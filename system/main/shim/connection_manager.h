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

#include <memory>
#include <string>
#include <utility>

#include "hci/hci_layer.h"
#include "le_address_manager.h"
#include "module.h"
#include "rust/cxx.h"

namespace bluetooth {
namespace connection {

/// This is the GD interface for the connection manager (that predominantly
/// lives in Rust). It stores callbacks registered from the upper layer,
/// and forwards calls to the appropriate module/handler.
class ConnectionManager : public Module, hci::LeAddressManagerCallback {
 public:
  void LeCreateConnection(bool use_fast_parameters) const;
  void LeCancelConnection() const;

  void AddToFilterAcceptList(AddressWithType address) const;
  void RemoveFromFilterAcceptList(AddressWithType address) const;

  void AckPause() const;
  void AckResume() const;

  void Pause() const;
  void Resume() const;

  void StoreHciCallbacksFromRust(
      ::rust::Fn<void(uint8_t status)> on_create_connection_status,
      ::rust::Fn<void(AddressWithType address, RoleForFFI role, uint8_t status)>
          on_connection_complete,
      ::rust::Fn<void(AddressWithType address)> on_disconnect);

  void RegisterWithAddressManager(::rust::Fn<void()> pause,
                                  ::rust::Fn<void()> resume);

 protected:
  void ListDependencies(ModuleList* list) const override;
  void Start() override;
  void Stop() override;

  std::string ToString() const override {
    return std::string("ConnectionManagerModule");
  }

 private:
  ::rust::Fn<void()> pause_;
  ::rust::Fn<void()> resume_;

  ::rust::Fn<void(uint8_t status)> on_create_connection_status_;
  ::rust::Fn<void(AddressWithType address, RoleForFFI role, uint8_t status)>
      on_connection_complete_;
  ::rust::Fn<void(AddressWithType address)> on_disconnect_;

  hci::HciLayer* hci_layer_;
  hci::AclManager* acl_manager_;
  hci::LeAddressManager* address_manager_;

 public:
  static const ModuleFactory Factory;
  ConnectionManager();
  ~ConnectionManager();
};

}  // namespace connection
}  // namespace bluetooth