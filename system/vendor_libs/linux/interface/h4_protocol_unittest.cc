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

#define LOG_TAG "bt_h4_unittest"

#include "h4_protocol.h"

#include <gmock/gmock.h>
#include <gtest/gtest.h>
#include <log/log.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>

#include <condition_variable>
#include <cstdint>
#include <cstring>
#include <mutex>
#include <vector>

#include "async_fd_watcher.h"
#include "log/log.h"

using android::hardware::bluetooth::async::AsyncFdWatcher;
using android::hardware::bluetooth::hci::H4Protocol;
using ::testing::Eq;

static char sample_data1[100] = "A point is that which has no part.";
static char sample_data2[100] = "A line is breadthless length.";
static char sample_data3[100] = "The ends of a line are points.";
static char sample_data4[100] =
    "A plane surface is a surface which lies evenly with the straight ...";
static char acl_data[100] =
    "A straight line is a line which lies evenly with the points on itself.";
static char sco_data[100] =
    "A surface is that which has length and breadth only.";
static char event_data[100] = "The edges of a surface are lines.";
static char iso_data[100] =
    "A plane angle is the inclination to one another of two lines in a ...";

MATCHER_P3(PacketMatches, header, header_length, payload,
           "Match header_length bytes of header and then the payload") {
  size_t payload_length = strlen(payload);
  if (header_length + payload_length != arg.size()) {
    return false;
  }

  if (memcmp(header, arg.data(), header_length) != 0) {
    return false;
  }

  return memcmp(payload, arg.data() + header_length, payload_length) == 0;
};

ACTION_P2(Notify, mutex, condition) {
  ALOGD("%s", __func__);
  std::unique_lock<std::mutex> lock(*mutex);
  condition->notify_one();
}

class H4ProtocolTest : public ::testing::Test {
 protected:
  void SetUp() override {
    ALOGD("%s", __func__);

    int sockfd[2];
    socketpair(AF_LOCAL, SOCK_STREAM, 0, sockfd);
    chip_uart_fd_ = sockfd[1];
    stack_uart_fd_ = sockfd[0];
    h4_hci_ = std::make_shared<H4Protocol>(
        stack_uart_fd_, event_cb_.AsStdFunction(), acl_cb_.AsStdFunction(),
        sco_cb_.AsStdFunction(), iso_cb_.AsStdFunction(),
        disconnect_cb_.AsStdFunction());
  }

  void TearDown() override {
    close(stack_uart_fd_);
    close(chip_uart_fd_);
  }

  virtual void CallDataReady() { h4_hci_->OnDataReady(stack_uart_fd_); }

  void SendAndReadUartOutbound(HciPacketType type, char* data) {
    ALOGD("%s sending", __func__);
    int data_length = strlen(data);
    h4_hci_->Send(type, (uint8_t*)data, data_length);

    int uart_length = data_length + 1;  // + 1 for data type code
    int i;

    ALOGD("%s reading", __func__);
    for (i = 0; i < uart_length; i++) {
      fd_set read_fds;
      FD_ZERO(&read_fds);
      FD_SET(chip_uart_fd_, &read_fds);
      TEMP_FAILURE_RETRY(
          select(chip_uart_fd_ + 1, &read_fds, nullptr, nullptr, nullptr));

      char byte;
      TEMP_FAILURE_RETRY(read(chip_uart_fd_, &byte, 1));

      EXPECT_EQ(i == 0 ? static_cast<uint8_t>(type) : data[i - 1], byte);
    }

    EXPECT_EQ(i, uart_length);
  }

  void ExpectInboundAclData(char* payload) {
    // h4 type[1] + handle[2] + size[2]
    header[0] = static_cast<uint8_t>(HCI_PACKET_TYPE_ACL_DATA);
    header[1] = 19;
    header[2] = 92;
    int length = strlen(payload);
    header[3] = length & 0xFF;
    header[4] = (length >> 8) & 0xFF;
    ALOGD("(%d bytes) %s", length, payload);

    EXPECT_CALL(acl_cb_,
                Call(PacketMatches(header + 1, HCI_ACL_PREAMBLE_SIZE, payload)))
        .WillOnce(Notify(&mutex_, &done_));
  }

  void WriteInboundAclData(char* payload) {
    // Use the header computed in ExpectInboundAclData
    TEMP_FAILURE_RETRY(write(chip_uart_fd_, header, HCI_ACL_PREAMBLE_SIZE + 1));
    TEMP_FAILURE_RETRY(write(chip_uart_fd_, payload, strlen(payload)));
  }

