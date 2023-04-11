/*
 * Copyright 2016 The Android Open Source Project
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

#define LOG_TAG "a2dp_aac_encoder"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/channel_layout.h>
#include <libavutil/common.h>
#include <libavutil/frame.h>
#include <libavutil/samplefmt.h>
}

#include <base/logging.h>
#include <inttypes.h>
#include <stdio.h>
#include <string.h>

#include <string>

#include "a2dp_aac.h"
#include "a2dp_aac_encoder.h"
#include "common/time_util.h"
#include "osi/include/allocator.h"
#include "osi/include/log.h"
#include "osi/include/osi.h"
#include "stack/include/bt_hdr.h"

//
// Encoder for AAC Source Codec
//

// A2DP AAC encoder interval in milliseconds
#define A2DP_AAC_ENCODER_INTERVAL_MS 20

// offset
#if (BTA_AV_CO_CP_SCMS_T == TRUE)
#define A2DP_AAC_OFFSET (AVDT_MEDIA_OFFSET + 1)
#else
#define A2DP_AAC_OFFSET AVDT_MEDIA_OFFSET
#endif

const int A2DP_AAC_HEADER_LEN = 9;
const int A2DP_AAC_MAX_LEN_REPR = 4;
constexpr int A2DP_AAC_MAX_PREFIX_SIZE =
    A2DP_AAC_OFFSET + A2DP_AAC_HEADER_LEN + A2DP_AAC_MAX_LEN_REPR;

typedef struct {
  uint32_t sample_rate;
  uint8_t channel_mode;
  uint8_t bits_per_sample;
  uint32_t frame_length;     // Samples per channel in a frame
  uint8_t input_channels_n;  // Number of channels
} tA2DP_AAC_ENCODER_PARAMS;

typedef struct {
  float counter;
  uint32_t bytes_per_tick; /* pcm bytes read each media task tick */
  uint64_t last_frame_us;
} tA2DP_AAC_FEEDING_STATE;

typedef struct {
  uint64_t session_start_us;

  size_t media_read_total_expected_packets;
  size_t media_read_total_expected_reads_count;
  size_t media_read_total_expected_read_bytes;

  size_t media_read_total_dropped_packets;
  size_t media_read_total_actual_reads_count;
  size_t media_read_total_actual_read_bytes;
} a2dp_aac_encoder_stats_t;

typedef struct {
  a2dp_source_read_callback_t read_callback;
  a2dp_source_enqueue_callback_t enqueue_callback;
  uint16_t TxAaMtuSize;

  bool use_SCMS_T;
  tA2DP_ENCODER_INIT_PEER_PARAMS peer_params;
  uint32_t timestamp;  // Timestamp for the A2DP frames

  AVCodecContext* aac_context;

  tA2DP_FEEDING_PARAMS feeding_params;
  tA2DP_AAC_ENCODER_PARAMS aac_encoder_params;
  tA2DP_AAC_FEEDING_STATE aac_feeding_state;

  a2dp_aac_encoder_stats_t stats;
} tA2DP_AAC_ENCODER_CB;

static tA2DP_AAC_ENCODER_CB a2dp_aac_encoder_cb;

static uint32_t a2dp_aac_encoder_interval_ms = A2DP_AAC_ENCODER_INTERVAL_MS;

static void a2dp_aac_encoder_update(A2dpCodecConfig* a2dp_codec_config,
                                    bool* p_restart_input,
                                    bool* p_restart_output,
                                    bool* p_config_updated);
static void a2dp_aac_get_num_frame_iteration(uint8_t* num_of_iterations,
                                             uint8_t* num_of_frames,
                                             uint64_t timestamp_us);
static void a2dp_aac_encode_frames(uint8_t nb_frame);
static bool a2dp_aac_read_feeding(uint8_t* read_buffer, uint32_t* bytes_read);
static uint16_t adjust_effective_mtu(
    const tA2DP_ENCODER_INIT_PEER_PARAMS& peer_params);

bool A2DP_LoadEncoderAac(void) {
  // Nothing to do - the library is statically linked
  return true;
}

