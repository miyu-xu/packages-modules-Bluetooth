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

namespace bluetooth {
namespace csis {
namespace storage {

void SetAutoconnect(const RawAddress& addr, bool autoconnect);

/** Stores information about the bonded CSIS device */
void UpdateInfo(const RawAddress& addr);

/** Loads information about the bonded CSIS device */
void LoadBondedDevices(void);

/** Removes information about the bonded CSIS device */
void RemoveDevice(const RawAddress& address);

}  // namespace storage
}  // namespace csis
}  // namespace bluetooth