  void ExpectInboundScoData(char* payload) {
    // h4 type[1] + handle[2] + size[1]
    header[0] = static_cast<uint8_t>(HCI_PACKET_TYPE_SCO_DATA);
    header[1] = 20;
    header[2] = 17;
    header[3] = strlen(payload) & 0xFF;
    EXPECT_CALL(sco_cb_,
                Call(PacketMatches(header + 1, HCI_SCO_PREAMBLE_SIZE, payload)))
        .WillOnce(Notify(&mutex_, &done_));
  }

  void WriteInboundScoData(char* payload) {
    // Use the header computed in ExpectInboundScoData
    ALOGD("%s writing", __func__);
    TEMP_FAILURE_RETRY(write(chip_uart_fd_, header, HCI_SCO_PREAMBLE_SIZE + 1));
    TEMP_FAILURE_RETRY(write(chip_uart_fd_, payload, strlen(payload)));
  }

  void ExpectInboundEvent(char* payload) {
    // h4 type[1] + event_code[1] + size[1]
    header[0] = static_cast<uint8_t>(HCI_PACKET_TYPE_EVENT);
    header[1] = 9;
    header[2] = strlen(payload) & 0xFF;
    EXPECT_CALL(event_cb_, Call(PacketMatches(
                               header + 1, HCI_EVENT_PREAMBLE_SIZE, payload)))
        .WillOnce(Notify(&mutex_, &done_));
  }

  void WriteInboundEvent(char* payload) {
    // Use the header computed in ExpectInboundEvent
    char preamble[3] = {static_cast<uint8_t>(HCI_PACKET_TYPE_EVENT), 9, 0};
    preamble[2] = strlen(payload) & 0xFF;
    ALOGD("%s writing", __func__);
    TEMP_FAILURE_RETRY(
        write(chip_uart_fd_, header, HCI_EVENT_PREAMBLE_SIZE + 1));
    TEMP_FAILURE_RETRY(write(chip_uart_fd_, payload, strlen(payload)));
  }

  void ExpectInboundIsoData(char* payload) {
    // h4 type[1] + handle[2] + size[1]
    header[0] = static_cast<uint8_t>(HCI_PACKET_TYPE_ISO_DATA);
    header[1] = 19;
    header[2] = 92;
    int length = strlen(payload);
    header[3] = length & 0xFF;
    header[4] = (length >> 8) & 0x3F;

    EXPECT_CALL(iso_cb_,
                Call(PacketMatches(header + 1, HCI_ISO_PREAMBLE_SIZE, payload)))
        .WillOnce(Notify(&mutex_, &done_));
  }

  void WriteInboundIsoData(char* payload) {
    // Use the header computed in ExpectInboundIsoData
    ALOGD("%s writing", __func__);
    TEMP_FAILURE_RETRY(write(chip_uart_fd_, header, HCI_ISO_PREAMBLE_SIZE + 1));
    TEMP_FAILURE_RETRY(write(chip_uart_fd_, payload, strlen(payload)));
  }

  void WriteAndExpectManyInboundAclDataPackets(char* payload) {
    size_t kNumPackets = 20;
    // h4 type[1] + handle[2] + size[2]
    char preamble[5] = {static_cast<uint8_t>(HCI_PACKET_TYPE_ACL_DATA), 19, 92,
                        0, 0};
    int length = strlen(payload);
    preamble[3] = length & 0xFF;
    preamble[4] = (length >> 8) & 0xFF;

    EXPECT_CALL(acl_cb_, Call(PacketMatches(preamble + 1, sizeof(preamble) - 1,
                                            payload)))
        .Times(kNumPackets);

    for (size_t i = 0; i < kNumPackets; i++) {
      TEMP_FAILURE_RETRY(write(chip_uart_fd_, preamble, sizeof(preamble)));
      TEMP_FAILURE_RETRY(write(chip_uart_fd_, payload, strlen(payload)));
    }

    ExpectInboundEvent(event_data);
    WriteInboundEvent(event_data);
    CallDataReady();
  }

  testing::MockFunction<void(const std::vector<uint8_t>&)> cmd_cb_;
  testing::MockFunction<void(const std::vector<uint8_t>&)> event_cb_;
  testing::MockFunction<void(const std::vector<uint8_t>&)> acl_cb_;
  testing::MockFunction<void(const std::vector<uint8_t>&)> sco_cb_;
  testing::MockFunction<void(const std::vector<uint8_t>&)> iso_cb_;
  testing::MockFunction<void(void)> disconnect_cb_;
  std::shared_ptr<H4Protocol> h4_hci_;
  int chip_uart_fd_;
  int stack_uart_fd_;