void A2DP_UnloadEncoderAac(void) {
  // Nothing to do - the library is statically linked
  if (a2dp_aac_encoder_cb.aac_context)
    avcodec_free_context(&a2dp_aac_encoder_cb.aac_context);
  memset(&a2dp_aac_encoder_cb, 0, sizeof(a2dp_aac_encoder_cb));
}

void a2dp_aac_encoder_init(const tA2DP_ENCODER_INIT_PEER_PARAMS* p_peer_params,
                           A2dpCodecConfig* a2dp_codec_config,
                           a2dp_source_read_callback_t read_callback,
                           a2dp_source_enqueue_callback_t enqueue_callback) {
  if (a2dp_aac_encoder_cb.aac_context)
    avcodec_free_context(&a2dp_aac_encoder_cb.aac_context);
  memset(&a2dp_aac_encoder_cb, 0, sizeof(a2dp_aac_encoder_cb));

  a2dp_aac_encoder_cb.stats.session_start_us =
      bluetooth::common::time_get_os_boottime_us();

  a2dp_aac_encoder_cb.read_callback = read_callback;
  a2dp_aac_encoder_cb.enqueue_callback = enqueue_callback;
  a2dp_aac_encoder_cb.peer_params = *p_peer_params;
  a2dp_aac_encoder_cb.timestamp = 0;

  a2dp_aac_encoder_cb.use_SCMS_T = false;  // TODO: should be a parameter
#if (BTA_AV_CO_CP_SCMS_T == TRUE)
  a2dp_aac_encoder_cb.use_SCMS_T = true;
#endif

  // NOTE: Ignore the restart_input / restart_output flags - this initization
  // happens when the audio session is (re)started.
  bool restart_input = false;
  bool restart_output = false;
  bool config_updated = false;
  a2dp_aac_encoder_update(a2dp_codec_config, &restart_input, &restart_output,
                          &config_updated);
}

