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

/*******************************************************************************
 *
 *  Filename:      btif_sdp.c
 *  Description:   SDP Bluetooth Interface.
 *                 Implements the generic message handling and search
 *                 functionality.
 *                 References btif_sdp_server.c for SDP record creation.
 *
 ******************************************************************************/

#define LOG_TAG "bt_btif_l2test"

#include <hardware/bluetooth.h>
#include <hardware/bt_l2test.h>
#include <stdlib.h>
#include <string.h>

#include "osi/include/allocator.h"
#include "osi/include/log.h"
#include "stack/include/l2c_api.h"
#include "types/raw_address.h"

/*****************************************************************************
 *  Functions implemented in l2c_api.cc
 *****************************************************************************/

static void echo_cb(const RawAddress& addr, uint16_t len, uint8_t* data) {
  LOG_INFO("%s. len %d", ADDRESS_TO_LOGGABLE_CSTR(addr), len);
}

static bool l2cap_echo(const RawAddress& p_bd_addr) {
  BT_HDR* p_buf = (BT_HDR*)osi_calloc(sizeof(BT_HDR) + 3);
  p_buf->offset = 0;
  memset(p_buf->data, 10, 3);
  return L2CA_Echo(p_bd_addr, p_buf, echo_cb);
}

static const btl2test_interface_t l2test_if = {sizeof(btl2test_interface_t),
                                               l2cap_echo};

const btl2test_interface_t* btif_l2cap_test_get_interface(void) {
  BTIF_TRACE_DEBUG("%s", __func__);
  return &l2test_if;
}
