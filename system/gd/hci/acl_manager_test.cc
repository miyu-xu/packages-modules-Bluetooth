/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "hci/acl_manager.h"

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include <algorithm>
#include <chrono>
#include <future>
#include <list>
#include <map>

#include "common/testing/packet.h"
#include "hci/address.h"
#include "hci/class_of_device.h"
#include "hci/controller.h"
#include "hci/hci_layer_fake.h"
#include "os/handler.h"
#include "os/thread.h"
#include "packet/raw_builder.h"

using namespace bluetooth;

using common::BidiQueue;
using common::BidiQueueEnd;
using packet::kLittleEndian;

using hci::acl_manager::ClassicAclConnection;
using hci::acl_manager::LeAclConnection;

using namespace common::testing::packet;

namespace {
constexpr size_t kAclQueueBufferDepth = 3;

constexpr bool kIsDirect = true;
constexpr bool kIsLocallyInitiated = true;

constexpr std::chrono::milliseconds kCompletionHandlerTimeout = std::chrono::milliseconds(1000);
constexpr std::chrono::milliseconds kQueueTimeout = std::chrono::seconds(2000);
constexpr std::chrono::milliseconds kReturnEventTimeout = std::chrono::seconds(2000);
constexpr std::chrono::milliseconds kSynchronizeModuleTimeout = std::chrono::milliseconds(20);

const hci::Address kLocalRandomAddress({0x05, 0x04, 0x03, 0x02, 0x01, 0xd0});
const hci::AddressWithType kLocalRandomAddressWithType(
    kLocalRandomAddress, hci::AddressType::RANDOM_DEVICE_ADDRESS);

const hci::Address kRemoteAddress({0x11, 0x22, 0x33, 0x44, 0x55, 0x66});
const hci::Address kRemoteAddress2({0x22, 0x44, 0x66, 0x88, 0xaa, 0xcc});
const hci::AddressWithType kRemoteWithPublicType(
    kRemoteAddress, hci::AddressType::PUBLIC_DEVICE_ADDRESS);
const hci::AddressWithType kRemoteWithPublicType2(
    kRemoteAddress2, hci::AddressType::PUBLIC_DEVICE_ADDRESS);
const hci::Address kRemoteRandomAddress({0xd0, 0x05, 0x04, 0x03, 0x02, 0x01});
hci::AddressWithType kRemoteWithRandomType(
    kRemoteRandomAddress, hci::AddressType::RANDOM_DEVICE_ADDRESS);
const hci::AddressWithType kEmptyAddressWithType = hci::AddressWithType();

constexpr uint16_t kHciHandle = 123;
constexpr uint8_t kNumPackets = 1;

constexpr uint16_t kL2capChannelIdentifier = 2468;

constexpr uint16_t kLeBufferSizeTotalNumLePackets = 2;
constexpr uint16_t kLeBufferSizeLeDataPacketLength = 32;
constexpr uint16_t kAclBufferLength = 1024;
constexpr uint16_t kTotalAclBuffers = 2;

constexpr uint16_t kHoldModeMinInterval = 0x0020;
constexpr uint16_t kHoldModeMaxInterval = 0x0500;

constexpr uint16_t kConnectionInterval = 0x0100;
constexpr uint16_t kConnectionLatency = 0x0010;
constexpr uint16_t kSupervisionTimeout = 0x0500;

const auto kMinimumRotationTime = std::chrono::milliseconds(7 * 60 * 1000);
const auto kMaximumRotationTime = std::chrono::milliseconds(15 * 60 * 1000);

class MockConnectionCallback : public hci::acl_manager::ConnectionCallbacks {
 public:
  MOCK_METHOD(void, HACK_OnEscoConnectRequest, (hci::Address, hci::ClassOfDevice), (override));
  MOCK_METHOD(void, HACK_OnScoConnectRequest, (hci::Address, hci::ClassOfDevice), (override));
  MOCK_METHOD(void, OnConnectFail, (hci::Address, hci::ErrorCode, bool), (override));
  MOCK_METHOD(void, OnConnectSuccess, (std::unique_ptr<ClassicAclConnection>), (override));
};

class MockConnectionManagementCallbacks : public hci::acl_manager::ConnectionManagementCallbacks {
 public:
  MOCK_METHOD(void, OnAuthenticationComplete, (hci::ErrorCode hci_status), (override));
  MOCK_METHOD(void, OnCentralLinkKeyComplete, (hci::KeyFlag flag), (override));
  MOCK_METHOD(void, OnChangeConnectionLinkKeyComplete, (), (override));
  MOCK_METHOD(void, OnConnectionPacketTypeChanged, (uint16_t packet_type), (override));
  MOCK_METHOD(void, OnDisconnection, (hci::ErrorCode reason), (override));
  MOCK_METHOD(void, OnEncryptionChange, (hci::EncryptionEnabled enabled), (override));
  MOCK_METHOD(
      void,
      OnFlowSpecificationComplete,
      (hci::FlowDirection flow_direction,
       hci::ServiceType service_type,
       uint32_t token_rate,
       uint32_t token_bucket_size,
       uint32_t peak_bandwidth,
       uint32_t access_latency),
      (override));
  MOCK_METHOD(void, OnFlushOccurred, (), (override));
  MOCK_METHOD(
      void,
      OnModeChange,
      (hci::ErrorCode status, hci::Mode current_mode, uint16_t interval),
      (override));
  MOCK_METHOD(
      void,
      OnQosSetupComplete,
      (hci::ServiceType service_type,
       uint32_t token_rate,
       uint32_t peak_bandwidth,
       uint32_t latency,
       uint32_t delay_variation),
      (override));
  MOCK_METHOD(
      void,
      OnReadAfhChannelMapComplete,
      (hci::AfhMode afh_mode, (std::array<uint8_t, 10>)afh_channel_map),
      (override));
  MOCK_METHOD(void, OnReadAutomaticFlushTimeoutComplete, (uint16_t flush_timeout), (override));
  MOCK_METHOD(void, OnReadClockComplete, (uint32_t clock, uint16_t accuracy), (override));
  MOCK_METHOD(void, OnReadClockOffsetComplete, (uint16_t clock_offset), (override));
  MOCK_METHOD(
      void, OnReadFailedContactCounterComplete, (uint16_t failed_contact_counter), (override));
  MOCK_METHOD(void, OnReadLinkPolicySettingsComplete, (uint16_t link_policy_settings), (override));
  MOCK_METHOD(void, OnReadLinkQualityComplete, (uint8_t link_quality), (override));
  MOCK_METHOD(
      void, OnReadLinkSupervisionTimeoutComplete, (uint16_t link_supervision_timeout), (override));
  MOCK_METHOD(
      void,
      OnReadRemoteExtendedFeaturesComplete,
      (uint8_t page_number, uint8_t max_page_number, uint64_t features),
      (override));
  MOCK_METHOD(void, OnReadRemoteSupportedFeaturesComplete, (uint64_t features), (override));
  MOCK_METHOD(
      void,
      OnReadRemoteVersionInformationComplete,
      (hci::ErrorCode hci_status,
       uint8_t lmp_version,
       uint16_t manufacturer_name,
       uint16_t sub_version),
      (override));
  MOCK_METHOD(void, OnReadRssiComplete, (uint8_t rssi), (override));
  MOCK_METHOD(void, OnReadTransmitPowerLevelComplete, (uint8_t transmit_power_level), (override));
  MOCK_METHOD(void, OnRoleChange, (hci::ErrorCode hci_status, hci::Role new_role), (override));
  MOCK_METHOD(void, OnRoleDiscoveryComplete, (hci::Role current_role), (override));
  MOCK_METHOD(
      void,
      OnSniffSubrating,
      (hci::ErrorCode status,
       uint16_t maximum_transmit_latency,
       uint16_t maximum_receive_latency,
       uint16_t minimum_remote_timeout,
       uint16_t minimum_local_timeout),
      (override));
};

class MockLeConnectionCallbacks : public hci::acl_manager::LeConnectionCallbacks {
 public:
  MOCK_METHOD(
      void,
      OnLeConnectFail,
      (hci::AddressWithType, hci::ErrorCode reason, bool locally_initiated),
      (override));

  MOCK_METHOD(
      void,
      OnLeConnectSuccess,
      (hci::AddressWithType address_with_type, std::unique_ptr<LeAclConnection> connection),
      (override));
};

class MockLeConnectionManagementCallbacks
    : public hci::acl_manager::LeConnectionManagementCallbacks {
 public:
  MOCK_METHOD(
      void,
      OnConnectionUpdate,
      (hci::ErrorCode hci_status,
       uint16_t connection_interval,
       uint16_t connection_latency,
       uint16_t supervision_timeout));
  MOCK_METHOD(
      void,
      OnDataLengthChange,
      (uint16_t tx_octets, uint16_t tx_time, uint16_t rx_octets, uint16_t rx_time));
  MOCK_METHOD(void, OnDisconnection, (hci::ErrorCode reason), (override));
  MOCK_METHOD(void, OnLeReadRemoteFeaturesComplete, (hci::ErrorCode hci_status, uint64_t features));
  MOCK_METHOD(
      void,
      OnLeSubrateChange,
      (hci::ErrorCode hci_status,
       uint16_t subrate_factor,
       uint16_t peripheral_latency,
       uint16_t continuation_number,
       uint16_t supervision_timeout));
  MOCK_METHOD(void, OnLocalAddressUpdate, (hci::AddressWithType address_with_type));
  MOCK_METHOD(void, OnPhyUpdate, (hci::ErrorCode hci_status, uint8_t tx_phy, uint8_t rx_phy));
  MOCK_METHOD(
      void,
      OnReadRemoteVersionInformationComplete,
      (hci::ErrorCode hci_status,
       uint8_t version,
       uint16_t manufacturer_name,
       uint16_t sub_version));
};

}  // namespace

namespace bluetooth {
namespace hci {

class TestController : public Controller {
 public:
  void RegisterCompletedAclPacketsCallback(
      common::ContextualCallback<void(uint16_t /* hci_handle */, uint16_t /* packets */)> cb)
      override {
    acl_cb_ = cb;
  }

  void UnregisterCompletedAclPacketsCallback() override {
    acl_cb_ = {};
  }

  uint16_t GetAclPacketLength() const override {
    return acl_buffer_length_;
  }

  uint16_t GetNumAclPacketBuffers() const override {
    return total_acl_buffers_;
  }

  bool IsSupported(bluetooth::hci::OpCode op_code) const override {
    return false;
  }

  LeBufferSize GetLeBufferSize() const override {
    return LeBufferSize(
        le_buffer_size_total_num_le_packets_, le_buffer_size_le_data_packet_length_);
  }

  void CompletePackets(uint16_t hci_handle, uint16_t packets) {
    acl_cb_.Invoke(hci_handle, packets);
  }

  uint16_t le_buffer_size_total_num_le_packets_ = kLeBufferSizeTotalNumLePackets;
  uint16_t le_buffer_size_le_data_packet_length_ = kLeBufferSizeLeDataPacketLength;

  uint16_t acl_buffer_length_ = kAclBufferLength;
  uint16_t total_acl_buffers_ = kTotalAclBuffers;
  common::ContextualCallback<void(uint16_t /* hci_handle */, uint16_t /* packets */)> acl_cb_;

