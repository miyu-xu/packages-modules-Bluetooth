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

#include "hci/acl_manager/le_impl.h"

#include <gtest/gtest.h>

#include <chrono>
#include <mutex>

#include "common/bidi_queue.h"
#include "common/callback.h"
#include "common/testing/log_capture.h"
#include "hci/acl_manager.h"
#include "hci/acl_manager/le_connection_callbacks.h"
#include "hci/address_with_type.h"
#include "hci/controller.h"
#include "hci/hci_packets.h"
#include "os/handler.h"
#include "os/log.h"
#include "packet/bit_inserter.h"
#include "packet/raw_builder.h"

using namespace std::chrono_literals;
using namespace ::bluetooth::hci;

using ::bluetooth::common::BidiQueue;
using ::bluetooth::common::Callback;
using ::bluetooth::os::Handler;
using ::bluetooth::os::Thread;
using ::bluetooth::packet::BitInserter;
using ::bluetooth::packet::RawBuilder;
using ::bluetooth::testing::LogCapture;

namespace {
[[maybe_unused]] const char* test_flags[] = {
    "INIT_logging_debug_enabled_for_all=true",
    nullptr,
};

constexpr char kFixedAddress[] = "c0:aa:bb:cc:dd:ee";
constexpr char kRemoteAddress[] = "00:11:22:33:44:55";
[[maybe_unused]] constexpr bool kAddToFilterAcceptList = true;
[[maybe_unused]] constexpr bool kSkipFilterAcceptList = !kAddToFilterAcceptList;
[[maybe_unused]] constexpr bool kIsDirectConnection = true;
[[maybe_unused]] constexpr bool kIsBackgroundConnection = !kIsDirectConnection;
[[maybe_unused]] constexpr ::bluetooth::crypto_toolbox::Octet16 kRotationIrk = {};
[[maybe_unused]] constexpr std::chrono::milliseconds kMinimumRotationTime(14 * 1000);
[[maybe_unused]] constexpr std::chrono::milliseconds kMaximumRotationTime(16 * 1000);
[[maybe_unused]] constexpr std::array<uint8_t, 16> kPeerIdentityResolvingKey({
    0x00,
    0x01,
    0x02,
    0x03,
    0x04,
    0x05,
    0x06,
    0x07,
    0x08,
    0x09,
    0x0a,
    0x0b,
    0x0c,
    0x0d,
    0x0e,
    0x0f,
});
[[maybe_unused]] constexpr std::array<uint8_t, 16> kLocalIdentityResolvingKey({
    0x80,
    0x81,
    0x82,
    0x83,
    0x84,
    0x85,
    0x86,
    0x87,
    0x88,
    0x89,
    0x8a,
    0x8b,
    0x8c,
    0x8d,
    0x8e,
    0x8f,
});

// Generic template for all commands
template <typename T, typename U>
T CreateCommand(U u) {
  T command;
  return command;
}

template <>
LeSetAddressResolutionEnableView CreateCommand(std::shared_ptr<std::vector<uint8_t>> bytes) {
  return LeSetAddressResolutionEnableView::Create(
      LeSecurityCommandView::Create(CommandView::Create(PacketView<kLittleEndian>(bytes))));
}

template <>
LeAddDeviceToResolvingListView CreateCommand(std::shared_ptr<std::vector<uint8_t>> bytes) {
  return LeAddDeviceToResolvingListView::Create(
      LeSecurityCommandView::Create(CommandView::Create(PacketView<kLittleEndian>(bytes))));
}

CommandCompleteView ReturnCommandComplete(OpCode op_code, ErrorCode error_code) {
  std::vector<uint8_t> success_vector{static_cast<uint8_t>(error_code)};
  auto bytes = std::make_shared<std::vector<uint8_t>>();
  BitInserter bi(*bytes);
  auto builder = CommandCompleteBuilder::Create(uint8_t{1}, op_code, std::make_unique<RawBuilder>(success_vector));
  builder->Serialize(bi);
  return CommandCompleteView::Create(EventView::Create(PacketView<kLittleEndian>(bytes)));
}

}  // namespace

namespace bluetooth {
namespace hci {
namespace acl_manager {

class TestController : public Controller {
 public:
  uint16_t GetNumAclPacketBuffers() const {
    return max_acl_packet_credits_;
  }

  uint16_t GetAclPacketLength() const {
    return hci_mtu_;
  }

