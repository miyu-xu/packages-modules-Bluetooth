/*
 * Copyright 2024 The Android Open Source Project
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
#define LOG_TAG "bta_rfcomm_metrics"

#include "../include/bta_rfcomm_metrics.h"

#include <bluetooth/log.h>
#include <frameworks/proto_logging/stats/enums/bluetooth/rfcomm/enums.pb.h>

#include "bta/include/bta_jv_api.h"
#include "os/metrics.h"
#include "stack/btm/security_device_record.h"
#include "stack/include/btm_sec_api_types.h"

using namespace bluetooth;

using namespace android::bluetooth;
using namespace android::bluetooth::rfcomm;

static BtaStatus toStatus(tBTA_JV_STATUS status);
static SocketConnectionSecurity toSecurity(int security);

// logged if SDP result is either FAILED or BUSY
void bta_collect_attempt_metrics_after_sdp(tBTA_JV_STATUS sdp_status, RawAddress addr, int app_uid,
                                           int security) {
  // If we are requesting SDP, initiated_as_client is true
  bool initiated_as_client = true;

  // We didn't make it to the stage of making a port, so assign default values for these fields
  PortResult close_reason = PortResult::PORT_RESULT_UNDEFINED;
  RfcommPortState state_prior = RfcommPortState::PORT_STATE_UNKNOWN;
  RfcommPortEvent last_event = RfcommPortEvent::PORT_EVENT_UNKNOWN;
  int open_duration_ms = 0;

  os::LogMetricRfcommConnectionAtClose(addr, close_reason, toSecurity(security), last_event,
                                       state_prior, open_duration_ms, app_uid, toStatus(sdp_status),
                                       initiated_as_client);
}

static BtaStatus toStatus(tBTA_JV_STATUS status) {
  switch (status) {
    case tBTA_JV_STATUS::SUCCESS:
      return BtaStatus::BTA_STATUS_SUCCESS;
    case tBTA_JV_STATUS::FAILURE:
      return BtaStatus::BTA_STATUS_FAILURE;
    case tBTA_JV_STATUS::BUSY:
      return BtaStatus::BTA_STATUS_BUSY;
  }
  return BtaStatus::BTA_STATUS_UNKNOWN;
}

static SocketConnectionSecurity toSecurity(int security) {
  if (((security & BTM_SEC_IN_FLAGS) == (BTM_SEC_IN_AUTHENTICATE | BTM_SEC_IN_ENCRYPT)) ||
      ((security & BTM_SEC_OUT_FLAGS) == (BTM_SEC_OUT_AUTHENTICATE | BTM_SEC_OUT_ENCRYPT))) {
    return SocketConnectionSecurity::SOCKET_SECURITY_SECURE;
  } else if (((security & BTM_SEC_IN_FLAGS) == (BTM_SEC_NONE)) ||
             ((security & BTM_SEC_OUT_FLAGS) == (BTM_SEC_NONE))) {
    return SocketConnectionSecurity::SOCKET_SECURITY_INSECURE;
  }
  return SocketConnectionSecurity::SOCKET_SECURITY_UNKNOWN;
}
