/******************************************************************************
 *
 *  Copyright (C) 2022 Google, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

#include "hal/snoop_logger_socket.h"

#include <arpa/inet.h>
#include <base/logging.h>
#include <errno.h>
#include <fcntl.h>
#include <netinet/in.h>
#include <pthread.h>
#include <stdbool.h>
#include <stdio.h>
#include <string.h>
#include <sys/prctl.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/un.h>
#include <unistd.h>

#include <mutex>

#include "common/init_flags.h"
#include "hal/snoop_logger_common.h"
#include "os/handler.h"
#include "os/log.h"
#include "os/thread.h"
#include "os/utils.h"

namespace bluetooth {
namespace hal {

using bluetooth::hal::SnoopLoggerCommon;

static constexpr int INVALID_FD = -1;

constexpr int INCOMING_SOCKET_CONNECTIONS_QUEUE_SIZE_ = 10;

static void SafeCloseSocket(int& fd) {
  LOG_DEBUG("%d", (fd));
  if (fd != -1) {
    close(fd);
    fd = -1;
  }
}

SnoopLoggerSocket::SnoopLoggerSocket(int socket_address, int socket_port) {
  LOG_INFO("address %d port %d", socket_address, socket_port);
  socket_address_ = socket_address;
  socket_port_ = socket_port;
}

SnoopLoggerSocket::~SnoopLoggerSocket() {
  Stop();
}

void SnoopLoggerSocket::Start() {
  LOG_DEBUG("");
  listen_thread_ = std::make_unique<std::thread>(&SnoopLoggerSocket::Run, this);
  WaitThreadIsRunning();
}

void SnoopLoggerSocket::Stop() {
  LOG_DEBUG("");
  if (listen_thread_running_) {
    NotifySocketListenerThread();
    listen_thread_->join();
    listen_thread_.reset();

    listen_thread_running_ = false;
  }
}

void SnoopLoggerSocket::Write(int& client_socket, const void* data, size_t length) {
  if (client_socket == -1) {
    return;
  }

  ssize_t ret;
  RUN_NO_INTR(ret = send(client_socket, data, length, MSG_DONTWAIT));

  if (ret == -1 && errno == ECONNRESET) {
    SafeCloseSocket(client_socket);
  } else if (ret == -1 && errno == EAGAIN) {
    LOG_ERROR("Dropping snoop pkts because of congestion");
  }
}

void SnoopLoggerSocket::Write(const void* data, size_t length) {
  std::lock_guard<std::mutex> lock(client_socket_mutex_);
  Write(client_socket_, data, length);
}

bool SnoopLoggerSocket::ThreadIsRunning() const {
  return listen_thread_running_;
}

bool SnoopLoggerSocket::WaitThreadIsRunning() {
  std::unique_lock<std::mutex> lk(listen_thread_running_mutex_);
  listen_thread_running_cv_.wait(lk, [this] { return listen_thread_running_; });
  return listen_thread_running_;
}

bool SnoopLoggerSocket::WaitForClientSocketConnected() {
  std::unique_lock<std::mutex> lk(client_socket_mutex_);
  client_socket_cv_.wait(lk, [this] { return client_socket_ != INVALID_FD; });
  return client_socket_ != INVALID_FD;
}

void SnoopLoggerSocket::Run() {
  LOG_DEBUG("");
  fd_set sock_fds;
  int self_pipe_fds[2];
  int ret;

  FD_ZERO(&sock_fds);
  FD_ZERO(&save_sock_fds_);

  // Set up the communication channel
  if (pipe2(self_pipe_fds, O_NONBLOCK | O_CLOEXEC)) {
    LOG_ERROR("Unable to establish a communication channel to the listen thread.");
    listen_thread_running_ = false;
    return;
  }

  notification_listen_fd = self_pipe_fds[0];
  notification_write_fd = self_pipe_fds[1];

  FD_SET(notification_listen_fd, &save_sock_fds_);
  fd_max_ = notification_listen_fd;

  listen_socket_ = CreateSocket();
  if (listen_socket_ == -1) {
    LOG_ERROR("Unable to create a listen socket.");
    SafeCloseSocket(notification_listen_fd);
    SafeCloseSocket(notification_write_fd);
    listen_thread_running_ = false;
    return;
  }

  {
    std::lock_guard<std::mutex> lk(listen_thread_running_mutex_);
    listen_thread_running_ = true;
    listen_thread_running_cv_.notify_one();
  }

  while (true) {
    LOG_DEBUG("Selecting socket to read from");
    sock_fds = save_sock_fds_;
    if ((select(fd_max_ + 1, &sock_fds, NULL, NULL, NULL)) == -1) {
      LOG_ERROR("%s select failed %s", __func__, strerror(errno));
      if (errno == EINTR) continue;
      break;
    }

    if ((listen_socket_ != -1) && FD_ISSET(listen_socket_, &sock_fds)) {
      int client_socket = -1;
      ret = AcceptIncomingConnection(listen_socket_, client_socket);
      if (ret != 0) {
        // Unrecoverable error, stop the thread.
        break;
      }

      if (client_socket < 0) {
        continue;
      }

      ret = InitializeClientSocket(client_socket);
      if (ret < 0) {
        continue;
      }

      ClientSocketConnected(client_socket);
    } else if ((notification_listen_fd != -1) && FD_ISSET(notification_listen_fd, &sock_fds)) {
      LOG_WARN("exting from listen_fn_ thread ");
      break;
    }
  }

  SafeCloseSocket(notification_listen_fd);
  SafeCloseSocket(notification_write_fd);
  SafeCloseSocket(client_socket_);
  SafeCloseSocket(listen_socket_);
  listen_thread_running_ = false;
}

int SnoopLoggerSocket::AcceptIncomingConnection(int listen_socket, int& client_socket) {
  socklen_t clen;
  struct sockaddr_in client_addr;

  RUN_NO_INTR(client_socket = accept4(listen_socket, (struct sockaddr*)&client_addr, &clen, SOCK_CLOEXEC));
  if (client_socket == -1) {
    LOG_WARN("error accepting socket: %s", strerror(errno));
    if (errno == EINVAL || errno == EBADF) {
      return errno;
    }
    return 0;
  }

  LOG_INFO(
      "Client socket fd: %d, IP address: %s, port: %d",
      client_socket,
      inet_ntoa(client_addr.sin_addr),
      (int)ntohs(client_addr.sin_port));

  return 0;
}

int SnoopLoggerSocket::InitializeClientSocket(int client_socket) {
  /* When a new client connects, we have to send the btsnoop file header. This
   * allows a decoder to treat the session as a new, valid btsnoop file. */
  Write(
      client_socket,
      reinterpret_cast<const char*>(&SnoopLoggerCommon::kBtSnoopFileHeader),
      sizeof(SnoopLoggerCommon::FileHeaderType));
  return client_socket;
}

