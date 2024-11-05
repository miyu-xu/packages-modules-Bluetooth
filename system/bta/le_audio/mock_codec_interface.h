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

#pragma once

#include <gmock/gmock.h>

#include <vector>

#include "codec_interface.h"

class MockCodecInterface : public bluetooth::le_audio::CodecInterface {
public:
  MOCK_METHOD((bluetooth::le_audio::CodecInterface::Status), InitEncoder,
              (const bluetooth::le_audio::LeAudioCodecConfiguration& pcm_config,
               const bluetooth::le_audio::LeAudioCodecConfiguration& codec_config),
              (override));
  MOCK_METHOD((bluetooth::le_audio::CodecInterface::Status), InitDecoder,
              (const bluetooth::le_audio::LeAudioCodecConfiguration& codec_config,
               const bluetooth::le_audio::LeAudioCodecConfiguration& pcm_config),
              (override));
  MOCK_METHOD((bluetooth::le_audio::CodecInterface::Status), Encode,
              (const uint8_t* data, int stride, uint16_t out_size, std::vector<uint8_t>* out_buffer,
               uint16_t out_offset),
              (override));
  MOCK_METHOD((bluetooth::le_audio::CodecInterface::Status), Decode,
              (uint8_t * data, uint16_t size), (override));
  MOCK_METHOD((void), Cleanup, (), (override));
  MOCK_METHOD((bool), IsReady, (), (override));
  MOCK_METHOD((uint16_t), GetNumOfSamplesPerChannel, (), (override));
  MOCK_METHOD((uint8_t), GetNumOfBytesPerSample, (), (override));
  MOCK_METHOD((std::vector<int16_t>&), GetDecodedSamples, (), (override));
  MOCK_METHOD((std::vector<uint8_t>&), GetEncodedData, (), (override));
};

class MockCodecFactory : public bluetooth::le_audio::CodecFactoryInterface {
public:
  MOCK_METHOD(std::unique_ptr<bluetooth::le_audio::CodecInterface>, Create,
              (const bluetooth::le_audio::types::LeAudioCodecId& codec_id), (override));

  static void SetMockInstanceForTesting(MockCodecFactory* codec_factory);
};