 protected:
  void Start() override {}
  void Stop() override {}
  void ListDependencies(ModuleList* list) const {}
};

struct l2cap_header_t {
  uint16_t pdu_size;
  uint16_t cid;
} __attribute__((packed));

// Simple creator until L2cap PDL packets that use proper builders and endianness
// Endiness does not matter in this test case as the same endianess architecture
// is used throughout.
std::unique_ptr<BasePacketBuilder> BuildL2capPayload(
    uint16_t hci_handle, uint32_t packet_number, const std::vector<uint8_t>& data) {
  auto payload = std::make_unique<packet::RawBuilder>();
  payload->AddOctets2(sizeof(hci_handle) + sizeof(packet_number) + data.size());  // L2CAP PDU size
  payload->AddOctets2(kL2capChannelIdentifier);                                   // L2CAP cid
  payload->AddOctets2(hci_handle);
  payload->AddOctets4(packet_number);
  payload->AddOctets(data);
  return std::move(payload);
}

std::unique_ptr<AclBuilder> NextAclPacket(
    uint16_t hci_handle, uint32_t packet_number, const std::vector<uint8_t>& data) {
  PacketBoundaryFlag packet_boundary_flag = PacketBoundaryFlag::FIRST_AUTOMATICALLY_FLUSHABLE;
  BroadcastFlag broadcast_flag = BroadcastFlag::POINT_TO_POINT;
  return AclBuilder::Create(
      hci_handle,
      packet_boundary_flag,
      broadcast_flag,
      BuildL2capPayload(hci_handle, packet_number, data));
}

class RemoteDevice {
 public:
  RemoteDevice(os::Handler* handler, BidiQueueEnd<AclView, AclBuilder>* queue_down_end)
      : handler_(handler), queue_down_end_(queue_down_end) {
    queue_down_end_->RegisterDequeue(
        handler_, common::Bind(&RemoteDevice::on_incoming_packet, common::Unretained(this)));
  }

  ~RemoteDevice() {
    queue_down_end_->UnregisterDequeue();
    queue_down_end_ = nullptr;
  }

  void on_incoming_packet() {
    auto packet = queue_down_end_->TryDequeue();
    if (packet == nullptr) {
      LOG_ERROR("Got notified for packet available in queue but none here");
      return;
    }
    incoming_packets_.push(std::move(packet));
  }

 private:
  os::Handler* handler_;
  BidiQueueEnd<AclView, AclBuilder>* queue_down_end_;

  std::queue<std::unique_ptr<AclView>> outgoing_packets_;
  std::queue<std::unique_ptr<AclBuilder>> incoming_packets_;

 public:
  // Enqueue a single payload onto an ACL packet from this remote device to be ingested
  void EnqueueAclData(
      uint16_t hci_handle, uint32_t packet_number = 1, const std::vector<uint8_t>& data = {}) {
    ASSERT_TRUE(
        data.size() <
        (UINT16_MAX - (sizeof(l2cap_header_t) + sizeof(hci_handle) + sizeof(packet_number))));
    std::unique_ptr<packet::BasePacketBuilder> builder =
        NextAclPacket(hci_handle, packet_number, data);
    packet::PacketView<kLittleEndian> view = GetPacketView(std::move(builder));
    auto acl_view = AclView::Create(view);
    outgoing_packets_.push(std::make_unique<AclView>(acl_view));
  }

  //  void EnqueueAclData(uint16_t hci_handle, uint32_t packet_number = 1) {
  //    std::vector<uint8_t> data;
  //    EnqueueAclData(hci_handle, packet_number, data);
  //  }

  // Sends a single ACL packet from the remote
  void SendAclDataSync() {
    ASSERT_FALSE(outgoing_packets_.empty());
    std::unique_ptr<AclView> acl_view = std::move(outgoing_packets_.front());
    outgoing_packets_.pop();

    std::promise<void> promise;
    auto future = promise.get_future();
    queue_down_end_->RegisterEnqueue(
        handler_,
        common::Bind(
            [](decltype(queue_down_end_) queue_down_end,
               std::unique_ptr<AclView> acl_view,
               std::promise<void> promise) {
              queue_down_end->UnregisterEnqueue();
              promise.set_value();
              LOG_INFO("Sent packet");
              return acl_view;
            },
            queue_down_end_,
            common::Passed(std::move(acl_view)),
            common::Passed(std::move(promise))));
    LOG_INFO("Wating for packet to be picked up");
    ASSERT_EQ(std::future_status::ready, future.wait_for(kQueueTimeout));
  }

  void EnqueueAndSendAclDataSync(
      uint16_t hci_handle, uint32_t packet_number, const std::vector<uint8_t>& data) {
    EnqueueAclData(hci_handle, packet_number, data);
    SendAclDataSync();
  }

  void SendIncomingDataSync(uint16_t hci_handle, uint32_t packet_number = 1) {
    EnqueueAclData(hci_handle, packet_number);
    SendAclDataSync();
  }

  bool IsNoOutgoingAclData() {
    return queue_down_end_->TryDequeue() == nullptr;
  }
};

class TestHciLayerWithAclData : public hci::TestHciLayer {
 public:
  void Disconnect(uint16_t hci_handle, ErrorCode reason) override {
    hci::TestHciLayer::Disconnect(hci_handle, reason);
  }

  BidiQueueEnd<AclBuilder, AclView>* GetAclQueueEnd() override {
    return acl_queue_.GetUpEnd();
  }

  BidiQueueEnd<AclView, AclBuilder>* GetAclQueueDownEnd() {
    return acl_queue_.GetDownEnd();
  }

  bool ReturnIncomingLeEventSync(std::unique_ptr<LeMetaEventBuilder> event_builder) {
    IncomingLeMetaEvent(std::move(event_builder));
    return true;
  }

 private:
  BidiQueue<AclView, AclBuilder> acl_queue_{kAclQueueBufferDepth};
};

namespace acl_manager {

class AclManagerBaseTest : public ::testing::Test {
 protected:
  void SetUp() override {
    test_hci_layer_ = new TestHciLayerWithAclData;  // Ownership is transferred to registry
    test_controller_ = new TestController;
    fake_registry_.InjectTestModule(&HciLayer::Factory, test_hci_layer_);
    fake_registry_.InjectTestModule(&Controller::Factory, test_controller_);
    handler_ = fake_registry_.GetTestModuleHandler(&HciLayer::Factory);
    fake_registry_.Start<AclManager>(&thread_);
    acl_manager_ = static_cast<AclManager*>(fake_registry_.GetModuleUnderTest(&AclManager::Factory));
    ASSERT_NE(acl_manager_, nullptr);
  }

  void TearDown() override {
    // Invalid mutex exception is raised if the connections
    // are cleared after the AclConnectionInterface is deleted
    // through fake_registry_.
    fake_registry_.SynchronizeModuleHandler(&HciLayer::Factory, kSynchronizeModuleTimeout);
    fake_registry_.SynchronizeModuleHandler(&AclManager::Factory, kSynchronizeModuleTimeout);
    fake_registry_.StopAll();
  }

  TestModuleRegistry fake_registry_;
  TestHciLayerWithAclData* test_hci_layer_ = nullptr;
  TestController* test_controller_ = nullptr;
  AclManager* acl_manager_ = nullptr;
  os::Handler* handler_ = nullptr;
  AddressWithType local_initiating_address_with_type_;
  const bool use_connect_list_ = true;  // gd currently only supports connect list

 private:
  os::Thread& thread_ = fake_registry_.GetTestThread();

 protected:
  MockConnectionCallback mock_connection_callbacks_;
  MockLeConnectionCallbacks mock_le_connection_callbacks_;

  MockConnectionManagementCallbacks mock_connection_management_callbacks_;
  MockLeConnectionManagementCallbacks mock_le_connection_management_callbacks_;

  void ReturnIncomingEvent(std::unique_ptr<EventBuilder> event_builder) {
    test_hci_layer_->IncomingEvent(std::move(event_builder));
  }

  void ReturnIncomingEvent(std::unique_ptr<LeMetaEventBuilder> event_builder) {
    test_hci_layer_->IncomingEvent(std::move(event_builder));
  }

  CommandView LastCommand() {
    return test_hci_layer_->GetCommand();
  }

  void sync_client_handler() {
    std::promise<void> promise;
    auto future = promise.get_future();
    handler_->Post(common::BindOnce(&std::promise<void>::set_value, common::Unretained(&promise)));
    auto future_status = future.wait_for(std::chrono::seconds(1));
    EXPECT_EQ(future_status, std::future_status::ready);
  }

  void RegisterClassicCallbacksAsync() {
    acl_manager_->RegisterCallbacks(&mock_connection_callbacks_, handler_);
  }

  void RegisterLeCallbacksAsync() {
    acl_manager_->RegisterLeCallbacks(&mock_le_connection_callbacks_, handler_);
  }

  void UnregisterClassicCallbacksSync() {
    std::promise<void> promise = std::promise<void>();
    auto future = promise.get_future();
    acl_manager_->UnregisterCallbacks(&mock_connection_callbacks_, std::move(promise));
    ASSERT_EQ(std::future_status::ready, future.wait_for(kCompletionHandlerTimeout));
  }

  void UnregisterLeCallbacksSync() {
    std::promise<void> promise = std::promise<void>();
    auto future = promise.get_future();
    acl_manager_->UnregisterLeCallbacks(&mock_le_connection_callbacks_, std::move(promise));
    ASSERT_EQ(std::future_status::ready, future.wait_for(kCompletionHandlerTimeout));
  }
};

class AclManagerStaticPublicPolicyTest : public AclManagerBaseTest {
 protected:
  void SetUp() override {
    AclManagerBaseTest::SetUp();
    acl_manager_->SetPrivacyPolicyForInitiatorAddress(
        LeAddressManager::AddressPolicy::USE_STATIC_ADDRESS,
        kLocalRandomAddressWithType,
        kMinimumRotationTime,
        kMaximumRotationTime);

    auto view = LastCommand();
    ASSERT_TRUE(view.IsValid());
    ASSERT_EQ(OpCode::LE_SET_RANDOM_ADDRESS, view.GetOpCode());

    ReturnIncomingEvent(LeSetRandomAddressCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS));

    auto set_random_address_packet =
        LeSetRandomAddressView::Create(LeAdvertisingCommandView::Create(view));
    ASSERT_TRUE(set_random_address_packet.IsValid());
    local_initiating_address_with_type_ = AddressWithType(
        set_random_address_packet.GetRandomAddress(), AddressType::RANDOM_DEVICE_ADDRESS);
    // Verify LE Set Random Address was sent during setup
  }

  void TearDown() override {
    AclManagerBaseTest::TearDown();
  }
};

class AclManagerRandomResolvablePublicPolicyTest : public AclManagerBaseTest {
 protected:
  void SetUp() override {
    AclManagerBaseTest::SetUp();
    RegisterClassicCallbacksAsync();
    RegisterLeCallbacksAsync();
    acl_manager_->SetPrivacyPolicyForInitiatorAddress(
        LeAddressManager::AddressPolicy::USE_RESOLVABLE_ADDRESS,
        kLocalRandomAddressWithType,
        kMinimumRotationTime,
        kMaximumRotationTime);

    ASSERT_EQ(OpCode::LE_SET_RANDOM_ADDRESS, LastCommand().GetOpCode());
    ReturnIncomingEvent(LeSetRandomAddressCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS));
  }

  void TearDown() override {
    AclManagerBaseTest::TearDown();
  }
};

class AclManagerUsingCallbacksTest : public AclManagerStaticPublicPolicyTest {
 protected:
  void SetUp() override {
    AclManagerStaticPublicPolicyTest::SetUp();
    RegisterClassicCallbacksAsync();
    RegisterLeCallbacksAsync();
  }

  void TearDown() override {
    // sync_client_handler();

    UnregisterClassicCallbacksSync();
    UnregisterLeCallbacksSync();
    AclManagerStaticPublicPolicyTest::TearDown();
  }
};

class AclManagerWithConnectionTest : public AclManagerUsingCallbacksTest {
 protected:
  void SetUp() override {
    AclManagerUsingCallbacksTest::SetUp();
    remote_device_ =
        std::make_unique<RemoteDevice>(handler_, test_hci_layer_->GetAclQueueDownEnd());
  }

