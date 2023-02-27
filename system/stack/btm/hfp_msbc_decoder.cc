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

#define LOG_TAG "hfp_msbc_decoder"

#include "hfp_msbc_decoder.h"

#include <base/logging.h>

#include <cfloat>
#include <cstring>

#include "embdrv/sbc/decoder/include/oi_codec_sbc.h"
#include "embdrv/sbc/decoder/include/oi_status.h"
#include "osi/include/allocator.h"
#include "osi/include/log.h"

const int HFP_MSBC_PKT_LEN = 60;
const int HFP_MSBC_FS = 120;
const int HFP_MSBC_SAMPLE_SIZE = 2; /* 2 bytes */
const int HFP_MSBC_PCM_BYTES = HFP_MSBC_FS * HFP_MSBC_SAMPLE_SIZE;

/* Used by PLC */

#define HFP_PLC_WL 256 /* 16ms - Window Length for pattern matching */
#define HFP_PLC_TL 64  /* 4ms - Template Length for matching */
#define HFP_PLC_HL \
  (HFP_PLC_WL + HFP_MSBC_FS - 1) /* Length of History buffer required */
#define HFP_PLC_SBCRL 36         /* SBC Reconvergence sample Length */
#define HFP_PLC_OLAL 16          /* OverLap-Add Length */

/* Disable the PLC when there are more than threshold of lost packets in the
 * window */
#define HFP_PLC_WINDOW_SIZE 5
#define HFP_PLC_PL_THRESHOLD 1

/* The pre-computed SCO packet per HFP 1.7 spec. This mSBC packet will be
 * decoded into all-zero input PCM. */
static const uint8_t hfp_msbc_zero_packet[] = {
    0x01, 0x08, /* Mock H2 header */
    0xad, 0x00, 0x00, 0xc5, 0x00, 0x00, 0x00, 0x00, 0x77, 0x6d, 0xb6, 0xdd,
    0xdb, 0x6d, 0xb7, 0x76, 0xdb, 0x6d, 0xdd, 0xb6, 0xdb, 0x77, 0x6d, 0xb6,
    0xdd, 0xdb, 0x6d, 0xb7, 0x76, 0xdb, 0x6d, 0xdd, 0xb6, 0xdb, 0x77, 0x6d,
    0xb6, 0xdd, 0xdb, 0x6d, 0xb7, 0x76, 0xdb, 0x6d, 0xdd, 0xb6, 0xdb, 0x77,
    0x6d, 0xb6, 0xdd, 0xdb, 0x6d, 0xb7, 0x76, 0xdb, 0x6c,
    /* End of Audio Samples */
    0x00 /* A padding byte defined by mSBC */};

/* Raised Cosine table for OLA */
static const float rcos[HFP_PLC_OLAL] = {
    0.99148655f, 0.96623611f, 0.92510857f, 0.86950446f,
    0.80131732f, 0.72286918f, 0.63683150f, 0.54613418f,
    0.45386582f, 0.36316850f, 0.27713082f, 0.19868268f,
    0.13049554f, 0.07489143f, 0.03376389f, 0.00851345f};

static int16_t f_to_s16(float input) {
  return input > INT16_MAX   ? INT16_MAX
         : input < INT16_MIN ? INT16_MIN
                             : (int16_t)input;
}

typedef struct {
  OI_CODEC_SBC_DECODER_CONTEXT decoder_context;
  uint32_t context_data[CODEC_DATA_WORDS(2, SBC_CODEC_FAST_FILTER_BUFFERS)];
} tHFP_MSBC_DECODER;

static tHFP_MSBC_DECODER hfp_msbc_decoder;

static bool decode_packet(const uint8_t* i_buf, int16_t* o_buf) {
  const OI_BYTE* oi_data;
  uint32_t oi_size, out_avail;

  oi_data = i_buf;
  oi_size = HFP_MSBC_PKT_LEN;
  out_avail = HFP_MSBC_PCM_BYTES;

  OI_STATUS status = OI_CODEC_SBC_DecodeFrame(
      &hfp_msbc_decoder.decoder_context, &oi_data, &oi_size, o_buf, &out_avail);
  if (!OI_SUCCESS(status) || out_avail != HFP_MSBC_PCM_BYTES || oi_size != 0) {
    LOG_ERROR("Decoding failure: %d, %lu, %lu", status,
              (unsigned long)out_avail, (unsigned long)oi_size);
    return false;
  }

  return true;
}

