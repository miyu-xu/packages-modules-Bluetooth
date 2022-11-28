/******************************************************************************
 *
 *  Copyright (c) 2016-2017, The Linux Foundation. All rights reserved.
 *  Copyright 2014 Google, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

#define LOG_TAG "bt_device_profile"
#include "profile_config.h"

#include <base/logging.h>

#include "osi/include/future.h"
#include "osi/include/log.h"

namespace {
const char* MAP_PROFILE = "MAP";
const char* PBAP_PROFILE = "PBAP";
const char* PBAP_SIM_SUPPORT_KEY = "PbapSimSupport";
const char* PBAP_VERSION_0102_SUPPORT_KEY = "Pbap0102Support";
const char* MAP_VERSION_0104_SUPPORT_KEY = "Map0104Support";

static std::unique_ptr<config_t> config;

}  // namespace

// Module lifecycle functions

static future_t* init() {
#if defined(TARGET_FLOSS)
  const char* path = "/var/lib/bluetooth/bt_profile.conf";
#elif defined(OS_GENERIC)
  const char* path = "btprofile.conf";
#else  // !defined(OS_GENERIC)
  const char* path = "/apex/com.android.btservices/etc/bluetooth/bt_profile.conf";
#endif  // defined(OS_GENERIC)
  CHECK(path != NULL);

  LOG_INFO("%s attempt to load stack conf from %s", __func__, path);

  config = config_new(path);
  if (!config) {
    LOG_INFO("%s file >%s< not found", __func__, path);
    config = config_new_empty();
  }

  return future_new_immediate(FUTURE_SUCCESS);
}

static future_t* clean_up() {
  config.reset();
  return future_new_immediate(FUTURE_SUCCESS);
}

EXPORT_SYMBOL extern const module_t profile_config_module = {
    .name = PROFILE_CONFIG_MODULE,
    .init = init,
    .start_up = NULL,
    .shut_down = NULL,
    .clean_up = clean_up,
    .dependencies = {NULL}};

// Interface functions
static bool is_pbap_sim_enabled(void) {
  return config_get_bool(*config, PBAP_PROFILE, PBAP_SIM_SUPPORT_KEY,
                         false);
}

static bool is_pbap_0102_enabled(void) {
  return config_get_bool(*config, PBAP_PROFILE,
                         PBAP_VERSION_0102_SUPPORT_KEY, false);
}

static bool is_map_0104_enabled(void) {
  return config_get_bool(*config, MAP_PROFILE,
                         MAP_VERSION_0104_SUPPORT_KEY, false);
}

const profile_config_t interface = {
    is_pbap_sim_enabled,     is_pbap_0102_enabled,
    is_map_0104_enabled};

const profile_config_t* profile_config_get_interface(void) { return &interface; }
