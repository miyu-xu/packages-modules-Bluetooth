
#include "osi/include/stack_power_telemetry.h"

#include <sys/stat.h>

#include <cstdio>
#include <ctime>
#include <filesystem>

#include "bt_trace.h"
#include "osi/include/alarm.h"
#include "stack/btm/btm_dev.h"
#include "stack/btm/btm_int_types.h"
#include "stack/btm/btm_sec.h"

using namespace std;

string GetTimeString(time_t tstamp) {
  ostringstream strStm;
  tm* nTm = localtime(&tstamp);
  string hour = (nTm->tm_hour >= 10) ? to_string(nTm->tm_hour)
                                     : "0" + to_string(nTm->tm_hour);
  string min = (nTm->tm_min >= 10) ? to_string(nTm->tm_min)
                                   : "0" + to_string(nTm->tm_min);
  string sec = (nTm->tm_sec >= 10) ? to_string(nTm->tm_sec)
                                   : "0" + to_string(nTm->tm_sec);
  strStm << (nTm->tm_mon + 1) << "-" << nTm->tm_mday << " " << hour << ":"
         << min << ":" << sec;
  return strStm.str();
}
string GetCurrentTimeString() {
  time_t tstamp = time(0);
  return GetTimeString(tstamp);
}

string GetTimeStringFromSec(long timeStampSec) {
  time_t tstamp = time_t(timeStampSec);
  return GetTimeString(tstamp);
}

long GetCurrentTimeSec() {
  time_t tstamp = time(0);
  return (long)tstamp;
}

PowerTelemetry* PowerTelemetry::GetInstance() {
  static PowerTelemetry powerTelemetry;
  return &powerTelemetry;
}

LogDataContainer* PowerTelemetry::getCurrentLogDataContainer() {
  return (LogDataContainer*)logDataContainers.back();
}

void PowerTelemetry::recordLogDataContainer() {
  struct stat logFileStat;
  LogDataContainer* ldc = this->getCurrentLogDataContainer();
  logFile.write((char*)ldc, sizeof(*ldc));
  logFile.flush();

  if (stat(LOG_DATA_FILE, &logFileStat) == -1) {
    return;
  }
  if (logFileStat.st_size >= LOG_DATA_FILE_SIZE_LIMIT) {
    string lastFileName = LOG_DATA_FILE;
    lastFileName += ".last";
    logFile.close();
    rename(LOG_DATA_FILE, lastFileName.c_str());
    logFile.open(LOG_DATA_FILE, ios::app);
  }

  if (logDataContainers.size() == LOG_DATA_ENTRIES_IN_MEMORY) {
    ldc = (LogDataContainer*)logDataContainers.front();
    logDataContainers.pop_front();
    delete (ldc);
  }

  ldc = new LogDataContainer();
  logDataContainers.push_back(ldc);
}

void PowerTelemetry::LogInqScanDetails(bool started) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex);
  if (started) {
    inqScanCount++;
  }

  if ((GetCurrentTimeSec() - trafficLoggedTs) >= TRAFFIC_LOG_TIME) {
    this->LogTrafficData();
  }
}

void PowerTelemetry::LogBleAdvDetails(bool started) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex);
  LogDataContainer* ldc = this->getCurrentLogDataContainer();
  AdvDetails advDs;
  if (started) {
    advDs.startTimeStamp = GetCurrentTimeString();
    ldc->advList.push_back(advDs);
  } else {
    advDs = ldc->advList.back();
    advDs.endTimeStamp = GetCurrentTimeString();
    ldc->advList.pop_back();
    ldc->advList.push_back(advDs);
  }
}

void LogTxPower_cb(void* res) {
  PowerTelemetry::GetInstance()->LogTxPower(res);
}

