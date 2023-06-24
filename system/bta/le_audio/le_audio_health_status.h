/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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

#include <base/functional/callback.h>

#include <ostream>

#include "hardware/bt_le_audio.h"
#include "types/raw_address.h"

using bluetooth::le_audio::LeAudioHealthBasedAction;

namespace le_audio {
using LeAudioRecommendationActionCb = base::RepeatingCallback<void(
    const RawAddress& address, int group_id, LeAudioHealthBasedAction action)>;

enum class LeAudioHealthDeviceStatType {
  INVALID_DB = 0,
  VALID_DB,
  INVALID_CSIS,
  VALID_CSIS,
};

enum class LeAudioHealthGroupStatType {
  STREAM_CREATE_SUCCESS,
  STREAM_CREATE_CIS_FAILED,
  STREAM_CREATE_SIGNALING_FAILED,
};

class LeAudioHealthStatus {
 public:
  virtual ~LeAudioHealthStatus(void) = default;
  static LeAudioHealthStatus* Get(void);
  static void Cleanup(void);
  static void DebugDump(int fd);

  virtual void RegisterCallback(LeAudioRecommendationActionCb cb) = 0;
  virtual void AddStatisticForDevice(const RawAddress& address,
                                     LeAudioHealthDeviceStatType type) = 0;
  virtual void AddStatisticForGroup(int group_id,
                                    LeAudioHealthGroupStatType type) = 0;
  virtual void RemoveStatistics(const RawAddress& address, int group) = 0;

  struct group_stats {
    group_stats(int group_id)
        : group_id_(group_id),
          latest_recommendation_(LeAudioHealthBasedAction::NONE),
          stream_success_cnt_(0),
          stream_failures_cnt_(0),
          stream_cis_failures_cnt_(0),
          stream_signaling_failures_cnt_(0){};

    int group_id_;
    LeAudioHealthBasedAction latest_recommendation_;

    int stream_success_cnt_;
    int stream_failures_cnt_;
    int stream_cis_failures_cnt_;
    int stream_signaling_failures_cnt_;
  };

  struct device_stats {
    device_stats(RawAddress address)
        : address_(address),
          latest_recommendation_(LeAudioHealthBasedAction::NONE),
          is_valid_service_(true),
          is_valid_group_member_(true){};
    RawAddress address_;
    LeAudioHealthBasedAction latest_recommendation_;

    bool is_valid_service_;
    bool is_valid_group_member_;
  };
};

inline std::ostream& operator<<(
    std::ostream& os, const le_audio::LeAudioHealthGroupStatType& stat) {
  switch (stat) {
    case le_audio::LeAudioHealthGroupStatType::STREAM_CREATE_SUCCESS:
      os << "STREAM_CREATE_SUCCESS";
      break;
    case le_audio::LeAudioHealthGroupStatType::STREAM_CREATE_CIS_FAILED:
      os << "STREAM_CREATE_CIS_FAILED";
      break;
    case le_audio::LeAudioHealthGroupStatType::STREAM_CREATE_SIGNALING_FAILED:
      os << "STREAM_CREATE_SIGNALING_FAILED";
      break;
    default:
      os << "UNKNOWN";
      break;
  }
  return os;
}

inline std::ostream& operator<<(
    std::ostream& os, const le_audio::LeAudioHealthDeviceStatType& stat) {
  switch (stat) {
    case le_audio::LeAudioHealthDeviceStatType::INVALID_DB:
      os << "INVALID_DB";
      break;
    case le_audio::LeAudioHealthDeviceStatType::VALID_DB:
      os << "VALID_DB";
      break;
    case le_audio::LeAudioHealthDeviceStatType::INVALID_CSIS:
      os << "INVALID_CSIS";
      break;
    case le_audio::LeAudioHealthDeviceStatType::VALID_CSIS:
      os << "VALID_CSIS";
      break;
    default:
      os << "UNKNOWN";
      break;
  }
  return os;
}
}  // namespace le_audio