  char header[5];
  std::mutex mutex_;
  std::condition_variable done_;
};

// Test sending data sends correct data onto the UART
TEST_F(H4ProtocolTest, TestSends) {
  SendAndReadUartOutbound(HCI_PACKET_TYPE_COMMAND, sample_data1);
  SendAndReadUartOutbound(HCI_PACKET_TYPE_ACL_DATA, sample_data2);
  SendAndReadUartOutbound(HCI_PACKET_TYPE_SCO_DATA, sample_data3);
  SendAndReadUartOutbound(HCI_PACKET_TYPE_ISO_DATA, sample_data4);
}

// Ensure we properly parse data coming from the UART
TEST_F(H4ProtocolTest, TestReads) {
  ExpectInboundAclData(acl_data);
  WriteInboundAclData(acl_data);
  CallDataReady();
  ExpectInboundScoData(sco_data);
  WriteInboundScoData(sco_data);
  CallDataReady();
  ExpectInboundEvent(event_data);
  WriteInboundEvent(event_data);
  CallDataReady();
  ExpectInboundIsoData(iso_data);
  WriteInboundIsoData(iso_data);
  CallDataReady();
}

TEST_F(H4ProtocolTest, TestMultiplePackets) {
  WriteAndExpectManyInboundAclDataPackets(sco_data);
}

TEST_F(H4ProtocolTest, TestDisconnect) {
  std::mutex mutex;
  std::condition_variable done;
  EXPECT_CALL(disconnect_cb_, Call()).WillOnce(Notify(&mutex_, &done_));
  close(chip_uart_fd_);
  CallDataReady();
}

TEST_F(H4ProtocolTest, TestPartialWrites) {
  size_t payload_len = strlen(acl_data);
  const size_t kNumIntervals = payload_len + 1;
  // h4 type[1] + handle[2] + size[2]
  header[0] = static_cast<uint8_t>(HCI_PACKET_TYPE_ACL_DATA);
  header[1] = 19;
  header[2] = 92;
  header[3] = payload_len & 0xFF;
  header[4] = (payload_len >> 8) & 0xFF;

  EXPECT_CALL(acl_cb_,
              Call(PacketMatches(header + 1, sizeof(header) - 1, acl_data)))
      .Times(kNumIntervals);

  for (size_t interval = 1; interval < kNumIntervals + 1; interval++) {
    // Use the header data that expect already set up.
    if (interval < HCI_ACL_PREAMBLE_SIZE) {
      TEMP_FAILURE_RETRY(write(chip_uart_fd_, header, interval));
      CallDataReady();
      TEMP_FAILURE_RETRY(write(chip_uart_fd_, header + interval,
                               HCI_ACL_PREAMBLE_SIZE + 1 - interval));
      CallDataReady();
    } else {
      TEMP_FAILURE_RETRY(
          write(chip_uart_fd_, header, HCI_ACL_PREAMBLE_SIZE + 1));
      CallDataReady();
    }

    for (size_t bytes = 0; bytes + interval <= payload_len; bytes += interval) {
      TEMP_FAILURE_RETRY(write(chip_uart_fd_, acl_data + bytes, interval));
      CallDataReady();
    }
    size_t extra_bytes = payload_len % interval;
    if (extra_bytes) {
      TEMP_FAILURE_RETRY(write(
          chip_uart_fd_, acl_data + payload_len - extra_bytes, extra_bytes));
      CallDataReady();
    }
  }
}

class H4ProtocolAsyncTest : public H4ProtocolTest {
 protected:
  void SetUp() override {
    H4ProtocolTest::SetUp();
    fd_watcher_.WatchFdForNonBlockingReads(
        stack_uart_fd_, [this](int fd) { h4_hci_->OnDataReady(fd); });
  }

  void TearDown() override { fd_watcher_.StopWatchingFileDescriptors(); }

  void CallDataReady() override {
    // The Async test can't call data ready.
    FAIL();
  }

