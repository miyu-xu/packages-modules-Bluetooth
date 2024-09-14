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
#include "hci/address.h"
#include "os/metrics.h"

namespace bluetooth {

namespace metrics {

using android::bluetooth::le::LeAclConnectionState;
using android::bluetooth::le::LeConnectionOriginType;
using android::bluetooth::le::LeConnectionState;
using android::bluetooth::le::LeConnectionType;

using ClockTimePoint = std::chrono::time_point<std::chrono::high_resolution_clock>;
const static ClockTimePoint kInvalidTimePoint{};

class BaseMetricsLoggerModule {
public:
  BaseMetricsLoggerModule() {}
  virtual void LogMetricBluetoothLESession(os::LEConnectionSessionOptions session_options) = 0;
  virtual ~BaseMetricsLoggerModule() {}
};

class MetricsLoggerModule : public BaseMetricsLoggerModule {
public:
  MetricsLoggerModule() {}
  void LogMetricBluetoothLESession(os::LEConnectionSessionOptions session_options);
  virtual ~MetricsLoggerModule() {}
};

class LEConnectionMetricState {
public:
  LEConnectionMetricState(const hci::Address address) : address_(address) {}

  hci::Address address_;
  LeAclConnectionState acl_connection_state_{LeAclConnectionState::LE_ACL_UNSPECIFIED};
  android::bluetooth::hci::StatusEnum acl_status_code_{android::bluetooth::hci::StatusEnum::STATUS_UNKNOWN};
  LeConnectionState connection_state_{LeConnectionState::STATE_UNSPECIFIED};
  LeConnectionType connection_type_{LeConnectionType::CONNECTION_TYPE_UNSPECIFIED};
  LeConnectionOriginType connection_origin_type_{LeConnectionOriginType::ORIGIN_UNSPECIFIED};
  ClockTimePoint start_timepoint_{kInvalidTimePoint};
  ClockTimePoint last_timepoint_{kInvalidTimePoint};
  bool is_cancelled{false};

  bool IsStarted() const;
  bool IsEnded() const;
  bool IsCancelled() const;

  void AddStateChangedEvent(LeConnectionOriginType origin_type, LeConnectionType connection_type,
                            LeConnectionState transaction_state,
                            std::vector<std::pair<os::ArgumentType, int>> const& argument_list);
};

class LEConnectionMetricsRemoteDevice {
public:
  LEConnectionMetricsRemoteDevice();
  LEConnectionMetricsRemoteDevice(BaseMetricsLoggerModule* baseMetricsLoggerModule);

  void AddStateChangedEvent(const hci::Address& address, LeConnectionOriginType origin_type,
                            LeConnectionType connection_type, LeConnectionState transaction_state,
                            std::vector<std::pair<os::ArgumentType, int>> const& argument_list);

  void UploadLEConnectionSession(const hci::Address& address);

private:
  mutable std::mutex opened_devices_mutex_;
  std::vector<std::unique_ptr<LEConnectionMetricState>> device_metrics_;
  std::unordered_map<hci::Address, LEConnectionMetricState*> opened_devices_;
  BaseMetricsLoggerModule* metrics_logger_module_;
};

class MetricsCollector {
public:
  // getting the LE Connection Metrics Collector
  static LEConnectionMetricsRemoteDevice* GetLEConnectionMetricsCollector();

private:
  static LEConnectionMetricsRemoteDevice* le_connection_metrics_remote_device;
};

}  // namespace metrics
}  // namespace bluetooth
