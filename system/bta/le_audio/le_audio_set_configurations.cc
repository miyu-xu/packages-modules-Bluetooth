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

#include "le_audio_types.h"

namespace le_audio {
namespace set_configurations {

const types::LeAudioCodecId LeAudioCodecIdLc3 = {
    .coding_format = types::kLeAudioCodingFormatLC3,
    .vendor_company_id = types::kLeAudioVendorCompanyIdUndefined,
    .vendor_codec_id = types::kLeAudioVendorCodecIdUndefined};

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

/**
 * Supported audio codec capability settings
 *
 * The subset of capabilities defined in BAP_Validation_r13 Table 3.6.
 */
constexpr set_configurations::CodecCapabilitySetting codec_lc3_16_1(
    uint8_t channel_count) {
  return set_configurations::CodecCapabilitySetting(
      LeAudioCodecIdLc3,
      types::LeAudioLc3Config({
          .sampling_frequency = codec_spec_conf::kLeAudioSamplingFreq16000Hz,
          .frame_duration = codec_spec_conf::kLeAudioCodecLC3FrameDur7500us,
          .octets_per_codec_frame = codec_spec_conf::kLeAudioCodecLC3FrameLen30,
          .channel_count = channel_count,
          .audio_channel_allocation = 0,
      }));
}

constexpr set_configurations::CodecCapabilitySetting codec_lc3_16_2(
    uint8_t channel_count) {
  return set_configurations::CodecCapabilitySetting(
      LeAudioCodecIdLc3,
      types::LeAudioLc3Config({
          .sampling_frequency = codec_spec_conf::kLeAudioSamplingFreq16000Hz,
          .frame_duration = codec_spec_conf::kLeAudioCodecLC3FrameDur10000us,
          .octets_per_codec_frame = codec_spec_conf::kLeAudioCodecLC3FrameLen40,
          .channel_count = channel_count,
          .audio_channel_allocation = 0,
      }));
}

constexpr set_configurations::CodecCapabilitySetting codec_lc3_48_4(
    uint8_t channel_count) {
  return set_configurations::CodecCapabilitySetting(
      LeAudioCodecIdLc3,
      types::LeAudioLc3Config({
          .sampling_frequency = codec_spec_conf::kLeAudioSamplingFreq48000Hz,
          .frame_duration = codec_spec_conf::kLeAudioCodecLC3FrameDur10000us,
          .octets_per_codec_frame =
              codec_spec_conf::kLeAudioCodecLC3FrameLen120,
          .channel_count = channel_count,
          .audio_channel_allocation = 0,
      }));
}

/*
 * set_configurations::AudioSetConfiguration defines the audio set configuration
 * and codec settings to to be used by le audio policy to match the required
 * configuration with audio server capabilities. The codec settings are defined
 * with respect to "Broadcast Source audio capability configuration support
 * requirements" defined in BAP d09r06
 */
const set_configurations::AudioSetConfiguration kSingleDev_OneChanMonoSnk_16_2 =
    {.name_ = "kSingleDev_OneChanMonoSnk_16_2",
     .confs_ = {set_configurations::SetConfiguration(
         types::kLeAudioDirectionSink, 1, 1,
         codec_lc3_16_2(
             codec_spec_caps::kLeAudioCodecLC3ChannelCountSingleChannel))}};

const set_configurations::AudioSetConfiguration kSingleDev_OneChanMonoSnk_16_1 =
    {.name_ = "kSingleDev_OneChanMonoSnk_16_1",
     .confs_ = {set_configurations::SetConfiguration(
         types::kLeAudioDirectionSink, 1, 1,
         codec_lc3_16_1(
             codec_spec_caps::kLeAudioCodecLC3ChannelCountSingleChannel))}};

const set_configurations::AudioSetConfiguration
    kSingleDev_TwoChanStereoSnk_16_1 = {
        .name_ = "kSingleDev_TwoChanStereoSnk_16_1",
        .confs_ = {set_configurations::SetConfiguration(
            types::kLeAudioDirectionSink, 1, 1,
            codec_lc3_16_1(
                codec_spec_caps::kLeAudioCodecLC3ChannelCountTwoChannel),
            LeAudioConfigurationStrategy::STEREO_ONE_CIS_PER_DEVICE)}};

const set_configurations::AudioSetConfiguration
    kSingleDev_OneChanStereoSnk_16_1 = {
        .name_ = "kSingleDev_OneChanStereoSnk_16_1",
        .confs_ = {set_configurations::SetConfiguration(
            types::kLeAudioDirectionSink, 1, 2,
            codec_lc3_16_1(
                codec_spec_caps::kLeAudioCodecLC3ChannelCountSingleChannel),
            LeAudioConfigurationStrategy::STEREO_TWO_CISES_PER_DEVICE)}};

const set_configurations::AudioSetConfiguration kDualDev_OneChanStereoSnk_16_1 =
    {.name_ = "kDualDev_OneChanStereoSnk_16_1",
     .confs_ = {set_configurations::SetConfiguration(
         types::kLeAudioDirectionSink, 2, 2,
         codec_lc3_16_1(
             codec_spec_caps::kLeAudioCodecLC3ChannelCountSingleChannel))}};

const set_configurations::AudioSetConfiguration
    kSingleDev_TwoChanStereoSnk_48_4 = {
        .name_ = "kSingleDev_TwoChanStereoSnk_48_4",
        .confs_ = {set_configurations::SetConfiguration(
            types::kLeAudioDirectionSink, 1, 1,
            codec_lc3_48_4(
                codec_spec_caps::kLeAudioCodecLC3ChannelCountTwoChannel),
            LeAudioConfigurationStrategy::STEREO_ONE_CIS_PER_DEVICE)}};

const set_configurations::AudioSetConfiguration kDualDev_OneChanStereoSnk_48_4 =
    {.name_ = "kDualDev_OneChanStereoSnk_48_4",
     .confs_ = {set_configurations::SetConfiguration(
         types::kLeAudioDirectionSink, 2, 2,
         codec_lc3_48_4(
             codec_spec_caps::kLeAudioCodecLC3ChannelCountSingleChannel))}};

const set_configurations::AudioSetConfiguration
    kSingleDev_OneChanStereoSnk_48_4 = {
        .name_ = "kSingleDev_OneChanStereoSnk_48_4",
        .confs_ = {set_configurations::SetConfiguration(
            types::kLeAudioDirectionSink, 1, 2,
            codec_lc3_48_4(
                codec_spec_caps::kLeAudioCodecLC3ChannelCountSingleChannel),
            LeAudioConfigurationStrategy::STEREO_TWO_CISES_PER_DEVICE)}};

const set_configurations::AudioSetConfiguration kSingleDev_OneChanMonoSnk_48_4 =
    {.name_ = "kSingleDev_OneChanMonoSnk_48_4",
     .confs_ = {set_configurations::SetConfiguration(
         types::kLeAudioDirectionSink, 1, 1,
         codec_lc3_48_4(
             codec_spec_caps::kLeAudioCodecLC3ChannelCountSingleChannel))}};

const set_configurations::AudioSetConfiguration
    kSingleDev_TwoChanStereoSnk_16_2 = {
        .name_ = "kSingleDev_TwoChanStereoSnk_16_2",
        .confs_ = {set_configurations::SetConfiguration(
            types::kLeAudioDirectionSink, 1, 1,
            codec_lc3_16_2(
                codec_spec_caps::kLeAudioCodecLC3ChannelCountTwoChannel),
            LeAudioConfigurationStrategy::STEREO_ONE_CIS_PER_DEVICE)}};

const set_configurations::AudioSetConfiguration
    kSingleDev_OneChanStereoSnk_16_2 = {
        .name_ = "kSingleDev_OneChanStereoSnk_16_2",
        .confs_ = {set_configurations::SetConfiguration(
            types::kLeAudioDirectionSink, 1, 2,
            codec_lc3_16_2(
                codec_spec_caps::kLeAudioCodecLC3ChannelCountSingleChannel),
            LeAudioConfigurationStrategy::STEREO_TWO_CISES_PER_DEVICE)}};

const set_configurations::AudioSetConfiguration kDualDev_OneChanStereoSnk_16_2 =
    {.name_ = "kDualDev_OneChanStereoSnk_16_2",
     .confs_ = {set_configurations::SetConfiguration(
         types::kLeAudioDirectionSink, 2, 2,
         codec_lc3_16_2(
             codec_spec_caps::kLeAudioCodecLC3ChannelCountSingleChannel))}};

const set_configurations::AudioSetConfiguration
    kSingleDev_OneChanMonoSnk_OneChanMonoSrc_16_1 = {
        .name_ = "kSingleDev_OneChanMonoSnk_OneChanMonoSrc_16_1",
        .confs_ = {
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSink, 1, 1,
                codec_lc3_16_1(codec_spec_caps::
                                   kLeAudioCodecLC3ChannelCountSingleChannel)),
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSource, 1, 1,
                codec_lc3_16_1(
                    codec_spec_caps::
                        kLeAudioCodecLC3ChannelCountSingleChannel))}};

