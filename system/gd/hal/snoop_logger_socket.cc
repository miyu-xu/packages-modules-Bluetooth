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

SnoopLoggerSocket::SnoopLoggerSocket(SocketInterface* socket, int socket_address, int socket_port) {
  socket_ = socket;

  LOG_INFO("address %d port %d", socket_address, socket_port);
  socket_address_ = socket_address;
  socket_port_ = socket_port;

  notification_listen_fd = -1;
  notification_write_fd = -1;
  client_socket_ = -1;
  listen_socket_ = -1;
}

SnoopLoggerSocket::~SnoopLoggerSocket() {
  Cleanup();
}

void SnoopLoggerSocket::Write(int& client_socket, const void* data, size_t length) {
  if (client_socket == -1) {
    return;
  }

  ssize_t ret;
  RUN_NO_INTR(ret = socket_->Send(client_socket, data, length, MSG_DONTWAIT));

  if (ret == -1 && socket_->GetErrno() == ECONNRESET) {
    SafeCloseSocket(client_socket);
  } else if (ret == -1 && socket_->GetErrno() == EAGAIN) {
    LOG_ERROR("Dropping snoop pkts because of congestion");
  }
}

void SnoopLoggerSocket::Write(const void* data, size_t length) {
  std::lock_guard<std::mutex> lock(client_socket_mutex_);
  Write(client_socket_, data, length);
}

int SnoopLoggerSocket::InitializeCommunications() {
  int self_pipe_fds[2];
  int ret;

  socket_->FDZero(&save_sock_fds_);

  // Set up the communication channel
  ret = socket_->Pipe2(self_pipe_fds, O_NONBLOCK | O_CLOEXEC);
  if (ret < 0) {
    LOG_ERROR("Unable to establish a communication channel to the listen thread.");
    return ret;
  }

  notification_listen_fd = self_pipe_fds[0];
  notification_write_fd = self_pipe_fds[1];

  socket_->FDSet(notification_listen_fd, &save_sock_fds_);
  fd_max_ = notification_listen_fd;

  listen_socket_ = CreateSocket();
  if (listen_socket_ == INVALID_FD) {
    LOG_ERROR("Unable to create a listen socket.");
    SafeCloseSocket(notification_listen_fd);
    SafeCloseSocket(notification_write_fd);
    return -1;
  }

  return 0;
}

bool SnoopLoggerSocket::ProcessIncomingRequest() {
  int ret;
  LOG_DEBUG("Selecting socket to read from");
  fd_set sock_fds = save_sock_fds_;
  if ((socket_->Select(fd_max_ + 1, &sock_fds, NULL, NULL, NULL)) == -1) {
    LOG_ERROR("%s select failed %s", __func__, strerror(socket_->GetErrno()));
    if (socket_->GetErrno() == EINTR) return true;
    return false;
  }

  if ((listen_socket_ != -1) && socket_->FDIsSet(listen_socket_, &sock_fds)) {
    int client_socket = -1;
    ret = AcceptIncomingConnection(listen_socket_, client_socket);
    if (ret != 0) {
      // Unrecoverable error, stop the thread.
      return false;
    }

    if (client_socket < 0) {
      return true;
    }

    InitializeClientSocket(client_socket);

    ClientSocketConnected(client_socket);
  } else if ((notification_listen_fd != -1) && socket_->FDIsSet(notification_listen_fd, &sock_fds)) {
    LOG_WARN("exting from listen_fn_ thread ");
    return false;
  }

  return true;
}

void SnoopLoggerSocket::Cleanup() {
  SafeCloseSocket(notification_listen_fd);
  SafeCloseSocket(notification_write_fd);
  SafeCloseSocket(client_socket_);
  SafeCloseSocket(listen_socket_);
}

int SnoopLoggerSocket::AcceptIncomingConnection(int listen_socket, int& client_socket) {
  socklen_t clen;
  struct sockaddr_in client_addr;

  RUN_NO_INTR(client_socket = socket_->Accept(listen_socket, (struct sockaddr*)&client_addr, &clen, SOCK_CLOEXEC));
  if (client_socket == -1) {
    int errno_ = socket_->GetErrno();
    LOG_WARN("error accepting socket: %s", strerror(errno_));
    if (errno_ == EINVAL || errno_ == EBADF) {
      return errno_;
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

void SnoopLoggerSocket::InitializeClientSocket(int client_socket) {
  /* When a new client connects, we have to send the btsnoop file header. This
   * allows a decoder to treat the session as a new, valid btsnoop file. */
  Write(
      client_socket,
      reinterpret_cast<const char*>(&SnoopLoggerCommon::kBtSnoopFileHeader),
      sizeof(SnoopLoggerCommon::FileHeaderType));
}

void SnoopLoggerSocket::ClientSocketConnected(int client_socket) {
  std::lock_guard<std::mutex> lock(client_socket_mutex_);
  SafeCloseSocket(client_socket_);
  client_socket_ = client_socket;
  client_socket_cv_.notify_one();
}

bool SnoopLoggerSocket::WaitForClientSocketConnected() {
  std::unique_lock<std::mutex> lk(client_socket_mutex_);
  client_socket_cv_.wait(lk, [this] { return IsClientSocketConnected(); });
  return IsClientSocketConnected();
}

bool SnoopLoggerSocket::IsClientSocketConnected() const {
  return client_socket_ != INVALID_FD;
}

int SnoopLoggerSocket::CreateSocket() {
  LOG_DEBUG("");
  int ret;

  // Create a TCP socket file descriptor
  int socket_fd = socket_->Socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, IPPROTO_TCP);
  if (socket_fd < 0) {
    LOG_ERROR("can't create socket: %s", strerror(socket_->GetErrno()));
    return INVALID_FD;
  }

  socket_->FDSet(socket_fd, &save_sock_fds_);
  if (socket_fd > fd_max_) {
    fd_max_ = socket_fd;
  }

  // Enable REUSEADDR
  int enable = 1;
  ret = socket_->Setsockopt(socket_fd, SOL_SOCKET, SO_REUSEADDR, &enable, sizeof(enable));
  if (ret < 0) {
    LOG_ERROR("unable to set SO_REUSEADDR: %s", strerror(socket_->GetErrno()));
    SafeCloseSocket(socket_fd);
    return INVALID_FD;
  }

  struct sockaddr_in addr;
  addr.sin_family = AF_INET;
  addr.sin_addr.s_addr = htonl(socket_address_);
  addr.sin_port = htons(socket_port_);

  // Bind socket to an address
  ret = socket_->Bind(socket_fd, (struct sockaddr*)&addr, sizeof(addr));
  if (ret < 0) {
    LOG_ERROR("unable to bind snoop socket to address: %s", strerror(socket_->GetErrno()));
    SafeCloseSocket(socket_fd);
    return INVALID_FD;
  }

  // Mark this socket as a socket that will accept connections.
  ret = socket_->Listen(socket_fd, INCOMING_SOCKET_CONNECTIONS_QUEUE_SIZE_);
  if (ret < 0) {
    LOG_ERROR("unable to listen: %s", strerror(socket_->GetErrno()));
    SafeCloseSocket(socket_fd);
    return INVALID_FD;
  }

  return socket_fd;
}

int SnoopLoggerSocket::NotifySocketListener() {
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

void SnoopLoggerSocket::SafeCloseSocket(int& fd) {
  LOG_DEBUG("%d", (fd));
  if (fd != -1) {
    socket_->Close(fd);
    fd = -1;
  }
}

SocketInterface* SnoopLoggerSocket::GetSocketInterface() const {
  return socket_;
}

}  // namespace hal
}  // namespace bluetooth
