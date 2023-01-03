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

#include <optional>
#include <tuple>
#include <unordered_set>
#include <variant>

#include "common/callback.h"
#include "packet/packet_view.h"
#include "packet/raw_builder.h"

namespace bluetooth {
namespace arbiter {

// The UnconditionalConnectionFilter matches ALL connections.
struct UnconditionalConnectionFilter {};

// The AdvertisingSetConnectionFilter matches all connections to the specified advertising_set_id
// (i.e. we are a peripheral).
struct AdvertisingSetConnectionFilter {
  uint8_t advertising_set_id;
};

using ConnectionFilter =
    std::variant<UnconditionalConnectionFilter, AdvertisingSetConnectionFilter>;

// The UnconditionalPacketFilter matches ALL packets.
struct UnconditionalPacketFilter {};

// This PacketFilter invokes callback() on each packet. If it returns true, the packet is MATCHED.
struct ExclusiveCallbackPacketFilter {
  base::Callback<bool(packet::PacketView<packet::kLittleEndian>*)> callback;
};

using PacketFilter = std::variant<UnconditionalPacketFilter, ExclusiveCallbackPacketFilter>;

}  // namespace arbiter
}  // namespace bluetooth