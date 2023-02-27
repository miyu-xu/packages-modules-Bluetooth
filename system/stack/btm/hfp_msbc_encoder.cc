/*
 * Copyright (C) 2022 The Android Open Source Project
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

#define LOG_TAG "hfp_msbc_encoder"

#include "hfp_msbc_encoder.h"

#include <cstring>

#include "embdrv/sbc/encoder/include/sbc_encoder.h"
#include "osi/include/log.h"

const int HFP_MSBC_PCM_BYTES = 240;
const int HFP_MSBC_FRAME_LEN = 57;

/* The pre-computed mSBC audio frame per HFP 1.7 spec. This mSBC frame will be
 * decoded into all-zero input PCM. */
static const uint8_t hfp_msbc_zero_frame[] = {
    0xad, 0x00, 0x00, 0xc5, 0x00, 0x00, 0x00, 0x00, 0x77, 0x6d, 0xb6, 0xdd,
    0xdb, 0x6d, 0xb7, 0x76, 0xdb, 0x6d, 0xdd, 0xb6, 0xdb, 0x77, 0x6d, 0xb6,
    0xdd, 0xdb, 0x6d, 0xb7, 0x76, 0xdb, 0x6d, 0xdd, 0xb6, 0xdb, 0x77, 0x6d,
    0xb6, 0xdd, 0xdb, 0x6d, 0xb7, 0x76, 0xdb, 0x6d, 0xdd, 0xb6, 0xdb, 0x77,
    0x6d, 0xb6, 0xdd, 0xdb, 0x6d, 0xb7, 0x76, 0xdb, 0x6c,
};

typedef struct {
  SBC_ENC_PARAMS sbc_encoder_params;
} tHFP_MSBC_ENCODER;

static tHFP_MSBC_ENCODER hfp_msbc_encoder = {};

void hfp_msbc_encoder_init(void) {
  SBC_ENC_PARAMS* p_encoder_params = &hfp_msbc_encoder.sbc_encoder_params;
  p_encoder_params->s16SamplingFreq = SBC_sf16000;
  p_encoder_params->s16ChannelMode = SBC_MONO;
  p_encoder_params->s16NumOfSubBands = 8;
  p_encoder_params->s16NumOfChannels = 1;
  p_encoder_params->s16NumOfBlocks = 15;
  p_encoder_params->s16AllocationMethod = SBC_LOUDNESS;
  p_encoder_params->s16BitPool = 26;
  p_encoder_params->Format = SBC_FORMAT_MSBC;
}

void hfp_msbc_encoder_cleanup(void) { hfp_msbc_encoder = {}; }

uint32_t hfp_msbc_encode_frames(int16_t* input, uint8_t* output) {
  uint32_t encoded_size =
      SBC_Encode(&hfp_msbc_encoder.sbc_encoder_params, input, output);
  if (encoded_size != HFP_MSBC_FRAME_LEN) {
    LOG_WARN("Encoding invalid packet size: %lu", (unsigned long)encoded_size);
    std::memcpy(output, hfp_msbc_zero_frame, HFP_MSBC_FRAME_LEN);
  }
  output[HFP_MSBC_FRAME_LEN] = 0;
  return HFP_MSBC_PCM_BYTES;
}
