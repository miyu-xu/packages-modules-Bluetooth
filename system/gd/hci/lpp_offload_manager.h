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
#pragma once

#include <bluetooth/log.h>

#include <memory>

#include "module.h"
#include "types/bluetooth/uuid.h"

namespace bluetooth {
namespace hci {

// Mirroring bt_lpp_offload_features_t definition.
struct LppOffloadFeatures {
  bool socket_offload_supported;
  uint8_t max_le_coc_socket_num;
  uint8_t max_rfcomm_socket_num;
};

class LppOffloadCallbacks {
public:
  virtual ~LppOffloadCallbacks() = default;
  virtual void OnReset() = 0;
  virtual void OnSocketCloseRequest(bluetooth::Uuid socket_id, uint8_t reason) = 0;
};

class LppOffloadManager : public bluetooth::Module {
public:
  LppOffloadManager();
  ~LppOffloadManager();
  LppOffloadManager(const LppOffloadManager&) = delete;
  LppOffloadManager& operator=(const LppOffloadManager&) = delete;

  void RegisterLppOffloadCallbacks(LppOffloadCallbacks* callbacks);
  void GetOffloadFeaturesSupported(LppOffloadFeatures& features);
  void SetAclCredits(int link, int credit);
  void NotifyAclConnectionStateChange(uint16_t handle, int link, int state);
  void NotifyAclLeDataLengthChange(uint16_t handle, int txDataLen, int rxDataLen);
  void NotifyAclPowerModeChange(uint16_t handle, int mode, uint16_t interval);

  static const ModuleFactory Factory;

protected:
  void ListDependencies(ModuleList* list) const override;

  void Start() override;

  void Stop() override;

  std::string ToString() const override;

private:
  struct impl;
  std::unique_ptr<impl> pimpl_;
};

}  // namespace hci
}  // namespace bluetooth