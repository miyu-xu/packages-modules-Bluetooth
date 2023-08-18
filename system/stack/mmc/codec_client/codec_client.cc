/*
 * Copyright 2023 The Android Open Source Project
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

#include "mmc/codec_client/codec_client.h"

#include <dbus/bus.h>
#include <dbus/message.h>
#include <dbus/object_proxy.h>
#include <errno.h>
#include <poll.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>

#include "mmc/daemon/constants.h"
#include "mmc/proto/mmc_config.pb.h"
#include "mmc/proto/mmc_service.pb.h"
#include "osi/include/log.h"

namespace mmc {

CodecClient::CodecClient() {
  skt_fd_ = -1;
  codec_manager_ = nullptr;

  // Set up DBus connection.
  dbus::Bus::Options options;
  options.bus_type = dbus::Bus::SYSTEM;
  bus_ = new dbus::Bus(options);

  if (!bus_->Connect()) {
    LOG_ERROR("%s: Failed to connect system bus.", __func__);
  }

  // Get proxy to send DBus method call.
  codec_manager_ = bus_->GetObjectProxy(mmc::kMmcServiceName,
                                        dbus::ObjectPath(mmc::kMmcServicePath));
  if (!codec_manager_) {
    LOG_ERROR("%s: Failed to get object proxy.", __func__);
  }
}

CodecClient::~CodecClient() { cleanup(); }

int CodecClient::init(const ConfigParam config) {
  dbus::MethodCall method_call(mmc::kMmcServiceInterface,
                               mmc::kCodecInitMethod);
  dbus::MessageWriter writer(&method_call);

  mmc::CodecInitRequest request;
  *request.mutable_config() = config;
  if (!writer.AppendProtoAsArrayOfBytes(request)) {
    LOG_ERROR("%s: Failed to encode CodecInitRequest protobuf.", __func__);
    return -1;
  }

  std::unique_ptr<dbus::Response> dbus_response =
      codec_manager_
          ->CallMethodAndBlock(&method_call,
                               dbus::ObjectProxy::TIMEOUT_USE_DEFAULT)
          .value_or(nullptr);
  if (!dbus_response) {
    LOG_ERROR("%s: Failed to send dbus message to mmc service.", __func__);
    return -1;
  }

  dbus::MessageReader reader(dbus_response.get());
  mmc::CodecInitResponse response;
  if (!reader.PopArrayOfBytesAsProto(&response)) {
    LOG_ERROR("%s: Failed to parse response protobuf.", __func__);
    return -1;
  }
  if (response.socket_token().empty()) {
    LOG_ERROR("%s: Failed to init codec.", __func__);
    return -1;
  }

  // Create socket.
  int rc;
  struct sockaddr_un addr;

  skt_fd_ = socket(AF_UNIX, SOCK_SEQPACKET, 0);
  if (skt_fd_ < 0) {
    LOG_ERROR("%s: Failed to create socket, %s.", __func__, strerror(errno));
    return -errno;
  }

  memset(&addr, 0, sizeof(struct sockaddr_un));
  addr.sun_family = AF_UNIX;
  strncpy(addr.sun_path, response.socket_token().c_str(),
          sizeof(addr.sun_path) - 1);

  // Connect to socket for transcoding.
  rc = connect(skt_fd_, (struct sockaddr*)&addr, sizeof(struct sockaddr_un));
  if (rc < 0) {
    LOG_ERROR("%s: Failed to connect socket, %s.", __func__, strerror(errno));
    return -errno;
  }
  return 0;
}

void CodecClient::cleanup() {
  close(skt_fd_);
  dbus::MethodCall method_call(mmc::kMmcServiceInterface,
                               mmc::kCodecCleanUpMethod);

  std::unique_ptr<dbus::Response> dbus_response =
      codec_manager_
          ->CallMethodAndBlock(&method_call,
                               dbus::ObjectProxy::TIMEOUT_USE_DEFAULT)
          .value_or(nullptr);
  if (!dbus_response) {
    LOG_ERROR("%s: Failed to send dbus message to mmc service.", __func__);
  }
  return;
}

int CodecClient::transcode(uint8_t* i_buf, int i_len, uint8_t* o_buf,
                           int o_len) {
  // i_buf and o_buf cannot be null.
  ASSERT(i_buf != nullptr && o_buf != nullptr);

  // Use MSG_NOSIGNAL to ignore SIGPIPE
  int rc = send(skt_fd_, i_buf, i_len, MSG_NOSIGNAL);

  // A packet should be sent at once.
  if (rc <= 0 || rc < i_len) {
    LOG_ERROR("%s: Failed to send data, %s.", __func__, strerror(errno));
    return -errno;
  }

  struct pollfd pfd;
  pfd.fd = skt_fd_;
  pfd.events = POLLIN;

  int pollret = poll(&pfd, 1, -1);
  if (pollret <= 0) {
    LOG_ERROR("%s: Failed to poll, %s.", __func__, strerror(errno));
    return -errno;
  }

  if (pfd.revents & (POLLHUP | POLLNVAL)) {
    LOG_ERROR("%s: Socket closed, %s.", __func__, strerror(errno));
    return -errno;
  }

  // POLLIN
  rc = recv(skt_fd_, o_buf, o_len, MSG_NOSIGNAL);
  LOG_INFO("%s: Received %d bytes", __func__, rc);
  if (rc <= 0) {
    LOG_ERROR("%s: Failed to recv data, %s.", __func__, strerror(errno));
    return -errno;
  }
  return 0;
}

}  // namespace mmc