void PowerTelemetry::LogTxPower(void* res) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex);
  tBTM_TX_POWER_RESULT* result = (tBTM_TX_POWER_RESULT*)res;
  LogDataContainer* ldc = this->getCurrentLogDataContainer();

  if (result->status != BTM_SUCCESS) {
    return;
  }

  for (auto it : ldc->aclLinkMap) {
    uint16_t handle = it.first;
    LinkDetails lds = it.second;
    if (lds.bdaddr == result->rem_bda) {
      lds.txPowerLevel = result->tx_power;
      ldc->aclLinkMap[handle] = lds;
      break;
    }
  }
  if ((GetCurrentTimeSec() - trafficLoggedTs) >= TRAFFIC_LOG_TIME) {
    this->LogTrafficData();
  }
}
void PowerTelemetry::LogAclLinkDetails(uint16_t handle, RawAddress bdaddr,
                                       bool isConnected) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex);
  LinkDetails linkDs;
  LogDataContainer* ldc = this->getCurrentLogDataContainer();

  if (ldc->aclLinkMap.count(handle) != 0) {
    linkDs = ldc->aclLinkMap[handle];
  }

  if (isConnected == false) {
    linkDs.disconnectedTs = GetCurrentTimeString();
    ldc->aclLinkList.push_back(linkDs);
    ldc->aclLinkMap.erase(handle);
  } else {
    linkDs.bdaddr = bdaddr;
    linkDs.handle = handle;
    linkDs.connectedTs = GetCurrentTimeString();
    ldc->aclLinkMap[handle] = linkDs;

    SniffData sData;
    if (ldc->sniffActivityMap.count(handle) != 0)
      ldc->sniffActivityMap.erase(handle);
    sData.bdaddr = bdaddr;
    sData.activeCount = 1;
    sData.lastModeChangeTs = GetCurrentTimeSec();
    ldc->sniffActivityMap[handle] = sData;

    // Read tx power
    // BTM_ReadTxPower(bdaddr, BT_TRANSPORT_BR_EDR, LogTxPower_cb);
  }
  if ((GetCurrentTimeSec() - trafficLoggedTs) >= TRAFFIC_LOG_TIME) {
    this->LogTrafficData();
  }
}

void PowerTelemetry::LogScoLinkDetails(uint16_t handle, RawAddress bdaddr,
                                       bool isConnected) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex);
  LinkDetails linkDs;
  LogDataContainer* ldc = this->getCurrentLogDataContainer();
  if (ldc->scoLinkMap.count(handle) != 0) {
    linkDs = ldc->scoLinkMap[handle];
  } else {
    linkDs.bdaddr = bdaddr;
    linkDs.handle = handle;
  }

  (isConnected) ? linkDs.connectedTs = GetCurrentTimeString()
                : linkDs.disconnectedTs = GetCurrentTimeString();

  if (isConnected == false) {
    ldc->scoLinkList.push_back(linkDs);
    ldc->scoLinkMap.erase(handle);
  } else {
    ldc->scoLinkMap[handle] = linkDs;
  }
  if ((GetCurrentTimeSec() - trafficLoggedTs) >= TRAFFIC_LOG_TIME) {
    this->LogTrafficData();
  }
}

void PowerTelemetry::LogHciCmdEvtDetails(int type) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex);
  if (type == DUMP_HCI_CMD) {
    cmdCount++;
  } else {
    eventCount++;
  }
  if ((GetCurrentTimeSec() - trafficLoggedTs) >= TRAFFIC_LOG_TIME) {
    this->LogTrafficData();
  }
}

void PowerTelemetry::LogSniffActivity(uint16_t handle, RawAddress bdaddr,
                                      bool sniffEntered) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex);
  SniffData sniffData;
  long currentTs = GetCurrentTimeSec();
  LogDataContainer* ldc = this->getCurrentLogDataContainer();
  if (ldc->sniffActivityMap.count(handle) == 0) {
    sniffData.bdaddr = bdaddr;
  } else {
    sniffData = ldc->sniffActivityMap[handle];
  }
  if (sniffEntered) {
    sniffData.sniffCount++;
    sniffData.activeDuration += currentTs - sniffData.lastModeChangeTs;
  } else {
    sniffData.activeCount++;
    sniffData.sniffDuration += currentTs - sniffData.lastModeChangeTs;
  }
  sniffData.lastModeChangeTs = GetCurrentTimeSec();
  ldc->sniffActivityMap[handle] = sniffData;

  if ((GetCurrentTimeSec() - trafficLoggedTs) >= TRAFFIC_LOG_TIME) {
    this->LogTrafficData();
  }
}

