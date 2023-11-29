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

#include "common/callback.h"
#include "hci/command_interface.h"
#include "hci/hci_packets.h"
#include "os/utils.h"

namespace bluetooth {
namespace hci {

constexpr EventCode ScoEvents[] = {
    EventCode::SYNCHRONOUS_CONNECTION_COMPLETE,
    EventCode::SYNCHRONOUS_CONNECTION_CHANGED,
};

typedef CommandInterface<CommandBuilder> ScoInterface;

}  // namespace hci
}  // namespace bluetooth
