/*
 * Copyright 2020 HIMSA II K/S - www.himsa.com. Represented by EHIMA -
 * www.ehima.com
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

/*
 * This file contains definitions for Basic Audio Profile / Audio Stream Control
 * and Published Audio Capabilities definitions, structures etc.
 */

#include "le_audio_types.h"

#include <base/strings/string_number_conversions.h>

#include "bt_types.h"
#include "bta_api.h"
#include "bta_le_audio_api.h"
#include "client_audio.h"
#include "client_parser.h"
#include "le_audio_set_configurations.h"

namespace le_audio {
using types::LeAudioContextType;
namespace types {
/* Helper map for matching various frequency notations */
const std::map<uint8_t, uint32_t> LeAudioLc3Config::sampling_freq_map = {
    {codec_spec_conf::kLeAudioSamplingFreq8000Hz,
     LeAudioCodecConfiguration::kSampleRate8000},
    {codec_spec_conf::kLeAudioSamplingFreq16000Hz,
     LeAudioCodecConfiguration::kSampleRate16000},
    {codec_spec_conf::kLeAudioSamplingFreq24000Hz,
     LeAudioCodecConfiguration::kSampleRate24000},
    {codec_spec_conf::kLeAudioSamplingFreq32000Hz,
     LeAudioCodecConfiguration::kSampleRate32000},
    {codec_spec_conf::kLeAudioSamplingFreq44100Hz,
     LeAudioCodecConfiguration::kSampleRate44100},
    {codec_spec_conf::kLeAudioSamplingFreq48000Hz,
     LeAudioCodecConfiguration::kSampleRate48000}};

/* Helper map for matching various frame durations notations */
const std::map<uint8_t, uint32_t> LeAudioLc3Config::frame_duration_map = {
    {codec_spec_conf::kLeAudioCodecLC3FrameDur7500us,
     LeAudioCodecConfiguration::kInterval7500Us},
    {codec_spec_conf::kLeAudioCodecLC3FrameDur10000us,
     LeAudioCodecConfiguration::kInterval10000Us}};

std::optional<std::vector<uint8_t>> LeAudioLtvMap::Find(uint8_t type) const {
  auto iter =
      std::find_if(values.cbegin(), values.cend(),
                   [type](const auto& value) { return value.first == type; });

  if (iter == values.cend()) return std::nullopt;

  return iter->second;
}

uint8_t* LeAudioLtvMap::RawPacket(uint8_t* p_buf) const {
  for (auto const& value : values) {
    UINT8_TO_STREAM(p_buf, value.second.size() + 1);
    UINT8_TO_STREAM(p_buf, value.first);
    ARRAY_TO_STREAM(p_buf, value.second.data(),
                    static_cast<int>(value.second.size()));
  }

  return p_buf;
}

LeAudioLtvMap LeAudioLtvMap::Parse(const uint8_t* p_value, uint8_t len,
                                   bool& success) {
  LeAudioLtvMap ltv_map;

  if (len > 0) {
    const auto p_value_end = p_value + len;

    while ((p_value_end - p_value) > 0) {
      uint8_t ltv_len;
      STREAM_TO_UINT8(ltv_len, p_value);

      // Unusual, but possible case
      if (ltv_len == 0) continue;

      if (p_value_end < (p_value + ltv_len)) {
        LOG(ERROR) << __func__
                   << " Invalid ltv_len: " << static_cast<int>(ltv_len);
        success = false;
        return LeAudioLtvMap();
      }

      uint8_t ltv_type;
      STREAM_TO_UINT8(ltv_type, p_value);
      ltv_len -= sizeof(ltv_type);

      const auto p_temp = p_value;
      p_value += ltv_len;

      std::vector<uint8_t> ltv_value(p_temp, p_value);
      ltv_map.values.emplace(ltv_type, std::move(ltv_value));
    }
  }

  success = true;
  return ltv_map;
}

size_t LeAudioLtvMap::RawPacketSize() const {
  size_t bytes = 0;

  for (auto const& value : values) {
    bytes += (/* ltv_len + ltv_type */ 2 + value.second.size());
  }

  return bytes;
}

std::string LeAudioLtvMap::ToString() const {
  std::string debug_str;

  for (const auto& value : values) {
    std::stringstream sstream;

    sstream << "\ttype: " << std::to_string(value.first)
            << "\tlen: " << std::to_string(value.second.size()) << "\tdata: "
            << base::HexEncode(value.second.data(), value.second.size()) + "\n";

    debug_str += sstream.str();
  }

  return debug_str;
}

}  // namespace types

void AppendMetadataLtvEntryForCcidList(std::vector<uint8_t>& metadata,
                                       LeAudioContextType context_type) {
  std::vector<uint8_t> ccid_ltv_entry;
  /* TODO: Get CCID values from Service */
  std::vector<uint8_t> ccid_conversational = {0x12};
  std::vector<uint8_t> ccid_media = {0x56};

  std::vector<uint8_t>* ccid_value = nullptr;

  /* CCID list */
  switch (context_type) {
    case LeAudioContextType::CONVERSATIONAL:
      ccid_value = &ccid_conversational;
      break;
    case LeAudioContextType::MEDIA:
      ccid_value = &ccid_media;
      break;
    default:
      break;
  }

  if (!ccid_value) return;

  ccid_ltv_entry.push_back(static_cast<uint8_t>(types::kLeAudioMetadataTypeLen +
                                                ccid_value->size()));
  ccid_ltv_entry.push_back(
      static_cast<uint8_t>(types::kLeAudioMetadataTypeCcidList));
  ccid_ltv_entry.insert(ccid_ltv_entry.end(), ccid_value->begin(),
                        ccid_value->end());

  metadata.insert(metadata.end(), ccid_ltv_entry.begin(), ccid_ltv_entry.end());
}

void AppendMetadataLtvEntryForStreamingContext(
    std::vector<uint8_t>& metadata, LeAudioContextType context_type) {
  std::vector<uint8_t> streaming_context_ltv_entry;

  streaming_context_ltv_entry.resize(
      types::kLeAudioMetadataTypeLen + types::kLeAudioMetadataLenLen +
      types::kLeAudioMetadataStreamingAudioContextLen);
  uint8_t* streaming_context_ltv_entry_buf = streaming_context_ltv_entry.data();

  UINT8_TO_STREAM(streaming_context_ltv_entry_buf,
                  types::kLeAudioMetadataTypeLen +
                      types::kLeAudioMetadataStreamingAudioContextLen);
  UINT8_TO_STREAM(streaming_context_ltv_entry_buf,
                  types::kLeAudioMetadataTypeStreamingAudioContext);
  UINT16_TO_STREAM(streaming_context_ltv_entry_buf,
                   static_cast<uint16_t>(context_type));

  metadata.insert(metadata.end(), streaming_context_ltv_entry.begin(),
                  streaming_context_ltv_entry.end());
}

}  // namespace le_audio

std::ostream& operator<<(std::ostream& os,
                         const le_audio::types::LeAudioLc3Config& config) {
  os << " LeAudioLc3Config(SamplFreq=" << loghex(config.sampling_frequency)
     << ", FrameDur=" << loghex(config.frame_duration)
     << ", OctetsPerFrame=" << int(config.octets_per_codec_frame)
     << ", AudioChanLoc=" << loghex(config.audio_channel_allocation) << ")";
  return os;
}

std::ostream& operator<<(std::ostream& os,
                         const le_audio::types::AseState& state) {
  static const char* char_value_[7] = {
      "IDLE",      "CODEC_CONFIGURED", "QOS_CONFIGURED", "ENABLING",
      "STREAMING", "DISABLING",        "RELEASING",
  };

  os << char_value_[static_cast<uint8_t>(state)] << " ("
     << "0x" << std::setfill('0') << std::setw(2) << static_cast<int>(state)
     << ")";
  return os;
}
