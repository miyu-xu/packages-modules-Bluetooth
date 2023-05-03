#include "osi/include/stack_power_telemetry.h"

#include <gtest/gtest.h>

#include "AllocationTestHarness.h"
#include "stack/include/acl_api_types.h"
#include "stack/include/btm_status.h"

using namespace power_telemetry;

class MockPowerTelemetry : public PowerTelemetry {
 public:
  static MockPowerTelemetry* GetInstance() {
    static MockPowerTelemetry powerTelemetry;
    return &powerTelemetry;
  }

  void resetNodes() {
    log_data_containers_.clear();
    std::unique_ptr<power_telemetry::LogDataContainer> ldc =
        std::make_unique<power_telemetry::LogDataContainer>();
    log_data_containers_.push_back(std::move(ldc));
  }

  void setEnabledPowerTelemetry(bool enabled) {
    power_telemerty_enabled_ = enabled;
  }

  int getNodeSize() { return log_data_containers_.size(); }
  uint16_t getInqScanCount() { return inq_scan_count_; }
  uint32_t getCmdCount() { return cmd_count_; }
  uint32_t getEventCount() { return event_count_; }
  uint16_t getScanCount() { return scan_count_; }
  uint32_t getAclRxPkt() { return acl_rx_pkt_; }
  uint32_t getAclTxPkt() { return acl_tx_pkt_; }
  long getAclRxLen() { return acl_rx_len_; }
  long getAclTxLen() { return acl_tx_len_; }
  long getRfcTxBytes() { return rfc_tx_bytes_; }
  long getL2cTxBytes() { return l2c_tx_bytes_; }
  long getRfcRxBytes() { return rfc_rx_bytes_; }
  long getL2cRxBytes() { return l2c_rx_bytes_; }
};

class PowerTelemetryTest : public AllocationTestHarness {
 protected:
  uint16_t handle = 123;
  RawAddress bdaddr;
  bool isConnected = true;

  void reset() {
    // Reset all variable
    MockPowerTelemetry::GetInstance()->LogTrafficData();
    // Reset records
    MockPowerTelemetry::GetInstance()->resetNodes();
  }

  void SetUp() override {
    AllocationTestHarness::SetUp();

    // Enable the feature flag
    MockPowerTelemetry::GetInstance()->setEnabledPowerTelemetry(true);
    RawAddress::FromString("00:00:00:00:00:00", bdaddr);
  }

  void TearDown() override { AllocationTestHarness::TearDown(); }
};

TEST_F(PowerTelemetryTest, test_getCurrentLogDataContainer) {
  reset();

  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      MockPowerTelemetry::GetInstance()->GetCurrentLogDataContainer();
  ASSERT_TRUE(ldc != NULL);

  // Record smth, log size increases to 2
  MockPowerTelemetry::GetInstance()->RecordLogDataContainer();
  ASSERT_EQ(2, MockPowerTelemetry::GetInstance()->getNodeSize());
}

TEST_F(PowerTelemetryTest, test_recordLogDataContainer) {
  reset();

  // Create maximum number of nodes
  for (int i = 1; i < LOG_DATA_ENTRIES_IN_MEMORY; i++) {
    MockPowerTelemetry::GetInstance()->RecordLogDataContainer();
    ASSERT_EQ(i + 1, MockPowerTelemetry::GetInstance()->getNodeSize());
  }

  // Create 1 more node. Size of nodes shouldn't over LOG_DATA_ENTRIES_IN_MEMORY
  MockPowerTelemetry::GetInstance()->RecordLogDataContainer();
  ASSERT_EQ(LOG_DATA_ENTRIES_IN_MEMORY,
            MockPowerTelemetry::GetInstance()->getNodeSize());
}

TEST_F(PowerTelemetryTest, test_LogInqScanDetails) {
  reset();

  MockPowerTelemetry::GetInstance()->LogInqScanDetails(false);
  ASSERT_EQ(0, MockPowerTelemetry::GetInstance()->getInqScanCount());

  MockPowerTelemetry::GetInstance()->LogInqScanDetails(true);
  ASSERT_EQ(1, MockPowerTelemetry::GetInstance()->getInqScanCount());
}

TEST_F(PowerTelemetryTest, test_LogBleAdvDetails) {
  reset();

  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      MockPowerTelemetry::GetInstance()->GetCurrentLogDataContainer();

  // Failed Case. Shouldn't crash if run false first
  MockPowerTelemetry::GetInstance()->LogBleAdvDetails(false);
  ASSERT_EQ(0, (int)ldc->adv_list.size());

  // Add new BleAdv data
  MockPowerTelemetry::GetInstance()->LogBleAdvDetails(true);
  ASSERT_EQ(1, (int)ldc->adv_list.size());

  // BleAdv data update endTime
  MockPowerTelemetry::GetInstance()->LogBleAdvDetails(false);
  ASSERT_EQ(1, (int)ldc->adv_list.size());
  ASSERT_NE("", ldc->adv_list.back().end_time_stamp);

  // Add new BleAdv data
  MockPowerTelemetry::GetInstance()->LogBleAdvDetails(true);
  ASSERT_EQ(2, (int)ldc->adv_list.size());
}

