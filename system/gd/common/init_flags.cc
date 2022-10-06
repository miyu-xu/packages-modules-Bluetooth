/******************************************************************************
 *
 *  Copyright 2019 The Android Open Source Project
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

#include "init_flags.h"

#include <cstdlib>
#include <string>

#include "common/strings.h"
#include "os/log.h"

namespace bluetooth {
namespace common {

void InitFlags::Load(const char** flags) {
  rust::Vec<rust::String> rusted_flags = rust::Vec<rust::String>();
  while (flags != nullptr && *flags != nullptr) {
    rusted_flags.push_back(rust::String(*flags));
    flags++;
  }
  init_flags::load(std::move(rusted_flags));
}

void InitFlags::SetAllForTesting() {
  init_flags::set_all_for_testing();
}

}  // namespace common
}  // namespace bluetooth
