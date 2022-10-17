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

namespace le_audio {
namespace storage {
/** Set autoconnect information for LeAudio device */
void SetLeAudioAutoconnect(const RawAddress& addr, bool autoconnect);

/** Store ASEs information */
void UpdateHandlesBin(const RawAddress& addr);

/** Store PACs information */
void UpdatePacsBin(const RawAddress& addr);

/** Store ASEs information */
void UpdateAseBin(const RawAddress& addr);

/** Store Le Audio device audio locations */
void SetAudioLocation(const RawAddress& addr, uint32_t sink_location,
                      uint32_t source_location);

/** Store Le Audio device context types */
void SetSupportedContextTypes(const RawAddress& addr,
                              uint16_t sink_supported_context_type,
                              uint16_t source_supported_context_type);

/** Loads information about bonded Le Audio devices */
void AddBondedDevices();

/** Remove the Le Audio device from storage */
void RemoveDevice(const RawAddress& address);

}  // namespace storage
}  // namespace le_audio