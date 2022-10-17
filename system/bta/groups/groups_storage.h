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
namespace groups {
namespace storage {

/** Adds the bonded Le Audio device grouping info into the NVRAM */
void AddGroups(const RawAddress& addr);

/** Deletes the bonded Le Audio device grouping info from the NVRAM */
void RemoveGroups(const RawAddress& address);

/** Loads information about bonded group devices */
void LoadBondedGroups(void);

}  // namespace storage
}  // namespace groups
}  // namespace bluetooth