/* This structure tracks the packet loss for last PLC_WINDOW_SIZE of packets */
struct tHFP_PLC_WINDOW {
  bool loss_hist[HFP_PLC_WINDOW_SIZE]; /* The packet loss history of receiving
                                      packets.*/
  unsigned int idx;   /* The index of the to be updated packet loss status. */
  unsigned int count; /* The count of lost packets in the window. */

 public:
  void update_plc_state(bool is_packet_loss) {
    bool* curr = &loss_hist[idx];
    if (is_packet_loss != *curr) {
      count += (is_packet_loss - *curr);
      *curr = is_packet_loss;
    }
    idx = (idx + 1) % HFP_PLC_WINDOW_SIZE;
  }

  bool is_packet_loss_too_high() {
    /* The packet loss count comes from a time window and we use it as an
     * indicator of our confidence of the PLC algorithm. It is known to
     * generate poorer and robotic feeling sounds, when the majority of
     * samples in the PLC history buffer are from the concealment results.
     */
    return count > HFP_PLC_PL_THRESHOLD;
  }
};

/* The PLC is specifically designed for mSBC. The algorithm searches the
 * history of receiving samples to find the best match samples and constructs
 * substitutions for the lost samples. The selection is based on pattern
 * matching a template, composed of a length of samples preceding to the lost
 * samples. It then uses the following samples after the best match as the
 * replacement samples and applies Overlap-Add to reduce the audible
 * distortion.
 *
 * This structure holds related info needed to conduct the PLC algorithm.
 */
struct tHFP_MSBC_PLC {
  int16_t hist[HFP_PLC_HL + HFP_MSBC_FS + HFP_PLC_SBCRL +
               HFP_PLC_OLAL]; /* The history buffer for receiving samples, we
                                 also use it to buffer the processed
                                 replacement samples */
  unsigned best_lag;      /* The index of the best substitution samples in the
                             sample history */
  int handled_bad_frames; /* Number of bad frames handled since the last good
                             frame */
  int16_t decoded_buffer[HFP_MSBC_FS]; /* Used for storing the samples from
                                      decoding the mSBC zero frame packet and
                                      also constructed frames */
  tHFP_PLC_WINDOW*
      pl_window; /* Used to monitor how many packets are bad within the recent
                    HFP_PLC_WINDOW_SIZE of packets. We use this to determine if
                    we want to disable the PLC temporarily */

  int num_decoded_frames; /* Number of total read mSBC frames. */
  int num_lost_frames;    /* Number of total lost mSBC frames. */

  void overlap_add(int16_t* output, float scaler_d, const int16_t* desc,
                   float scaler_a, const int16_t* asc) {
    for (int i = 0; i < HFP_PLC_OLAL; i++) {
      output[i] = f_to_s16(scaler_d * desc[i] * rcos[i] +
                           scaler_a * asc[i] * rcos[HFP_PLC_OLAL - 1 - i]);
    }
  }

  float cross_correlation(int16_t* x, int16_t* y) {
    float sum = 0, x2 = 0, y2 = 0;

    for (int i = 0; i < HFP_PLC_TL; i++) {
      sum += ((float)x[i]) * y[i];
      x2 += ((float)x[i]) * x[i];
      y2 += ((float)y[i]) * y[i];
    }
    return sum / sqrtf(x2 * y2);
  }

  int pattern_match(int16_t* hist) {
    int best = 0;
    float cn, max_cn = FLT_MIN;

    for (int i = 0; i < HFP_PLC_WL; i++) {
      cn = cross_correlation(&hist[HFP_PLC_HL - HFP_PLC_TL], &hist[i]);
      if (cn > max_cn) {
        best = i;
        max_cn = cn;
      }
    }
    return best;
  }

