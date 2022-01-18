/*
 * Copyright 2021 The Android Open Source Project
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

#define LOG_TAG "a2dp_vendor_lc3_encoder"
#define ATRACE_TAG ATRACE_TAG_AUDIO

#include "a2dp_vendor_lc3_encoder.h"

#include <dlfcn.h>
#include <inttypes.h>
#include <stdio.h>
#include <string.h>

#include "a2dp_vendor.h"
#include "a2dp_vendor_lc3.h"
#include "common/time_util.h"
#include "lc3.h"
#include "osi/include/allocator.h"
#include "osi/include/log.h"
#include "osi/include/osi.h"

//#ifndef ENCODER_FILE_DUMP
//#define ENCODER_FILE_DUMP
//#endif

#ifdef ENCODER_FILE_DUMP
#include <fcntl.h>
int enc_timestamps = 0;
int raw_enc_wav = 0;
uint32_t enc_num_frames = 0;
#endif

//
// Encoder for LC3 Source Codec
//
//
// offset
#if (BTA_AV_CO_CP_SCMS_T == TRUE)
#define A2DP_LC3_OFFSET (AVDT_MEDIA_OFFSET + A2DP_LC3_MPL_HDR_LEN + 1)
#else
#define A2DP_LC3_OFFSET (AVDT_MEDIA_OFFSET + A2DP_LC3_MPL_HDR_LEN)
#endif

typedef struct {
  uint32_t sample_rate;
  uint8_t channel_count;
  uint16_t frame_duration;
  uint16_t frame_size;
  uint16_t byte_count;
} tA2DP_LC3_ENCODER_PARAMS;

typedef struct {
  float counter;
  uint32_t bytes_per_tick; /* pcm bytes read each media task tick */
  uint64_t last_frame_us;
} tA2DP_LC3_FEEDING_STATE;

typedef struct {
  uint64_t session_start_us;

  size_t media_read_total_expected_packets;
  size_t media_read_total_expected_reads_count;
  size_t media_read_total_expected_read_bytes;

  size_t media_read_total_dropped_packets;
  size_t media_read_total_actual_reads_count;
  size_t media_read_total_actual_read_bytes;
} a2dp_lc3_encoder_stats_t;

typedef struct {
  a2dp_source_read_callback_t read_callback;
  a2dp_source_enqueue_callback_t enqueue_callback;

  bool use_SCMS_T;
  tA2DP_ENCODER_INIT_PEER_PARAMS peer_params;
  uint32_t timestamp;  // Timestamp for the A2DP frames

  void* lc3_encoder_left_mem;
  void* lc3_encoder_right_mem;
  lc3_encoder_t lc3_encoder_left;
  lc3_encoder_t lc3_encoder_right;

  tA2DP_FEEDING_PARAMS feeding_params;
  tA2DP_LC3_ENCODER_PARAMS lc3_encoder_params;
  tA2DP_LC3_FEEDING_STATE lc3_feeding_state;

  a2dp_lc3_encoder_stats_t stats;
} tA2DP_LC3_ENCODER_CB;

static tA2DP_LC3_ENCODER_CB a2dp_lc3_encoder_cb;

static bool a2dp_vendor_lc3_encoder_update(A2dpCodecConfig* a2dp_codec_config,
                                           bool* p_restart_input,
                                           bool* p_restart_output,
                                           bool* p_config_updated);
static void a2dp_lc3_get_num_frame_iteration(uint8_t* num_of_iterations,
                                             uint8_t* num_of_frames,
                                             uint64_t timestamp_us);
static void a2dp_lc3_encode_frames(uint8_t nb_frame);
static bool a2dp_lc3_read_feeding(uint8_t* read_buffer, uint32_t* bytes_read);

bool A2DP_VendorLoadEncoderLc3(void) {
  // Nothing to do - the library is statically linked
  return true;
}