  void TearDown() override {
    remote_device_.reset();
    AclManagerUsingCallbacksTest::TearDown();
  }

  RemoteDevice& GetRemoteDevice() const {
    return *remote_device_;
  }

  virtual packet::PacketView<kLittleEndian> WaitForReceiveIncomingPacket() = 0;

  uint16_t hci_handle_ = kHciHandle;

  std::unique_ptr<RemoteDevice> remote_device_;
};

class AclManagerWithClassicConnectionTest : public AclManagerWithConnectionTest {
 protected:
  std::shared_ptr<ClassicAclConnection> connection_;

  void SetUp() override {
    AclManagerWithConnectionTest::SetUp();
    EXPECT_CALL(
        mock_connection_management_callbacks_, OnRoleChange(hci::ErrorCode::SUCCESS, Role::CENTRAL))
        .Times(1);

    acl_manager_->CreateConnection(kRemoteAddress);
    ASSERT_EQ(OpCode::CREATE_CONNECTION, LastCommand().GetOpCode());

    std::promise<std::unique_ptr<ClassicAclConnection>> promise;
    auto future = promise.get_future();
    EXPECT_CALL(mock_connection_callbacks_, OnConnectSuccess(testing::_))
        .Times(1)
        .WillOnce([&promise](std::unique_ptr<ClassicAclConnection> connection) {
          promise.set_value(std::move(connection));
        });

    ReturnIncomingEvent(ConnectionCompleteBuilder::Create(
        ErrorCode::SUCCESS, hci_handle_, kRemoteAddress, LinkType::ACL, Enable::DISABLED));
    ASSERT_EQ(std::future_status::ready, future.wait_for(kReturnEventTimeout));
    connection_ = std::move(future.get());
    ASSERT_NE(nullptr, connection_);
    connection_->RegisterCallbacks(&mock_connection_management_callbacks_, handler_);
  }

  void TearDown() override {
    sync_client_handler();

    remote_device_.reset();
    connection_.reset();

    AclManagerWithConnectionTest::TearDown();
  }

  packet::PacketView<kLittleEndian> WaitForReceiveIncomingPacket() {
    LOG_INFO("Waiting for connection to receive packet");
    auto queue_end = connection_->GetAclQueueEnd();
    std::unique_ptr<packet::PacketView<kLittleEndian>> received;
    do {
      received = queue_end->TryDequeue();
    } while (received == nullptr);

    return *received;
  }
};

class AclManagerWithLeConnectionTest : public AclManagerWithConnectionTest {
 protected:
  std::shared_ptr<LeAclConnection> connection_;

  void SetUp() override {
    AclManagerWithConnectionTest::SetUp();

    std::promise<std::unique_ptr<LeAclConnection>> promise;
    auto future = promise.get_future();
    EXPECT_CALL(
        mock_le_connection_callbacks_, OnLeConnectSuccess(kRemoteWithPublicType, testing::_))
        .Times(1)
        .WillOnce([&promise](
                      hci::AddressWithType address_with_type,
                      std::unique_ptr<LeAclConnection> connection) {
          promise.set_value(std::move(connection));
        });

    acl_manager_->CreateLeConnection(kRemoteWithPublicType, kIsDirect);
    ASSERT_EQ(OpCode::LE_ADD_DEVICE_TO_FILTER_ACCEPT_LIST, LastCommand().GetOpCode());
    ReturnIncomingEvent(
        LeAddDeviceToFilterAcceptListCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS));

    auto packet = LastCommand();
    ASSERT_EQ(OpCode::LE_CREATE_CONNECTION, packet.GetOpCode());
    auto command_view = LeConnectionManagementCommand<LeCreateConnectionView>(packet);
    ASSERT_TRUE(command_view.IsValid());
    if (use_connect_list_) {
      ASSERT_EQ(command_view.GetPeerAddress(), kEmptyAddressWithType.GetAddress());
      ASSERT_EQ(command_view.GetPeerAddressType(), kEmptyAddressWithType.GetAddressType());
    } else {
      ASSERT_EQ(command_view.GetPeerAddress(), kRemoteAddress);
      ASSERT_EQ(command_view.GetPeerAddressType(), AddressType::PUBLIC_DEVICE_ADDRESS);
    }
    ReturnIncomingEvent(LeCreateConnectionStatusBuilder::Create(ErrorCode::SUCCESS, kNumPackets));

    test_hci_layer_->IncomingLeMetaEvent(LeConnectionCompleteBuilder::Create(
        ErrorCode::SUCCESS,
        kHciHandle,
        Role::CENTRAL,
        AddressType::PUBLIC_DEVICE_ADDRESS,
        kRemoteAddress,
        kConnectionInterval,
        kConnectionLatency,
        kSupervisionTimeout,
        ClockAccuracy::PPM_30));

    ASSERT_EQ(std::future_status::ready, future.wait_for(kReturnEventTimeout));
    connection_ = std::move(future.get());
    ASSERT_NE(nullptr, connection_);
    ASSERT_EQ(local_initiating_address_with_type_, connection_->GetLocalAddress());
    ASSERT_EQ(kRemoteWithPublicType, connection_->GetRemoteAddress());
    ASSERT_EQ(kHciHandle, connection_->GetHandle());

    ASSERT_EQ(OpCode::LE_REMOVE_DEVICE_FROM_FILTER_ACCEPT_LIST, LastCommand().GetOpCode());
    ReturnIncomingEvent(
        LeRemoveDeviceFromFilterAcceptListCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS));

    connection_->RegisterCallbacks(&mock_le_connection_management_callbacks_, handler_);
  }

  void TearDown() override {
    sync_client_handler();

    remote_device_.reset();
    connection_.reset();

    AclManagerWithConnectionTest::TearDown();
  }

  packet::PacketView<kLittleEndian> WaitForReceiveIncomingPacket() {
    auto queue_end = connection_->GetAclQueueEnd();
    std::unique_ptr<packet::PacketView<kLittleEndian>> received;
    do {
      received = queue_end->TryDequeue();
    } while (received == nullptr);

    return *received;
  }
};

class AclManagerWithResolvableAddressTest : public AclManagerBaseTest {
 protected:
  void SetUp() override {
    AclManagerBaseTest::SetUp();
    acl_manager_->RegisterCallbacks(&mock_connection_callbacks_, handler_);
    acl_manager_->RegisterLeCallbacks(&mock_le_connection_callbacks_, handler_);

    acl_manager_->SetPrivacyPolicyForInitiatorAddress(
        LeAddressManager::AddressPolicy::USE_RESOLVABLE_ADDRESS,
        kLocalRandomAddressWithType,
        kMinimumRotationTime,
        kMaximumRotationTime);

    ASSERT_EQ(OpCode::LE_SET_RANDOM_ADDRESS, LastCommand().GetOpCode());
    ReturnIncomingEvent(LeSetRandomAddressCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS));
  }

  void TearDown() override {
    AclManagerBaseTest::TearDown();
  }
};

TEST_F(AclManagerStaticPublicPolicyTest, unregister_classic_after_create_connection) {
  RegisterClassicCallbacksAsync();
  RegisterLeCallbacksAsync();

  // Inject create connection
  acl_manager_->CreateConnection(kRemoteAddress);
  ASSERT_EQ(OpCode::CREATE_CONNECTION, LastCommand().GetOpCode());

  // Unregister callbacks after sending connection request
  auto promise = std::promise<void>();
  auto future = promise.get_future();
  acl_manager_->UnregisterCallbacks(&mock_connection_callbacks_, std::move(promise));
  future.get();

  // Inject peer sending connection complete
  ReturnIncomingEvent(ConnectionCompleteBuilder::Create(
      ErrorCode::SUCCESS, kHciHandle, kRemoteAddress, LinkType::ACL, Enable::DISABLED));

  // Mock is not invoked and the stack silently absorbs event without callback
  UnregisterLeCallbacksSync();
}

TEST_F(AclManagerStaticPublicPolicyTest, unregister_le_before_connection_complete) {
  RegisterClassicCallbacksAsync();
  RegisterLeCallbacksAsync();

  acl_manager_->CreateLeConnection(kRemoteWithPublicType, kIsDirect);
  ASSERT_EQ(OpCode::LE_ADD_DEVICE_TO_FILTER_ACCEPT_LIST, LastCommand().GetOpCode());
  ReturnIncomingEvent(
      LeAddDeviceToFilterAcceptListCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS));

  auto packet = LastCommand();
  ASSERT_EQ(OpCode::LE_CREATE_CONNECTION, packet.GetOpCode());
  auto command_view = LeConnectionManagementCommand<LeCreateConnectionView>(packet);
  ASSERT_TRUE(command_view.IsValid());
  if (use_connect_list_) {
    ASSERT_EQ(command_view.GetPeerAddress(), hci::Address::kEmpty);
  } else {
    ASSERT_EQ(command_view.GetPeerAddress(), kRemoteAddress);
  }
  ASSERT_EQ(command_view.GetPeerAddressType(), AddressType::PUBLIC_DEVICE_ADDRESS);

  // Unregister callbacks after sending connection request
  UnregisterLeCallbacksSync();

  test_hci_layer_->IncomingLeMetaEvent(LeConnectionCompleteBuilder::Create(
      ErrorCode::SUCCESS,
      kHciHandle,
      Role::PERIPHERAL,
      AddressType::PUBLIC_DEVICE_ADDRESS,
      kRemoteAddress,
      kConnectionInterval,
      kConnectionLatency,
      kSupervisionTimeout,
      ClockAccuracy::PPM_30));

  UnregisterClassicCallbacksSync();
}

TEST_F(AclManagerStaticPublicPolicyTest, unregister_le_before_enhanced_connection_complete) {
  RegisterClassicCallbacksAsync();
  RegisterLeCallbacksAsync();

  acl_manager_->CreateLeConnection(kRemoteWithPublicType, kIsDirect);
  ASSERT_EQ(OpCode::LE_ADD_DEVICE_TO_FILTER_ACCEPT_LIST, LastCommand().GetOpCode());
  ReturnIncomingEvent(
      LeAddDeviceToFilterAcceptListCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS));

  auto packet = LastCommand();
  ASSERT_EQ(OpCode::LE_CREATE_CONNECTION, packet.GetOpCode());
  auto command_view = LeConnectionManagementCommand<LeCreateConnectionView>(packet);
  ASSERT_TRUE(command_view.IsValid());
  if (use_connect_list_) {
    ASSERT_EQ(command_view.GetPeerAddress(), hci::Address::kEmpty);
  } else {
    ASSERT_EQ(command_view.GetPeerAddress(), kRemoteAddress);
  }
  ASSERT_EQ(command_view.GetPeerAddressType(), AddressType::PUBLIC_DEVICE_ADDRESS);

  // Unregister callbacks after sending connection request
  UnregisterLeCallbacksSync();

  test_hci_layer_->IncomingLeMetaEvent(LeEnhancedConnectionCompleteBuilder::Create(
      ErrorCode::SUCCESS,
      kHciHandle,
      Role::PERIPHERAL,
      AddressType::PUBLIC_DEVICE_ADDRESS,
      kRemoteAddress,
      Address::kEmpty,
      Address::kEmpty,
      kConnectionInterval,
      kConnectionLatency,
      kSupervisionTimeout,
      ClockAccuracy::PPM_30));

  UnregisterClassicCallbacksSync();
}

TEST_F(AclManagerUsingCallbacksTest, startup_teardown) {}