// Update the A2DP AAC encoder.
// |a2dp_codec_config| is the A2DP codec to use for the update.
static void a2dp_aac_encoder_update(A2dpCodecConfig* a2dp_codec_config,
                                    bool* p_restart_input,
                                    bool* p_restart_output,
                                    bool* p_config_updated) {
  tA2DP_AAC_ENCODER_PARAMS* p_encoder_params =
      &a2dp_aac_encoder_cb.aac_encoder_params;
  uint8_t codec_info[AVDT_CODEC_SIZE];

  *p_restart_input = false;
  *p_restart_output = false;
  *p_config_updated = false;

  AVCodecContext* ctx = a2dp_aac_encoder_cb.aac_context;

  const AVCodec* codec = avcodec_find_encoder(AV_CODEC_ID_AAC);

  if (!a2dp_aac_encoder_cb.aac_context) {
    if (!codec) {
      LOG_ERROR("%s: Codec not found.", __func__);
      return;
    }

    ctx = avcodec_alloc_context3(codec);
    if (!ctx) {
      LOG_ERROR("%s: Cannot allocate codec context.", __func__);
      return;
    }
  }

  if (!a2dp_codec_config->copyOutOtaCodecConfig(codec_info)) {
    LOG_ERROR(
        "%s: Cannot update the codec encoder for %s: "
        "invalid codec config",
        __func__, a2dp_codec_config->name().c_str());
    return;
  }
  const uint8_t* p_codec_info = codec_info;

  // The feeding parameters
  tA2DP_FEEDING_PARAMS* p_feeding_params = &a2dp_aac_encoder_cb.feeding_params;
  p_feeding_params->sample_rate = A2DP_GetTrackSampleRateAac(p_codec_info);
  p_feeding_params->bits_per_sample =
      a2dp_codec_config->getAudioBitsPerSample();
  p_feeding_params->channel_count = A2DP_GetTrackChannelCountAac(p_codec_info);
  LOG_INFO("%s: sample_rate=%u bits_per_sample=%u channel_count=%u", __func__,
           p_feeding_params->sample_rate, p_feeding_params->bits_per_sample,
           p_feeding_params->channel_count);

  // The codec parameters
  p_encoder_params->sample_rate =
      a2dp_aac_encoder_cb.feeding_params.sample_rate;
  p_encoder_params->channel_mode = A2DP_GetChannelModeCodeAac(p_codec_info);

  const tA2DP_ENCODER_INIT_PEER_PARAMS& peer_params =
      a2dp_aac_encoder_cb.peer_params;
  a2dp_aac_encoder_cb.TxAaMtuSize = adjust_effective_mtu(peer_params);
  LOG_INFO("%s: MTU=%d, peer_mtu=%d", __func__, a2dp_aac_encoder_cb.TxAaMtuSize,
           peer_params.peer_mtu);
  LOG_INFO("%s: sample_rate: %d channel_mode: %d ", __func__,
           p_encoder_params->sample_rate, p_encoder_params->channel_mode);

  ctx->sample_rate = p_encoder_params->sample_rate;

  ctx->bit_rate = A2DP_GetBitRateAac(p_codec_info);
  const int max_bit_rate = A2DP_ComputeMaxBitRateAac(
      p_codec_info, a2dp_aac_encoder_cb.TxAaMtuSize - A2DP_AAC_MAX_PREFIX_SIZE);
  if (ctx->bit_rate > max_bit_rate) {
    LOG_INFO("%s: Requested bit rate: %d, limit to: %d", __func__,
             ctx->bit_rate, max_bit_rate);
    ctx->bit_rate = max_bit_rate;
  }
  LOG_INFO("%s: MTU = %d Sampling Frequency = %d Bit Rate = %d", __func__,
           a2dp_aac_encoder_cb.TxAaMtuSize, ctx->sample_rate, ctx->bit_rate);
  if (ctx->bit_rate == -1) {
    LOG_ERROR("%s: invalid codec bit rate", __func__);
    return;
  }

  // TODO: patch FFmpeg so this dictates that the bit rate is the upperbound
  ctx->flags |= AV_CODEC_FLAG_LOW_DELAY;

  p_encoder_params->input_channels_n =
      A2DP_GetTrackChannelCountAac(p_codec_info);
  if (p_encoder_params->input_channels_n == 1) {
    AVChannelLayout mono = AV_CHANNEL_LAYOUT_MONO;
    av_channel_layout_copy(&ctx->ch_layout, &mono);
  } else if (p_encoder_params->input_channels_n == 2) {
    AVChannelLayout stereo = AV_CHANNEL_LAYOUT_STEREO;
    av_channel_layout_copy(&ctx->ch_layout, &stereo);
  } else {
    LOG_ERROR("%s: invalid number of channels", __func__);
    return;
  }

  ctx->sample_fmt = AV_SAMPLE_FMT_FLTP;

  int error = avcodec_open2(ctx, codec, NULL);
  if (error < 0) {
    LOG_ERROR("%s: Could not open codec: '%s'", __func__, av_err2str(error));
    return;
  }

  // Retrieve the encoder info so we can save the frame length
  p_encoder_params->frame_length = ctx->frame_size;
  LOG_INFO("%s: AAC frame_length = %u input_channels_n = %u", __func__,
           p_encoder_params->frame_length, p_encoder_params->input_channels_n);

  a2dp_aac_encoder_cb.aac_context = ctx;

  // After encoder params ready, reset the feeding state and its interval.
  a2dp_aac_feeding_reset();
}

void a2dp_aac_encoder_cleanup(void) {
  if (a2dp_aac_encoder_cb.aac_context)
    avcodec_free_context(&a2dp_aac_encoder_cb.aac_context);
  memset(&a2dp_aac_encoder_cb, 0, sizeof(a2dp_aac_encoder_cb));
}

