#include "remote_name_request.h"

#include "osi/include/log.h"

namespace bluetooth {
namespace inquiry {

const auto BTM_EXT_RMT_NAME_TIMEOUT_MS = 40 * 1000;  // 40 seconds

tBTM_STATUS RemoteNameRequestScheduler::InitiateRemoteNameRequest(
    const RawAddress& address, RemoteNameRequestCallback callback,
    Transport transport, PendingRemoteNameRequestHandle* handle) {
  *handle = PendingRemoteNameRequestHandle::newHandle();

  // enqueue operation for later, return success for now
  // any errors later on will be returned through the callback
  bool added = false;
  for (auto& request : pendingRequestQueue) {
    if (request.remote_addr == remote_bda) {
      added = true;
      request.callbacks.push_back({*handle, p_cb});
    }
  }

  if (!added) {
    pendingRequestQueue.push_back({remote_bda, {{*handle, p_cb}}, transport});
  }

  if (!isActive) {
    isActive = true;
    btm_inq_rmt_name_dequeue(/* synchronous = */ true);
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
      if (callback->handle == handle) {
        callback->callback(failedResult);
        request->callbacks.erase(callback);
        if (request->callbacks.empty()) {
          queue.erase(request);
        }
        return true;
      }
    }
  }

  /* Make sure there is already one in progress */
  if (!p_inq->remname_active) {
    return false;
  }

  // If the request is live, report synchronously to the callback
  auto failedResult = RemoteNameRequestResult::newFailureWithStatus(
      request->address, BTM_ERR_PROCESSING);
  for (auto callback = activeRequest.callbacks.begin();
       callback != activeRequest.callbacks.end(); ++callback) {
    if (callback->handle == handle) {
      callback->callback(&failedResult);
      activeRequest.callbacks.erase(callback);
      // note that we intentionally keep the request active until we get
      // confirmation that cancellation has completed
      cancelRequest(activeRequest.address, activeRequest.transport);
      return BTM_SUCCESS;
    }
  }

  // handle not found anywhere
  return BTM_ILLEGAL_VALUE;
}

void RemoteNameRequestScheduler::Stop() {
  for (auto& request : pendingRequestQueue) {
    auto result = RemoteNameRequestResult::newFailureWithStatus(request.address,
                                                                BTM_DEV_RESET);
    for (auto& callback : request.callbacks) {
      callback.callback(result);
    }
  }

  if (isActive) {
    alarm_cancel(timeoutAlarm);
    auto result = RemoteNameRequestResult::newFailureWithStatus(
        activeRequest.address, BTM_DEV_RESET);
    for (auto& callback : activeRequest.callbacks) {
      callback.callback(result);
    }
  }

  pendingRequestQueue = {};
  activeRequest = {};
  isActive = false;
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
        activeRequest.address, result.bd_addr);
    return;
  }

  alarm_cancel(timeoutAlarm);

  if (result.hci_status == HCI_ERR_UNSPECIFIED) {
    completeRequestOnFailure(activeRequest.address, activeRequest.transport);
  }

  for (auto& callback : activeRequest.callbacks) {
    callback.callback(result);
  }

  dequeueNext(false);
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
      alarm_set_on_mloop(timeoutAlarm, BTM_EXT_RMT_NAME_TIMEOUT_MS,
                         Bind(ReportRemoteNameRequestResult, this,
                              RemoteNameRequestResult::newFailureWithStatus(
                                  nextRequest.address, BTM_BAD_VALUE_RET)),
                         NULL);
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
            RemoteNameRequestResult::newFailureWithStatus(next_request.address,
                                                          status);
        callback.callback(&rem_name);
      }
    }
  }
  isActive = false;
  return BTM_UNDEFINED;
};

// The below functions manage the active request on the correct transport
tBTM_STATUS RemoteNameRequestScheduler::startRequest(const RawAddress& address,
                                                     Transport transport) {
  if (transport == BT_TRANSPORT_LE) {
    return btm_ble_read_remote_name(nextRequest.address);
  } else {
    return btm_ble_read_remote_name(nextRequest.address);
  }
}

tBTM_STATUS RemoteNameRequestScheduler::cancelRequest(const RawAddress& address,
                                                      Transport transport) {
  if (transport == BT_TRANSPORT_LE) {
    return btm_inq_rmt_name_failed_cancelled(nextRequest.address);
  } else {
    return btsnd_hcic_rmt_name_req_cancel(nextRequest.address);
  }
}

void RemoteNameRequestScheduler::completeRequestOnFailure(
    const RawAddress& address, Transport transport) {
  if (transport == BT_TRANSPORT_LE) {
    // For GATT-based remote-name lookup, we need to explicitly cancel the
    // connection on failure
    return GAP_BleCancelReadPeerDevName(nextRequest.address);
  } else {
    // Classic remote-name lookups complete regardless of success/failure
  }
}

}  // namespace inquiry
}  // namespace bluetooth