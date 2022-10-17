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

#include "bta_hearing_aid_api.h"

namespace bluetooth {
namespace hearing_aid {
namespace storage {

void AddHearingDeviceToStorage(const HearingDevice& dev_info);

/** Deletes the bonded hearing aid device info from NVRAM */
void RemoveHearingDeviceFromStorage(const RawAddress& address);

/** Loads information about bonded hearing aid devices */
void LoadBondedHearingAidsFromStorage();

/** Set/Unset the hearing aid device HEARING_AID_IS_ACCEPTLISTED flag. */
void SetHearingDeviceAcceptlist(const RawAddress& address,
                                bool add_to_acceptlist);

/** Get the hearing aid device properties. */
bool GetHearingDeviceProperties(const RawAddress& address,
                                uint8_t* capabilities, uint64_t* hi_sync_id,
                                uint16_t* render_delay,
                                uint16_t* preparation_delay, uint16_t* codecs);

}  // namespace storage
}  // namespace hearing_aid
}  // namespace bluetooth
