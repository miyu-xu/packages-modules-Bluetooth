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
  // An initial buffering is proposed, during this startup period, the stream
  // is internally buffered, before being returned in a whole to generate a burst
  // and fulfill the audio pipeline buffering.

  LeAudioSourceAudioHalAsrc(
    int channels, int samplerate, int bitdepth, int interval_us,
    int initial_buffering_ms = 50);

  ~LeAudioSourceAudioHalAsrc();

  // Gives an input buffer, and return a list of resamples buffers locked to
  // the cadence of the transmission. The input and output buffers have a fixed
  // size, deducted from the PCM characteristics, given to the constructor.

  std::vector<const std::vector<uint8_t> *> Run(const std::vector<uint8_t>& in);

 private:

  const unsigned samplerate_;
  const unsigned bitdepth_;
  const unsigned interval_us_;

  size_t buffers_size_;
  struct {
    std::vector<std::vector<uint8_t>> pool;
    int initial_buffering;
    int index, offset;
  } buffers_;

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
  struct { unsigned seconds, samples; } resampler_pos_;

  template <typename R, typename T>
  std::vector<const std::vector<uint8_t>*> Resample(
    std::vector<R>&, double, const std::vector<uint8_t>&, uint32_t *);

  friend class ResamplerTest;
};

} // namespace le_audio
