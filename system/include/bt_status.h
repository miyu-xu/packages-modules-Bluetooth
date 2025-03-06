//
//  Copyright 2025 Google, Inc.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at:
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

#pragma once

#include <cassert>
#include <string>

#include "macros.h"

#define StatusCodeMap std::map<uint16_t, std::string>
#define BtStatusCode uint16_t
#define BT_SUCCESS 0

// Define the all status code origins.
//
// 0x0000           is reserved for universal success.
// 0xFFFF           is reserved for the stopgap LegacyStatus origin.
// 0x0001 - 0x007F  are reserved for Bluetooth spec origins.
// 0x0080 - 0x00FF  are reserved for native stack origins.
// 0x0100 - 0x01FF  are reserved for Java stack origins.
// 0x0200 - 0xFFFE  are reserved for future use.
#define BtStatusOrigin(f) \
    f(SUCCESS, 0x0000)  \
    f(HCI, 0x0001)  \
    f(XYZ)  \
    f(LegacyStatus, 0xFFFF)
CREATE_STRINGABLE_ENUM(BtStatusOrigin);

// The base class for all Bluetooth status codes.
class BtStatus {
protected:
  BtStatusOrigin origin_;
  BtStatusCode code_;
  std::string (*toString_)(uint16_t);

public:
  BtStatus()
      : origin_(static_cast<BtStatusOrigin>(BT_SUCCESS)),
        code_(static_cast<BtStatusCode>(BT_SUCCESS)),
        toString_(nullptr) {}

  BtStatusOrigin origin() { return origin_; }
  BtStatusCode code() { return code_; }

  // To easily pass around between stacks and compare
  uint32_t toUInt32() { return origin_ << 16 | code_; }
  operator int() { return toUInt32(); }

  // Quickly check if status == SUCCESS
  bool isSuccess() { return toUInt32() == BT_SUCCESS; }
  operator bool() { return isSuccess(); }

  // To compare against other statuses
  bool operator==(BtStatus& other) { return toUInt32() == other.toUInt32(); }

  // Used for logging
  std::string toString() {
    return !isSuccess() ? toStringBtStatusOrigin(origin_) + "_" + toString_(code_) : "BT_SUCCESS";
  }
  operator std::string() { return toString(); }

protected:
  BtStatus(BtStatusOrigin o, BtStatusCode c, std::string (*s)(uint16_t))
      : origin_(o), code_(c), toString_(s) {
    assert(toString_ != nullptr);
  }
};