void PowerTelemetry::LogTrafficData() {
  LogDataContainer* ldc = this->getCurrentLogDataContainer();

  if ((l2cRxBytes != 0) || (l2cTxBytes != 0)) {
    ldc->l2cData.txBytes = l2cTxBytes;
    ldc->l2cData.rxBytes = l2cRxBytes;
    l2cTxBytes = l2cRxBytes = 0;
  }

  if ((rfcRxBytes != 0) || (rfcTxBytes != 0)) {
    ldc->rfcData.txBytes = rfcTxBytes;
    ldc->rfcData.rxBytes = rfcRxBytes;
    rfcTxBytes = rfcRxBytes = 0;
  }

  if (scanCount != 0) {
    ldc->scanDs.count = scanCount;
    scanCount = 0;
  }

  if (inqScanCount != 0) {
    ldc->inqScanDs.count = inqScanCount;
    inqScanCount = 0;
  }

  if ((aclRxPkt != 0) || (aclTxPkt != 0)) {
    ldc->aclPktDs.txPktCount = aclTxPkt;
    ldc->aclPktDs.txTotalBytes = aclTxLen;
    ldc->aclPktDs.rxPktCount = aclRxPkt;
    ldc->aclPktDs.rxTotalBytes = aclRxLen;
    aclRxPkt = aclTxPkt = 0;
  }

  if ((cmdCount != 0) || (eventCount != 0)) {
    ldc->hciCmdEvtDs.txPktCount = cmdCount;
    ldc->hciCmdEvtDs.rxPktCount = eventCount;
    cmdCount = eventCount = 0;
  }

  ldc->startTimeStamp = GetTimeStringFromSec(trafficLoggedTs);
  ldc->endTimeStamp = GetCurrentTimeString();

  trafficLoggedTs = GetCurrentTimeSec();
  this->recordLogDataContainer();
}

void PowerTelemetry::LogScanStarted() {
  std::lock_guard<std::mutex> lock(dumpsys_mutex);
  scanCount++;
  if ((GetCurrentTimeSec() - trafficLoggedTs) >= TRAFFIC_LOG_TIME) {
    this->LogTrafficData();
  }
}

void PowerTelemetry::LogScanEnded() {}

void PowerTelemetry::LogAclPktDetails(int type, uint16_t len) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex);
  if (type == ACL_PKT_TX) {
    aclTxPkt++;
    aclTxLen += len;
  } else {
    aclRxPkt++;
    aclRxLen += len;
  }
  if ((GetCurrentTimeSec() - trafficLoggedTs) >= TRAFFIC_LOG_TIME) {
    this->LogTrafficData();
  }
}

void PowerTelemetry::LogLeScanStarted() {
  std::lock_guard<std::mutex> lock(dumpsys_mutex);
  LogDataContainer* ldc = this->getCurrentLogDataContainer();
  ScanDetails scDts;
  ldc->scanLeList.push_back(scDts);
}

void PowerTelemetry::LogLeScanEnded() {
  std::lock_guard<std::mutex> lock(dumpsys_mutex);
  LogDataContainer* ldc = this->getCurrentLogDataContainer();
  if (ldc->scanLeList.size() > 0) {
    ScanDetails scDts = (ScanDetails)ldc->scanLeList.back();
    ldc->scanLeList.pop_back();
    ldc->scanLeList.push_back(scDts);
  }
}

void PowerTelemetry::LogChannelConnected(int channel_type, int src_id,
                                         int dst_id, RawAddress bd_addr,
                                         int psm) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex);
  list<ChannelDetails> chDtsList;
  LogDataContainer* ldc = this->getCurrentLogDataContainer();
  ChannelDetails chDts;
  chDts.srcId = src_id;
  chDts.dstId = dst_id;
  chDts.state = STATE_CONNECTED;
  chDts.channelType = channel_type;
  chDts.connTimeStamp = GetCurrentTimeString();
  chDts.psm = psm;

  if (ldc->channelMap.count(bd_addr) == 0) {
    chDtsList.push_back(chDts);
    ldc->channelMap.insert(
        std::pair<RawAddress, list<ChannelDetails>>(bd_addr, chDtsList));
  } else {
    chDtsList = ldc->channelMap[bd_addr];
    chDtsList.push_back(chDts);
    ldc->channelMap[bd_addr] = chDtsList;
  }
  if ((GetCurrentTimeSec() - trafficLoggedTs) >= TRAFFIC_LOG_TIME) {
    this->LogTrafficData();
  }
}

