/******************************************************************************
 *
 *  Copyright 2025, The Android Open Source Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

extern "C" {

#include <stdbool.h>
#include <stdint.h>

/**
 * Supported Codecs types
 */
enum swoff_codec_type {
  SWOFF_CODEC_LC3,
  SWOFF_CODEC_OPUS,
};

/**
 * LC3 Configuration
 */
struct swoff_lc3_config {
  bool hr_mode;
  int frame_duration_us;
  int sample_rate;
  int block_bytes;
};

/**
 * Opus Configuration
 */
struct swoff_opus_config {};

/**
 * Codec configuration
 */
struct swoff_codec {
  enum swoff_codec_type type;
  union {
    struct swoff_lc3_config lc3;
  } config;
};

/**
 * BIS/CIS definition
 */
struct swoff_iso_stream {
  uint16_t handle;
  unsigned channel_allocation;
};

/**
 * Control Callbacks, from Rust to C
 * Theses functions can be called from different threads, but NOT concurrently.
 * Locking over `handle` is not necessary.
 */
struct swoff_lea_callbacks {
  /**
   * Passed as the first parameter of all functions.
   */
  void *handle;

  /**
   * Called when an ISO stream of a group has started; the input PCM FIFO
   * is open, and should be fed with `swoff_leaudio_write()`.
   * This function MUST not be NULL.
   */
  void (*start)(void *handle);

  /**
   * Called when all the ISO streams of a group have stopped; the PCM FIFO
   * is closed, calling `swoff_leaudio_write()` will fail.
   * This function MUST not be NULL.
   */
  void (*stop)(void *handle);
};

/**
 * Handle of Software Offload audio stream,
 * returned by `swoff_lea_stream_t`, it must be passed to all
 * other interface functions operating to this stream.
 */
typedef struct swoff_lea_stream *swoff_lea_stream_t;

/**
 * Setup an input PCM audio stream transported on one or more CIS/BIS Isochronous
 * streams, as specified by the `num_iso_streams` entries of the `iso_streams` table.
 * The `bitdepth` of the inputs PCM stream must be 16, 24 or 32 bits per sample.
 * When not NULL, the `handle` returned shall be passed of the first parameter
 * of other functions acting on this stream.
 * In case of error, a NULL value is returned.
 */
swoff_lea_stream_t swoff_leaudio_setup(const struct swoff_iso_stream iso_streams[],
                                       size_t num_iso_streams, int bitdepth,
                                       const struct swoff_codec *codec,
                                       const struct swoff_lea_callbacks *callbacks);

/**
 * Free a non NULL `handle` returned by `swoff_leaudio_setup()`.
 * At the call of this function, the `handle` must no more by used by any other
 * interface functions.
 */
void swoff_leaudio_drop(swoff_lea_stream_t handle);

/**
 * Write a chunk of input PCM stream. The stream must be PCM Stereo, channels
 * interleaved, and occupy 2, 3 or 4 bytes per samples according to the `bitdepth`
 * indicated by `swoff_leaudio_setup()`.
 *
 * This function blocks until there is space in the input FIFO; it is required
 * that the caller follows the imposed write speed.
 *
 * This function returns `0` on success. `-1` is returned when `callbacks.start()`
 * has not yet been called, or `callbacks.stop()` has been called.
 */
int swoff_leaudio_write(swoff_lea_stream_t handle, const uint8_t *data, size_t len);
}
