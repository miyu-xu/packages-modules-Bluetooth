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

#include "btm_sco_hfp_hal.h"

#include <vector>

namespace hfp_hal_interface {
namespace {
bool offload_supported = false;
bool offload_enabled = false;

struct Codec {
codec codec_type;
uint8_t date_path;
std::vector<uint8_t> data;
};

std::vector<Codec> cached_codecs;

}

void init() {
    // update offload_supported
    // update offload_enabled

    // update supported codecs
}

// Check if wideband speech is supported on local device
bool get_wbs_supported() {
    for (Codec c: cached_codecs) {
        if (c.codec_type == MSBC || c.codec_type == MSBC_TRANSPARENT) {
            return true;
        }
    }
    return false;
}

// Checks the supported codecs
bt_codecs get_codec_capabilities(uint64_t codecs) {
    return {};
}

// Check if hardware offload is supported
bool get_offload_supported() {
    return offload_supported;
}

// Check if hardware offload is enabled
bool get_offload_enabled() {
    return offload_supported && offload_enabled;
}

// Set offload enable/disable
bool enable_offload(bool enable) {
    if (!offload_supported) {
        return false;
    }
    offload_enabled = enable;
    return true;

}

// Notify the codec datapath to lower layer for offload mode
bool set_codec_datapath(int codec) {
    // notify the lower layer
    return true;
}

// Get the maximum supported packet size from the lower layer
int get_packet_size();

// Notify the lower layer about SCO connection change
void notify_sco_connection_change(RawAddress device, bool is_connected, int codec);

int get_packet_size() {
  // implement me using ioctl
  return 0xff;
}

void notify_sco_connection_change(RawAddress device, bool is_connected) {
}

}


// bool hfp_hal_interface::get_wbs_supported() {
//   return DISABLE_WBS == false;
// }

// int hfp_hal_interface::get_packet_size() {
//   // for hardware encoding, let's assume we can use the maximum SCO packet size (0xff)
//   return 0xff;
// }

// // Notify the lower layer about SCO connection change
// void hfp_hal_interface::notify_sco_connection_change(RawAddress, bool) {
//   // for hardware encoding, we don't need to notify the lower layer; it should be notified in hardware path
// }
