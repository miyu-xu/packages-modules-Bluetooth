/*
 * Copyright 2022 The Android Open Source Project
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

#include <mutex>

namespace bluetooth {
namespace audio {

class HalTransportManager {
 public:
  enum Transport {
    HAL_TRANSPORT_UNKNOWN,
    HAL_TRANSPORT_HIDL,
    HAL_TRANSPORT_AIDL,
  };

 private:
  static inline std::mutex lock;
  static inline Transport transport = HAL_TRANSPORT_UNKNOWN;

 public:
  static Transport GetTransport() {
    if (transport != HAL_TRANSPORT_UNKNOWN) {
      return transport;
    }
    std::lock_guard<std::mutex> lock_guard(lock);
    transport = HAL_TRANSPORT_HIDL;
    return transport;
  }

 private:
};

}  // namespace audio
}  // namespace bluetooth