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

#include "metrics_state.h"

#include <bluetooth/log.h>
#include <frameworks/proto_logging/stats/enums/bluetooth/hci/enums.pb.h>
#include <frameworks/proto_logging/stats/enums/bluetooth/le/enums.pb.h>

#include <chrono>
#include <climits>
#include <memory>
#include <unordered_map>
#include <utility>

#include "common/strings.h"
#include "hci/address.h"
#include "metrics/utils.h"
#include "os/log.h"
#include "os/metrics.h"

namespace bluetooth::metrics {

using android::bluetooth::le::LeConnectionOriginType;
using android::bluetooth::le::LeConnectionState;
using android::bluetooth::le::LeConnectionType;

inline int64_t get_timedelta_nanos(const ClockTimePoint& t1, const ClockTimePoint& t2) {
  if (t1 == kInvalidTimePoint || t2 == kInvalidTimePoint) {
    return -1;
  }
  return std::abs(std::chrono::duration_cast<std::chrono::nanoseconds>(t2 - t1).count());
}

/*
 * This is the device level metrics state, which will be modified based on
 * incoming state events.
 */
void LEConnectionMetricState::AddStateChangedEvent(
        LeConnectionOriginType connection_origin_type, LeConnectionType connection_type,
        LeConnectionState connection_state,
        std::vector<std::pair<os::ArgumentType, int>> const& argument_list) {

  ClockTimePoint current_timestamp = std::chrono::high_resolution_clock::now();

  connection_state_ = connection_state;

  if (connection_origin_type_ == LeConnectionOriginType::ORIGIN_UNSPECIFIED) {
    connection_origin_type_ = origin_type;
  }

  if (connection_type_ == LeConnectionType::CONNECTION_TYPE_UNSPECIFIED) {
    connection_type_ = connection_type;
  }

  if (start_timepoint_ == kInvalidTimePoint) {
    start_timepoint_ = current_timestamp;
  }

  end_timepoint_ = current_timestamp;

  switch (connection_state_) {
    case LeConnectionState::STATE_LE_ACL_START: {
      int connection_type_cid = GetArgumentTypeFromList(argument_list, os::ArgumentType::L2CAP_CID);
      LeConnectionType connection_type = GetLeConnectionTypeFromCID(connection_type_cid);
      if (connection_type != LeConnectionType::CONNECTION_TYPE_UNSPECIFIED) {
        log::info("LEConnectionMetricsRemoteDevice: Populating the connection type");
        connection_type_ = connection_type;
      }
      break;
    }

    case LeConnectionState::STATE_LE_ACL_END: {
      int acl_status_code = GetArgumentTypeFromList(argument_list, os::ArgumentType::ACL_STATUS_CODE);
      acl_status_code_ = static_cast<android::bluetooth::hci::StatusEnum>(acl_status_code);
      acl_connection_state_ =
        acl_status_code_ == android::bluetooth::hci::StatusEnum::STATUS_SUCCESS ?
          LeAclConnectionState::LE_ACL_SUCCESS : LeAclConnectionState::LE_ACL_FAILED;
      break;
    }

    case LeConnectionState::STATE_LE_ACL_TIMEOUT: {
      int acl_status_code = GetArgumentTypeFromList(argument_list, os::ArgumentType::ACL_STATUS_CODE);
      acl_status_code_ = static_cast<android::bluetooth::hci::StatusEnum>(acl_status_code);
      acl_connection_state_ = LeAclConnectionState::LE_ACL_FAILED;
      break;
    }

    case LeConnectionState::STATE_LE_ACL_CANCEL: {
      acl_connection_state_ = LeAclConnectionState::LE_ACL_FAILED;
      is_cancelled = true;
      break;
    }

    default:
      break;
  }
}

bool LEConnectionMetricState::IsEnded() const {
  return acl_connection_state_ == LeAclConnectionState::LE_ACL_SUCCESS ||
         acl_connection_state_ == LeAclConnectionState::LE_ACL_FAILED;
}

bool LEConnectionMetricState::IsStarted() const {
  return connection_state_ == LeConnectionState::STATE_LE_ACL_START;
}

bool LEConnectionMetricState::IsCancelled() const { return is_cancelled; }

LEConnectionMetricsRemoteDevice::LEConnectionMetricsRemoteDevice()
  : metrics_logger_module_(new MetricsLoggerModule()) {}

LEConnectionMetricsRemoteDevice::LEConnectionMetricsRemoteDevice(
        BaseMetricsLoggerModule* metrics_logger_module)
  : metrics_logger_module_(metrics_logger_module) {}

void LEConnectionMetricsRemoteDevice::UploadLEConnectionSession(const hci::Address& address) {
  auto it = opened_devices_.find(address);
  if (it == opened_devices_.end()) {
    return;
  }

  auto latency = get_timedelta_nanos(it->second->start_timepoint, it->second->end_timepoint);
  os::LEConnectionSessionOptions session_options = {
      .acl_connection_state_ = it->second->acl_connection_state_,
      .origin_type = it->second->connection_origin_type_,
      .transaction_type = it->second->connection_type_,
      .latency = latency,
      .remote_address = address,
      .status = it->second->acl_status_code_,
      // TODO: keep the acl latency the same as the overall latency for now
      // When more events are added, we will an overall latency
      .acl_latency = latency,
      .is_cancelled = it->second->is_cancelled_,
  };

  metrics_logger_module_->LogMetricBluetoothLESession(session_options);
  opened_devices_.erase(it);

  log::info("LEConnectionMetricsRemoteDevice: The session is uploaded for {}", address);
}

void LEConnectionMetricsRemoteDevice::AddStateChangedEvent(
        const hci::Address& address, LeConnectionOriginType origin_type,
        LeConnectionType connection_type, LeConnectionState transaction_state,
        std::vector<std::pair<os::ArgumentType, int>> const& argument_list) {
  log::info(
          "LEConnectionMetricsRemoteDevice: Address {}, Transaction State 0x{:x}, Connection Type 0x{:x},"
          " Origin Type 0x{:x}", address, transaction_state, connection_type, origin_type);

  std::unique_lock<std::mutex> lock(opened_devices_mutex_);

  if (address.IsEmpty()) {
    for (auto& device_metric : device_metrics_) {
      if (device_metric->IsStarted() &&
          transaction_state == LeConnectionState::STATE_LE_ACL_CANCEL) {
        log::info("LEConnectionMetricsRemoteDevice: Cancellation Begin");
        // cancel the connection
        device_metric->AddStateChangedEvent(origin_type, connection_type, transaction_state,
                                            argument_list);
        continue;
      }

      if (device_metric->IsCancelled() &&
          transaction_state == LeConnectionState::STATE_LE_ACL_END) {
        // complete the connection
        device_metric->AddStateChangedEvent(origin_type, connection_type, transaction_state,
                                            argument_list);
        UploadLEConnectionSession(address);
        continue;
      }
    }

    return;
  }

  auto it = opened_devices_.find(address);
  if (it == opened_devices_.end()) {
    device_metrics_.push_back(std::make_unique<LEConnectionMetricState>(address));
    it = opened_devices_.insert(std::begin(opened_devices_), {address, device_metrics_.back().get()});
  }

  it->second->AddStateChangedEvent(origin_type, connection_type, transaction_state, argument_list);

  if (it->second->IsEnded()) {
    UploadLEConnectionSession(address);
  }
}

void MetricsLoggerModule::LogMetricBluetoothLESession(
        os::LEConnectionSessionOptions session_options) {
  os::LogMetricBluetoothLEConnection(session_options);
}

// Instance of Metrics Collector for LEConnectionMetricsRemoteDeviceImpl
LEConnectionMetricsRemoteDevice* MetricsCollector::le_connection_metrics_remote_device =
        new LEConnectionMetricsRemoteDevice();

LEConnectionMetricsRemoteDevice* MetricsCollector::GetLEConnectionMetricsCollector() {
  return MetricsCollector::le_connection_metrics_remote_device;
}

}  // namespace bluetooth::metrics
