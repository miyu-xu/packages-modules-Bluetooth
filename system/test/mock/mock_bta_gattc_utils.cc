/*
 * Copyright 2021 The Android Open Source Project
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

/*
 * Generated mock file from original source file
 *   Functions generated:21
 */

#include <cstdint>
#include <map>
#include <string>

#include "bt_target.h"
#include "bta/gatt/bta_gattc_int.h"
#include "bta/include/bta_ar_api.h"
#include "bta/include/utl.h"
#include "btif/include/btif_config.h"
#include "main/shim/dumpsys.h"
#include "osi/include/log.h"
#include "osi/include/osi.h"
#include "osi/include/properties.h"
#include "stack/include/acl_api.h"
#include "stack/include/bt_hdr.h"
#include "test/common/mock_functions.h"
#include "types/hci_role.h"
#include "types/raw_address.h"
#include "types/bt_transport.h"

#ifndef UNUSED_ATTR
#define UNUSED_ATTR
#endif



tBTA_GATTC_CLCB* bta_gattc_find_alloc_clcb(tGATT_IF client_if,
                                           const RawAddress& remote_bda,
                                           tBT_TRANSPORT transport) {
  inc_func_call_count(__func__);
  return nullptr;
}
