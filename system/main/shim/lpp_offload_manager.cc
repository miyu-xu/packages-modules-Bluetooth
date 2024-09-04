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

#include "hci/lpp_offload_manager.h"
#include "main/shim/entry.h"

class LppOffloadInterfaceImpl : public LppOffloadInterface,
                                public bluetooth::hci::LppOffloadCallbacks {
public:
  ~LppOffloadInterfaceImpl() override {}

  void Init() { bluetooth::shim::GetLppOffloadManager()->RegisterLppOffloadCallbacks(this); }

  void OnReset() {}

  void OnSocketCloseRequest(bluetooth::Uuid /*socket_id*/, uint8_t /*reason*/) {}
};

LppOffloadInterfaceImpl* lpp_offload_instance = nullptr;

void bluetooth::shim::init_lpp_offload_manager() {
  static_cast<LppOffloadInterfaceImpl*>(bluetooth::shim::get_lpp_offload_instance())->Init();
}

LppOffloadInterface* bluetooth::shim::get_lpp_offload_instance() {
  if (lpp_offload_instance == nullptr) {
    lpp_offload_instance = new LppOffloadInterfaceImpl();
  }
  return lpp_offload_instance;
}