const set_configurations::AudioSetConfiguration
    kSingleDev_OneChanMonoSnk_OneChanMonoSrc_16_2 = {
        .name_ = "kSingleDev_OneChanMonoSnk_OneChanMonoSrc_16_2",
        .confs_ = {
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSink, 1, 1,
                codec_lc3_16_2(codec_spec_caps::
                                   kLeAudioCodecLC3ChannelCountSingleChannel)),
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSource, 1, 1,
                codec_lc3_16_2(
                    codec_spec_caps::
                        kLeAudioCodecLC3ChannelCountSingleChannel))}};

const set_configurations::AudioSetConfiguration
    kSingleDev_TwoChanStereoSnk_OneChanMonoSrc_16_2 = {
        .name_ = "kSingleDev_TwoChanStereoSnk_OneChanMonoSrc_16_2",
        .confs_ = {
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSink, 1, 1,
                codec_lc3_16_2(
                    codec_spec_caps::kLeAudioCodecLC3ChannelCountTwoChannel),
                LeAudioConfigurationStrategy::STEREO_ONE_CIS_PER_DEVICE),
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSource, 1, 1,
                codec_lc3_16_2(
                    codec_spec_caps::
                        kLeAudioCodecLC3ChannelCountSingleChannel))}};

const set_configurations::AudioSetConfiguration
    kDualDev_OneChanDoubleStereoSnk_OneChanMonoSrc_16_2 = {
        .name_ = "kDualDev_OneChanDoubleStereoSnk_OneChanMonoSrc_16_2",
        .confs_ = {
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSink, 2, 4,
                codec_lc3_16_2(
                    codec_spec_caps::kLeAudioCodecLC3ChannelCountSingleChannel),
                LeAudioConfigurationStrategy::STEREO_TWO_CISES_PER_DEVICE),
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSource, 1, 1,
                codec_lc3_16_2(
                    codec_spec_caps::
                        kLeAudioCodecLC3ChannelCountSingleChannel))}};

