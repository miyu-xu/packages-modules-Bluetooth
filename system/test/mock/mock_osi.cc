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

/*
 * Generated mock file from original source file
 *   Functions generated:6
 *
 *  mockcify.pl ver 0.3.0
 */

#include <base/logging.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

// Mock include file to share data between tests and mock
#include "test/mock/mock_osi.h"

// Mocked internal structures, if any

namespace test {
namespace mock {
namespace osi {

// Function state capture and return values, if needed
struct osi_rand osi_rand;

}  // namespace osi
}  // namespace mock
}  // namespace test

int osi_rand(void) {
  inc_func_call_count(__func__);
  return test::mock::osi::osi_rand();
}
// Mocked functions complete
// END mockcify generation