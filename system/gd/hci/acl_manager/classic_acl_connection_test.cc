/*
 * Copyright 2022 The Android Open Source Project
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

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include <chrono>
#include <list>
#include <memory>
#include <mutex>
#include <queue>

#include "common/bidi_queue.h"
#include "common/callback.h"
#include "common/testing/log_capture.h"
#include "hci/acl_manager.h"
#include "hci/acl_manager/le_connection_callbacks.h"
#include "hci/acl_manager/le_connection_management_callbacks.h"
#include "hci/acl_manager/le_impl.h"
#include "hci/address.h"
#include "hci/controller.h"
#include "hci/hci_packets.h"
#include "os/handler.h"
#include "os/log.h"
#include "os/queue.h"
#include "os/thread.h"
#include "packet/bit_inserter.h"
#include "packet/raw_builder.h"

using namespace bluetooth;

namespace {
constexpr char kAddress[] = "00:11:22:33:44:55";
[[maybe_unused]] constexpr uint16_t kConnectionHandle = 123;

[[maybe_unused]] std::vector<hci::DisconnectReason> disconnect_reason_vector = {
    hci::DisconnectReason::AUTHENTICATION_FAILURE,
    hci::DisconnectReason::REMOTE_USER_TERMINATED_CONNECTION,
    hci::DisconnectReason::REMOTE_DEVICE_TERMINATED_CONNECTION_LOW_RESOURCES,
    hci::DisconnectReason::REMOTE_DEVICE_TERMINATED_CONNECTION_POWER_OFF,
    hci::DisconnectReason::UNSUPPORTED_REMOTE_FEATURE,
    hci::DisconnectReason::PAIRING_WITH_UNIT_KEY_NOT_SUPPORTED,
    hci::DisconnectReason::UNACCEPTABLE_CONNECTION_PARAMETERS,
};

// Generic template for all commands
template <typename T, typename U>
T CreateCommand(U u) {
  T command;
  return command;
}

template <>
[[maybe_unused]] hci::DisconnectView CreateCommand(std::shared_ptr<std::vector<uint8_t>> bytes) {
  return hci::DisconnectView::Create(
      hci::AclCommandView::Create(hci::CommandView::Create(hci::PacketView<hci::kLittleEndian>(bytes))));
}

}  // namespace

class TestAclConnectionInterface : public hci::AclConnectionInterface {
 private:
  void EnqueueCommand(
      std::unique_ptr<hci::AclCommandBuilder> command,
      common::ContextualOnceCallback<void(hci::CommandStatusView)> on_status) override {
    const std::lock_guard<std::mutex> lock(command_queue_mutex_);
    command_queue_.push(std::move(command));
    command_status_callbacks.push_back(std::move(on_status));
    if (command_promise_ != nullptr) {
      command_promise_->set_value();
      command_promise_.reset();
    }
  }

  void EnqueueCommand(
      std::unique_ptr<hci::AclCommandBuilder> command,
      common::ContextualOnceCallback<void(hci::CommandCompleteView)> on_complete) override {
    const std::lock_guard<std::mutex> lock(command_queue_mutex_);
    command_queue_.push(std::move(command));
    command_complete_callbacks.push_back(std::move(on_complete));
    if (command_promise_ != nullptr) {
      command_promise_->set_value();
      command_promise_.reset();
    }
  }

 public:
  virtual ~TestAclConnectionInterface() = default;

  std::unique_ptr<hci::CommandBuilder> DequeueCommand() {
    const std::lock_guard<std::mutex> lock(command_queue_mutex_);
    auto packet = std::move(command_queue_.front());
    command_queue_.pop();
    return std::move(packet);
  }

  std::shared_ptr<std::vector<uint8_t>> DequeueCommandBytes() {
    auto command = DequeueCommand();
    auto bytes = std::make_shared<std::vector<uint8_t>>();
    packet::BitInserter bi(*bytes);
    command->Serialize(bi);
    return bytes;
  }

  bool IsPacketQueueEmpty() const {
    const std::lock_guard<std::mutex> lock(command_queue_mutex_);
    return command_queue_.empty();
  }

  size_t NumberOfQueuedCommands() const {
    const std::lock_guard<std::mutex> lock(command_queue_mutex_);
    return command_queue_.size();
  }

 private:
  std::list<common::ContextualOnceCallback<void(hci::CommandCompleteView)>> command_complete_callbacks;
  std::list<common::ContextualOnceCallback<void(hci::CommandStatusView)>> command_status_callbacks;
  std::queue<std::unique_ptr<hci::CommandBuilder>> command_queue_;
  mutable std::mutex command_queue_mutex_;
  std::unique_ptr<std::promise<void>> command_promise_;
  std::unique_ptr<std::future<void>> command_future_;
};

class TestConnectionManagementCallbacks : public hci::acl_manager::ConnectionManagementCallbacks {
 public:
  ~TestConnectionManagementCallbacks() = default;
  void OnConnectionPacketTypeChanged(uint16_t packet_type) override {}
  void OnAuthenticationComplete(hci::ErrorCode hci_status) override {}
  void OnEncryptionChange(hci::EncryptionEnabled enabled) override {}
  void OnChangeConnectionLinkKeyComplete() override {}
  void OnReadClockOffsetComplete(uint16_t clock_offset) override {}
  void OnModeChange(hci::ErrorCode status, hci::Mode current_mode, uint16_t interval) override {}
  void OnSniffSubrating(
      hci::ErrorCode hci_status,
      uint16_t maximum_transmit_latency,
      uint16_t maximum_receive_latency,
      uint16_t minimum_remote_timeout,
      uint16_t minimum_local_timeout) override {}
  // Invoked when controller sends QoS Setup Complete event with Success error code
  void OnQosSetupComplete(
      hci::ServiceType service_type,
      uint32_t token_rate,
      uint32_t peak_bandwidth,
      uint32_t latency,
      uint32_t delay_variation) override {}
  // Invoked when controller sends Flow Specification Complete event with Success error code
  void OnFlowSpecificationComplete(
      hci::FlowDirection flow_direction,
      hci::ServiceType service_type,
      uint32_t token_rate,
      uint32_t token_bucket_size,
      uint32_t peak_bandwidth,
      uint32_t access_latency) override {}
  // Invoked when controller sends Flush Occurred event
  void OnFlushOccurred() override {}
  // Invoked when controller sends Command Complete event for Role Discovery command with Success error code
  void OnRoleDiscoveryComplete(hci::Role current_role) override {}
  // Invoked when controller sends Command Complete event for Read Link Policy Settings command with Success error code
  void OnReadLinkPolicySettingsComplete(uint16_t link_policy_settings) override {}
  // Invoked when controller sends Command Complete event for Read Automatic Flush Timeout command with Success error
  // code
  void OnReadAutomaticFlushTimeoutComplete(uint16_t flush_timeout) override {}
  // Invoked when controller sends Command Complete event for Read Transmit Power Level command with Success error code
  void OnReadTransmitPowerLevelComplete(uint8_t transmit_power_level) override {}
  // Invoked when controller sends Command Complete event for Read Link Supervision Time out command with Success error
  // code
  void OnReadLinkSupervisionTimeoutComplete(uint16_t link_supervision_timeout) override {}
  // Invoked when controller sends Command Complete event for Read Failed Contact Counter command with Success error
  // code
  void OnReadFailedContactCounterComplete(uint16_t failed_contact_counter) override {}
  // Invoked when controller sends Command Complete event for Read Link Quality command with Success error code
  void OnReadLinkQualityComplete(uint8_t link_quality) override {}
  // Invoked when controller sends Command Complete event for Read AFH Channel Map command with Success error code
  void OnReadAfhChannelMapComplete(hci::AfhMode afh_mode, std::array<uint8_t, 10> afh_channel_map) override {}
  void OnReadRssiComplete(uint8_t rssi) override {}
  void OnReadClockComplete(uint32_t clock, uint16_t accuracy) override {}
  void OnCentralLinkKeyComplete(hci::KeyFlag key_flag) override {}
  void OnRoleChange(hci::ErrorCode hci_status, hci::Role new_role) override {}
  void OnDisconnection(hci::ErrorCode reason) override {
    LOG_INFO("Disconnect");
  }
  void OnReadRemoteVersionInformationComplete(
      hci::ErrorCode hci_status, uint8_t lmp_version, uint16_t manufacturer_name, uint16_t sub_version) override {}
  void OnReadRemoteSupportedFeaturesComplete(uint64_t features) override {}
  void OnReadRemoteExtendedFeaturesComplete(uint8_t page_number, uint8_t max_page_number, uint64_t features) override {}
};

namespace bluetooth {
namespace hci {
namespace acl_manager {

class ClassicAclConnectionTest : public ::testing::Test {
 protected:
  void SetUp() override {
    ASSERT_TRUE(hci::Address::FromString(kAddress, address_));
    thread_ = new os::Thread("thread", os::Thread::Priority::NORMAL);
    handler_ = new os::Handler(thread_);
    queue_ = std::make_shared<hci::acl_manager::AclConnection::Queue>(10);
  }

  void TearDown() override {
    handler_->Clear();
    delete handler_;
    delete thread_;
  }

 protected:
  Address address_;
  os::Handler* handler_{nullptr};
  os::Thread* thread_{nullptr};
  std::shared_ptr<hci::acl_manager::AclConnection::Queue> queue_;

  TestAclConnectionInterface acl_connection_interface_;
  TestConnectionManagementCallbacks callbacks;
};

TEST_F(ClassicAclConnectionTest, simple) {
  AclConnectionInterface* acl_connection_interface = nullptr;
  ClassicAclConnection* connection =
      new ClassicAclConnection(queue_, acl_connection_interface, kConnectionHandle, address_);
  connection->RegisterCallbacks(&callbacks, handler_);

  delete connection;
}

class ClassicAclConnectionWithCallbacksTest : public ClassicAclConnectionTest {
 protected:
  void SetUp() override {
    connection_ =
        std::make_unique<ClassicAclConnection>(queue_, &acl_connection_interface_, kConnectionHandle, address_);
    connection_->RegisterCallbacks(&callbacks, handler_);
  }

  void TearDown() override {}

 protected:
  std::unique_ptr<ClassicAclConnection> connection_;
};

TEST_F(ClassicAclConnectionWithCallbacksTest, Disconnect) {
  for (const auto& reason : disconnect_reason_vector) {
    ASSERT_TRUE(connection_->Disconnect(reason));
    ASSERT_FALSE(acl_connection_interface_.IsPacketQueueEmpty());

    auto command = CreateCommand<DisconnectView>(acl_connection_interface_.DequeueCommandBytes());
    ASSERT_TRUE(acl_connection_interface_.IsPacketQueueEmpty());
    ASSERT_TRUE(command.IsValid());
    ASSERT_EQ(reason, command.GetReason());
    ASSERT_EQ(kConnectionHandle, command.GetConnectionHandle());
  }
}

}  // namespace acl_manager
}  // namespace hci
}  // namespace bluetooth
