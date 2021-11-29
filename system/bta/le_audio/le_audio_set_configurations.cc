/*
 *  Copyright (c) 2021 The Android Open Source Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
#include "le_audio_set_configurations.h"

#include <memory>

#include "audio_set_configurations_generated.h"
#include "audio_set_scenarios_generated.h"
#include "flatbuffers/idl.h"
#include "flatbuffers/util.h"
#include "le_audio_types.h"

namespace le_audio {
namespace set_configurations {

uint32_t CodecCapabilitySetting::GetConfigSamplingFrequency() const {
  switch (id_.coding_format) {
    case types::kLeAudioCodingFormatLC3:
      return std::get<types::LeAudioLc3Config>(config_)
          .GetSamplingFrequencyHz();
    default:
      DLOG(WARNING) << __func__ << ", invalid codec id";
      return 0;
  }
};

uint32_t CodecCapabilitySetting::GetConfigDataIntervalUs() const {
  switch (id_.coding_format) {
    case types::kLeAudioCodingFormatLC3:
      return std::get<types::LeAudioLc3Config>(config_).GetFrameDurationUs();
    default:
      DLOG(WARNING) << __func__ << ", invalid codec id";
      return 0;
  }
};

uint8_t CodecCapabilitySetting::GetConfigBitsPerSample() const {
  switch (id_.coding_format) {
    case types::kLeAudioCodingFormatLC3:
      /* XXX LC3 supports 16, 24, 32 */
      return 16;
    default:
      DLOG(WARNING) << __func__ << ", invalid codec id";
      return 0;
  }
};

uint8_t CodecCapabilitySetting::GetConfigChannelCount() const {
  switch (id_.coding_format) {
    case types::kLeAudioCodingFormatLC3:
      DLOG(INFO) << __func__ << ", count = "
                 << static_cast<int>(std::get<types::LeAudioLc3Config>(config_)
                                         .channel_count);
      return std::get<types::LeAudioLc3Config>(config_).channel_count;
    default:
      DLOG(WARNING) << __func__ << ", invalid codec id";
      return 0;
  }
}

static const bluetooth::le_audio::CodecSpecificConfiguration*
LookupCodecSpecificParam(
    const flatbuffers::Vector<
        flatbuffers::Offset<bluetooth::le_audio::CodecSpecificConfiguration>>*
        flat_codec_specific_params,
    bluetooth::le_audio::CodecSpecificLtvGenericTypes type) {
  auto it = std::find_if(
      flat_codec_specific_params->cbegin(), flat_codec_specific_params->cend(),
      [&type](const auto& csc) { return (csc->type() == type); });
  return (it != flat_codec_specific_params->cend()) ? *it : nullptr;
}

CodecCapabilitySetting::CodecCapabilitySetting(
    const bluetooth::le_audio::CodecId* flat_codec_id,
    const flatbuffers::Vector<
        flatbuffers::Offset<bluetooth::le_audio::CodecSpecificConfiguration>>*
        flat_codec_specific_params) {
  /* Cache the le_audio::types::CodecId type value */
  id_ = types::LeAudioCodecId({
      .coding_format = flat_codec_id->coding_format(),
      .vendor_company_id = flat_codec_id->vendor_company_id(),
      .vendor_codec_id = flat_codec_id->vendor_codec_id(),
  });

  /* Cache the types::LeAudioLc3Config type value */
  uint8_t sampling_frequency = 0;
  uint8_t frame_duration = 0;
  uint32_t audio_channel_allocation = 0;
  uint16_t octets_per_codec_frame = 0;

  auto param = LookupCodecSpecificParam(
      flat_codec_specific_params,
      bluetooth::le_audio::
          CodecSpecificLtvGenericTypes_SUPPORTED_SAMPLING_FREQUENCY);
  if (param) {
    LOG_ASSERT(param->compound_value()->value()->size() == 1)
        << " Invalid compound value length: "
        << param->compound_value()->value()->size();
    auto ptr = param->compound_value()->value()->data();
    STREAM_TO_UINT8(sampling_frequency, ptr);
  }

  param = LookupCodecSpecificParam(
      flat_codec_specific_params,
      bluetooth::le_audio::
          CodecSpecificLtvGenericTypes_SUPPORTED_FRAME_DURATION);
  if (param) {
    LOG_ASSERT(param->compound_value()->value()->size() == 1)
        << " Invalid compound value length: "
        << param->compound_value()->value()->size();
    auto ptr = param->compound_value()->value()->data();
    STREAM_TO_UINT8(frame_duration, ptr);
  }

  param = LookupCodecSpecificParam(
      flat_codec_specific_params,
      bluetooth::le_audio::
          CodecSpecificLtvGenericTypes_SUPPORTED_AUDIO_CHANNEL_ALLOCATION);
  if (param) {
    LOG_ASSERT(param->compound_value()->value()->size() == 4)
        << " Invalid compound value length"
        << param->compound_value()->value()->size();
    auto ptr = param->compound_value()->value()->data();
    STREAM_TO_UINT32(audio_channel_allocation, ptr);
  }

  param = LookupCodecSpecificParam(
      flat_codec_specific_params,
      bluetooth::le_audio::
          CodecSpecificLtvGenericTypes_SUPPORTED_OCTETS_PER_CODEC_FRAME);
  if (param) {
    LOG_ASSERT(param->compound_value()->value()->size() == 2)
        << " Invalid compound value length"
        << param->compound_value()->value()->size();
    auto ptr = param->compound_value()->value()->data();
    STREAM_TO_UINT16(octets_per_codec_frame, ptr);
  }

  config_ = types::LeAudioLc3Config({
      .sampling_frequency = sampling_frequency,
      .frame_duration = frame_duration,
      .octets_per_codec_frame = octets_per_codec_frame,
      .channel_count =
          (uint8_t)std::bitset<32>(audio_channel_allocation).count(),
      .audio_channel_allocation = audio_channel_allocation,
  });
}