  LeBufferSize GetLeBufferSize() const {
    LeBufferSize le_buffer_size;
    le_buffer_size.le_data_packet_length_ = le_hci_mtu_;
    le_buffer_size.total_num_le_packets_ = le_max_acl_packet_credits_;
    return le_buffer_size;
  }

  void RegisterCompletedAclPacketsCallback(CompletedAclPacketsCallback cb) {
    acl_credits_callback_ = cb;
  }

  void SendCompletedAclPacketsCallback(uint16_t handle, uint16_t credits) {
    acl_credits_callback_.Invoke(handle, credits);
  }

  void UnregisterCompletedAclPacketsCallback() {
    acl_credits_callback_ = {};
  }

  const uint16_t max_acl_packet_credits_ = 10;
  const uint16_t hci_mtu_ = 1024;
  const uint16_t le_max_acl_packet_credits_ = 15;
  const uint16_t le_hci_mtu_ = 27;

 private:
  CompletedAclPacketsCallback acl_credits_callback_;
};

class TestHciLayer : public HciLayer {
  // This is a springboard class that converts from `AclCommandBuilder`
  // to `ComandBuilder` for use in the hci layer.
  template <typename T>
  class CommandInterfaceImpl : public CommandInterface<T> {
   public:
    explicit CommandInterfaceImpl(HciLayer& hci) : hci_(hci) {}
    ~CommandInterfaceImpl() = default;

    void EnqueueCommand(
        std::unique_ptr<T> command, common::ContextualOnceCallback<void(CommandCompleteView)> on_complete) override {
      hci_.EnqueueCommand(std::move(command), std::move(on_complete));
    }

    void EnqueueCommand(
        std::unique_ptr<T> command, common::ContextualOnceCallback<void(CommandStatusView)> on_status) override {
      hci_.EnqueueCommand(std::move(command), std::move(on_status));
    }
    HciLayer& hci_;
  };

  std::queue<std::unique_ptr<CommandBuilder>> packet_queue_;
  mutable std::mutex packet_queue_mutex_;

 public:
  virtual void EnqueueCommand(
      std::unique_ptr<CommandBuilder> command,
      common::ContextualOnceCallback<void(CommandCompleteView)> on_complete) override {
    const std::lock_guard<std::mutex> lock(packet_queue_mutex_);
    packet_queue_.push(std::move(command));
  }

  virtual void EnqueueCommand(
      std::unique_ptr<CommandBuilder> command,
      common::ContextualOnceCallback<void(CommandStatusView)> on_status) override {
    const std::lock_guard<std::mutex> lock(packet_queue_mutex_);
    packet_queue_.push(std::move(command));
  }

  std::unique_ptr<CommandBuilder> DequeueCommand() {
    const std::lock_guard<std::mutex> lock(packet_queue_mutex_);
    auto packet = std::move(packet_queue_.front());
    packet_queue_.pop();
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
    const std::lock_guard<std::mutex> lock(packet_queue_mutex_);
    return packet_queue_.empty();
  }

  size_t NumberOfQueuedCommands() const {
    const std::lock_guard<std::mutex> lock(packet_queue_mutex_);
    return packet_queue_.size();
  }

  LeAclConnectionInterface* GetLeAclConnectionInterface(
      common::ContextualCallback<void(LeMetaEventView)> event_handler,
      common::ContextualCallback<void(uint16_t, ErrorCode)> on_disconnect,
      common::ContextualCallback<
          void(hci::ErrorCode hci_status, uint16_t, uint8_t version, uint16_t manufacturer_name, uint16_t sub_version)>
          on_read_remote_version) override {
    disconnect_handlers_.push_back(on_disconnect);
    read_remote_version_handlers_.push_back(on_read_remote_version);
    return &le_acl_connection_manager_interface_;
  }

  void PutLeAclConnectionInterface() override {}