TEST_F(AclManagerUsingCallbacksTest, local_create_connection_success) {
  acl_manager_->CreateConnection(kRemoteAddress);
  ASSERT_EQ(OpCode::CREATE_CONNECTION, LastCommand().GetOpCode());

  std::promise<std::unique_ptr<ClassicAclConnection>> promise;
  auto future = promise.get_future();
  EXPECT_CALL(mock_connection_callbacks_, OnConnectSuccess(testing::_))
      .Times(1)
      .WillOnce([&promise](std::unique_ptr<ClassicAclConnection> connection) {
        promise.set_value(std::move(connection));
      });

  ReturnIncomingEvent(ConnectionCompleteBuilder::Create(
      ErrorCode::SUCCESS, kHciHandle, kRemoteAddress, LinkType::ACL, Enable::DISABLED));
  ASSERT_EQ(std::future_status::ready, future.wait_for(kReturnEventTimeout));
  std::unique_ptr<ClassicAclConnection> connection = std::move(future.get());
  ASSERT_NE(nullptr, connection);
  ASSERT_EQ(kHciHandle, connection->GetHandle());
  ASSERT_EQ(kRemoteAddress, connection->GetAddress());
  ASSERT_TRUE(connection->locally_initiated_);
}

TEST_F(AclManagerUsingCallbacksTest, local_create_connection_fail) {
  acl_manager_->CreateConnection(kRemoteAddress);
  ASSERT_EQ(OpCode::CREATE_CONNECTION, LastCommand().GetOpCode());

  auto promise = std::promise<std::tuple<hci::Address, hci::ErrorCode, bool>>();
  auto future = promise.get_future();
  EXPECT_CALL(
      mock_connection_callbacks_,
      OnConnectFail(kRemoteAddress, hci::ErrorCode::PAGE_TIMEOUT, kIsLocallyInitiated))
      .Times(1)
      .WillOnce(testing::Invoke(
          [&promise](hci::Address address, hci::ErrorCode error_code, bool is_locally_initiated) {
            promise.set_value(std::make_tuple(address, error_code, is_locally_initiated));
          }));

  ReturnIncomingEvent(ConnectionCompleteBuilder::Create(
      ErrorCode::PAGE_TIMEOUT, kHciHandle, kRemoteAddress, LinkType::ACL, Enable::DISABLED));
  ASSERT_EQ(std::future_status::ready, future.wait_for(kReturnEventTimeout));
  hci::Address address;
  hci::ErrorCode error_code;
  bool is_locally_initiated;
  std::tie(address, error_code, is_locally_initiated) = future.get();
  ASSERT_EQ(kRemoteAddress, address);
  ASSERT_EQ(ErrorCode::PAGE_TIMEOUT, error_code);
  ASSERT_EQ(true, is_locally_initiated);
}

// TODO: implement version of this test where controller supports Extended Advertising Feature in
// GetLeLocalSupportedFeatures, and LE Extended Create Connection is used
TEST_F(AclManagerWithLeConnectionTest, create_le_connection_success) {
  ASSERT_EQ(local_initiating_address_with_type_, connection_->GetLocalAddress());
  ASSERT_EQ(kRemoteWithPublicType, connection_->GetRemoteAddress());
}

TEST_F(AclManagerUsingCallbacksTest, create_le_connection_fail) {
  acl_manager_->CreateLeConnection(kRemoteWithPublicType, kIsDirect);
  ASSERT_EQ(OpCode::LE_ADD_DEVICE_TO_FILTER_ACCEPT_LIST, LastCommand().GetOpCode());

  ReturnIncomingEvent(
      LeAddDeviceToFilterAcceptListCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS));
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::LE_CREATE_CONNECTION, packet.GetOpCode());

  auto create_command_view = LeConnectionManagementCommand<LeCreateConnectionView>(packet);

  ASSERT_TRUE(create_command_view.IsValid());
  if (use_connect_list_) {
    ASSERT_EQ(create_command_view.GetPeerAddress(), hci::Address::kEmpty);
  } else {
    ASSERT_EQ(create_command_view.GetPeerAddress(), kRemoteAddress);
  }
  EXPECT_EQ(create_command_view.GetPeerAddressType(), AddressType::PUBLIC_DEVICE_ADDRESS);

  ReturnIncomingEvent(LeCreateConnectionStatusBuilder::Create(ErrorCode::SUCCESS, kNumPackets));

  EXPECT_CALL(
      mock_le_connection_callbacks_,
      OnLeConnectFail(
          kRemoteWithPublicType,
          ErrorCode::CONNECTION_REJECTED_LIMITED_RESOURCES,
          kIsLocallyInitiated))
      .Times(1);

  test_hci_layer_->IncomingLeMetaEvent(LeConnectionCompleteBuilder::Create(
      ErrorCode::CONNECTION_REJECTED_LIMITED_RESOURCES,
      kHciHandle,
      Role::CENTRAL,
      AddressType::PUBLIC_DEVICE_ADDRESS,
      kRemoteAddress,
      kConnectionInterval,
      kConnectionLatency,
      kSupervisionTimeout,
      ClockAccuracy::PPM_30));

  packet = LastCommand();
  ASSERT_EQ(OpCode::LE_REMOVE_DEVICE_FROM_FILTER_ACCEPT_LIST, packet.GetOpCode());

  ASSERT_TRUE(
      LeConnectionManagementCommand<LeRemoveDeviceFromFilterAcceptListView>(packet).IsValid());

  ReturnIncomingEvent(
      LeRemoveDeviceFromFilterAcceptListCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS));
}

TEST_F(AclManagerUsingCallbacksTest, cancel_le_connection) {
  acl_manager_->CreateLeConnection(kRemoteWithPublicType, kIsDirect);
  ASSERT_EQ(OpCode::LE_ADD_DEVICE_TO_FILTER_ACCEPT_LIST, LastCommand().GetOpCode());
  ReturnIncomingEvent(
      LeAddDeviceToFilterAcceptListCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS));
  ASSERT_EQ(OpCode::LE_CREATE_CONNECTION, LastCommand().GetOpCode());
  ReturnIncomingEvent(LeCreateConnectionStatusBuilder::Create(ErrorCode::SUCCESS, kNumPackets));

  acl_manager_->CancelLeConnect(kRemoteWithPublicType);
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::LE_CREATE_CONNECTION_CANCEL, packet.GetOpCode());
  auto le_connection_management_command_view =
      LeConnectionManagementCommandView::Create(AclCommandView::Create(packet));
  auto command_view = LeCreateConnectionCancelView::Create(le_connection_management_command_view);
  ASSERT_TRUE(command_view.IsValid());

  ReturnIncomingEvent(
      LeCreateConnectionCancelCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS));
  test_hci_layer_->IncomingLeMetaEvent(LeConnectionCompleteBuilder::Create(
      ErrorCode::UNKNOWN_CONNECTION,
      kHciHandle,
      Role::CENTRAL,
      AddressType::PUBLIC_DEVICE_ADDRESS,
      kRemoteAddress,
      kConnectionInterval,
      kConnectionLatency,
      kSupervisionTimeout,
      ClockAccuracy::PPM_30));

  packet = LastCommand();
  ASSERT_EQ(OpCode::LE_REMOVE_DEVICE_FROM_FILTER_ACCEPT_LIST, packet.GetOpCode());
  le_connection_management_command_view = LeConnectionManagementCommandView::Create(AclCommandView::Create(packet));
  auto remove_command_view = LeRemoveDeviceFromFilterAcceptListView::Create(le_connection_management_command_view);
  ASSERT_TRUE(remove_command_view.IsValid());

  ReturnIncomingEvent(
      LeRemoveDeviceFromFilterAcceptListCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS));
}

TEST_F(AclManagerUsingCallbacksTest, create_connection_with_fast_mode) {
  constexpr uint16_t kScanIntervalFast = 0x0060;
  constexpr uint16_t kScanWindowFast = 0x0030;

  std::promise<std::unique_ptr<LeAclConnection>> promise;
  auto future = promise.get_future();
  EXPECT_CALL(mock_le_connection_callbacks_, OnLeConnectSuccess(kRemoteWithPublicType, testing::_))
      .Times(1)
      .WillOnce(
          [&promise](
              hci::AddressWithType address_with_type, std::unique_ptr<LeAclConnection> connection) {
            promise.set_value(std::move(connection));
          });

  acl_manager_->CreateLeConnection(kRemoteWithPublicType, kIsDirect);
  ASSERT_EQ(OpCode::LE_ADD_DEVICE_TO_FILTER_ACCEPT_LIST, LastCommand().GetOpCode());
  ReturnIncomingEvent(
      LeAddDeviceToFilterAcceptListCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS));

  auto packet = LastCommand();
  ASSERT_EQ(OpCode::LE_CREATE_CONNECTION, packet.GetOpCode());

  auto command_view = LeConnectionManagementCommand<LeCreateConnectionView>(packet);
  ASSERT_TRUE(command_view.IsValid());
  ASSERT_EQ(command_view.GetLeScanInterval(), kScanIntervalFast);
  ASSERT_EQ(command_view.GetLeScanWindow(), kScanWindowFast);
  ReturnIncomingEvent(LeCreateConnectionStatusBuilder::Create(ErrorCode::SUCCESS, kNumPackets));

  test_hci_layer_->IncomingLeMetaEvent(LeConnectionCompleteBuilder::Create(
      ErrorCode::SUCCESS,
      0x00,  // hci_handle
      Role::CENTRAL,
      AddressType::PUBLIC_DEVICE_ADDRESS,
      kRemoteAddress,
      kConnectionInterval,
      kConnectionLatency,
      kSupervisionTimeout,
      ClockAccuracy::PPM_30));
  ASSERT_EQ(std::future_status::ready, future.wait_for(kReturnEventTimeout));
  auto connection = std::move(future.get());
  ASSERT_NE(nullptr, connection);

  ASSERT_EQ(OpCode::LE_REMOVE_DEVICE_FROM_FILTER_ACCEPT_LIST, LastCommand().GetOpCode());
  ReturnIncomingEvent(
      LeRemoveDeviceFromFilterAcceptListCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS));
}

TEST_F(AclManagerUsingCallbacksTest, create_connection_with_slow_mode) {
  constexpr uint16_t kScanIntervalSlow = 0x0800;
  constexpr uint16_t kScanWindowSlow = 0x0030;

  std::promise<std::unique_ptr<LeAclConnection>> promise;
  auto future = promise.get_future();
  EXPECT_CALL(mock_le_connection_callbacks_, OnLeConnectSuccess(kRemoteWithPublicType, testing::_))
      .Times(1)
      .WillOnce(
          [&promise](
              hci::AddressWithType address_with_type, std::unique_ptr<LeAclConnection> connection) {
            promise.set_value(std::move(connection));
          });

  acl_manager_->CreateLeConnection(kRemoteWithPublicType, false);
  ASSERT_EQ(OpCode::LE_ADD_DEVICE_TO_FILTER_ACCEPT_LIST, LastCommand().GetOpCode());

  ReturnIncomingEvent(
      LeAddDeviceToFilterAcceptListCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS));
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::LE_CREATE_CONNECTION, packet.GetOpCode());
  auto command_view = LeConnectionManagementCommand<LeCreateConnectionView>(packet);
  ASSERT_TRUE(command_view.IsValid());
  ASSERT_EQ(command_view.GetLeScanInterval(), kScanIntervalSlow);
  ASSERT_EQ(command_view.GetLeScanWindow(), kScanWindowSlow);

  ReturnIncomingEvent(LeCreateConnectionStatusBuilder::Create(ErrorCode::SUCCESS, kNumPackets));
  test_hci_layer_->IncomingLeMetaEvent(LeConnectionCompleteBuilder::Create(
      ErrorCode::SUCCESS,
      0x00,  // hci_handle
      Role::CENTRAL,
      AddressType::PUBLIC_DEVICE_ADDRESS,
      kRemoteAddress,
      kConnectionInterval,
      kConnectionLatency,
      kSupervisionTimeout,
      ClockAccuracy::PPM_30));
  ASSERT_EQ(std::future_status::ready, future.wait_for(kReturnEventTimeout));
  auto connection = std::move(future.get());
  ASSERT_NE(nullptr, connection);

  ASSERT_EQ(OpCode::LE_REMOVE_DEVICE_FROM_FILTER_ACCEPT_LIST, LastCommand().GetOpCode());
  ReturnIncomingEvent(
      LeRemoveDeviceFromFilterAcceptListCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS));
}

