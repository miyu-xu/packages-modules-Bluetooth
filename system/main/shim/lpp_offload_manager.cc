/*
 * Copyright 2024 The Android Open Source Project
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
#include "lpp_offload_manager.h"

#include "btif/include/btif_sock_l2cap.h"
#include "hci/lpp_offload_interface.h"
#include "main/shim/entry.h"
class LppOffloadManagerInterfaceImpl : public LppOffloadManagerInterface,
                                       public bluetooth::hci::LppOffloadCallbacks,
                                       public bluetooth::hci::LppOffloadInterface {
public:
  ~LppOffloadManagerInterfaceImpl() override {}

  void Init() { RegisterLppOffloadCallbacks(this); }

  // bluetooth::hci::LppOffloadInterface
  void RegisterLppOffloadCallbacks(bluetooth::hci::LppOffloadCallbacks* callbacks) {
    bluetooth::shim::GetLppOffloadManager()->RegisterLppOffloadCallbacks(callbacks);
  }

  void GetOffloadFeaturesSupported(bluetooth::hci::LppOffloadFeatures* features) {
    bluetooth::shim::GetLppOffloadManager()->GetOffloadFeaturesSupported(features);
  }

  void GetSocketCapabilities(bluetooth::hci::SocketCapabilities* socketCapabilities) {
    bluetooth::shim::GetLppOffloadManager()->GetSocketCapabilities(socketCapabilities);
  }

  bool SocketOpened(const bluetooth::hci::SocketContext& context) {
    return bluetooth::shim::GetLppOffloadManager()->SocketOpened(context);
  }

  void SocketClosed(uint64_t socketId) {
    bluetooth::shim::GetLppOffloadManager()->SocketClosed(socketId);
  }

  // bluetooth::hci::LppOffloadCallbacks
  void SocketOpenedComplete(uint64_t socketId, bluetooth::hci::SocketStatus status) {
    on_btsocket_l2cap_opened_complete(socketId, (status == bluetooth::hci::SocketStatus::SUCCESS));
  }

  void SocketClose(uint64_t socketId) { on_btsocket_l2cap_close(socketId); }
};

LppOffloadManagerInterfaceImpl* lpp_offload_instance = nullptr;

void bluetooth::shim::init_lpp_offload_manager() {
  static_cast<LppOffloadManagerInterfaceImpl*>(bluetooth::shim::get_lpp_offload_instance())->Init();
}

LppOffloadManagerInterface* bluetooth::shim::get_lpp_offload_instance() {
  if (lpp_offload_instance == nullptr) {
    lpp_offload_instance = new LppOffloadManagerInterfaceImpl();
  }
  return lpp_offload_instance;
}
