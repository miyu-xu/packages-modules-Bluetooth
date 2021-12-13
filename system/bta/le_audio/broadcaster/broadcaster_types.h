/*
 * Copyright 2021 HIMSA II K/S - www.himsa.com.
 * Represented by EHIMA - www.ehima.com
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

#pragma once

#include <variant>

#include "../client_audio.h"
#include "../le_audio_types.h"
#include "bta_le_audio_api.h"
#include "bta_le_audio_broadcaster_api.h"
#include "embdrv/lc3/Api/Lc3Config.hpp"

/* Types used internally by various modules of the broadcaster but not exposed
 * in the API.
 */

namespace le_audio {
namespace broadcaster {
static const uint16_t kBroadcastAudioAnnouncementServiceUuid = 0x8FDD;
static const uint16_t kBasicAudioAnnouncementServiceUuid = 0x8FDC;

struct BasicAudioAnnouncementCodecConfig {
  /* 5 octets for the Codec ID */
  uint8_t codec_id;
  uint16_t vendor_company_id;
  uint16_t vendor_codec_id;

  /* Codec params - series of LTV formatted triplets */
  std::vector<uint8_t> codec_specific_params;
};

struct BasicAudioAnnouncementBisConfig {
  std::vector<uint8_t> codec_specific_params;
  uint8_t bis_index;
};

struct BasicAudioAnnouncementSubgroup {
  /* Subgroup specific codec configuration and metadata */
  BasicAudioAnnouncementCodecConfig codec_config;
  std::vector<uint8_t> metadata;
  std::vector<BasicAudioAnnouncementBisConfig> bis_configs;
};

struct BasicAudioAnnouncementData {
  /* Announcement Header fields */
  uint32_t presentation_delay;

  /* Subgroup specific configurations */
  std::vector<BasicAudioAnnouncementSubgroup> subgroup_configs;

  bool FromRawPacket(const uint8_t* p_value, uint8_t len);
  bool ToRawPacket(std::vector<uint8_t>& data) const;
};

void PrepareAdvertisingData(bluetooth::le_audio::BroadcastId& broadcast_id,
                            std::vector<uint8_t>& periodic_data);
void PreparePeriodicData(const BasicAudioAnnouncementData& announcement,
                         std::vector<uint8_t>& periodic_data);

struct BroadcastCodecWrapper {
  BroadcastCodecWrapper(types::LeAudioCodecId codec_id,
                        LeAudioCodecConfiguration source_codec_config,
                        uint32_t codec_bitrate)
      : codec_id(codec_id),
        source_codec_config(source_codec_config),
        codec_bitrate(codec_bitrate),
        /* WARNING: The assumption is that we use LC3 encoder in a
         *          single-channel mode. See SupportsMultichannelEncoding().
         */
        source_encoder_config(
            Lc3Config(source_codec_config.sample_rate,
                      LC3::GetFrameDurationFromDataInterval(
                          source_codec_config.data_interval_us),
                      1)) {
    if (codec_id.coding_format != types::kLeAudioCodingFormatLC3)
      LOG(ERROR) << "Unsupported coding format!";
  }

  /* We need this copy-assignment operator as we currently use global copy of a
   * wrapper for the currently active Broadcast. Maybe we should consider using
   * shared pointer instead.
   */
  BroadcastCodecWrapper& operator=(const BroadcastCodecWrapper& other) {
    codec_id = other.codec_id;
    source_codec_config = other.source_codec_config;
    codec_bitrate = other.codec_bitrate;

    /* WARNING: The assumption is that we use LC3 encoder in a
     *          single-channel mode. See SupportsMultichannelEncoding().
     */
    source_encoder_config.emplace<0>(
        other.source_codec_config.sample_rate,
        LC3::GetFrameDurationFromDataInterval(
            other.source_codec_config.data_interval_us),
        1);

    return *this;
  };

  bool SupportsMultichannelEncoding() const {
    if (codec_id.coding_format != types::kLeAudioCodingFormatLC3) {
      LOG(ERROR) << "Unsupported coding format!";
    }

    return false;
  }

  static const BroadcastCodecWrapper& getCodecConfigForProfile(
      LeAudioBroadcaster::AudioProfile profile);

  std::vector<uint8_t> GetCodecSpecData() const;

  uint16_t GetMaxSduSize() const {
    if (codec_id.coding_format == types::kLeAudioCodingFormatLC3) {
      return std::get<Lc3Config>(source_encoder_config)
          .getByteCountFromBitrate(codec_bitrate);
    }

    LOG(ERROR) << "Invalid codec ID: "
               << "[" << +codec_id.coding_format << ":"
               << +codec_id.vendor_company_id << ":"
               << +codec_id.vendor_codec_id << "]";
    return 0;
  }

  const LeAudioCodecConfiguration& GetLeAudioCodecConfiguration() const {
    return source_codec_config;
  }

  const types::LeAudioCodecId& GetLeAudioCodecId() const { return codec_id; }

  uint8_t GetNumChannels() const { return source_codec_config.num_channels; }

  uint32_t GetBitrate() const { return codec_bitrate; }

  const std::variant<Lc3Config>& GetEncoderConfig() const {
    return source_encoder_config;
  }

 private:
  types::LeAudioCodecId codec_id;
  LeAudioCodecConfiguration source_codec_config;
  uint32_t codec_bitrate;
  std::variant<Lc3Config> source_encoder_config;

  /* LC3 Codec specific helpers. */
  struct LC3 {
    static Lc3Config::FrameDuration GetFrameDurationFromDataInterval(
        uint16_t interval) {
      switch (interval) {
        case LeAudioCodecConfiguration::kInterval7500Us:
          return Lc3Config::FrameDuration::d7p5ms;
        case LeAudioCodecConfiguration::kInterval10000Us:
          return Lc3Config::FrameDuration::d10ms;
        default:
          return Lc3Config::FrameDuration::d10ms;
      }
    }
  };
};
}  // namespace broadcaster
}  // namespace le_audio

std::ostream& operator<<(
    std::ostream& os,
    const le_audio::broadcaster::BroadcastCodecWrapper& config);