AudioSetConfiguration::AudioSetConfiguration(
    const bluetooth::le_audio::AudioSetConfiguration* flat_cfg) {
  name_ = flat_cfg->name()->c_str();

  if (!flat_cfg->subconfigurations()) {
    LOG(ERROR) << "Configuration ' " << name_
               << "' has no valid subconfigurations.";
    return;
  }
  for (auto subconfig : *flat_cfg->subconfigurations()) {
    confs_.push_back(SetConfiguration(subconfig));
  }
}

static const bluetooth::le_audio::AudioSetConfiguration* LookupConfig(
    const flatbuffers::Vector<flatbuffers::Offset<
        bluetooth::le_audio::AudioSetConfiguration>>* configs,
    const flatbuffers::String* name) {
  auto it =
      std::find_if(configs->cbegin(), configs->cend(), [name](const auto& cfg) {
        return std::string(cfg->name()->c_str())
                   .compare(std::string(name->c_str())) == 0;
      });
  return (it != configs->cend()) ? *it : nullptr;
}

AudioSetConfigurations::AudioSetConfigurations(
    const bluetooth::le_audio::AudioSetScenario* const flat_scenario,
    const flatbuffers::Vector<
        flatbuffers::Offset<bluetooth::le_audio::AudioSetConfiguration>>*
        flat_configs) {
  if (!flat_scenario->configurations()) return;

  for (auto config_name : *flat_scenario->configurations()) {
    auto flat_config = LookupConfig(flat_configs, config_name);
    if (!flat_config) {
      LOG(ERROR) << __func__ << ": Unknown configuration entry '"
                 << config_name->c_str() << "' in '"
                 << flat_scenario->name()->c_str() << "' scenario.";
      continue;
    }

    items_.push_back(AudioSetConfiguration(flat_config));
  }
}

static uint8_t min_req_devices_cnt(
    const AudioSetConfiguration* audio_set_conf) {
  std::pair<uint8_t /* sink */, uint8_t /* source */> snk_src_pair(0, 0);

  for (auto ent : (*audio_set_conf).confs()) {
    if (ent.direction() == types::kLeAudioDirectionSink)
      snk_src_pair.first += ent.device_cnt();
    if (ent.direction() == types::kLeAudioDirectionSource)
      snk_src_pair.second += ent.device_cnt();
  }

  return std::max(snk_src_pair.first, snk_src_pair.second);
}

static uint8_t min_req_devices_cnt(
    const AudioSetConfigurations* audio_set_confs) {
  uint8_t curr_min_req_devices_cnt = 0xff;

  for (auto ent : *audio_set_confs) {
    uint8_t req_devices_cnt = min_req_devices_cnt(&ent);
    if (req_devices_cnt < curr_min_req_devices_cnt)
      curr_min_req_devices_cnt = req_devices_cnt;
  }

  return curr_min_req_devices_cnt;
}

bool CheckIfMayCoverScenario(const AudioSetConfigurations* audio_set_confs,
                             uint8_t group_size) {
  if (!audio_set_confs) {
    DLOG(ERROR) << __func__ << ", no audio requirements for group";
    return false;
  }

  return group_size >= min_req_devices_cnt(audio_set_confs);
}

