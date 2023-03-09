/*
 * Copyright 2022 The Android Open Source Project
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

#include <frameworks/proto_logging/stats/enums/bluetooth/le/enums.pb.h>

#include <chrono>
#include <cstdint>
#include <memory>
#include <unordered_map>
#include <utility>
#include <vector>

#include "common/strings.h"
#include "os/metrics.h"
#include "hci/address.h"


namespace bluetooth {

namespace metrics {

using android::bluetooth::le::LEACLConnectionState;
using android::bluetooth::le::LEACLConnectionType;
using android::bluetooth::le::LEConnectionTransactionState;
using android::bluetooth::le::LETransactionOriginType;




using ClockTimePoint = std::chrono::time_point<std::chrono::high_resolution_clock>;

const static ClockTimePoint kInvalidTimePoint{};

inline int64_t get_timedelta_nanos(const ClockTimePoint& t1,
                                   const ClockTimePoint& t2) {
  if (t1 == kInvalidTimePoint || t2 == kInvalidTimePoint) {
    return -1;
  }
  return std::abs(
      std::chrono::duration_cast<std::chrono::nanoseconds>(t1 - t2).count());
}


class LEConnectionMetricState {
 public:
  hci::Address address;
  LEConnectionMetricState(const hci::Address address) : address(address) {}
  LEConnectionTransactionState state;
  LEACLConnectionState acl_state;
  android::bluetooth::hci::StatusEnum acl_status_code;
  ClockTimePoint start_timepoint = kInvalidTimePoint;
  ClockTimePoint end_timepoint = kInvalidTimePoint;
  bool is_cancelled = false;
  LETransactionOriginType connection_origin_type = LETransactionOriginType::ORIGIN_UNSPECIFIED;

  bool IsStarted();
  bool IsEnded();
  bool IsCancelled();

  void AddStateChangedEvent(
      LETransactionOriginType origin_type,
      LEACLConnectionType connection_type,
      LEConnectionTransactionState transaction_state,
      std::vector<std::pair<os::ArgumentType, int>> argument_list);
};

class LEConnectionMetricsRemoteDevice {
 public:
  void AddStateChangedEvent(
      const hci::Address& address,
      LETransactionOriginType origin_type,
      LEACLConnectionType connection_type,
      LEConnectionTransactionState transaction_state,
      std::vector<std::pair<os::ArgumentType, int>> argument_list);

 private:
  void UploadLEConnectionSession(const hci::Address& address);
  std::vector<std::unique_ptr<LEConnectionMetricState>> device_metrics;
  std::unordered_map<hci::Address, LEConnectionMetricState*> opened_devices;
};


class MetricsCollector {
 public:
  // getting the LE Metrics Collector
  static LEConnectionMetricsRemoteDevice* GetLEConnectionMetricsCollector();

 private:
  static LEConnectionMetricsRemoteDevice* le_connection_metrics_remote_device_;
};




}  // namespace metrics
}  // namespace bluetooth
