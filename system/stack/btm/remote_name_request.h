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

#include "deque"
#include "osi/include/alarm.h"
#include "stack/include/bt_name.h"
#include "stack/include/btm_status.h"
#include "stack/include/hci_error_code.h"
#include "types/bt_transport.h"
#include "types/raw_address.h"
#include "vector"

namespace bluetooth {
namespace inquiry {

using Transport = tBT_TRANSPORT;

/* Structure returned with remote name request, used for interop with legacy
 * code */
struct RemoteNameRequestResult {
  tBTM_STATUS status;
  tHCI_STATUS hci_status;
  RawAddress bd_addr;
  uint16_t length;
  BD_NAME remote_bd_name;

  static RemoteNameRequestResult newFailureWithStatus(const RawAddress& address,
                                                      tBTM_STATUS status) {
    return {address, HCI_ERR_UNSPECIFIED, address, 0};
  }
};

class PendingRemoteNameRequestHandle {
 public:
  bool operator==(PendingRemoteNameRequestHandle other) {
    return handle == other.handle;
  }

  static PendingRemoteNameRequestHandle newHandle();

  // default constructor exists for compatibility but produces an invalid handle
  PendingRemoteNameRequestHandle() : handle(-1) {}

 private:
  uint16_t handle;
  PendingRemoteNameRequestHandle(uint16_t handle) : handle(handle) {}
}

using RemoteNameRequestCallback = void(const RemoteNameRequestResult&);

struct RemoteNameRequestCallbackWithHandle {
  PendingRemoteNameRequestHandle handle;
  RemoteNameRequestCallback callback;
}

struct PendingRemoteNameRequest {
  RawAddress address;
  Transport transport;
  std::vector<RemoteNameRequestCallbackWithHandle> callbacks;
}

class RemoteNameRequestScheduler {
 public:
  RemoteNameRequestScheduler() : activeRequest {
    timeoutAlarm = alarm_new("RemoteNameRequestScheduler.timeoutAlarm");
  }
  RemoteNameRequestScheduler(const RemoteNameRequestScheduler&) = delete;
  RemoteNameRequestScheduler& operator=(const RemoteNameRequestScheduler&) =
      delete;
  ~RemoteNameRequestScheduler() {
    if (timeoutAlarm != NULL) {
      alarm_free(timeoutAlarm);
    }
  }

  /* These functions represent the public API to be used by callers */
  tBTM_STATUS InitiateRemoteNameRequest(const RawAddress& address,
                                        RemoteNameRequestCallback callback,
                                        Transport transport,
                                        PendingRemoteNameRequestHandle* handle);

  bool CancelRemoteNameRequest(PendingRemoteNameRequestHandle handle);

  /* These functions are used to manage the lifecycle of the scheduler */
  void Stop();

  /* These functions should only be called by the HCI layer to report the
   * success/failure of events */
  void ReportRemoteNameRequestResult();
  void ReportRemoteNameRequestCancelled();

 private:
  bool isActive = false;
  PendingRemoteNameRequest activeRequest;
  std::deque<PendingRemoteNameRequest> pendingRequestQueue = {};
  alarm_t timeoutAlarm;

  bool dequeueNext(bool synchronous);
};

}  // namespace inquiry
}  // namespace bluetooth