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

  void GetOffloadFeaturesSupported(bluetooth::hci::LppOffloadFeatures& features) {
    bluetooth::shim::GetLppOffloadManager()->GetOffloadFeaturesSupported(features);
  }

  bool SetAclCredits(int link, int credit) {
    return bluetooth::shim::GetLppOffloadManager()->SetAclCredits(link, credit);
  }

  bool NotifyAclConnectionStateChange(int handle, int link, int state) {
    return bluetooth::shim::GetLppOffloadManager()->NotifyAclConnectionStateChange(handle, link,
                                                                                   state);
  }

  bool NotifyAclLeDataLengthChange(int handle, int txDataLen, int rxDataLen) {
    return bluetooth::shim::GetLppOffloadManager()->NotifyAclLeDataLengthChange(handle, txDataLen,
                                                                                rxDataLen);
  }

  bool NotifyAclPowerModeChange(int handle, int mode, int interval) {
    return bluetooth::shim::GetLppOffloadManager()->NotifyAclPowerModeChange(handle, mode,
                                                                             interval);
  }

  // bluetooth::hci::LppOffloadCallbacks
  void OnReset() {}

  void OnSocketCloseRequest(bluetooth::Uuid /*socket_id*/, int /*reason*/) {}
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