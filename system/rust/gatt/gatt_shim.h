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
#include "osi/include/allocator.h"
#include "rust/cxx.h"
#include "types/bluetooth/uuid.h"
#include "types/raw_address.h"

extern bt_status_t do_in_jni_thread(const base::Location& from_here,
                                    base::OnceClosure task);

namespace bluetooth {
namespace gatt {

class GattServerCallbacks {
 public:
  GattServerCallbacks(const btgatt_server_callbacks_t& callbacks)
      : callbacks(callbacks){};

  void OnServerReadCharacteristic(uint16_t conn_id, uint32_t trans_id,
                                  ::rust::Slice<const uint8_t> bda,
                                  uint16_t attr_handle, uint32_t offset,
                                  bool is_long) const {
    RawAddress rawAddress;
    rawAddress.FromOctets(bda.data());
    do_in_jni_thread(
        FROM_HERE,
        base::Bind(callbacks.request_read_characteristic_cb, conn_id, trans_id,
                   rawAddress, attr_handle, offset, is_long));
  }

  void OnServerWriteCharacteristic(uint16_t conn_id, uint32_t trans_id,
                                   ::rust::Slice<const uint8_t> bda,
                                   uint16_t attr_handle, uint32_t offset,
                                   bool need_response, bool is_prepare,
                                   ::rust::Slice<const uint8_t> value,
                                   size_t length) const {
    RawAddress rawAddress;
    rawAddress.FromOctets(bda.data());

    auto buf = (uint8_t*)osi_malloc(value.size());
    std::copy(value.begin(), value.end(), buf);

    do_in_jni_thread(
        FROM_HERE,
        base::Bind(callbacks.request_write_characteristic_cb, conn_id, trans_id,
                   rawAddress, attr_handle, offset, need_response, is_prepare,
                   base::Owned(buf), length));
  }

 private:
  const btgatt_server_callbacks_t& callbacks;
};

}  // namespace gatt
}  // namespace bluetooth