TEST_F(AclManagerWithClassicConnectionTest, simple) {}

TEST_F(AclManagerWithClassicConnectionTest, send_disconnection) {
  auto promise = std::promise<hci::ErrorCode>();
  auto future = promise.get_future();
  EXPECT_CALL(
      mock_connection_management_callbacks_,
      OnDisconnection(hci::ErrorCode::REMOTE_USER_TERMINATED_CONNECTION))
      .Times(1)
      .WillOnce([&promise](hci::ErrorCode error_code) { promise.set_value(error_code); });

  test_hci_layer_->Disconnect(kHciHandle, ErrorCode::REMOTE_USER_TERMINATED_CONNECTION);

  ASSERT_EQ(std::future_status::ready, future.wait_for(kReturnEventTimeout));
  ASSERT_EQ(ErrorCode::REMOTE_USER_TERMINATED_CONNECTION, future.get());
}

std::vector<uint8_t> PacketViewToVector(const packet::PacketView<kLittleEndian>& packet) {
  std::vector<uint8_t> vector;
  for (size_t i = 0; i < packet.size(); i++) {
    vector.push_back(packet[i]);
  }
  return vector;
}

TEST_F(AclManagerWithClassicConnectionTest, acl_send_incoming_data_one_classic_connection) {
  static uint32_t packet_number = 1;
  const std::vector<uint8_t> data{0x01, 0x11, 0x21, 0x31, 0x41, 0x51, 0x61, 0x71};
  struct l2cap_payload_t {
    l2cap_header_t hdr;
    struct {
      uint16_t hci_handle;
      uint32_t packet_number;
      uint8_t data[8];  // data.size()
    } __attribute__((packed)) payload;
  } __attribute__((packed));

  // Send first packet initiated from remote
  {
    GetRemoteDevice().EnqueueAndSendAclDataSync(hci_handle_, packet_number, data);

    packet::PacketView<kLittleEndian> l2cap_packet = WaitForReceiveIncomingPacket();
    auto vector = PacketViewToVector(l2cap_packet);

    const l2cap_payload_t* payload = reinterpret_cast<const l2cap_payload_t*>(&(*vector.begin()));

    // Check l2cap header
    ASSERT_EQ(sizeof(uint32_t) + sizeof(uint16_t) + data.size(), payload->hdr.pdu_size);
    ASSERT_EQ(kL2capChannelIdentifier, payload->hdr.cid);

    // Check l2cap payload
    ASSERT_EQ(kHciHandle, payload->payload.hci_handle);
    ASSERT_EQ(packet_number, payload->payload.packet_number);

    // Ensure the data matches
    for (size_t i = 0; i < data.size(); i++) {
      ASSERT_EQ(data[i], payload->payload.data[i]);
    }
  }

  // Send another packet from the remote
  packet_number++;
  {
    GetRemoteDevice().EnqueueAndSendAclDataSync(hci_handle_, packet_number, data);

    packet::PacketView<kLittleEndian> l2cap_packet = WaitForReceiveIncomingPacket();
    auto vector = PacketViewToVector(l2cap_packet);

    const l2cap_payload_t* payload = reinterpret_cast<const l2cap_payload_t*>(&(*vector.begin()));

    // Check l2cap header
    ASSERT_EQ(sizeof(uint32_t) + sizeof(uint16_t) + data.size(), payload->hdr.pdu_size);
    ASSERT_EQ(kL2capChannelIdentifier, payload->hdr.cid);

    // Check l2cap payload
    ASSERT_EQ(kHciHandle, payload->payload.hci_handle);
    ASSERT_EQ(packet_number, payload->payload.packet_number);

    // Ensure the data matches
    for (size_t i = 0; i < data.size(); i++) {
      ASSERT_EQ(data[i], payload->payload.data[i]);
    }
  }

  sync_client_handler();
}

TEST_F(AclManagerWithClassicConnectionTest, acl_send_data_credits) {
  // Use all the credits
  for (uint16_t credits = 0; credits < test_controller_->total_acl_buffers_; credits++) {
    // Send a packet across the connection from the remote device
    GetRemoteDevice().EnqueueAclData(hci_handle_);
    GetRemoteDevice().SendAclDataSync();
  }

  // Send another packet across the connection from the remote device
  // and ensure it is not accepted due to credit exhaustion
  GetRemoteDevice().EnqueueAclData(hci_handle_);
  ASSERT_TRUE(GetRemoteDevice().IsNoOutgoingAclData());

  test_controller_->CompletePackets(hci_handle_, test_controller_->total_acl_buffers_);

  // Ensure data transfer is restored after credits restored
  GetRemoteDevice().EnqueueAclData(hci_handle_);
  GetRemoteDevice().SendAclDataSync();

  sync_client_handler();
  // Command status/complete either success or failure consumed by hci layer
}

TEST_F(AclManagerWithClassicConnectionTest, send_write_default_link_policy_settings) {
  constexpr uint16_t kLinkPolicySettings = 0x05;

  acl_manager_->WriteDefaultLinkPolicySettings(kLinkPolicySettings);
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::WRITE_DEFAULT_LINK_POLICY_SETTINGS, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<WriteDefaultLinkPolicySettingsView>(packet);
  ASSERT_TRUE(command_view.IsValid());
  ASSERT_EQ(kLinkPolicySettings, command_view.GetDefaultLinkPolicySettings());

  ReturnIncomingEvent(
      WriteDefaultLinkPolicySettingsCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS));

  ASSERT_EQ(kLinkPolicySettings, acl_manager_->ReadDefaultLinkPolicySettings());

  // Command status/complete either success or failure consumed by hci layer
}

TEST_F(AclManagerWithClassicConnectionTest, send_authentication_requested) {
  connection_->AuthenticationRequested();
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::AUTHENTICATION_REQUESTED, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<AuthenticationRequestedView>(packet);
  ASSERT_TRUE(command_view.IsValid());

  auto promise = std::promise<hci::ErrorCode>();
  auto future = promise.get_future();
  EXPECT_CALL(mock_connection_management_callbacks_, OnAuthenticationComplete)
      .Times(1)
      .WillOnce(testing::Invoke(
          [&promise](hci::ErrorCode hci_status) { promise.set_value(hci_status); }));

  ReturnIncomingEvent(AuthenticationCompleteBuilder::Create(ErrorCode::SUCCESS, hci_handle_));

  hci::ErrorCode error_code = future.get();
  ASSERT_EQ(ErrorCode::SUCCESS, error_code);
}

TEST_F(AclManagerWithClassicConnectionTest, send_read_clock_offset) {
  constexpr uint16_t kClockOffset = 0x0123;

  connection_->ReadClockOffset();
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::READ_CLOCK_OFFSET, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<ReadClockOffsetView>(packet);
  ASSERT_TRUE(command_view.IsValid());

  auto promise = std::promise<uint16_t>();
  auto future = promise.get_future();
  EXPECT_CALL(mock_connection_management_callbacks_, OnReadClockOffsetComplete(kClockOffset))
      .Times(1)
      .WillOnce(
          testing::Invoke([&promise](uint16_t clock_offset) { promise.set_value(clock_offset); }));

  ReturnIncomingEvent(
      ReadClockOffsetCompleteBuilder::Create(ErrorCode::SUCCESS, hci_handle_, kClockOffset));

  ASSERT_EQ(std::future_status::ready, future.wait_for(kReturnEventTimeout));
  ASSERT_EQ(kClockOffset, future.get());
}

TEST_F(AclManagerWithClassicConnectionTest, send_hold_mode) {
  connection_->HoldMode(kHoldModeMaxInterval, kHoldModeMinInterval);
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::HOLD_MODE, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<HoldModeView>(packet);
  ASSERT_TRUE(command_view.IsValid());
  ASSERT_EQ(command_view.GetHoldModeMaxInterval(), kHoldModeMaxInterval);
  ASSERT_EQ(command_view.GetHoldModeMinInterval(), kHoldModeMinInterval);

  auto promise = std::promise<std::tuple<hci::ErrorCode, hci::Mode, uint16_t>>();
  auto future = promise.get_future();
  EXPECT_CALL(
      mock_connection_management_callbacks_,
      OnModeChange(ErrorCode::SUCCESS, Mode::HOLD, kHoldModeMinInterval))
      .Times(1)
      .WillOnce(testing::Invoke(
          [&promise](hci::ErrorCode status, hci::Mode current_mode, uint16_t interval) {
            promise.set_value(std::make_tuple(status, current_mode, interval));
          }));

  ReturnIncomingEvent(
      ModeChangeBuilder::Create(ErrorCode::SUCCESS, hci_handle_, Mode::HOLD, kHoldModeMinInterval));

  hci::ErrorCode error_code;
  hci::Mode mode;
  uint16_t interval;
  std::tie(error_code, mode, interval) = future.get();
  ASSERT_EQ(ErrorCode::SUCCESS, error_code);
  ASSERT_EQ(Mode::HOLD, mode);
}

TEST_F(AclManagerWithClassicConnectionTest, send_sniff_mode) {
  constexpr uint16_t kSniffMaxInterval = 0x0500;
  constexpr uint16_t kSniffMinInterval = 0x0020;
  constexpr uint16_t kSniffAttempt = 0x0040;
  constexpr uint16_t kSniffTimeout = 0x0014;
  constexpr uint16_t kInterval = 0x0028;

  connection_->SniffMode(kSniffMaxInterval, kSniffMinInterval, kSniffAttempt, kSniffTimeout);
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::SNIFF_MODE, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<SniffModeView>(packet);
  ASSERT_TRUE(command_view.IsValid());
  ASSERT_EQ(command_view.GetSniffMaxInterval(), kSniffMaxInterval);
  ASSERT_EQ(command_view.GetSniffMinInterval(), kSniffMinInterval);
  ASSERT_EQ(command_view.GetSniffAttempt(), kSniffAttempt);
  ASSERT_EQ(command_view.GetSniffTimeout(), kSniffTimeout);

  auto promise = std::promise<std::tuple<hci::ErrorCode, hci::Mode, uint16_t>>();
  auto future = promise.get_future();
  EXPECT_CALL(
      mock_connection_management_callbacks_,
      OnModeChange(ErrorCode::SUCCESS, Mode::SNIFF, kInterval))
      .Times(1)
      .WillOnce(testing::Invoke(
          [&promise](hci::ErrorCode status, hci::Mode current_mode, uint16_t interval) {
            promise.set_value(std::make_tuple(status, current_mode, interval));
          }));

  ReturnIncomingEvent(
      ModeChangeBuilder::Create(ErrorCode::SUCCESS, hci_handle_, Mode::SNIFF, kInterval));

  hci::ErrorCode status;
  hci::Mode current_mode;
  uint16_t interval;
  std::tie(status, current_mode, interval) = future.get();
  ASSERT_EQ(ErrorCode::SUCCESS, status);
  ASSERT_EQ(Mode::SNIFF, current_mode);
  ASSERT_EQ(kInterval, interval);
}

