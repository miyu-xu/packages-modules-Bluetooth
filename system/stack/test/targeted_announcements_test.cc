#include "stack/gatt/targeted_announcements.h"

#include <base/bind_helpers.h>
#include <base/functional/bind.h>
#include <base/functional/callback.h>
#include <base/location.h>
#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include <memory>

#include "common/init_flags.h"
#include "osi/include/alarm.h"
#include "osi/test/alarm_mock.h"
#include "stack/test/common/mock_btm_api_layer.h"

using testing::_;
using testing::DoAll;
using testing::Mock;
using testing::Return;
using testing::SaveArg;

using namespace targeted_announcements;

namespace {
const char* test_flags[] = {
    "INIT_logging_debug_enabled_for_all=true",
    nullptr,
};

// convenience mock, for verifying acceptlist operations on lower layer are
// actually scheduled
class AcceptlistMock {
 public:
  MOCK_METHOD1(AcceptlistAdd, bool(const RawAddress&));
  MOCK_METHOD1(AcceptlistRemove, void(const RawAddress&));
  MOCK_METHOD0(AcceptlistClear, void());
  MOCK_METHOD2(OnConnectionTimedOut, void(uint8_t, const RawAddress&));

  /* Not really accept list related, btui still BTM - just for testing put it
   * here. */
  MOCK_METHOD2(EnableTargetedAnnouncements, void(bool, tBTM_INQ_RESULTS_CB*));
};

std::unique_ptr<AcceptlistMock> localAcceptlistMock;

RawAddress address1{{0x01, 0x01, 0x01, 0x01, 0x01, 0x01}};
RawAddress address2{{0x22, 0x22, 0x02, 0x22, 0x33, 0x22}};

constexpr tAPP_ID CLIENT1 = 1;
constexpr tAPP_ID CLIENT2 = 2;

}  // namespace

// Implementation of btm_ble_bgconn.h API for test.
bool BTM_AcceptlistAdd(const RawAddress& address) {
  return localAcceptlistMock->AcceptlistAdd(address);
}

void BTM_AcceptlistRemove(const RawAddress& address) {
  return localAcceptlistMock->AcceptlistRemove(address);
}

void BTM_AcceptlistClear() { return localAcceptlistMock->AcceptlistClear(); }

void BTM_BleTargetAnnouncementObserve(bool enable,
                                      tBTM_INQ_RESULTS_CB* p_results_cb) {
  localAcceptlistMock->EnableTargetedAnnouncements(enable, p_results_cb);
}

void BTM_LogHistory(const std::string& tag, const RawAddress& bd_addr,
                    const std::string& msg){};

namespace bluetooth {
namespace shim {
bool is_gd_l2cap_enabled() { return false; }
void set_target_announcements_filter(bool enable) {}
}  // namespace shim
}  // namespace bluetooth

bool L2CA_ConnectFixedChnl(uint16_t fixed_cid, const RawAddress& bd_addr) {
  return false;
}
uint16_t BTM_GetHCIConnHandle(RawAddress const&, unsigned char) {
  return 0xFFFF;
};

namespace connection_manager {
void on_connection_timed_out(uint8_t app_id, const RawAddress& address) {
  TargetedAnnouncementsManager::Get().CancelConnect(app_id, address);
  localAcceptlistMock->OnConnectionTimedOut(app_id, address);
}
}  // namespace connection_manager

namespace {

class TargetedAnnouncementsTest : public testing::Test {
  void SetUp() override {
    bluetooth::common::InitFlags::Load(test_flags);
    localAcceptlistMock = std::make_unique<AcceptlistMock>();
  }

  void TearDown() override {
    connection_manager::reset(true);
    AlarmMock::Reset();
    localAcceptlistMock.reset();
  }
};

TEST_F(TargetedAnnouncementsTest, test_target_announement_connect) {
  EXPECT_CALL(*localAcceptlistMock, AcceptlistRemove(_)).Times(0);

  EXPECT_TRUE(TargetedAnnouncementsManager::Get().Connect(CLIENT1, address1));
  EXPECT_TRUE(TargetedAnnouncementsManager::Get().Connect(CLIENT1, address1));
}

TEST_F(TargetedAnnouncementsTest,
       test_add_targeted_announement_when_allow_list_used) {
  /* Accept adding to allow list */
  EXPECT_CALL(*localAcceptlistMock, AcceptlistAdd(address1))
      .WillOnce(Return(true));
  /* This shall be called when registering announcements */
  EXPECT_CALL(*localAcceptlistMock, AcceptlistRemove(_)).Times(1);

  EXPECT_TRUE(connection_manager::background_connect_add(CLIENT1, address1));
  EXPECT_TRUE(TargetedAnnouncementsManager::Get().Connect(CLIENT2, address1));
}

TEST_F(TargetedAnnouncementsTest,
       test_add_background_connect_when_targeted_announcement_are_enabled) {
  /* Accept adding to allow list */
  EXPECT_CALL(*localAcceptlistMock, AcceptlistAdd(address1)).Times(0);
  /* This shall be called when registering announcements */
  EXPECT_CALL(*localAcceptlistMock, AcceptlistRemove(_)).Times(0);

  EXPECT_TRUE(TargetedAnnouncementsManager::Get().Connect(CLIENT2, address1));
  EXPECT_TRUE(connection_manager::background_connect_add(CLIENT1, address1));
}

TEST_F(TargetedAnnouncementsTest,
       test_re_add_background_connect_to_allow_list) {
  EXPECT_CALL(*localAcceptlistMock, AcceptlistAdd(address1)).Times(0);
  EXPECT_CALL(*localAcceptlistMock, AcceptlistRemove(_)).Times(0);

  EXPECT_TRUE(TargetedAnnouncementsManager::Get().Connect(CLIENT2, address1));

  EXPECT_TRUE(connection_manager::background_connect_add(CLIENT1, address1));
  Mock::VerifyAndClearExpectations(localAcceptlistMock.get());

  /* Now remove app using targeted announcement and expect device
   * to be added to white list
   */

  /* Accept adding to allow list */
  EXPECT_CALL(*localAcceptlistMock, AcceptlistAdd(address1))
      .WillOnce(Return(true));

  EXPECT_TRUE(
      TargetedAnnouncementsManager::Get().CancelConnect(CLIENT2, address1));
  Mock::VerifyAndClearExpectations(localAcceptlistMock.get());

  EXPECT_CALL(*localAcceptlistMock, AcceptlistRemove(_)).Times(1);
  EXPECT_TRUE(connection_manager::background_connect_remove(CLIENT1, address1));
  Mock::VerifyAndClearExpectations(localAcceptlistMock.get());
}

}  // namespace