void A2DP_VendorUnloadEncoderLc3(void) {
  // Nothing to do - the library is statically linked
}

void a2dp_vendor_lc3_encoder_cleanup(void) {
  osi_free(a2dp_lc3_encoder_cb.lc3_encoder_left_mem);
  osi_free(a2dp_lc3_encoder_cb.lc3_encoder_right_mem);
  a2dp_lc3_encoder_cb.lc3_encoder_left_mem = nullptr;
  a2dp_lc3_encoder_cb.lc3_encoder_right_mem = nullptr;
  memset(&a2dp_lc3_encoder_cb, 0, sizeof(a2dp_lc3_encoder_cb));

  a2dp_lc3_encoder_cb.stats.session_start_us =
      bluetooth::common::time_get_os_boottime_us();

  a2dp_lc3_encoder_cb.timestamp = 0;

  a2dp_lc3_encoder_cb.use_SCMS_T = false;  // TODO: should be a parameter
#if (BTA_AV_CO_CP_SCMS_T == TRUE)
  a2dp_lc3_encoder_cb.use_SCMS_T = true;
#endif
}

void a2dp_vendor_lc3_encoder_init(
    const tA2DP_ENCODER_INIT_PEER_PARAMS* p_peer_params,
    A2dpCodecConfig* a2dp_codec_config,
    a2dp_source_read_callback_t read_callback,
    a2dp_source_enqueue_callback_t enqueue_callback) {
  a2dp_vendor_lc3_encoder_cleanup();

  a2dp_lc3_encoder_cb.read_callback = read_callback;
  a2dp_lc3_encoder_cb.enqueue_callback = enqueue_callback;
  a2dp_lc3_encoder_cb.peer_params = *p_peer_params;
  a2dp_lc3_encoder_cb.timestamp = 0;

  // NOTE: Ignore the restart_input / restart_output flags - this initization
  // happens when the connection is (re)started.
  bool restart_input = false;
  bool restart_output = false;
  bool config_updated = false;
  a2dp_vendor_lc3_encoder_update(a2dp_codec_config, &restart_input,
                                 &restart_output, &config_updated);

  int dt_us = a2dp_lc3_encoder_cb.lc3_encoder_params.frame_duration;
  int sr_hz = a2dp_lc3_encoder_cb.lc3_encoder_params.sample_rate;
  unsigned enc_size = lc3_encoder_size(dt_us, sr_hz);
  a2dp_lc3_encoder_cb.lc3_encoder_left_mem = osi_malloc(enc_size);
  a2dp_lc3_encoder_cb.lc3_encoder_left =
      lc3_setup_encoder(dt_us, sr_hz, a2dp_lc3_encoder_cb.lc3_encoder_left_mem);
  a2dp_lc3_encoder_cb.lc3_encoder_right_mem = osi_malloc(enc_size);
  a2dp_lc3_encoder_cb.lc3_encoder_right = lc3_setup_encoder(
      dt_us, sr_hz, a2dp_lc3_encoder_cb.lc3_encoder_right_mem);
  LOG_ERROR("%s:dt_us=%d nbytes=%d bitrate=%d", __func__, dt_us,
            a2dp_lc3_encoder_cb.lc3_encoder_params.byte_count,
            A2DP_LC3_DEFAULT_BITRATE);

  if (a2dp_lc3_encoder_cb.lc3_encoder_left == nullptr ||
      a2dp_lc3_encoder_cb.lc3_encoder_right == nullptr) {
    LOG_ERROR("%s: failed to allocate and init lc3_encoder_t", __func__);
  } else {
    LOG_ERROR("%s: allocated and init lc3_encoder_t success", __func__);
  }

  return;
}

