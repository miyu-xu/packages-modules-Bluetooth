#include "osi/include/stack_power_telemetry.h"

#include <sys/stat.h>

#include <cstdio>
#include <filesystem>

#include "bt_trace.h"
#include "osi/include/alarm.h"
#include "stack/btm/btm_dev.h"
#include "stack/btm/btm_int_types.h"
#include "stack/btm/btm_sec.h"

std::string GetTimeString(time_t tstamp) {
  char buffer[15];
  tm* nTm = localtime(&tstamp);
  strftime(buffer, 15, "%m-%d %H:%M:%S", nTm);
  return std::string(buffer);
}

power_telemetry::PowerTelemetry& power_telemetry::GetInstance() {
  static power_telemetry::PowerTelemetry power_telemetry;
  return power_telemetry;
}

power_telemetry::LogDataContainer&
power_telemetry::PowerTelemetry::GetCurrentLogDataContainer() {
  return log_data_containers_[idx_containers];
}

void power_telemetry::PowerTelemetry::RecordLogDataContainer() {
  LogDataContainer& ldc = GetCurrentLogDataContainer();

  LOG_INFO("bt_power: scan: %d, inqScan: %d, aclTx: %d, aclRx: %d",
           ldc.scan_details.count, ldc.inq_scan_details.count,
           ldc.acl_pkt_ds.tx_pkt_count, ldc.acl_pkt_ds.rx_pkt_count);

  idx_containers++;
  if (idx_containers >= kLogEntriesSize) {
    idx_containers = 0;
  }

  log_data_containers_[idx_containers] = LogDataContainer();
}

