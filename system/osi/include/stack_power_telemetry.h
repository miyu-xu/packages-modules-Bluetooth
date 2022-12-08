#include <base/logging.h>
#include <time.h>

#include <fstream>
#include <iostream>
#include <list>
#include <map>
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
#define LOG_DATA_FILE "/data/misc/bluetooth/logs/power_telemetry.txt"
#define LOG_DATA_FILE_SIZE_LIMIT 20480
#define LOG_PER_CHANNEL_PROPERTY \
  "bluetooth.powertelemetry.log_per_channel.enabled"
extern long GetCurrentTimeSec();

class LinkDetails {
 public:
  RawAddress bdaddr;
  string connectedTs;
  string disconnectedTs;
  uint16_t handle;
  uint8_t txPowerLevel;
  LinkDetails() { handle = txPowerLevel = 0; }
};
class ChannelDetails {
 public:
  int channelType;
  int srcId;
  int dstId;
  RawAddress remoteAddr;
  long txBytes;
  long rxBytes;
  int state;
  string connTimeStamp;
  string disConnTimeStamp;
  string lastTxTimeStamp;
  string lastRxTimeStamp;
  int psm;

  ChannelDetails() {
    channelType = 0;
    state = 0;
    srcId = 0;
    dstId = 0;
    txBytes = 0;
    rxBytes = 0;
    psm = 0;
  }
};

class AclPktDetails {
 public:
  uint32_t txPktCount;
  long txTotalBytes;
  uint32_t rxPktCount;
  long rxTotalBytes;

  AclPktDetails() {
    txPktCount = rxPktCount = 0;
    txTotalBytes = rxTotalBytes = 0;
  }
};

class AdvDetails {
 public:
  string startTimeStamp;
  string endTimeStamp;
  AdvDetails() { startTimeStamp = endTimeStamp = ""; }
};

class ScanDetails {
 public:
  int count;
  ScanDetails() { count = 0; }
};
class TrafficData {
 public:
  long txBytes;
  long rxBytes;
};

class SniffData {
 public:
  RawAddress bdaddr;
  uint32_t sniffCount, activeCount;
  long sniffDuration, activeDuration;
  long lastModeChangeTs;
  SniffData() {
    sniffCount = activeCount = 0;
    sniffDuration = activeDuration = 0;
    lastModeChangeTs = GetCurrentTimeSec();
  }
};

class LogDataContainer {
 public:
  string startTimeStamp;
  string endTimeStamp;
  map<RawAddress, list<ChannelDetails>> channelMap;
  list<ScanDetails> scanLeList;
  TrafficData l2cData, rfcData;
  map<uint16_t, SniffData> sniffActivityMap;
  map<uint16_t, LinkDetails> aclLinkMap;
  map<uint16_t, LinkDetails> scoLinkMap;
  list<LinkDetails> aclLinkList;
  list<LinkDetails> scoLinkList;
  list<AdvDetails> advList;
  ScanDetails scanDs, inqScanDs;
  AclPktDetails aclPktDs, hciCmdEvtDs;
};

class PowerTelemetry {
 public:
  PowerTelemetry() {
    trafficLoggedTs = GetCurrentTimeSec();
    LogDataContainer* ldc = new LogDataContainer();
    logDataContainers.push_back(ldc);
    logFile.open(LOG_DATA_FILE, ios::app);
    logPerChannel = osi_property_get_bool(LOG_PER_CHANNEL_PROPERTY, false);
  }
  ~PowerTelemetry() {
    for (auto ldc : logDataContainers) {
      delete (ldc);
    }
    logFile.close();
  }
  static PowerTelemetry* GetInstance();
  LogDataContainer* getCurrentLogDataContainer();
  void recordLogDataContainer();
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

 private:
  list<LogDataContainer*> logDataContainers;
  ofstream logFile;
  const long TRAFFIC_LOG_TIME = 120;  // 120seconds
  long trafficLoggedTs = 0;
  long l2cTxBytes = 0;
  long rfcTxBytes = 0;
  long l2cRxBytes = 0;
  long rfcRxBytes = 0;
  uint32_t aclRxPkt = 0, aclTxPkt = 0;
  long aclTxLen = 0, aclRxLen = 0;
  std::mutex dumpsys_mutex;
  uint16_t scanCount = 0, inqScanCount = 0, bleAdvCount = 0;
  uint32_t cmdCount = 0, eventCount = 0;
  bool scanTimerStarted = false;
  bool logPerChannel = false;
};
