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

#pragma once

#include <sys/select.h>
#include <sys/socket.h>

namespace bluetooth {
namespace hal {

class SocketInterface {
 public:
  virtual ~SocketInterface() = default;

  /* Create a new socket of type TYPE in domain DOMAIN, using
     protocol PROTOCOL.  If PROTOCOL is zero, one is chosen automatically.
     Returns a file descriptor for the new socket, or -1 for errors.  */
  virtual int Socket(int domain, int type, int protocol) = 0;

  /* Give the socket FD the local address ADDR (which is LEN bytes long).  */
  virtual int Bind(int fd, const struct sockaddr* addr, socklen_t len) = 0;

  /* Open a connection on socket FD to peer at ADDR (which LEN bytes long).
      For connectionless socket types, just set the default address to send to
      and the only address from which to accept transmissions.
      Return 0 on success, -1 for errors.

      This function is a cancellation point and therefore not marked with
      __THROW.  */
  virtual int Connect(int fd, const struct sockaddr* addr, socklen_t len) = 0;

  /* Send N bytes of BUF to socket FD.  Returns the number sent or -1.

     This function is a cancellation point and therefore not marked with
     __THROW.  */
  virtual ssize_t Send(int fd, const void* buf, size_t n, int flags) = 0;

  /* Read N bytes into BUF from socket FD.
     Returns the number read or -1 for errors.

     This function is a cancellation point and therefore not marked with
     __THROW.  */
  virtual ssize_t Recv(int fd, void* buf, size_t n, int flags) = 0;

  /* Set socket FD's option OPTNAME at protocol level LEVEL
     to *OPTVAL (which is OPTLEN bytes long).
     Returns 0 on success, -1 for errors.  */
  virtual int Setsockopt(int fd, int level, int optname, const void* optval, socklen_t optlen) = 0;

  /* Prepare to accept connections on socket FD.
     N connection requests will be queued before further requests are refused.
     Returns 0 on success, -1 for errors.  */
  virtual int Listen(int fd, int n) = 0;

  /* Await a connection on socket FD.
     When a connection arrives, open a new socket to communicate with it,
     set *ADDR (which is *ADDR_LEN bytes long) to the address of the connecting
     peer and *ADDR_LEN to the address's actual length, and return the
     new socket's descriptor, or -1 for errors.
     Similar to 'accept' but takes an additional parameter to specify flags.
     */
  virtual int Accept(int fd, struct sockaddr* addr, socklen_t* addr_len, int flags) = 0;

  virtual int Pipe2(int* pipefd, int flags) = 0;

  virtual int GetErrno() const = 0;

  virtual int Close(int fd) = 0;

  virtual void FDSet(int fd, fd_set* set) = 0;

  virtual bool FDIsSet(int fd, fd_set* set) = 0;

  virtual void FDZero(fd_set* set) = 0;

  virtual int Select(
      int __nfds, fd_set* __readfds, fd_set* __writefds, fd_set* __exceptfds, struct timeval* __timeout) = 0;
};

}  // namespace hal
}  // namespace bluetooth
