
// Copyright (C) 2022 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
#pragma once

#include <memory>                           // for shared_ptr

#include "net/async_data_channel_server.h"  // for AsyncConnectionServer

namespace test_vendor_lib {
class HciTransport;

using android::net::AsyncDataChannelServer;
using android::net::AsyncConnectionServer;

using HciTransportConnectCallback = android::net::AsyncConnectionServer<HciTransport>::ConnectCallback;

// A HciTransport server produces HciTransport based on raw Datachannels.
class HciTransportServer : public AsyncConnectionServer<HciTransport> {
  public:
  HciTransportServer(std::shared_ptr<AsyncDataChannelServer> connectionServer);
  ~HciTransportServer() = default;

  // Start listening for new connections. The callback will be invoked
  // when a new socket has been accepted.
  //
  // errno will be set in case of failure.
  bool StartListening() override;

  // Stop listening for new connections. The callback will not be
  // invoked, and sockets will not be accepted.
  //
  // This DOES not disconnect the server, and connections can still
  // be queued up.
  void StopListening() override;

  // Disconnects the server, no new connections are possible.
  // The callback will never be invoked again.
  void Close() override;

  // True if this server is connected and can accept incoming
  // connections.
  bool Connected() override;

 private:
  std::shared_ptr<AsyncDataChannelServer> connectionServer_;
};
}  // namespace test_vendor_lib