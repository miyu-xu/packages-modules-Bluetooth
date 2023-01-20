/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "mock_codec_interface.h"

namespace le_audio {

struct CodecInterface::Impl : public MockCodecInterface {
 public:
  Impl(const types::LeAudioCodecId& codec_id) {
    output_channel_data_.resize(1);
  };
  ~Impl() = default;

  std::vector<int16_t>& getDecodedSamples() { return output_channel_data_; }
  std::vector<int16_t> output_channel_data_;
};

CodecInterface::CodecInterface(const types::LeAudioCodecId& codec_id) {
  impl = new Impl(codec_id);
}
CodecInterface::~CodecInterface() { delete impl; }
bool CodecInterface::isReady() { return impl->isReady(); };
CodecInterface::Status CodecInterface::initEncoder(
    const LeAudioCodecConfiguration& pcm_config,
    const LeAudioCodecConfiguration& codec_config) {
  return impl->initEncoder(pcm_config, codec_config);
}
CodecInterface::Status CodecInterface::initDecoder(
    const LeAudioCodecConfiguration& codec_config,
    const LeAudioCodecConfiguration& pcm_config) {
  return impl->initDecoder(codec_config, pcm_config);
}
std::vector<int16_t>& CodecInterface::getDecodedSamples() {
  return impl->getDecodedSamples();
}
CodecInterface::Status CodecInterface::decode(uint8_t* data, uint16_t size) {
  return impl->decode(data, size);
}
CodecInterface::Status CodecInterface::encode(const uint8_t* data, int stride,
                                              uint16_t out_size,
                                              std::vector<int16_t>* out_buffer,
                                              uint16_t out_offset) {
  return impl->encode(data, stride, out_size, out_buffer, out_offset);
}
void CodecInterface::cleanup() { return impl->cleanup(); }

uint16_t CodecInterface::getNumOfSamplesPerChannel() {
  return impl->getNumOfSamplesPerChannel();
};
uint8_t CodecInterface::getNumOfBytesPerSample() {
  return impl->getNumOfBytesPerSample();
};
}  // namespace le_audio
