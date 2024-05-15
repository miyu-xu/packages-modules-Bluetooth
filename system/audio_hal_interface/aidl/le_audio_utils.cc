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

#include "le_audio_utils.h"

#include <optional>

namespace bluetooth {
namespace audio {
namespace aidl {

::aidl::android::hardware::bluetooth::audio::CodecId
GetAidlCodecIdFromStackFormat(
    const ::bluetooth::le_audio::types::LeAudioCodecId& codec_id) {
  ::aidl::android::hardware::bluetooth::audio::CodecId codec;

  if (codec_id.coding_format == 0x06) {
    codec = ::aidl::android::hardware::bluetooth::audio::CodecId::Core::LC3;
  } else if (codec_id.coding_format == 0x02) {
    codec = ::aidl::android::hardware::bluetooth::audio::CodecId::Core::CVSD;
  } else if (codec_id.coding_format == 0x05) {
    codec = ::aidl::android::hardware::bluetooth::audio::CodecId::Core::MSBC;
  } else if (codec_id.coding_format == 0xFF) {
    auto vc = ::aidl::android::hardware::bluetooth::audio::CodecId::Vendor();
    vc.id = codec_id.vendor_company_id;
    vc.codecId = codec_id.vendor_codec_id;
    codec = vc;
  } else {
    LOG(ERROR) << __func__ << "Invalid coding format: " << std::hex
               << codec_id.coding_format;
  }
  return codec;
}

::bluetooth::le_audio::types::LeAudioCodecId GetStackCodecIdFromAidlFormat(
    const ::aidl::android::hardware::bluetooth::audio::CodecId& codec_id) {
  ::bluetooth::le_audio::types::LeAudioCodecId codec;
  switch (codec_id.getTag()) {
    case ::aidl::android::hardware::bluetooth::audio::CodecId::core: {
      codec.vendor_codec_id = 0x00;
      codec.vendor_company_id = 0x00;

      if (codec_id ==
          ::aidl::android::hardware::bluetooth::audio::CodecId::Core::LC3) {
        codec.coding_format = 0x06;
      } else if (codec_id == ::aidl::android::hardware::bluetooth::audio::
                                 CodecId::Core::CVSD) {
        codec.coding_format = 0x02;
      } else if (codec_id == ::aidl::android::hardware::bluetooth::audio::
                                 CodecId::Core::MSBC) {
        codec.coding_format = 0x05;
      }
    } break;
    case ::aidl::android::hardware::bluetooth::audio::CodecId::vendor: {
      auto vendor = codec_id.get<
          ::aidl::android::hardware::bluetooth::audio::CodecId::vendor>();
      codec.coding_format = 0xFF;
      codec.vendor_company_id = vendor.id;
      codec.vendor_codec_id = vendor.codecId;
    } break;
    case ::aidl::android::hardware::bluetooth::audio::CodecId::a2dp:
      LOG(ERROR) << __func__ << "A2DP codecs not supported here";
      break;
    default:
      break;
  }
  return codec;
}

std::vector<
    ::aidl::android::hardware::bluetooth::audio::CodecSpecificCapabilitiesLtv>
GetAidlCodecCapabilitiesFromStack(
    const ::bluetooth::le_audio::types::LeAudioLtvMap& in) {
  std::vector<
      ::aidl::android::hardware::bluetooth::audio::CodecSpecificCapabilitiesLtv>
      ltvs;
  auto stack_caps = in.GetAsCoreCodecCapabilities();

  if (stack_caps.supported_sampling_frequencies) {
    /* Note: The values match precisely */
    ::aidl::android::hardware::bluetooth::audio::CodecSpecificCapabilitiesLtv::
        SupportedSamplingFrequencies freqs{
            .bitmask = *stack_caps.supported_sampling_frequencies};
    if (freqs.bitmask) ltvs.push_back(freqs);
  }

  if (stack_caps.supported_frame_durations) {
    /* Note: The values match precisely */
    ::aidl::android::hardware::bluetooth::audio::CodecSpecificCapabilitiesLtv::
        SupportedFrameDurations durations{
            .bitmask = *stack_caps.supported_frame_durations};
    if (durations.bitmask) ltvs.push_back(durations);
  }

  if (stack_caps.supported_audio_channel_counts) {
    /* Note: The values match precisely */
    ::aidl::android::hardware::bluetooth::audio::CodecSpecificCapabilitiesLtv::
        SupportedAudioChannelCounts counts{
            .bitmask = *stack_caps.supported_audio_channel_counts};
    if (counts.bitmask) ltvs.push_back(counts);
  }

  if (stack_caps.supported_min_octets_per_codec_frame &&
      stack_caps.supported_max_octets_per_codec_frame) {
    ::aidl::android::hardware::bluetooth::audio::CodecSpecificCapabilitiesLtv::
        SupportedOctetsPerCodecFrame octets_per_frame{
            .min = *stack_caps.supported_min_octets_per_codec_frame,
            .max = *stack_caps.supported_max_octets_per_codec_frame,
        };
    ltvs.push_back(octets_per_frame);
  }

  if (stack_caps.supported_max_codec_frames_per_sdu) {
    ::aidl::android::hardware::bluetooth::audio::CodecSpecificCapabilitiesLtv::
        SupportedMaxCodecFramesPerSDU codec_frames{
            .value = *stack_caps.supported_max_codec_frames_per_sdu};
    ltvs.push_back(codec_frames);
  }

  return ltvs;
}

std::optional<std::vector<
    std::optional<::aidl::android::hardware::bluetooth::audio::MetadataLtv>>>
GetAidlMetadataFromStackFormat(const std::vector<uint8_t>& vec) {
  if (vec.empty()) return std::nullopt;
  std::vector<
      std::optional<::aidl::android::hardware::bluetooth::audio::MetadataLtv>>
      out_ltvs;

  auto ltvs = ::bluetooth::le_audio::types::LeAudioLtvMap();
  if (ltvs.Parse(vec.data(), vec.size())) {
    auto stackMetadata = ltvs.GetAsLeAudioMetadata();

    if (stackMetadata.preferred_audio_context) {
      out_ltvs.push_back(
          ::aidl::android::hardware::bluetooth::audio::MetadataLtv::
              PreferredAudioContexts{
                  .values =
                      ::aidl::android::hardware::bluetooth::audio::AudioContext{
                          .bitmask =
                              stackMetadata.preferred_audio_context.value()}});
    }
    if (stackMetadata.streaming_audio_context) {
      out_ltvs.push_back(
          ::aidl::android::hardware::bluetooth::audio::MetadataLtv::
              StreamingAudioContexts{
                  .values =
                      ::aidl::android::hardware::bluetooth::audio::AudioContext{
                          .bitmask =
                              stackMetadata.streaming_audio_context.value()}});
    }
    if (stackMetadata.vendor_specific) {
      if (stackMetadata.vendor_specific->size() >= 2) {
        out_ltvs.push_back(
            ::aidl::android::hardware::bluetooth::audio::MetadataLtv::
                VendorSpecific{/* Two octets for the company identifier */
                               stackMetadata.vendor_specific->at(0) |
                                   (stackMetadata.vendor_specific->at(1) << 8),
                               /* The rest is a payload */
                               .opaqueValue = std::vector<uint8_t>(
                                   stackMetadata.vendor_specific->begin() + 2,
                                   stackMetadata.vendor_specific->end())});
      }
    }
    /* Note: stackMetadata.program_info
     *       stackMetadata.language
     *       stackMetadata.ccid_list
     *       stackMetadata.parental_rating
     *       stackMetadata.program_info_uri
     *       stackMetadata.extended_metadata
     *       stackMetadata.audio_active_state
     *       stackMetadata.broadcast_audio_immediate_rendering
     *       are not sent over the AIDL interface as they are considered as
     *       irrelevant for the configuration process.
     */
  }
  return out_ltvs;
}

bluetooth::le_audio::types::LeAudioLtvMap GetStackMetadataFromAidlFormat(
    const std::vector<std::optional<
        ::aidl::android::hardware::bluetooth::audio::MetadataLtv>>& source) {
  bluetooth::le_audio::types::LeAudioLtvMap cfg;
  (void)source;
  for (auto const& entry : source) {
    if (!entry.has_value()) continue;

    if (entry->getTag() == ::aidl::android::hardware::bluetooth::audio::
                               MetadataLtv::preferredAudioContexts) {
      auto aidl_contexts =
          entry->get<::aidl::android::hardware::bluetooth::audio::MetadataLtv::
                         preferredAudioContexts>();
      cfg.Add(
          bluetooth::le_audio::types::kLeAudioMetadataTypePreferredAudioContext,
          (uint16_t)aidl_contexts.values.bitmask);

    } else if (entry->getTag() == ::aidl::android::hardware::bluetooth::audio::
                                      MetadataLtv::streamingAudioContexts) {
      auto aidl_contexts =
          entry->get<::aidl::android::hardware::bluetooth::audio::MetadataLtv::
                         streamingAudioContexts>();
      cfg.Add(
          bluetooth::le_audio::types::kLeAudioMetadataTypeStreamingAudioContext,
          (uint16_t)aidl_contexts.values.bitmask);

    } else if (entry->getTag() == ::aidl::android::hardware::bluetooth::audio::
                                      MetadataLtv::vendorSpecific) {
      auto aidl_vendor_data =
          entry->get<::aidl::android::hardware::bluetooth::audio::MetadataLtv::
                         vendorSpecific>();
      cfg.Add(bluetooth::le_audio::types::kLeAudioMetadataTypeVendorSpecific,
              aidl_vendor_data.companyId, aidl_vendor_data.opaqueValue);
    }
  }
  return cfg;
}

std::optional<std::vector<
    std::optional<::aidl::android::hardware::bluetooth::audio::
                      IBluetoothAudioProvider::LeAudioDeviceCapabilities>>>
GetAidlLeAudioDeviceCapabilitiesFromStackFormat(
    std::optional<
        const ::bluetooth::le_audio::types::PublishedAudioCapabilities*>
        pacs) {
  std::vector<
      std::optional<::aidl::android::hardware::bluetooth::audio::
                        IBluetoothAudioProvider::LeAudioDeviceCapabilities>>
      caps;

  if (pacs) {
    for (auto const& group_rec : **pacs) {
      LOG(INFO) << __func__ << "inside pacs";
      /* key tuple (att handles) are not important here */
      for (auto const& rec : std::get<1>(group_rec)) {
        ::aidl::android::hardware::bluetooth::audio::IBluetoothAudioProvider::
            LeAudioDeviceCapabilities cap;
        cap.codecId = GetAidlCodecIdFromStackFormat(rec.codec_id);

        cap.codecSpecificCapabilities =
            GetAidlCodecCapabilitiesFromStack(rec.codec_spec_caps);

        cap.vendorCodecSpecificCapabilities =
            (rec.codec_spec_caps_raw.empty()
                 ? std::nullopt
                 : std::optional<std::vector<uint8_t>>(
                       rec.codec_spec_caps_raw));
        cap.metadata = GetAidlMetadataFromStackFormat(rec.metadata);

        LOG(INFO) << __func__ << "adding capa record";
        caps.push_back(cap);
      }
    }
  }
  return (caps.empty()
              ? std::nullopt
              : std::optional<std::vector<std::optional<
                    ::aidl::android::hardware::bluetooth::audio::
                        IBluetoothAudioProvider::LeAudioDeviceCapabilities>>>(
                    caps));
}

::bluetooth::le_audio::types::LeAudioLtvMap GetStackLeAudioLtvMapFromAidlFormat(
    const std::vector<::aidl::android::hardware::bluetooth::audio::
                          CodecSpecificConfigurationLtv>& aidl_config_ltvs) {
  ::bluetooth::le_audio::types::LeAudioLtvMap stack_ltv;
  for (auto const& ltv : aidl_config_ltvs) {
    switch (ltv.getTag()) {
      case ::aidl::android::hardware::bluetooth::audio::
          CodecSpecificConfigurationLtv::Tag::codecFrameBlocksPerSDU:
        stack_ltv.Add(::bluetooth::le_audio::codec_spec_conf::
                          kLeAudioLtvTypeCodecFrameBlocksPerSdu,
                      (uint8_t)ltv
                          .get<::aidl::android::hardware::bluetooth::audio::
                                   CodecSpecificConfigurationLtv::Tag::
                                       codecFrameBlocksPerSDU>()
                          .value);
        break;
      case ::aidl::android::hardware::bluetooth::audio::
          CodecSpecificConfigurationLtv::Tag::samplingFrequency:
        stack_ltv.Add(
            ::bluetooth::le_audio::codec_spec_conf::kLeAudioLtvTypeSamplingFreq,
            (uint8_t)ltv.get<
                ::aidl::android::hardware::bluetooth::audio::
                    CodecSpecificConfigurationLtv::Tag::samplingFrequency>());
        break;
      case ::aidl::android::hardware::bluetooth::audio::
          CodecSpecificConfigurationLtv::Tag::frameDuration:
        stack_ltv.Add(
            ::bluetooth::le_audio::codec_spec_conf::
                kLeAudioLtvTypeFrameDuration,
            (uint8_t)ltv
                .get<::aidl::android::hardware::bluetooth::audio::
                         CodecSpecificConfigurationLtv::Tag::frameDuration>());
        break;
      case ::aidl::android::hardware::bluetooth::audio::
          CodecSpecificConfigurationLtv::Tag::audioChannelAllocation:
        stack_ltv.Add(::bluetooth::le_audio::codec_spec_conf::
                          kLeAudioLtvTypeAudioChannelAllocation,
                      (uint32_t)ltv
                          .get<::aidl::android::hardware::bluetooth::audio::
                                   CodecSpecificConfigurationLtv::Tag::
                                       audioChannelAllocation>()
                          .bitmask);
        break;
      case ::aidl::android::hardware::bluetooth::audio::
          CodecSpecificConfigurationLtv::Tag::octetsPerCodecFrame:
        stack_ltv.Add(::bluetooth::le_audio::codec_spec_conf::
                          kLeAudioLtvTypeOctetsPerCodecFrame,
                      (uint16_t)ltv
                          .get<::aidl::android::hardware::bluetooth::audio::
                                   CodecSpecificConfigurationLtv::Tag::
                                       octetsPerCodecFrame>()
                          .value);
        break;
      default:
        break;
    }
  }
  return stack_ltv;
}

::bluetooth::le_audio::broadcaster::BroadcastSubgroupBisCodecConfig
GetStackBisConfigFromAidlFormat(
    const ::aidl::android::hardware::bluetooth::audio::IBluetoothAudioProvider::
        LeAudioSubgroupBisConfiguration& aidl_cfg,
    ::bluetooth::le_audio::types::LeAudioCodecId& out_codec_id) {
  out_codec_id =
      GetStackCodecIdFromAidlFormat(aidl_cfg.bisConfiguration.codecId);
  // TODO: Support metadata at the BIS level in the BT stack.
  // @nullable MetadataLtv[] aidl_cfg.bisConfiguration.metadata;

  // TODO: Don't use the hardcoded value - the allocated channel count should be
  // used instead
  uint8_t bis_channel_cnt = 1;

  return ::bluetooth::le_audio::broadcaster::BroadcastSubgroupBisCodecConfig(
      aidl_cfg.numBis, bis_channel_cnt,
      GetStackLeAudioLtvMapFromAidlFormat(
          aidl_cfg.bisConfiguration.codecConfiguration),
      aidl_cfg.bisConfiguration.vendorCodecConfiguration.empty()
          ? std::nullopt
          : std::optional<std::vector<uint8_t>>(
                aidl_cfg.bisConfiguration.vendorCodecConfiguration));
}

std::vector<::bluetooth::le_audio::broadcaster::BroadcastSubgroupCodecConfig>
GetStackSubgroupsFromAidlFormat(
    const std::vector<
        ::aidl::android::hardware::bluetooth::audio::IBluetoothAudioProvider::
            LeAudioBroadcastSubgroupConfiguration>& aidl_subgroups) {
  std::vector<::bluetooth::le_audio::broadcaster::BroadcastSubgroupCodecConfig>
      vec;
  for (const auto& subgroup : aidl_subgroups) {
    std::vector<
        ::bluetooth::le_audio::broadcaster::BroadcastSubgroupBisCodecConfig>
        bis_codec_configs;
    ::bluetooth::le_audio::types::LeAudioCodecId codec_id;
    for (auto const& bis_cfg : subgroup.bisConfigurations) {
      bis_codec_configs.push_back(
          GetStackBisConfigFromAidlFormat(bis_cfg, codec_id));
    }

    uint8_t bits_per_sample = 16;  // Note: Irrelevant for the offloader
    ::bluetooth::le_audio::broadcaster::BroadcastSubgroupCodecConfig
        stack_subgroup(codec_id, bis_codec_configs, bits_per_sample,
                       subgroup.vendorCodecConfiguration);
    vec.push_back(stack_subgroup);
  }
  return vec;
}

std::optional<::bluetooth::le_audio::broadcaster::BroadcastConfiguration>
GetStackBroadcastConfigurationFromAidlFormat(
    const ::aidl::android::hardware::bluetooth::audio::IBluetoothAudioProvider::
        LeAudioBroadcastConfigurationSetting& setting) {
  ::bluetooth::le_audio::broadcaster::BroadcastConfiguration cfg{
      .subgroups =
          GetStackSubgroupsFromAidlFormat(setting.subgroupsConfigurations),
      .qos = ::bluetooth::le_audio::broadcaster::BroadcastQosConfig(
          setting.retransmitionNum, setting.maxTransportLatencyMs),
      .data_path =
          GetStackDataPathFromAidlFormat(*setting.dataPathConfiguration),
      .sduIntervalUs = (uint32_t)setting.sduIntervalUs,
      .maxSduOctets = (uint16_t)setting.maxSduOctets,
      .phy = 0,  // recomputed later on
      .packing = (uint8_t)setting.packing,
      .framing = (uint8_t)setting.framing,
  };

  for (auto phy : setting.phy) {
    cfg.phy |= static_cast<int8_t>(phy);
  }

  return std::move(cfg);
}

::bluetooth::le_audio::set_configurations::QosConfigSetting
GetStackQosConfigSettingFromAidl(
    const std::optional<
        ::aidl::android::hardware::bluetooth::audio::IBluetoothAudioProvider::
            LeAudioAseQosConfiguration>& aidl_qos,
    ::aidl::android::hardware::bluetooth::audio::LeAudioAseConfiguration::
        TargetLatency target_latency) {
  auto config = ::bluetooth::le_audio::set_configurations::QosConfigSetting();
  if (aidl_qos.has_value()) {
    config.sduIntervalUs = aidl_qos->sduIntervalUs;
    config.max_transport_latency = aidl_qos->maxTransportLatencyMs;
    config.maxSdu = aidl_qos->maxSdu;
    config.retransmission_number = aidl_qos->retransmissionNum;
  }
  config.target_latency = (uint8_t)target_latency;

  return config;
}

::bluetooth::le_audio::set_configurations::CodecConfigSetting
GetCodecConfigSettingFromAidl(
    const std::optional<
        ::aidl::android::hardware::bluetooth::audio::LeAudioAseConfiguration>&
        ase_config) {
  auto stack_config =
      ::bluetooth::le_audio::set_configurations::CodecConfigSetting();

  if (ase_config.has_value()) {
    if (ase_config->codecId.has_value()) {
      stack_config.id =
          GetStackCodecIdFromAidlFormat(ase_config->codecId.value());
    }
    if (ase_config->vendorCodecConfiguration.has_value()) {
      stack_config.vendor_params = ase_config->vendorCodecConfiguration.value();
    }

    if (!ase_config->codecConfiguration.empty()) {
      stack_config.params =
          GetStackLeAudioLtvMapFromAidlFormat(ase_config->codecConfiguration);
    }

    // FIXME: Seems redundant if audio allocations in .codec.params is mandatory
    // stack_config.channel_count_per_iso_stream = ;
  }

  return stack_config;
}

::bluetooth::le_audio::types::DataPathConfiguration
GetStackDataPathFromAidlFormat(
    const ::aidl::android::hardware::bluetooth::audio::IBluetoothAudioProvider::
        LeAudioDataPathConfiguration& dp) {
  auto config = ::bluetooth::le_audio::types::DataPathConfiguration{
      .dataPathId = static_cast<uint8_t>(dp.dataPathId),
      .dataPathConfig = {},
      .isoDataPathConfig = {
          .codecId = GetStackCodecIdFromAidlFormat(
              dp.isoDataPathConfiguration.codecId),
          .isTransparent = dp.isoDataPathConfiguration.isTransparent,
          .controllerDelayUs =
              (uint32_t)dp.isoDataPathConfiguration.controllerDelayUs,
          .configuration = {},
      }};

  if (dp.dataPathConfiguration.configuration) {
    config.dataPathConfig = *dp.dataPathConfiguration.configuration;
  }

  if (dp.isoDataPathConfiguration.configuration) {
    config.isoDataPathConfig.configuration =
        *dp.isoDataPathConfiguration.configuration;
  }

  return config;
}

// The number of source entries is the total count of ASEs within the group to
// be configured
::bluetooth::le_audio::set_configurations::AseConfiguration
GetStackAseConfigurationFromAidl(
    const ::aidl::android::hardware::bluetooth::audio::IBluetoothAudioProvider::
        LeAudioAseConfigurationSetting::AseDirectionConfiguration& source) {
  auto stack_qos = GetStackQosConfigSettingFromAidl(
      source.qosConfiguration, source.aseConfiguration.targetLatency);

  auto config = ::bluetooth::le_audio::set_configurations::AseConfiguration(
      GetCodecConfigSettingFromAidl(source.aseConfiguration), stack_qos);
  if (source.dataPathConfiguration.has_value()) {
    config.data_path_configuration =
        GetStackDataPathFromAidlFormat(*source.dataPathConfiguration);
  }
  return config;
}

::bluetooth::le_audio::set_configurations::AudioSetConfiguration
GetStackConfigSettingFromAidl(
    ::bluetooth::le_audio::types::LeAudioContextType ctx_type,
    const ::aidl::android::hardware::bluetooth::audio::IBluetoothAudioProvider::
        LeAudioAseConfigurationSetting& aidl_ase_config) {
  /* Verify the returned audio context and report if not matching */
  if (aidl_ase_config.audioContext.bitmask != (uint16_t)ctx_type) {
    log::error("Audio Context mismatch. Expected {}, but received: {}",
               (int)ctx_type, aidl_ase_config.audioContext.bitmask);
  }

  ::bluetooth::le_audio::set_configurations::AudioSetConfiguration cig_config{
      .name = "",
      .packing = (uint8_t)aidl_ase_config.packing,
      .confs = {.sink = {}, .source = {}},
  };

  if (aidl_ase_config.sinkAseConfiguration) {
    for (auto const& entry : *aidl_ase_config.sinkAseConfiguration) {
      if (entry) {
        cig_config.confs.sink.push_back(
            GetStackAseConfigurationFromAidl(*entry));
      }
    }
  }

  if (aidl_ase_config.sourceAseConfiguration) {
    for (auto const& entry : *aidl_ase_config.sourceAseConfiguration) {
      if (entry) {
        cig_config.confs.source.push_back(
            GetStackAseConfigurationFromAidl(*entry));
      }
    }
  }

  return cig_config;
}

std::optional<::bluetooth::le_audio::set_configurations::AudioSetConfiguration>
GetStackUnicastConfigurationFromAidlFormat(
    ::bluetooth::le_audio::types::LeAudioContextType ctx_type,
    const ::aidl::android::hardware::bluetooth::audio::IBluetoothAudioProvider::
        LeAudioAseConfigurationSetting& config) {
  auto stack_config = GetStackConfigSettingFromAidl(ctx_type, config);

  if (stack_config.confs.sink.empty() && stack_config.confs.source.empty()) {
    log::error("Unexpected empty sink and source configurations!");
    return std::nullopt;
  }
  return stack_config;
}
}  // namespace aidl
}  // namespace audio
}  // namespace bluetooth