// Update the A2DP LC3 encoder.
// |a2dp_codec_config| is the A2DP codec to use for the update.
static bool a2dp_vendor_lc3_encoder_update(A2dpCodecConfig* a2dp_codec_config,
                                           bool* p_restart_input,
                                           bool* p_restart_output,
                                           bool* p_config_updated) {
  tA2DP_LC3_ENCODER_PARAMS* p_encoder_params =
      &a2dp_lc3_encoder_cb.lc3_encoder_params;
  uint8_t codec_info[AVDT_CODEC_SIZE];

  *p_restart_input = false;
  *p_restart_output = false;
  *p_config_updated = false;
  if (!a2dp_codec_config->copyOutOtaCodecConfig(codec_info)) {
    LOG_ERROR(
        "%s: Cannot update the codec encoder for %s: "
        "invalid codec config",
        __func__, a2dp_codec_config->name().c_str());
    return false;
  }
  const uint8_t* p_codec_info = codec_info;
  btav_a2dp_codec_config_t codec_config = a2dp_codec_config->getCodecConfig();

  // The feeding parameters
  tA2DP_FEEDING_PARAMS* p_feeding_params = &a2dp_lc3_encoder_cb.feeding_params;
  p_feeding_params->sample_rate =
      A2DP_VendorGetTrackSampleRateLc3(p_codec_info);
  p_feeding_params->bits_per_sample =
      a2dp_codec_config->getAudioBitsPerSample();
  p_feeding_params->channel_count =
      A2DP_VendorGetTrackChannelCountLc3(p_codec_info);
  LOG_INFO("%s: sample_rate=%u bits_per_sample=%u channel_count=%u", __func__,
           p_feeding_params->sample_rate, p_feeding_params->bits_per_sample,
           p_feeding_params->channel_count);

  // The codec parameters
  p_encoder_params->sample_rate =
      a2dp_lc3_encoder_cb.feeding_params.sample_rate;
  p_encoder_params->channel_count = p_feeding_params->channel_count;
  switch (codec_config.codec_specific_2) {
    case BTAV_A2DP_CODEC_FRAME_SIZE_750:
      p_encoder_params->frame_duration = 7500;
      break;
    case BTAV_A2DP_CODEC_FRAME_SIZE_1000:
      p_encoder_params->frame_duration = 10000;
  }
  p_encoder_params->frame_size = lc3_frame_samples(
      p_encoder_params->frame_duration, p_encoder_params->sample_rate);
  p_encoder_params->byte_count =
      lc3_frame_bytes(a2dp_lc3_encoder_cb.lc3_encoder_params.frame_duration,
                      A2DP_LC3_DEFAULT_BITRATE);

  LOG_INFO(
      "%s: sample_rate=%u channel_count=%u frame_duration=%u frame_size=%u "
      "byte_count=%u",
      __func__, p_encoder_params->sample_rate, p_encoder_params->channel_count,
      p_encoder_params->frame_duration, p_encoder_params->frame_size,
      p_encoder_params->byte_count);
  a2dp_vendor_lc3_feeding_reset();
  return true;
}

void a2dp_vendor_lc3_feeding_reset(void) {
  /* By default, just clear the entire state */
  memset(&a2dp_lc3_encoder_cb.lc3_feeding_state, 0,
         sizeof(a2dp_lc3_encoder_cb.lc3_feeding_state));

  a2dp_lc3_encoder_cb.lc3_feeding_state.bytes_per_tick =
      (a2dp_lc3_encoder_cb.feeding_params.sample_rate *
       a2dp_lc3_encoder_cb.feeding_params.bits_per_sample / 8 *
       a2dp_lc3_encoder_cb.feeding_params.channel_count *
       a2dp_lc3_encoder_cb.lc3_encoder_params.frame_duration) /
      (1000 * 1000);

  LOG_INFO("%s: PCM bytes per tick %u", __func__,
           a2dp_lc3_encoder_cb.lc3_feeding_state.bytes_per_tick);

#ifdef ENCODER_FILE_DUMP
  if (raw_enc_wav > 0) {
    close(raw_enc_wav);
    close(enc_timestamps);
    LOG_ERROR("%s: closed lc3_enc.raw. frames sent %d", __func__,
              enc_num_frames);
    enc_num_frames = 0;
    raw_enc_wav = 0;
  } else
    LOG_ERROR("%s: failed to close lc3_enc.raw", __func__);
#endif
}