TEST_F(AclManagerWithClassicConnectionTest, send_exit_sniff_mode) {
  constexpr uint16_t kInterval = 0xaaaa;

  auto promise = std::promise<std::tuple<hci::ErrorCode, hci::Mode, uint16_t>>();
  auto future = promise.get_future();
  EXPECT_CALL(
      mock_connection_management_callbacks_,
      OnModeChange(ErrorCode::SUCCESS, Mode::ACTIVE, kInterval))
      .Times(1)
      .WillOnce(testing::Invoke(
          [&promise](hci::ErrorCode status, hci::Mode current_mode, uint16_t interval) {
            promise.set_value(std::make_tuple(status, current_mode, interval));
          }));

  connection_->ExitSniffMode();
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::EXIT_SNIFF_MODE, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<ExitSniffModeView>(packet);
  ASSERT_TRUE(command_view.IsValid());

  ReturnIncomingEvent(
      ModeChangeBuilder::Create(ErrorCode::SUCCESS, hci_handle_, Mode::ACTIVE, kInterval));

  hci::ErrorCode status;
  hci::Mode current_mode;
  uint16_t interval;
  std::tie(status, current_mode, interval) = future.get();
  ASSERT_EQ(ErrorCode::SUCCESS, status);
  ASSERT_EQ(Mode::ACTIVE, current_mode);
  ASSERT_EQ(kInterval, interval);
}

TEST_F(AclManagerWithClassicConnectionTest, send_qos_setup) {
  constexpr uint16_t kTokenRate = 0x1234;
  constexpr uint16_t kPeakBandwidth = 0x1233;
  constexpr uint16_t kLatency = 0x1232;
  constexpr uint16_t kDelayVariation = 0x1231;

  connection_->QosSetup(
      ServiceType::BEST_EFFORT, kTokenRate, kPeakBandwidth, kLatency, kDelayVariation);
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::QOS_SETUP, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<QosSetupView>(packet);
  ASSERT_TRUE(command_view.IsValid());
  ASSERT_EQ(command_view.GetServiceType(), ServiceType::BEST_EFFORT);
  ASSERT_EQ(kTokenRate, command_view.GetTokenRate());
  ASSERT_EQ(kPeakBandwidth, command_view.GetPeakBandwidth());
  ASSERT_EQ(kLatency, command_view.GetLatency());
  ASSERT_EQ(kDelayVariation, command_view.GetDelayVariation());

  EXPECT_CALL(
      mock_connection_management_callbacks_,
      OnQosSetupComplete(
          ServiceType::BEST_EFFORT, kTokenRate, kPeakBandwidth, kLatency, kDelayVariation))
      .Times(1);
  ReturnIncomingEvent(QosSetupCompleteBuilder::Create(
      ErrorCode::SUCCESS,
      hci_handle_,
      ServiceType::BEST_EFFORT,
      kTokenRate,
      kPeakBandwidth,
      kLatency,
      kDelayVariation));

  sync_client_handler();
}

TEST_F(AclManagerUsingCallbacksTest, send_switch_role) {
  auto switch_role_promise = std::promise<void>();
  EXPECT_CALL(
      mock_connection_management_callbacks_, OnRoleChange(hci::ErrorCode::SUCCESS, Role::CENTRAL))
      .Times(1);
  EXPECT_CALL(
      mock_connection_management_callbacks_,
      OnRoleChange(hci::ErrorCode::SUCCESS, Role::PERIPHERAL))
      .Times(1)
      .WillOnce(testing::Invoke([&switch_role_promise]() { switch_role_promise.set_value(); }));
  std::promise<std::unique_ptr<ClassicAclConnection>> promise;
  auto future = promise.get_future();
  EXPECT_CALL(mock_connection_callbacks_, OnConnectSuccess(testing::_))
      .Times(1)
      .WillOnce([&promise](std::unique_ptr<ClassicAclConnection> connection) {
        promise.set_value(std::move(connection));
      });

  acl_manager_->CreateConnection(kRemoteAddress);
  ASSERT_EQ(OpCode::CREATE_CONNECTION, LastCommand().GetOpCode());

  ReturnIncomingEvent(ConnectionCompleteBuilder::Create(
      ErrorCode::SUCCESS, kHciHandle, kRemoteAddress, LinkType::ACL, Enable::DISABLED));
  ASSERT_EQ(std::future_status::ready, future.wait_for(kReturnEventTimeout));

  std::shared_ptr<ClassicAclConnection> connection = std::move(future.get());
  ASSERT_NE(nullptr, connection);
  connection->RegisterCallbacks(&mock_connection_management_callbacks_, handler_);

  acl_manager_->SwitchRole(connection->GetAddress(), Role::PERIPHERAL);
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::SWITCH_ROLE, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<SwitchRoleView>(packet);
  ASSERT_TRUE(command_view.IsValid());
  ASSERT_EQ(connection->GetAddress(), command_view.GetBdAddr());
  ASSERT_EQ(Role::PERIPHERAL, command_view.GetRole());

  ReturnIncomingEvent(
      RoleChangeBuilder::Create(ErrorCode::SUCCESS, connection->GetAddress(), Role::PERIPHERAL));

  EXPECT_EQ(
      std::future_status::ready,
      switch_role_promise.get_future().wait_for(std::chrono::seconds(3)));
  // Command status/complete either success or failure consumed by hci layer
}

TEST_F(AclManagerWithClassicConnectionTest, send_flow_specification) {
  constexpr uint16_t kTokenRate = 0x1234;
  constexpr uint16_t kTokenBucketSize = 0x1233;
  constexpr uint16_t kPeakBandwidth = 0x1232;
  constexpr uint16_t kAccessLatency = 0x1231;

  connection_->FlowSpecification(
      FlowDirection::OUTGOING_FLOW,
      ServiceType::BEST_EFFORT,
      kTokenRate,
      kTokenBucketSize,
      kPeakBandwidth,
      kAccessLatency);
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::FLOW_SPECIFICATION, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<FlowSpecificationView>(packet);
  ASSERT_TRUE(command_view.IsValid());
  ASSERT_EQ(command_view.GetFlowDirection(), FlowDirection::OUTGOING_FLOW);
  ASSERT_EQ(command_view.GetServiceType(), ServiceType::BEST_EFFORT);
  ASSERT_EQ(kTokenRate, command_view.GetTokenRate());
  ASSERT_EQ(kTokenBucketSize, command_view.GetTokenBucketSize());
  ASSERT_EQ(kPeakBandwidth, command_view.GetPeakBandwidth());
  ASSERT_EQ(kAccessLatency, command_view.GetAccessLatency());

  auto promise = std::promise<
      std::tuple<hci::FlowDirection, hci::ServiceType, uint32_t, uint32_t, uint32_t, uint32_t>>();
  auto future = promise.get_future();
  EXPECT_CALL(
      mock_connection_management_callbacks_,
      OnFlowSpecificationComplete(
          FlowDirection::OUTGOING_FLOW,
          ServiceType::BEST_EFFORT,
          kTokenRate,
          kTokenBucketSize,
          kPeakBandwidth,
          kAccessLatency))
      .Times(1)
      .WillOnce(testing::Invoke([&promise](
                                    FlowDirection flow_direction,
                                    ServiceType service_type,
                                    uint32_t token_rate,
                                    uint32_t token_bucket_size,
                                    uint32_t peak_bandwidth,
                                    uint32_t access_latency) {
        promise.set_value(std::make_tuple(
            flow_direction,
            service_type,
            token_rate,
            token_bucket_size,
            peak_bandwidth,
            access_latency));
      }));

  ReturnIncomingEvent(FlowSpecificationCompleteBuilder::Create(
      ErrorCode::SUCCESS,
      hci_handle_,
      FlowDirection::OUTGOING_FLOW,
      ServiceType::BEST_EFFORT,
      kTokenRate,
      kTokenBucketSize,
      kPeakBandwidth,
      kAccessLatency));

  hci::FlowDirection flow_direction;
  hci::ServiceType service_type;
  uint32_t token_rate;
  uint32_t token_bucket_size;
  uint32_t peak_bandwidth;
  uint32_t access_latency;
  std::tie(
      flow_direction, service_type, token_rate, token_bucket_size, peak_bandwidth, access_latency) =
      future.get();
  ASSERT_EQ(FlowDirection::OUTGOING_FLOW, flow_direction);
  ASSERT_EQ(ServiceType::BEST_EFFORT, service_type);
  ASSERT_EQ(kTokenRate, token_rate);
  ASSERT_EQ(kTokenBucketSize, token_bucket_size);
  ASSERT_EQ(kPeakBandwidth, peak_bandwidth);
  ASSERT_EQ(kAccessLatency, access_latency);
}

TEST_F(AclManagerWithClassicConnectionTest, send_flush) {
  connection_->Flush();
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::FLUSH, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<FlushView>(packet);
  ASSERT_TRUE(command_view.IsValid());

  std::promise<void> promise;
  auto future = promise.get_future();
  EXPECT_CALL(mock_connection_management_callbacks_, OnFlushOccurred())
      .Times(1)
      .WillOnce([&promise]() { promise.set_value(); });
  ReturnIncomingEvent(FlushOccurredBuilder::Create(hci_handle_));

  ASSERT_EQ(std::future_status::ready, future.wait_for(kReturnEventTimeout));
}

TEST_F(AclManagerWithClassicConnectionTest, send_role_discovery) {
  connection_->RoleDiscovery();
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::ROLE_DISCOVERY, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<RoleDiscoveryView>(packet);
  ASSERT_TRUE(command_view.IsValid());

  std::promise<hci::Role> promise;
  auto future = promise.get_future();
  EXPECT_CALL(mock_connection_management_callbacks_, OnRoleDiscoveryComplete(Role::CENTRAL))
      .Times(1)
      .WillOnce([&promise](hci::Role role) { promise.set_value(role); });
  ReturnIncomingEvent(RoleDiscoveryCompleteBuilder::Create(
      kNumPackets, ErrorCode::SUCCESS, hci_handle_, Role::CENTRAL));

  ASSERT_EQ(std::future_status::ready, future.wait_for(kReturnEventTimeout));
  ASSERT_EQ(hci::Role::CENTRAL, future.get());
}

TEST_F(AclManagerWithClassicConnectionTest, send_read_link_policy_settings) {
  constexpr uint16_t kLinkPolicySettings = 0x0007;

  connection_->ReadLinkPolicySettings();
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::READ_LINK_POLICY_SETTINGS, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<ReadLinkPolicySettingsView>(packet);
  ASSERT_TRUE(command_view.IsValid());

  std::promise<uint16_t> promise;
  auto future = promise.get_future();
  EXPECT_CALL(
      mock_connection_management_callbacks_, OnReadLinkPolicySettingsComplete(kLinkPolicySettings))
      .Times(1)
      .WillOnce(
          [&promise](uint16_t link_policy_settings) { promise.set_value(link_policy_settings); });

  ReturnIncomingEvent(ReadLinkPolicySettingsCompleteBuilder::Create(
      kNumPackets, ErrorCode::SUCCESS, hci_handle_, kLinkPolicySettings));

  ASSERT_EQ(std::future_status::ready, future.wait_for(kReturnEventTimeout));
  ASSERT_EQ(kLinkPolicySettings, future.get());
}