TEST_F(PowerTelemetryTest, test_LogTxPower) {
  reset();

  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      MockPowerTelemetry::GetInstance()->GetCurrentLogDataContainer();
  isConnected = true;
  tBTM_TX_POWER_RESULT dummy_res;
  dummy_res.rem_bda = bdaddr;

  // Failed Case. Shouldn't crash if no init data
  dummy_res.status = BTM_SUCCESS;
  void* p = &dummy_res;
  MockPowerTelemetry::GetInstance()->LogTxPower(p);

  // init data
  MockPowerTelemetry::GetInstance()->LogAclLinkDetails(handle, &bdaddr,
                                                       isConnected);

  // Successful case
  dummy_res.tx_power = 100;
  MockPowerTelemetry::GetInstance()->LogTxPower(p);
  ASSERT_EQ(dummy_res.tx_power, ldc->acl_link_map[handle].tx_power_level);

  // Failed case
  dummy_res.tx_power = 99;
  dummy_res.status = BTM_UNDEFINED;
  MockPowerTelemetry::GetInstance()->LogTxPower(p);
  ASSERT_NE(dummy_res.tx_power, ldc->acl_link_map[handle].tx_power_level);
}

TEST_F(PowerTelemetryTest, test_LogAclLinkDetails) {
  reset();
  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      MockPowerTelemetry::GetInstance()->GetCurrentLogDataContainer();

  // Failed Case. Shouldn't crash if first invoke function with false
  isConnected = false;
  MockPowerTelemetry::GetInstance()->LogAclLinkDetails(handle, &bdaddr,
                                                       isConnected);
  ASSERT_EQ(0, (int)ldc->acl_link_list.size());

  // Successful case
  isConnected = true;
  MockPowerTelemetry::GetInstance()->LogAclLinkDetails(handle, &bdaddr,
                                                       isConnected);
  ASSERT_EQ(1, (int)ldc->acl_link_map.count(handle));
  ASSERT_EQ(0, (int)ldc->acl_link_list.size());
  ASSERT_EQ(1, (int)ldc->sniff_activity_map.count(handle));

  isConnected = false;
  MockPowerTelemetry::GetInstance()->LogAclLinkDetails(handle, &bdaddr,
                                                       isConnected);
  ASSERT_EQ(0, (int)ldc->acl_link_map.count(handle));
  ASSERT_EQ(1, (int)ldc->acl_link_list.size());
}

TEST_F(PowerTelemetryTest, test_LogScoLinkDetails) {
  reset();
  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      MockPowerTelemetry::GetInstance()->GetCurrentLogDataContainer();

  // Failed Case. Shouldn't crash if first invoke function with false
  isConnected = false;
  MockPowerTelemetry::GetInstance()->LogScoLinkDetails(handle, bdaddr,
                                                       isConnected);
  ASSERT_EQ(0, (int)ldc->sco_link_list.size());

  // Successful case
  isConnected = true;
  MockPowerTelemetry::GetInstance()->LogScoLinkDetails(handle, bdaddr,
                                                       isConnected);
  ASSERT_EQ(1, (int)ldc->sco_link_map.count(handle));
  ASSERT_EQ(0, (int)ldc->sco_link_list.size());

  isConnected = false;
  MockPowerTelemetry::GetInstance()->LogScoLinkDetails(handle, bdaddr,
                                                       isConnected);
  ASSERT_EQ(0, (int)ldc->sco_link_map.count(handle));
  ASSERT_EQ(1, (int)ldc->sco_link_list.size());
}

TEST_F(PowerTelemetryTest, test_LogHciCmdEvtDetails) {
  reset();

  // After log hci_cmd, the number of it should be 1
  MockPowerTelemetry::GetInstance()->LogHciCmdEvtDetails(DUMP_HCI_CMD);
  ASSERT_EQ(1, (int)MockPowerTelemetry::GetInstance()->getCmdCount());
  ASSERT_EQ(0, (int)MockPowerTelemetry::GetInstance()->getEventCount());

  // After log hci_evt, the number of it should be 1
  MockPowerTelemetry::GetInstance()->LogHciCmdEvtDetails(DUMP_HCI_EVENT);
  ASSERT_EQ(1, (int)MockPowerTelemetry::GetInstance()->getCmdCount());
  ASSERT_EQ(1, (int)MockPowerTelemetry::GetInstance()->getEventCount());
}

