/*
 * Copyright 2024 The Android Open Source Project
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

#define LOG_TAG "AIDLProviderInfo"

#include "provider_info.h"

#include <android/binder_manager.h>
#include <android_bluetooth_flags.h>

#include <optional>

#include "client_interface_aidl.h"

namespace bluetooth::audio::aidl {
using ::aidl::android::hardware::bluetooth::audio::CodecId;
using ::aidl::android::hardware::bluetooth::audio::CodecInfo;
using ::aidl::android::hardware::bluetooth::audio::SessionType;

::hfp::sco_config recordHfpCodecInfo(CodecInfo codecInfo) {
  auto hfp_transport = codecInfo.transport.get<CodecInfo::Transport::hfp>();
  ::hfp::sco_config config{
      .inputDataPath = hfp_transport.inputDataPath,
      .outputDataPath = hfp_transport.outputDataPath,
      .useControllerCodec = hfp_transport.useControllerCodec,
  };
  return config;
}

std::unique_ptr<ProviderInfo>
bluetooth::audio::aidl::ProviderInfo::GetProviderInfo(SessionType sessionType) {
  auto provider_info =
      BluetoothAudioClientInterface::GetProviderInfo(sessionType, nullptr);

  std::vector<CodecInfo> codecInfos;
  if (provider_info.has_value()) {
    codecInfos = std::move(provider_info->codecInfos);
  }

  return std::make_unique<ProviderInfo>(std::move(codecInfos), sessionType);
}

ProviderInfo::ProviderInfo(std::vector<CodecInfo> codecs,
                           SessionType sessionType)
    : codecInfos(std::move(codecs)) {
  for (auto codecInfo : codecInfos) {
    switch (codecInfo.id.getTag()) {
      case CodecId::core: {
        switch (codecInfo.id.get<CodecId::core>()) {
          case CodecId::Core::CVSD: {
            hfpScoConfigMap[UUID_CODEC_CVSD] = recordHfpCodecInfo(codecInfo);
            break;
          }
          case CodecId::Core::MSBC: {
            hfpScoConfigMap[UUID_CODEC_MSBC] = recordHfpCodecInfo(codecInfo);
            break;
          }
          case CodecId::Core::LC3: {
            if (sessionType == SessionType::HFP_HARDWARE_OFFLOAD_DATAPATH ||
                sessionType == SessionType::HFP_SOFTWARE_ENCODING_DATAPATH ||
                sessionType == SessionType::HFP_SOFTWARE_DECODING_DATAPATH) {
              hfpScoConfigMap[UUID_CODEC_LC3] = recordHfpCodecInfo(codecInfo);
            }
            break;
          }
        }
        break;
      }
      default:
        break;
    }
  }
}

const std::unordered_map<int, ::hfp::sco_config>&
ProviderInfo::GetHfpScoConfig() {
  return hfpScoConfigMap;
}
}  // namespace bluetooth::audio::aidl
