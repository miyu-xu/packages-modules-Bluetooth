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

#pragma once

#include <array>
#include <cstdint>
#include <list>
#include <memory>
#include <utility>
#include <vector>

namespace le_audio {

class LeAudioSourceAudioHalAsrc {

 public:

  // The Asynchronous Sample Rate Conversion (ASRC) is setup from the PCM stream
  // characteristics and the length, expressed in us, of the buffers.
  //
  // An initial buffering is proposed, after a warmup delay of `warmup_delay_ms`,
  // a burst of `initial_packet_buffering` is generated, intended to fulfill
  // the audio pipeline buffering.
  // By experience, it looks like some controllers discard and acknowledge the
  // first packets without following transmission intervals. This behavior leads to
  // trash the initial buffering. The `warmup_delay_ms` helps to ensure that
  // the synchronization with the transmission intervals is done.

  LeAudioSourceAudioHalAsrc(
    int channels, int samplerate, int bitdepth, int interval_us,
    int initial_packet_buffering = 2, int warmup_delay_ms = 200);

  ~LeAudioSourceAudioHalAsrc();

  // Gives an input buffer, and return a list of resamples buffers locked to
  // the cadence of the transmission. The input and output buffers have a fixed
  // size, deducted from the PCM characteristics, given to the constructor.

  std::vector<const std::vector<uint8_t> *> Run(const std::vector<uint8_t>& in);

 private:

  const int samplerate_;
  const int bitdepth_;
  const int interval_us_;

  const int warmup_delay_ms_;
  const int initial_packet_buffering_;

  struct {
    std::array<std::vector<uint8_t>, 3> pool;
    int initial_buffering;
    int index, offset;
  } buffers_;

  std::vector<uint8_t>& silence_buffer_;
  size_t buffers_size_;

  unsigned stream_us_;
  double drift_z0_, drift_us_;
  unsigned out_counter_;

  class ClockRecovery;
  std::unique_ptr<ClockRecovery> clock_recovery_;

  template <typename TH, typename TS, typename TD> class Resampler;
  using ResamplerI16 = Resampler<int8_t , int16_t, int32_t>;
  using ResamplerI32 = Resampler<int16_t, int32_t, int64_t>;
  std::unique_ptr<std::vector<ResamplerI16>> resampler_i16_;
  std::unique_ptr<std::vector<ResamplerI32>> resampler_i32_;
  struct { unsigned seconds; int samples; } resampler_pos_;

  template <typename R, typename T> void Resample(
    std::vector<R>&, double, const std::vector<uint8_t>&,
    std::vector<const std::vector<uint8_t>*>*, uint32_t *);

  friend class LeAudioSourceAudioHalAsrcTest;
};

} // namespace le_audio
