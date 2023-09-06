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

#include "bta/include/bta_ag_api.h"
#include "btif/include/stack_manager.h"

static const stack_manager_t interface = {nullptr, nullptr, nullptr, nullptr,
                                          nullptr};

const stack_manager_t* stack_manager_get_interface() { return &interface; }

void bte_load_did_conf(const char* p_path) {}

// Stubbed
const tBTA_AG_RES_DATA tBTA_AG_RES_DATA::kEmpty = {};