TEST_F(AclManagerWithClassicConnectionTest, send_write_link_policy_settings) {
  constexpr uint16_t kLinkPolicySettings = 0x0005;

  connection_->WriteLinkPolicySettings(kLinkPolicySettings);
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::WRITE_LINK_POLICY_SETTINGS, packet.GetOpCode());
  auto command_view = WriteLinkPolicySettingsView::Create(
      ConnectionManagementCommandView::Create(AclCommandView::Create(packet)));
  ASSERT_TRUE(command_view.IsValid());
  ASSERT_EQ(command_view.GetLinkPolicySettings(), kLinkPolicySettings);

  ReturnIncomingEvent(
      WriteLinkPolicySettingsCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS, hci_handle_));
  // Command status/complete either success or failure consumed by hci layer
}

TEST_F(AclManagerWithClassicConnectionTest, send_sniff_subrating) {
  constexpr uint16_t kMaximumLatency = 0x1234;
  constexpr uint16_t kMinimumRemoteTimeout = 0x1235;
  constexpr uint16_t kMinimumLocalTimeout = 0x1236;

  connection_->SniffSubrating(kMaximumLatency, kMinimumRemoteTimeout, kMinimumLocalTimeout);
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::SNIFF_SUBRATING, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<SniffSubratingView>(packet);
  ASSERT_TRUE(command_view.IsValid());
  ASSERT_EQ(command_view.GetMaximumLatency(), kMaximumLatency);
  ASSERT_EQ(command_view.GetMinimumRemoteTimeout(), kMinimumRemoteTimeout);
  ASSERT_EQ(command_view.GetMinimumLocalTimeout(), kMinimumLocalTimeout);

  ReturnIncomingEvent(
      SniffSubratingCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS, hci_handle_));
  // Command status/complete either success or failure consumed by hci layer
}

TEST_F(AclManagerWithClassicConnectionTest, send_read_automatic_flush_timeout) {
  constexpr uint16_t kFlushTimeout = 0x07ff;

  connection_->ReadAutomaticFlushTimeout();
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::READ_AUTOMATIC_FLUSH_TIMEOUT, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<ReadAutomaticFlushTimeoutView>(packet);
  ASSERT_TRUE(command_view.IsValid());

  std::promise<uint16_t> promise;
  auto future = promise.get_future();
  EXPECT_CALL(
      mock_connection_management_callbacks_, OnReadAutomaticFlushTimeoutComplete(kFlushTimeout))
      .Times(1)
      .WillOnce([&promise](uint16_t flush_timeout) { promise.set_value(flush_timeout); });
  ReturnIncomingEvent(ReadAutomaticFlushTimeoutCompleteBuilder::Create(
      kNumPackets, ErrorCode::SUCCESS, hci_handle_, kFlushTimeout));

  ASSERT_EQ(std::future_status::ready, future.wait_for(kReturnEventTimeout));
  ASSERT_EQ(kFlushTimeout, future.get());
}

TEST_F(AclManagerWithClassicConnectionTest, send_write_automatic_flush_timeout) {
  constexpr uint16_t flush_timeout = 0x07ff;

  connection_->WriteAutomaticFlushTimeout(flush_timeout);
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::WRITE_AUTOMATIC_FLUSH_TIMEOUT, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<WriteAutomaticFlushTimeoutView>(packet);
  ASSERT_TRUE(command_view.IsValid());
  ASSERT_EQ(command_view.GetFlushTimeout(), flush_timeout);

  ReturnIncomingEvent(WriteAutomaticFlushTimeoutCompleteBuilder::Create(
      kNumPackets, ErrorCode::SUCCESS, hci_handle_));
  // Command status/complete either success or failure consumed by hci layer
}

TEST_F(AclManagerWithClassicConnectionTest, send_read_transmit_power_level) {
  constexpr uint8_t kTransmitPowerLevel = 0x07;
  connection_->ReadTransmitPowerLevel(TransmitPowerLevelType::CURRENT);
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::READ_TRANSMIT_POWER_LEVEL, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<ReadTransmitPowerLevelView>(packet);
  ASSERT_TRUE(command_view.IsValid());
  ASSERT_EQ(command_view.GetTransmitPowerLevelType(), TransmitPowerLevelType::CURRENT);

  std::promise<uint16_t> promise;
  auto future = promise.get_future();
  EXPECT_CALL(
      mock_connection_management_callbacks_, OnReadTransmitPowerLevelComplete(kTransmitPowerLevel))
      .Times(1)
      .WillOnce(
          [&promise](uint16_t transmit_power_level) { promise.set_value(transmit_power_level); });

  ReturnIncomingEvent(ReadTransmitPowerLevelCompleteBuilder::Create(
      kNumPackets, ErrorCode::SUCCESS, hci_handle_, kTransmitPowerLevel));

  ASSERT_EQ(std::future_status::ready, future.wait_for(kReturnEventTimeout));
  ASSERT_EQ(kTransmitPowerLevel, future.get());
}

TEST_F(AclManagerWithClassicConnectionTest, send_read_link_supervision_timeout) {
  constexpr uint16_t kSupervisionTimeout = 0x5677;
  connection_->ReadLinkSupervisionTimeout();
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::READ_LINK_SUPERVISION_TIMEOUT, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<ReadLinkSupervisionTimeoutView>(packet);
  ASSERT_TRUE(command_view.IsValid());

  std::promise<uint16_t> promise;
  auto future = promise.get_future();
  EXPECT_CALL(
      mock_connection_management_callbacks_,
      OnReadLinkSupervisionTimeoutComplete(kSupervisionTimeout))
      .Times(1)
      .WillOnce([&promise](uint16_t link_supervision_timeout) {
        promise.set_value(link_supervision_timeout);
      });
  ReturnIncomingEvent(ReadLinkSupervisionTimeoutCompleteBuilder::Create(
      kNumPackets, ErrorCode::SUCCESS, hci_handle_, kSupervisionTimeout));

  ASSERT_EQ(std::future_status::ready, future.wait_for(kReturnEventTimeout));
  ASSERT_EQ(kSupervisionTimeout, future.get());
}

TEST_F(AclManagerWithClassicConnectionTest, send_write_link_supervision_timeout) {
  constexpr uint16_t kSupervisionTimeout = 0x5678;
  connection_->WriteLinkSupervisionTimeout(kSupervisionTimeout);
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::WRITE_LINK_SUPERVISION_TIMEOUT, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<WriteLinkSupervisionTimeoutView>(packet);
  ASSERT_TRUE(command_view.IsValid());
  ASSERT_EQ(command_view.GetLinkSupervisionTimeout(), kSupervisionTimeout);

  ReturnIncomingEvent(WriteLinkSupervisionTimeoutCompleteBuilder::Create(
      kNumPackets, ErrorCode::SUCCESS, hci_handle_));
  // Command status/complete either success or failure consumed by hci layer
}

TEST_F(AclManagerWithClassicConnectionTest, send_read_failed_contact_counter) {
  constexpr uint16_t kFailedContactCounter = 0x0055;

  connection_->ReadFailedContactCounter();
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::READ_FAILED_CONTACT_COUNTER, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<ReadFailedContactCounterView>(packet);
  ASSERT_TRUE(command_view.IsValid());

  std::promise<uint16_t> promise;
  auto future = promise.get_future();
  EXPECT_CALL(
      mock_connection_management_callbacks_,
      OnReadFailedContactCounterComplete(kFailedContactCounter))
      .Times(1)
      .WillOnce([&promise](uint16_t failed_contact_counter) {
        promise.set_value(failed_contact_counter);
      });

  ReturnIncomingEvent(ReadFailedContactCounterCompleteBuilder::Create(
      kNumPackets, ErrorCode::SUCCESS, hci_handle_, kFailedContactCounter));

  ASSERT_EQ(std::future_status::ready, future.wait_for(kReturnEventTimeout));
  ASSERT_EQ(kFailedContactCounter, future.get());
}

TEST_F(AclManagerWithClassicConnectionTest, send_reset_failed_contact_counter) {
  connection_->ResetFailedContactCounter();
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::RESET_FAILED_CONTACT_COUNTER, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<ResetFailedContactCounterView>(packet);
  ASSERT_TRUE(command_view.IsValid());

  ReturnIncomingEvent(ResetFailedContactCounterCompleteBuilder::Create(
      kNumPackets, ErrorCode::SUCCESS, hci_handle_));
  // Command status/complete either success or failure consumed by hci layer
}

TEST_F(AclManagerWithClassicConnectionTest, send_read_link_quality) {
  const uint8_t kLinkQuality = 0xa9;

  connection_->ReadLinkQuality();
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::READ_LINK_QUALITY, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<ReadLinkQualityView>(packet);
  ASSERT_TRUE(command_view.IsValid());

  std::promise<uint16_t> promise;
  auto future = promise.get_future();
  EXPECT_CALL(mock_connection_management_callbacks_, OnReadLinkQualityComplete(kLinkQuality))
      .Times(1)
      .WillOnce([&promise](uint8_t link_quality) { promise.set_value(link_quality); });
  ReturnIncomingEvent(ReadLinkQualityCompleteBuilder::Create(
      kNumPackets, ErrorCode::SUCCESS, hci_handle_, kLinkQuality));

  ASSERT_EQ(std::future_status::ready, future.wait_for(kReturnEventTimeout));
  ASSERT_EQ(kLinkQuality, future.get());
}

TEST_F(AclManagerWithClassicConnectionTest, send_read_afh_channel_map) {
  const std::array<uint8_t, 10> kAfhChannelMap = {
      0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09};

  connection_->ReadAfhChannelMap();
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::READ_AFH_CHANNEL_MAP, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<ReadAfhChannelMapView>(packet);
  ASSERT_TRUE(command_view.IsValid());

  std::promise<std::tuple<hci::AfhMode, std::array<uint8_t, 10>>> promise;
  auto future = promise.get_future();
  EXPECT_CALL(
      mock_connection_management_callbacks_,
      OnReadAfhChannelMapComplete(AfhMode::AFH_ENABLED, kAfhChannelMap))
      .Times(1)
      .WillOnce([&promise](AfhMode afh_mode, std::array<uint8_t, 10> afh_channel_map) {
        promise.set_value(std::make_tuple(afh_mode, afh_channel_map));
      });
  ReturnIncomingEvent(ReadAfhChannelMapCompleteBuilder::Create(
      kNumPackets, ErrorCode::SUCCESS, hci_handle_, AfhMode::AFH_ENABLED, kAfhChannelMap));

  AfhMode afh_mode;
  std::array<uint8_t, 10> afh_channel_map;
  std::tie(afh_mode, afh_channel_map) = future.get();
  ASSERT_EQ(AfhMode::AFH_ENABLED, afh_mode);
  ASSERT_EQ(0x09, afh_channel_map[9]);
}

TEST_F(AclManagerWithClassicConnectionTest, send_read_rssi) {
  constexpr int8_t kRssiValue = 0xaa;

  connection_->ReadRssi();
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::READ_RSSI, packet.GetOpCode());
  auto command_view = AclCommand<ReadRssiView>(packet);
  ASSERT_TRUE(command_view.IsValid());
  std::promise<int8_t> promise;
  auto future = promise.get_future();
  EXPECT_CALL(mock_connection_management_callbacks_, OnReadRssiComplete(kRssiValue))
      .Times(1)
      .WillOnce([&promise](int8_t rssi_value) { promise.set_value(rssi_value); });
  ReturnIncomingEvent(
      ReadRssiCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS, hci_handle_, kRssiValue));
  ASSERT_EQ(std::future_status::ready, future.wait_for(kReturnEventTimeout));
  ASSERT_EQ(kRssiValue, future.get());
}

