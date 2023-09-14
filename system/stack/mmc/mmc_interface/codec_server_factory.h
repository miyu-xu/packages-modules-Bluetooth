/*
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

#ifndef MMC_MMC_INTERFACE_CODEC_SERVER_FACTORY_H_
#define MMC_MMC_INTERFACE_CODEC_SERVER_FACTORY_H_

#include <memory>
#include <optional>

#include "mmc/codec_server/a2dp_aac_mmc_encoder.h"
#include "mmc/codec_server/hfp_lc3_mmc_decoder.h"
#include "mmc/codec_server/hfp_lc3_mmc_encoder.h"
#include "mmc/mmc_interface/mmc_interface.h"
#include "mmc/proto/mmc_config.pb.h"

namespace mmc {

// Generates a codec server from |ConfigParam|.
class CodecServerFactory {
 public:
  // Returns:
  //   codec server instance on succeed.
  //   "no value" on wrong configuration.
  std::optional<std::unique_ptr<MmcInterface>> CreateCodecServer(
      ConfigParam* config) {
    if (config.has_hfp_lc3_decoder_param()) {
      return std::make_unique<HfpLc3Decoder>();
    } else if (config.has_hfp_lc3_encoder_param()) {
      return std::make_unique<HfpLc3Encoder>();
    }
#if !defined(EXCLUDE_NONSTANDARD_CODECS)
    else if (config.has_a2dp_aac_encoder_param()) {
      return std::make_unique<A2dpAacEncoder>();
    }
#endif
    else {
      return std::nullopt;
    }
  }
};
}  // namespace mmc

#endif  // MMC_MMC_INTERFACE_CODEC_SERVER_FACTORY_H_