bool CheckIfMayCoverScenario(const AudioSetConfiguration* audio_set_conf,
                             uint8_t group_size) {
  if (!audio_set_conf) {
    DLOG(ERROR) << __func__ << ", no audio requirement for group";
    return false;
  }

  return group_size >= min_req_devices_cnt(audio_set_conf);
}

static bool IsCodecConfigurationSupported(
    const types::LeAudioLtvMap& pacs,
    const types::LeAudioLc3Config& lc3_config) {
  const auto& reqs = lc3_config.GetAsLtvMap();
  uint8_t u8_req_val, u8_pac_val;
  uint16_t u16_req_val, u16_pac_val;

  /* Sampling frequency */
  auto req = reqs.Find(codec_spec_conf::kLeAudioCodecLC3TypeSamplingFreq);
  auto pac = pacs.Find(codec_spec_caps::kLeAudioCodecLC3TypeSamplingFreq);
  if (!req || !pac) {
    DLOG(ERROR) << __func__ << ", lack of sampling frequency fields";
    return false;
  }

  u8_req_val = VEC_UINT8_TO_UINT8(req.value());
  u16_pac_val = VEC_UINT8_TO_UINT16(pac.value());

  /*
   * Note: Requirements are in the codec configuration specification which
   * are values coming from BAP Appendix A1.2.1
   */
  DLOG(INFO) << __func__ << " Req:SamplFreq=" << loghex(u8_req_val);
  /* NOTE: Below is Codec specific cababilities comes form BAP Appendix A A1.1.1
   * Note this is a bitfield
   */
  DLOG(INFO) << __func__ << " Pac:SamplFreq=" << loghex(u16_pac_val);

  /* TODO: Integrate with codec capabilities */
  if ((u8_req_val != codec_spec_conf::kLeAudioSamplingFreq16000Hz &&
       u8_req_val != codec_spec_conf::kLeAudioSamplingFreq48000Hz) ||
      !(u16_pac_val &
        codec_spec_caps::SamplingFreqConfig2Capability(u8_req_val))) {
    DLOG(ERROR) << __func__ << ", sampling frequency not supported";
    return false;
  }

  /* Frame duration */
  req = reqs.Find(codec_spec_conf::kLeAudioCodecLC3TypeFrameDuration);
  pac = pacs.Find(codec_spec_caps::kLeAudioCodecLC3TypeFrameDuration);
  if (!req || !pac) {
    DLOG(ERROR) << __func__ << ", lack of frame duration fields";
    return false;
  }

  u8_req_val = VEC_UINT8_TO_UINT8(req.value());
  u8_pac_val = VEC_UINT8_TO_UINT8(pac.value());
  DLOG(INFO) << __func__ << " Req:FrameDur=" << loghex(u8_req_val);
  DLOG(INFO) << __func__ << " Pac:FrameDur=" << loghex(u8_pac_val);

  if ((u8_req_val != codec_spec_conf::kLeAudioCodecLC3FrameDur7500us &&
       u8_req_val != codec_spec_conf::kLeAudioCodecLC3FrameDur10000us) ||
      !(u8_pac_val &
        (codec_spec_caps::FrameDurationConfig2Capability(u8_req_val)))) {
    DLOG(ERROR) << __func__ << ", frame duration not supported";
    return false;
  }

  uint8_t required_audio_chan_num = lc3_config.GetChannelCount();
  pac = pacs.Find(codec_spec_caps::kLeAudioCodecLC3TypeAudioChannelCounts);

  /*
   * BAP_Validation_r07 1.9.2 Audio channel support requirements
   * "The Unicast Server shall support an Audio_Channel_Counts value of 0x01
   * (0b00000001 = one channel) and may support other values defined by an
   * implementation or by a higher-layer specification."
   *
   * Thus if Audio_Channel_Counts is not present in PAC LTV structure, we assume
   * the Unicast Server supports mandatory one channel.
   */
  if (!pac) {
    DLOG(WARNING) << __func__ << ", no Audio_Channel_Counts field in PAC";
    u8_pac_val = 0x01;
  } else {
    u8_pac_val = VEC_UINT8_TO_UINT8(pac.value());
  }

  DLOG(INFO) << __func__ << " Pac:AudioChanCnt=" << loghex(u8_pac_val);
  if (!((1 << (required_audio_chan_num - 1)) & u8_pac_val)) {
    DLOG(ERROR) << __func__ << ", channel count warning";
    return false;
  }

  /* Octets per frame */
  req = reqs.Find(codec_spec_conf::kLeAudioCodecLC3TypeOctetPerFrame);
  pac = pacs.Find(codec_spec_caps::kLeAudioCodecLC3TypeOctetPerFrame);

  if (!req || !pac) {
    DLOG(ERROR) << __func__ << ", lack of octet per frame fields";
    return false;
  }

  u16_req_val = VEC_UINT8_TO_UINT16(req.value());
  DLOG(INFO) << __func__ << " Req:OctetsPerFrame=" << int(u16_req_val);

  /* Minimal value 0-1 byte */
  u16_pac_val = VEC_UINT8_TO_UINT16(pac.value());
  DLOG(INFO) << __func__ << " Pac:MinOctetsPerFrame=" << int(u16_pac_val);
  if (u16_req_val < u16_pac_val) {
    DLOG(ERROR) << __func__ << ", octet per frame below minimum";
    return false;
  }

  /* Maximal value 2-3 byte */
  u16_pac_val = OFF_VEC_UINT8_TO_UINT16(pac.value(), 2);
  DLOG(INFO) << __func__ << " Pac:MaxOctetsPerFrame=" << int(u16_pac_val);
  if (u16_req_val > u16_pac_val) {
    DLOG(ERROR) << __func__ << ", octet per frame above maximum";
    return false;
  }

  return true;
}

