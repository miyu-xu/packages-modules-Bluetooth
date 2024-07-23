<<<<<<< PATCH SET (7f079b BluetoothMetrics: Log Remote Name Request Completion)
/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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

#include "hci/address.h"
#include "hci/hci_packets.h"
#include "stack/include/btm_status.h"
#include "types/raw_address.h"

namespace bluetooth {
namespace metrics {

void LogAclCompletionEvent(const hci::Address& address, hci::ErrorCode reason,
                           bool is_locally_initiated);

void LogRemoteNameRequestCompletion(const RawAddress& address);

void LogAclAfterRemoteNameRequest(const RawAddress& raw_address, tBTM_STATUS status);

}  // namespace metrics
}  // namespace bluetooth
=======
>>>>>>> BASE      (b9e1f3 Merge changes Ibe26844b,Ib3cbe9ef,Id0935c0f,I828a96d1,I73d16)
