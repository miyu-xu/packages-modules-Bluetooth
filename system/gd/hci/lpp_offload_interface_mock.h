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

#include "hci/lpp_offload_interface.h"

// Unit test interfaces
namespace bluetooth {
namespace hci {
namespace testing {

class MockLppOffloadCallbacks : public LppOffloadCallbacks {
public:
  MOCK_METHOD(void, onReceiveAsyncEvent, (AsyncEventType), (override));
  MOCK_METHOD(void, onReceiveSocketCloseRequest, (const bluetooth::Uuid&, RequestReason),
              (override));
  MOCK_METHOD(void, onReceiveSocketData, (const bluetooth::Uuid&, const std::vector<uint8_t>&),
              (override));
};

class MockLppOffloadInterface : public LppOffloadInterface {
public:
  MOCK_METHOD(void, RegisterLppOffloadCallbacks, (LppOffloadCallbacks*), (override));
  MOCK_METHOD(void, GetOffloadFeaturesSupported, (LppOffloadFeatures*), (override));
  MOCK_METHOD(bool, GetSocketProperties, (SocketDataPath, SocketProperties*), (override));
  MOCK_METHOD(bool, SetAclCreditsForSockets, (AclLinkType, int), (override));
  MOCK_METHOD(bool, NotifyAclLeDataLengthChange, (int, int, int), (override));
  MOCK_METHOD(bool, NotifySocketConnectionStateChange, (const SocketContext&), (override));
  MOCK_METHOD(bool, SendSocketData, (const SocketContext&, const std::vector<uint8_t>&),
              (override));
};

}  // namespace testing
}  // namespace hci
}  // namespace bluetooth
