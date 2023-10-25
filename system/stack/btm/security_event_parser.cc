/*
 * Copyright 2023 The Android Open Source Project
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

#include "security_event_parser.h"

#include <optional>
#include <string>

#include "common/metrics.h"
#include "hci/hci_packets.h"
#include "main/shim/helpers.h"
#include "stack/include/btm_sec_api_types.h"
#include "stack/include/sec_hci_link_interface.h"
#include "stack/include/stack_metrics_logging.h"
#include "types/raw_address.h"

using namespace bluetooth::hci;
using android::bluetooth::hci::CMD_UNKNOWN;
using android::bluetooth::hci::STATUS_UNKNOWN;
using bluetooth::common::kUnknownConnectionHandle;

namespace bluetooth::stack::btm {
namespace {
void parse_io_capabilities_req(EventView event) {
  auto request_opt = IoCapabilityRequestView::CreateOptional(event);
  ASSERT(request_opt.has_value());
  auto request = request_opt.value();

  RawAddress p = ToRawAddress(request.GetBdAddr());

  btm_io_capabilities_req(p);
  log_classic_pairing_event(p, kUnknownConnectionHandle, CMD_UNKNOWN,
                            static_cast<uint16_t>(event.GetEventCode()),
                            STATUS_UNKNOWN, STATUS_UNKNOWN, 0);
}
void parse_io_capabilities_rsp(EventView event) {
  auto response_opt = IoCapabilityResponseView::CreateOptional(event);
  ASSERT(response_opt.has_value());
  auto response = response_opt.value();

  tBTM_SP_IO_RSP evt_data{
      .bd_addr = ToRawAddress(response.GetBdAddr()),
      .io_cap = static_cast<tBTM_IO_CAP>(response.GetIoCapability()),
      .oob_data = static_cast<tBTM_OOB_DATA>(response.GetOobDataPresent()),
      .auth_req =
          static_cast<tBTM_AUTH_REQ>(response.GetAuthenticationRequirements()),
  };

  btm_io_capabilities_rsp(evt_data);
  log_classic_pairing_event(evt_data.bd_addr, kUnknownConnectionHandle,
                            CMD_UNKNOWN,
                            static_cast<uint16_t>(event.GetEventCode()),
                            STATUS_UNKNOWN, STATUS_UNKNOWN, 0);
}
}  // namespace
}  // namespace bluetooth::stack::btm

namespace bluetooth::stack::btm {

void SecurityEventParser::OnSecurityEvent(bluetooth::hci::EventView event) {
  switch (event.GetEventCode()) {
    case EventCode::IO_CAPABILITY_REQUEST:
      parse_io_capabilities_req(event);
      break;
    case EventCode::IO_CAPABILITY_RESPONSE:
      parse_io_capabilities_rsp(event);
      break;
    default:
      LOG_ERROR("Unhandled event %s",
                EventCodeText(event.GetEventCode()).c_str());
  }
}
}  // namespace bluetooth::stack::btm