  CommandInterfaceImpl<AclCommandBuilder> le_acl_connection_manager_interface_{*this};
};

class LeConnectionCallbacksTest : public LeConnectionCallbacks {
 public:
  virtual ~LeConnectionCallbacksTest() = default;
  virtual void OnLeConnectSuccess(
      AddressWithType address_with_type, std::unique_ptr<LeAclConnection> connection) override {}
  virtual void OnLeConnectFail(AddressWithType address_with_type, ErrorCode reason) override {}
};

class LeImplTest : public ::testing::Test {
 protected:
  void SetUp() override {
    thread_ = new Thread("thread", Thread::Priority::NORMAL);
    handler_ = new Handler(thread_);
    controller_ = new TestController();
    hci_layer_ = new TestHciLayer();

    round_robin_scheduler_ = new RoundRobinScheduler(handler_, controller_, hci_queue_.GetUpEnd());
    hci_queue_.GetDownEnd()->RegisterDequeue(
        handler_, common::Bind(&LeImplTest::HciDownEndDequeue, common::Unretained(this)));
    le_impl_ = new le_impl(hci_layer_, controller_, handler_, round_robin_scheduler_, true);

    Address address;
    Address::FromString(kFixedAddress, address);
    fixed_address_ = AddressWithType(address, AddressType::PUBLIC_DEVICE_ADDRESS);

    Address::FromString(kRemoteAddress, address);
    remote_public_address_ = AddressWithType(address, AddressType::PUBLIC_DEVICE_ADDRESS);
  }

  void TearDown() override {
    // We cannot teardown our structure without unregistering outselves
    // from our own structure we created.
    if (le_impl_->address_manager_registered) {
      le_impl_->ready_to_unregister = true;
      le_impl_->check_for_unregister();
      sync_handler();
    }

    sync_handler();
    delete le_impl_;

    hci_queue_.GetDownEnd()->UnregisterDequeue();

    delete hci_layer_;
    delete round_robin_scheduler_;
    delete controller_;

    handler_->Clear();
    delete handler_;
    delete thread_;
  }

  void sync_handler() {
    auto promise = std::promise<void>();
    auto future = promise.get_future();
    handler_->BindOnceOn(&promise, &std::promise<void>::set_value).Invoke();
    auto status = future.wait_for(1s);
    ASSERT_EQ(status, std::future_status::ready);
  }

  void HciDownEndDequeue() {
    auto packet = hci_queue_.GetDownEnd()->TryDequeue();
    // Convert from a Builder to a View
    auto bytes = std::make_shared<std::vector<uint8_t>>();
    bluetooth::packet::BitInserter i(*bytes);
    bytes->reserve(packet->size());
    packet->Serialize(i);
    auto packet_view = bluetooth::packet::PacketView<bluetooth::packet::kLittleEndian>(bytes);
    AclView acl_packet_view = AclView::Create(packet_view);
    ASSERT_TRUE(acl_packet_view.IsValid());
    PacketView<true> count_view = acl_packet_view.GetPayload();
    sent_acl_packets_.push(acl_packet_view);

    packet_count_--;
    if (packet_count_ == 0) {
      packet_promise_->set_value();
      packet_promise_ = nullptr;
    }
  }

 protected:
  void set_privacy_policy_for_initiator_address(
      const AddressWithType& address, const LeAddressManager::AddressPolicy& policy) {
    le_impl_->set_privacy_policy_for_initiator_address(
        policy, address, kRotationIrk, kMinimumRotationTime, kMaximumRotationTime);
  }

  AddressWithType fixed_address_;
  AddressWithType remote_public_address_;

  uint16_t packet_count_;
  std::unique_ptr<std::promise<void>> packet_promise_;
  std::unique_ptr<std::future<void>> packet_future_;
  std::queue<AclView> sent_acl_packets_;

  BidiQueue<AclView, AclBuilder> hci_queue_{3};

  Thread* thread_;
  Handler* handler_;
  TestHciLayer* hci_layer_{nullptr};
  TestController* controller_;
  RoundRobinScheduler* round_robin_scheduler_{nullptr};

  LeConnectionCallbacksTest connection_callbacks_;
  struct le_impl* le_impl_;
};

class LeImplWithCallbacksTest : public LeImplTest {
 protected:
  void SetUp() override {
    LeImplTest::SetUp();
    le_impl_->handle_register_le_callbacks(&connection_callbacks_, handler_);
  }