void a2dp_aac_feeding_reset(void) {
  auto frame_length = a2dp_aac_encoder_cb.aac_encoder_params.frame_length;
  auto sample_rate = a2dp_aac_encoder_cb.feeding_params.sample_rate;
  if (a2dp_aac_encoder_cb.aac_context) {
    a2dp_aac_encoder_cb.aac_context->sample_rate = sample_rate;
  }
  if (frame_length == 0 || sample_rate == 0) {
    LOG_WARN("%s: AAC encoder is not configured", __func__);
    a2dp_aac_encoder_interval_ms = A2DP_AAC_ENCODER_INTERVAL_MS;
  } else {
    // PCM data size per AAC frame (bits)
    // = aac_encoder_params.frame_length * feeding_params.bits_per_sample
    //   * feeding_params.channel_count
    // = feeding_params.sample_rate * feeding_params.bits_per_sample
    //   * feeding_params.channel_count * (T_interval_ms / 1000);
    // Here we use the nearest integer not greater than the value.
    a2dp_aac_encoder_interval_ms = frame_length * 1000 / sample_rate;
    if (a2dp_aac_encoder_interval_ms < A2DP_AAC_ENCODER_INTERVAL_MS)
      a2dp_aac_encoder_interval_ms = A2DP_AAC_ENCODER_INTERVAL_MS;
  }

  /* By default, just clear the entire state */
  memset(&a2dp_aac_encoder_cb.aac_feeding_state, 0,
         sizeof(a2dp_aac_encoder_cb.aac_feeding_state));

  a2dp_aac_encoder_cb.aac_feeding_state.bytes_per_tick =
      (a2dp_aac_encoder_cb.feeding_params.sample_rate *
       a2dp_aac_encoder_cb.feeding_params.bits_per_sample / 8 *
       a2dp_aac_encoder_cb.feeding_params.channel_count *
       a2dp_aac_encoder_interval_ms) /
      1000;

  LOG_INFO("%s: PCM bytes %u per tick %u ms", __func__,
           a2dp_aac_encoder_cb.aac_feeding_state.bytes_per_tick,
           a2dp_aac_encoder_interval_ms);
}

void a2dp_aac_feeding_flush(void) {
  a2dp_aac_encoder_cb.aac_feeding_state.counter = 0.0f;
}

uint64_t a2dp_aac_get_encoder_interval_ms(void) {
  return a2dp_aac_encoder_interval_ms;
}

int a2dp_aac_get_effective_frame_size() {
  return a2dp_aac_encoder_cb.TxAaMtuSize;
}

void a2dp_aac_send_frames(uint64_t timestamp_us) {
  uint8_t nb_frame = 0;
  uint8_t nb_iterations = 0;

  a2dp_aac_get_num_frame_iteration(&nb_iterations, &nb_frame, timestamp_us);
  LOG_VERBOSE("%s: Sending %d frames per iteration, %d iterations", __func__,
              nb_frame, nb_iterations);
  if (nb_frame == 0) return;

  for (uint8_t counter = 0; counter < nb_iterations; counter++) {
    // Transcode frame and enqueue
    a2dp_aac_encode_frames(nb_frame);
  }
}

// Obtains the number of frames to send and number of iterations
// to be used. |num_of_iterations| and |num_of_frames| parameters
// are used as output param for returning the respective values.
static void a2dp_aac_get_num_frame_iteration(uint8_t* num_of_iterations,
                                             uint8_t* num_of_frames,
                                             uint64_t timestamp_us) {
  uint32_t result = 0;
  uint8_t nof = 0;
  uint8_t noi = 1;

  uint32_t pcm_bytes_per_frame =
      a2dp_aac_encoder_cb.aac_encoder_params.frame_length *
      a2dp_aac_encoder_cb.feeding_params.channel_count *
      a2dp_aac_encoder_cb.feeding_params.bits_per_sample / 8;
  LOG_VERBOSE("%s: pcm_bytes_per_frame %u", __func__, pcm_bytes_per_frame);

  uint32_t us_this_tick = a2dp_aac_encoder_interval_ms * 1000;
  uint64_t now_us = timestamp_us;
  if (a2dp_aac_encoder_cb.aac_feeding_state.last_frame_us != 0)
    us_this_tick =
        (now_us - a2dp_aac_encoder_cb.aac_feeding_state.last_frame_us);
  a2dp_aac_encoder_cb.aac_feeding_state.last_frame_us = now_us;

  a2dp_aac_encoder_cb.aac_feeding_state.counter +=
      (float)a2dp_aac_encoder_cb.aac_feeding_state.bytes_per_tick *
      us_this_tick / (a2dp_aac_encoder_interval_ms * 1000);

  result = a2dp_aac_encoder_cb.aac_feeding_state.counter / pcm_bytes_per_frame;
  a2dp_aac_encoder_cb.aac_feeding_state.counter -= result * pcm_bytes_per_frame;
  nof = result;

  LOG_VERBOSE("%s: effective num of frames %u, iterations %u", __func__, nof,
              noi);

  *num_of_frames = nof;
  *num_of_iterations = noi;
}

