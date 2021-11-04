/*
 * Copyright 2019 HIMSA II K/S - www.himsa.com. Represented by EHIMA -
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

#pragma once

#include <array>
#include <optional>

#include "bt_av.h"
#include "raw_address.h"

namespace bluetooth {
namespace le_audio {

using btle_audio_codec_priority_t = bt_codec_priority_t;
using btle_audio_codec_sample_rate_t = bt_codec_sample_rate_t;
using btle_audio_codec_bits_per_sample_t = bt_codec_bits_per_sample_t;
using btle_audio_codec_channel_mode_t = bt_codec_channel_mode_t;

enum class ConnectionState {
  DISCONNECTED = 0,
  CONNECTING,
  CONNECTED,
  DISCONNECTING
};

enum class GroupStatus {
  INACTIVE = 0,
  ACTIVE,
};

enum class GroupStreamStatus {
  IDLE = 0,
  STREAMING,
  SUSPENDED,
  RECONFIGURED,
  DESTROYED,
};

enum class GroupNodeStatus {
  ADDED = 1,
  REMOVED,
};

typedef enum {
  LE_AUDIO_CODEC_INDEX_SOURCE_LC3 = BT_CODEC_INDEX_SOURCE_LC3
} btle_audio_codec_index_t;

typedef struct {
  btle_audio_codec_index_t codec_type;
  btle_audio_codec_priority_t
      codec_priority;  // Codec selection priority
                       // relative to other codecs: larger value
                       // means higher priority. If 0, reset to
                       // default.
  btle_audio_codec_sample_rate_t sample_rate;
  btle_audio_codec_bits_per_sample_t bits_per_sample;
  btle_audio_codec_channel_mode_t channel_mode;
  int64_t codec_specific_1;  // Codec-specific value 1
  int64_t codec_specific_2;  // Codec-specific value 2
  int64_t codec_specific_3;  // Codec-specific value 3
  int64_t codec_specific_4;  // Codec-specific value 4

  std::string ToString() const {
    std::string codec_name_str;

    switch (codec_type) {
      case LE_AUDIO_CODEC_INDEX_SOURCE_LC3:
        codec_name_str = "LC3";
        break;
      default:
        codec_name_str = "Unknown LE codec " + std::to_string(codec_type);
        break;
    }

    std::string sample_rate_str;
    AppendCapability(sample_rate_str,
                     (sample_rate == BT_CODEC_SAMPLE_RATE_NONE), "NONE");
    AppendCapability(sample_rate_str,
                     (sample_rate & BT_CODEC_SAMPLE_RATE_44100), "44100");
    AppendCapability(sample_rate_str,
                     (sample_rate & BT_CODEC_SAMPLE_RATE_48000), "48000");
    AppendCapability(sample_rate_str,
                     (sample_rate & BT_CODEC_SAMPLE_RATE_88200), "88200");
    AppendCapability(sample_rate_str,
                     (sample_rate & BT_CODEC_SAMPLE_RATE_96000), "96000");
    AppendCapability(sample_rate_str,
                     (sample_rate & BT_CODEC_SAMPLE_RATE_176400), "176400");
    AppendCapability(sample_rate_str,
                     (sample_rate & BT_CODEC_SAMPLE_RATE_192000), "192000");
    AppendCapability(sample_rate_str,
                     (sample_rate & BT_CODEC_SAMPLE_RATE_16000), "16000");
    AppendCapability(sample_rate_str,
                     (sample_rate & BT_CODEC_SAMPLE_RATE_24000), "24000");

    std::string bits_per_sample_str;
    AppendCapability(bits_per_sample_str,
                     (bits_per_sample == BT_CODEC_BITS_PER_SAMPLE_NONE),
                     "NONE");
    AppendCapability(bits_per_sample_str,
                     (bits_per_sample & BT_CODEC_BITS_PER_SAMPLE_16), "16");
    AppendCapability(bits_per_sample_str,
                     (bits_per_sample & BT_CODEC_BITS_PER_SAMPLE_24), "24");
    AppendCapability(bits_per_sample_str,
                     (bits_per_sample & BT_CODEC_BITS_PER_SAMPLE_32), "32");

    std::string channel_mode_str;
    AppendCapability(channel_mode_str,
                     (channel_mode == BT_CODEC_CHANNEL_MODE_NONE), "NONE");
    AppendCapability(channel_mode_str,
                     (channel_mode & BT_CODEC_CHANNEL_MODE_MONO), "MONO");
    AppendCapability(channel_mode_str,
                     (channel_mode & BT_CODEC_CHANNEL_MODE_STEREO), "STEREO");

    return "codec: " + codec_name_str +
           " priority: " + std::to_string(codec_priority) +
           " sample_rate: " + sample_rate_str +
           " bits_per_sample: " + bits_per_sample_str +
           " channel_mode: " + channel_mode_str +
           " codec_specific_1: " + std::to_string(codec_specific_1) +
           " codec_specific_2: " + std::to_string(codec_specific_2) +
           " codec_specific_3: " + std::to_string(codec_specific_3) +
           " codec_specific_4: " + std::to_string(codec_specific_4);
  }

 private:
  static void AppendCapability(std::string& result, bool append,
                               const std::string& name) {
    if (!append) return;
    result += result.empty() ? name : "|" + name;
  }

} btle_audio_codec_config_t;

class LeAudioClientCallbacks {
 public:
  virtual ~LeAudioClientCallbacks() = default;

  /** Callback for profile connection state change */
  virtual void OnConnectionState(ConnectionState state,
                                 const RawAddress& address) = 0;

  /* Callback with group status update */
  virtual void OnGroupStatus(int group_id, GroupStatus group_status) = 0;

  /* Callback with node status update */
  virtual void OnGroupNodeStatus(const RawAddress& bd_addr, int group_id,
                                 GroupNodeStatus node_status) = 0;
  /* Callback for newly recognized or reconfigured existing le audio group */
  virtual void OnAudioConf(uint8_t direction, int group_id,
                           uint32_t snk_audio_location,
                           uint32_t src_audio_location,
                           uint16_t avail_cont) = 0;
};

class LeAudioClientInterface {
 public:
  virtual ~LeAudioClientInterface() = default;

  /* Register the LeAudio callbacks */
  virtual void Initialize(
      LeAudioClientCallbacks* callbacks,
      const std::vector<btle_audio_codec_config_t>& offloading_preference) = 0;

  /** Connect to LEAudio */
  virtual void Connect(const RawAddress& address) = 0;

  /** Disconnect from LEAudio */
  virtual void Disconnect(const RawAddress& address) = 0;

  /* Cleanup the LeAudio */
  virtual void Cleanup(void) = 0;

  /* Called when LeAudio is unbonded. */
  virtual void RemoveDevice(const RawAddress& address) = 0;

  /* Attach le audio node to group */
  virtual void GroupAddNode(int group_id, const RawAddress& addr) = 0;

  /* Detach le audio node from a group */
  virtual void GroupRemoveNode(int group_id, const RawAddress& addr) = 0;

  /* Set active le audio group */
  virtual void GroupSetActive(int group_id) = 0;
};

static constexpr uint8_t INSTANCE_ID_UNDEFINED = 0xFF;

} /* namespace le_audio */
} /* namespace bluetooth */
