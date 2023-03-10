/******************************************************************************
 *
 *  Copyright 2018 The Android Open Source Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

#include "stack/btm/security_device_record.h"
#include "types/raw_address.h"

/** Removes the device from acceptlist */
extern void BTM_AcceptlistRemove(const RawAddress& address);

/** Clear the acceptlist, end any pending acceptlist connections */
extern void BTM_AcceptlistClear();

/** Resolve a raw address based on the provided security record */
extern const tBLE_BD_ADDR BTM_ConvertToAddressWithType(
    const RawAddress& bd_addr, const tBTM_SEC_DEV_REC* p_dev_rec);