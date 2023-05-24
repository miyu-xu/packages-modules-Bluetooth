
#include "osi/include/stack_power_telemetry.h"

#include <sys/stat.h>

#include <cstdio>
#include <filesystem>
#include <sstream>

#include "bt_trace.h"
#include "osi/include/alarm.h"
#include "stack/btm/btm_dev.h"
#include "stack/btm/btm_int_types.h"
#include "stack/btm/btm_sec.h"

std::string GetTimeString(time_t tstamp) {
  std::ostringstream str_stm;
  tm* nTm = localtime(&tstamp);
  std::string hour = (nTm->tm_hour >= 10) ? std::to_string(nTm->tm_hour)
                                          : "0" + std::to_string(nTm->tm_hour);
  std::string min = (nTm->tm_min >= 10) ? std::to_string(nTm->tm_min)
                                        : "0" + std::to_string(nTm->tm_min);
  std::string sec = (nTm->tm_sec >= 10) ? std::to_string(nTm->tm_sec)
                                        : "0" + std::to_string(nTm->tm_sec);
  str_stm << (nTm->tm_mon + 1) << "-" << nTm->tm_mday << " " << hour << ":"
          << min << ":" << sec;
  return str_stm.str();
}

std::string GetCurrentTimeString() {
  time_t tstamp = time(0);
  return GetTimeString(tstamp);
}

std::string GetTimeStringFromSec(int64_t timeStampSec) {
  time_t tstamp = time_t(timeStampSec);
  return GetTimeString(tstamp);
}

int64_t GetCurrentTimeSec() {
  time_t tstamp = time(0);
  return (int64_t)tstamp;
}

power_telemetry::PowerTelemetry* power_telemetry::GetInstance() {
  static power_telemetry::PowerTelemetry power_telemetry;
  return &power_telemetry;
}

std::unique_ptr<power_telemetry::LogDataContainer>
power_telemetry::PowerTelemetry::GetCurrentLogDataContainer() {
  return std::move(log_data_containers_.back());
}

void power_telemetry::PowerTelemetry::RecordLogDataContainer() {
  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      GetCurrentLogDataContainer();

  LOG_INFO("bt_power: scan: %d, inqScan: %d, aclTx: %d, aclRx: %d",
           ldc->scan_ds.count, ldc->inq_scan_ds.count,
           ldc->acl_pkt_ds.tx_pkt_count, ldc->acl_pkt_ds.rx_pkt_count);

  if (log_data_containers_.size() == LOG_DATA_ENTRIES_IN_MEMORY) {
    ldc = std::move(log_data_containers_.front());
    log_data_containers_.pop_front();
  }

  ldc = std::make_unique<power_telemetry::LogDataContainer>();
  log_data_containers_.push_back(std::move(ldc));
}