void a2dp_vendor_lc3_feeding_flush(void) {
  a2dp_lc3_encoder_cb.lc3_feeding_state.counter = 0.0f;
}

uint64_t a2dp_vendor_lc3_get_encoder_interval_ms(void) {
  return a2dp_lc3_encoder_cb.lc3_encoder_params.frame_duration / 1000;
}

int a2dp_vendor_lc3_get_effective_frame_size() {
  return a2dp_lc3_encoder_cb.peer_params.peer_mtu;
}

void a2dp_vendor_lc3_send_frames(uint64_t timestamp_us) {
  uint8_t nb_frame = 0;
  uint8_t nb_iterations = 0;

  a2dp_lc3_get_num_frame_iteration(&nb_iterations, &nb_frame, timestamp_us);
  LOG_WARN("%s: Sending %d frames per iteration, %d iterations", __func__,
           nb_frame, nb_iterations);
  if (nb_frame == 0) return;

  for (uint8_t counter = 0; counter < nb_iterations; counter++) {
    a2dp_lc3_encode_frames(nb_frame);
  }
}

// Obtains the number of frames to send and number of iterations
// to be used. |num_of_iterations| and |num_of_frames| parameters
// are used as output param for returning the respective values.
static void a2dp_lc3_get_num_frame_iteration(uint8_t* num_of_iterations,
                                             uint8_t* num_of_frames,
                                             uint64_t timestamp_us) {
  uint32_t result = 0;
  uint8_t nof = 0;
  uint8_t noi = 1;
  uint32_t pcm_bytes_per_frame =
      a2dp_lc3_encoder_cb.lc3_feeding_state.bytes_per_tick;
  LOG_WARN("%s: pcm_bytes_per_frame %u", __func__, pcm_bytes_per_frame);

  uint32_t us_this_tick = a2dp_lc3_encoder_cb.lc3_encoder_params.frame_duration;
  uint64_t now_us = timestamp_us;
  if (a2dp_lc3_encoder_cb.lc3_feeding_state.last_frame_us != 0)
    us_this_tick =
        (now_us - a2dp_lc3_encoder_cb.lc3_feeding_state.last_frame_us);
  a2dp_lc3_encoder_cb.lc3_feeding_state.last_frame_us = now_us;

  a2dp_lc3_encoder_cb.lc3_feeding_state.counter +=
      (float)a2dp_lc3_encoder_cb.lc3_feeding_state.bytes_per_tick *
      us_this_tick / a2dp_lc3_encoder_cb.lc3_encoder_params.frame_duration;

  result = a2dp_lc3_encoder_cb.lc3_feeding_state.counter / pcm_bytes_per_frame;
  a2dp_lc3_encoder_cb.lc3_feeding_state.counter -= result * pcm_bytes_per_frame;
  nof = result;

  LOG_WARN("%s: effective num of frames %u, iterations %u", __func__, nof, noi);

  *num_of_frames = nof;
  *num_of_iterations = noi;
}

