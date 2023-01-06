// Copyright 2022, The Android Open Source Project
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

#include <base/bind.h>
#include <base/location.h>

#include <cstdint>

#include "include/hardware/bluetooth.h"
#include "include/hardware/bt_common_types.h"
#include "include/hardware/bt_gatt_client.h"
#include "include/hardware/bt_gatt_server.h"
#include "types/bluetooth/uuid.h"

extern bt_status_t do_in_jni_thread(const base::Location& from_here,
                                    base::OnceClosure task);

namespace bluetooth {
namespace gatt {

class GattServerCallbacks {
 public:
  GattServerCallbacks(const btgatt_server_callbacks_t& callbacks)
      : callbacks(callbacks){};

  void OnRegisterServer(int32_t status, int32_t server_if,
                        const Uuid& uuid) const {
    do_in_jni_thread(FROM_HERE, base::Bind(callbacks.register_server_cb, status,
                                           server_if, uuid));
  }

 private:
  const btgatt_server_callbacks_t& callbacks;
};

}  // namespace gatt
}  // namespace bluetooth