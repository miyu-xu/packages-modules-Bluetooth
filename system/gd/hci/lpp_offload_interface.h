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
  virtual void OnSocketCloseRequest(bluetooth::Uuid socket_id, int reason) = 0;
};

class LppOffloadInterface {
public:
  LppOffloadInterface() = default;
  virtual ~LppOffloadInterface() = default;

  LppOffloadInterface(const LppOffloadInterface&) = delete;
  LppOffloadInterface& operator=(const LppOffloadInterface&) = delete;

  virtual void RegisterLppOffloadCallbacks(LppOffloadCallbacks* callbacks) = 0;
  virtual void GetOffloadFeaturesSupported(LppOffloadFeatures& features) = 0;
  virtual bool SetAclCredits(int linkType, int credit) = 0;
  virtual bool NotifyAclConnectionStateChange(int aclHandle, int linkType, int state) = 0;
  virtual bool NotifyAclLeDataLengthChange(int aclHandle, int txDataLen, int rxDataLen) = 0;
  virtual bool NotifyAclPowerModeChange(int aclHandle, int powerMode, int interval) = 0;
};

}  // namespace hci
}  // namespace bluetooth