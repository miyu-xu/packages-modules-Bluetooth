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

#define LOG_TAG "hfp_lc3_decoder"

#include "hfp_lc3_decoder.h"

#include <base/logging.h>

#include <cstring>

#include "embdrv/lc3/include/lc3.h"
#include "osi/include/log.h"

const int HFP_LC3_H2_HEADER_LEN = 2;
const int HFP_LC3_PKT_FRAME_LEN = 58;
const int HFP_LC3_PCM_BYTES = 480;

static void* hfp_lc3_decoder_mem;
static lc3_decoder_t hfp_lc3_decoder;

/* For stats */
static int decoded_frames;
static int lost_frames;

bool hfp_lc3_decoder_init() {
  if (hfp_lc3_decoder_mem) {
    LOG_WARN("%s: The decoder instance should have had been released.",
             __func__);
    free(hfp_lc3_decoder_mem);
  }

  const int dt_us = 7500;
  const int sr_hz = 32000;
  const int sr_pcm_hz = 32000;
  const unsigned dec_size = lc3_decoder_size(dt_us, sr_pcm_hz);

  hfp_lc3_decoder_mem = malloc(dec_size);
  hfp_lc3_decoder =
      lc3_setup_decoder(dt_us, sr_hz, sr_pcm_hz, hfp_lc3_decoder_mem);

  decoded_frames = 0;
  lost_frames = 0;

  return true;
}

bool hfp_lc3_decoder_cleanup(int* num_decoded_frames,
                             double* packet_loss_ratio) {
  auto deinit = [&]() {
    if (hfp_lc3_decoder_mem) {
      free(hfp_lc3_decoder_mem);
      hfp_lc3_decoder_mem = nullptr;
    }

    decoded_frames = 0;
    lost_frames = 0;
  };

  if (!num_decoded_frames || !packet_loss_ratio) {
    deinit();
    return false;
  }

  if (decoded_frames <= 0 || lost_frames <= 0 || lost_frames > decoded_frames) {
    deinit();
    return false;
  }

  *num_decoded_frames = decoded_frames;
  *packet_loss_ratio = (double)lost_frames / decoded_frames;

  deinit();
  return true;
}

uint32_t hfp_lc3_decoder_decode_packet(const uint8_t* i_buf, int16_t* o_buf) {
  const uint8_t* frame = i_buf ? i_buf + HFP_LC3_H2_HEADER_LEN : nullptr;

  /* Note this only fails when wrong parameters are supplied. */
  int rc = lc3_decode(hfp_lc3_decoder, frame, HFP_LC3_PKT_FRAME_LEN,
                      LC3_PCM_FORMAT_S16, o_buf, 1);

  if (rc == 1) {
    ++lost_frames;
    LOG_WARN("%s: PLC conducted", __func__);
    /* TODO(b/269970706): change this to debug log */
  }

  ASSERT(rc >= 0);

  ++decoded_frames;
  return HFP_LC3_PCM_BYTES;
}
