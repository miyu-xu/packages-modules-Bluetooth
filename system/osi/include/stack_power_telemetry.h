#pragma once

#include <base/logging.h>
#include <time.h>

#include <atomic>
#include <cstdint>
#include <iostream>
#include <list>
#include <map>
#include <mutex>
#include <string>
#include <string_view>

#include "osi/include/properties.h"
#include "types/raw_address.h"

namespace power_telemetry {
constexpr uint8_t kAclTxPacket{1};
constexpr uint8_t kAclRxPacket{2};
constexpr uint8_t kChannelRfcomm{0};
constexpr uint8_t kChannelL2cap{1};
constexpr uint8_t kDumpHciCmd{1};
constexpr uint8_t kDumpHciEvt{2};
constexpr uint8_t kLogEntriesSize{15};
constexpr uint8_t kStateConnected{1};
constexpr uint8_t kStateDisconnected{0};
constexpr std::string_view kLogPerChannelProperty =
    "bluetooth.powertelemetry.log_per_channel.enabled";
constexpr std::string_view kEnabledPowerTelemetryProperty =
    "bluetooth.powertelemetry.enabled";

struct LinkDetails {
  RawAddress bdaddr;
  time_t connected_ts;
  time_t disconnected_ts;
  uint16_t handle = 0;
  uint8_t tx_power_level = 0;
};
struct ChannelDetails {
  int32_t channel_type = 0;
  int32_t src_id = 0;
  int32_t dst_id = 0;
  RawAddress remote_addr;
  int64_t tx_bytes = 0;
  int64_t rx_bytes = 0;
  int32_t state = 0;
  time_t conn_time_stamp;
  time_t disconn_time_stamp;
  time_t last_tx_time_stamp;
  time_t last_rx_time_stamp;
  int32_t psm = 0;
};

struct AclPacketDetails {
  uint32_t tx_pkt_count = 0;
  int64_t tx_total_bytes = 0;
  uint32_t rx_pkt_count = 0;
  int64_t rx_total_bytes = 0;
};

struct AdvDetails {
  time_t start_time_stamp;
  time_t end_time_stamp;
};

struct ScanDetails {
  int32_t count = 0;
};
struct TrafficData {
  int64_t tx_bytes = 0;
  int64_t rx_bytes = 0;
};

struct SniffData {
  RawAddress bdaddr;
  uint32_t sniff_count = 0, active_count = 0;
  time_t sniff_duration_ts = 0, active_duration_ts = 0;
  time_t last_mode_change_ts = time(0);
};

class LogDataContainer {
 public:
  time_t start_time_stamp;
  time_t end_time_stamp;
  std::map<RawAddress, std::list<ChannelDetails>> channel_map;
  std::list<ScanDetails> scan_le_list;
  TrafficData l2c_data, rfc_data;
  std::map<uint16_t, SniffData> sniff_activity_map;
  std::map<uint16_t, LinkDetails> acl_link_map;
  std::map<uint16_t, LinkDetails> sco_link_map;
  std::list<LinkDetails> acl_link_list;
  std::list<LinkDetails> sco_link_list;
  std::list<AdvDetails> adv_list;
  ScanDetails scan_details, inq_scan_details;
  AclPacketDetails acl_pkt_ds, hci_cmd_evt_ds;
};

class PowerTelemetry {
 public:
  PowerTelemetry() {
    idx_containers = 0;
    traffic_logged_ts_ = time(0);
    log_per_channel_ = osi_property_get_bool(
        std::string(kLogPerChannelProperty).c_str(), false);
    power_telemerty_enabled_ = osi_property_get_bool(
        std::string(kEnabledPowerTelemetryProperty).c_str(), false);
  }
  LogDataContainer& GetCurrentLogDataContainer();
  void RecordLogDataContainer();
  void LogScanStarted();
  void LogChannelConnected(int32_t channel_type, int32_t src_id, int32_t dst_id,
                           const RawAddress& bd_addr, int32_t psm);
  void LogChannelDisconnected(int32_t channel_type, int32_t src_id,
                              int32_t dst_id, const RawAddress& bd_addr);
  void LogTxBytes(int32_t channel_type, int32_t src_id, int32_t dst_id,
                  const RawAddress& bd_addr, int32_t num_bytes);
  void LogRxBytes(int32_t channel_type, int32_t src_id, int32_t dst_id,
                  const RawAddress& bd_addr, int32_t num_bytes);
  void PowerTelemetryDump(int32_t fd);
  void LogSniffActivity(uint16_t handle, const RawAddress& bdaddr,
                        bool sniffEntered);
  void LogAclPktDetails(int32_t type, uint16_t len);
  void LogLinkDetails(uint16_t handle, const RawAddress& bdaddr,
                      bool isConnected, bool is_acl_link);
  void LogAclTxPowerLevel(uint16_t handle, uint8_t txPower);
  void LogInqScanDetails(bool started);
  void LogBleAdvDetails(bool started);
  void LogHciCmdEvtDetails(int32_t type);
  void LogTxPower(void* res);
  void LogTrafficData();

 protected:
  LogDataContainer log_data_containers_[kLogEntriesSize];
  std::atomic_int idx_containers;
  const int64_t kTrafficLogTime = 120;  // 120seconds
  time_t traffic_logged_ts_ = 0;
  int64_t l2c_tx_bytes_ = 0;
  int64_t rfc_tx_bytes_ = 0;
  int64_t l2c_rx_bytes_ = 0;
  int64_t rfc_rx_bytes_ = 0;
  uint32_t acl_rx_pkt_ = 0, acl_tx_pkt_ = 0;
  int64_t acl_tx_len_ = 0, acl_rx_len_ = 0;
  std::mutex dumpsys_mutex_;
  uint16_t scan_count_ = 0, inq_scan_count_ = 0, ble_adv_count_ = 0;
  uint32_t cmd_count_ = 0, event_count_ = 0;
  bool scan_timer_started_ = false;
  bool log_per_channel_ = false;
  bool power_telemerty_enabled_ = false;
};

PowerTelemetry& GetInstance();
}  // namespace power_telemetry
