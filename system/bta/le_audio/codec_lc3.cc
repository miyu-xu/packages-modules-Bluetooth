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

#include "codec_lc3.h"

#include <bluetooth/log.h>
#include <lc3.h>

#include <memory>
#include <optional>
#include <vector>

namespace bluetooth::le_audio {

// LC3 Codec specific structure
struct CodecLc3::Impl {
  static inline uint8_t bits_to_bytes_per_sample(uint8_t bits_per_sample) {
    // 24-bit audio stream is sent as unpacked, each sample takes 4 bytes.
    if (bits_per_sample == 24) {
      return 4;
    }
    return bits_per_sample / 8;
  }

  void Cleanup() {
    decoder_ = nullptr;
    encoder_ = nullptr;
    codec_mem_.reset();
  }

  Impl() : codec_mem_(nullptr, &std::free) {}
  lc3_pcm_format pcm_format_;
  union {
    lc3_decoder_t decoder_;
    lc3_encoder_t encoder_;
  };
  std::unique_ptr<void, decltype(&std::free)> codec_mem_;
};

CodecLc3::CodecLc3() : pImpl_(std::make_unique<Impl>()) {}

CodecLc3::~CodecLc3() { Cleanup(); }

CodecInterface::Status CodecLc3::InitEncoder(const LeAudioCodecConfiguration& pcm_config,
                                             const LeAudioCodecConfiguration& codec_config) {
  // Output codec configuration
  bt_codec_config_ = codec_config;

  // TODO: For now only blocks_per_sdu = 1 is supported
  if (pcm_config_.has_value()) {
    Cleanup();
  }
  pcm_config_ = pcm_config;

  pImpl_->pcm_format_ =
          (pcm_config_->bits_per_sample == 24) ? LC3_PCM_FORMAT_S24 : LC3_PCM_FORMAT_S16;

  // Prepare the encoder
  const auto encoder_size =
          lc3_encoder_size(bt_codec_config_.data_interval_us, pcm_config_->sample_rate);
  pImpl_->codec_mem_.reset(malloc(encoder_size));
  pImpl_->encoder_ =
          lc3_setup_encoder(bt_codec_config_.data_interval_us, bt_codec_config_.sample_rate,
                            pcm_config_->sample_rate, pImpl_->codec_mem_.get());

  return CodecInterface::Status::STATUS_OK;
}

CodecInterface::Status CodecLc3::InitDecoder(const LeAudioCodecConfiguration& codec_config,
                                             const LeAudioCodecConfiguration& pcm_config) {
  // Input codec configuration
  bt_codec_config_ = codec_config;

  // TODO: For now only blocks_per_sdu = 1 is supported
  if (pcm_config_.has_value()) {
    Cleanup();
  }
  pcm_config_ = pcm_config;

  pImpl_->pcm_format_ =
          (pcm_config_->bits_per_sample == 24) ? LC3_PCM_FORMAT_S24 : LC3_PCM_FORMAT_S16;

  // Prepare the decoded output buffer
  output_channel_samples_ =
          lc3_frame_samples(bt_codec_config_.data_interval_us, pcm_config_->sample_rate);
  adjustOutputBufferSizeIfNeeded(&output_channel_data_);

  // Prepare the decoder
  const auto decoder_size =
          lc3_decoder_size(bt_codec_config_.data_interval_us, pcm_config_->sample_rate);
  pImpl_->codec_mem_.reset(malloc(decoder_size));
  pImpl_->decoder_ =
          lc3_setup_decoder(bt_codec_config_.data_interval_us, bt_codec_config_.sample_rate,
                            pcm_config_->sample_rate, pImpl_->codec_mem_.get());

  return CodecInterface::Status::STATUS_OK;
}

CodecInterface::Status CodecLc3::Decode(uint8_t* data, uint16_t size) {
  if (!IsReady()) {
    log::error("decoder not ready");
    return CodecInterface::Status::STATUS_ERR_CODEC_NOT_READY;
  }

  // For now only LC3 is supported
  adjustOutputBufferSizeIfNeeded(&output_channel_data_);
  auto err = lc3_decode(pImpl_->decoder_, data, size, pImpl_->pcm_format_,
                        output_channel_data_.data(), 1 /* stride */);
  if (err < 0) {
    log::error("bad decoding parameters: {}", static_cast<int>(err));
    return CodecInterface::Status::STATUS_ERR_CODING_ERROR;
  }

  return CodecInterface::Status::STATUS_OK;
}

CodecInterface::Status CodecLc3::Encode(const uint8_t* data, int stride, uint16_t out_size,
                                        std::vector<int16_t>* out_buffer, uint16_t out_offset) {
  if (!IsReady()) {
    log::error("decoder not ready");
    return CodecInterface::Status::STATUS_ERR_CODEC_NOT_READY;
  }

  if (out_size == 0) {
    log::error("out_size cannot be 0");
    return CodecInterface::Status::STATUS_ERR_CODING_ERROR;
  }

  // Prepare the encoded output buffer
  if (out_buffer == nullptr) {
    out_buffer = &output_channel_data_;
  }

  // We have two bytes per sample in the buffer, while out_size and
  // out_offset are in bytes
  size_t channel_samples = (out_offset + out_size) / 2;
  if (output_channel_samples_ < channel_samples) {
    output_channel_samples_ = channel_samples;
  }
  adjustOutputBufferSizeIfNeeded(out_buffer);

  // Encode
  auto err = lc3_encode(pImpl_->encoder_, pImpl_->pcm_format_, data, stride, out_size,
                        ((uint8_t*)out_buffer->data()) + out_offset);
  if (err < 0) {
    log::error("bad encoding parameters: {}", static_cast<int>(err));
    return CodecInterface::Status::STATUS_ERR_CODING_ERROR;
  }

  return CodecInterface::Status::STATUS_OK;
}

void CodecLc3::Cleanup() {
  pcm_config_ = std::nullopt;
  pImpl_->Cleanup();
  output_channel_data_.clear();
  output_channel_samples_ = 0;
}

uint16_t CodecLc3::GetNumOfSamplesPerChannel() {
  if (!IsReady()) {
    log::error("decoder not ready");
    return 0;
  }

  return lc3_frame_samples(bt_codec_config_.data_interval_us, pcm_config_->sample_rate);
}

uint8_t CodecLc3::GetNumOfBytesPerSample() {
  return pImpl_->bits_to_bytes_per_sample(bt_codec_config_.bits_per_sample);
}

}  // namespace bluetooth::le_audio