TEST_F(PowerTelemetryTest, test_LogSniffActivity) {
  reset();
  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      MockPowerTelemetry::GetInstance()->GetCurrentLogDataContainer();

  MockPowerTelemetry::GetInstance()->LogSniffActivity(handle, bdaddr, true);
  ASSERT_EQ(1, (int)ldc->sniff_activity_map[handle].sniff_count);
  ASSERT_EQ(0, (int)ldc->sniff_activity_map[handle].active_count);

  MockPowerTelemetry::GetInstance()->LogSniffActivity(handle, bdaddr, false);
  ASSERT_EQ(1, (int)ldc->sniff_activity_map[handle].sniff_count);
  ASSERT_EQ(1, (int)ldc->sniff_activity_map[handle].active_count);
}

TEST_F(PowerTelemetryTest, test_LogTrafficData) {
  reset();

  // We should create new node.
  MockPowerTelemetry::GetInstance()->LogTrafficData();
  ASSERT_EQ(2, (int)MockPowerTelemetry::GetInstance()->getNodeSize());
}

TEST_F(PowerTelemetryTest, test_LogScanStarted) {
  reset();

  MockPowerTelemetry::GetInstance()->LogScanStarted();
  ASSERT_EQ(1, (int)MockPowerTelemetry::GetInstance()->getScanCount());
}

TEST_F(PowerTelemetryTest, test_LogAclPktDetails) {
  reset();

  // scanCount should be 1
  MockPowerTelemetry::GetInstance()->LogAclPktDetails(ACL_PKT_TX, 10);
  ASSERT_EQ(1, (int)MockPowerTelemetry::GetInstance()->getAclTxPkt());
  ASSERT_EQ(10, (int)MockPowerTelemetry::GetInstance()->getAclTxLen());

  MockPowerTelemetry::GetInstance()->LogAclPktDetails(ACL_PKT_RX, 11);
  ASSERT_EQ(1, (int)MockPowerTelemetry::GetInstance()->getAclRxPkt());
  ASSERT_EQ(11, (int)MockPowerTelemetry::GetInstance()->getAclRxLen());
}

TEST_F(PowerTelemetryTest, test_LogLeScanStarted) {
  reset();
  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      MockPowerTelemetry::GetInstance()->GetCurrentLogDataContainer();

  MockPowerTelemetry::GetInstance()->LogLeScanStarted();
  ASSERT_EQ(1, (int)ldc->scan_le_list.size());
}

TEST_F(PowerTelemetryTest, test_LogChannelConnected) {
  reset();
  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      MockPowerTelemetry::GetInstance()->GetCurrentLogDataContainer();

  MockPowerTelemetry::GetInstance()->LogChannelConnected(0, 0, 0, bdaddr, 0);
  ASSERT_EQ(1, (int)ldc->channel_map[bdaddr].size());
  ASSERT_EQ(STATE_CONNECTED, ldc->channel_map[bdaddr].back().state);

  MockPowerTelemetry::GetInstance()->LogChannelConnected(0, 0, 0, bdaddr, 0);
  ASSERT_EQ(2, (int)ldc->channel_map[bdaddr].size());
  ASSERT_EQ(STATE_CONNECTED, ldc->channel_map[bdaddr].back().state);
}

TEST_F(PowerTelemetryTest, test_LogChannelDisconnected) {
  reset();
  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      MockPowerTelemetry::GetInstance()->GetCurrentLogDataContainer();

  MockPowerTelemetry::GetInstance()->LogChannelConnected(0, 0, 0, bdaddr, 0);
  MockPowerTelemetry::GetInstance()->LogChannelDisconnected(0, 0, 0, bdaddr);
  ASSERT_EQ(STATE_DISCONNECTED, ldc->channel_map[bdaddr].back().state);

  RawAddress dummyAddr;
  RawAddress::FromString("00:00:00:00:00:11", dummyAddr);
  MockPowerTelemetry::GetInstance()->LogChannelDisconnected(0, 0, 0, bdaddr);
  ASSERT_EQ(1, (int)ldc->channel_map[bdaddr].size());
}

TEST_F(PowerTelemetryTest, test_LogTxBytes) {
  reset();

  MockPowerTelemetry::GetInstance()->LogTxBytes(CHANNEL_TYPE_RFCOMM, 0, 0,
                                                bdaddr, 10);
  ASSERT_EQ(10, (int)MockPowerTelemetry::GetInstance()->getRfcTxBytes());

  MockPowerTelemetry::GetInstance()->LogTxBytes(CHANNEL_TYPE_L2CAP, 0, 0,
                                                bdaddr, 11);
  ASSERT_EQ(11, (int)MockPowerTelemetry::GetInstance()->getL2cTxBytes());
}