bool IsCodecCapabilitySettingSupported(
    const types::acs_ac_record& pac,
    const CodecCapabilitySetting& codec_capability_setting) {
  const auto& codec_id = codec_capability_setting.id();

  if (codec_id != pac.codec_id) return false;

  DLOG(INFO) << __func__ << ": Settings for format " << +codec_id.coding_format;

  switch (codec_id.coding_format) {
    case types::kLeAudioCodingFormatLC3:
      return IsCodecConfigurationSupported(
          pac.codec_spec_caps,
          std::get<types::LeAudioLc3Config>(codec_capability_setting.config()));
    default:
      DLOG(INFO) << " ...not supported";
      return false;
  }
}

class AudioSetConfigurationProviderImpl : public AudioSetConfigurationProvider {
 public:
  AudioSetConfigurationProviderImpl() = default;
  ~AudioSetConfigurationProviderImpl() = default;

  bool LoadContent(
      std::vector<std::pair<const char* /*schema*/, const char* /*content*/>>
          config_files,
      std::vector<std::pair<const char* /*schema*/, const char* /*content*/>>
          scenario_files) {
    for (auto [schema, content] : config_files) {
      if (!LoadConfigurationsFromFiles(schema, content)) return false;
    }

    for (auto [schema, content] : scenario_files) {
      if (!LoadScenariosFromFiles(schema, content)) return false;
    }

    /* Get the root container */
    auto scenarios_root = bluetooth::le_audio::GetAudioSetScenarios(
        scenarios_parser_.builder_.GetBufferPointer());
    if (!scenarios_root) return false;

    /* Load scenario configurations */
    auto configurations_root = bluetooth::le_audio::GetAudioSetConfigurations(
        configurations_parser_.builder_.GetBufferPointer());
    auto all_configs = configurations_root->configurations();
    if ((all_configs == nullptr) || (all_configs->size() == 0)) return false;

    /* Get all scenarios */
    auto scenarios = scenarios_root->scenarios();
    if (scenarios->size() == 0) return false;
    for (auto const scenario : *scenarios) {
      auto cfgs = AudioSetConfigurations(scenario,
                                         configurations_root->configurations());
      DLOG(INFO) << __func__ << ": Updating scenario "
                 << scenario->name()->c_str()
                 << " configurations :" << cfgs.size();
      context_configurations_.insert_or_assign(
          ScenarioToContextType(scenario->name()->c_str()),
          AudioSetConfigurations(scenario,
                                 configurations_root->configurations()));
    }

    return true;
  }

  const AudioSetConfigurations* GetConfigurations(
      ::le_audio::types::LeAudioContextType context_type) const override {
    if (context_configurations_.count(context_type))
      return &context_configurations_.at(context_type);

    LOG(WARNING) << __func__ << ": No predefined scenario for the context '"
                 << (int)context_type << "' was found.";

    auto fallback_scenario = "Default";
    context_type = ScenarioToContextType(fallback_scenario);
    if (context_configurations_.count(context_type)) {
      LOG(WARNING) << __func__ << ": Using '" << fallback_scenario
                   << "' scenario by default.";
      return &context_configurations_.at(context_type);
    }

    LOG(ERROR) << __func__
               << ": No fallback configuration for the 'Default' scenario or"
                  " no valid audio set configurations loaded at all.";
    return nullptr;
  };

