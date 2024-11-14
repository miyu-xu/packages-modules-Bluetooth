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
#define LOG_TAG "rfc_metrics"

#include "../include/rfc_metrics.h"

#include <bluetooth/log.h>
#include <frameworks/proto_logging/stats/enums/bluetooth/rfcomm/enums.pb.h>

#include "common/time_util.h"
#include "os/metrics.h"
#include "stack/include/port_api.h"
#include "stack/rfcomm/port_int.h"
#include "stack/rfcomm/rfc_event.h"
#include "stack/rfcomm/rfc_state.h"
#include "types/raw_address.h"

using namespace bluetooth;

static android::bluetooth::rfcomm::PortResult toPortResult(tPORT_RESULT result);
static android::bluetooth::rfcomm::RfcommPortState toPortState(tRFC_PORT_STATE state);

void port_collect_attempt_metrics(RfcommPortSm sm_cb, uint32_t uid) {
  log::assert_that(sm_cb.state == RFC_STATE_CLOSED, "Assert failed: Port not closed");
  uint64_t close_timestamp = bluetooth::common::time_gettimeofday_us();
  bool success = (sm_cb.second_state_prior == RFC_STATE_OPENED &&
                  sm_cb.state_prior == RFC_STATE_DISC_WAIT_UA);
  uint64_t open_duration = (close_timestamp - sm_cb.open_timestamp) / 1000;  // to milliseconds
  if (open_duration < 0) {
    open_duration = 0;
  }
  bluetooth::os::LogMetricRfcommConnectionAtClose(
          toPortResult(sm_cb.close_reason),
          android::bluetooth::rfcomm::SocketConnectionSecurity::SOCKET_SECURITY_UNKNOWN,
          toPortState(sm_cb.second_state_prior), toPortState(sm_cb.state_prior),
          static_cast<int32_t>(open_duration), static_cast<int32_t>(uid));
}

static android::bluetooth::rfcomm::PortResult toPortResult(tPORT_RESULT result) {
  switch (result) {
    case PORT_SUCCESS:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_SUCCESS;
    case PORT_UNKNOWN_ERROR:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_UNKNOWN_ERROR;
    case PORT_ALREADY_OPENED:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_ALREADY_OPENED;
    case PORT_CMD_PENDING:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_CMD_PENDING;
    case PORT_APP_NOT_REGISTERED:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_APP_NOT_REGISTERED;
    case PORT_NO_MEM:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_NO_MEM;
    case PORT_NO_RESOURCES:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_NO_RESOURCES;
    case PORT_BAD_BD_ADDR:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_BAD_BD_ADDR;
    case PORT_BAD_HANDLE:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_BAD_HANDLE;
    case PORT_NOT_OPENED:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_NOT_OPENED;
    case PORT_LINE_ERR:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_LINE_ERR;
    case PORT_START_FAILED:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_START_FAILED;
    case PORT_PAR_NEG_FAILED:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_PAR_NEG_FAILED;
    case PORT_PORT_NEG_FAILED:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_PORT_NEG_FAILED;
    case PORT_SEC_FAILED:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_SEC_FAILED;
    case PORT_PEER_CONNECTION_FAILED:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_PEER_CONNECTION_FAILED;
    case PORT_PEER_FAILED:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_PEER_FAILED;
    case PORT_PEER_TIMEOUT:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_PEER_TIMEOUT;
    case PORT_CLOSED:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_CLOSED;
    case PORT_TX_FULL:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_TX_FULL;
    case PORT_LOCAL_CLOSED:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_LOCAL_CLOSED;
    case PORT_LOCAL_TIMEOUT:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_LOCAL_TIMEOUT;
    case PORT_TX_QUEUE_DISABLED:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_TX_QUEUE_DISABLED;
    case PORT_PAGE_TIMEOUT:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_PAGE_TIMEOUT;
    case PORT_INVALID_SCN:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_INVALID_SCN;
    case PORT_ERR_MAX:
      return android::bluetooth::rfcomm::PortResult::PORT_RESULT_ERR_MAX;
  }
  return android::bluetooth::rfcomm::PortResult::PORT_RESULT_UNDEFINED;
}

static android::bluetooth::rfcomm::RfcommPortState toPortState(tRFC_PORT_STATE state) {
  switch (state) {
    case RFC_STATE_SABME_WAIT_UA:
      return android::bluetooth::rfcomm::RfcommPortState::PORT_STATE_SABME_WAIT_UA;
    case RFC_STATE_ORIG_WAIT_SEC_CHECK:
      return android::bluetooth::rfcomm::RfcommPortState::PORT_STATE_ORIG_WAIT_SEC_CHECK;
    case RFC_STATE_TERM_WAIT_SEC_CHECK:
      return android::bluetooth::rfcomm::RfcommPortState::PORT_STATE_TERM_WAIT_SEC_CHECK;
    case RFC_STATE_OPENED:
      return android::bluetooth::rfcomm::RfcommPortState::PORT_STATE_OPENED;
    case RFC_STATE_DISC_WAIT_UA:
      return android::bluetooth::rfcomm::RfcommPortState::PORT_STATE_DISC_WAIT_UA;
    case RFC_STATE_CLOSED:
      return android::bluetooth::rfcomm::RfcommPortState::PORT_STATE_CLOSED;
  }
  return android::bluetooth::rfcomm::RfcommPortState::PORT_STATE_UNKNOWN;
}