TEST_F(PowerTelemetryTest, test_LogRxBytes) {
  reset();

  MockPowerTelemetry::GetInstance()->LogRxBytes(CHANNEL_TYPE_RFCOMM, 0, 0,
                                                bdaddr, 10);
  ASSERT_EQ(10, (int)MockPowerTelemetry::GetInstance()->getRfcRxBytes());

  MockPowerTelemetry::GetInstance()->LogRxBytes(CHANNEL_TYPE_L2CAP, 0, 0,
                                                bdaddr, 11);
  ASSERT_EQ(11, (int)MockPowerTelemetry::GetInstance()->getL2cRxBytes());
}

TEST_F(PowerTelemetryTest, test_feature_flag) {
  reset();

  // init data
  isConnected = true;
  tBTM_TX_POWER_RESULT dummy_res;
  dummy_res.rem_bda = bdaddr;
  dummy_res.status = BTM_SUCCESS;
  void* p = &dummy_res;
  MockPowerTelemetry::GetInstance()->LogAclLinkDetails(handle, &bdaddr,
                                                       isConnected);
  std::unique_ptr<power_telemetry::LogDataContainer> ldc =
      MockPowerTelemetry::GetInstance()->GetCurrentLogDataContainer();

  // Set feature flag to false
  MockPowerTelemetry::GetInstance()->setEnabledPowerTelemetry(false);

  // All function shouldn't work if flag is false
  MockPowerTelemetry::GetInstance()->PowerTelemetryDump(0);
  ASSERT_EQ(1, MockPowerTelemetry::GetInstance()->getNodeSize());

  MockPowerTelemetry::GetInstance()->LogRxBytes(CHANNEL_TYPE_RFCOMM, 0, 0,
                                                bdaddr, 87);
  ASSERT_EQ(0, (int)MockPowerTelemetry::GetInstance()->getRfcRxBytes());

  MockPowerTelemetry::GetInstance()->LogTxBytes(CHANNEL_TYPE_RFCOMM, 0, 0,
                                                bdaddr, 10);
  ASSERT_EQ(0, (int)MockPowerTelemetry::GetInstance()->getRfcTxBytes());

  MockPowerTelemetry::GetInstance()->LogChannelConnected(0, 0, 0, bdaddr, 0);
  ASSERT_EQ(0, (int)ldc->channel_map.count(bdaddr));

  MockPowerTelemetry::GetInstance()->LogChannelDisconnected(0, 0, 0, bdaddr);
  ASSERT_EQ(0, (int)ldc->channel_map.count(bdaddr));

  MockPowerTelemetry::GetInstance()->LogLeScanStarted();
  ASSERT_EQ(0, (int)ldc->scan_le_list.size());

  MockPowerTelemetry::GetInstance()->LogAclPktDetails(ACL_PKT_TX, 10);
  ASSERT_EQ(0, (int)MockPowerTelemetry::GetInstance()->getAclTxPkt());

  MockPowerTelemetry::GetInstance()->LogScanStarted();
  ASSERT_EQ(0, (int)MockPowerTelemetry::GetInstance()->getScanCount());

  // Set to 1 because of default value
  MockPowerTelemetry::GetInstance()->LogTrafficData();
  ASSERT_EQ(1, (int)MockPowerTelemetry::GetInstance()->getNodeSize());

  MockPowerTelemetry::GetInstance()->LogSniffActivity(handle, bdaddr, true);
  ASSERT_EQ(0, (int)ldc->sniff_activity_map[handle].sniff_count);

  MockPowerTelemetry::GetInstance()->LogHciCmdEvtDetails(DUMP_HCI_CMD);
  ASSERT_EQ(0, (int)MockPowerTelemetry::GetInstance()->getCmdCount());

  MockPowerTelemetry::GetInstance()->LogScoLinkDetails(handle, bdaddr,
                                                       isConnected);
  ASSERT_EQ(0, (int)ldc->sco_link_map.count(handle));

  // Set to 1 because of fake data
  MockPowerTelemetry::GetInstance()->LogAclLinkDetails(handle, &bdaddr,
                                                       isConnected);
  ASSERT_EQ(1, (int)ldc->acl_link_map.count(handle));

  dummy_res.tx_power = 100;
  MockPowerTelemetry::GetInstance()->LogTxPower(p);
  ASSERT_EQ(0, ldc->acl_link_map[handle].tx_power_level);

  MockPowerTelemetry::GetInstance()->LogBleAdvDetails(true);
  ASSERT_EQ(0, (int)ldc->adv_list.size());

  MockPowerTelemetry::GetInstance()->LogInqScanDetails(true);
  ASSERT_EQ(0, MockPowerTelemetry::GetInstance()->getInqScanCount());

  // Set to 1 because of default value
  MockPowerTelemetry::GetInstance()->RecordLogDataContainer();
  ASSERT_EQ(1, MockPowerTelemetry::GetInstance()->getNodeSize());

  MockPowerTelemetry::GetInstance()->setEnabledPowerTelemetry(true);
}
