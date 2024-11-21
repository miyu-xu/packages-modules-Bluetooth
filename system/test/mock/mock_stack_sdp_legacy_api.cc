/*
 * Copyright 2023 The Android Open Source Project
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

#include <cstdint>

#include "stack/include/sdp_api.h"
#include "types/bluetooth/uuid.h"

namespace test {
namespace mock {
namespace stack_sdp_legacy {

bluetooth::legacy::stack::sdp::tSdpApi api_ = {
        .service = {
                .SDP_InitDiscoveryDb = [](tSDP_DISCOVERY_DB*, uint32_t, uint16_t,
                                          const bluetooth::Uuid*, uint16_t,
                                          const uint16_t*) -> bool { return false; },
        }};

}  // namespace stack_sdp_legacy
}  // namespace mock
}  // namespace test

const struct bluetooth::legacy::stack::sdp::tSdpApi*
bluetooth::legacy::stack::sdp::get_legacy_stack_sdp_api() {
  return &test::mock::stack_sdp_legacy::api_;
}
