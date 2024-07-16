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

#include <frameworks/proto_logging/stats/enums/bluetooth/enums.pb.h>

#include "bluetooth_event.h"
#include "os/log.h"
#include "os/metrics.h"

namespace bluetooth {
namespace metrics {

using android::bluetooth::EventType;
using android::bluetooth::State;
using hci::ErrorCode;

State mapHciReasonToEnum(ErrorCode reason) {
  // TODO - add a manual mapping from each status code to State
  switch (reason) {
    case ErrorCode::SUCCESS:
      return State::SUCCESS;
    case ErrorCode::CONNECTION_ALREADY_EXISTS:
      return State::ALREADY_CONNECTED;
    case ErrorCode::CONNECTION_TIMEOUT:
    case ErrorCode::CONNECTION_ACCEPT_TIMEOUT:
    case ErrorCode::PAGE_TIMEOUT:
      return State::TIMEOUT;
    case ErrorCode::REMOTE_USER_TERMINATED_CONNECTION:
    case ErrorCode::REMOTE_DEVICE_TERMINATED_CONNECTION_LOW_RESOURCES:
    case ErrorCode::REMOTE_DEVICE_TERMINATED_CONNECTION_POWER_OFF:
      return State::REMOTE_USER_TERMINATED_CONNECTION;
    case ErrorCode::MEMORY_CAPACITY_EXCEEDED:
      return State::MEMORY_EXCEEDED;
    default:
      return State::STATE_UNKNOWN;
  }
}

void LogAclCompletionEvent(const hci::Address& address, ErrorCode reason,
                           bool is_locally_initiated) {
  if (is_locally_initiated) {
    bluetooth::os::LogMetricBluetoothEvent(address,
                                           EventType::ACL_CONNECTION_INITIATOR,
                                           mapHciReasonToEnum(reason));
  } else {
    bluetooth::os::LogMetricBluetoothEvent(address,
                                           EventType::ACL_CONNECTION_RESPONDER,
                                           mapHciReasonToEnum(reason));
  }
}

}  // namespace metrics
}  // namespace bluetooth
