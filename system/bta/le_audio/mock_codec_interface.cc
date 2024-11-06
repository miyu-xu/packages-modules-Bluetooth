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

#include "le_audio/codec_interface.h"

static MockCodecFactory* mock = nullptr;

void MockCodecFactory::SetMockInstanceForTesting(MockCodecFactory* codec_factory) {
  mock = codec_factory;
}

bluetooth::le_audio::CodecFactoryInterface& bluetooth::le_audio::CodecFactory::Get() {
  return *mock;
}
