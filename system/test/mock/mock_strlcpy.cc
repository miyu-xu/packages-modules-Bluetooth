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
 *   Functions generated:3
 *
 *  mockcify.pl ver 0.6.2
 */

// Mock include file to share data between tests and mock
#include "test/mock/mock_strlcpy.h"

#include "test/common/mock_functions.h"

// Original usings

// Mocked internal structures, if any

namespace test {
namespace mock {
namespace osi_strlcpy {

// Function state capture and return values, if needed
struct strlcat strlcat;
struct strlcpy strlcpy;

}  // namespace osi_strlcpy
}  // namespace mock
}  // namespace test

// Mocked function return values, if any
namespace test {
namespace mock {
namespace osi_strlcpy {

size_t strlcat::return_value = 0;
size_t strlcpy::return_value = 0;

}  // namespace osi_strlcpy
}  // namespace mock
}  // namespace test

// Mocked functions, if any
size_t strlcat(char* dst, const char* src, size_t siz) {
  inc_func_call_count(__func__);
  return test::mock::osi_strlcpy::strlcat(dst, src, siz);
}
size_t strlcpy(char* dst, const char* src, size_t siz) {
  inc_func_call_count(__func__);
  return test::mock::osi_strlcpy::strlcpy(dst, src, siz);
}
// Mocked functions complete
// END mockcify generation