  float amplitude_match(int16_t* x, int16_t* y) {
    uint32_t sum_x = 0, sum_y = 0;
    float scaler;
    for (int i = 0; i < HFP_MSBC_FS; i++) {
      sum_x += abs(x[i]);
      sum_y += abs(y[i]);
    }

    if (sum_y == 0) return 1.2f;

    scaler = (float)sum_x / sum_y;
    return scaler > 1.2f ? 1.2f : scaler < 0.75f ? 0.75f : scaler;
  }

 public:
  void init() {
    if (pl_window) osi_free(pl_window);
    pl_window = (tHFP_PLC_WINDOW*)osi_calloc(sizeof(*pl_window));
  }

  void deinit() {
    if (pl_window) osi_free(pl_window);
    pl_window = nullptr;
  }

  int get_num_decoded_frames() { return num_decoded_frames; }

  int get_num_lost_frames() { return num_lost_frames; }

  void handle_bad_frames(const uint8_t** output) {
    float scaler;
    int16_t* best_match_hist;
    int16_t* frame_head = &hist[HFP_PLC_HL];

    num_decoded_frames++;
    num_lost_frames++;

    /* mSBC codec is stateful, the history of signal would contribute to the
     * decode result decoded_buffer. This should never fail. */
    ASSERT(decode_packet(hfp_msbc_zero_packet, decoded_buffer));

    /* The PLC algorithm is more likely to generate bad results that sound
     * robotic after severe packet losses happened. Only applying it when
     * we are confident. */
    if (!pl_window->is_packet_loss_too_high()) {
      if (handled_bad_frames == 0) {
        /* Finds the best matching samples and amplitude */
        best_lag = pattern_match(hist) + HFP_PLC_TL;
        best_match_hist = &hist[best_lag];
        scaler =
            amplitude_match(&hist[HFP_PLC_HL - HFP_MSBC_FS], best_match_hist);

        /* Constructs the substitution samples */
        overlap_add(frame_head, 1.0, decoded_buffer, scaler, best_match_hist);
        for (int i = HFP_PLC_OLAL; i < HFP_MSBC_FS; i++)
          hist[HFP_PLC_HL + i] = f_to_s16(scaler * best_match_hist[i]);
        overlap_add(&frame_head[HFP_MSBC_FS], scaler,
                    &best_match_hist[HFP_MSBC_FS], 1.0,
                    &best_match_hist[HFP_MSBC_FS]);

        memmove(&frame_head[HFP_MSBC_FS + HFP_PLC_OLAL],
                &best_match_hist[HFP_MSBC_FS + HFP_PLC_OLAL],
                HFP_PLC_SBCRL * HFP_MSBC_SAMPLE_SIZE);
      } else {
        /* Using the existing best lag and copy the following frames */
        memmove(frame_head, &hist[best_lag],
                (HFP_MSBC_FS + HFP_PLC_SBCRL + HFP_PLC_OLAL) *
                    HFP_MSBC_SAMPLE_SIZE);
      }
      /* Copy the constructed frames to decoded buffer for caller to use */
      std::copy(frame_head, &frame_head[HFP_MSBC_FS], decoded_buffer);

      handled_bad_frames++;
    } else {
      /* This is a case similar to receiving a good frame with all zeros, we set
       * handled_bad_frames to zero to prevent the following good frame from
       * being concealed to reconverge with the zero frames we fill in. The
       * concealment result sounds more artificial and weird than simply writing
       * zeros and following samples.
       */
      std::copy(std::begin(decoded_buffer), std::end(decoded_buffer),
                frame_head);
      std::fill(&frame_head[HFP_MSBC_FS],
                &frame_head[HFP_MSBC_FS + HFP_PLC_SBCRL + HFP_PLC_OLAL], 0);
      /* No need to copy the frames as we'll use the decoded zero frames in the
       * decoded buffer as our concealment frames */

      handled_bad_frames = 0;
    }

    *output = (const uint8_t*)decoded_buffer;

    /* Shift the frames to update the history window */
    memmove(hist, &hist[HFP_MSBC_FS],
            (HFP_PLC_HL + HFP_PLC_SBCRL + HFP_PLC_OLAL) * HFP_MSBC_SAMPLE_SIZE);
    pl_window->update_plc_state(1);
  }

