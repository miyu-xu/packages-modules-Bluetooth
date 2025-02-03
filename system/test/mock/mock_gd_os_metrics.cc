/*
 * Copyright 2025 The Android Open Source Project
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

#include <frameworks/proto_logging/stats/enums/bluetooth/enums.pb.h>
#include <frameworks/proto_logging/stats/enums/bluetooth/hci/enums.pb.h>
#include <frameworks/proto_logging/stats/enums/bluetooth/le/enums.pb.h>
#include <frameworks/proto_logging/stats/enums/bluetooth/rfcomm/enums.pb.h>

#include "hci/address.h"
#include "os/metrics.h"
#include "test/common/mock_functions.h"
#include "types/raw_address.h"

namespace bluetooth {
namespace os {

void LogMetricLinkLayerConnectionEvent(const hci::Address* /* address */,
                                       uint32_t /* connection_handle */,
                                       android::bluetooth::DirectionEnum /* direction */,
                                       uint16_t /* link_type */, uint32_t /* hci_cmd */,
                                       uint16_t /* hci_event */, uint16_t /* hci_ble_event */,
                                       uint16_t /* cmd_status */, uint16_t /* reason_code */) {
  inc_func_call_count(__func__);
}

void LogMetricHciTimeoutEvent(uint32_t /* hci_cmd */) { inc_func_call_count(__func__); }

void LogMetricRemoteVersionInfo(uint16_t /* handle */, uint8_t /* status */, uint8_t /* version */,
                                uint16_t /* manufacturer_name */, uint16_t /* subversion */) {
  inc_func_call_count(__func__);
}

void LogMetricA2dpAudioUnderrunEvent(const hci::Address& /* address */,
                                     uint64_t /* encoding_interval_millis */,
                                     int /* num_missing_pcm_bytes */) {
  inc_func_call_count(__func__);
}

void LogMetricA2dpAudioOverrunEvent(const hci::Address& /* address */,
                                    uint64_t /* encoding_interval_millis */,
                                    int /* num_dropped_buffers */,
                                    int /* num_dropped_encoded_frames */,
                                    int /* num_dropped_encoded_bytes */) {
  inc_func_call_count(__func__);
}

void LogMetricA2dpPlaybackEvent(const hci::Address& /* address */, int /* playback_state */,
                                int /* audio_coding_mode */) {
  inc_func_call_count(__func__);
}

void LogMetricA2dpSessionMetricsEvent(
        const hci::Address& /* address */, int64_t /* audio_duration_ms */,
        int /* media_timer_min_ms */, int /* media_timer_max_ms */, int /* media_timer_avg_ms */,
        int /* total_scheduling_count */, int /* buffer_overruns_max_count */,
        int /* buffer_overruns_total */, float /* buffer_underruns_average */,
        int /* buffer_underruns_count */, int64_t /* codec_index */, bool /* is_a2dp_offload */) {
  inc_func_call_count(__func__);
}

void LogMetricHfpPacketLossStats(const hci::Address& /* address */, int /* num_decoded_frames */,
                                 double /* packet_loss_ratio */, uint16_t /* codec_id */) {
  inc_func_call_count(__func__);
}

void LogMetricMmcTranscodeRttStats(int /* maximum_rtt */, double /* mean_rtt */,
                                   int /* num_requests */, int /* codec_type */) {
  inc_func_call_count(__func__);
}

void LogMetricReadRssiResult(const hci::Address& /* address */, uint16_t /* handle */,
                             uint32_t /* cmd_status */, int8_t /* rssi */) {
  inc_func_call_count(__func__);
}

void LogMetricReadFailedContactCounterResult(const hci::Address& /* address */,
                                             uint16_t /* handle */, uint32_t /* cmd_status */,
                                             int32_t /* failed_contact_counter */) {
  inc_func_call_count(__func__);
}

void LogMetricReadTxPowerLevelResult(const hci::Address& /* address */, uint16_t /* handle */,
                                     uint32_t /* cmd_status */,
                                     int32_t /* transmit_power_level */) {
  inc_func_call_count(__func__);
}

