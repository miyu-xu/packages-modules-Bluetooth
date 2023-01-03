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

#pragma once

#include <base/bind.h>

#include "arbiter_client_types.h"
#include "arbiter_filters.h"
#include "hci/acl_manager.h"
#include "module.h"
#include "packet/raw_builder.h"

namespace bluetooth {
namespace arbiter {

// The LeConnectionArbiter allows us to share an incoming LE connection with multiple BLE
// stacks ("clients"). Each client MAY register a set of ConnectionCallbacks, and the Arbiter will
// invoke the appropriate ones using the supplied ConnectionFilter (multiple matches are ALLOWED).
// If the registered client receives an LeAclConnection, it MUST register itself for callbacks on
// that connection - the Arbiter guarantees that multiple connections may register themselves with
// no conflict.
//
// Considering all clients with matching filters, all packets sent on the LE connection will pass
// through each of the their PacketFilters until a match is found (multiple matches are NOT
// allowed).
//
// Reference-counting across clients will unregister the handle from le_impl once the
// connection is dropped. Clients MUST drop their reference to a received connection once it is
// disconnected, to avoid dangling handles in le_impl (since after disconnection the controller may
// reuse a handle value).
//
// Filter matching is done based on client priority, with tiebreak using LIFO based on client
// registration order, for both ConnectionFilters and PacketFilters, until the FIRST filter is
// matched.
class LeConnectionArbiterModule : public bluetooth::Module {
 public:
  // Register a new client to participate in the arbitration logic described above. The
  // Token returned can be used for de-registration. The supplied handler will be used to
  // invoke all methods in the ConnectionCallbacks and supplied filters.
  Token RegisterClient(
      hci::acl_manager::LeConnectionCallbacks* callbacks,
      os::Handler* handler,
      ConnectionFilter connection_filter,
      PacketFilter packet_filter,
      ClientPriority priority);

  // Remove a client from arbitration. This is done asynchronously, but the supplied promise will be
  // fulfilled once removal is complete. Note that, if a client holds a connection, it will remain
  // active + packet filters will be invoked even after unregistration, until the connection is
  // disconnected / destructed. However, after unregistration, it is guaranteed that
  // ConnectionFilters will no longer be invoked.
  void UnregisterClient(Token token, std::promise<void> promise);

 private:
  struct impl;
  std::unique_ptr<impl> pimpl_;

 protected:
  void ListDependencies(ModuleList* list) const override;
  void Start() override;
  void Stop() override;
  std::string ToString() const override {
    return std::string("LeConnectionArbiterModule");
  }

 public:
  static const ModuleFactory Factory;
  LeConnectionArbiterModule();
  ~LeConnectionArbiterModule();
};

}  // namespace arbiter
}  // namespace bluetooth