TEST_F(AclManagerWithClassicConnectionTest, send_read_clock) {
  constexpr uint32_t kClock = 0x00002e6a;
  constexpr uint16_t kAccuracy = 0x0000;

  connection_->ReadClock(WhichClock::LOCAL);
  auto packet = LastCommand();
  ASSERT_EQ(OpCode::READ_CLOCK, packet.GetOpCode());
  auto command_view = ConnectionManagementCommand<ReadClockView>(packet);
  ASSERT_TRUE(command_view.IsValid());
  ASSERT_EQ(command_view.GetWhichClock(), WhichClock::LOCAL);

  auto promise = std::promise<std::tuple<uint32_t, uint16_t>>();
  auto future = promise.get_future();
  EXPECT_CALL(mock_connection_management_callbacks_, OnReadClockComplete(kClock, kAccuracy))
      .Times(1)
      .WillOnce([&promise](uint32_t clock, uint16_t accuracy) {
        promise.set_value(std::make_tuple(clock, accuracy));
      });
  ReturnIncomingEvent(ReadClockCompleteBuilder::Create(
      kNumPackets, ErrorCode::SUCCESS, hci_handle_, kClock, kAccuracy));

  uint32_t clock;
  uint16_t accuracy;
  std::tie(clock, accuracy) = future.get();
  ASSERT_EQ(kClock, clock);
  ASSERT_EQ(kAccuracy, accuracy);
}

TEST_F(AclManagerWithClassicConnectionTest, remote_sco_connect_request) {
  ClassOfDevice class_of_device = {};

  EXPECT_CALL(mock_connection_callbacks_, HACK_OnScoConnectRequest(kRemoteAddress, class_of_device))
      .Times(1);

  ReturnIncomingEvent(ConnectionRequestBuilder::Create(
      kRemoteAddress, class_of_device, ConnectionRequestLinkType::SCO));
}

TEST_F(AclManagerWithClassicConnectionTest, remote_esco_connect_request) {
  ClassOfDevice class_of_device = {};

  EXPECT_CALL(
      mock_connection_callbacks_, HACK_OnEscoConnectRequest(kRemoteAddress, class_of_device))
      .Times(1);

  ReturnIncomingEvent(ConnectionRequestBuilder::Create(
      kRemoteAddress, class_of_device, ConnectionRequestLinkType::ESCO));
}

TEST_F(AclManagerWithLeConnectionTest, simple) {}

TEST_F(AclManagerWithLeConnectionTest, acl_send_data_one_le_connection) {
  static uint32_t packet_number = 1;
  const std::vector<uint8_t> data{0x01, 0x11, 0x21, 0x31, 0x41, 0x51, 0x61, 0x71};
  struct l2cap_payload_t {
    l2cap_header_t hdr;
    struct {
      uint16_t hci_handle;
      uint32_t packet_number;
      uint8_t data[8];  // data.size()
    } __attribute__((packed)) payload;
  } __attribute__((packed));

  // Send first packet initiated from remote
  {
    GetRemoteDevice().EnqueueAndSendAclDataSync(hci_handle_, packet_number, data);

    packet::PacketView<kLittleEndian> l2cap_packet = WaitForReceiveIncomingPacket();
    auto vector = PacketViewToVector(l2cap_packet);

    const l2cap_payload_t* payload = reinterpret_cast<const l2cap_payload_t*>(&(*vector.begin()));

    // Check l2cap header
    ASSERT_EQ(sizeof(uint32_t) + sizeof(uint16_t) + data.size(), payload->hdr.pdu_size);
    ASSERT_EQ(kL2capChannelIdentifier, payload->hdr.cid);

    // Check l2cap payload
    ASSERT_EQ(kHciHandle, payload->payload.hci_handle);
    ASSERT_EQ(packet_number, payload->payload.packet_number);

    // Ensure the data matches
    for (size_t i = 0; i < data.size(); i++) {
      ASSERT_EQ(data[i], payload->payload.data[i]);
    }
  }

  // Send another packet from the remote
  packet_number++;
  {
    GetRemoteDevice().EnqueueAndSendAclDataSync(hci_handle_, packet_number, data);

    packet::PacketView<kLittleEndian> l2cap_packet = WaitForReceiveIncomingPacket();
    auto vector = PacketViewToVector(l2cap_packet);

    const l2cap_payload_t* payload = reinterpret_cast<const l2cap_payload_t*>(&(*vector.begin()));

    // Check l2cap header
    ASSERT_EQ(sizeof(uint32_t) + sizeof(uint16_t) + data.size(), payload->hdr.pdu_size);
    ASSERT_EQ(kL2capChannelIdentifier, payload->hdr.cid);

    // Check l2cap payload
    ASSERT_EQ(kHciHandle, payload->payload.hci_handle);
    ASSERT_EQ(packet_number, payload->payload.packet_number);

    // Ensure the data matches
    for (size_t i = 0; i < data.size(); i++) {
      ASSERT_EQ(data[i], payload->payload.data[i]);
    }
  }

  sync_client_handler();
}

TEST_F(AclManagerWithLeConnectionTest, le_connection_update_success) {
  connection_->RegisterCallbacks(&mock_le_connection_management_callbacks_, handler_);

  constexpr uint16_t kConnectionIntervalMin = 0x0012;
  constexpr uint16_t kConnectionIntervalMax = 0x0080;
  constexpr uint16_t kConnectionInterval = (kConnectionIntervalMax + kConnectionIntervalMin) / 2;
  constexpr uint16_t kConnectionLatency = 0x0001;
  constexpr uint16_t kSupervisionTimeout = 0x0a00;
  constexpr uint16_t kMinCeLength = 0x0010;
  constexpr uint16_t kMaxCeLength = 0x0020;

  connection_->LeConnectionUpdate(
      kConnectionIntervalMin,
      kConnectionIntervalMax,
      kConnectionLatency,
      kSupervisionTimeout,
      kMinCeLength,
      kMaxCeLength);
  auto update_packet = LastCommand();
  ASSERT_EQ(OpCode::LE_CONNECTION_UPDATE, update_packet.GetOpCode());
  auto update_view = LeConnectionManagementCommand<LeConnectionUpdateView>(update_packet);
  ASSERT_TRUE(update_view.IsValid());
  EXPECT_EQ(update_view.GetConnectionHandle(), kHciHandle);
  ReturnIncomingEvent(LeConnectionUpdateStatusBuilder::Create(ErrorCode::SUCCESS, kNumPackets));
  EXPECT_CALL(
      mock_le_connection_management_callbacks_,
      OnConnectionUpdate(
          hci::ErrorCode::SUCCESS, kConnectionInterval, kConnectionLatency, kSupervisionTimeout))
      .Times(1);
  test_hci_layer_->IncomingLeMetaEvent(LeConnectionUpdateCompleteBuilder::Create(
      ErrorCode::SUCCESS,
      kHciHandle,
      kConnectionInterval,
      kConnectionLatency,
      kSupervisionTimeout));

  sync_client_handler();
}

TEST_F(AclManagerWithLeConnectionTest, send_le_disconnect) {
  auto promise = std::promise<hci::ErrorCode>();
  auto future = promise.get_future();
  EXPECT_CALL(
      mock_le_connection_management_callbacks_,
      OnDisconnection(hci::ErrorCode::REMOTE_USER_TERMINATED_CONNECTION))
      .Times(1)
      .WillOnce([&promise](hci::ErrorCode error_code) { promise.set_value(error_code); });

  test_hci_layer_->Disconnect(kHciHandle, ErrorCode::REMOTE_USER_TERMINATED_CONNECTION);

  ASSERT_EQ(std::future_status::ready, future.wait_for(kCompletionHandlerTimeout));
  ASSERT_EQ(ErrorCode::REMOTE_USER_TERMINATED_CONNECTION, future.get());
}

TEST_F(AclManagerWithLeConnectionTest, send_le_disconnect_data_race) {
  auto promise = std::promise<hci::ErrorCode>();
  auto future = promise.get_future();
  EXPECT_CALL(
      mock_le_connection_management_callbacks_,
      OnDisconnection(hci::ErrorCode::REMOTE_USER_TERMINATED_CONNECTION))
      .Times(1)
      .WillOnce([&promise](hci::ErrorCode error_code) { promise.set_value(error_code); });

  GetRemoteDevice().SendIncomingDataSync(kHciHandle);
  test_hci_layer_->Disconnect(kHciHandle, ErrorCode::REMOTE_USER_TERMINATED_CONNECTION);

  ASSERT_EQ(std::future_status::ready, future.wait_for(kCompletionHandlerTimeout));
  ASSERT_EQ(ErrorCode::REMOTE_USER_TERMINATED_CONNECTION, future.get());
}

TEST_F(AclManagerWithLeConnectionTest, send_le_queue_disconnect) {
  auto promise = std::promise<hci::ErrorCode>();
  auto future = promise.get_future();
  EXPECT_CALL(
      mock_le_connection_management_callbacks_,
      OnDisconnection(hci::ErrorCode::REMOTE_USER_TERMINATED_CONNECTION))
      .Times(1)
      .WillOnce([&promise](hci::ErrorCode error_code) { promise.set_value(error_code); });

  test_hci_layer_->Disconnect(kHciHandle, ErrorCode::REMOTE_USER_TERMINATED_CONNECTION);

  ASSERT_EQ(std::future_status::ready, future.wait_for(kCompletionHandlerTimeout));
  ASSERT_EQ(ErrorCode::REMOTE_USER_TERMINATED_CONNECTION, future.get());
}

TEST_F(AclManagerWithResolvableAddressTest, create_connection_cancel_fail) {
  acl_manager_->CreateLeConnection(kRemoteWithPublicType, kIsDirect);

  // Add device to connect list
  ASSERT_EQ(OpCode::LE_ADD_DEVICE_TO_FILTER_ACCEPT_LIST, LastCommand().GetOpCode());
  ReturnIncomingEvent(
      LeAddDeviceToFilterAcceptListCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS));

  // send create connection command
  ASSERT_EQ(OpCode::LE_CREATE_CONNECTION, LastCommand().GetOpCode());
  ReturnIncomingEvent(LeCreateConnectionStatusBuilder::Create(ErrorCode::SUCCESS, kNumPackets));

  fake_registry_.SynchronizeModuleHandler(
      &HciLayer::Factory, std::chrono::milliseconds(kSynchronizeModuleTimeout));
  fake_registry_.SynchronizeModuleHandler(
      &AclManager::Factory, std::chrono::milliseconds(kSynchronizeModuleTimeout));

  // create another connection
  acl_manager_->CreateLeConnection(kRemoteWithPublicType2, kIsDirect);

  // cancel previous connection
  ASSERT_EQ(OpCode::LE_CREATE_CONNECTION_CANCEL, LastCommand().GetOpCode());

  // receive connection complete of first device
  test_hci_layer_->IncomingLeMetaEvent(LeConnectionCompleteBuilder::Create(
      ErrorCode::SUCCESS,
      kHciHandle,
      Role::PERIPHERAL,
      AddressType::PUBLIC_DEVICE_ADDRESS,
      kRemoteAddress,
      kConnectionInterval,
      kConnectionLatency,
      kSupervisionTimeout,
      ClockAccuracy::PPM_30));

  // receive create connection cancel complete with ErrorCode::CONNECTION_ALREADY_EXISTS
  ReturnIncomingEvent(LeCreateConnectionCancelCompleteBuilder::Create(
      kNumPackets, ErrorCode::CONNECTION_ALREADY_EXISTS));

  // Add another device to connect list
  ASSERT_EQ(OpCode::LE_ADD_DEVICE_TO_FILTER_ACCEPT_LIST, LastCommand().GetOpCode());
  ReturnIncomingEvent(
      LeAddDeviceToFilterAcceptListCompleteBuilder::Create(kNumPackets, ErrorCode::SUCCESS));

  sync_client_handler();
}

}  // namespace acl_manager
}  // namespace hci
}  // namespace bluetooth
