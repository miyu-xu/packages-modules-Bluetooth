#include <base/logging.h>
#include <time.h>

#include <fstream>
#include <iostream>
#include <list>
#include <map>
#include <mutex>
#include <sstream>

#include "osi/include/properties.h"
#include "types/raw_address.h"

#define STATE_CONNECTED 1
#define STATE_DISCONNECTED 0
#define CHANNEL_TYPE_RFCOMM 0
#define CHANNEL_TYPE_L2CAP 1
#define ACL_PKT_TX 1
#define ACL_PKT_RX 2
#define DUMP_HCI_CMD 1
#define DUMP_HCI_EVENT 2

using namespace std;

#define LOG_DATA_ENTRIES_IN_MEMORY 15
#define ENABLED_POWER_TELEMETRY_PROPERTY "bluetooth.powertelemetry.enabled"
#define LOG_PER_CHANNEL_PROPERTY \
  "bluetooth.powertelemetry.log_per_channel.enabled"
extern long GetCurrentTimeSec();

class LinkDetails {
 public:
  RawAddress bdaddr;
  string connected_ts;
  string disconnected_ts;
  uint16_t handle;
  uint8_t tx_power_level;
  LinkDetails() { handle = tx_power_level = 0; }
};
class ChannelDetails {
 public:
  int channel_type;
  int src_id;
  int dst_id;
  RawAddress remote_addr;
  long tx_bytes;
  long rx_bytes;
  int state;
  string conn_time_stamp;
  string disconn_time_stamp;
  string last_tx_time_stamp;
  string last_rx_time_stamp;
  int psm;

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

class AclPktDetails {
 public:
  uint32_t tx_pkt_count;
  long tx_total_bytes;
  uint32_t rx_pkt_count;
  long rx_total_bytes;

  AclPktDetails() {
    tx_pkt_count = rx_pkt_count = 0;
    tx_total_bytes = rx_total_bytes = 0;
  }
};

class AdvDetails {
 public:
  string start_time_stamp;
  string end_time_stamp;
  AdvDetails() { start_time_stamp = end_time_stamp = ""; }
};

class ScanDetails {
 public:
  int count;
  ScanDetails() { count = 0; }
};
class TrafficData {
 public:
  long tx_bytes;
  long rx_bytes;
};

class SniffData {
 public:
  RawAddress bdaddr;
  uint32_t sniff_count, active_count;
  long sniff_duration, active_duration;
  long last_mode_change_ts;
  SniffData() {
    sniff_count = active_count = 0;
    sniff_duration = active_duration = 0;
    last_mode_change_ts = GetCurrentTimeSec();
  }
};

class LogDataContainer {
 public:
  string start_time_stamp;
  string end_time_stamp;
  map<RawAddress, list<ChannelDetails>> channel_map;
  list<ScanDetails> scan_le_list;
  TrafficData l2c_data, rfc_data;
  map<uint16_t, SniffData> sniff_activity_map;
  map<uint16_t, LinkDetails> acl_link_map;
  map<uint16_t, LinkDetails> sco_link_map;
  list<LinkDetails> acl_link_list;
  list<LinkDetails> sco_link_list;
  list<AdvDetails> adv_list;
  ScanDetails scan_ds, inq_scan_ds;
  AclPktDetails acl_pkt_ds, hci_cmd_evt_ds;
};

class PowerTelemetry {
 public:
  PowerTelemetry() {
    traffic_logged_ts_ = GetCurrentTimeSec();
    LogDataContainer* ldc = new LogDataContainer();
    log_data_containers_.push_back(ldc);
    log_per_channel_ = osi_property_get_bool(LOG_PER_CHANNEL_PROPERTY, false);
    power_telemerty_enabled_ =
        osi_property_get_bool(ENABLED_POWER_TELEMETRY_PROPERTY, false);
  }
  ~PowerTelemetry() {
    for (auto ldc : log_data_containers_) {
      delete (ldc);
    }
  }
  static PowerTelemetry* GetInstance();
  LogDataContainer* GetCurrentLogDataContainer();
  void RecordLogDataContainer();
  void LogScanStarted();
  void LogScanEnded();
  void LogLeScanStarted();
  void LogLeScanEnded();
  void LogChannelConnected(int channel_type, int src_id, int dst_id,
                           RawAddress bd_addr, int psm);
  void LogChannelDisconnected(int channel_type, int src_id, int dst_id,
                              RawAddress bd_addr);
  void LogTxBytes(int channel_type, int src_id, int dst_id, RawAddress bd_addr,
                  int num_bytes);
  void LogRxBytes(int channel_type, int src_id, int dst_id, RawAddress bd_addr,
                  int num_bytes);
  void PowerTelemetryDump(int fd);
  void LogSniffActivity(uint16_t handle, RawAddress bdaddr, bool sniffEntered);
  void LogAclPktDetails(int type, uint16_t len);
  void LogAclLinkDetails(uint16_t handle, RawAddress bdaddr, bool isConnected);
  void LogScoLinkDetails(uint16_t handle, RawAddress bdaddr, bool isConnected);
  void LogAclTxPowerLevel(uint16_t handle, uint8_t txPower);
  void LogInqScanDetails(bool started);
  void LogBleAdvDetails(bool started);
  void LogHciCmdEvtDetails(int type);
  void LogTxPower(void* res);
  void LogTrafficData();

 protected:
  list<LogDataContainer*> log_data_containers_;
  const long kTrafficLogTime = 120;  // 120seconds
  long traffic_logged_ts_ = 0;
  long l2c_tx_bytes_ = 0;
  long rfc_tx_bytes_ = 0;
  long l2c_rx_bytes_ = 0;
  long rfc_rx_bytes_ = 0;
  uint32_t acl_rx_pkt_ = 0, acl_tx_pkt_ = 0;
  long acl_tx_len_ = 0, acl_rx_len_ = 0;
  std::mutex dumpsys_mutex_;
  uint16_t scan_count_ = 0, inq_scan_count_ = 0, ble_adv_count_ = 0;
  uint32_t cmd_count_ = 0, event_count_ = 0;
  bool scan_timer_started_ = false;
  bool log_per_channel_ = false;
  bool power_telemerty_enabled_ = false;
};
