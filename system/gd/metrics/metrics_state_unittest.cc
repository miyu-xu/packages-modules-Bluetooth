#include "metrics_state.h"

#include <gmock/gmock.h>

#include <cstdint>
#include <vector>

#include "gtest/gtest.h"
#include "hci/address.h"

//
using android::bluetooth::le::LEACLConnectionState;
using android::bluetooth::le::LEConnectionOriginType;
using android::bluetooth::le::LEConnectionState;
using android::bluetooth::le::LEConnectionType;

LEACLConnectionState le_acl_state = LEACLConnectionState::LE_ACL_UNSPECIFIED;

namespace bluetooth {
namespace os {

// Mock Test Method for uploading sessions
void LogMetricBluetoothLEConnection(){
}

}  // namespace os
}  // namespace bluetooth

namespace bluetooth {
namespace metrics {

const hci::Address address1 = hci::Address({0x11, 0x22, 0x33, 0x44, 0x55, 0x66});

namespace {

class LEConnectionMetricsRemoteDeviceTest : public ::testing::Test {};

TEST(LEConnectionMetricsRemoteDeviceTest, Initialize) {
  ASSERT_EQ(0, 0);
}

TEST(LEConnectionMetricsRemoteDeviceTest, ConnectionStarted) {

  auto argument_list = std::vector<std::pair<os::ArgumentType, int>>();
  MetricsCollector::GetLEConnectionMetricsCollector()->AddStateChangedEvent(
      address1,
      LEConnectionOriginType::ORIGIN_NATIVE,
      LEConnectionType::CONNECTION_TYPE_LE_ACL,
      LEConnectionState::STATE_LE_ACL_START,
      argument_list);

  MetricsCollector::GetLEConnectionMetricsCollector()->AddStateChangedEvent(
      address1,
      LEConnectionOriginType::ORIGIN_NATIVE,
      LEConnectionType::CONNECTION_TYPE_LE_ACL,
      LEConnectionState::STATE_LE_ACL_END,
      argument_list);
  // assert that these are equal
  ASSERT_EQ(le_acl_state, LEACLConnectionState::LE_ACL_UNSPECIFIED);
}

}  // namespace
}  // namespace metrics
}  // namespace bluetooth
