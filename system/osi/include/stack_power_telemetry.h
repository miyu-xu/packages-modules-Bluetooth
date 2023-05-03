#include <base/logging.h>
#include <time.h>

#include <cstdint>
#include <iostream>
#include <list>
#include <map>
#include <mutex>
#include <string>
#include <string_view>

#include "osi/include/properties.h"
#include "types/raw_address.h"

constexpr uint8_t ACL_PKT_TX{1};
constexpr uint8_t ACL_PKT_RX{2};
constexpr uint8_t CHANNEL_TYPE_RFCOMM{0};
constexpr uint8_t CHANNEL_TYPE_L2CAP{1};
constexpr uint8_t DUMP_HCI_CMD{1};
constexpr uint8_t DUMP_HCI_EVENT{2};
constexpr uint8_t LOG_DATA_ENTRIES_IN_MEMORY{15};
constexpr uint8_t STATE_CONNECTED{1};
constexpr uint8_t STATE_DISCONNECTED{0};
constexpr std::string_view LOG_PER_CHANNEL_PROPERTY =
    "bluetooth.powertelemetry.log_per_channel.enabled";
constexpr std::string_view ENABLED_POWER_TELEMETRY_PROPERTY =
    "bluetooth.powertelemetry.enabled";
extern int64_t GetCurrentTimeSec();

namespace power_telemetry {
class LinkDetails {
 public:
  RawAddress bdaddr;
  std::string connected_ts;
  std::string disconnected_ts;
  uint16_t handle;
  uint8_t tx_power_level;
  LinkDetails() { handle = tx_power_level = 0; }
};
class ChannelDetails {
 public:
  int32_t channel_type;
  int32_t src_id;
  int32_t dst_id;
  RawAddress remote_addr;
  int64_t tx_bytes;
  int64_t rx_bytes;
  int32_t state;
  std::string conn_time_stamp;
  std::string disconn_time_stamp;
  std::string last_tx_time_stamp;
  std::string last_rx_time_stamp;
  int32_t psm;

  ChannelDetails() {
    channel_type = 0;
    state = 0;
    src_id = 0;
    dst_id = 0;
    tx_bytes = 0;
    rx_bytes = 0;
    psm = 0;
  }
};

class AclPacketDetails {
 public:
  uint32_t tx_pkt_count;
  int64_t tx_total_bytes;
  uint32_t rx_pkt_count;
  int64_t rx_total_bytes;

  AclPacketDetails() {
    tx_pkt_count = rx_pkt_count = 0;
    tx_total_bytes = rx_total_bytes = 0;
  }
};

class AdvDetails {
 public:
  std::string start_time_stamp;
  std::string end_time_stamp;
  AdvDetails() { start_time_stamp = end_time_stamp = ""; }
};

class ScanDetails {
 public:
  int32_t count;
  ScanDetails() { count = 0; }
};
class TrafficData {
 public:
  int64_t tx_bytes;
  int64_t rx_bytes;
};

class SniffData {
 public:
  RawAddress bdaddr;
  uint32_t sniff_count, active_count;
  int64_t sniff_duration, active_duration;
  int64_t last_mode_change_ts;
  SniffData() {
    sniff_count = active_count = 0;
    sniff_duration = active_duration = 0;
    last_mode_change_ts = GetCurrentTimeSec();
  }
};

class LogDataContainer {
 public:
  std::string start_time_stamp;
  std::string end_time_stamp;
  std::map<RawAddress, std::list<ChannelDetails>> channel_map;
  std::list<ScanDetails> scan_le_list;
  TrafficData l2c_data, rfc_data;
  std::map<uint16_t, SniffData> sniff_activity_map;
  std::map<uint16_t, LinkDetails> acl_link_map;
  std::map<uint16_t, LinkDetails> sco_link_map;
  std::list<LinkDetails> acl_link_list;
  std::list<LinkDetails> sco_link_list;
  std::list<AdvDetails> adv_list;
  ScanDetails scan_ds, inq_scan_ds;
  AclPacketDetails acl_pkt_ds, hci_cmd_evt_ds;
};

class PowerTelemetry {
 public:
  PowerTelemetry() {
    traffic_logged_ts_ = GetCurrentTimeSec();
    std::unique_ptr<LogDataContainer> ldc =
        std::make_unique<LogDataContainer>();
    log_data_containers_.push_back(std::move(ldc));
    log_per_channel_ = osi_property_get_bool(
        std::string(LOG_PER_CHANNEL_PROPERTY).c_str(), false);
    power_telemerty_enabled_ = osi_property_get_bool(
        std::string(ENABLED_POWER_TELEMETRY_PROPERTY).c_str(), false);
  }
  std::unique_ptr<LogDataContainer>& GetCurrentLogDataContainer();
  void RecordLogDataContainer();
  void LogScanStarted();
  void LogScanEnded();
  void LogLeScanStarted();
  void LogLeScanEnded();
  void LogChannelConnected(int32_t channel_type, int32_t src_id, int32_t dst_id,
                           RawAddress bd_addr, int32_t psm);
  void LogChannelDisconnected(int32_t channel_type, int32_t src_id,
                              int32_t dst_id, RawAddress bd_addr);
  void LogTxBytes(int32_t channel_type, int32_t src_id, int32_t dst_id,
                  RawAddress bd_addr, int32_t num_bytes);
  void LogRxBytes(int32_t channel_type, int32_t src_id, int32_t dst_id,
                  RawAddress bd_addr, int32_t num_bytes);
  void PowerTelemetryDump(int32_t fd);
  void LogSniffActivity(uint16_t handle, RawAddress bdaddr, bool sniffEntered);
  void LogAclPktDetails(int32_t type, uint16_t len);
  void LogAclLinkDetails(uint16_t handle, const RawAddress* bdaddr,
                         bool isConnected);
  void LogScoLinkDetails(uint16_t handle, RawAddress bdaddr, bool isConnected);
  void LogAclTxPowerLevel(uint16_t handle, uint8_t txPower);
  void LogInqScanDetails(bool started);
  void LogBleAdvDetails(bool started);
  void LogHciCmdEvtDetails(int32_t type);
  void LogTxPower(void* res);
  void LogTrafficData();

 protected:
  std::list<std::unique_ptr<LogDataContainer>> log_data_containers_;
  const int64_t kTrafficLogTime = 120;  // 120seconds
  int64_t traffic_logged_ts_ = 0;
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

PowerTelemetry* GetInstance();
}  // namespace power_telemetry
