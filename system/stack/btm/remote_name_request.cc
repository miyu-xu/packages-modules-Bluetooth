#include "remote_name_request.h"

#include <base/bind.h>

#include "osi/include/log.h"
#include "stack/btm/neighbor_inquiry.h"
#include "stack/include/btu.h"
#include "stack/include/gap_api.h"
#include "stack/include/hcimsgs.h"

extern tBTM_STATUS btm_ble_read_remote_name(const RawAddress& remote_bda);

namespace bluetooth {
namespace inquiry {

const auto BTM_EXT_RMT_NAME_TIMEOUT_MS = base::TimeDelta::FromSeconds(40);

// The below functions manage the active request on the correct transport
namespace {
tBTM_STATUS startRequest(const RawAddress& address, Transport transport) {
  if (transport == BT_TRANSPORT_LE) {
    return btm_ble_read_remote_name(address);
  } else {
    auto entry = BTM_InqDbRead(address);
    if (entry && (entry->results.inq_result_type & BTM_INQ_RESULT_BR)) {
      btsnd_hcic_rmt_name_req(
          address, entry->results.page_scan_rep_mode,
          entry->results.page_scan_mode,
          (uint16_t)(entry->results.clock_offset | BTM_CLOCK_OFFSET_VALID));
    } else {
      btsnd_hcic_rmt_name_req(address, HCI_PAGE_SCAN_REP_MODE_R1,
                              HCI_MANDATARY_PAGE_SCAN_MODE, 0);
    }
    return BTM_CMD_STARTED;
  }
}

void cancelRequest(const RawAddress& address, Transport transport) {
  if (transport == BT_TRANSPORT_LE) {
    GAP_BleCancelReadPeerDevName(address);
  } else {
    btsnd_hcic_rmt_name_req_cancel(address);
  }
}
}  // namespace

RemoteNameRequestCallbacks RemoteNameRequestCallbacks::forName(
    void (*nameCallback)(const RemoteNameRequestResult&)) {
  RemoteNameRequestCallbacks out;
  out.featuresCallback = nullptr;
  out.nameCallback = nameCallback;
  return out;
}

RemoteNameRequestCallbacks RemoteNameRequestCallbacks::forFeaturesAndName(
    void (*featuresCallback)(const RemoteHostSupportedFeaturesResult&),
    void (*nameCallback)(const RemoteNameRequestResult&)) {
  RemoteNameRequestCallbacks out;
  out.featuresCallback = featuresCallback;
  out.nameCallback = nameCallback;
  return out;
}

bool RemoteNameRequestCallbacks::needsFeatures() {
  return featuresCallback != nullptr;
}

void RemoteNameRequestCallbacks::invokeWithFeatures(
    const RemoteHostSupportedFeaturesResult& result) {
  if (needsFeatures()) {
    do_in_main_thread(FROM_HERE, base::Bind(featuresCallback, result));
  }
}

void RemoteNameRequestCallbacks::invokeWithName(
    const RemoteNameRequestResult& result) {
  do_in_main_thread(FROM_HERE, base::Bind(nameCallback, result));
}

tBTM_STATUS RemoteNameRequestScheduler::InitiateRemoteNameRequest(
    const RawAddress& address, RemoteNameRequestCallbacks callbacks,
    Transport transport, PendingRemoteNameRequestHandle* handle) {
  *handle = PendingRemoteNameRequestHandle::newHandle();

  // enqueue operation for later, return success for now
  // any errors later on will be returned through the callback
  bool added = false;
  for (auto& request : pendingRequestQueue) {
    if (request.address == address && request.transport == transport &&
        (!callbacks.needsFeatures() || !request.featuresArrived)) {
      added = true;
      request.callbacks.push_back({*handle, callbacks});
    }
  }

  if (!added) {
    pendingRequestQueue.push_back({address,
                                   transport,
                                   /* featuresArrived = */ false,
                                   {{*handle, callbacks}}});
  }

  if (!isActive) {
    isActive = true;
    dequeueNext(/* synchronous = */ true);
  }

  return BTM_CMD_STARTED;
}

bool RemoteNameRequestScheduler::CancelRemoteNameRequest(
    const PendingRemoteNameRequestHandle& handle) {
  // if the request is queued, just dequeue it and synchronously report to the
  // callback
  for (auto request = pendingRequestQueue.begin();
       request != pendingRequestQueue.end(); ++request) {
    auto failedResult = RemoteNameRequestResult::newFailureWithStatus(
        request->address, BTM_ERR_PROCESSING);
    for (auto callback = request->callbacks.begin();
         callback != request->callbacks.end(); ++callback) {
      if (callback->handle != handle) {
        continue;
      }
      callback->callbacks.invokeWithName(failedResult);
      request->callbacks.erase(callback);
      if (request->callbacks.empty()) {
        pendingRequestQueue.erase(request);
      }
      return true;
    }
  }

  /* Make sure there is already one in progress */
  if (!isActive) {
    return false;
  }

  // If the request is live, report synchronously to the callback
  auto failedResult = RemoteNameRequestResult::newFailureWithStatus(
      activeRequest.address, BTM_ERR_PROCESSING);
  for (auto callback = activeRequest.callbacks.begin();
       callback != activeRequest.callbacks.end(); ++callback) {
    if (callback->handle != handle) {
      continue;
    }
    callback->callbacks.invokeWithName(failedResult);
    activeRequest.callbacks.erase(callback);
    cancelRequest(activeRequest.address, activeRequest.transport);
    if (activeRequest.transport == BT_TRANSPORT_LE) {
      // For LE connections, we don't get a callback after cancellation
      // So to keep the scheduler moving, we can immediately move to the next
      // request.
      // Note that there exists a scary race condition if the LE
      // response arrives before LE cancellation completes. In that case,
      // we will just ignore the response and continue, if it does not
      // match what we are expecting to receive. If it does, great! We got the
      // reply faster than expected and can again continue.
      timeoutAction.Cancel();
      dequeueNext(false /* synchronous */);
    }
    return BTM_SUCCESS;
  }

  // handle not found anywhere
  return BTM_ILLEGAL_VALUE;
}

void RemoteNameRequestScheduler::Stop() {
  for (auto& request : pendingRequestQueue) {
    auto result = RemoteNameRequestResult::newFailureWithStatus(request.address,
                                                                BTM_DEV_RESET);
    for (auto& callback : request.callbacks) {
      callback.callbacks.invokeWithName(result);
    }
  }

  if (isActive) {
    timeoutAction.Cancel();
    auto result = RemoteNameRequestResult::newFailureWithStatus(
        activeRequest.address, BTM_DEV_RESET);
    for (auto& callback : activeRequest.callbacks) {
      callback.callbacks.invokeWithName(result);
    }
  }

  pendingRequestQueue = {};
  activeRequest = {};
  isActive = false;
}

void RemoteNameRequestScheduler::ReportRemoteHostSupportedFeaturesResult(
    RemoteHostSupportedFeaturesResult result) {
  if (!isActive) {
    LOG_ERROR(
        "Got unexpected host supported features response - "
        "RemoteNameRequestScheduler is "
        "inactive. Ignoring it.");
    return;
  }
  if (result.bd_addr != activeRequest.address) {
    LOG_ERROR(
        "Got unexpected host supported features response - inconsistent with "
        "stored "
        "data (expected result from %s, got result from %s). Ignoring it.",
        activeRequest.address.ToString().c_str(),
        result.bd_addr.ToString().c_str());
    return;
  }

  activeRequest.featuresArrived = true;
  for (auto& callback : activeRequest.callbacks) {
    callback.callbacks.invokeWithFeatures(result);
  }
}

void RemoteNameRequestScheduler::ReportRemoteNameRequestResult(
    RemoteNameRequestResult result) {
  if (!isActive) {
    LOG_ERROR(
        "Got unexpected remote name response - RemoteNameRequestScheduler is "
        "inactive. Ignoring it.");
    return;
  }
  if (result.bd_addr != activeRequest.address) {
    LOG_ERROR(
        "Got unexpected remote name response - inconsistent with stored "
        "data (expected result from %s, got result from %s). Ignoring it.",
        activeRequest.address.ToString().c_str(),
        result.bd_addr.ToString().c_str());
    return;
  }

  timeoutAction.Cancel();

  for (auto& callback : activeRequest.callbacks) {
    callback.callbacks.invokeWithName(result);
  }

  dequeueNext(false /* synchronous */);
}

bool RemoteNameRequestScheduler::dequeueNext(bool synchronous) {
  if (!isActive) {
    LOG_ERROR("Dequeuing next remote name request but none currently active");
  }

  while (!pendingRequestQueue.empty()) {
    auto nextRequest = pendingRequestQueue.front();
    pendingRequestQueue.pop_front();

    auto status = startRequest(nextRequest.address, nextRequest.transport);

    if (status == BTM_CMD_STARTED) {
      timeoutAction.Reset(
          base::Bind(&RemoteNameRequestScheduler::ReportRemoteNameRequestResult,
                     base::Unretained(this),
                     RemoteNameRequestResult::newFailureWithStatus(
                         nextRequest.address, BTM_BAD_VALUE_RET)));
      do_in_main_thread_delayed(FROM_HERE, timeoutAction.callback(),
                                BTM_EXT_RMT_NAME_TIMEOUT_MS);
      activeRequest = nextRequest;
      return status;
    }

    // something failed, report to callbacks
    if (synchronous) {
      if (pendingRequestQueue.empty() && nextRequest.callbacks.size() == 1) {
        // We should only be in synchronous mode if the queue is empty, so
        // only a single callback is present, the one that was just enqueued.
        // Thus, it is safe to just directly return the status and pass it to
        // the caller, rather than going through the callbacks.
        isActive = false;
        return status;
      } else {
        LOG_ERROR(
            "Cannot dequeue synchronously because multiple requests or "
            "callbacks are "
            "present");
      }
    } else {
      for (auto& callback : nextRequest.callbacks) {
        RemoteNameRequestResult rem_name =
            RemoteNameRequestResult::newFailureWithStatus(nextRequest.address,
                                                          status);
        callback.callbacks.invokeWithName(rem_name);
      }
    }
  }
  isActive = false;
  return BTM_UNDEFINED;
};

}  // namespace inquiry
}  // namespace bluetooth