void SnoopLoggerSocket::ClientSocketConnected(int client_socket) {
  std::lock_guard<std::mutex> lock(client_socket_mutex_);
  SafeCloseSocket(client_socket_);
  client_socket_ = client_socket;
  client_socket_cv_.notify_one();
}

int SnoopLoggerSocket::CreateSocket() {
  LOG_DEBUG("");
  int ret;

  // Create a TCP socket file descriptor
  int socket_fd = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, IPPROTO_TCP);
  if (socket_fd < 0) {
    LOG_ERROR("can't create socket: %s", strerror(errno));
    return INVALID_FD;
  }

  FD_SET(socket_fd, &save_sock_fds_);
  if (socket_fd > fd_max_) {
    fd_max_ = socket_fd;
  }

  // Enable REUSEADDR
  int enable = 1;
  ret = setsockopt(socket_fd, SOL_SOCKET, SO_REUSEADDR, &enable, sizeof(enable));
  if (ret < 0) {
    LOG_ERROR("unable to set SO_REUSEADDR: %s", strerror(errno));
    SafeCloseSocket(socket_fd);
    return INVALID_FD;
  }

  struct sockaddr_in addr;
  addr.sin_family = AF_INET;
  addr.sin_addr.s_addr = htonl(socket_address_);
  addr.sin_port = htons(socket_port_);

  // Bind socket to an address
  ret = bind(socket_fd, (struct sockaddr*)&addr, sizeof(addr));
  if (ret < 0) {
    LOG_ERROR("unable to bind snoop socket to address: %s", strerror(errno));
    return INVALID_FD;
  }

  // Mark this socket as a socket that will accept connections.
  ret = listen(socket_fd, INCOMING_SOCKET_CONNECTIONS_QUEUE_SIZE_);
  if (ret < 0) {
    LOG_ERROR("unable to listen: %s", strerror(errno));
    return INVALID_FD;
  }

  return socket_fd;
}

int SnoopLoggerSocket::NotifySocketListenerThread() {
  LOG_DEBUG("");
  char buffer = '0';
  int ret = -1;

  if (notification_write_fd == -1) {
    return 0;
  }

  RUN_NO_INTR(ret = write(notification_write_fd, &buffer, 1));
  if (ret < 0) {
    LOG_ERROR("Error in notifying the listen thread to exit (%d)", ret);
    return -1;
  }

  return 0;
}

}  // namespace hal
}  // namespace bluetooth