void power_telemetry::PowerTelemetry::LogInqScanDetails(bool started) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  if (started) {
    inq_scan_count_++;
  }

  if ((time(0) - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogBleAdvDetails(bool started) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  LogDataContainer& ldc = GetCurrentLogDataContainer();
  if (started) {
    ldc.adv_list.emplace_back(AdvDetails{.start_time_stamp = time(0)});
  } else {
    if (ldc.adv_list.size() == 0) {
      LOG_WARN("Empty advList. Skip LogBleAdvDetails.");
      return;
    }
    ldc.adv_list.back().end_time_stamp = time(0);
  }
}

void power_telemetry::PowerTelemetry::LogTxPower(void* res) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  tBTM_TX_POWER_RESULT* result = (tBTM_TX_POWER_RESULT*)res;
  LogDataContainer& ldc = GetCurrentLogDataContainer();

  if (result->status != BTM_SUCCESS) {
    return;
  }

  for (auto it : ldc.acl_link_map) {
    uint16_t handle = it.first;
    LinkDetails lds = it.second;
    if (lds.bdaddr == result->rem_bda) {
      lds.tx_power_level = result->tx_power;
      ldc.acl_link_map[handle] = lds;
      break;
    }
  }
  if ((time(0) - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogLinkDetails(uint16_t handle,
                                                     const RawAddress& bdaddr,
                                                     bool is_connected,
                                                     bool is_acl_link) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  LogDataContainer& ldc = GetCurrentLogDataContainer();
  std::map<uint16_t, LinkDetails>& link_map =
      is_acl_link ? ldc.acl_link_map : ldc.sco_link_map;
  std::list<LinkDetails>& link_list =
      is_acl_link ? ldc.acl_link_list : ldc.sco_link_list;

  if (is_connected == false && link_map.count(handle) != 0) {
    LinkDetails link_details = link_map[handle];
    link_details.disconnected_ts = time(0);
    link_list.push_back(link_details);
    link_map.erase(handle);
  } else if (is_connected == true) {
    link_map[handle] = {
        .bdaddr = bdaddr,
        .handle = handle,
        .connected_ts = time(0),
    };

    if (is_acl_link) {
      SniffData sniff_data;
      if (ldc.sniff_activity_map.count(handle) != 0) {
        ldc.sniff_activity_map.erase(handle);
      }
      sniff_data.bdaddr = bdaddr;
      sniff_data.active_count = 1;
      sniff_data.last_mode_change_ts = time(0);
      ldc.sniff_activity_map[handle] = sniff_data;
    }
  }

  if ((time(0) - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogHciCmdEvtDetails(int32_t type) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  if (type == kDumpHciCmd) {
    cmd_count_++;
  } else {
    event_count_++;
  }
  if ((time(0) - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogSniffActivity(uint16_t handle,
                                                       const RawAddress& bdaddr,
                                                       bool sniff_entered) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  SniffData sniff_data;
  time_t current_timestamp = time(0);
  LogDataContainer& ldc = GetCurrentLogDataContainer();
  if (ldc.sniff_activity_map.count(handle) == 0) {
    sniff_data.bdaddr = bdaddr;
  } else {
    sniff_data = ldc.sniff_activity_map[handle];
  }
  if (sniff_entered) {
    sniff_data.sniff_count++;
    sniff_data.active_duration_ts +=
        current_timestamp - sniff_data.last_mode_change_ts;
  } else {
    sniff_data.active_count++;
    sniff_data.sniff_duration_ts +=
        current_timestamp - sniff_data.last_mode_change_ts;
  }
  sniff_data.last_mode_change_ts = time(0);
  ldc.sniff_activity_map[handle] = sniff_data;

  if ((time(0) - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogTrafficData() {
  LogDataContainer& ldc = GetCurrentLogDataContainer();

  if ((l2c_rx_bytes_ != 0) || (l2c_tx_bytes_ != 0)) {
    ldc.l2c_data = {
        .tx_bytes = l2c_tx_bytes_,
        .rx_bytes = l2c_rx_bytes_,
    };
    l2c_tx_bytes_ = l2c_rx_bytes_ = 0;
  }

  if ((rfc_rx_bytes_ != 0) || (rfc_tx_bytes_ != 0)) {
    ldc.rfc_data = {
        .tx_bytes = rfc_tx_bytes_,
        .rx_bytes = rfc_rx_bytes_,
    };
    rfc_tx_bytes_ = rfc_rx_bytes_ = 0;
  }

  if (scan_count_ != 0) {
    ldc.scan_details.count = scan_count_;
    scan_count_ = 0;
  }

  if (inq_scan_count_ != 0) {
    ldc.inq_scan_details.count = inq_scan_count_;
    inq_scan_count_ = 0;
  }

  if ((acl_rx_pkt_ != 0) || (acl_tx_pkt_ != 0)) {
    ldc.acl_pkt_ds = {
        .tx_pkt_count = acl_tx_pkt_,
        .tx_total_bytes = acl_tx_len_,
        .rx_pkt_count = acl_rx_pkt_,
        .rx_total_bytes = acl_rx_len_,
    };
    acl_rx_pkt_ = acl_tx_pkt_ = 0;
  }

  if ((cmd_count_ != 0) || (event_count_ != 0)) {
    ldc.hci_cmd_evt_ds = {
        .tx_pkt_count = cmd_count_,
        .rx_pkt_count = event_count_,
    };
    cmd_count_ = event_count_ = 0;
  }

  ldc.start_time_stamp = traffic_logged_ts_;
  ldc.end_time_stamp = time(0);

  traffic_logged_ts_ = time(0);
  RecordLogDataContainer();
}

void power_telemetry::PowerTelemetry::LogScanStarted() {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  scan_count_++;
  if ((time(0) - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogAclPktDetails(int32_t type,
                                                       uint16_t len) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  if (type == kAclTxPacket) {
    acl_tx_pkt_++;
    acl_tx_len_ += len;
  } else {
    acl_rx_pkt_++;
    acl_rx_len_ += len;
  }

  if ((time(0) - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogChannelConnected(
    int32_t channel_type, int32_t src_id, int32_t dst_id,
    const RawAddress& bdaddr, int32_t psm) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  std::list<ChannelDetails> channel_details_list;
  LogDataContainer& ldc = GetCurrentLogDataContainer();
  ChannelDetails channel_details = {
      .src_id = src_id,
      .dst_id = dst_id,
      .state = kStateConnected,
      .channel_type = channel_type,
      .conn_time_stamp = time(0),
      .psm = psm,
  };

  if (ldc.channel_map.count(bdaddr) == 0) {
    ldc.channel_map.insert(std::pair<RawAddress, std::list<ChannelDetails>>(
        bdaddr, std::list<ChannelDetails>({channel_details})));
  } else {
    ldc.channel_map[bdaddr].emplace_back(channel_details);
  }

  if ((time(0) - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogChannelDisconnected(
    int32_t channel_type, int32_t src_id, int32_t dst_id,
    const RawAddress& bdaddr) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  std::list<ChannelDetails> channel_details_list;
  LogDataContainer& ldc = GetCurrentLogDataContainer();
  if (ldc.channel_map.count(bdaddr) == 0) {
    return;
  }

  for (auto& channel_detail : ldc.channel_map[bdaddr]) {
    if (channel_detail.src_id == src_id && channel_detail.dst_id == dst_id &&
        channel_detail.channel_type == channel_type) {
      channel_detail.state = kStateDisconnected;
      channel_detail.disconn_time_stamp = time(0);
      break;
    }
  }

  if ((time(0) - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogTxBytes(int32_t channel_type,
                                                 int32_t src_id, int32_t dst_id,
                                                 const RawAddress& bdaddr,
                                                 int32_t num_bytes) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  if (log_per_channel_ == true) {
    std::list<ChannelDetails> channel_details_list;
    LogDataContainer& ldc = GetCurrentLogDataContainer();
    if (ldc.channel_map.count(bdaddr) == 0) {
      return;
    }

    for (auto& channel_details : ldc.channel_map[bdaddr]) {
      if (channel_details.src_id == src_id &&
          channel_details.dst_id == dst_id &&
          channel_details.channel_type == channel_type) {
        channel_details.tx_bytes += num_bytes;
        channel_details.last_tx_time_stamp = time(0);
        break;
      }
    }
  }
  if (channel_type == kChannelRfcomm) {
    rfc_tx_bytes_ += num_bytes;
  } else {
    l2c_tx_bytes_ += num_bytes;
  }
  if ((time(0) - traffic_logged_ts_) >= kTrafficLogTime) {
    LogTrafficData();
  }
}

void power_telemetry::PowerTelemetry::LogRxBytes(int32_t channel_type,
                                                 int32_t src_id, int32_t dst_id,
                                                 const RawAddress& bdaddr,
                                                 int32_t num_bytes) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex_);
  if (log_per_channel_ == true) {
    std::list<ChannelDetails> channel_details_list;
    LogDataContainer& ldc = GetCurrentLogDataContainer();
    if (ldc.channel_map.count(bdaddr) == 0) {
      return;
    }

    for (auto& channel_detail : ldc.channel_map[bdaddr]) {
      if (channel_detail.src_id == src_id && channel_detail.dst_id == dst_id &&
          channel_detail.channel_type == channel_type) {
        channel_detail.rx_bytes += num_bytes;
        channel_detail.last_rx_time_stamp = time(0);
        break;
      }
    }
  }

  if (channel_type == kChannelRfcomm) {
    rfc_rx_bytes_ += num_bytes;
  } else {
    l2c_rx_bytes_ += num_bytes;
  }

  if ((time(0) - traffic_logged_ts_) >= kTrafficLogTime) {
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
    if (ldc.scan_details.count == 0) {
      continue;
    }
    dprintf(fd, "%-22s %-22s %-15d\n",
            GetTimeString(ldc.start_time_stamp).c_str(),
            GetTimeString(ldc.end_time_stamp).c_str(), ldc.scan_details.count);
  }
  dprintf(fd, "\nBR/EDR InqScan Events:\n");
  dprintf(fd, "%-22s %-22s %-15s\n", "StartTimeStamp", "EndTimeStamp",
          "Number of InqScans");
  for (auto&& ldc : log_data_containers_) {
    if (ldc.inq_scan_details.count == 0) {
      continue;
    }
    dprintf(
        fd, "%-22s %-22s %-15d\n", GetTimeString(ldc.start_time_stamp).c_str(),
        GetTimeString(ldc.end_time_stamp).c_str(), ldc.inq_scan_details.count);
  }

  dprintf(fd, "\nACL Packet Details:\n");
  dprintf(fd, "%-22s %-22s %-12s %-12s %-12s %-12s\n", "StartTimeStamp",
          "EndTimeStamp", "Tx Packets", "Tx Bytes", "Rx Packets", "Rx Bytes");
  for (auto&& ldc : log_data_containers_) {
    if ((ldc.acl_pkt_ds.tx_total_bytes == 0) &&
        (ldc.acl_pkt_ds.rx_total_bytes == 0)) {
      continue;
    }
    dprintf(fd, "%-22s %-22s %-12d %-12ld %-12d %-12ld\n",
            GetTimeString(ldc.start_time_stamp).c_str(),
            GetTimeString(ldc.end_time_stamp).c_str(),
            ldc.acl_pkt_ds.tx_pkt_count, (long)ldc.acl_pkt_ds.tx_total_bytes,
            ldc.acl_pkt_ds.rx_pkt_count, (long)ldc.acl_pkt_ds.rx_total_bytes);
  }

  dprintf(fd, "\nHCI CMD/EVT Details:\n");
  dprintf(fd, "%-22s %-22s %-14s %-14s\n", "StartTimeStamp", "EndTimeStamp",
          "HCI Commands", "HCI Events");
  for (auto&& ldc : log_data_containers_) {
    if ((ldc.hci_cmd_evt_ds.tx_pkt_count == 0) &&
        (ldc.hci_cmd_evt_ds.rx_pkt_count == 0)) {
      continue;
    }
    dprintf(fd, "%-22s %-22s %-14d %-14d\n",
            GetTimeString(ldc.start_time_stamp).c_str(),
            GetTimeString(ldc.end_time_stamp).c_str(),
            ldc.hci_cmd_evt_ds.tx_pkt_count, ldc.hci_cmd_evt_ds.rx_pkt_count);
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
    for (auto& itr : ldc.channel_map) {
      const RawAddress& remote_addr = itr.first;
      std::list<ChannelDetails> channel_details_list = itr.second;
      for (auto& channel_details : channel_details_list) {
        dprintf(fd, "%-19s ", ADDRESS_TO_LOGGABLE_CSTR(remote_addr));
        dprintf(fd, "%-7s %-7d %-7d %-8d %-22s %-22s %-14s",
                (channel_details.channel_type == kChannelRfcomm) ? "RFCOMM"
                                                                 : "L2CAP",
                channel_details.src_id, channel_details.dst_id,
                channel_details.psm,
                GetTimeString(channel_details.conn_time_stamp).c_str(),
                GetTimeString(channel_details.disconn_time_stamp).c_str(),
                (channel_details.state == kStateDisconnected) ? "DISCONNECTED"
                                                              : "CONNECTED");
        if (log_per_channel_ == true) {
          dprintf(fd, "%-10ld %-10ld %-22s %-22s",
                  (long)channel_details.tx_bytes,
                  (long)channel_details.rx_bytes,
                  GetTimeString(channel_details.last_tx_time_stamp).c_str(),
                  GetTimeString(channel_details.last_rx_time_stamp).c_str());
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
    dprintf(fd, "%-22s %-22s %-10ld %-10ld\n",
            GetTimeString(ldc.start_time_stamp).c_str(),
            GetTimeString(ldc.end_time_stamp).c_str(),
            (long)ldc.l2c_data.tx_bytes, (long)ldc.l2c_data.rx_bytes);
  }

  dprintf(fd, "\nRfcomm Data Traffic\n");
  dprintf(fd, "%-22s %-22s %-10s %-10s\n", "StartTime", "EndTime", "TxBytes",
          "RxBytes");
  for (auto&& ldc : log_data_containers_) {
    dprintf(fd, "%-22s %-22s %-10ld %-10ld\n",
            GetTimeString(ldc.start_time_stamp).c_str(),
            GetTimeString(ldc.end_time_stamp).c_str(),
            (long)ldc.rfc_data.tx_bytes, (long)ldc.rfc_data.rx_bytes);
  }

  dprintf(fd, "\n\nSniff Activity Details\n");
  dprintf(fd, "%-8s %-19s %-19s %-24s %-19s %-24s\n", "Handle", "BDADDR",
          "ActiveModeCount", "ActiveModeDuration(sec)", "SniffModeCount",
          "SniffModeDuration(sec)");
  for (auto&& ldc : log_data_containers_) {
    for (auto itr : ldc.sniff_activity_map) {
      uint16_t handle = itr.first;
      SniffData sniff_data = itr.second;
      dprintf(fd, "%-8d %-19s %-19d %-24ld %-19d %-24ld\n", handle,
              ADDRESS_TO_LOGGABLE_CSTR(sniff_data.bdaddr),
              sniff_data.active_count, (long)sniff_data.active_duration_ts,
              sniff_data.sniff_count, (long)sniff_data.sniff_duration_ts);
    }
  }

  dprintf(fd, "\n\nACL Link Details\n");
  dprintf(fd, "%-6s %-19s %-22s %-22s %-8s\n", "handle", "BDADDR",
          "ConnectedTimeStamp", "DisconnectedTimeStamp", "TxPower");
  for (auto&& ldc : log_data_containers_) {
    for (auto it : ldc.acl_link_map) {
      uint16_t handle = it.first;
      LinkDetails lds = it.second;
      dprintf(fd, "%-6d %-19s %-22s %-22s %-8d\n", handle,
              ADDRESS_TO_LOGGABLE_CSTR(lds.bdaddr),
              GetTimeString(lds.connected_ts).c_str(),
              GetTimeString(lds.disconnected_ts).c_str(), lds.tx_power_level);
    }

    for (auto& it : ldc.acl_link_list) {
      dprintf(fd, "%-6d %-19s %-22s %-22s %-8d\n", it.handle,
              ADDRESS_TO_LOGGABLE_CSTR(it.bdaddr),
              GetTimeString(it.connected_ts).c_str(),
              GetTimeString(it.disconnected_ts).c_str(), it.tx_power_level);
    }
  }
  dprintf(fd, "\nSCO Link Details\n");
  dprintf(fd, "%-6s %-19s %-22s %-22s\n", "handle", "BDADDR",
          "ConnectedTimeStamp", "DisconnectedTimeStamp");
  for (auto&& ldc : log_data_containers_) {
    for (auto& it : ldc.sco_link_list) {
      dprintf(fd, "%-6d %-19s %-22s %-22s\n", it.handle,
              ADDRESS_TO_LOGGABLE_CSTR(it.bdaddr),
              GetTimeString(it.connected_ts).c_str(),
              GetTimeString(it.disconnected_ts).c_str());
    }
  }

  dprintf(fd, "\n\n");
}
