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
#include "bluetooth_event.h"

namespace bluetooth {
namespace metrics {

using android::bluetooth::EventType;
using android::bluetooth::State;

android::bluetooth::State mapHciReasonToEnum(hci::ErrorCode reason) {
  // TODO - add a manual mapping from each status code to State
  switch (reason) {
    case hci::ErrorCode::SUCCESS:
      return android::bluetooth::State::SUCCESS;
    case hci::ErrorCode::CONNECTION_ALREADY_EXISTS:
      return android::bluetooth::State::ALREADY_CONNECTED;
    case hci::ErrorCode::CONNECTION_TIMEOUT:
    case hci::ErrorCode::CONNECTION_ACCEPT_TIMEOUT:
    case hci::ErrorCode::PAGE_TIMEOUT:
      return android::bluetooth::State::TIMEOUT;
    case hci::ErrorCode::REMOTE_USER_TERMINATED_CONNECTION:
    case hci::ErrorCode::REMOTE_DEVICE_TERMINATED_CONNECTION_LOW_RESOURCES:
    case hci::ErrorCode::REMOTE_DEVICE_TERMINATED_CONNECTION_POWER_OFF:
      return android::bluetooth::State::REMOTE_USER_TERMINATED_CONNECTION;
    case hci::ErrorCode::MEMORY_CAPACITY_EXCEEDED:
      return android::bluetooth::State::MEMORY_EXCEEDED;
    default:
      return android::bluetooth::State::STATE_UNKNOWN;
  }
}

void LogAclCompletionEvent(const Address& address, hci::ErrorCode reason,
                           bool is_locally_initiated) {
  if (is_locally_initiated) {
    bluetooth::os::LogMetricBluetoothEvent(address,
                                           android::bluetooth::EventType::ACL_CONNECTION_INITIATOR,
                                           mapHciReasonToEnum(reason));
  } else {
    bluetooth::os::LogMetricBluetoothEvent(address,
                                           android::bluetooth::EventType::ACL_CONNECTION_RESPONDER,
                                           mapHciReasonToEnum(reason));
  }
}

}  // namespace metrics
}  // namespace bluetooth
