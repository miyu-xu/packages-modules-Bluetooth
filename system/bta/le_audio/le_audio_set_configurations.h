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
#pragma once

#include <variant>
#include <vector>

#include "le_audio_types.h"

namespace le_audio {
namespace set_configurations {

struct CodecCapabilitySetting {
  constexpr CodecCapabilitySetting(types::LeAudioCodecId id,
                                   std::variant<types::LeAudioLc3Config> config)
      : id_(id), config_(config) {}
  /* Codec ID */
  const types::LeAudioCodecId& id() const { return id_; }
  /* Codec Configuration */
  const std::variant<types::LeAudioLc3Config>& config() const {
    return config_;
  }

  /* Sampling freqency requested for codec */
  uint32_t GetConfigSamplingFrequency() const;
  /* Data fetch/feed interval for codec in microseconds */
  uint32_t GetConfigDataIntervalUs() const;
  /* Audio bit depth required for codec */
  uint8_t GetConfigBitsPerSample() const;
  /* Audio channels number for stream */
  uint8_t GetConfigChannelCount() const;

 private:
  types::LeAudioCodecId id_;

  /* Codec Specific Configuration variant */
  std::variant<types::LeAudioLc3Config> config_;
};

/* Configuration strategy */
enum class LeAudioConfigurationStrategy : uint8_t {
  /* Common true wireless speakers */
  MONO_ONE_CIS_PER_DEVICE = 0x00,
  /* Requires 2 ASEs and 2 Audio Allocation for left/right */
  STEREO_TWO_CISES_PER_DEVICE = 0x01,
  /* Requires channel count 2*/
  STEREO_ONE_CIS_PER_DEVICE = 0x02,
  RFU = 0x03,
};

struct SetConfiguration {
  constexpr SetConfiguration(
      uint8_t direction, uint8_t device_cnt, uint8_t ase_cnt,
      CodecCapabilitySetting codec,
      LeAudioConfigurationStrategy strategy =
          LeAudioConfigurationStrategy::MONO_ONE_CIS_PER_DEVICE)
      : direction_(direction),
        device_cnt_(device_cnt),
        ase_cnt_(ase_cnt),
        codec_(codec),
        strategy_(strategy) {}

  uint8_t direction() const { return direction_; }
  uint8_t device_cnt() const { return device_cnt_; }
  uint8_t ase_cnt() const { return ase_cnt_; }
  const CodecCapabilitySetting& codec() const { return codec_; }
  LeAudioConfigurationStrategy strategy() const { return strategy_; }

 private:
  uint8_t direction_;  /* Direction of set */
  uint8_t device_cnt_; /* How many devices must be in set */
  uint8_t ase_cnt_;    /* How many ASE we need in configuration */
  CodecCapabilitySetting codec_;
  LeAudioConfigurationStrategy strategy_;
};

struct AudioSetConfiguration {
  const std::string name() const { return std::string(name_); }
  const std::vector<struct SetConfiguration>& confs() const { return confs_; }

  const char* name_;
  const std::vector<struct SetConfiguration> confs_;
};

struct AudioSetConfigurations {
  using const_iterator =
      typename std::vector<const AudioSetConfiguration*>::const_iterator;
  const_iterator begin(void) const { return items_.begin(); }
  const_iterator end(void) const { return items_.end(); }

  auto size() const { return items_.size(); }

  const std::vector<const AudioSetConfiguration*> items_;
};

struct StreamConfiguration {
  bool valid;

  types::LeAudioCodecId id;

  /* Pointer to chosen req */
  const le_audio::set_configurations::AudioSetConfiguration* conf;

  /* Sink configuration */
  /* For now we have always same frequency for all the channels */
  uint32_t sink_sample_frequency_hz;
  uint32_t sink_frame_duration_us;
  uint16_t sink_octets_per_codec_frame;
  /* Number of channels is what we will request from audio framework */
  uint8_t sink_num_of_channels;
  int sink_num_of_devices;
  /* cis_handle, audio location*/
  std::vector<std::pair<uint16_t, uint32_t>> sink_streams;

  /* Source configuration */
  /* For now we have always same frequency for all the channels */
  uint32_t source_sample_frequency_hz;
  uint32_t source_frame_duration_us;
  uint16_t source_octets_per_codec_frame;
  /* Number of channels is what we will request from audio framework */
  uint8_t source_num_of_channels;
  int source_num_of_devices;
  /* cis_handle, audio location*/
  std::vector<std::pair<uint16_t, uint32_t>> source_streams;
};

bool CheckIfMayCoverScenario(
    const AudioSetConfigurations* audio_set_configurations, uint8_t group_size);
bool CheckIfMayCoverScenario(
    const AudioSetConfiguration* audio_set_configuration, uint8_t group_size);
bool IsCodecCapabilitySettingSupported(
    const types::acs_ac_record& pac_record,
    const CodecCapabilitySetting& codec_capability_setting);
const AudioSetConfigurations* GetConfigurationsByType(
    ::le_audio::types::LeAudioContextType content_type);

}  // namespace set_configurations
}  // namespace le_audio
