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

#include <frameworks/proto_logging/stats/enums/bluetooth/hci/enums.pb.h>
#include <frameworks/proto_logging/stats/enums/bluetooth/le/enums.pb.h>

#include <chrono>
#include <climits>
#include <memory>
#include <unordered_map>
#include <utility>

#include "common/strings.h"
#include "hci/address.h"
#include "os/log.h"
#include "metrics/utils.h"

namespace bluetooth {
namespace metrics {

using android::bluetooth::le::LEConnectionType;
using android::bluetooth::le::LEConnectionState;
using android::bluetooth::le::LEConnectionOriginType;

// const static ClockTimePoint kInvalidTimePoint{};

/*
 * This is the device level metrics state, which will be modified based on
 * incoming state events.
 *
 */

void LEConnectionMetricState::AddStateChangedEvent(
    LEConnectionOriginType origin_type,
    LEConnectionType connection_type,
    LEConnectionState transaction_state,
    std::vector<std::pair<os::ArgumentType, int>> argument_list) {
  LOG_INFO(
      "LEConnectionMetricState:  Origin Type: %s, Connection Type: %s, Transaction State: "
      "%s",
      common::ToHexString(origin_type).c_str(),
      common::ToHexString(connection_type).c_str(),
      common::ToHexString(transaction_state).c_str());

  ClockTimePoint current_timestamp = std::chrono::high_resolution_clock::now();
  state = transaction_state;

  // Assign the origin of the connection
  if (connection_origin_type != LEConnectionOriginType::ORIGIN_UNSPECIFIED) {
    connection_origin_type = origin_type;
  }

  if (start_timepoint != kInvalidTimePoint) {
    start_timepoint = current_timestamp;
  }
  end_timepoint = current_timestamp;

  switch (transaction_state) {
    case LEConnectionState::STATE_LE_ACL_END: {
      int acl_status_code_from_args = GetArgumentTypeFromList(argument_list, os::ArgumentType::ACL_STATUS_CODE);
      acl_status_code = static_cast<android::bluetooth::hci::StatusEnum>(acl_status_code_from_args);
      acl_state = LEACLConnectionState::LE_ACL_SUCCESS;

      if (acl_status_code != android::bluetooth::hci::StatusEnum::STATUS_SUCCESS) {
        acl_state = LEACLConnectionState::LE_ACL_FAILED;
      }
      break;
    }
    case LEConnectionState::STATE_LE_ACL_CANCEL: {
      acl_state = LEACLConnectionState::LE_ACL_FAILED;
      is_cancelled = true;
      break;
    }
      [[fallthrough]];
    default: {
      // do nothing
    }
  }
}

bool LEConnectionMetricState::IsEnded() {
  return acl_state == LEACLConnectionState::LE_ACL_SUCCESS || acl_state == LEACLConnectionState::LE_ACL_FAILED;
}

bool LEConnectionMetricState::IsStarted() {
  return state == LEConnectionState::STATE_LE_ACL_START;
}

bool LEConnectionMetricState::IsCancelled() {
  return is_cancelled;
}

// Flush the state
void LEConnectionMetricState::Flush() {
  auto argument_list = std::vector<std::pair<os::ArgumentType, int>>();
  AddStateChangedEvent(LEConnectionOriginType::ORIGIN_UNSPECIFIED,
                       LEConnectionType::CONNECTION_TYPE_UNSPECIFIED,
                       LEConnectionState::STATE_LE_ACL_END,
                       argument_list);
}

// Uploading the session
void LEConnectionMetricsRemoteDevice::UploadLEConnectionSession(const hci::Address& address) {
    auto it = opened_devices.find(address);
    if (it != opened_devices.end()) {
      os::LEConnectionSessionOptions session_options;
      session_options.acl_connection_state = it->second->acl_state;
      session_options.origin_type = it->second->connection_origin_type;
      session_options.latency = bluetooth::metrics::get_timedelta_nanos(it->second->start_timepoint, it->second->end_timepoint);
      session_options.remote_address = address;
      session_options.status = it->second->acl_status_code;
      // TODO: keep the acl latency the same as the overall latency for now
      // When more events are added, we will an overall latency
      session_options.acl_latency = session_options.latency;
      session_options.is_cancelled  = it->second->is_cancelled;
      os::LogMetricBluetoothLEConnection(session_options);
      LOG_INFO("LEConnectionMetricsRemoteDevice: The session is uploaded for %s\n", ADDRESS_TO_LOGGABLE_CSTR(address));
      opened_devices.erase(it);
    }
}

// Implementation of metrics per remote device
void LEConnectionMetricsRemoteDevice::AddStateChangedEvent(
    const hci::Address& address,
    LEConnectionOriginType origin_type,
    LEConnectionType connection_type,
    LEConnectionState transaction_state,
    std::vector<std::pair<os::ArgumentType, int>> argument_list) {


  if (address.IsEmpty()) {
      LOG_INFO("LEConnectionMetricsRemoteDevice: Empty Address");
      for (auto &device_metric : device_metrics) {
        if (device_metric->IsStarted() && transaction_state == LEConnectionState::STATE_LE_ACL_CANCEL) {
          LOG_INFO("LEConnectionMetricsRemoteDevice: Cancellation Begin");
          // cancel the connection
          device_metric->AddStateChangedEvent(origin_type, connection_type, transaction_state, argument_list);
          continue;
        }

        if (device_metric->IsCancelled() && transaction_state == LEConnectionState::STATE_LE_ACL_END) {
          LOG_INFO("LEConnectionMetricsRemoteDevice: Session is now complete after cancellation");
          // complete the connection
          device_metric->AddStateChangedEvent(origin_type, connection_type, transaction_state, argument_list);
          UploadLEConnectionSession(device_metric->address);
          continue;
        }
      }
      return;
  }

  auto it = opened_devices.find(address);
  if (it == opened_devices.end()) {
    device_metrics.push_back(std::make_unique<LEConnectionMetricState>(address));
    it = opened_devices.insert(std::begin(opened_devices),
                                {address, device_metrics.back().get()});
  }

  it->second->AddStateChangedEvent(origin_type, connection_type, transaction_state, argument_list);

  // Connection is finished
  if (it->second->IsEnded()) {
    UploadLEConnectionSession(address);
  }
}

// Flush of the sessions
void LEConnectionMetricsRemoteDevice::Flush() {
  for (auto &p: opened_devices) {
    p.second->Flush();
  }
  opened_devices.clear();
}

// Instance of Metrics Collector for LEConnectionMetricsRemoteDeviceImpl
LEConnectionMetricsRemoteDevice* MetricsCollector::le_connection_metrics_remote_device_ = nullptr;

LEConnectionMetricsRemoteDevice* MetricsCollector::GetLEConnectionMetricsCollector() {
  if (MetricsCollector::le_connection_metrics_remote_device_ == nullptr) {
    MetricsCollector::le_connection_metrics_remote_device_ = new LEConnectionMetricsRemoteDevice();
  }
  return MetricsCollector::le_connection_metrics_remote_device_;
}

void MetricsCollector::Flush() {
  if (MetricsCollector::le_connection_metrics_remote_device_ == nullptr) {
    MetricsCollector::le_connection_metrics_remote_device_->Flush();
  }

}

}  // namespace metrics

}  // namespace bluetooth
