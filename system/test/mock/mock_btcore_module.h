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

/*
 * Generated mock file from original source file
 *   Functions generated:7
 *
 *  mockcify.pl ver 0.3.0
 */

#include <cstdint>
#include <functional>
#include <map>
#include <string>

#include "test/common/mock_functions.h"

// Original included files, if any
// NOTE: Since this is a mock file with mock definitions some number of
//       include files may not be required.  The include-what-you-use
//       still applies, but crafting proper inclusion is out of scope
//       for this effort.  This compilation unit may compile as-is, or
//       may need attention to prune from (or add to ) the inclusion set.
#include <base/logging.h>
#include <dlfcn.h>
#include <string.h>

#include <mutex>
#include <unordered_map>

#include "btcore/include/module.h"
#include "check.h"
#include "common/message_loop_thread.h"
#include "osi/include/allocator.h"
#include "osi/include/log.h"
#include "osi/include/osi.h"

// Mocked compile conditionals, if any

namespace test {
namespace mock {
namespace btcore_module {

// Shared state between mocked functions and tests
// Name: module_init_and_start_up
// Params: const module_t& module
// Return: bool
struct module_init_and_start_up {
  bool return_value{false};
  std::function<bool(const module_t& module)> body{
      [this](const module_t& module) { return return_value; }};
  bool operator()(const module_t& module) { return body(module); };
};
extern struct module_init_and_start_up module_init_and_start_up;

// Name: module_management_clean_up
// Params: void
// Return: void
struct module_management_clean_up {
  std::function<void(void)> body{[](void) {}};
  void operator()(void) { body(); };
};
extern struct module_management_clean_up module_management_clean_up;

// Name: module_shut_down_and_clean_up
// Params: const module_t& module
// Return: void
struct module_shut_down_and_clean_up {
  std::function<void(const module_t& module)> body{
      [](const module_t& module) {}};
  void operator()(const module_t& module) { body(module); };
};
extern struct module_shut_down_and_clean_up module_shut_down_and_clean_up;

}  // namespace btcore_module
}  // namespace mock
}  // namespace test

// END mockcify generation