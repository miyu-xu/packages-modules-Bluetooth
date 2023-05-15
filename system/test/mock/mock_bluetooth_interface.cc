/*
 * Copyright 2021 The Android Open Source Project
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

#include "btif/include/stack_manager.h"
#include "device/include/interop.h"
#include "hardware/bluetooth.h"
#include "stack/include/bt_octets.h"
#include "types/raw_address.h"

void invoke_energy_info_cb(bt_activity_energy_info energy_info,
                           bt_uid_traffic_t* uid_data) {}
void invoke_link_quality_report_cb(uint64_t timestamp, int report_id, int rssi,
                                   int snr, int retransmission_count,
                                   int packets_not_receive_count,
                                   int negative_acknowledgement_count) {}

static void init_stack(bluetooth::core::CoreInterface* interface) {}

static void start_up_stack_async(bluetooth::core::CoreInterface* interface,
                                 ProfileStartCallback startProfiles,
                                 ProfileStopCallback stopProfiles) {}

static void shut_down_stack_async(ProfileStopCallback stopProfiles) {}

static void clean_up_stack(ProfileStopCallback stopProfiles) {}

static bool get_stack_is_running() { return true; }

static const stack_manager_t interface = {init_stack, start_up_stack_async,
                                          shut_down_stack_async, clean_up_stack,
                                          get_stack_is_running};

const stack_manager_t* stack_manager_get_interface() { return &interface; }