void PowerTelemetry::LogChannelDisconnected(int channel_type, int src_id,
                                            int dst_id, RawAddress bd_addr) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex);
  list<ChannelDetails> chDtsList;
  list<ChannelDetails>::iterator itr;
  LogDataContainer* ldc = this->getCurrentLogDataContainer();
  if (ldc->channelMap.count(bd_addr) == 0) {
    return;
  } else {
    chDtsList = ldc->channelMap[bd_addr];
    for (itr = chDtsList.begin(); itr != chDtsList.end(); itr++) {
      if (itr->srcId == src_id && itr->dstId == dst_id &&
          itr->channelType == channel_type) {
        itr->state = STATE_DISCONNECTED;
        itr->disConnTimeStamp = GetCurrentTimeString();
        ldc->channelMap[bd_addr] = chDtsList;
        break;
      }
    }
  }
  if ((GetCurrentTimeSec() - trafficLoggedTs) >= TRAFFIC_LOG_TIME) {
    this->LogTrafficData();
  }
}

void PowerTelemetry::LogTxBytes(int channel_type, int src_id, int dst_id,
                                RawAddress bd_addr, int num_bytes) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex);
  if (logPerChannel == true) {
    list<ChannelDetails> chDtsList;
    list<ChannelDetails>::iterator itr;
    LogDataContainer* ldc = this->getCurrentLogDataContainer();
    if (ldc->channelMap.count(bd_addr) == 0) {
      return;
    } else {
      chDtsList = ldc->channelMap[bd_addr];
      for (itr = chDtsList.begin(); itr != chDtsList.end(); itr++) {
        if (itr->srcId == src_id && itr->dstId == dst_id &&
            itr->channelType == channel_type) {
          itr->txBytes += num_bytes;
          itr->lastTxTimeStamp = GetCurrentTimeString();
          ldc->channelMap[bd_addr] = chDtsList;
          break;
        }
      }
    }
  }
  if (channel_type == CHANNEL_TYPE_RFCOMM)  // RFCOMM
  {
    rfcTxBytes += num_bytes;
  } else {
    l2cTxBytes += num_bytes;
  }
  if ((GetCurrentTimeSec() - trafficLoggedTs) >= TRAFFIC_LOG_TIME) {
    this->LogTrafficData();
  }
}

void PowerTelemetry::LogRxBytes(int channel_type, int src_id, int dst_id,
                                RawAddress bd_addr, int num_bytes) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex);
  if (logPerChannel == true) {
    list<ChannelDetails> chDtsList;
    list<ChannelDetails>::iterator itr;
    LogDataContainer* ldc = this->getCurrentLogDataContainer();
    if (ldc->channelMap.count(bd_addr) == 0) {
      return;
    } else {
      chDtsList = ldc->channelMap[bd_addr];
      for (itr = chDtsList.begin(); itr != chDtsList.end(); itr++) {
        if (itr->srcId == src_id && itr->dstId == dst_id &&
            itr->channelType == channel_type) {
          itr->rxBytes += num_bytes;
          itr->lastRxTimeStamp = GetCurrentTimeString();
          ldc->channelMap[bd_addr] = chDtsList;
          break;
        }
      }
    }
  }
  if (channel_type == CHANNEL_TYPE_RFCOMM)  // RFCOMM
  {
    rfcRxBytes += num_bytes;
  } else {
    l2cRxBytes += num_bytes;
  }
  if ((GetCurrentTimeSec() - trafficLoggedTs) >= TRAFFIC_LOG_TIME) {
    this->LogTrafficData();
  }
}