static void a2dp_lc3_encode_frames(uint8_t nb_frame) {
  tA2DP_LC3_ENCODER_PARAMS* p_encoder_params =
      &a2dp_lc3_encoder_cb.lc3_encoder_params;
  unsigned char* packet;
  uint8_t remain_nb_frame = nb_frame;
  uint16_t lc3_frame_size = p_encoder_params->frame_size;
  uint8_t read_buffer[1024 /* max frameSize */ * 4 /* max bytes/sample */ *
                      2 /* max chs */];
  uint8_t left_buffer[1024 /* max frameSize */ * 4 /* max bytes/sample */ *
                      2 /* max chs */];
  uint8_t right_buffer[1024 /* max frameSize */ * 4 /* max bytes/sample */ *
                       2 /* max chs */];

#ifdef ENCODER_FILE_DUMP
  if (raw_enc_wav == 0) {
    raw_enc_wav = open("/data/misc/bluetooth/logs/lc3_enc.raw", O_WRONLY);
    enc_timestamps = open("/data/misc/bluetooth/logs/lc3_enc_ts.raw", O_WRONLY);
    if (raw_enc_wav > 0)
      LOG_ERROR("%s: opened lc3_enc.raw", __func__);
    else
      LOG_ERROR("%s: failed to open lc3_enc.raw", __func__);
  }
#endif

  uint32_t count = 0;
  int32_t out_frames = 0;
  uint32_t written = 0;
  uint32_t temp_bytes_read = 0;
  uint32_t bytes_read = 0;
  while (nb_frame) {
    BT_HDR* p_buf = (BT_HDR*)osi_malloc(BT_DEFAULT_BUFFER_SIZE);
    p_buf->offset = A2DP_LC3_OFFSET;
    p_buf->len = 0;
    p_buf->layer_specific = 0;
    a2dp_lc3_encoder_cb.stats.media_read_total_expected_packets++;

    if (nb_frame > 1) LOG_WARN("%s: nb_frame %d", __func__, nb_frame);

    do {
      //
      // Read the PCM data and encode it
      //
      temp_bytes_read = 0;
      if (a2dp_lc3_read_feeding(read_buffer, &temp_bytes_read)) {
        bytes_read += temp_bytes_read;
        packet = (unsigned char*)(p_buf + 1) + p_buf->offset + p_buf->len;

        if (a2dp_lc3_encoder_cb.lc3_encoder_left == NULL ||
            a2dp_lc3_encoder_cb.lc3_encoder_right == NULL) {
          LOG_ERROR("%s: invalid lc3 handle", __func__);
          a2dp_lc3_encoder_cb.stats.media_read_total_dropped_packets++;
          osi_free(p_buf);
          return;
        }

        uint16_t byte_count = p_encoder_params->byte_count;
        int bytes_per_ch = byte_count / 2;

        for (int i = 0; i < lc3_frame_size; ++i) {
          reinterpret_cast<int16_t*>(&left_buffer[0])[i] =
              reinterpret_cast<const int16_t*>(&read_buffer[0])[2 * i];
          reinterpret_cast<int16_t*>(&right_buffer[0])[i] =
              reinterpret_cast<const int16_t*>(&read_buffer[0])[2 * i + 1];
        }
#if 0
        if (a2dp_lc3_encoder_cb.lc3_encoder_params.channel_count == 1) {
          uint8_t mono_buffrt[1024 /* max frameSize */ * 4 /* max bytes/sample */ *
                               2 /* max chs */];
          for (int i = 0; i < lc3_frame_size; ++i) {
            uint16_t left = reinterpret_cast<const int16_t*>(&left_buffer[0])[i];
            uint16_t right = reinterpret_cast<const int16_t*>(&right_buffer[0])[i];
            reinterpret_cast<int16_t*>(&mono[0])[i] =
                (uint16_t)(((uint32_t)left + (uint32_t)right) >> 1);
          }

          lc3_encode(a2dp_lc3_encoder_cb.lc3_encoder_left,
                     reinterpret_cast<int16_t*>(mono), 1, byte_count, packet);

        } else if (a2dp_lc3_encoder_cb.lc3_encoder_params.channel_count == 2) {
#endif
        lc3_encode(a2dp_lc3_encoder_cb.lc3_encoder_left,
                   reinterpret_cast<int16_t*>(left_buffer), 1, bytes_per_ch,
                   packet);
        lc3_encode(a2dp_lc3_encoder_cb.lc3_encoder_right,
                   reinterpret_cast<int16_t*>(right_buffer), 1, bytes_per_ch,
                   packet + bytes_per_ch);
#if 0
        }
#endif
        written = byte_count;

        if (written <= 0) {
          LOG_ERROR("%s: lc3 encoding error", __func__);
          a2dp_lc3_encoder_cb.stats.media_read_total_dropped_packets++;
          osi_free(p_buf);
          return;
        } else {
          out_frames++;
        }
        count += written;
        p_buf->len += written;
        nb_frame--;
        p_buf->layer_specific += out_frames;  // added a frame to the buffer
        LOG_WARN(
            "%s: lc3 encoder %d read %d written, %d outframes %d nb_frame %d "
            "bytesRead %d writtencount",
            __func__, temp_bytes_read, written, out_frames, nb_frame,
            bytes_read, count);

#ifdef ENCODER_FILE_DUMP
        if (raw_enc_wav > 0) {
          (void)write(raw_enc_wav, read_buffer, temp_bytes_read);
          (void)write(enc_timestamps, p_buf + 1, sizeof((uint32_t)(1)));
          ++enc_num_frames;
        }
#endif

      } else {
        LOG_WARN("%s: lc3 src buffer underflow %d", __func__, nb_frame);
        a2dp_lc3_encoder_cb.lc3_feeding_state.counter +=
            nb_frame * lc3_frame_size *  // TODO oo double check this value
            a2dp_lc3_encoder_cb.feeding_params.channel_count *
            a2dp_lc3_encoder_cb.feeding_params.bits_per_sample / 8;

        // no more pcm to read
        nb_frame = 0;
      }
    } while ((written == 0) && nb_frame);

    if (p_buf->len) {
      /*
       * Timestamp of the media packet header represent the TS of the
       * first frame, i.e the timestamp before including this frame.
       */
      *((uint32_t*)(p_buf + 1)) = a2dp_lc3_encoder_cb.timestamp;

      a2dp_lc3_encoder_cb.timestamp += p_buf->layer_specific * lc3_frame_size;

      uint8_t done_nb_frame = remain_nb_frame - nb_frame;
      remain_nb_frame = nb_frame;

      if (!a2dp_lc3_encoder_cb.enqueue_callback(p_buf, done_nb_frame,
                                                temp_bytes_read))
        return;
    } else {
      a2dp_lc3_encoder_cb.stats.media_read_total_dropped_packets++;
      osi_free(p_buf);
    }
  }
}

