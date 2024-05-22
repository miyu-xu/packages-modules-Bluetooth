/*
 * Copyright 2024 The Android Open Source Project
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

#include <gtest/gtest.h>

#include <chrono>
#include <list>
#include <optional>
#include <thread>

#include "hal/hci_audio.h"
#include "hal/hci_backend.h"
#include "hal/hci_hal.h"
#include "os/thread.h"

using ::bluetooth::os::Thread;

namespace bluetooth::hal {

class TestBackend : public HciBackend {
 public:
  static std::chrono::milliseconds initialization_delay;

  std::shared_ptr<HciBackendCallbacks> callbacks;
  std::list<std::vector<uint8_t>> cmd, acl, sco, iso;

  void initialize(std::shared_ptr<HciBackendCallbacks> callbacks) override {
    this->callbacks = callbacks;
    std::thread(
        [callbacks](std::chrono::milliseconds delay) {
          std::this_thread::sleep_for(delay);
          callbacks->initializationComplete();
        },
        TestBackend::initialization_delay)
        .detach();
  }

  void sendHciCommand(const std::vector<uint8_t>& command) override {
    cmd.push_back(command);
  }
  void sendAclData(const std::vector<uint8_t>& packet) override {
    acl.push_back(packet);
  }
  void sendScoData(const std::vector<uint8_t>& packet) override {
    sco.push_back(packet);
  }
  void sendIsoData(const std::vector<uint8_t>& packet) override {
    iso.push_back(packet);
  }
};

std::shared_ptr<TestBackend> backend;
std::chrono::milliseconds TestBackend::initialization_delay = std::chrono::milliseconds(0);

std::shared_ptr<HciBackend> HciBackend::CreateAidl() {
  backend = std::make_shared<TestBackend>();
  return backend;
}

std::shared_ptr<HciBackend> HciBackend::CreateHidl(
    [[maybe_unused]] ::bluetooth::os::Handler* handler) {
  backend = std::make_shared<TestBackend>();
  return backend;
}

namespace {

class Incoming : public HciHalCallbacks, public A2dpNotification {
 public:
  std::list<std::vector<uint8_t>> evt, acl, sco, iso;

  struct StartStopA2dpParameters {
    A2dpStreamDirection direction;
    A2dpLinkId link_id;
  };

  std::optional<StartStopA2dpParameters> start_a2dp;
  std::optional<StartStopA2dpParameters> stop_a2dp;
  std::list<std::vector<uint8_t>> a2dp;

  void hciEventReceived(std::vector<uint8_t> packet) override {
    evt.emplace_back(packet);
  }

  void aclDataReceived(std::vector<uint8_t> packet) override {
    acl.emplace_back(packet);
  }

  void scoDataReceived(std::vector<uint8_t> packet) override {
    sco.emplace_back(packet);
  }

  void isoDataReceived(std::vector<uint8_t> packet) override {
    iso.emplace_back(packet);
  }

  void startA2dp(A2dpStreamDirection direction, A2dpLinkId link_id) override {
    start_a2dp.emplace(StartStopA2dpParameters{direction, link_id});
  }

  void stopA2dp(A2dpStreamDirection direction, A2dpLinkId link_id) override {
    stop_a2dp.emplace(StartStopA2dpParameters{direction, link_id});
  }

  void a2dpPacketReceived(std::vector<uint8_t> packet) {
    a2dp.emplace_back(packet);
  }
};

class HciHalAndroidTest : public ::testing::Test {
 protected:
  void SetUp() override {
    thread_ = new Thread("test_thread", Thread::Priority::NORMAL);
    hal = fake_registry_.Start<HciHal>(thread_);

    incoming = std::make_shared<Incoming>();
    hal->registerIncomingPacketCallback(incoming.get());
  }

  void TearDown() override {
    fake_registry_.StopAll();
    delete thread_;
  }

  std::shared_ptr<Incoming> incoming;
  HciHal* hal;

 private:
  ModuleRegistry fake_registry_;
  Thread* thread_;
};

#define __1B(v) uint8_t((v))
#define __2B(v) __1B((v) & 0xff), __1B((v) >> 8)

#define __Z(...) std::initializer_list<uint8_t>({__VA_ARGS__}).size()
#define __1Z(...) __1B(__Z(__VA_ARGS__))
#define __2Z(...) __2B(__Z(__VA_ARGS__))

#define __OPCODE(ogf, ocf) __2B(((ogf) << 10) | (ocf))

#define BDADDR(a0, a1, a2, a3, a4, a5) ((uint8_t[6]){a0, a1, a2, a3, a4, a5})

#define __BDADDR(bdaddr)                                                               \
  __1B(bdaddr[0]), __1B(bdaddr[1]), __1B(bdaddr[2]), __1B(bdaddr[3]), __1B(bdaddr[4]), \
      __1B(bdaddr[5])

#define CMD(ogf, ocf, ...) \
  std::initializer_list<uint8_t>({__OPCODE(ogf, ocf), __1Z(__VA_ARGS__), __VA_ARGS__})

#define CMD_RESET() CMD(0x03, 0x003)

#define CMD_START_A2DP_OFFLOAD_( \
    conn_handle,                 \
    l2cap_cid,                   \
    data_path_direction,         \
    peer_mtu,                    \
    cp_enable_scms_t,            \
    cp_header_scms_t,            \
    ...)                         \
  CMD(0x3f,                      \
      0x15d,                     \
      __1B(0x03),                \
      __2B(conn_handle),         \
      __2B(l2cap_cid),           \
      __1B(data_path_direction), \
      __2B(peer_mtu),            \
      __1B(cp_enable_scms_t),    \
      __1B(cp_header_scms_t),    \
      __VA_ARGS__)

#define CMD_START_A2DP_OFFLOAD(conn_handle, l2cap_cid, data_path_direction, ...) \
  CMD_START_A2DP_OFFLOAD_(conn_handle, l2cap_cid, data_path_direction, 0xffff, 0, 0, __VA_ARGS__)

#define CMD_STOP_A2DP_OFFLOAD(conn_handle, l2cap_cid, data_path_direction) \
  CMD(0x3f, 0x15d, __1B(0x04), __2B(conn_handle), __2B(l2cap_cid), __1B(data_path_direction))

#define EVT(code, ...) std::initializer_list<uint8_t>({__1B(code), __1Z(__VA_ARGS__), __VA_ARGS__})

#define EVT_CONN_COMPLETE_(status, conn_handle, bdaddr, link_type, encryption_mode) \
  EVT(0x03,                                                                         \
      __1B(status),                                                                 \
      __2B(conn_handle),                                                            \
      __BDADDR(bdaddr),                                                             \
      __1B(link_type),                                                              \
      __1B(encryption_mode))

#define EVT_CONN_COMPLETE(status, conn_handle) \
  EVT_CONN_COMPLETE_(status, conn_handle, BDADDR(0x01, 0x23, 0x45, 0x67, 0x89, 0xAB), 0x01, 0x00)

#define EVT_DISCONN_COMPLETE_(status, conn_handle, reason) \
  EVT(0x05, __1B(status), __2B(conn_handle), __1B(reason))

#define EVT_DISCONN_COMPLETE(status, conn_handle) EVT_DISCONN_COMPLETE_(status, conn_handle, 0)

#define EVT_CMD_COMPLETE_(num_of_allowed_command_packets, cmd_ogf, cmd_ocf, ...) \
  EVT(0x0e, __1B(num_of_allowed_command_packets), __OPCODE(cmd_ogf, cmd_ocf), __VA_ARGS__)

#define EVT_CMD_COMPLETE(cmd_ogf, cmd_ocf, ...) EVT_CMD_COMPLETE_(1, cmd_ogf, cmd_ocf, __VA_ARGS__)

#define EVT_RESET_COMPLETE(status) EVT_CMD_COMPLETE(0x03, 0x003, __1B(status))

#define EVT_READ_BUFFER_SIZE_COMPLETE_(                                             \
    status, num_acl_packets, acl_packet_length, num_sco_packets, sco_packet_length) \
  EVT_CMD_COMPLETE(                                                                 \
      0x04,                                                                         \
      0x005,                                                                        \
      __1B(status),                                                                 \
      __2B(acl_packet_length),                                                      \
      __1B(sco_packet_length),                                                      \
      __2B(num_acl_packets),                                                        \
      __2B(num_sco_packets))

#define EVT_READ_BUFFER_SIZE_COMPLETE(status, num_acl_packets, acl_packet_length) \
  EVT_READ_BUFFER_SIZE_COMPLETE_(status, num_acl_packets, acl_packet_length, 0, 0)

#define EVT_START_A2DP_OFFLOAD_COMPLETE(status) \
  EVT_CMD_COMPLETE(0x3f, 0x15d, __1B(status), __1B(0x03))

#define EVT_STOP_A2DP_OFFLOAD_COMPLETE(status) \
  EVT_CMD_COMPLETE(0x3f, 0x15d, __1B(status), __1B(0x04))

#define EVT_NUM_COMPLETED_PACKETS_1(conn_handle, n) EVT(0x13, 1, __2B(conn_handle), __2B(n))

#define EVT_NUM_COMPLETED_PACKETS_2(h1, n1, h2, n2) \
  EVT(0x13, 2, __2B(h1), __2B(n1), __2B(h2), __2B(n2))

#define ACL(h, f, ...) \
  std::initializer_list<uint8_t>({__2B((h) | ((f) << 12)), __2Z(__VA_ARGS__), __VA_ARGS__})

TEST_F(HciHalAndroidTest, init) {
  TearDown();

  TestBackend::initialization_delay = std::chrono::milliseconds(100);
  const auto t0 = std::chrono::steady_clock::now();
  SetUp();
  const auto t1 = std::chrono::steady_clock::now();
  TestBackend::initialization_delay = std::chrono::milliseconds(0);

  EXPECT_GE(t1 - t0, TestBackend::initialization_delay);
}

TEST_F(HciHalAndroidTest, reset) {
  hal->sendHciCommand(CMD_RESET());
  EXPECT_EQ(backend->cmd.size(), size_t(1));
  EXPECT_EQ(backend->cmd.front(), std::vector<uint8_t>(CMD_RESET()));
  backend->cmd.clear();

  backend->callbacks->hciEventReceived(EVT_RESET_COMPLETE(0));
  EXPECT_EQ(incoming->evt.size(), size_t(1));
  EXPECT_EQ(incoming->evt.front(), std::vector<uint8_t>(EVT_RESET_COMPLETE(0)));
  incoming->evt.clear();
}

TEST_F(HciHalAndroidTest, transparent) {
  backend->callbacks->hciEventReceived(EVT_RESET_COMPLETE(0));
  backend->callbacks->hciEventReceived(EVT_READ_BUFFER_SIZE_COMPLETE(0, 12, 1021));
  backend->callbacks->hciEventReceived(EVT_CONN_COMPLETE(0, 0x123));
  EXPECT_EQ(incoming->evt.size(), size_t(3));

  EXPECT_EQ(incoming->evt.front(), std::vector<uint8_t>(EVT_RESET_COMPLETE(0)));
  incoming->evt.pop_front();

  EXPECT_EQ(
      incoming->evt.front(), std::vector<uint8_t>(EVT_READ_BUFFER_SIZE_COMPLETE(0, 12, 1021)));
  incoming->evt.pop_front();

  EXPECT_EQ(incoming->evt.front(), std::vector<uint8_t>(EVT_CONN_COMPLETE(0, 0x123)));
  incoming->evt.pop_front();

  for (int i = 0; i < 12; i++) hal->sendAclData(ACL(0x123, ~i, __1B(i)));

  EXPECT_EQ(backend->acl.size(), size_t(12));
  for (int i = 0; i < int(backend->acl.size()); backend->acl.pop_front(), ++i)
    EXPECT_EQ(backend->acl.front(), std::vector<uint8_t>(ACL(0x123, ~i, __1B(i))));

  for (int i = 0; i < 12 / 2; i++)
    backend->callbacks->hciEventReceived(EVT_NUM_COMPLETED_PACKETS_1(0x123, 2));

  EXPECT_EQ(incoming->evt.size(), size_t(12 / 2));
  for (int i = 0; i < 12 / 2; incoming->evt.pop_front(), ++i)
    EXPECT_EQ(incoming->evt.front(), std::vector<uint8_t>(EVT_NUM_COMPLETED_PACKETS_1(0x123, 2)));

  backend->callbacks->hciEventReceived(EVT_DISCONN_COMPLETE(0, 0x123));
  EXPECT_EQ(incoming->evt.front(), std::vector<uint8_t>(EVT_DISCONN_COMPLETE(0, 0x123)));
  incoming->evt.pop_front();
}

TEST_F(HciHalAndroidTest, a2dp_hardware) {
  backend->callbacks->hciEventReceived(EVT_RESET_COMPLETE(0));
  backend->callbacks->hciEventReceived(EVT_READ_BUFFER_SIZE_COMPLETE(0, 12, 1021));
  backend->callbacks->hciEventReceived(EVT_CONN_COMPLETE(0, 0x123));
  incoming->evt.clear();

  hal->sendHciCommand(CMD_START_A2DP_OFFLOAD(0x123, 0xabcd, 0));
  EXPECT_EQ(incoming->evt.size(), size_t(0));
  EXPECT_EQ(backend->cmd.size(), size_t(1));
  EXPECT_EQ(backend->cmd.front(), std::vector<uint8_t>(CMD_START_A2DP_OFFLOAD(0x123, 0xabcd, 0)));
  backend->cmd.pop_front();

  hal->sendHciCommand(CMD_STOP_A2DP_OFFLOAD(0x123, 0xabcd, 0));
  EXPECT_EQ(incoming->evt.size(), size_t(0));
  EXPECT_EQ(backend->cmd.size(), size_t(1));
  EXPECT_EQ(backend->cmd.front(), std::vector<uint8_t>(CMD_STOP_A2DP_OFFLOAD(0x123, 0xabcd, 0)));
  backend->cmd.pop_front();
}

TEST_F(HciHalAndroidTest, a2dp_start_stop) {
  backend->callbacks->hciEventReceived(EVT_RESET_COMPLETE(0));
  backend->callbacks->hciEventReceived(EVT_READ_BUFFER_SIZE_COMPLETE(0, 12, 1021));
  incoming->evt.clear();

  setupA2dpOutput(std::nullopt, incoming);

  backend->callbacks->hciEventReceived(EVT_CONN_COMPLETE(0, 0x123));
  incoming->evt.clear();

  hal->sendHciCommand(CMD_START_A2DP_OFFLOAD(0x123, 0xabcd, 0));
  EXPECT_EQ(backend->cmd.size(), size_t(0));
  EXPECT_EQ(incoming->evt.size(), size_t(1));
  EXPECT_EQ(incoming->evt.front(), std::vector<uint8_t>(EVT_START_A2DP_OFFLOAD_COMPLETE(0)));
  incoming->evt.clear();

  EXPECT_TRUE(incoming->start_a2dp != std::nullopt);
  EXPECT_EQ(incoming->start_a2dp->direction, 0);
  EXPECT_EQ(incoming->start_a2dp->link_id, A2dpLinkId({0x123, 0xabcd}));
  incoming->start_a2dp.reset();

  hal->sendHciCommand(CMD_STOP_A2DP_OFFLOAD(0x123, 0xabcd, 0));
  EXPECT_EQ(backend->cmd.size(), size_t(0));
  EXPECT_EQ(incoming->evt.size(), size_t(1));
  EXPECT_EQ(incoming->evt.front(), std::vector<uint8_t>(EVT_STOP_A2DP_OFFLOAD_COMPLETE(0)));
  incoming->evt.clear();

  EXPECT_TRUE(incoming->stop_a2dp != std::nullopt);
  EXPECT_EQ(incoming->stop_a2dp->direction, 0);
  EXPECT_EQ(incoming->stop_a2dp->link_id, A2dpLinkId({0x123, 0xabcd}));
  incoming->stop_a2dp.reset();
}

TEST_F(HciHalAndroidTest, a2dp_enabled) {
  backend->callbacks->hciEventReceived(EVT_RESET_COMPLETE(0));
  backend->callbacks->hciEventReceived(EVT_READ_BUFFER_SIZE_COMPLETE(0, 12, 1021));
  backend->callbacks->hciEventReceived(EVT_CONN_COMPLETE(0, 0x123));
  incoming->evt.clear();

  // Setup A2DP Output

  setupA2dpOutput(std::nullopt, incoming);

  // Fulfill the FIFO Controller

  int isend = 0;

  for (; isend < 12; isend++) hal->sendAclData(ACL(0x123, ~isend, __1B(isend)));
  EXPECT_EQ(backend->acl.size(), size_t(12));

  // Start A2DP Stream

  hal->sendHciCommand(CMD_START_A2DP_OFFLOAD(0x123, 0xabcd, 0));
  incoming->evt.clear();
  incoming->start_a2dp.reset();

  // Acknowledge 6 packets

  backend->callbacks->hciEventReceived(EVT_NUM_COMPLETED_PACKETS_1(0x123, 6));
  EXPECT_EQ(incoming->evt.size(), size_t(1));
  EXPECT_EQ(incoming->evt.front(), std::vector<uint8_t>(EVT_NUM_COMPLETED_PACKETS_1(0x123, 6)));
  incoming->evt.clear();

  // Refill to 12 packets, expect 3 packets not committed

  for (; isend < 12 + 6; isend++) hal->sendAclData(ACL(0x123, ~isend, __1B(isend)));
  EXPECT_EQ(backend->acl.size(), size_t(18 - 3));

  // Acknowledge 3 packets, all packets should be now committed

  backend->callbacks->hciEventReceived(EVT_NUM_COMPLETED_PACKETS_1(0x123, 1));
  EXPECT_EQ(backend->acl.size(), size_t(18 - 2));

  backend->callbacks->hciEventReceived(EVT_NUM_COMPLETED_PACKETS_1(0x123, 2));
  EXPECT_EQ(backend->acl.size(), size_t(18));

  incoming->evt.clear();

  // Finally check packets

  for (int i = 0; i < isend; backend->acl.pop_front(), ++i)
    EXPECT_EQ(backend->acl.front(), std::vector<uint8_t>(ACL(0x123, ~i, __1B(i))));
}

TEST_F(HciHalAndroidTest, a2dp_send) {
  backend->callbacks->hciEventReceived(EVT_RESET_COMPLETE(0));
  backend->callbacks->hciEventReceived(EVT_READ_BUFFER_SIZE_COMPLETE(0, 12, 1021));
  backend->callbacks->hciEventReceived(EVT_CONN_COMPLETE(0, 0x123));

  setupA2dpOutput(std::nullopt, incoming);

  hal->sendHciCommand(CMD_START_A2DP_OFFLOAD(0x123, 0xabcd, 0));
  incoming->evt.clear();
  incoming->start_a2dp.reset();

  // Sent 3 packets from the stack, and 3 from A2DP

  for (int i = 0; i < 3; i++) {
    hal->sendAclData(ACL(0x123, ~i, __1B(i)));
    sendA2dpPacket(ACL(0x123, ~i, __2B(0), __2B(0xabcd), __1B(i)));
  }

  EXPECT_EQ(backend->acl.size(), size_t(2 * 3));
  for (int i = 0; i < 3; ++i) {
    EXPECT_EQ(backend->acl.front(), std::vector<uint8_t>(ACL(0x123, ~i, __1B(i))));
    backend->acl.pop_front();
    EXPECT_EQ(
        backend->acl.front(), std::vector<uint8_t>(ACL(0x123, ~i, __2B(0), __2B(0xabcd), __1B(i))));
    backend->acl.pop_front();
  }

  // Send 6 more packets from A2DP, expext 3 buffered,
  // and 6 more from the stack, expect 3 buffered too

  for (int i = 3; i < 3 + 6; i++) sendA2dpPacket(ACL(0x123, ~i, __2B(0), __2B(0xabcd), __1B(i)));
  EXPECT_EQ(backend->acl.size(), size_t(3));

  for (int i = 3; i < 3 + 6; i++) hal->sendAclData(ACL(0x123, ~i, __1B(i)));
  EXPECT_EQ(backend->acl.size(), size_t(6));

  for (int i = 3; i < 6; ++i, backend->acl.pop_front())
    EXPECT_EQ(
        backend->acl.front(), std::vector<uint8_t>(ACL(0x123, ~i, __2B(0), __2B(0xabcd), __1B(i))));

  for (int i = 3; i < 6; ++i, backend->acl.pop_front())
    EXPECT_EQ(backend->acl.front(), std::vector<uint8_t>(ACL(0x123, ~i, __1B(i))));

  // Acknowledge, on each path, 1, then 2 packets

  backend->callbacks->hciEventReceived(EVT_NUM_COMPLETED_PACKETS_1(0x123, 2));
  EXPECT_EQ(incoming->evt.front(), std::vector<uint8_t>(EVT_NUM_COMPLETED_PACKETS_1(0x123, 1)));
  incoming->evt.pop_front();

  EXPECT_EQ(backend->acl.size(), size_t(2));
  for (int i = 6; i < 7; ++i) {
    EXPECT_EQ(
        backend->acl.front(), std::vector<uint8_t>(ACL(0x123, ~i, __2B(0), __2B(0xabcd), __1B(i))));
    backend->acl.pop_front();
    EXPECT_EQ(backend->acl.front(), std::vector<uint8_t>(ACL(0x123, ~i, __1B(i))));
    backend->acl.pop_front();
  }

  backend->callbacks->hciEventReceived(EVT_NUM_COMPLETED_PACKETS_1(0x123, 4));
  EXPECT_EQ(incoming->evt.front(), std::vector<uint8_t>(EVT_NUM_COMPLETED_PACKETS_1(0x123, 2)));
  incoming->evt.pop_front();

  EXPECT_EQ(backend->acl.size(), size_t(4));
  for (int i = 7; i < 9; ++i, backend->acl.pop_front())
    EXPECT_EQ(
        backend->acl.front(), std::vector<uint8_t>(ACL(0x123, ~i, __2B(0), __2B(0xabcd), __1B(i))));
  for (int i = 7; i < 9; ++i, backend->acl.pop_front())
    EXPECT_EQ(backend->acl.front(), std::vector<uint8_t>(ACL(0x123, ~i, __1B(i))));
}

TEST_F(HciHalAndroidTest, a2dp_send_limit) {
  backend->callbacks->hciEventReceived(EVT_RESET_COMPLETE(0));
  backend->callbacks->hciEventReceived(EVT_READ_BUFFER_SIZE_COMPLETE(0, 12, 1021));
  backend->callbacks->hciEventReceived(EVT_CONN_COMPLETE(0, 0x123));

  setupA2dpOutput(std::nullopt, incoming);

  hal->sendHciCommand(CMD_START_A2DP_OFFLOAD(0x123, 0xabcd, 0));
  incoming->evt.clear();
  incoming->start_a2dp.reset();

  // Sent 3 packets from the stack, and 3 from A2DP

  for (int i = 0; i < 3; i++) {
    hal->sendAclData(ACL(0x123, ~i, __1B(i)));
    sendA2dpPacket(ACL(0x123, ~i, __2B(0), __2B(0xabcd), __1B(i)));
  }

  EXPECT_EQ(backend->acl.size(), size_t(2 * 3));
  backend->acl.clear();

  // Send A2DP packets up to the limit

  int max_packets = A2dpBuffers().max_packets;
  for (int i = 3; i < max_packets; ++i)
    sendA2dpPacket(ACL(0x123, ~i, __2B(0), __2B(0xabcd), __1B(i)));
  EXPECT_EQ(backend->acl.size(), size_t(12 / 2 - 3));

  backend->callbacks->hciEventReceived(EVT_NUM_COMPLETED_PACKETS_1(0x123, 3 + 12 / 2));

  EXPECT_EQ(backend->acl.size(), size_t(max_packets - 3));
  for (int i = 3; i < max_packets; ++i, backend->acl.pop_front())
    EXPECT_EQ(
        backend->acl.front(), std::vector<uint8_t>(ACL(0x123, ~i, __2B(0), __2B(0xabcd), __1B(i))));

  backend->callbacks->hciEventReceived(EVT_NUM_COMPLETED_PACKETS_1(0x123, 3 + max_packets));

  // Sent 3 packets from the stack, and 3 from A2DP

  for (int i = 0; i < 3; i++) {
    hal->sendAclData(ACL(0x123, ~i, __1B(i)));
    sendA2dpPacket(ACL(0x123, ~i, __2B(0), __2B(0xabcd), __1B(i)));
  }

  EXPECT_EQ(backend->acl.size(), size_t(2 * 3));
  backend->acl.clear();

  // Send A2DP packets exceeding the limit

  for (int i = 3; i < max_packets; i++)
    sendA2dpPacket(ACL(0x123, ~i, __2B(0), __2B(0xabcd), __1B(i)));
  sendA2dpPacket(ACL(0x123, 0, __2B(0), __2B(0xabcd), __1B(0xdead)));
  EXPECT_EQ(backend->acl.size(), size_t(12 / 2 - 3));

  backend->callbacks->hciEventReceived(EVT_NUM_COMPLETED_PACKETS_1(0x123, 3 + 12 / 2));

  EXPECT_EQ(backend->acl.size(), size_t(max_packets - 3));
  for (int i = 3; i < max_packets; ++i, backend->acl.pop_front())
    EXPECT_EQ(
        backend->acl.front(), std::vector<uint8_t>(ACL(0x123, ~i, __2B(0), __2B(0xabcd), __1B(i))));
}

TEST_F(HciHalAndroidTest, a2dp_two_connections) {
  backend->callbacks->hciEventReceived(EVT_RESET_COMPLETE(0));
  backend->callbacks->hciEventReceived(EVT_READ_BUFFER_SIZE_COMPLETE(0, 12, 1021));
  backend->callbacks->hciEventReceived(EVT_CONN_COMPLETE(0, 0x123));
  backend->callbacks->hciEventReceived(EVT_CONN_COMPLETE(0, 0x456));

  setupA2dpOutput(std::nullopt, incoming);

  hal->sendHciCommand(CMD_START_A2DP_OFFLOAD(0x456, 0xabcd, 0));
  incoming->evt.clear();
  incoming->start_a2dp.reset();

  // Repeat 4 times, 1 packet on first conn, 1 on second and 1 A2DP on second

  for (int i = 0; i < 4; i++) {
    hal->sendAclData(ACL(0x123, ~i, __1B(i)));
    hal->sendAclData(ACL(0x456, ~i, __1B(i)));
    sendA2dpPacket(ACL(0x456, ~i, __2B(0), __2B(0xabcd), __1B(i)));
  }
  EXPECT_EQ(backend->acl.size(), size_t(12));
  backend->acl.clear();

  backend->callbacks->hciEventReceived(EVT_NUM_COMPLETED_PACKETS_1(0x123, 1));
  EXPECT_EQ(incoming->evt.front(), std::vector<uint8_t>(EVT_NUM_COMPLETED_PACKETS_1(0x123, 1)));
  incoming->evt.pop_front();

  backend->callbacks->hciEventReceived(EVT_NUM_COMPLETED_PACKETS_2(0x123, 1, 0x456, 1));
  EXPECT_EQ(
      incoming->evt.front(), std::vector<uint8_t>(EVT_NUM_COMPLETED_PACKETS_2(0x123, 1, 0x456, 1)));
  incoming->evt.pop_front();

  backend->callbacks->hciEventReceived(EVT_NUM_COMPLETED_PACKETS_2(0x123, 2, 0x456, 2));
  EXPECT_EQ(
      incoming->evt.front(), std::vector<uint8_t>(EVT_NUM_COMPLETED_PACKETS_2(0x123, 2, 0x456, 1)));
  incoming->evt.pop_front();

  backend->callbacks->hciEventReceived(EVT_NUM_COMPLETED_PACKETS_1(0x456, 5));
  EXPECT_EQ(incoming->evt.front(), std::vector<uint8_t>(EVT_NUM_COMPLETED_PACKETS_1(0x456, 2)));
  incoming->evt.pop_front();

  // Repeat 6 times, 1 packet on first conn, 1 on second and 1 A2DP on second
  // First connection drop -> reschedule transfer

  for (int i = 0; i < 6; i++) {
    hal->sendAclData(ACL(0x123, ~i, __1B(i)));
    hal->sendAclData(ACL(0x456, ~i, __1B(i)));
    sendA2dpPacket(ACL(0x456, ~i, __2B(0), __2B(0xabcd), __1B(i)));
  }
  EXPECT_EQ(backend->acl.size(), size_t(12));
  backend->acl.clear();

  backend->callbacks->hciEventReceived(EVT_DISCONN_COMPLETE(0, 0x123));
  EXPECT_EQ(backend->acl.size(), size_t(4));
  backend->acl.clear();
}

TEST_F(HciHalAndroidTest, a2dp_release) {
  backend->callbacks->hciEventReceived(EVT_RESET_COMPLETE(0));
  backend->callbacks->hciEventReceived(EVT_READ_BUFFER_SIZE_COMPLETE(0, 12, 1021));
  backend->callbacks->hciEventReceived(EVT_CONN_COMPLETE(0, 0x123));

  // Setup, Start, Stop, and Release

  setupA2dpOutput(std::nullopt, incoming);

  hal->sendHciCommand(CMD_START_A2DP_OFFLOAD(0x123, 0xabcd, 0));
  incoming->evt.clear();

  for (int i = 0; i < int(A2dpBuffers().max_packets); i++)
    sendA2dpPacket(ACL(0x123, ~i, __2B(0), __2B(0xabcd), __1B(i)));
  EXPECT_EQ(backend->acl.size(), size_t(12 / 2));
  backend->acl.clear();

  hal->sendHciCommand(CMD_STOP_A2DP_OFFLOAD(0x123, 0xabcd, 0));
  incoming->evt.clear();
  incoming->stop_a2dp.reset();

  for (int i = 0; i < 12; i++) hal->sendAclData(ACL(0x123, ~i, __1B(i)));

  backend->callbacks->hciEventReceived(EVT_NUM_COMPLETED_PACKETS_1(0x123, 12 / 2));
  EXPECT_EQ(backend->acl.size(), size_t(12));
  backend->acl.clear();

  backend->callbacks->hciEventReceived(EVT_NUM_COMPLETED_PACKETS_1(0x123, 12));
  incoming->evt.clear();

  releaseA2dpOutput(std::nullopt);
  incoming->start_a2dp.reset();

  // Setup, Start, ... and Release

  setupA2dpOutput(std::nullopt, incoming);

  hal->sendHciCommand(CMD_START_A2DP_OFFLOAD(0x123, 0xabcd, 0));
  incoming->evt.clear();

  for (int i = 0; i < int(A2dpBuffers().max_packets); i++)
    sendA2dpPacket(ACL(0x123, ~i, __2B(0), __2B(0xabcd), __1B(i)));
  EXPECT_EQ(backend->acl.size(), size_t(12 / 2));
  backend->acl.clear();

  releaseA2dpOutput(std::nullopt);
  incoming->start_a2dp.reset();

  for (int i = 0; i < 12; i++) hal->sendAclData(ACL(0x123, ~i, __1B(i)));

  backend->callbacks->hciEventReceived(EVT_NUM_COMPLETED_PACKETS_1(0x123, 12 / 2));
  EXPECT_EQ(backend->acl.size(), size_t(12));
  backend->acl.clear();
}

TEST_F(HciHalAndroidTest, a2dp_recv) {
  backend->callbacks->hciEventReceived(EVT_RESET_COMPLETE(0));
  backend->callbacks->hciEventReceived(EVT_READ_BUFFER_SIZE_COMPLETE(0, 12, 1021));
  backend->callbacks->hciEventReceived(EVT_CONN_COMPLETE(0, 0x123));
  backend->callbacks->hciEventReceived(EVT_CONN_COMPLETE(0, 0x456));
  incoming->evt.clear();

  // Setup and start A2dp Input

  setupA2dpInput(std::nullopt, incoming, [this](std::vector<uint8_t> packet) {
    incoming->a2dpPacketReceived(std::move(packet));
  });

  hal->sendHciCommand(CMD_START_A2DP_OFFLOAD(0x456, 0xabcd, 1));
  EXPECT_EQ(backend->cmd.size(), size_t(0));
  EXPECT_EQ(incoming->evt.size(), size_t(1));
  EXPECT_EQ(incoming->evt.front(), std::vector<uint8_t>(EVT_START_A2DP_OFFLOAD_COMPLETE(0)));
  incoming->evt.clear();

  // Repeat 4 times, 1 packet on first conn, 1 on second and 1 A2DP on second

  for (int i = 0; i < 4; i++) {
    backend->callbacks->aclDataReceived(ACL(0x123, ~i, __1B(i)));
    backend->callbacks->aclDataReceived(ACL(0x456, ~i, __1B(i)));
    backend->callbacks->aclDataReceived(ACL(0x456, ~i, __2B(0), __2B(0xabcd), __1B(i)));
  }

  EXPECT_EQ(incoming->acl.size(), size_t(4 * 2));
  EXPECT_EQ(incoming->a2dp.size(), size_t(4));
  for (int i = 0; i < 4; ++i) {
    EXPECT_EQ(incoming->acl.front(), std::vector<uint8_t>(ACL(0x123, ~i, __1B(i))));
    incoming->acl.pop_front();
    EXPECT_EQ(incoming->acl.front(), std::vector<uint8_t>(ACL(0x456, ~i, __1B(i))));
    incoming->acl.pop_front();
    EXPECT_EQ(
        incoming->a2dp.front(),
        std::vector<uint8_t>(ACL(0x456, ~i, __2B(0), __2B(0xabcd), __1B(i))));
    incoming->a2dp.pop_front();
  }

  // Stop A2dp and check disabling

  hal->sendHciCommand(CMD_STOP_A2DP_OFFLOAD(0x456, 0xabcd, 1));
  EXPECT_EQ(backend->cmd.size(), size_t(0));
  EXPECT_EQ(incoming->evt.size(), size_t(1));
  EXPECT_EQ(incoming->evt.front(), std::vector<uint8_t>(EVT_STOP_A2DP_OFFLOAD_COMPLETE(0)));
  incoming->evt.clear();

  for (int i = 0; i < 4; i++) {
    backend->callbacks->aclDataReceived(ACL(0x123, ~i, __1B(i)));
    backend->callbacks->aclDataReceived(ACL(0x456, ~i, __1B(i)));
    backend->callbacks->aclDataReceived(ACL(0x456, ~i, __2B(0), __2B(0xabcd), __1B(i)));
  }

  EXPECT_EQ(incoming->acl.size(), size_t(4 * 3));
  incoming->acl.clear();

  // Let's restart, ...

  hal->sendHciCommand(CMD_START_A2DP_OFFLOAD(0x456, 0xabcd, 1));
  EXPECT_EQ(backend->cmd.size(), size_t(0));
  EXPECT_EQ(incoming->evt.size(), size_t(1));
  EXPECT_EQ(incoming->evt.front(), std::vector<uint8_t>(EVT_START_A2DP_OFFLOAD_COMPLETE(0)));
  incoming->evt.clear();

  for (int i = 0; i < 4; i++) {
    backend->callbacks->aclDataReceived(ACL(0x123, ~i, __1B(i)));
    backend->callbacks->aclDataReceived(ACL(0x456, ~i, __1B(i)));
    backend->callbacks->aclDataReceived(ACL(0x456, ~i, __2B(0), __2B(0xabcd), __1B(i)));
  }

  EXPECT_EQ(incoming->acl.size(), size_t(4 * 2));
  incoming->acl.clear();
  EXPECT_EQ(incoming->a2dp.size(), size_t(4));
  incoming->a2dp.clear();

  // ... and release without stop

  releaseA2dpInput(std::nullopt);

  for (int i = 0; i < 4; i++) {
    backend->callbacks->aclDataReceived(ACL(0x123, ~i, __1B(i)));
    backend->callbacks->aclDataReceived(ACL(0x456, ~i, __1B(i)));
    backend->callbacks->aclDataReceived(ACL(0x456, ~i, __2B(0), __2B(0xabcd), __1B(i)));
  }

  EXPECT_EQ(incoming->acl.size(), size_t(4 * 3));
  incoming->acl.clear();
}

}  // namespace
}  // namespace bluetooth::hal
