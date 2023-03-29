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

#pragma once

#include <cstdint>

#include "rust/cxx.h"
#include "src/core/ffi/types.h"

namespace bluetooth {
namespace connection {

using ::bluetooth::core::AddressWithType;

enum class RoleForFFI : uint32_t {
  CENTRAL = 0,
  PERIPHERAL = 1,
};

class LeConnectHciManagerShim {
 public:
  LeConnectHciManagerShim();

  void LeCreateConnection(bool use_fast_parameters) const;
  void LeCancelConnection() const;

  void AddToFilterAcceptList(AddressWithType address) const;
  void RemoveFromFilterAcceptList(AddressWithType address) const;
};

void StoreHciCallbacksFromRust(
    ::rust::Fn<void(uint8_t status)> on_create_connection_status,
    ::rust::Fn<void(AddressWithType address, RoleForFFI role, uint8_t status)>
        on_connection_complete,
    ::rust::Fn<void(AddressWithType address)> on_disconnect);

class LeAddressManagerShim {
 public:
  LeAddressManagerShim();

  void AckPause() const;
  void AckResume() const;
};

void RegisterWithAddressManager(::rust::Fn<void()> pause,
                                ::rust::Fn<void()> resume);

void RegisterRustApis(
    ::rust::Fn<void(uint8_t client_id, AddressWithType address)>
        start_direct_connection,
    ::rust::Fn<void(uint8_t client_id, AddressWithType address)>
        stop_direct_connection,
    ::rust::Fn<void(uint8_t client_id, AddressWithType address)>
        add_background_connection,
    ::rust::Fn<void(uint8_t client_id, AddressWithType address)>
        remove_background_connection,
    ::rust::Fn<void(uint8_t client_id)> stop_all_connections_from_client,
    ::rust::Fn<void(AddressWithType address)> stop_all_connections_to_device);

}  // namespace connection
}  // namespace bluetooth