const set_configurations::AudioSetConfiguration
    kSingleDev_OneChanStereoSnk_OneChanMonoSrc_16_2 = {
        .name_ = "kSingleDev_OneChanStereoSnk_OneChanMonoSrc_16_2",
        .confs_ = {
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSink, 1, 2,
                codec_lc3_16_2(
                    codec_spec_caps::kLeAudioCodecLC3ChannelCountSingleChannel),
                LeAudioConfigurationStrategy::STEREO_TWO_CISES_PER_DEVICE),
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSource, 1, 1,
                codec_lc3_16_2(
                    codec_spec_caps::
                        kLeAudioCodecLC3ChannelCountSingleChannel))}};

const set_configurations::AudioSetConfiguration
    kDualDev_OneChanStereoSnk_OneChanMonoSrc_16_2 = {
        .name_ = "kDualDev_OneChanStereoSnk_OneChanMonoSrc_16_2",
        .confs_ = {
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSink, 2, 2,
                codec_lc3_16_2(codec_spec_caps::
                                   kLeAudioCodecLC3ChannelCountSingleChannel)),
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSource, 1, 1,
                codec_lc3_16_2(
                    codec_spec_caps::
                        kLeAudioCodecLC3ChannelCountSingleChannel))}};

const set_configurations::AudioSetConfiguration
    kSingleDev_TwoChanStereoSnk_OneChanMonoSrc_16_1 = {
        .name_ = "kSingleDev_TwoChanStereoSnk_OneChanMonoSrc_16_1",
        .confs_ = {
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSink, 1, 1,
                codec_lc3_16_1(
                    codec_spec_caps::kLeAudioCodecLC3ChannelCountTwoChannel),
                LeAudioConfigurationStrategy::STEREO_ONE_CIS_PER_DEVICE),
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSource, 1, 1,
                codec_lc3_16_1(
                    codec_spec_caps::
                        kLeAudioCodecLC3ChannelCountSingleChannel))}};

const set_configurations::AudioSetConfiguration
    kSingleDev_OneChanStereoSnk_OneChanMonoSrc_16_1 = {
        .name_ = "kSingleDev_OneChanStereoSnk_OneChanMonoSrc_16_1",
        .confs_ = {
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSink, 1, 2,
                codec_lc3_16_1(
                    codec_spec_caps::kLeAudioCodecLC3ChannelCountSingleChannel),
                LeAudioConfigurationStrategy::STEREO_TWO_CISES_PER_DEVICE),
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSource, 1, 1,
                codec_lc3_16_1(
                    codec_spec_caps::
                        kLeAudioCodecLC3ChannelCountSingleChannel))}};

