
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
#include "hci_transport_server.h"

#include <functional>              // for __base
#include <type_traits>             // for remove_extent_t

#include "hci_socket_transport.h"  // for HciSocketTransport

namespace test_vendor_lib {

HciTransportServer::HciTransportServer(
    std::shared_ptr<AsyncDataChannelServer> connectionServer)
    : connectionServer_(connectionServer) {
  connectionServer->SetOnConnectCallback([this](auto connection, auto from) {
    auto transport = HciSocketTransport::Create(connection);
    callback_(transport, this);
  });
}

bool HciTransportServer::StartListening() {
  return connectionServer_->StartListening();
}

void HciTransportServer::StopListening() { connectionServer_->StopListening(); }
void HciTransportServer::Close() { connectionServer_->Close(); }
bool HciTransportServer::Connected() { return connectionServer_->Connected(); }
}  // namespace test_vendor_lib