static bool a2dp_lc3_read_feeding(uint8_t* read_buffer, uint32_t* bytes_read) {
  uint32_t read_size = a2dp_lc3_encoder_cb.lc3_feeding_state.bytes_per_tick;

  a2dp_lc3_encoder_cb.stats.media_read_total_expected_reads_count++;
  a2dp_lc3_encoder_cb.stats.media_read_total_expected_read_bytes += read_size;

  /* Read Data from UIPC channel */
  uint32_t nb_byte_read =
      a2dp_lc3_encoder_cb.read_callback(read_buffer, read_size);
  a2dp_lc3_encoder_cb.stats.media_read_total_actual_read_bytes += nb_byte_read;

  if (nb_byte_read < read_size) {
    if (nb_byte_read == 0) return false;

    /* Fill the unfilled part of the read buffer with silence (0) */
    memset(((uint8_t*)read_buffer) + nb_byte_read, 0, read_size - nb_byte_read);
    nb_byte_read = read_size;
  }
  a2dp_lc3_encoder_cb.stats.media_read_total_actual_reads_count++;

  *bytes_read = nb_byte_read;
  return true;
}

void A2dpCodecConfigLc3Source::debug_codec_dump(int fd) {
  a2dp_lc3_encoder_stats_t* stats = &a2dp_lc3_encoder_cb.stats;

  A2dpCodecConfig::debug_codec_dump(fd);

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
