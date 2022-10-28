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
constexpr struct timeval SOCKET_TIMEOUT_ = {
    .tv_sec = 0,
    .tv_usec = 5000,
};

static int SetClientSocketSendTimeout(int client_socket_fd, const struct timeval& socket_timeout) {
  LOG_DEBUG("client_socket_fd %d sec %ld usec %ld", client_socket_fd, socket_timeout.tv_sec, socket_timeout.tv_usec);
  int ret = setsockopt(client_socket_fd, SOL_SOCKET, SO_SNDTIMEO, &socket_timeout, sizeof(socket_timeout));
  if (ret < 0) {
    LOG_WARN("fail to set client socket send timeout option %s", strerror(errno));
    close(client_socket_fd);
    return ret;
  }

  return 0;
}

static void SafeCloseSocket(int* fd) {
  CHECK(fd != NULL);
  LOG_DEBUG("%d", (*fd));
  if (fd != NULL && *fd != -1) {
    close(*fd);
    *fd = -1;
  }
}

SnoopLoggerSocket::SnoopLoggerSocket() {
  socket_address_ = DEFAULT_LOCALHOST_;
  socket_port_ = DEFAULT_LISTEN_PORT_;
  LOG_INFO("address %d port %d", socket_address_, socket_port_);
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
  listen_thread_running_ = true;

  // Trigger a context switch so that the Socket thread can start listening for connections.
  std::chrono::milliseconds timespan(1);
  std::this_thread::sleep_for(timespan);
}

void SnoopLoggerSocket::Stop() {
  LOG_DEBUG("");
  if (listen_thread_running_) {
    NotifySocketListenerThread();
    listen_thread_->join();
    listen_thread_.reset();

    SafeCloseSocket(&notification_listen_fd);
    SafeCloseSocket(&notification_write_fd);
    SafeCloseSocket(&client_socket_);
    SafeCloseSocket(&listen_socket_);

    listen_thread_running_ = false;
  }
}

void SnoopLoggerSocket::Write(const void* data, size_t length) {
  LOG_DEBUG("");
  std::lock_guard<std::mutex> lock(client_socket_mutex_);
  if (client_socket_ == -1) {
    LOG_DEBUG("no client socket");
    return;
  }

  ssize_t ret;
  RUN_NO_INTR(ret = send(client_socket_, data, length, 0));

  if (ret == -1 && errno == ECONNRESET) {
    SafeCloseSocket(&client_socket_);
  } else if (ret == -1 && errno == EAGAIN) {
    LOG_ERROR("%s Dropping snoop pkts because of congestion", __func__);
  }
}

bool SnoopLoggerSocket::ThreadIsRunning() const {
  return listen_thread_running_;
}

void SnoopLoggerSocket::Run() {
  LOG_DEBUG("");
  fd_set sock_fds;
  int self_pipe_fds[2];
  int ret;

  FD_ZERO(&sock_fds);
  FD_ZERO(&save_sock_fds_);

  // Set up the communication channel
  if (pipe2(self_pipe_fds, O_NONBLOCK)) {
    LOG_ERROR("%s:Unable to establish a communication channel to the listen thread ", __func__);
    return;
  }

  notification_listen_fd = self_pipe_fds[0];
  notification_write_fd = self_pipe_fds[1];

  FD_SET(notification_listen_fd, &save_sock_fds_);
  fd_max_ = notification_listen_fd;

  listen_socket_ = CreateSocket();

  while (true) {
    int client_socket = -1;

    LOG_DEBUG("Selecting socket to read from");
    sock_fds = save_sock_fds_;
    if ((select(fd_max_ + 1, &sock_fds, NULL, NULL, NULL)) == -1) {
      LOG_ERROR("%s select failed %s", __func__, strerror(errno));
      if (errno == EINTR) continue;
      goto cleanup;
    }

    if ((listen_socket_ != -1) && FD_ISSET(listen_socket_, &sock_fds)) {
      socklen_t clen;
      struct sockaddr_in client_addr;

      RUN_NO_INTR(client_socket = accept(listen_socket_, (struct sockaddr*)&client_addr, &clen));
      if (client_socket == -1) {
        if (errno == EINVAL || errno == EBADF) {
          break;
        }
        LOG_WARN("error accepting socket: %s", strerror(errno));
        continue;
      }

      LOG_INFO("Client socket fd: %d", client_socket);
      LOG_INFO("IP address is: %s\n", inet_ntoa(client_addr.sin_addr));
      LOG_INFO("port is: %d\n", (int)ntohs(client_addr.sin_port));
    } else if ((notification_listen_fd != -1) && FD_ISSET(notification_listen_fd, &sock_fds)) {
      LOG_WARN("exting from listen_fn_ thread ");
      return;
    }

    if (client_socket == -1) {
      continue;
    }

    ret = SetClientSocketSendTimeout(client_socket, SOCKET_TIMEOUT_);
    if (ret < 0) {
      continue;
    }

    /* When a new client connects, we have to send the btsnoop file header. This
     * allows a decoder to treat the session as a new, valid btsnoop file. */
    std::lock_guard<std::mutex> lock(client_socket_mutex_);
    SafeCloseSocket(&client_socket_);
    client_socket_ = client_socket;

    RUN_NO_INTR(send(
        client_socket_,
        reinterpret_cast<const char*>(&SnoopLoggerCommon::kBtSnoopFileHeader),
        sizeof(SnoopLoggerCommon::FileHeaderType),
        0));
  }

cleanup:
  SafeCloseSocket(&listen_socket_);
  return;
}

int SnoopLoggerSocket::CreateSocket() {
  LOG_DEBUG("");
  int ret;

  // Create a TCP socket file descriptor
  int socket_fd = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
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

  RUN_NO_INTR(ret = write(notification_write_fd, &buffer, 1));
  if (ret < 0) {
    LOG_ERROR("Error in notifying the listen thread to exit (%d)", ret);
    return -1;
  }

  return 0;
}

}  // namespace hal
}  // namespace bluetooth