void PowerTelemetry::PowerTelemetryDump(int fd) {
  std::lock_guard<std::mutex> lock(dumpsys_mutex);
  this->recordLogDataContainer();

  dprintf(fd, "\nPower Telemetry Data:\n");
  dprintf(fd, "\nBR/EDR Scan Events:\n");
  dprintf(fd, "%-22s %-22s %-15s\n", "StartTimeStamp", "EndTimeStamp",
          "Number of Scans");
  for (auto ldc : logDataContainers) {
    if (ldc->scanDs.count == 0) {
      continue;
    }
    dprintf(fd, "%-22s %-22s %-15d\n", ldc->startTimeStamp.c_str(),
            ldc->endTimeStamp.c_str(), ldc->scanDs.count);
  }
  dprintf(fd, "\nBR/EDR InqScan Events:\n");
  dprintf(fd, "%-22s %-22s %-15s\n", "StartTimeStamp", "EndTimeStamp",
          "Number of IngScans");
  for (auto ldc : logDataContainers) {
    if (ldc->inqScanDs.count == 0) {
      continue;
    }
    dprintf(fd, "%-22s %-22s %-15d\n", ldc->startTimeStamp.c_str(),
            ldc->endTimeStamp.c_str(), ldc->inqScanDs.count);
  }

  dprintf(fd, "\nACL Packet Details:\n");
  dprintf(fd, "%-22s %-22s %-12s %-12s %-12s %-12s\n", "StartTimeStamp",
          "EndTimeStamp", "Tx Packets", "Tx Bytes", "Rx Packets", "Rx Bytes");
  for (auto ldc : logDataContainers) {
    if ((ldc->aclPktDs.txTotalBytes == 0) &&
        (ldc->aclPktDs.rxTotalBytes == 0)) {
      continue;
    }
    dprintf(fd, "%-22s %-22s %-12d %-12ld %-12d %-12ld\n",
            ldc->startTimeStamp.c_str(), ldc->endTimeStamp.c_str(),
            ldc->aclPktDs.txPktCount, ldc->aclPktDs.txTotalBytes,
            ldc->aclPktDs.rxPktCount, ldc->aclPktDs.rxTotalBytes);
  }

  dprintf(fd, "\nHCI CMD/EVT Details:\n");
  dprintf(fd, "%-22s %-22s %-14s %-14s\n", "StartTimeStamp", "EndTimeStamp",
          "HCI Commands", "HCI Events");
  for (auto ldc : logDataContainers) {
    if ((ldc->hciCmdEvtDs.txPktCount == 0) &&
        (ldc->hciCmdEvtDs.rxPktCount == 0)) {
      continue;
    }
    dprintf(fd, "%-22s %-22s %-14d %-14d\n", ldc->startTimeStamp.c_str(),
            ldc->endTimeStamp.c_str(), ldc->hciCmdEvtDs.txPktCount,
            ldc->hciCmdEvtDs.rxPktCount);
  }
  dprintf(fd, "\nL2CAP/RFCOMM Channel Events:\n");
  dprintf(fd, "%-19s %-7s %-7s %-7s %-8s %-22s", "RemoteAddress", "Type",
          "SrcId", "DstId", "PSM", "ConnectedTimeStamp");
  dprintf(fd, " %-22s %-14s ", "DisconnectedTimeStamp", "State");
  if (logPerChannel == true) {
    dprintf(fd, " %-10s %-10s %-22s %-22s", "TxBytes", "RxBytes",
            "LastTxTimeStamp", "LastRxTimeStamp");
  }
  dprintf(fd, "\n");
  for (auto ldc : logDataContainers) {
    for (auto& itr : ldc->channelMap) {
      RawAddress remAddr = itr.first;
      list<ChannelDetails> chDtsList = itr.second;
      for (auto& chDts : chDtsList) {
        dprintf(fd, "%-19s ", remAddr.ToString().c_str());
        dprintf(
            fd, "%-7s %-7d %-7d %-8d %-22s %-22s %-14s",
            (chDts.channelType == CHANNEL_TYPE_RFCOMM) ? "RFCOMM" : "L2CAP",
            chDts.srcId, chDts.dstId, chDts.psm, chDts.connTimeStamp.c_str(),
            chDts.disConnTimeStamp.c_str(),
            (chDts.state == STATE_DISCONNECTED) ? "DISCONNECTED" : "CONNECTED");
        if (logPerChannel == true) {
          dprintf(fd, "%-10ld %-10ld %-22s %-22s", chDts.txBytes, chDts.rxBytes,
                  chDts.lastTxTimeStamp.c_str(), chDts.lastRxTimeStamp.c_str());
        }
        dprintf(fd, "\n");
      }
    }
  }

  dprintf(fd, "\n\nBluetooth Data Traffic Details\n");
  dprintf(fd, "L2cap Data Traffic\n");
  dprintf(fd, "%-22s %-22s %-10s %-10s\n", "StartTime", "EndTime", "TxBytes",
          "RxBytes");
  for (auto ldc : logDataContainers) {
    dprintf(fd, "%-22s %-22s %-10ld %-10ld\n", ldc->startTimeStamp.c_str(),
            ldc->endTimeStamp.c_str(), ldc->l2cData.txBytes,
            ldc->l2cData.rxBytes);
  }

  dprintf(fd, "\nRfcomm Data Traffic\n");
  dprintf(fd, "%-22s %-22s %-10s %-10s\n", "StartTime", "EndTime", "TxBytes",
          "RxBytes");
  for (auto ldc : logDataContainers) {
    dprintf(fd, "%-22s %-22s %-10ld %-10ld\n", ldc->startTimeStamp.c_str(),
            ldc->endTimeStamp.c_str(), ldc->rfcData.txBytes,
            ldc->rfcData.rxBytes);
  }

  dprintf(fd, "\n\nSniff Activity Details\n");
  dprintf(fd, "%-8s %-19s %-19s %-24s %-19s %-24s\n", "Handle", "BDADDR",
          "ActiveModeCount", "ActiveModeDuration(sec)", "SniffModeCount",
          "SniffModeDuration(sec)");
  for (auto ldc : logDataContainers) {
    for (auto itr : ldc->sniffActivityMap) {
      uint16_t handle = itr.first;
      SniffData sData = itr.second;
      dprintf(fd, "%-8d %-19s %-19d %-24ld %-19d %-24ld\n", handle,
              sData.bdaddr.ToString().c_str(), sData.activeCount,
              sData.activeDuration, sData.sniffCount, sData.sniffDuration);
    }
  }

  dprintf(fd, "\n\nACL Link Details\n");
  dprintf(fd, "%-6s %-19s %-22s %-22s %-8s\n", "handle", "BDADDR",
          "ConnectedTimeStamp", "DisconnectedTimeStamp", "TxPower");
  for (auto ldc : logDataContainers) {
    for (auto it : ldc->aclLinkMap) {
      uint16_t handle = it.first;
      LinkDetails lds = it.second;
      dprintf(fd, "%-6d %-19s %-22s %-22s %-8d\n", handle,
              lds.bdaddr.ToString().c_str(), lds.connectedTs.c_str(),
              lds.disconnectedTs.c_str(), lds.txPowerLevel);
    }

    for (auto& it : ldc->aclLinkList) {
      dprintf(fd, "%-6d %-19s %-22s %-22s %-8d\n", it.handle,
              it.bdaddr.ToString().c_str(), it.connectedTs.c_str(),
              it.disconnectedTs.c_str(), it.txPowerLevel);
    }
  }
  dprintf(fd, "\nSCO Link Details\n");
  dprintf(fd, "%-6s %-19s %-22s %-22s\n", "handle", "BDADDR",
          "ConnectedTimeStamp", "DisconnectedTimeStamp");
  for (auto ldc : logDataContainers) {
    for (auto& it : ldc->scoLinkList) {
      dprintf(fd, "%-6d %-19s %-22s %-22s\n", it.handle,
              it.bdaddr.ToString().c_str(), it.connectedTs.c_str(),
              it.disconnectedTs.c_str());
    }
  }

  dprintf(fd, "\n\n");
}