  void TearDown() override {
    LeImplTest::TearDown();
  }
};

TEST_F(LeImplTest, nop) {}

TEST_F(LeImplTest, add_device_to_connect_list) {
  le_impl_->add_device_to_connect_list({{0x01, 0x02, 0x03, 0x04, 0x05, 0x06}, AddressType::PUBLIC_DEVICE_ADDRESS});
  ASSERT_EQ(1UL, le_impl_->connect_list.size());

  le_impl_->add_device_to_connect_list({{0x11, 0x12, 0x13, 0x14, 0x15, 0x16}, AddressType::PUBLIC_DEVICE_ADDRESS});
  ASSERT_EQ(2UL, le_impl_->connect_list.size());

  le_impl_->add_device_to_connect_list({{0x01, 0x02, 0x03, 0x04, 0x05, 0x06}, AddressType::PUBLIC_DEVICE_ADDRESS});
  ASSERT_EQ(2UL, le_impl_->connect_list.size());

  le_impl_->add_device_to_connect_list({{0x11, 0x12, 0x13, 0x14, 0x15, 0x16}, AddressType::PUBLIC_DEVICE_ADDRESS});
  ASSERT_EQ(2UL, le_impl_->connect_list.size());
}

TEST_F(LeImplTest, remove_device_from_connect_list) {
  le_impl_->add_device_to_connect_list({{0x01, 0x02, 0x03, 0x04, 0x05, 0x06}, AddressType::PUBLIC_DEVICE_ADDRESS});
  le_impl_->add_device_to_connect_list({{0x11, 0x12, 0x13, 0x14, 0x15, 0x16}, AddressType::PUBLIC_DEVICE_ADDRESS});
  le_impl_->add_device_to_connect_list({{0x21, 0x22, 0x23, 0x24, 0x25, 0x26}, AddressType::PUBLIC_DEVICE_ADDRESS});
  le_impl_->add_device_to_connect_list({{0x31, 0x32, 0x33, 0x34, 0x35, 0x36}, AddressType::PUBLIC_DEVICE_ADDRESS});
  ASSERT_EQ(4UL, le_impl_->connect_list.size());

  le_impl_->remove_device_from_connect_list({{0x01, 0x02, 0x03, 0x04, 0x05, 0x06}, AddressType::PUBLIC_DEVICE_ADDRESS});
  ASSERT_EQ(3UL, le_impl_->connect_list.size());

  le_impl_->remove_device_from_connect_list({{0x11, 0x12, 0x13, 0x14, 0x15, 0x16}, AddressType::PUBLIC_DEVICE_ADDRESS});
  ASSERT_EQ(2UL, le_impl_->connect_list.size());

  le_impl_->remove_device_from_connect_list({{0x11, 0x12, 0x13, 0x14, 0x15, 0x16}, AddressType::PUBLIC_DEVICE_ADDRESS});
  ASSERT_EQ(2UL, le_impl_->connect_list.size());

  le_impl_->remove_device_from_connect_list({Address::kEmpty, AddressType::PUBLIC_DEVICE_ADDRESS});
  ASSERT_EQ(2UL, le_impl_->connect_list.size());

  le_impl_->remove_device_from_connect_list({{0x21, 0x22, 0x23, 0x24, 0x25, 0x26}, AddressType::PUBLIC_DEVICE_ADDRESS});
  le_impl_->remove_device_from_connect_list({{0x31, 0x32, 0x33, 0x34, 0x35, 0x36}, AddressType::PUBLIC_DEVICE_ADDRESS});
  ASSERT_EQ(0UL, le_impl_->connect_list.size());
}

TEST_F(LeImplTest, register_with_address_manager__AddressPolicyNotSet) {
  bluetooth::common::InitFlags::Load(test_flags);
  std::unique_ptr<LogCapture> log_capture = std::make_unique<LogCapture>();

  le_impl_->register_with_address_manager();
  sync_handler();  // Let |LeAddressManager::register_client| execute on handler
  ASSERT_TRUE(le_impl_->address_manager_registered);
  ASSERT_TRUE(le_impl_->pause_connection);

  le_impl_->ready_to_unregister = true;

  le_impl_->check_for_unregister();
  sync_handler();  // Let |LeAddressManager::unregister_client| execute on handler
  ASSERT_FALSE(le_impl_->address_manager_registered);
  ASSERT_FALSE(le_impl_->pause_connection);
  ASSERT_TRUE(log_capture->Rewind()->Find("address policy isn't set yet"));
  ASSERT_TRUE(log_capture->Rewind()->Find("Client unregistered"));
}

TEST_F(LeImplTest, register_with_address_manager__AddressPolicyPublicAddress) {
  bluetooth::common::InitFlags::Load(test_flags);
  std::unique_ptr<LogCapture> log_capture = std::make_unique<LogCapture>();

  set_privacy_policy_for_initiator_address(fixed_address_, LeAddressManager::AddressPolicy::USE_PUBLIC_ADDRESS);

  le_impl_->register_with_address_manager();
  sync_handler();  // Let |eAddressManager::register_client| execute on handler
  ASSERT_TRUE(le_impl_->address_manager_registered);
  ASSERT_TRUE(le_impl_->pause_connection);

  le_impl_->ready_to_unregister = true;

  le_impl_->check_for_unregister();
  sync_handler();  // Let |LeAddressManager::unregister_client| execute on handler
  ASSERT_FALSE(le_impl_->address_manager_registered);
  ASSERT_FALSE(le_impl_->pause_connection);
  ASSERT_TRUE(log_capture->Rewind()->Find("SetPrivacyPolicyForInitiatorAddress with policy 1"));
  ASSERT_TRUE(log_capture->Rewind()->Find("Client unregistered"));
}

TEST_F(LeImplTest, register_with_address_manager__AddressPolicyStaticAddress) {
  bluetooth::common::InitFlags::Load(test_flags);
  std::unique_ptr<LogCapture> log_capture = std::make_unique<LogCapture>();

  set_privacy_policy_for_initiator_address(fixed_address_, LeAddressManager::AddressPolicy::USE_STATIC_ADDRESS);

  le_impl_->register_with_address_manager();
  sync_handler();  // Let |LeAddressManager::register_client| execute on handler
  ASSERT_TRUE(le_impl_->address_manager_registered);
  ASSERT_TRUE(le_impl_->pause_connection);

  le_impl_->ready_to_unregister = true;

  le_impl_->check_for_unregister();
  sync_handler();  // Let |LeAddressManager::unregister_client| execute on handler
  ASSERT_FALSE(le_impl_->address_manager_registered);
  ASSERT_FALSE(le_impl_->pause_connection);
  ASSERT_TRUE(log_capture->Rewind()->Find("SetPrivacyPolicyForInitiatorAddress with policy 2"));
  ASSERT_TRUE(log_capture->Rewind()->Find("Client unregistered"));
}

TEST_F(LeImplTest, register_with_address_manager__AddressPolicyNonResolvableAddress) {
  bluetooth::common::InitFlags::Load(test_flags);
  std::unique_ptr<LogCapture> log_capture = std::make_unique<LogCapture>();

  set_privacy_policy_for_initiator_address(fixed_address_, LeAddressManager::AddressPolicy::USE_NON_RESOLVABLE_ADDRESS);

  le_impl_->register_with_address_manager();
  sync_handler();  // Let |LeAddressManager::register_client| execute on handler
  ASSERT_TRUE(le_impl_->address_manager_registered);
  ASSERT_TRUE(le_impl_->pause_connection);

  le_impl_->ready_to_unregister = true;

  le_impl_->check_for_unregister();
  sync_handler();  // Let |LeAddressManager::unregister_client| execute on handler
  ASSERT_FALSE(le_impl_->address_manager_registered);
  ASSERT_FALSE(le_impl_->pause_connection);
  ASSERT_TRUE(log_capture->Rewind()->Find("SetPrivacyPolicyForInitiatorAddress with policy 3"));
  ASSERT_TRUE(log_capture->Rewind()->Find("Client unregistered"));
}

TEST_F(LeImplTest, register_with_address_manager__AddressPolicyResolvableAddress) {
  bluetooth::common::InitFlags::Load(test_flags);
  std::unique_ptr<LogCapture> log_capture = std::make_unique<LogCapture>();

  set_privacy_policy_for_initiator_address(fixed_address_, LeAddressManager::AddressPolicy::USE_RESOLVABLE_ADDRESS);

  le_impl_->register_with_address_manager();
  sync_handler();  // Let |LeAddressManager::register_client| execute on handler
  ASSERT_TRUE(le_impl_->address_manager_registered);
  ASSERT_TRUE(le_impl_->pause_connection);

  le_impl_->ready_to_unregister = true;

  le_impl_->check_for_unregister();
  sync_handler();  // Let |LeAddressManager::unregister_client| execute on handler
  ASSERT_FALSE(le_impl_->address_manager_registered);
  ASSERT_FALSE(le_impl_->pause_connection);
  ASSERT_TRUE(log_capture->Rewind()->Find("SetPrivacyPolicyForInitiatorAddress with policy 4"));
  ASSERT_TRUE(log_capture->Rewind()->Find("Client unregistered"));
}

}  // namespace acl_manager
}  // namespace hci
}  // namespace bluetooth
