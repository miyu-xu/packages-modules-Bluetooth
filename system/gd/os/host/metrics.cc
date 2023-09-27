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
    const Address&, uint16_t, uint32_t, uint16_t, uint16_t, uint16_t, int64_t) {}

void LogMetricSocketConnectionState(
    const Address&,
    int,
    int,
    android::bluetooth::SocketConnectionstateEnum,
    int64_t,
    int64_t,
    int,
    int,
    android::bluetooth::SocketRoleEnum) {}

void LogMetricHciTimeoutEvent(uint32_t) {}

void LogMetricA2dpAudioUnderrunEvent(const Address&, uint64_t, int) {}

void LogMetricA2dpAudioOverrunEvent(const Address&, uint64_t, int, int, int) {}

void LogMetricHfpPacketLossStats(const Address&, int, double, uint16_t) {}

void LogMetricMmcTranscodeRttStats(int, double, int, int) {}

void LogMetricReadRssiResult(const Address&, uint16_t, uint32_t, int8_t) {}

void LogMetricReadFailedContactCounterResult(const Address&, uint16_t, uint32_t, int32_t) {}

void LogMetricReadTxPowerLevelResult(const Address&, uint16_t, uint32_t, int32_t) {}

void LogMetricRemoteVersionInfo(uint16_t, uint8_t, uint8_t, uint16_t, uint16_t) {}

void LogMetricLinkLayerConnectionEvent(
    const Address*,
    uint32_t,
    android::bluetooth::DirectionEnum,
    uint16_t,
    uint32_t,
    uint16_t,
    uint16_t,
    uint16_t,
    uint16_t) {}

void LogMetricManufacturerInfo(
    const Address&,
    android::bluetooth::AddressTypeEnum,
    android::bluetooth::DeviceInfoSrcEnum,
    const std::string&,
    const std::string&,
    const std::string&,
    const std::string&,
    const std::string&) {}

void LogMetricSdpAttribute(const Address&, uint16_t, uint16_t, size_t, const char*) {}

void LogMetricSmpPairingEvent(
    const Address&, uint16_t, android::bluetooth::DirectionEnum, uint16_t) {}

void LogMetricA2dpPlaybackEvent(const Address&, int, int) {}

void LogMetricBluetoothHalCrashReason(const Address&, uint32_t, uint32_t) {}

void LogMetricBluetoothLocalSupportedFeatures(uint32_t, uint64_t) {}

void LogMetricBluetoothLocalVersions(uint32_t, uint8_t, uint32_t, uint8_t, uint32_t) {}

void LogMetricBluetoothDisconnectionReasonReported(uint32_t, const Address&, uint32_t) {}

void LogMetricBluetoothRemoteSupportedFeatures(const Address&, uint32_t, uint64_t, uint32_t) {}

void LogMetricBluetoothCodePathCounterMetrics(int32_t, int64_t) {}

void LogMetricBluetoothLEConnectionMetricEvent(
    const Address&,
    android::bluetooth::le::LeConnectionOriginType,
    android::bluetooth::le::LeConnectionType,
    android::bluetooth::le::LeConnectionState,
    std::vector<std::pair<os::ArgumentType, int>>&) {}

void LogMetricBluetoothLEConnection(os::LEConnectionSessionOptions) {}

}  // namespace os
}  // namespace bluetooth
