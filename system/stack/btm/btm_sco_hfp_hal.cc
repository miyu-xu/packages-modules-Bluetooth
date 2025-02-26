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

#include "device/include/esco_parameters.h"
#include "osi/include/properties.h"

namespace hfp_hal_interface {
namespace {
bool offload_supported;
bool offload_enabled;
std::vector<bt_codec> cached_codecs;
}  // namespace

void init() {
  cached_codecs.clear();

  cached_codecs.push_back({
          .codec = codec::CVSD,
          .data_path = ESCO_DATA_PATH_PCM,
          .pkt_size = kDefaultPacketSize,
  });

  // We assert WBS support on either SW path (!offload_enabled) or HW path (offload_enabled).
  // offload_enabled is defined by sysprop and never changes during runtime.
  if (osi_property_get_bool("bluetooth.hfp.software_datapath.enabled", false)) {
    offload_supported = false;
    offload_enabled = false;
    cached_codecs.push_back({
            .codec = codec::MSBC_TRANSPARENT,
            .data_path = ESCO_DATA_PATH_HCI,
            // TODO(b/387424290): Query the USB cap and select the size here. Currently all devices
            // support alt 6 so leave it 60 for now.
            .pkt_size = 60,
    });
  } else {
    offload_supported = true;
    offload_enabled = true;
    cached_codecs.push_back({
            .codec = codec::MSBC,
            .data_path = ESCO_DATA_PATH_PCM,
            .pkt_size = kDefaultPacketSize,
    });
  }

  for (const auto& c : cached_codecs) {
    bluetooth::log::info("Caching HFP codec {}, data path {}, pkt_size {}", (uint64_t)c.codec,
                         c.data_path, c.pkt_size);
  }
}

// This is not used in Android.
bool is_coding_format_supported(esco_coding_format_t /* coding_format */) { return true; }

// Android statically compiles WBS support.
bool get_wbs_supported() { return true; }

// Software path (!offload_supported) implies support of SWB.
bool get_swb_supported() {
  return !offload_supported || osi_property_get_bool("bluetooth.hfp.swb.supported", false);
}

// Checks the supported codecs
bt_codecs get_codec_capabilities(uint64_t codecs) {
  bt_codecs codec_list = {.offload_capable = offload_supported};

  for (const auto& c : cached_codecs) {
    if (c.codec & codecs) {
      codec_list.codecs.push_back(c);
    }
  }

  return codec_list;
}

// Check if hardware offload is supported
bool get_offload_supported() { return offload_supported; }

// Check if hardware offload is enabled
bool get_offload_enabled() { return offload_supported && offload_enabled; }

// This is not used in Android.
bool enable_offload(bool /* enable */) { return true; }

// On Android, this is a no-op because the settings default to work and offload mode won't change.
void set_codec_datapath(tBTA_AG_UUID_CODEC /* codec_uuid */) {}

size_t get_packet_size(int codec) {
  for (const auto& c : cached_codecs) {
    if (c.codec == static_cast<uint64_t>(codec)) {
      return c.pkt_size;
    }
  }

  return kDefaultPacketSize;
}

void notify_sco_connection_change(RawAddress /* device */, bool /* is_connected */,
                                  int /* codec */) {
  // Do nothing since this is handled by Android's audio hidl.
}

// On Android, this is a no-op because the settings default to work for Android.
void update_esco_parameters(enh_esco_params_t* /* p_parms */) {}
}  // namespace hfp_hal_interface