const set_configurations::AudioSetConfiguration
    kDualDev_OneChanStereoSnk_OneChanMonoSrc_16_1 = {
        .name_ = "kDualDev_OneChanStereoSnk_OneChanMonoSrc_16_1",
        .confs_ = {
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSink, 2, 2,
                codec_lc3_16_1(codec_spec_caps::
                                   kLeAudioCodecLC3ChannelCountSingleChannel)),
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSource, 1, 1,
                codec_lc3_16_1(
                    codec_spec_caps::
                        kLeAudioCodecLC3ChannelCountSingleChannel))}};

const set_configurations::AudioSetConfiguration
    kDualDev_OneChanDoubleStereoSnk_OneChanMonoSrc_16_1 = {
        .name_ = "kDualDev_OneChanDoubleStereoSnk_OneChanMonoSrc_16_1",
        .confs_ = {
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSink, 2, 4,
                codec_lc3_16_1(
                    codec_spec_caps::kLeAudioCodecLC3ChannelCountSingleChannel),
                LeAudioConfigurationStrategy::STEREO_TWO_CISES_PER_DEVICE),
            set_configurations::SetConfiguration(
                types::kLeAudioDirectionSource, 1, 1,
                codec_lc3_16_1(
                    codec_spec_caps::
                        kLeAudioCodecLC3ChannelCountSingleChannel))}};

/* Defined audio scenario linked with context type, priority sorted */
const set_configurations::AudioSetConfigurations audio_set_conf_ringtone = {
    .items_ = {
        &kDualDev_OneChanStereoSnk_16_2,
        &kDualDev_OneChanStereoSnk_16_1,
        &kSingleDev_OneChanStereoSnk_16_2,
        &kSingleDev_OneChanStereoSnk_16_1,
        &kSingleDev_TwoChanStereoSnk_16_2,
        &kSingleDev_TwoChanStereoSnk_16_1,
        &kSingleDev_OneChanMonoSnk_16_2,
        &kSingleDev_OneChanMonoSnk_16_1,
    }};

const set_configurations::AudioSetConfigurations audio_set_conf_conversational =
    {.items_ = {
         &kDualDev_OneChanStereoSnk_OneChanMonoSrc_16_2,
         &kDualDev_OneChanStereoSnk_OneChanMonoSrc_16_1,
         &kDualDev_OneChanDoubleStereoSnk_OneChanMonoSrc_16_2,
         &kDualDev_OneChanDoubleStereoSnk_OneChanMonoSrc_16_1,
         &kSingleDev_TwoChanStereoSnk_OneChanMonoSrc_16_2,
         &kSingleDev_TwoChanStereoSnk_OneChanMonoSrc_16_1,
         &kSingleDev_OneChanStereoSnk_OneChanMonoSrc_16_2,
         &kSingleDev_OneChanStereoSnk_OneChanMonoSrc_16_1,
         &kSingleDev_OneChanMonoSnk_OneChanMonoSrc_16_2,
         &kSingleDev_OneChanMonoSnk_OneChanMonoSrc_16_1,
     }};

const set_configurations::AudioSetConfigurations audio_set_conf_media = {
    .items_ = {
        &kDualDev_OneChanStereoSnk_48_4,
        &kDualDev_OneChanStereoSnk_16_2,
        &kDualDev_OneChanStereoSnk_16_1,
        &kSingleDev_OneChanStereoSnk_48_4,
        &kSingleDev_OneChanStereoSnk_16_2,
        &kSingleDev_OneChanStereoSnk_16_1,
        &kSingleDev_TwoChanStereoSnk_48_4,
        &kSingleDev_TwoChanStereoSnk_16_2,
        &kSingleDev_TwoChanStereoSnk_16_1,
        &kSingleDev_OneChanMonoSnk_48_4,
        &kSingleDev_OneChanMonoSnk_16_2,
        &kSingleDev_OneChanMonoSnk_16_1,
    }};

const set_configurations::AudioSetConfigurations audio_set_conf_default = {
    .items_ = {
        &kDualDev_OneChanStereoSnk_16_2,
        &kSingleDev_OneChanStereoSnk_16_2,
        &kSingleDev_TwoChanStereoSnk_16_2,
        &kSingleDev_OneChanMonoSnk_16_2,
    }};

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
    uint8_t req_devices_cnt = min_req_devices_cnt(ent);
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
      return false;
  }
}

const AudioSetConfigurations* GetConfigurationsByType(
    ::le_audio::types::LeAudioContextType content_type) {
  switch (content_type) {
    case types::LeAudioContextType::MEDIA:
      return &audio_set_conf_media;
    case types::LeAudioContextType::CONVERSATIONAL:
      return &audio_set_conf_conversational;
    case types::LeAudioContextType::RINGTONE:
      return &audio_set_conf_ringtone;
    default:
      return &audio_set_conf_default;
  }
};

}  // namespace set_configurations
}  // namespace le_audio