static bool ffmpeg_encode_frame(AVCodecContext* ctx,
                                tA2DP_FEEDING_PARAMS* p_feeding_params,
                                uint8_t* i_buf, int i_len, BT_HDR* o_buf) {
  int rc;
  uint8_t* packet = (uint8_t*)(o_buf + 1) + o_buf->offset;

  AVFrame* frame = av_frame_alloc();

  frame->nb_samples = ctx->frame_size;
  frame->format = ctx->sample_fmt;
  frame->sample_rate = ctx->sample_rate;

  rc = av_channel_layout_copy(&frame->ch_layout, &ctx->ch_layout);
  if (rc < 0) {
    LOG_ERROR("%s: failed to copy channel layout: '%s'", __func__,
              av_err2str(rc));
    av_frame_free(&frame);
    return false;
  }

  rc = av_frame_get_buffer(frame, 0);
  if (rc < 0) {
    LOG_ERROR("%s: failed to get buffer for frame: '%s'", __func__,
              av_err2str(rc));
    av_frame_free(&frame);
    return false;
  }

  rc = av_frame_make_writable(frame);
  if (rc < 0) {
    LOG_ERROR("%s: failed to make frame writable: '%s'", __func__,
              av_err2str(rc));
    av_frame_free(&frame);
    return false;
  }

  const int sample_rate = p_feeding_params->sample_rate;
  const int bit_depth = p_feeding_params->bits_per_sample;
  const int bytes_per_sample = bit_depth / 8;
  const float scaling_factor = (float)1 / (1 << (bit_depth - 1));

  if (bit_depth != 16 && bit_depth != 32) {
    LOG_ERROR("%s: Unsupported bit depth %d", bit_depth);
    av_frame_free(&frame);
    return false;
  }

  uint8_t* buff = i_buf;
  float* data[] = {(float*)frame->data[0], (float*)frame->data[1]};
  for (int i = 0; i < i_len / bytes_per_sample; ++i) {
    int pcm = bit_depth == 16 ? *((int16_t*)buff) : *((int32_t*)buff);
    *data[i & 1]++ = pcm * scaling_factor;
    buff += bytes_per_sample;
  }

  AVPacket* pkt = av_packet_alloc();

  rc = avcodec_send_frame(ctx, frame);
  if (rc < 0) {
    LOG_ERROR("%s: failed to send_frame: '%s'", __func__, av_err2str(rc));
    av_frame_free(&frame);
    av_packet_free(&pkt);
    return false;
  }

  rc = avcodec_receive_packet(ctx, pkt);
  if (rc == -EAGAIN) {
    LOG_INFO("%s: encoder is buffering: '%s'", __func__, av_err2str(rc));
    av_frame_free(&frame);
    av_packet_free(&pkt);
    return true;
  } else if (rc < 0) {
    LOG_ERROR("%s: avcodec_receive_packet: '%s'", __func__, av_err2str(rc));
    a2dp_aac_encoder_cb.stats.media_read_total_dropped_packets++;
    av_frame_free(&frame);
    av_packet_free(&pkt);
    return false;
  }

  uint8_t* dst = (uint8_t*)(o_buf + 1) + o_buf->offset;

  uint8_t header[A2DP_AAC_HEADER_LEN] = {
      0x47, 0xfc, 0x00,
      0x00, 0xb0, (uint8_t)(sample_rate == 44100 ? 0x90 : 0x8c),
      0x80, 0x03, 0x00,
  };
  memcpy(dst, header, A2DP_AAC_HEADER_LEN);
  dst += A2DP_AAC_HEADER_LEN;
  int written = A2DP_AAC_HEADER_LEN;

  int cap = a2dp_aac_get_effective_frame_size();
  if (cap < pkt->size + A2DP_AAC_MAX_PREFIX_SIZE) {
    LOG_WARN("%s: dropped packet: size=%d, cap=%d'", __func__, pkt->size, cap);
    static uint8_t silent_frame[7] = {
        0x06, 0x21, 0x10, 0x04, 0x60, 0x8c, 0x1c,
    };
    memcpy(dst, silent_frame, 7);
    dst += 7;
    written += 7;
  } else {
    int fsize = pkt->size;

    while (fsize >= 255) {
      *(dst++) = 0xff;
      fsize -= 255;
      ++written;
    }
    *(dst++) = fsize;
    ++written;

    memcpy(dst, pkt->data, pkt->size);
    written += pkt->size;
  }

  av_packet_unref(pkt);
  av_frame_free(&frame);
  av_packet_free(&pkt);

  o_buf->layer_specific++;
  o_buf->len += written;

  return true;
}

