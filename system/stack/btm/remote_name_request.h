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

#include <base/cancelable_callback.h>

#include "deque"
#include "stack/btm/security_device_record.h"
#include "stack/include/bt_name.h"
#include "stack/include/btm_status.h"
#include "stack/include/hci_error_code.h"
#include "types/bt_transport.h"
#include "types/raw_address.h"
#include "vector"

namespace bluetooth {
namespace inquiry {

using Transport = tBT_TRANSPORT;

struct RemoteHostSupportedFeaturesResult {
  RawAddress bd_addr;
  const uint8_t* p;  // the raw packet, which client code will parse
};

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
    return {status, HCI_ERR_UNSPECIFIED, address, 0, {}};
  }
};

class PendingRemoteNameRequestHandle {
 public:
  bool operator==(PendingRemoteNameRequestHandle other) {
    return handle == other.handle;
  }

  static PendingRemoteNameRequestHandle newHandle() {
    static uint16_t nextHandle = 0xabcd;
    if (nextHandle == -1) {
      ++nextHandle;
    }
    return {nextHandle++};
  }

  // default constructor exists for compatibility but produces an invalid
  // handle
  PendingRemoteNameRequestHandle() : handle(-1){};

 private:
  uint16_t handle;
  PendingRemoteNameRequestHandle(uint16_t handle) : handle(handle) {}
};

struct RemoteNameRequestCallbacks {
  void (*featuresCallback)(const RemoteHostSupportedFeaturesResult&);
  void (*nameCallback)(const RemoteNameRequestResult&);

  static RemoteNameRequestCallbacks forName(
      void (*nameCallback)(const RemoteNameRequestResult&)) {
    RemoteNameRequestCallbacks out;
    out.featuresCallback = nullptr;
    out.nameCallback = nameCallback;
    return out;
  }

  static RemoteNameRequestCallbacks forFeaturesAndName(
      void (*featuresCallback)(const RemoteHostSupportedFeaturesResult&),
      void (*nameCallback)(const RemoteNameRequestResult&)) {
    RemoteNameRequestCallbacks out;
    out.featuresCallback = featuresCallback;
    out.nameCallback = nameCallback;
    return out;
  }
};

struct RemoteNameRequestCallbacksWithHandle {
  PendingRemoteNameRequestHandle handle;
  RemoteNameRequestCallbacks callbacks;
};

struct PendingRemoteNameRequest {
  RawAddress address;
  Transport transport;
  bool featuresArrived;
  std::vector<RemoteNameRequestCallbacksWithHandle> callbacks;
};

class RemoteNameRequestScheduler {
 public:
  RemoteNameRequestScheduler() = default;
  RemoteNameRequestScheduler(const RemoteNameRequestScheduler&) = delete;
  RemoteNameRequestScheduler& operator=(const RemoteNameRequestScheduler&) =
      delete;
  ~RemoteNameRequestScheduler() = default;

  /* These functions represent the public API to be used by callers */
  tBTM_STATUS InitiateRemoteNameRequest(const RawAddress& address,
                                        RemoteNameRequestCallbacks callback,
                                        Transport transport,
                                        PendingRemoteNameRequestHandle* handle);

  bool CancelRemoteNameRequest(const PendingRemoteNameRequestHandle& handle);

  /* These functions are used to manage the lifecycle of the scheduler */
  void Stop();

  /* These functions should only be called by the HCI layer to report the
   * success/failure of events */
  void ReportRemoteHostSupportedFeaturesResult(
      RemoteHostSupportedFeaturesResult result);
  void ReportRemoteNameRequestResult(RemoteNameRequestResult result);

 private:
  bool isActive = false;
  PendingRemoteNameRequest activeRequest;
  std::deque<PendingRemoteNameRequest> pendingRequestQueue = {};
  base::CancelableClosure timeoutAction;

  bool dequeueNext(bool synchronous);
};

}  // namespace inquiry
}  // namespace bluetooth