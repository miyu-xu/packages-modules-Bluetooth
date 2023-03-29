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
#include "stack/btm/btm_dev.h"

extern const tBLE_BD_ADDR convert_to_address_with_type(
    const RawAddress& bd_addr, const tBTM_SEC_DEV_REC* p_dev_rec);

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
core::AddressWithType ToRustAddress(tBLE_BD_ADDR address) {
  return core::AddressWithType{address.bda.ToArray(),
                               address.IsPublic() ? core::AddressType::Public
                                                  : core::AddressType::Random};
}

}  // namespace

struct LeAclManagerShim::impl : hci::acl_manager::LeAcceptlistCallbacks {
 public:
  impl() {
    acl_manager_ = shim::GetAclManager();
    acl_manager_->RegisterLeAcceptlistCallbacks(this);
  }

  void CreateLeConnection(core::AddressWithType address, bool is_direct) {
    acl_manager_->CreateLeConnection(ToCppAddress(address), is_direct);
  }

  void CancelLeConnect(core::AddressWithType address) {
    acl_manager_->CancelLeConnect(ToCppAddress(address));
  }

  void RegisterRustCallbacks(::rust::Box<LeAclManagerCallbackShim> callbacks) {
    callbacks_ = std::move(callbacks);
  }

  // hci::acl_manager::LeAcceptlistCallbacks
  virtual void OnLeConnectSuccess(hci::AddressWithType address) {
    callbacks_.value()->OnLeConnectSuccess(ToRustAddress(address));
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

namespace {
struct RustConnectionManager {
  ::rust::Fn<void(uint8_t client_id, core::AddressWithType address)>
      start_direct_connection;
  ::rust::Fn<void(uint8_t client_id, core::AddressWithType address)>
      stop_direct_connection;
  ::rust::Fn<void(uint8_t client_id, core::AddressWithType address)>
      add_background_connection;
  ::rust::Fn<void(uint8_t client_id, core::AddressWithType address)>
      remove_background_connection;
  ::rust::Fn<void(uint8_t client_id)> stop_all_connections_from_client;
  ::rust::Fn<void(core::AddressWithType address)>
      stop_all_connections_to_device;
};

std::optional<RustConnectionManager> connection_manager;

}  // namespace

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
        stop_all_connections_to_device) {
  connection_manager = {
      start_direct_connection,          stop_direct_connection,
      add_background_connection,        remove_background_connection,
      stop_all_connections_from_client, stop_all_connections_to_device};
}

tBLE_BD_ADDR ResolveRawAddress(RawAddress bd_addr) {
  tBTM_SEC_DEV_REC* p_dev_rec = btm_find_dev(bd_addr);
  return convert_to_address_with_type(bd_addr, p_dev_rec);
}

void StartDirectConnection(uint8_t client_id, tBLE_BD_ADDR address_with_type) {
  connection_manager->start_direct_connection(client_id,
                                              ToRustAddress(address_with_type));
}
void StopDirectConnection(uint8_t client_id, tBLE_BD_ADDR address_with_type) {
  connection_manager->stop_direct_connection(client_id,
                                             ToRustAddress(address_with_type));
}
void AddBackgroundConnection(uint8_t client_id,
                             tBLE_BD_ADDR address_with_type) {
  connection_manager->add_background_connection(
      client_id, ToRustAddress(address_with_type));
}
void RemoveBackgroundConnection(uint8_t client_id,
                                tBLE_BD_ADDR address_with_type) {
  connection_manager->remove_background_connection(
      client_id, ToRustAddress(address_with_type));
}
void StopAllConnectionsFromClient(uint8_t client_id) {
  connection_manager->stop_all_connections_from_client(client_id);
}
void StopAllConnectionsToDevice(tBLE_BD_ADDR address_with_type) {
  connection_manager->stop_all_connections_to_device(
      ToRustAddress(address_with_type));
}

}  // namespace connection
}  // namespace bluetooth