void LogMetricSmpPairingEvent(const hci::Address& /* address */, uint16_t /* smp_cmd */,
                              android::bluetooth::DirectionEnum /* direction */,
                              uint16_t /* smp_fail_reason */) {
  inc_func_call_count(__func__);
}

void LogMetricClassicPairingEvent(const hci::Address& /* address */, uint16_t /* handle */,
                                  uint32_t /* hci_cmd */, uint16_t /* hci_event */,
                                  uint16_t /* cmd_status */, uint16_t /* reason_code */,
                                  int64_t /* event_value */) {
  inc_func_call_count(__func__);
}

void LogMetricSdpAttribute(const hci::Address& /* address */, uint16_t /* protocol_uuid */,
                           uint16_t /* attribute_id */, size_t /* attribute_size */,
                           const char* /* attribute_value */) {
  inc_func_call_count(__func__);
}

void LogMetricSocketConnectionState(
        const hci::Address& /* address */, int /* port */, int /* type */,
        android::bluetooth::SocketConnectionstateEnum /* connection_state */,
        int64_t /* tx_bytes */, int64_t /* rx_bytes */, int /* uid */, int /* server_port */,
        android::bluetooth::SocketRoleEnum /* socket_role */) {
  inc_func_call_count(__func__);
}

void LogMetricManufacturerInfo(const hci::Address& /* address */,
                               android::bluetooth::AddressTypeEnum /* address_type */,
                               android::bluetooth::DeviceInfoSrcEnum /* source_type */,
                               const std::string& /* source_name */,
                               const std::string& /* manufacturer */,
                               const std::string& /* model */,
                               const std::string& /* hardware_version */,
                               const std::string& /* software_version */) {
  inc_func_call_count(__func__);
}

void LogMetricBluetoothHalCrashReason(const hci::Address& /* address */, uint32_t /* error_code */,
                                      uint32_t /* vendor_error_code */) {
  inc_func_call_count(__func__);
}

void LogMetricBluetoothLocalSupportedFeatures(uint32_t /* page_num */, uint64_t /* features */) {
  inc_func_call_count(__func__);
}

void LogMetricBluetoothLocalVersions(uint32_t /* lmp_manufacturer_name */,
                                     uint8_t /* lmp_version */, uint32_t /* lmp_subversion */,
                                     uint8_t /* hci_version */, uint32_t /* hci_revision */) {
  inc_func_call_count(__func__);
}

void LogMetricBluetoothDisconnectionReasonReported(uint32_t /* reason */,
                                                   const hci::Address& /* address */,
                                                   uint32_t /* connection_handle */) {
  inc_func_call_count(__func__);
}

void LogMetricBluetoothRemoteSupportedFeatures(const hci::Address& /* address */,
                                               uint32_t /* page */, uint64_t /* features */,
                                               uint32_t /* connection_handle */);

void LogMetricBluetoothCodePathCounterMetrics(int32_t /* key */, int64_t /* count */) {
  inc_func_call_count(__func__);
}

void LogMetricBluetoothLEConnection(os::LEConnectionSessionOptions /* session_options */) {
  inc_func_call_count(__func__);
}

void LogMetricBluetoothEvent(const hci::Address& /* address */,
                             android::bluetooth::EventType /* event_type */,
                             android::bluetooth::State /* state */) {
  inc_func_call_count(__func__);
}

void LogMetricRfcommConnectionAtClose(
        const RawAddress& /* address */, android::bluetooth::rfcomm::PortResult /* close_reason */,
        android::bluetooth::rfcomm::SocketConnectionSecurity /* security */,
        android::bluetooth::rfcomm::RfcommPortEvent /* last_event */,
        android::bluetooth::rfcomm::RfcommPortState /* previous_state */,
        int32_t /* open_duration_ms */, int32_t /* uid */,
        android::bluetooth::BtaStatus /* sdp_status */, bool /* is_server */,
        bool /* sdp_initiated */, int32_t /* sdp_duration_ms */) {
  inc_func_call_count(__func__);
}

}  // namespace os
}  // namespace bluetooth
