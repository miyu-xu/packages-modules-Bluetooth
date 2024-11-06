/******************************************************************************
 *
 * Copyright (c) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

#pragma once

#include <optional>
#include <vector>

#include "codec_interface.h"

namespace bluetooth::le_audio {

class CodecLc3 : public CodecInterface {
public:
  CodecLc3();
  ~CodecLc3() override;

  CodecInterface::Status InitEncoder(const LeAudioCodecConfiguration& pcm_config,
                                     const LeAudioCodecConfiguration& codec_config) override;
  CodecInterface::Status InitDecoder(const LeAudioCodecConfiguration& codec_config,
                                     const LeAudioCodecConfiguration& pcm_config) override;
  CodecInterface::Status Encode(const uint8_t* data, int stride, uint16_t out_size,
                                std::vector<int16_t>* out_buffer = nullptr,
                                uint16_t out_offset = 0) override;
  CodecInterface::Status Decode(uint8_t* data, uint16_t size) override;
  void Cleanup() override;
  bool IsReady() override { return pcm_config_.has_value(); }
  uint16_t GetNumOfSamplesPerChannel() override;
  uint8_t GetNumOfBytesPerSample() override;
  std::vector<int16_t>& GetDecodedSamples() override { return output_channel_data_; }

private:
  inline void adjustOutputBufferSizeIfNeeded(std::vector<int16_t>* out_buffer) {
    if (out_buffer->size() < output_channel_samples_) {
      out_buffer->resize(output_channel_samples_);
    }
  }

  // BT codec params set when codec is initialized
  LeAudioCodecConfiguration bt_codec_config_;
  std::optional<LeAudioCodecConfiguration> pcm_config_;

  // Output buffer
  std::vector<int16_t> output_channel_data_;
  size_t output_channel_samples_ = 0;

  // Forward declaration of the opaque structure
  struct Impl;
  std::unique_ptr<Impl> pImpl_;
};

}  // namespace bluetooth::le_audio
