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

#include "main/shim/entry.h"

class LppOffloadInterfaceMainImpl : public bluetooth::shim::LppOffloadInterfaceMain {
public:
  ~LppOffloadInterfaceMainImpl() override {}

  void Init() { RegisterLppOffloadCallbacks(this); }

  // bluetooth::hci::LppOffloadInterface
  void RegisterLppOffloadCallbacks(LppOffloadCallbacks* callbacks) {
    bluetooth::shim::GetLppOffloadManager()->RegisterLppOffloadCallbacks(this);
  }

  void GetOffloadFeaturesSupported(bluetooth::hci::LppOffloadFeatures& features) {
    bluetooth::shim::GetLppOffloadManager()->GetOffloadFeaturesSupported(features);
  }

  void SetAclCredits(int link, int credit) {
    bluetooth::shim::GetLppOffloadManager()->SetAclCredits(link, credit);
  }

  void NotifyAclConnectionStateChange(uint16_t handle, int link, int state) {
    bluetooth::shim::GetLppOffloadManager()->NotifyAclConnectionStateChange(handle, link, state);
  }

  void NotifyAclLeDataLengthChange(uint16_t handle, int txDataLen, int rxDataLen) {
    bluetooth::shim::GetLppOffloadManager()->NotifyAclLeDataLengthChange(handle, txDataLen,
                                                                         rxDataLen);
  }

  void NotifyAclPowerModeChange(uint16_t handle, int mode, uint16_t interval) {
    bluetooth::shim::GetLppOffloadManager()->NotifyAclPowerModeChange(handle, mode, interval);
  }

  // bluetooth::hci::LppOffloadCallbacks
  void OnReset() {}

  void OnSocketCloseRequest(bluetooth::Uuid /*socket_id*/, uint8_t /*reason*/) {}
};

LppOffloadInterfaceMainImpl* lpp_offload_instance = nullptr;

void bluetooth::shim::init_lpp_offload_manager() {
  static_cast<LppOffloadInterfaceMainImpl*>(bluetooth::shim::get_lpp_offload_instance())->Init();
}

bluetooth::shim::LppOffloadInterfaceMain* bluetooth::shim::get_lpp_offload_instance() {
  if (lpp_offload_instance == nullptr) {
    lpp_offload_instance = new LppOffloadInterfaceMainImpl();
  }
  return lpp_offload_instance;
}