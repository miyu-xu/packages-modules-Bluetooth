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

#pragma once

#include "a2dp_codec_api.h"
#include "audio_hal_interface/a2dp_encoding.h"

/// Codec configuration for codecs that are supported by a2dp hardware offload
/// codec extensibility. The codec index may be a standard codec, in which case
/// this class is preferred over the dedicated class, or an unknown codec in
/// the reserved ranges for codec extensibility.
/// The codec priority is always the lowest, so that software codecs
/// can be picked over offloaded codecs.
class A2dpCodecConfigExt : public A2dpCodecConfig {
 public:
  A2dpCodecConfigExt(btav_a2dp_codec_index_t codec_index, bool is_source)
      : A2dpCodecConfig(
            codec_index,
            bluetooth::audio::a2dp::codec_index_str(codec_index).value(),
            BTAV_A2DP_CODEC_PRIORITY_DEFAULT),
        is_source_(is_source) {}

  bool init() override { return true; }
  bool useRtpHeaderMarkerBit() const override { return false; }
  bool setCodecConfig(const uint8_t* p_peer_codec_info, bool is_capability,
                      uint8_t* p_result_codec_config) override;
  bool setPeerCodecCapabilities(
      const uint8_t* p_peer_codec_capabilities) override;

 private:
  [[maybe_unused]] bool is_source_;  // True if local is Source
};
