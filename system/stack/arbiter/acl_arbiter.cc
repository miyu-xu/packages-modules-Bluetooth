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

#include "stack/arbiter/acl_arbiter.h"

#include <base/bind.h>

#include <iterator>
#include <unordered_map>

#include "common/init_flags.h"
#include "os/log.h"
#include "osi/include/allocator.h"
#include "stack/include/btu.h"  // do_in_main_thread
#include "stack/include/l2c_api.h"

namespace bluetooth {
namespace shim {
namespace arbiter {

class PassthroughAclArbiter : public AclArbiter {
 public:
  virtual void OnLeConnect(const RawAddress& address,
                           uint16_t handle) override {
    // no-op
  }

  virtual void OnLeDisconnect(const RawAddress& address) override {
    // no-op
  }

  virtual InterceptAction InterceptAttPacket(const RawAddress& address,
                                             const BT_HDR* packet) override {
    return InterceptAction::FORWARD;
  }

  static PassthroughAclArbiter& Get() {
    static auto singleton = PassthroughAclArbiter();
    return singleton;
  }
};

AclArbiter& GetArbiter() {
  return static_cast<AclArbiter&>(PassthroughAclArbiter::Get());
}

}  // namespace arbiter
}  // namespace shim
}  // namespace bluetooth
