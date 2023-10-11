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

#define LOG_TAG "AIDLProviderInfo"

#include "provider_info.h"

#include <android/binder_manager.h>

#include <optional>

#include "a2dp_codec_api.h"
#include "a2dp_constants.h"
#include "a2dp_vendor.h"
#include "a2dp_vendor_aptx_constants.h"
#include "a2dp_vendor_aptx_hd_constants.h"
#include "a2dp_vendor_ldac_constants.h"
#include "a2dp_vendor_opus_constants.h"
#include "client_interface_aidl.h"
#include "osi/include/log.h"

namespace bluetooth::audio::aidl::a2dp {

using ::aidl::android::hardware::bluetooth::audio::CodecId;
using ::aidl::android::hardware::bluetooth::audio::CodecInfo;
using ::aidl::android::hardware::bluetooth::audio::
    IBluetoothAudioProviderFactory;
using ::aidl::android::hardware::bluetooth::audio::SessionType;

/***
 * Reads the provider information from the HAL.
 * May return nullptr if the HAL does not implement
 * getProviderInfo, or if the feature flag for codec
 * extensibility is disabled.
 ***/
ProviderInfo* ProviderInfo::GetProviderInfo() {
  if (false /*XXX codec_ext_enabled*/) {
    return nullptr;
  }

  auto source_provider_info = BluetoothAudioClientInterface::GetProviderInfo(
      SessionType::A2DP_HARDWARE_OFFLOAD_ENCODING_DATAPATH);

  auto sink_provider_info = BluetoothAudioClientInterface::GetProviderInfo(
      SessionType::A2DP_HARDWARE_OFFLOAD_DECODING_DATAPATH);

  if (!source_provider_info.has_value() && !sink_provider_info.has_value()) {
    LOG(INFO) << __func__ << ": no provider info reported";
    return nullptr;
  }

  std::vector<CodecInfo> source_codecs;
  std::vector<CodecInfo> sink_codecs;

  if (source_provider_info.has_value()) {
    source_codecs = std::move(source_provider_info->codecInfos);
  }

  if (sink_provider_info.has_value()) {
    sink_codecs = std::move(sink_provider_info->codecInfos);
  }

  return new ProviderInfo(source_codecs, sink_codecs);
}

/***
 * Return the assigned source codec index if the codec
 * matches a known codec, or pick a new codec index starting from
 * ext_index.
 ***/
static std::optional<btav_a2dp_codec_index_t> assignSourceCodecIndex(
    CodecInfo const& codec, btav_a2dp_codec_index_t* ext_index) {
  switch (codec.id.getTag()) {
    case CodecId::core:
    case CodecId::undef:
    default:
      return std::nullopt;
    case CodecId::a2dp:
      switch (codec.id.get<CodecId::a2dp>()) {
        case CodecId::A2dp::SBC:
          return BTAV_A2DP_CODEC_INDEX_SOURCE_SBC;
        case CodecId::A2dp::AAC:
          return BTAV_A2DP_CODEC_INDEX_SOURCE_AAC;
        default:
          return std::nullopt;
      }
      break;
    case CodecId::vendor: {
      int vendor_id = codec.id.get<CodecId::vendor>().id;
      int codec_id = codec.id.get<CodecId::vendor>().codecId;

      /* match know vendor codecs */
      if (vendor_id == A2DP_APTX_VENDOR_ID &&
          codec_id == A2DP_APTX_CODEC_ID_BLUETOOTH) {
        return BTAV_A2DP_CODEC_INDEX_SOURCE_APTX;
      }
      if (vendor_id == A2DP_APTX_HD_VENDOR_ID &&
          codec_id == A2DP_APTX_HD_CODEC_ID_BLUETOOTH) {
        return BTAV_A2DP_CODEC_INDEX_SOURCE_APTX_HD;
      }
      if (vendor_id == A2DP_LDAC_VENDOR_ID && codec_id == A2DP_LDAC_CODEC_ID) {
        return BTAV_A2DP_CODEC_INDEX_SOURCE_LDAC;
      }
      if (vendor_id == A2DP_OPUS_VENDOR_ID && codec_id == A2DP_OPUS_CODEC_ID) {
        return BTAV_A2DP_CODEC_INDEX_SOURCE_OPUS;
      }

      /* out of extension codec indexes */
      if (*ext_index >= BTAV_A2DP_CODEC_INDEX_SOURCE_EXT_MAX) {
        LOG(ERROR) << "unable to assign a source codec index for vendorId="
                   << vendor_id << ", codecId=" << codec_id;
      }

      /* assign a new codec index for the
         unknown vendor codec */
      return *(ext_index++);
    }
  }
}

/***
 * Return the assigned source codec index if the codec
 * matches a known codec, or pick a new codec index starting from
 * ext_index.
 ***/
static std::optional<btav_a2dp_codec_index_t> assignSinkCodecIndex(
    CodecInfo const& codec, btav_a2dp_codec_index_t* ext_index) {
  switch (codec.id.getTag()) {
    case CodecId::core:
    case CodecId::undef:
    default:
      return std::nullopt;
    case CodecId::a2dp:
      switch (codec.id.get<CodecId::a2dp>()) {
        case CodecId::A2dp::SBC:
          return BTAV_A2DP_CODEC_INDEX_SINK_SBC;
        case CodecId::A2dp::AAC:
          return BTAV_A2DP_CODEC_INDEX_SINK_AAC;
        default:
          return std::nullopt;
      }
      break;
    case CodecId::vendor: {
      int vendor_id = codec.id.get<CodecId::vendor>().id;
      int codec_id = codec.id.get<CodecId::vendor>().codecId;

      /* match know vendor codecs */
      if (vendor_id == A2DP_LDAC_VENDOR_ID && codec_id == A2DP_LDAC_CODEC_ID) {
        return BTAV_A2DP_CODEC_INDEX_SINK_LDAC;
      }
      if (vendor_id == A2DP_OPUS_VENDOR_ID && codec_id == A2DP_OPUS_CODEC_ID) {
        return BTAV_A2DP_CODEC_INDEX_SINK_OPUS;
      }

      /* out of extension codec indexes */
      if (*ext_index >= BTAV_A2DP_CODEC_INDEX_SINK_EXT_MAX) {
        LOG(ERROR) << "unable to assign a sink codec index for vendorId="
                   << vendor_id << ", codecId=" << codec_id;
      }

      /* assign a new codec index for the
         unknown vendor codec */
      return *(ext_index++);
    }
  }
}

ProviderInfo::ProviderInfo(std::vector<CodecInfo> source_codecs,
                           std::vector<CodecInfo> sink_codecs)
    : source_codecs(std::move(source_codecs)),
      sink_codecs(std::move(sink_codecs)) {
  btav_a2dp_codec_index_t ext_source_index =
      BTAV_A2DP_CODEC_INDEX_SOURCE_EXT_MIN;
  for (auto& codec : source_codecs) {
    auto index = assignSourceCodecIndex(codec, &ext_source_index);
    if (index.has_value()) {
      assigned_source_codec_indexes.insert({index.value(), &codec});
    }
  }

  btav_a2dp_codec_index_t ext_sink_index = BTAV_A2DP_CODEC_INDEX_SINK_EXT_MIN;
  for (auto& codec : source_codecs) {
    auto index = assignSinkCodecIndex(codec, &ext_sink_index);
    if (index.has_value()) {
      assigned_sink_codec_indexes.insert({index.value(), &codec});
    }
  }
}

std::optional<btav_a2dp_codec_index_t> ProviderInfo::SourceCodecIndex(
    uint32_t vendor_id, uint16_t codec_id) const {
  for (auto const& [index, codec] : assigned_source_codec_indexes) {
    if (codec->id.getTag() == CodecId::vendor &&
        codec->id.get<CodecId::vendor>().id == (int)vendor_id &&
        codec->id.get<CodecId::vendor>().codecId == codec_id) {
      return index;
    }
  }
  return std::nullopt;
}

std::optional<btav_a2dp_codec_index_t> ProviderInfo::SourceCodecIndex(
    uint8_t const* codec_info) const {
  if (A2DP_GetCodecType(codec_info) != A2DP_MEDIA_CT_NON_A2DP) {
    // TODO(henrichataing): would be required if a vendor decided
    // to implement a standard codec other than SBC, AAC.
    return std::nullopt;
  }

  uint32_t vendor_id = A2DP_VendorCodecGetVendorId(codec_info);
  uint16_t codec_id = A2DP_VendorCodecGetCodecId(codec_info);
  return SourceCodecIndex(vendor_id, codec_id);
}

std::optional<btav_a2dp_codec_index_t> ProviderInfo::SinkCodecIndex(
    uint32_t vendor_id, uint16_t codec_id) const {
  for (auto const& [index, codec] : assigned_sink_codec_indexes) {
    if (codec->id.getTag() == CodecId::vendor &&
        codec->id.get<CodecId::vendor>().id == (int)vendor_id &&
        codec->id.get<CodecId::vendor>().codecId == codec_id) {
      return index;
    }
  }
  return std::nullopt;
}

std::optional<btav_a2dp_codec_index_t> ProviderInfo::SinkCodecIndex(
    uint8_t const* codec_info) const {
  if (A2DP_GetCodecType(codec_info) != A2DP_MEDIA_CT_NON_A2DP) {
    // TODO(henrichataing): would be required if a vendor decided
    // to implement a standard codec other than SBC, AAC.
    return std::nullopt;
  }

  uint32_t vendor_id = A2DP_VendorCodecGetVendorId(codec_info);
  uint16_t codec_id = A2DP_VendorCodecGetCodecId(codec_info);
  return SinkCodecIndex(vendor_id, codec_id);
}

std::optional<const char*> ProviderInfo::CodecIndexStr(
    btav_a2dp_codec_index_t codec_index) const {
  if (codec_index >= BTAV_A2DP_CODEC_INDEX_SOURCE_MIN &&
      codec_index < BTAV_A2DP_CODEC_INDEX_SOURCE_MAX) {
    auto it = assigned_source_codec_indexes.find(codec_index);
    return it != assigned_source_codec_indexes.end()
               ? std::make_optional(it->second->name.c_str())
               : std::nullopt;
  }
  if (codec_index >= BTAV_A2DP_CODEC_INDEX_SINK_MIN &&
      codec_index < BTAV_A2DP_CODEC_INDEX_SINK_MAX) {
    auto it = assigned_sink_codec_indexes.find(codec_index);
    return it != assigned_sink_codec_indexes.end()
               ? std::make_optional(it->second->name.c_str())
               : std::nullopt;
  }
  return std::nullopt;
}

bool ProviderInfo::SupportsCodec(btav_a2dp_codec_index_t codec_index) const {
  if (codec_index >= BTAV_A2DP_CODEC_INDEX_SOURCE_MIN &&
      codec_index < BTAV_A2DP_CODEC_INDEX_SOURCE_MAX) {
    return assigned_source_codec_indexes.find(codec_index) !=
      assigned_source_codec_indexes.end();
  }
  if (codec_index >= BTAV_A2DP_CODEC_INDEX_SINK_MIN &&
      codec_index < BTAV_A2DP_CODEC_INDEX_SINK_MAX) {
    return assigned_sink_codec_indexes.find(codec_index) !=
      assigned_sink_codec_indexes.end();
  }
  return false;
}

}  // namespace bluetooth::audio::aidl::a2dp
