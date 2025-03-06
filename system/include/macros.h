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

#include <bluetooth/log.h>

#include <cstdint>
#include <string>

#define CASE_RETURN_TEXT(code) \
  case code:                   \
    return #code

#define CASE_RETURN_STRING(enumerator) \
  case enumerator:                     \
    return std::format(#enumerator "(0x{:x})", static_cast<uint64_t>(enumerator))

#define CASE_RETURN_STRING_HEX04(enumerator) \
  case enumerator:                           \
    return std::format(#enumerator "(0x{:04x})", static_cast<uint64_t>(enumerator))

#define RETURN_UNKNOWN_TYPE_STRING(type, variable) \
  return std::format("Unknown {}(0x{:x})", #type, static_cast<uint64_t>(variable))

#define CREATE_STRING_WITH_VALUE(name, value) \
  case value:                                 \
    return #name;

#define CREATE_ENUM_DEFAULT_VALUE(name) name,

#define CREATE_ENUM_WITH_VALUE(name, value) name = value,

#define CHOOSE_MACRO(_1, _2, macro, ...) macro

#define CREATE_STRING(...) \
  CHOOSE_MACRO(__VA_ARGS__, CREATE_STRING_WITH_VALUE, CASE_RETURN_TEXT)(__VA_ARGS__)
#define CREATE_ENUM(...) \
  CHOOSE_MACRO(__VA_ARGS__, CREATE_ENUM_WITH_VALUE, CREATE_ENUM_DEFAULT_VALUE)(__VA_ARGS__)

#define CREATE_STRINGABLE_ENUM(name)                                                      \
  enum name : uint16_t { name(CREATE_ENUM) };                                             \
  std::string toString##name(uint16_t code) {                                             \
    switch (code) { name(CREATE_STRING) default : FATAL("Unknown enum value {}", code); } \
  }

#define CREATE_BT_STATUS(name, enum, origin)              \
  CREATE_STRINGABLE_ENUM(enum)                            \
  class name : public BtStatus {                          \
  public:                                                 \
    name(enum c) : BtStatus(origin, c, toString##enum) {} \
  };