  void SendAndReadUartOutbound(HciPacketType type, char* data) {
    ALOGD("%s sending", __func__);
    int data_length = strlen(data);
    h4_hci_->Send(type, (uint8_t*)data, data_length);

    int uart_length = data_length + 1;  // + 1 for data type code
    int i;

    ALOGD("%s reading", __func__);
    for (i = 0; i < uart_length; i++) {
      fd_set read_fds;
      FD_ZERO(&read_fds);
      FD_SET(chip_uart_fd_, &read_fds);
      TEMP_FAILURE_RETRY(
          select(chip_uart_fd_ + 1, &read_fds, nullptr, nullptr, nullptr));

      char byte;
      TEMP_FAILURE_RETRY(read(chip_uart_fd_, &byte, 1));

      EXPECT_EQ(i == 0 ? static_cast<uint8_t>(type) : data[i - 1], byte);
    }

    EXPECT_EQ(i, uart_length);
  }

  void WaitForTimeout(size_t timeout_ms) {
    // Fail if it takes longer than timeout_ms.
    auto timeout_time = std::chrono::steady_clock::now() +
                        std::chrono::milliseconds(timeout_ms);
    {
      std::unique_lock<std::mutex> lock(mutex_);
      done_.wait_until(lock, timeout_time);
    }
  }

  void WriteAndExpectInboundAclData(char* payload) {
    ExpectInboundAclData(payload);
    WriteInboundAclData(payload);
    WaitForTimeout(100);
  }

  void WriteAndExpectInboundScoData(char* payload) {
    ExpectInboundScoData(payload);
    WriteInboundScoData(payload);
    WaitForTimeout(100);
  }

  void WriteAndExpectInboundEvent(char* payload) {
    ExpectInboundEvent(payload);
    WriteInboundEvent(payload);
    WaitForTimeout(100);
  }

  void WriteAndExpectInboundIsoData(char* payload) {
    ExpectInboundIsoData(payload);
    WriteInboundIsoData(payload);
    WaitForTimeout(100);
  }

  void WriteAndExpectManyInboundAclDataPackets(char* payload) {
    const size_t kNumPackets = 20;
    // h4 type[1] + handle[2] + size[2]
    char preamble[5] = {static_cast<uint8_t>(HCI_PACKET_TYPE_ACL_DATA), 19, 92,
                        0, 0};
    int length = strlen(payload);
    preamble[3] = length & 0xFF;
    preamble[4] = (length >> 8) & 0xFF;

    EXPECT_CALL(acl_cb_, Call(PacketMatches(preamble + 1, sizeof(preamble) - 1,
                                            payload)))
        .Times(kNumPackets);

    for (size_t i = 0; i < kNumPackets; i++) {
      TEMP_FAILURE_RETRY(write(chip_uart_fd_, preamble, sizeof(preamble)));
      TEMP_FAILURE_RETRY(write(chip_uart_fd_, payload, strlen(payload)));
    }

    WriteAndExpectInboundEvent(event_data);
  }

  AsyncFdWatcher fd_watcher_;
};

// Test sending data sends correct data onto the UART
TEST_F(H4ProtocolAsyncTest, TestSends) {
  SendAndReadUartOutbound(HCI_PACKET_TYPE_COMMAND, sample_data1);
  SendAndReadUartOutbound(HCI_PACKET_TYPE_ACL_DATA, sample_data2);
  SendAndReadUartOutbound(HCI_PACKET_TYPE_SCO_DATA, sample_data3);
  SendAndReadUartOutbound(HCI_PACKET_TYPE_ISO_DATA, sample_data4);
}

// Ensure we properly parse data coming from the UART
TEST_F(H4ProtocolAsyncTest, TestReads) {
  WriteAndExpectInboundAclData(acl_data);
  WriteAndExpectInboundScoData(sco_data);
  WriteAndExpectInboundEvent(event_data);
  WriteAndExpectInboundIsoData(iso_data);
}

TEST_F(H4ProtocolAsyncTest, TestMultiplePackets) {
  WriteAndExpectManyInboundAclDataPackets(sco_data);
}

TEST_F(H4ProtocolAsyncTest, TestDisconnect) {
  std::mutex mutex;
  std::condition_variable done;
  EXPECT_CALL(disconnect_cb_, Call()).WillOnce(Notify(&mutex_, &done_));
  close(chip_uart_fd_);

  // Fail if it takes longer than 100 ms.
  WaitForTimeout(100);
  auto timeout_time =
      std::chrono::steady_clock::now() + std::chrono::milliseconds(100);
  {
    std::unique_lock<std::mutex> lock(mutex_);
    done.wait_until(lock, timeout_time);
  }
}
