/**
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

#include "a2dp_ext.h"

#include <base/logging.h>

#include "a2dp_codec_api.h"
#include "audio_hal_interface/a2dp_encoding.h"

A2dpCodecConfigExt::A2dpCodecConfigExt(btav_a2dp_codec_index_t codec_index,
                                       bool is_source)
    : A2dpCodecConfig(
          codec_index,
          bluetooth::audio::a2dp::provider::codec_index_str(codec_index)
              .value(),
          BTAV_A2DP_CODEC_PRIORITY_DEFAULT),
      is_source_(is_source) {
  // Load the local capabilities from the provider info.
  auto result = ::bluetooth::audio::a2dp::provider::codec_info(
      codec_index, ota_codec_config_, &codec_capability_);
  LOG_ASSERT(result) << "provider::codec_info unexpectdly failed";
  codec_selectable_capability_ = codec_capability_;
}

bool A2dpCodecConfigExt::setCodecConfig(const uint8_t* p_peer_codec_info,
                                        bool is_capability,
                                        uint8_t* p_result_codec_config) {
  // XXX call get_a2dp_config to recompute best capabilities
  // need to update:
  //   - codec_capability_
  //   - codec_config_
  return false;
}

bool A2dpCodecConfigExt::setPeerCodecCapabilities(
    const uint8_t* p_peer_codec_capabilities) {
  // setPeerCodecCapabilities updates the selectable
  // capabilities in the codec config. It can be safely
  // ignored as providing a superset of the selectable
  // capabilities is safe.
  return true;
}
