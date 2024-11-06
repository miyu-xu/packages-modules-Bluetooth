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

#include "codec_interface.h"

#include <bluetooth/log.h>

#include "codec_lc3.h"

namespace bluetooth::le_audio {

std::unique_ptr<CodecInterface> CodecFactory::Create(
        const bluetooth::le_audio::types::LeAudioCodecId& codec_id) {
  if (codec_id == set_configurations::LeAudioCodecIdLc3) {
    return std::unique_ptr<CodecInterface>(new CodecLc3());
  }

  log::error("Invalid codec ID: [{}:{}:{}]", codec_id.coding_format, codec_id.vendor_company_id,
             codec_id.vendor_codec_id);

  return nullptr;
}

CodecFactoryInterface& CodecFactory::Get() {
  static CodecFactory instance;
  return instance;
}

}  // namespace bluetooth::le_audio
