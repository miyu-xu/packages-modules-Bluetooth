/*
 * Copyright 2023 The Android Open Source Project
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

#include "le_connect_hci_manager.h"

#include <string>

#include "common/contextual_callback.h"
#include "hci/address_with_type.h"
#include "hci/controller.h"
#include "hci/hci_layer.h"
#include "hci/hci_packets.h"
#include "hci/le_address_manager.h"
#include "os/handler.h"
#include "os/system_properties.h"

namespace bluetooth {
namespace hci {
namespace acl_manager {

namespace {

static const std::string kPropertyMinConnInterval = "bluetooth.core.le.min_connection_interval";
static const std::string kPropertyMaxConnInterval = "bluetooth.core.le.max_connection_interval";
static const std::string kPropertyConnLatency = "bluetooth.core.le.connection_latency";
static const std::string kPropertyConnSupervisionTimeout =
    "bluetooth.core.le.connection_supervision_timeout";
static const std::string kPropertyConnScanIntervalFast =
    "bluetooth.core.le.connection_scan_interval_fast";
static const std::string kPropertyConnScanWindowFast =
    "bluetooth.core.le.connection_scan_window_fast";
static const std::string kPropertyConnScanWindow2mFast =
    "bluetooth.core.le.connection_scan_window_2m_fast";
static const std::string kPropertyConnScanWindowCodedFast =
    "bluetooth.core.le.connection_scan_window_coded_fast";
static const std::string kPropertyConnScanIntervalSlow =
    "bluetooth.core.le.connection_scan_interval_slow";
static const std::string kPropertyConnScanWindowSlow =
    "bluetooth.core.le.connection_scan_window_slow";
static const std::string kPropertyEnableBleOnlyInit1mPhy =
    "bluetooth.core.gap.le.conn.only_init_1m_phy.enabled";

constexpr uint16_t kConnIntervalMin = 0x0018;
constexpr uint16_t kConnIntervalMax = 0x0028;
constexpr uint16_t kConnLatency = 0x0000;
constexpr uint16_t kSupervisionTimeout = 0x01f4;
constexpr uint16_t kScanIntervalFast = 0x0060;          /* 30 ~ 60 ms (use 60)  = 96 *0.625 */
constexpr uint16_t kScanWindowFast = 0x0030;            /* 30 ms = 48 *0.625 */
constexpr uint16_t kScanWindow2mFast = 0x0018;          /* 15 ms = 24 *0.625 */
constexpr uint16_t kScanWindowCodedFast = 0x0018;       /* 15 ms = 24 *0.625 */
constexpr uint16_t kScanIntervalSlow = 0x0800;          /* 1.28 s = 2048 *0.625 */
constexpr uint16_t kScanWindowSlow = 0x0030;            /* 30 ms = 48 *0.625 */
constexpr uint16_t kScanIntervalSystemSuspend = 0x0400; /* 640 ms = 1024 * 0.625 */
constexpr uint16_t kScanWindowSystemSuspend = 0x0012;   /* 11.25ms = 18 * 0.625 */
constexpr bool kEnableBleOnlyInit1mPhy = false;

constexpr uint8_t PHY_LE_1M = 0x01;
constexpr uint8_t PHY_LE_2M = 0x02;
constexpr uint8_t PHY_LE_CODED = 0x04;

bool CheckConnectionParameters(
    uint16_t conn_interval_min,
    uint16_t conn_interval_max,
    uint16_t conn_latency,
    uint16_t supervision_timeout) {
  if (conn_interval_min < 0x0006 || conn_interval_min > 0x0C80 || conn_interval_max < 0x0006 ||
      conn_interval_max > 0x0C80 || conn_latency > 0x01F3 || supervision_timeout < 0x000A ||
      supervision_timeout > 0x0C80) {
    LOG_ERROR("Invalid parameter");
    return false;
  }

  // The Maximum interval in milliseconds will be conn_interval_max * 1.25 ms
  // The Timeout in milliseconds will be expected_supervision_timeout * 10 ms
  // The Timeout in milliseconds shall be larger than (1 + Latency) * Interval_Max * 2, where
  // Interval_Max is given in milliseconds.
  uint32_t supervision_timeout_min = (uint32_t)(1 + conn_latency) * conn_interval_max * 2 + 1;
  if (supervision_timeout * 8 < supervision_timeout_min || conn_interval_max < conn_interval_min) {
    LOG_ERROR("Invalid parameter");
    return false;
  }

  return true;
}

}  // namespace

