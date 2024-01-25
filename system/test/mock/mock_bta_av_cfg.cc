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

/*
 * Generated mock file from original source file
 *   Functions generated:21
 */
#include <cstdint>

#include "bta/include/bta_av_cfg.h"

class MockBtaAvConfig : public BtaAvConfig {};

const BtaAvConfig BtaAvCfgFactory::createCustomConfig(
    const bool source_enabled, const bool sink_enabled,
    const uint16_t profile_version) {
  MockBtaAvConfig mock_bta_av_config;
  return mock_bta_av_config;
}
