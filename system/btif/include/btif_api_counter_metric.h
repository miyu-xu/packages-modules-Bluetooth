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

#pragma once

#include <string>
#include <utility>
#include <vector>

// Initializes the API counter metric containers.
void api_counter_metric_init();

// Enable or disable the collection of api counter metrics.
void api_counter_metric_disable();
void api_counter_metric_enable();

// Output the API name and access count in consistent order to filedescriptor.
void api_counter_metric_dump(int fd);

// Threadsafe increment to API count with give name.
void api_counter_metric_entry_inc(const char* name);
void api_counter_metric_callback_inc(const char* name);

// Return container of pairs with given API names and counts
std::vector<std::pair<std::string, size_t>> api_counter_metric_entry_dump();
std::vector<std::pair<std::string, size_t>> api_counter_metric_callback_dump();