  void handle_good_frames(int16_t* input) {
    int16_t* frame_head;
    num_decoded_frames++;
    if (handled_bad_frames != 0) {
      /* If there was a packet concealment before this good frame, we need to
       * reconverge the input frames */
      frame_head = &hist[HFP_PLC_HL];

      /* For the first good frame after packet loss, we need to conceal the
       * received samples to have it reconverge with the true output */
      std::copy(frame_head, &frame_head[HFP_PLC_SBCRL], input);
      /* Overlap the input frame with the previous output frame */
      overlap_add(&input[HFP_PLC_SBCRL], 1.0, &frame_head[HFP_PLC_SBCRL], 1.0,
                  &input[HFP_PLC_SBCRL]);
      handled_bad_frames = 0;
    }

    /* Shift the history and update the good frame to the end of it */
    memmove(hist, &hist[HFP_MSBC_FS],
            (HFP_PLC_HL - HFP_MSBC_FS) * HFP_MSBC_SAMPLE_SIZE);
    std::copy(input, &input[HFP_MSBC_FS], &hist[HFP_PLC_HL - HFP_MSBC_FS]);
    pl_window->update_plc_state(0);
  }
};

static tHFP_MSBC_PLC* plc; /* PLC component to handle packet loss */

bool hfp_msbc_decoder_init() {
  if (plc) {
    plc->deinit();
    osi_free(plc);
  }
  plc = (tHFP_MSBC_PLC*)osi_calloc(sizeof(*plc));
  plc->init();

  OI_STATUS status = OI_CODEC_SBC_DecoderReset(
      &hfp_msbc_decoder.decoder_context, hfp_msbc_decoder.context_data,
      sizeof(hfp_msbc_decoder.context_data), 1, 1, false);
  if (!OI_SUCCESS(status)) {
    LOG_ERROR("%s: OI_CODEC_SBC_DecoderReset failed with error code %d",
              __func__, status);
    return false;
  }

  status = OI_CODEC_SBC_DecoderConfigureMSbc(&hfp_msbc_decoder.decoder_context);
  if (!OI_SUCCESS(status)) {
    LOG_ERROR("%s: OI_CODEC_SBC_DecoderConfigureMSbc failed with error code %d",
              __func__, status);
    return false;
  }

  return true;
}

bool hfp_msbc_decoder_cleanup(int* num_decoded_frames,
                              double* packet_loss_ratio) {
  auto deinit = [&]() {
    if (plc) {
      plc->deinit();
      osi_free(plc);
      plc = nullptr;
    }

    memset(&hfp_msbc_decoder, 0, sizeof(hfp_msbc_decoder));
  };

  if (!plc || !num_decoded_frames || !packet_loss_ratio) {
    deinit();
    return false;
  }

  int decoded_frames = plc->get_num_decoded_frames();
  int lost_frames = plc->get_num_lost_frames();
  if (decoded_frames <= 0 || lost_frames <= 0 || lost_frames > decoded_frames) {
    deinit();
    return false;
  }

  *num_decoded_frames = decoded_frames;
  *packet_loss_ratio = (double)lost_frames / decoded_frames;

  deinit();
  return true;
}

uint32_t hfp_msbc_decoder_decode_packet(const uint8_t* i_buf, int16_t* o_buf) {
  if (i_buf == nullptr) {
    LOG_DEBUG("No valid mSBC packet to decode");
    goto packet_loss;
  }

  if (!decode_packet(i_buf, o_buf)) {
    goto packet_loss;
  }

  plc->handle_good_frames(o_buf);
  return HFP_MSBC_PCM_BYTES;

packet_loss:
  plc->handle_bad_frames((const uint8_t**)&o_buf);
  return HFP_MSBC_PCM_BYTES;
}
