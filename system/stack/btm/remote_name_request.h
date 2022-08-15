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

  bool operator!=(PendingRemoteNameRequestHandle other) {
    return !(*this == other);
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

class RemoteNameRequestCallbacks {
 public:
  static RemoteNameRequestCallbacks forName(
      void (*nameCallback)(const RemoteNameRequestResult&));

  static RemoteNameRequestCallbacks forFeaturesAndName(
      void (*featuresCallback)(const RemoteHostSupportedFeaturesResult&),
      void (*nameCallback)(const RemoteNameRequestResult&));

  bool needsFeatures();

  void invokeWithFeatures(const RemoteHostSupportedFeaturesResult&);
  void invokeWithName(const RemoteNameRequestResult&);

 private:
  void (*featuresCallback)(const RemoteHostSupportedFeaturesResult&);
  void (*nameCallback)(const RemoteNameRequestResult&);
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

class RemoteNameRequestController {
 public:
  RemoteNameRequestController() = default;
  RemoteNameRequestController& operator=(const RemoteNameRequestController&) =
      delete;
  virtual ~RemoteNameRequestController() = default;

  virtual tBTM_STATUS startRequest(const RawAddress& address,
                                   Transport transport);

  virtual void cancelRequest(const RawAddress& address, Transport transport);
};

class RemoteNameRequestScheduler {
 public:
  RemoteNameRequestScheduler()
      : controller(std::make_unique<RemoteNameRequestController>()){};
  RemoteNameRequestScheduler(
      std::unique_ptr<RemoteNameRequestController> controller)
      : controller(std::move(controller)){};

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
  std::unique_ptr<RemoteNameRequestController> controller;
  bool isActive = false;
  PendingRemoteNameRequest activeRequest;
  std::deque<PendingRemoteNameRequest> pendingRequestQueue = {};
  base::CancelableClosure timeoutAction;

  bool dequeueNext(bool synchronous);
};

}  // namespace inquiry
}  // namespace bluetooth