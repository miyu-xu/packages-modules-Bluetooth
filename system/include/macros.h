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

#include <ios>
#include <sstream>
#include <string>

#define CASE_RETURN_TEXT(code) \
  case code:                   \
    return #code

#define CASE_RETURN_STRING(enumerator)                     \
  case enumerator:                                         \
    return []() {                                          \
      std::stringstream builder;                           \
      builder << #enumerator << "(0x" << std::hex          \
              << static_cast<uint64_t>(enumerator) << ")"; \
      return builder.str();                                \
    }()

#define RETURN_UNKNOWN_TYPE_STRING(type, variable)      \
  return [variable]() {                                 \
    std::stringstream builder;                          \
    builder << "Unknown " << #type << "(0x" << std::hex \
            << static_cast<uint64_t>(variable) << ")";  \
    return builder.str();                               \
  }()
