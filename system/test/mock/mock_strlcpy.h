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

/*
 * Generated mock file from original source file
 *   Functions generated:3
 *
 *  mockcify.pl ver 0.6.2
 */

#include <cstddef>
#include <functional>

// Original included files, if any
// NOTE: Since this is a mock file with mock definitions some number of
//       include files may not be required.  The include-what-you-use
//       still applies, but crafting proper inclusion is out of scope
//       for this effort.  This compilation unit may compile as-is, or
//       may need attention to prune from (or add to ) the inclusion set.

// Original usings

// Mocked compile conditionals, if any

namespace test {
namespace mock {
namespace osi_strlcpy {

// Name: strlcat
// Params: char* dst, const char* src, size_t siz
// Return: size_t
struct strlcat {
  static size_t return_value;
  std::function<size_t(char* dst, const char* src, size_t siz)> body{
      [](char* dst, const char* src, size_t siz) { return return_value; }};
  size_t operator()(char* dst, const char* src, size_t siz) {
    return body(dst, src, siz);
  };
};
extern struct strlcat strlcat;

// Name: strlcpy
// Params: char* dst, const char* src, size_t siz
// Return: size_t
struct strlcpy {
  static size_t return_value;
  std::function<size_t(char* dst, const char* src, size_t siz)> body{
      [](char* dst, const char* src, size_t siz) { return return_value; }};
  size_t operator()(char* dst, const char* src, size_t siz) {
    return body(dst, src, siz);
  };
};
extern struct strlcpy strlcpy;

}  // namespace osi_strlcpy
}  // namespace mock
}  // namespace test

// END mockcify generation
