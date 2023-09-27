/******************************************************************************
 *
 *  Copyright 2021 Google, Inc.
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
 ******************************************************************************/

#include "os/metrics.h"

#include "os/log.h"

namespace bluetooth {
namespace os {

using bluetooth::hci::Address;

void LogMetricClassicPairingEvent(
    [[maybe_unused]] const Address& address,
    [[maybe_unused]] uint16_t handle,
    [[maybe_unused]] uint32_t hci_cmd,
    [[maybe_unused]] uint16_t hci_event,
    [[maybe_unused]] uint16_t cmd_status,
    [[maybe_unused]] uint16_t reason_code,
    [[maybe_unused]] int64_t event_value) {}

void LogMetricSocketConnectionState(
    [[maybe_unused]] const Address& address,
    [[maybe_unused]] int port,
    [[maybe_unused]] int type,
    [[maybe_unused]] android::bluetooth::SocketConnectionstateEnum connection_state,
    [[maybe_unused]] int64_t tx_bytes,
    [[maybe_unused]] int64_t rx_bytes,
    [[maybe_unused]] int uid,
    [[maybe_unused]] int server_port,
    [[maybe_unused]] android::bluetooth::SocketRoleEnum socket_role) {}

void LogMetricHciTimeoutEvent([[maybe_unused]] uint32_t hci_cmd) {}

void LogMetricA2dpAudioUnderrunEvent(
    [[maybe_unused]] const Address& address,
    [[maybe_unused]] uint64_t encoding_interval_millis,
    [[maybe_unused]] int num_missing_pcm_bytes) {}

void LogMetricA2dpAudioOverrunEvent(
    [[maybe_unused]] const Address& address,
    [[maybe_unused]] uint64_t encoding_interval_millis,
    [[maybe_unused]] int num_dropped_buffers,
    [[maybe_unused]] int num_dropped_encoded_frames,
    [[maybe_unused]] int num_dropped_encoded_bytes) {}

void LogMetricHfpPacketLossStats(
    [[maybe_unused]] const Address& address,
    [[maybe_unused]] int num_decoded_frames,
    [[maybe_unused]] double packet_loss_ratio,
    [[maybe_unused]] uint16_t codec_type) {}

void LogMetricMmcTranscodeRttStats(
    [[maybe_unused]] int maximum_rtt,
    [[maybe_unused]] double mean_rtt,
    [[maybe_unused]] int num_requests,
    [[maybe_unused]] int codec_type) {}

void LogMetricReadRssiResult(
    [[maybe_unused]] const Address& address,
    [[maybe_unused]] uint16_t handle,
    [[maybe_unused]] uint32_t cmd_status,
    [[maybe_unused]] int8_t rssi) {}

void LogMetricReadFailedContactCounterResult(
    [[maybe_unused]] const Address& address,
    [[maybe_unused]] uint16_t handle,
    [[maybe_unused]] uint32_t cmd_status,
    [[maybe_unused]] int32_t failed_contact_counter) {}

void LogMetricReadTxPowerLevelResult(
    [[maybe_unused]] const Address& address,
    [[maybe_unused]] uint16_t handle,
    [[maybe_unused]] uint32_t cmd_status,
    [[maybe_unused]] int32_t transmit_power_level) {}

void LogMetricRemoteVersionInfo(
    [[maybe_unused]] uint16_t handle,
    [[maybe_unused]] uint8_t status,
    [[maybe_unused]] uint8_t version,
    [[maybe_unused]] uint16_t manufacturer_name,
    [[maybe_unused]] uint16_t subversion) {}

void LogMetricLinkLayerConnectionEvent(
    [[maybe_unused]] const Address* address,
    [[maybe_unused]] uint32_t connection_handle,
    [[maybe_unused]] android::bluetooth::DirectionEnum direction,
    [[maybe_unused]] uint16_t link_type,
    [[maybe_unused]] uint32_t hci_cmd,
    [[maybe_unused]] uint16_t hci_event,
    [[maybe_unused]] uint16_t hci_ble_event,
    [[maybe_unused]] uint16_t cmd_status,
    [[maybe_unused]] uint16_t reason_code) {}

void LogMetricManufacturerInfo(
    [[maybe_unused]] const Address& address,
    [[maybe_unused]] android::bluetooth::AddressTypeEnum address_type,
    [[maybe_unused]] android::bluetooth::DeviceInfoSrcEnum source_type,
    [[maybe_unused]] const std::string& source_name,
    [[maybe_unused]] const std::string& manufacturer,
    [[maybe_unused]] const std::string& model,
    [[maybe_unused]] const std::string& hardware_version,
    [[maybe_unused]] const std::string& software_version) {}

void LogMetricSdpAttribute(
    [[maybe_unused]] const Address& address,
    [[maybe_unused]] uint16_t protocol_uuid,
    [[maybe_unused]] uint16_t attribute_id,
    [[maybe_unused]] size_t attribute_size,
    [[maybe_unused]] const char* attribute_value) {}

void LogMetricSmpPairingEvent(
    [[maybe_unused]] const Address& address,
    [[maybe_unused]] uint16_t smp_cmd,
    [[maybe_unused]] android::bluetooth::DirectionEnum direction,
    [[maybe_unused]] uint16_t smp_fail_reason) {}

void LogMetricA2dpPlaybackEvent(
    [[maybe_unused]] const Address& address,
    [[maybe_unused]] int playback_state,
    [[maybe_unused]] int audio_coding_mode) {}

void LogMetricBluetoothHalCrashReason(
    [[maybe_unused]] const Address& address,
    [[maybe_unused]] uint32_t error_code,
    [[maybe_unused]] uint32_t vendor_error_code) {}

void LogMetricBluetoothLocalSupportedFeatures(
    [[maybe_unused]] uint32_t page_num, [[maybe_unused]] uint64_t features) {}

void LogMetricBluetoothLocalVersions(
    [[maybe_unused]] uint32_t lmp_manufacturer_name,
    [[maybe_unused]] uint8_t lmp_version,
    [[maybe_unused]] uint32_t lmp_subversion,
    [[maybe_unused]] uint8_t hci_version,
    [[maybe_unused]] uint32_t hci_reversion) {}

void LogMetricBluetoothDisconnectionReasonReported(
    [[maybe_unused]] uint32_t reason,
    [[maybe_unused]] const Address& address,
    [[maybe_unused]] uint32_t connection_handle) {}

void LogMetricBluetoothRemoteSupportedFeatures(
    [[maybe_unused]] const Address& address,
    [[maybe_unused]] uint32_t page,
    [[maybe_unused]] uint64_t features,
    [[maybe_unused]] uint32_t connection_handle) {}

void LogMetricBluetoothCodePathCounterMetrics(
    [[maybe_unused]] int32_t key, [[maybe_unused]] int64_t count) {}

void LogMetricBluetoothLEConnectionMetricEvent(
    [[maybe_unused]] const Address& address,
    [[maybe_unused]] android::bluetooth::le::LeConnectionOriginType origin_type,
    [[maybe_unused]] android::bluetooth::le::LeConnectionType connection_type,
    [[maybe_unused]] android::bluetooth::le::LeConnectionState transaction_state,
    [[maybe_unused]] std::vector<std::pair<os::ArgumentType, int>>& argument_list) {}

void LogMetricBluetoothLEConnection(
    [[maybe_unused]] os::LEConnectionSessionOptions session_options) {}

}  // namespace os
}  // namespace bluetooth