 private:
  /* Flatbuffers content */
  flatbuffers::Parser configurations_parser_;
  flatbuffers::Parser scenarios_parser_;

  /* Flatbuffers wrappers */
  std::map<::le_audio::types::LeAudioContextType, AudioSetConfigurations>
      context_configurations_;

  bool LoadConfigurationsFromFiles(const char* schema_file,
                                   const char* content_file) {
    std::string configurations_schema_binary_content;
    bool ok = flatbuffers::LoadFile(schema_file, true,
                                    &configurations_schema_binary_content);
    if (!ok) return ok;

    /* Load the binary schema */
    ok = configurations_parser_.Deserialize(
        (uint8_t*)configurations_schema_binary_content.c_str(),
        configurations_schema_binary_content.length());
    if (!ok) return ok;

    /* Load the content from JSON */
    std::string configurations_json_content;
    ok = flatbuffers::LoadFile(content_file, false,
                               &configurations_json_content);
    ok = configurations_parser_.Parse(configurations_json_content.c_str());
    return ok;
  }

  bool LoadScenariosFromFiles(const char* schema_file,
                              const char* content_file) {
    std::string scenarios_schema_binary_content;
    bool ok = flatbuffers::LoadFile(schema_file, true,
                                    &scenarios_schema_binary_content);
    if (!ok) return ok;

    /* Load the binary schema */
    ok = scenarios_parser_.Deserialize(
        (uint8_t*)scenarios_schema_binary_content.c_str(),
        scenarios_schema_binary_content.length());
    if (!ok) return ok;

    /* Load the content from JSON */
    std::string scenarios_json_content;
    ok = flatbuffers::LoadFile(content_file, false, &scenarios_json_content);
    ok = scenarios_parser_.Parse(scenarios_json_content.c_str());
    return ok;
  }

  std::string ContextTypeToScenario(
      ::le_audio::types::LeAudioContextType context_type) {
    switch (context_type) {
      case types::LeAudioContextType::MEDIA:
        return "Media";
      case types::LeAudioContextType::CONVERSATIONAL:
        return "Conversational";
      case types::LeAudioContextType::RINGTONE:
        return "Ringtone";
      default:
        return "Default";
    }
  }

  static ::le_audio::types::LeAudioContextType ScenarioToContextType(
      std::string scenario) {
    static const std::map<std::string, ::le_audio::types::LeAudioContextType>
        scenarios = {
            {"Media", types::LeAudioContextType::MEDIA},
            {"Conversational", types::LeAudioContextType::CONVERSATIONAL},
            {"Ringtone", types::LeAudioContextType::RINGTONE},
            {"Default", types::LeAudioContextType::UNSPECIFIED},
        };
    return scenarios.count(scenario) ? scenarios.at(scenario)
                                     : types::LeAudioContextType::RFU;
  }
};

static std::unique_ptr<AudioSetConfigurationProviderImpl> impl;

void AudioSetConfigurationProvider::Initialize(
    std::vector<std::pair<const char* /*schema*/, const char* /*content*/>>
        configs,
    std::vector<std::pair<const char* /*schema*/, const char* /*content*/>>
        scenarios) {
  if (!impl.get()) impl = std::make_unique<AudioSetConfigurationProviderImpl>();

  if (configs.empty()) {
    DLOG(INFO) << __func__ << ": Loading default audio set configurations";
    configs = {
        {"/system/etc/bluetooth/le_audio/audio_set_configurations.bfbs",
         "/system/etc/bluetooth/le_audio/audio_set_configurations.json"}};
  }

  if (scenarios.empty()) {
    DLOG(INFO) << __func__ << ": Loading default audio set scenarios";
    scenarios = {{"/system/etc/bluetooth/le_audio/audio_set_scenarios.bfbs",
                  "/system/etc/bluetooth/le_audio/audio_set_scenarios.json"}};
  }

  if (!impl->LoadContent(configs, scenarios))
    LOG(ERROR) << __func__ << ": Unable to load le audio configuration files.";
}
void AudioSetConfigurationProvider::Cleanup() {
  if (impl.get()) impl.release();
}
AudioSetConfigurationProvider* AudioSetConfigurationProvider::Get() {
  return impl.get();
}

}  // namespace set_configurations
}  // namespace le_audio
