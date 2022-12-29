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

/*
 * Generated mock file from original source file
 *   Functions generated:34
 *
 *  mockcify.pl ver 0.3.0
 */

#include <cstdint>
#include <functional>
#include <map>
#include <string>

extern std::map<std::string, int> mock_function_count_map;

// Mock include file to share data between tests and mock
#include "test/mock/mock_bta_dm_api.h"
#include "types/raw_address.h"

// Mocked internal structures, if any

namespace test {
namespace mock {
namespace bta_dm_api {

// Function state capture and return values, if needed
struct bta_dm_pm_active bta_dm_pm_active;

}  // namespace bta_dm_api
}  // namespace mock
}  // namespace test

// Mocked functions, if any
void bta_dm_pm_active(const RawAddress& peer_addr) {
  mock_function_count_map[__func__]++;
  test::mock::bta_dm_api::bta_dm_pm_active(peer_addr);
}

// END mockcify generation