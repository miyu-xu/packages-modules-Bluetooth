/*
 * Copyright 2020 The Android Open Source Project
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

#define LOG_TAG "bt_headless_sdp"

#include "test/headless/nop/nop.h"

#include <future>
#include <string>

#include "base/logging.h"     // LOG() stdout and android log
#include "osi/include/log.h"  // android log only
#include "stack/include/sdp_api.h"
#include "test/headless/get_options.h"
#include "test/headless/headless.h"
#include "types/raw_address.h"

using namespace bluetooth::test::headless;

class NopOpt : public ModOpt {
 public:
  int get_integer() const {
    auto it = arg_map_.find("-i");
    return (it == arg_map_.end()) ? 0 : std::stoi(it->second);
  }

  double get_double() const {
    auto it = arg_map_.find("-d");
    return (it == arg_map_.end()) ? 0.0 : std::stod(it->second);
  }

  std::string get_string() const {
    auto it = arg_map_.find("-s");
    return (it == arg_map_.end()) ? std::string("") : it->second;
  }
};

int bluetooth::test::headless::Nop::Run() {
  return RunOnHeadlessStack<int>([this]() {
    // Module options
    const NopOpt* opts = options_.get_module_options<NopOpt>();
    const int one = opts->get_integer();
    const double two = opts->get_double();
    const std::string three = opts->get_string();
    for (const auto& [k, v] : opts->GetDefaultShortArgMap()) {
      LOG_CONSOLE("  arg string:%s:%s", k.c_str(), v.c_str());
    }
    LOG_CONSOLE("one:%d two:%f three:%s", one, two, three.c_str());
    LOG_CONSOLE("Nop loop:%lu\n", loop_);
    return 0;
  });
}
