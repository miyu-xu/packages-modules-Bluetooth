// Copyright 2023, The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include "connection_shim.h"

#include <cstdint>
#include <optional>

#include "hci/acl_manager.h"
#include "hci/address_with_type.h"
#include "hci/hci_packets.h"
#include "main/shim/entry.h"
#include "src/connection/ffi.rs.h"

namespace bluetooth {
namespace connection {

namespace {
hci::AddressWithType ToCppAddress(core::AddressWithType address) {
  auto hci_address = hci::Address();
  hci_address.FromOctets(address.address.data());
  return hci::AddressWithType(hci_address,
                              (hci::AddressType)address.address_type);
}

core::AddressWithType ToRustAddress(hci::AddressWithType address) {
  return core::AddressWithType{address.GetAddress().address,
                               (core::AddressType)address.GetAddressType()};
}

}  // namespace

struct LeAclManagerShim::impl : hci::acl_manager::LeConnectionCallbacks {
 public:
  impl() { acl_manager_ = shim::GetAclManager(); }

  void CreateLeConnection(core::AddressWithType address, bool is_direct) {
    acl_manager_->CreateLeConnection(ToCppAddress(address), is_direct);
  }

  void CancelLeConnect(core::AddressWithType address) {
    acl_manager_->CancelLeConnect(ToCppAddress(address));
  }

  void RegisterRustCallbacks(::rust::Box<LeAclManagerCallbackShim> callbacks) {
    callbacks_ = std::move(callbacks);
  }

  // hci::acl_manager::LeConnectionCallbacks
  virtual void OnLeConnectSuccess(
      hci::AddressWithType address,
      std::unique_ptr<hci::acl_manager::LeAclConnection> _connection) {
    callbacks_.value()->OnLeConnectSuccess(ToRustAddress(address));
  }

  virtual void OnLeConnectFail(hci::AddressWithType address,
                               hci::ErrorCode reason, bool locally_initiated) {
    callbacks_.value()->OnLeConnectFail(ToRustAddress(address),
                                        (uint8_t)reason);
  }

 private:
  std::optional<::rust::Box<LeAclManagerCallbackShim>> callbacks_;
  hci::AclManager* acl_manager_{};
};

LeAclManagerShim::LeAclManagerShim() {}

LeAclManagerShim::~LeAclManagerShim() = default;

void LeAclManagerShim::CreateLeConnection(core::AddressWithType address,
                                          bool is_direct) const {
  pimpl_->CreateLeConnection(address, is_direct);
}

void LeAclManagerShim::CancelLeConnect(core::AddressWithType address) const {
  pimpl_->CancelLeConnect(address);
}

void LeAclManagerShim::RegisterRustCallbacks(
    ::rust::Box<LeAclManagerCallbackShim> callbacks) {
  pimpl_->RegisterRustCallbacks(std::move(callbacks));
}

void RegisterRustApis(
    ::rust::Fn<void(uint8_t client_id, core::AddressWithType address)>
        start_direct_connection,
    ::rust::Fn<void(uint8_t client_id, core::AddressWithType address)>
        stop_direct_connection,
    ::rust::Fn<void(uint8_t client_id, core::AddressWithType address)>
        add_background_connection,
    ::rust::Fn<void(uint8_t client_id, core::AddressWithType address)>
        remove_background_connection,
    ::rust::Fn<void(uint8_t client_id)> stop_all_connections_from_client,
    ::rust::Fn<void(core::AddressWithType address)>
        stop_all_connections_to_device) {}

}  // namespace connection
}  // namespace bluetooth
