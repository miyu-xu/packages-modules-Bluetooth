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

#include <cstdint>

constexpr uint16_t kAggressiveConnectionThreshold = 2;

constexpr uint16_t kMinConnIntervalRelaxed = 0x0018;     // 24, *1.25 becomes 30ms
constexpr uint16_t kMaxConnIntervalRelaxed = 0x0028;     // 40, *1.25 becomes 50ms
constexpr uint16_t kMinConnIntervalAggressive = 0x0006;  // 6, *1.25 becomes 7.5ms
constexpr uint16_t kMaxConnIntervalAggressive = 0x0008;  // 8, *1.25 becomes 10ms

static const std::string kPropertyMinConnIntervalRelaxed =
        "bluetooth.core.le.min_connection_interval_relaxed";
static const std::string kPropertyMaxConnIntervalRelaxed =
        "bluetooth.core.le.max_connection_interval_relaxed";
static const std::string kPropertyMinConnIntervalAggressive =
        "bluetooth.core.le.min_connection_interval_aggressive";
static const std::string kPropertyMaxConnIntervalAggressive =
        "bluetooth.core.le.max_connection_interval_aggressive";
