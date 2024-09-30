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

#include "hci/lpp_offload_interface.h"
#include "main/shim/entry.h"
class LppOffloadManagerInterfaceImpl : public LppOffloadManagerInterface,
                                       public bluetooth::hci::LppOffloadCallbacks,
                                       public bluetooth::hci::LppOffloadInterface {
public:
  ~LppOffloadManagerInterfaceImpl() override {}

  void Init() { RegisterLppOffloadCallbacks(this); }

  // bluetooth::hci::LppOffloadInterface
  void RegisterLppOffloadCallbacks(LppOffloadCallbacks* callbacks) {
    bluetooth::shim::GetLppOffloadManager()->RegisterLppOffloadCallbacks(callbacks);
  }

  void GetOffloadFeaturesSupported(bluetooth::hci::LppOffloadFeatures* features) {
    bluetooth::shim::GetLppOffloadManager()->GetOffloadFeaturesSupported(features);
  }

  bool GetSocketProperties(bluetooth::hci::SocketDataPath dataPath,
                           bluetooth::hci::SocketProperties* socketPros) {
    return bluetooth::shim::GetLppOffloadManager()->GetSocketProperties(dataPath, socketPros);
  }

  bool SetAclCreditsForSockets(bluetooth::hci::AclLinkType linkType, int credit) {
    return bluetooth::shim::GetLppOffloadManager()->SetAclCreditsForSockets(linkType, credit);
  }

  bool NotifyAclLeDataLengthChange(int aclHandle, int txDataLen, int rxDataLen) {
    return bluetooth::shim::GetLppOffloadManager()->NotifyAclLeDataLengthChange(
            aclHandle, txDataLen, rxDataLen);
  }

  bool NotifySocketConnectionStateChange(const bluetooth::hci::SocketContext& context) {
    return bluetooth::shim::GetLppOffloadManager()->NotifySocketConnectionStateChange(context);
  }

  bool SendSocketData(const bluetooth::hci::SocketContext& context,
                      const std::vector<uint8_t>& data) {
    return bluetooth::shim::GetLppOffloadManager()->SendSocketData(context, data);
  }

  // bluetooth::hci::LppOffloadCallbacks
  void onReceiveAsyncEvent(bluetooth::hci::AsyncEventType /*eventType*/) {}

  void onReceiveSocketCloseRequest(const bluetooth::Uuid& /*socket_id*/,
                                   bluetooth::hci::RequestReason /*reason*/) {}

  void onReceiveSocketData(const bluetooth::Uuid& /*socketId*/,
                           const std::vector<uint8_t>& /*data*/) {}
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
