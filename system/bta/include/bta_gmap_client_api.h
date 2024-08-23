/*
 * Copyright (C) 2024 The Android Open Source Project
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
#include <hardware/bluetooth.h>
#include <bluetooth/log.h>

#include <optional>
#include <string>

namespace bluetooth::le_audio {
    namespace gmap {

        enum class UGTFeatureBitMask:uint16_t{
SourceFeatureSupport=0,
EightyKbpsSourceSupport=1,
SinkFeatureSupport=1<<2,
SixtyFourSinkFeatureSupport=1<<3,
MultiplexFeatureSupport=1<<4,
MultisinkFeatureSupport=1<<5,
MultisourceFeatureSupport=1<<6
        };

        enum class Roles : uint8_t {
            UGG = 0,
  UGT,
  BGS,
  BGR,
};


class GmapClient{
private:
    uint16_t handle;
    static bool is_offloader_support_gmap;
public:
    constexpr uint16_t kGmapRoleLen = 2;
    static bool ParseGmapRole(std::bitset<16>& role, uint16_t len, const uint8_t* value) {
        if (len != kGmapRoleLen) {
            log::error(", Wrong len of Telephony Media Audio Profile Role, characteristic");
            return false;
        }

        STREAM_TO_UINT16(role, value);

        log::info(", Telephony Media Audio Profile Role:\n\tRole: {}", role.to_string());

        return true;
    }

    static bool ParseUGTFeatures(std::bitset<16>& role, uint16_t len, const uint8_t* value) {
        if (len != kTmapRoleLen) {
            log::error(", Wrong len of Telephony Media Audio Profile Role, characteristic");
            return false;
        }

        STREAM_TO_UINT16(role, value);

        log::info(", Telephony Media Audio Profile Role:\n\tRole: {}", role.to_string());

        return true;
    }

    static bool isGmapClientEnabled(){
        // TODO check system property and feature flag
        return is_offloader_support_gmap;
    }
    static void setGmapOffloaderSupport(bool value){
        is_offloader_support_gmap=value;
    }

    bool readRole();
    uint8_t getRole();
    bool readUGTFeatures();
    uint8_t getUGTFeatures();

};

    }
}