static void a2dp_aac_encode_frames(uint8_t nb_frame) {
  tA2DP_AAC_ENCODER_PARAMS* p_encoder_params =
      &a2dp_aac_encoder_cb.aac_encoder_params;
  tA2DP_FEEDING_PARAMS* p_feeding_params = &a2dp_aac_encoder_cb.feeding_params;
  uint8_t read_buffer[BT_DEFAULT_BUFFER_SIZE];
  int pcm_bytes_per_frame = p_encoder_params->frame_length *
                            p_feeding_params->channel_count *
                            p_feeding_params->bits_per_sample / 8;
  CHECK(pcm_bytes_per_frame <= static_cast<int>(sizeof(read_buffer)));
  CHECK(a2dp_aac_encoder_cb.aac_context);

  uint32_t total_bytes_read = 0;

  while (nb_frame) {
    a2dp_aac_encoder_cb.stats.media_read_total_expected_packets++;

    uint32_t bytes_read = 0;
    if (!a2dp_aac_read_feeding(read_buffer, &bytes_read)) {
      LOG_WARN("%s: underflow %d", __func__, nb_frame);
      a2dp_aac_encoder_cb.aac_feeding_state.counter +=
          nb_frame * p_encoder_params->frame_length *
          p_feeding_params->channel_count * p_feeding_params->bits_per_sample /
          8;
      return;
    }

    total_bytes_read += bytes_read;

    BT_HDR* p_buf = (BT_HDR*)osi_malloc(BT_DEFAULT_BUFFER_SIZE);
    memset(p_buf, 0x00, BT_DEFAULT_BUFFER_SIZE);
    p_buf->offset = A2DP_AAC_OFFSET;
    p_buf->len = 0;
    p_buf->layer_specific = 0;

    if (!ffmpeg_encode_frame(a2dp_aac_encoder_cb.aac_context, p_feeding_params,
                             read_buffer, bytes_read, p_buf)) {
      a2dp_aac_encoder_cb.stats.media_read_total_dropped_packets++;
      osi_free(p_buf);
      return;
    }

    --nb_frame;

    if (p_buf->len == 0) {
      LOG_INFO("%s: dropped a frame, may be due to buffering.", __func__);
      a2dp_aac_encoder_cb.stats.media_read_total_dropped_packets++;
      osi_free(p_buf);
      continue;
    }

    *((uint32_t*)(p_buf + 1)) = a2dp_aac_encoder_cb.timestamp;

    a2dp_aac_encoder_cb.timestamp +=
        p_buf->layer_specific * p_encoder_params->frame_length;

    if (!a2dp_aac_encoder_cb.enqueue_callback(p_buf, 1, total_bytes_read))
      return;
  }
}

