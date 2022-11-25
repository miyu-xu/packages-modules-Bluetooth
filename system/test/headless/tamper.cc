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

#define LOG_TAG "bt_headless_tamper"

#include "test/headless/tamper.h"

#include "main/shim/acl_api.h"
#include "stack/acl/acl.h"
#include "test/headless/handler.h"
#include "test/headless/log.h"
#include "types/bt_transport.h"
#include "types/raw_address.h"

tACL_CONN* acl_get_connection_from_address(const RawAddress& bd_addr,
                                           tBT_TRANSPORT transport);

void bluetooth::test::headless::disconnector(
    [[maybe_unused]] bluetooth::test::headless::Handler* handler,
    const RawAddress& bd_addr, tBT_TRANSPORT transport) {
  sleep(4);
  LOG_CONSOLE("tamper: Issuing disconnect addr:%s", bd_addr.ToString().c_str());

  tACL_CONN* p_acl = acl_get_connection_from_address(bd_addr, transport);
  if (p_acl == nullptr) {
    LOG_CONSOLE("ERROR Unable to find acl peer:%s transport:%s",
                bd_addr.ToString().c_str(),
                bt_transport_text(transport).c_str());
  } else {
    LOG_CONSOLE("Forcing disconnect handle:%hu", p_acl->hci_handle);
    bluetooth::shim::ACL_Disconnect(p_acl->hci_handle, true, HCI_ERR_PEER_USER,
                                    "headless");
  }
  //  handler->Post(bluetooth::common::BindOnce(heartbeat, handler));
}
