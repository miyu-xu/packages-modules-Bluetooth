// Copyright (C) 2021 The Android Open Source Project
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

#include <functional>
#include <memory>

#include "model/hci/hci_socket_transport.h"
#include "net/async_data_channel_server.h"

namespace android {
namespace net {

// Adapts raw socket channel server to a hci transport channel server
class AsyncHciTransportChannelAdapter : public AsyncHciTransportChannelServer {
 public:
  AsyncHciTransportChannelAdapter(
      std::shared_ptr<AsyncDataChannelServer> toAdapt)
      : adapt_(toAdapt) {
    toAdapt->SetOnConnectCallback([this](auto socket, auto server) {
      auto transport = rootcanal::HciSocketTransport::Create(socket);
      if (callback_) {
        callback_(transport, this);
      }
    });
  }

  bool StartListening() override { return adapt_->StartListening(); }
  void StopListening() override { adapt_->StopListening(); }
  void Close() override { adapt_->Close(); }
  bool Connected() override { return adapt_->Connected(); }

 private:
  std::shared_ptr<AsyncDataChannelServer> adapt_;
};
}  // namespace net
}  // namespace android