static bool a2dp_aac_read_feeding(uint8_t* read_buffer, uint32_t* bytes_read) {
  uint32_t read_size = a2dp_aac_encoder_cb.aac_encoder_params.frame_length *
                       a2dp_aac_encoder_cb.feeding_params.channel_count *
                       a2dp_aac_encoder_cb.feeding_params.bits_per_sample / 8;

  a2dp_aac_encoder_cb.stats.media_read_total_expected_reads_count++;
  a2dp_aac_encoder_cb.stats.media_read_total_expected_read_bytes += read_size;

  /* Read Data from UIPC channel */
  uint32_t nb_byte_read =
      a2dp_aac_encoder_cb.read_callback(read_buffer, read_size);
  a2dp_aac_encoder_cb.stats.media_read_total_actual_read_bytes += nb_byte_read;
  *bytes_read = nb_byte_read;

  if (nb_byte_read < read_size) {
    if (nb_byte_read == 0) return false;

    /* Fill the unfilled part of the read buffer with silence (0) */
    memset(((uint8_t*)read_buffer) + nb_byte_read, 0, read_size - nb_byte_read);
    nb_byte_read = read_size;
  }
  a2dp_aac_encoder_cb.stats.media_read_total_actual_reads_count++;

  return true;
}

static uint16_t adjust_effective_mtu(
    const tA2DP_ENCODER_INIT_PEER_PARAMS& peer_params) {
  uint16_t mtu_size = BT_DEFAULT_BUFFER_SIZE - A2DP_AAC_OFFSET - sizeof(BT_HDR);
  if (mtu_size > peer_params.peer_mtu) {
    mtu_size = peer_params.peer_mtu;
  }
  LOG_VERBOSE("%s: original AVDTP MTU size: %d", __func__, mtu_size);
  if (peer_params.is_peer_edr && !peer_params.peer_supports_3mbps) {
    // This condition would be satisfied only if the remote device is
    // EDR and supports only 2 Mbps, but the effective AVDTP MTU size
    // exceeds the 2DH5 packet size.
    LOG_VERBOSE("%s: The remote device is EDR but does not support 3 Mbps",
                __func__);
    if (mtu_size > MAX_2MBPS_AVDTP_MTU) {
      LOG_WARN("%s: Restricting AVDTP MTU size from %d to %d", __func__,
               mtu_size, MAX_2MBPS_AVDTP_MTU);
      mtu_size = MAX_2MBPS_AVDTP_MTU;
    }
  }
  return mtu_size;
}

void A2dpCodecConfigAacSource::debug_codec_dump(int fd) {
  a2dp_aac_encoder_stats_t* stats = &a2dp_aac_encoder_cb.stats;

  A2dpCodecConfig::debug_codec_dump(fd);

  auto codec_specific_1 = getCodecConfig().codec_specific_1;
  dprintf(
      fd,
      "  AAC bitrate mode                                        : %s "
      "(0x%" PRIx64 ")\n",
      ((codec_specific_1 & ~A2DP_AAC_VARIABLE_BIT_RATE_MASK) == 0 ? "Constant"
                                                                  : "Variable"),
      codec_specific_1);
  dprintf(fd, "  Encoder interval (ms): %" PRIu64 "\n",
          a2dp_aac_get_encoder_interval_ms());
  dprintf(fd, "  Effective MTU: %d\n", a2dp_aac_get_effective_frame_size());
  dprintf(fd,
          "  Packet counts (expected/dropped)                        : %zu / "
          "%zu\n",
          stats->media_read_total_expected_packets,
          stats->media_read_total_dropped_packets);

  dprintf(fd,
          "  PCM read counts (expected/actual)                       : %zu / "
          "%zu\n",
          stats->media_read_total_expected_reads_count,
          stats->media_read_total_actual_reads_count);

  dprintf(fd,
          "  PCM read bytes (expected/actual)                        : %zu / "
          "%zu\n",
          stats->media_read_total_expected_read_bytes,
          stats->media_read_total_actual_read_bytes);
}
