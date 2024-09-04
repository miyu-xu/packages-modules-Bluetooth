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

#include <gmock/gmock.h>

#include "hci/lpp_offload_manager.h"

// Unit test interfaces
namespace bluetooth {
namespace hci {

struct LppOffloadManager::impl {};

namespace testing {

class MockLppOffloadManager : public LppOffloadManager {
public:
  MOCK_METHOD(void, RegisterLppOffloadCallbacks, (LppOffloadCallbacks*));
  MOCK_METHOD(void, GetOffloadFeaturesSupported, (LppOffloadFeatures&));
  MOCK_METHOD(void, SetAclCredits, (int, int));
  MOCK_METHOD(void, NotifyAclConnectionStateChange, (uint16_t, int, int));
  MOCK_METHOD(void, NotifyAclLeDataLengthChange, (uint16_t, int, int));
  MOCK_METHOD(void, NotifyAclPowerModeChange, (uint16_t, int, uint16_t));
};

}  // namespace testing
}  // namespace hci
}  // namespace bluetooth