LeConnectHciManager::LeConnectHciManager(
    Controller* controller,
    LeAddressManager* le_address_manager,
    LeAclConnectionInterface* le_acl_connection_interface,
    os::Handler* handler)
    : controller_(controller),
      le_address_manager_(le_address_manager),
      le_acl_connection_interface_(le_acl_connection_interface),
      handler_(handler){};

void LeConnectHciManager::LeCreateConnection(
    bool use_fast_parameters, common::ContextualOnceCallback<void(ErrorCode)> on_complete) {
  uint16_t le_scan_interval =
      os::GetSystemPropertyUint32(kPropertyConnScanIntervalSlow, kScanIntervalSlow);
  uint16_t le_scan_window =
      os::GetSystemPropertyUint32(kPropertyConnScanWindowSlow, kScanWindowSlow);
  uint16_t le_scan_window_2m = le_scan_window;
  uint16_t le_scan_window_coded = le_scan_window;

  if (use_fast_parameters) {
    le_scan_interval =
        os::GetSystemPropertyUint32(kPropertyConnScanIntervalFast, kScanIntervalFast);
    le_scan_window = os::GetSystemPropertyUint32(kPropertyConnScanWindowFast, kScanWindowFast);
    le_scan_window_2m =
        os::GetSystemPropertyUint32(kPropertyConnScanWindow2mFast, kScanWindow2mFast);
    le_scan_window_coded =
        os::GetSystemPropertyUint32(kPropertyConnScanWindowCodedFast, kScanWindowCodedFast);
  }

  // Use specific parameters when in system suspend.
  if (system_suspend_) {
    le_scan_interval = kScanIntervalSystemSuspend;
    le_scan_window = kScanWindowSystemSuspend;
    le_scan_window_2m = le_scan_window;
    le_scan_window_coded = le_scan_window;
  }
  InitiatorFilterPolicy initiator_filter_policy = InitiatorFilterPolicy::USE_FILTER_ACCEPT_LIST;
  OwnAddressType own_address_type =
      static_cast<OwnAddressType>(le_address_manager_->GetInitiatorAddress().GetAddressType());
  uint16_t conn_interval_min =
      os::GetSystemPropertyUint32(kPropertyMinConnInterval, kConnIntervalMin);
  uint16_t conn_interval_max =
      os::GetSystemPropertyUint32(kPropertyMaxConnInterval, kConnIntervalMax);
  uint16_t conn_latency = os::GetSystemPropertyUint32(kPropertyConnLatency, kConnLatency);
  uint16_t supervision_timeout =
      os::GetSystemPropertyUint32(kPropertyConnSupervisionTimeout, kSupervisionTimeout);
  ASSERT(CheckConnectionParameters(
      conn_interval_min, conn_interval_max, conn_latency, supervision_timeout));

  auto address_with_type = AddressWithType();

  if (controller_->IsSupported(OpCode::LE_EXTENDED_CREATE_CONNECTION)) {
    bool only_init_1m_phy =
        os::GetSystemPropertyBool(kPropertyEnableBleOnlyInit1mPhy, kEnableBleOnlyInit1mPhy);

    uint8_t initiating_phys = PHY_LE_1M;
    std::vector<LeCreateConnPhyScanParameters> parameters = {};
    LeCreateConnPhyScanParameters scan_parameters;
    scan_parameters.scan_interval_ = le_scan_interval;
    scan_parameters.scan_window_ = le_scan_window;
    scan_parameters.conn_interval_min_ = conn_interval_min;
    scan_parameters.conn_interval_max_ = conn_interval_max;
    scan_parameters.conn_latency_ = conn_latency;
    scan_parameters.supervision_timeout_ = supervision_timeout;
    scan_parameters.min_ce_length_ = 0x00;
    scan_parameters.max_ce_length_ = 0x00;
    parameters.push_back(scan_parameters);

    if (controller_->SupportsBle2mPhy() && !only_init_1m_phy) {
      LeCreateConnPhyScanParameters scan_parameters_2m;
      scan_parameters_2m.scan_interval_ = le_scan_interval;
      scan_parameters_2m.scan_window_ = le_scan_window_2m;
      scan_parameters_2m.conn_interval_min_ = conn_interval_min;
      scan_parameters_2m.conn_interval_max_ = conn_interval_max;
      scan_parameters_2m.conn_latency_ = conn_latency;
      scan_parameters_2m.supervision_timeout_ = supervision_timeout;
      scan_parameters_2m.min_ce_length_ = 0x00;
      scan_parameters_2m.max_ce_length_ = 0x00;
      parameters.push_back(scan_parameters_2m);
      initiating_phys |= PHY_LE_2M;
    }
    if (controller_->SupportsBleCodedPhy() && !only_init_1m_phy) {
      LeCreateConnPhyScanParameters scan_parameters_coded;
      scan_parameters_coded.scan_interval_ = le_scan_interval;
      scan_parameters_coded.scan_window_ = le_scan_window_coded;
      scan_parameters_coded.conn_interval_min_ = conn_interval_min;
      scan_parameters_coded.conn_interval_max_ = conn_interval_max;
      scan_parameters_coded.conn_latency_ = conn_latency;
      scan_parameters_coded.supervision_timeout_ = supervision_timeout;
      scan_parameters_coded.min_ce_length_ = 0x00;
      scan_parameters_coded.max_ce_length_ = 0x00;
      parameters.push_back(scan_parameters_coded);
      initiating_phys |= PHY_LE_CODED;
    }

    le_acl_connection_interface_->EnqueueCommand(
        LeExtendedCreateConnectionBuilder::Create(
            initiator_filter_policy,
            own_address_type,
            address_with_type.GetAddressType(),
            address_with_type.GetAddress(),
            initiating_phys,
            parameters),
        handler_->BindOnce(
            [](common::ContextualOnceCallback<void(ErrorCode)> on_complete,
               CommandStatusView status) {
              ASSERT(status.IsValid());
              ASSERT(status.GetCommandOpCode() == OpCode::LE_EXTENDED_CREATE_CONNECTION);
              on_complete.Invoke(status.GetStatus());
            },
            std::move(on_complete)));
  } else {
    le_acl_connection_interface_->EnqueueCommand(
        LeCreateConnectionBuilder::Create(
            le_scan_interval,
            le_scan_window,
            initiator_filter_policy,
            address_with_type.GetAddressType(),
            address_with_type.GetAddress(),
            own_address_type,
            conn_interval_min,
            conn_interval_max,
            conn_latency,
            supervision_timeout,
            0x00,
            0x00),
        handler_->BindOnce(
            [](common::ContextualOnceCallback<void(ErrorCode)> on_complete,
               CommandStatusView status) {
              ASSERT(status.IsValid());
              ASSERT(status.GetCommandOpCode() == OpCode::LE_CREATE_CONNECTION);
              on_complete.Invoke(status.GetStatus());
            },
            std::move(on_complete)));
  }
}

void LeConnectHciManager::LeCancelConnection(
    common::ContextualOnceCallback<void(ErrorCode)> on_complete) {
  le_acl_connection_interface_->EnqueueCommand(
      LeCreateConnectionCancelBuilder::Create(),
      handler_->BindOnce(
          [](common::ContextualOnceCallback<void(ErrorCode)> on_complete,
             CommandCompleteView view) {
            auto complete_view = LeCreateConnectionCancelCompleteView::Create(view);
            ASSERT(complete_view.IsValid());
            on_complete.Invoke(complete_view.GetStatus());
          },
          std::move(on_complete)));
}

void LeConnectHciManager::AddToFilterAcceptList(AddressWithType address) {
  le_address_manager_->AddDeviceToFilterAcceptList(
      address.ToFilterAcceptListAddressType(), address.GetAddress());
}

void LeConnectHciManager::RemoveFromFilterAcceptList(AddressWithType address) {
  le_address_manager_->RemoveDeviceFromFilterAcceptList(
      address.ToFilterAcceptListAddressType(), address.GetAddress());
}

void LeConnectHciManager::SetSystemSuspendState(bool suspended) {
  system_suspend_ = suspended;
}

}  // namespace acl_manager
}  // namespace hci
}  // namespace bluetooth