void power_telemetry::PowerTelemetry::LogInqScanDetails(bool started) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  if (started) {
    inq_scan_count_++;
  }

  if ((GetCurrentTimeSec() - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogBleAdvDetails(bool started) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      GetCurrentLogDataContainer();
  AdvDetails adv_details;
  if (started) {
    adv_details.start_time_stamp = GetCurrentTimeString();
    ldc->adv_list.push_back(adv_details);
  } else {
    adv_details = ldc->adv_list.back();
    adv_details.end_time_stamp = GetCurrentTimeString();
    ldc->adv_list.pop_back();
    ldc->adv_list.push_back(adv_details);
  }
}

void LogTxPower_cb(void* res) {
  power_telemetry::GetInstance()->LogTxPower(res);
}

void power_telemetry::PowerTelemetry::LogTxPower(void* res) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  tBTM_TX_POWER_RESULT* result = (tBTM_TX_POWER_RESULT*)res;
  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      GetCurrentLogDataContainer();

  if (result->status != BTM_SUCCESS) {
    return;
  }

  for (auto it : ldc->acl_link_map) {
    uint16_t handle = it.first;
    LinkDetails lds = it.second;
    if (lds.bdaddr == result->rem_bda) {
      lds.tx_power_level = result->tx_power;
      ldc->acl_link_map[handle] = lds;
      break;
    }
  }
  if ((GetCurrentTimeSec() - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}
void power_telemetry::PowerTelemetry::LogAclLinkDetails(
    uint16_t handle, const RawAddress* bdaddr, bool is_connected) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  LinkDetails link_details;
  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      GetCurrentLogDataContainer();

  if (ldc->acl_link_map.count(handle) != 0) {
    link_details = ldc->acl_link_map[handle];
  }

  if (is_connected == false) {
    link_details.disconnected_ts = GetCurrentTimeString();
    ldc->acl_link_list.push_back(link_details);
    ldc->acl_link_map.erase(handle);
  } else {
    if (bdaddr) {
      link_details.bdaddr = (*bdaddr);
    }
    link_details.handle = handle;
    link_details.connected_ts = GetCurrentTimeString();
    ldc->acl_link_map[handle] = link_details;

    SniffData sniff_data;
    if (ldc->sniff_activity_map.count(handle) != 0)
      ldc->sniff_activity_map.erase(handle);
    if (bdaddr) {
      sniff_data.bdaddr = (*bdaddr);
    }
    sniff_data.active_count = 1;
    sniff_data.last_mode_change_ts = GetCurrentTimeSec();
    ldc->sniff_activity_map[handle] = sniff_data;

    // Read tx power
    // BTM_ReadTxPower(bdaddr, BT_TRANSPORT_BR_EDR, LogTxPower_cb);
  }
  if ((GetCurrentTimeSec() - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogScoLinkDetails(uint16_t handle,
                                                        RawAddress bdaddr,
                                                        bool is_connected) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  LinkDetails link_details;
  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      GetCurrentLogDataContainer();
  if (ldc->sco_link_map.count(handle) != 0) {
    link_details = ldc->sco_link_map[handle];
  } else {
    link_details.bdaddr = bdaddr;
    link_details.handle = handle;
  }

  (is_connected) ? link_details.connected_ts = GetCurrentTimeString()
                 : link_details.disconnected_ts = GetCurrentTimeString();

  if (is_connected == false) {
    ldc->sco_link_list.push_back(link_details);
    ldc->sco_link_map.erase(handle);
  } else {
    ldc->sco_link_map[handle] = link_details;
  }
  if ((GetCurrentTimeSec() - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogHciCmdEvtDetails(int32_t type) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  if (type == DUMP_HCI_CMD) {
    cmd_count_++;
  } else {
    event_count_++;
  }
  if ((GetCurrentTimeSec() - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogSniffActivity(uint16_t handle,
                                                       RawAddress bdaddr,
                                                       bool sniff_entered) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  SniffData sniff_data;
  int64_t current_timestamp = GetCurrentTimeSec();
  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      GetCurrentLogDataContainer();
  if (ldc->sniff_activity_map.count(handle) == 0) {
    sniff_data.bdaddr = bdaddr;
  } else {
    sniff_data = ldc->sniff_activity_map[handle];
  }
  if (sniff_entered) {
    sniff_data.sniff_count++;
    sniff_data.active_duration +=
        current_timestamp - sniff_data.last_mode_change_ts;
  } else {
    sniff_data.active_count++;
    sniff_data.sniff_duration +=
        current_timestamp - sniff_data.last_mode_change_ts;
  }
  sniff_data.last_mode_change_ts = GetCurrentTimeSec();
  ldc->sniff_activity_map[handle] = sniff_data;

  if ((GetCurrentTimeSec() - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogTrafficData() {
  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      GetCurrentLogDataContainer();

  if ((l2c_rx_bytes_ != 0) || (l2c_tx_bytes_ != 0)) {
    ldc->l2c_data.tx_bytes = l2c_tx_bytes_;
    ldc->l2c_data.rx_bytes = l2c_rx_bytes_;
    l2c_tx_bytes_ = l2c_rx_bytes_ = 0;
  }

  if ((rfc_rx_bytes_ != 0) || (rfc_tx_bytes_ != 0)) {
    ldc->rfc_data.tx_bytes = rfc_tx_bytes_;
    ldc->rfc_data.rx_bytes = rfc_rx_bytes_;
    rfc_tx_bytes_ = rfc_rx_bytes_ = 0;
  }

  if (scan_count_ != 0) {
    ldc->scan_ds.count = scan_count_;
    scan_count_ = 0;
  }

  if (inq_scan_count_ != 0) {
    ldc->inq_scan_ds.count = inq_scan_count_;
    inq_scan_count_ = 0;
  }

  if ((acl_rx_pkt_ != 0) || (acl_tx_pkt_ != 0)) {
    ldc->acl_pkt_ds.tx_pkt_count = acl_tx_pkt_;
    ldc->acl_pkt_ds.tx_total_bytes = acl_tx_len_;
    ldc->acl_pkt_ds.rx_pkt_count = acl_rx_pkt_;
    ldc->acl_pkt_ds.rx_total_bytes = acl_rx_len_;
    acl_rx_pkt_ = acl_tx_pkt_ = 0;
  }

  if ((cmd_count_ != 0) || (event_count_ != 0)) {
    ldc->hci_cmd_evt_ds.tx_pkt_count = cmd_count_;
    ldc->hci_cmd_evt_ds.rx_pkt_count = event_count_;
    cmd_count_ = event_count_ = 0;
  }

  ldc->start_time_stamp = GetTimeStringFromSec(traffic_logged_ts_);
  ldc->end_time_stamp = GetCurrentTimeString();

  traffic_logged_ts_ = GetCurrentTimeSec();
  RecordLogDataContainer();
}

void power_telemetry::PowerTelemetry::LogScanStarted() {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  scan_count_++;
  if ((GetCurrentTimeSec() - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogScanEnded() {}

void power_telemetry::PowerTelemetry::LogAclPktDetails(int32_t type,
                                                       uint16_t len) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  if (type == ACL_PKT_TX) {
    acl_tx_pkt_++;
    acl_tx_len_ += len;
  } else {
    acl_rx_pkt_++;
    acl_rx_len_ += len;
  }
  if ((GetCurrentTimeSec() - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogLeScanStarted() {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      GetCurrentLogDataContainer();
  ScanDetails scan_details;
  ldc->scan_le_list.push_back(scan_details);
}

void power_telemetry::PowerTelemetry::LogLeScanEnded() {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      GetCurrentLogDataContainer();
  if (ldc->scan_le_list.size() > 0) {
    ScanDetails scan_details = (ScanDetails)ldc->scan_le_list.back();
    ldc->scan_le_list.pop_back();
    ldc->scan_le_list.push_back(scan_details);
  }
}

void power_telemetry::PowerTelemetry::LogChannelConnected(int32_t channel_type,
                                                          int32_t src_id,
                                                          int32_t dst_id,
                                                          RawAddress bdaddr,
                                                          int32_t psm) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  std::list<ChannelDetails> channel_details_list;
  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      GetCurrentLogDataContainer();
  ChannelDetails channel_details;
  channel_details.src_id = src_id;
  channel_details.dst_id = dst_id;
  channel_details.state = STATE_CONNECTED;
  channel_details.channel_type = channel_type;
  channel_details.conn_time_stamp = GetCurrentTimeString();
  channel_details.psm = psm;

  if (ldc->channel_map.count(bdaddr) == 0) {
    channel_details_list.push_back(channel_details);
    ldc->channel_map.insert(std::pair<RawAddress, std::list<ChannelDetails>>(
        bdaddr, channel_details_list));
  } else {
    channel_details_list = ldc->channel_map[bdaddr];
    channel_details_list.push_back(channel_details);
    ldc->channel_map[bdaddr] = channel_details_list;
  }
  if ((GetCurrentTimeSec() - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogChannelDisconnected(
    int32_t channel_type, int32_t src_id, int32_t dst_id, RawAddress bdaddr) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  std::list<ChannelDetails> channel_details_list;
  std::list<ChannelDetails>::iterator itr;
  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      GetCurrentLogDataContainer();
  if (ldc->channel_map.count(bdaddr) == 0) {
    return;
  } else {
    channel_details_list = ldc->channel_map[bdaddr];
    for (itr = channel_details_list.begin(); itr != channel_details_list.end();
         itr++) {
      if (itr->src_id == src_id && itr->dst_id == dst_id &&
          itr->channel_type == channel_type) {
        itr->state = STATE_DISCONNECTED;
        itr->disconn_time_stamp = GetCurrentTimeString();
        ldc->channel_map[bdaddr] = channel_details_list;
        break;
      }
    }
  }
  if ((GetCurrentTimeSec() - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogTxBytes(int32_t channel_type,
                                                 int32_t src_id, int32_t dst_id,
                                                 RawAddress bdaddr,
                                                 int32_t num_bytes) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  if (log_per_channel_ == true) {
    std::list<ChannelDetails> channel_details_list;
    std::list<ChannelDetails>::iterator itr;
    std::unique_ptr<power_telemetry::LogDataContainer> ldc =
        GetCurrentLogDataContainer();
    if (ldc->channel_map.count(bdaddr) == 0) {
      return;
    } else {
      channel_details_list = ldc->channel_map[bdaddr];
      for (itr = channel_details_list.begin();
           itr != channel_details_list.end(); itr++) {
        if (itr->src_id == src_id && itr->dst_id == dst_id &&
            itr->channel_type == channel_type) {
          itr->tx_bytes += num_bytes;
          itr->last_tx_time_stamp = GetCurrentTimeString();
          ldc->channel_map[bdaddr] = channel_details_list;
          break;
        }
      }
    }
  }
  if (channel_type == CHANNEL_TYPE_RFCOMM)  // RFCOMM
  {
    rfc_tx_bytes_ += num_bytes;
  } else {
    l2c_tx_bytes_ += num_bytes;
  }
  if ((GetCurrentTimeSec() - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogRxBytes(int32_t channel_type,
                                                 int32_t src_id, int32_t dst_id,
                                                 RawAddress bdaddr,
                                                 int32_t num_bytes) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  if (log_per_channel_ == true) {
    std::list<ChannelDetails> channel_details_list;
    std::list<ChannelDetails>::iterator itr;
    std::unique_ptr<power_telemetry::LogDataContainer> ldc =
        GetCurrentLogDataContainer();
    if (ldc->channel_map.count(bdaddr) == 0) {
      return;
    } else {
      channel_details_list = ldc->channel_map[bdaddr];
      for (itr = channel_details_list.begin();
           itr != channel_details_list.end(); itr++) {
        if (itr->src_id == src_id && itr->dst_id == dst_id &&
            itr->channel_type == channel_type) {
          itr->rx_bytes += num_bytes;
          itr->last_rx_time_stamp = GetCurrentTimeString();
          ldc->channel_map[bdaddr] = channel_details_list;
          break;
        }
      }
    }
  }
  if (channel_type == CHANNEL_TYPE_RFCOMM)  // RFCOMM
  {
    rfc_rx_bytes_ += num_bytes;
  } else {
    l2c_rx_bytes_ += num_bytes;
  }
  if ((GetCurrentTimeSec() - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::PowerTelemetryDump(int32_t fd) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  RecordLogDataContainer();

  dprintf(fd, "\nPower Telemetry Data:\n");
  dprintf(fd, "\nBR/EDR Scan Events:\n");
  dprintf(fd, "%-22s %-22s %-15s\n", "StartTimeStamp", "EndTimeStamp",
          "Number of Scans");
  for (auto&& ldc : log_data_containers_) {
    if (ldc->scan_ds.count == 0) {
      continue;
    }
    dprintf(fd, "%-22s %-22s %-15d\n", ldc->start_time_stamp.c_str(),
            ldc->end_time_stamp.c_str(), ldc->scan_ds.count);
  }
  dprintf(fd, "\nBR/EDR InqScan Events:\n");
  dprintf(fd, "%-22s %-22s %-15s\n", "StartTimeStamp", "EndTimeStamp",
          "Number of IngScans");
  for (auto&& ldc : log_data_containers_) {
    if (ldc->inq_scan_ds.count == 0) {
      continue;
    }
    dprintf(fd, "%-22s %-22s %-15d\n", ldc->start_time_stamp.c_str(),
            ldc->end_time_stamp.c_str(), ldc->inq_scan_ds.count);
  }

  dprintf(fd, "\nACL Packet Details:\n");
  dprintf(fd, "%-22s %-22s %-12s %-12s %-12s %-12s\n", "StartTimeStamp",
          "EndTimeStamp", "Tx Packets", "Tx Bytes", "Rx Packets", "Rx Bytes");
  for (auto&& ldc : log_data_containers_) {
    if ((ldc->acl_pkt_ds.tx_total_bytes == 0) &&
        (ldc->acl_pkt_ds.rx_total_bytes == 0)) {
      continue;
    }
    dprintf(fd, "%-22s %-22s %-12d %-12ld %-12d %-12ld\n",
            ldc->start_time_stamp.c_str(), ldc->end_time_stamp.c_str(),
            ldc->acl_pkt_ds.tx_pkt_count, (long)ldc->acl_pkt_ds.tx_total_bytes,
            ldc->acl_pkt_ds.rx_pkt_count, (long)ldc->acl_pkt_ds.rx_total_bytes);
  }

  dprintf(fd, "\nHCI CMD/EVT Details:\n");
  dprintf(fd, "%-22s %-22s %-14s %-14s\n", "StartTimeStamp", "EndTimeStamp",
          "HCI Commands", "HCI Events");
  for (auto&& ldc : log_data_containers_) {
    if ((ldc->hci_cmd_evt_ds.tx_pkt_count == 0) &&
        (ldc->hci_cmd_evt_ds.rx_pkt_count == 0)) {
      continue;
    }
    dprintf(fd, "%-22s %-22s %-14d %-14d\n", ldc->start_time_stamp.c_str(),
            ldc->end_time_stamp.c_str(), ldc->hci_cmd_evt_ds.tx_pkt_count,
            ldc->hci_cmd_evt_ds.rx_pkt_count);
  }
  dprintf(fd, "\nL2CAP/RFCOMM Channel Events:\n");
  dprintf(fd, "%-19s %-7s %-7s %-7s %-8s %-22s", "RemoteAddress", "Type",
          "SrcId", "DstId", "PSM", "ConnectedTimeStamp");
  dprintf(fd, " %-22s %-14s ", "DisconnectedTimeStamp", "State");
  if (log_per_channel_ == true) {
    dprintf(fd, " %-10s %-10s %-22s %-22s", "TxBytes", "RxBytes",
            "LastTxTimeStamp", "LastRxTimeStamp");
  }
  dprintf(fd, "\n");
  for (auto&& ldc : log_data_containers_) {
    for (auto& itr : ldc->channel_map) {
      RawAddress remote_addr = itr.first;
      std::list<ChannelDetails> channel_details_list = itr.second;
      for (auto& channel_details : channel_details_list) {
        dprintf(fd, "%-19s ", remote_addr.ToString().c_str());
        dprintf(fd, "%-7s %-7d %-7d %-8d %-22s %-22s %-14s",
                (channel_details.channel_type == CHANNEL_TYPE_RFCOMM) ? "RFCOMM"
                                                                      : "L2CAP",
                channel_details.src_id, channel_details.dst_id,
                channel_details.psm, channel_details.conn_time_stamp.c_str(),
                channel_details.disconn_time_stamp.c_str(),
                (channel_details.state == STATE_DISCONNECTED) ? "DISCONNECTED"
                                                              : "CONNECTED");
        if (log_per_channel_ == true) {
          dprintf(fd, "%-10ld %-10ld %-22s %-22s",
                  (long)channel_details.tx_bytes,
                  (long)channel_details.rx_bytes,
                  channel_details.last_tx_time_stamp.c_str(),
                  channel_details.last_rx_time_stamp.c_str());
        }
        dprintf(fd, "\n");
      }
    }
  }

  dprintf(fd, "\n\nBluetooth Data Traffic Details\n");
  dprintf(fd, "L2cap Data Traffic\n");
  dprintf(fd, "%-22s %-22s %-10s %-10s\n", "StartTime", "EndTime", "TxBytes",
          "RxBytes");
  for (auto&& ldc : log_data_containers_) {
    dprintf(fd, "%-22s %-22s %-10ld %-10ld\n", ldc->start_time_stamp.c_str(),
            ldc->end_time_stamp.c_str(), (long)ldc->l2c_data.tx_bytes,
            (long)ldc->l2c_data.rx_bytes);
  }

  dprintf(fd, "\nRfcomm Data Traffic\n");
  dprintf(fd, "%-22s %-22s %-10s %-10s\n", "StartTime", "EndTime", "TxBytes",
          "RxBytes");
  for (auto&& ldc : log_data_containers_) {
    dprintf(fd, "%-22s %-22s %-10ld %-10ld\n", ldc->start_time_stamp.c_str(),
            ldc->end_time_stamp.c_str(), (long)ldc->rfc_data.tx_bytes,
            (long)ldc->rfc_data.rx_bytes);
  }

  dprintf(fd, "\n\nSniff Activity Details\n");
  dprintf(fd, "%-8s %-19s %-19s %-24s %-19s %-24s\n", "Handle", "BDADDR",
          "ActiveModeCount", "ActiveModeDuration(sec)", "SniffModeCount",
          "SniffModeDuration(sec)");
  for (auto&& ldc : log_data_containers_) {
    for (auto itr : ldc->sniff_activity_map) {
      uint16_t handle = itr.first;
      SniffData sniff_data = itr.second;
      dprintf(fd, "%-8d %-19s %-19d %-24ld %-19d %-24ld\n", handle,
              sniff_data.bdaddr.ToString().c_str(), sniff_data.active_count,
              (long)sniff_data.active_duration, sniff_data.sniff_count,
              (long)sniff_data.sniff_duration);
    }
  }

  dprintf(fd, "\n\nACL Link Details\n");
  dprintf(fd, "%-6s %-19s %-22s %-22s %-8s\n", "handle", "BDADDR",
          "ConnectedTimeStamp", "DisconnectedTimeStamp", "TxPower");
  for (auto&& ldc : log_data_containers_) {
    for (auto it : ldc->acl_link_map) {
      uint16_t handle = it.first;
      LinkDetails lds = it.second;
      dprintf(fd, "%-6d %-19s %-22s %-22s %-8d\n", handle,
              lds.bdaddr.ToString().c_str(), lds.connected_ts.c_str(),
              lds.disconnected_ts.c_str(), lds.tx_power_level);
    }

    for (auto& it : ldc->acl_link_list) {
      dprintf(fd, "%-6d %-19s %-22s %-22s %-8d\n", it.handle,
              it.bdaddr.ToString().c_str(), it.connected_ts.c_str(),
              it.disconnected_ts.c_str(), it.tx_power_level);
    }
  }
  dprintf(fd, "\nSCO Link Details\n");
  dprintf(fd, "%-6s %-19s %-22s %-22s\n", "handle", "BDADDR",
          "ConnectedTimeStamp", "DisconnectedTimeStamp");
  for (auto&& ldc : log_data_containers_) {
    for (auto& it : ldc->sco_link_list) {
      dprintf(fd, "%-6d %-19s %-22s %-22s\n", it.handle,
              it.bdaddr.ToString().c_str(), it.connected_ts.c_str(),
              it.disconnected_ts.c_str());
    }
  }

  dprintf(fd, "\n\n");
}
