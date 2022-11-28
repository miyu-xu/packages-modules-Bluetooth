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

#pragma once

#include <stdbool.h>
#include "btcore/include/module.h"
#include "osi/include/config.h"

static const char PROFILE_CONFIG_MODULE[] = "profile_config_module";
#pragma once

typedef struct {
  bool (*is_pbap_sim_enabled)(void);
  bool (*is_pbap_0102_enabled)(void);
  bool (*is_map_0104_enabled)(void);
} profile_config_t;

const profile_config_t* profile_config_get_interface(void);
