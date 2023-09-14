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

#ifndef MMC_SOCKET_WRAPPER_SOCKET_WRAPPER_INTERFACE_H_
#define MMC_SOCKET_WRAPPER_SOCKET_WRAPPER_INTERFACE_H_

#include <stdint.h>

#include <string>

namespace mmc {

// Wrapper of socket API
class SocketWrapperInterface {
 public:
  virtual ~SocketWrapperInterface() = default;

  // Create Unix Domain Socket with SOCK_SEQPACKET socket type.
  // Returns:
  //   0, if socket creation succeeded.
  //   Otherwise, a negative errno on error.
  virtual int CreateNamedSocket(const std::string& socket_name) = 0;

  // Bind and listen to the socket, allowing client connect to it.
  // This method must be called by server.
  // Returns:
  //   0, if bind and listen succeeded.
  //   Otherwise, a negative errno on error.
  virtual int BindAndListen() = 0;

  // Wrapper for <sys/socket.h> accept().
  // This method is separated from bind and listen because it would block until
  // the server receive a client connection request.
  // Must be called by server.
  // Returns:
  //   0, if accept succeeded.
  //   Otherwise, a negative errno on error.
  virtual int Accept() = 0;

  // Wrapper for <sys/socket.h> connect().
  // This method must be called by client after the server call BindAndListen().
  // Returns:
  //   0, if connection succeeded.
  //   Otherwise, a negative errno on error.
  virtual int Connect() = 0;

  // Send |len| bytes of data to the socket, and blocking until all data is
  // sent. Returns:
  //   len, if |len| bytes of data is sent.
  //   Otherwise, a negative errno on error.
  virtual int SendAndBlock(uint8_t* buf, int len) = 0;

  // Receive at most |len| bytes of data from the socket, and blocking until
  // there's readable data.
  // Returns:
  //   Received data length, if data is received.
  //   Otherwise, a negative errno on error.
  virtual int RecvAndBlock(uint8_t* buf, int len) = 0;

  // Close the socket and unlink the socket file.
  virtual void Disconnect() = 0;
};

}  // namespace mmc

#endif  // MMC_SOCKET_WRAPPER_SOCKET_WRAPPER_INTERFACE_H_
