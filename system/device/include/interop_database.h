/******************************************************************************
 *
 *  Copyright 2015 Google, Inc.
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

#pragma once

#include "device/include/interop.h"
#include "raw_address.h"

typedef struct {
  RawAddress addr;
  size_t length;
  interop_feature_t feature;
} interop_addr_entry_t;

<<<<<<< PATCH SET (088c4f Fix IOT device issue with master role)
static const interop_addr_entry_t interop_addr_database[] = {
    // Nexus Remote (Spike)
    // Note: May affect other Asus brand devices
    {{{0x08, 0x62, 0x66, 0, 0, 0}}, 3, INTEROP_DISABLE_LE_SECURE_CONNECTIONS},
    {{{0x38, 0x2c, 0x4a, 0xc9, 0, 0}},
     4,
     INTEROP_DISABLE_LE_SECURE_CONNECTIONS},
    {{{0x38, 0x2c, 0x4a, 0xe6, 0, 0}},
     4,
     INTEROP_DISABLE_LE_SECURE_CONNECTIONS},
    {{{0x54, 0xa0, 0x50, 0xd9, 0, 0}},
     4,
     INTEROP_DISABLE_LE_SECURE_CONNECTIONS},
    {{{0xac, 0x9e, 0x17, 0, 0, 0}}, 3, INTEROP_DISABLE_LE_SECURE_CONNECTIONS},
    {{{0xf0, 0x79, 0x59, 0, 0, 0}}, 3, INTEROP_DISABLE_LE_SECURE_CONNECTIONS},

    {{{0x08, 0x62, 0x66, 0, 0, 0}}, 3, INTEROP_HID_PREF_CONN_SUP_TIMEOUT_3S},
    {{{0x38, 0x2c, 0x4a, 0xc9, 0, 0}}, 4, INTEROP_HID_PREF_CONN_SUP_TIMEOUT_3S},
    {{{0x38, 0x2c, 0x4a, 0xe6, 0, 0}}, 4, INTEROP_HID_PREF_CONN_SUP_TIMEOUT_3S},
    {{{0x54, 0xa0, 0x50, 0xd9, 0, 0}}, 4, INTEROP_HID_PREF_CONN_SUP_TIMEOUT_3S},
    {{{0xac, 0x9e, 0x17, 0, 0, 0}}, 3, INTEROP_HID_PREF_CONN_SUP_TIMEOUT_3S},
    {{{0xf0, 0x79, 0x59, 0, 0, 0}}, 3, INTEROP_HID_PREF_CONN_SUP_TIMEOUT_3S},

    // Ausdom M05 - unacceptably loud volume
    {{{0xa0, 0xe9, 0xdb, 0, 0, 0}}, 3, INTEROP_DISABLE_ABSOLUTE_VOLUME},

    // BMW car kits (Harman/Becker)
    {{{0x9c, 0xdf, 0x03, 0, 0, 0}}, 3, INTEROP_AUTO_RETRY_PAIRING},

    // Flic smart button
    {{{0x80, 0xe4, 0xda, 0x70, 0, 0}},
     4,
     INTEROP_DISABLE_LE_SECURE_CONNECTIONS},

    // iKross IKBT83B HS - unacceptably loud volume
    {{{0x00, 0x14, 0x02, 0, 0, 0}}, 3, INTEROP_DISABLE_ABSOLUTE_VOLUME},

    // JayBird BlueBuds X - low granularity on volume control
    {{{0x44, 0x5e, 0xf3, 0, 0, 0}}, 3, INTEROP_DISABLE_ABSOLUTE_VOLUME},
    {{{0xd4, 0x9c, 0x28, 0, 0, 0}}, 3, INTEROP_DISABLE_ABSOLUTE_VOLUME},

    // Bose QuiteComfort 35, SoundSport and similar (because of older firmware)
    {{{0x04, 0x52, 0xc7, 0, 0, 0}}, 3, INTEROP_2MBPS_LINK_ONLY},

    // JayBird Family
    {{{0x00, 0x18, 0x91, 0, 0, 0}}, 3, INTEROP_2MBPS_LINK_ONLY},

    // Sony MBH-10
    {{{0x20, 0x15, 0x06, 0, 0, 0}}, 3, INTEROP_2MBPS_LINK_ONLY},

    // Uconnect
    {{{0x00, 0x54, 0xaf, 0, 0, 0}}, 3, INTEROP_2MBPS_LINK_ONLY},
    {{{0x30, 0x14, 0x4a, 0, 0, 0}}, 3, INTEROP_2MBPS_LINK_ONLY},

    // LG Tone HBS-730 - unacceptably loud volume
    {{{0x00, 0x18, 0x6b, 0, 0, 0}}, 3, INTEROP_DISABLE_ABSOLUTE_VOLUME},
    {{{0xb8, 0xad, 0x3e, 0, 0, 0}}, 3, INTEROP_DISABLE_ABSOLUTE_VOLUME},

    // LG Tone HV-800 - unacceptably loud volume
    {{{0xa0, 0xe9, 0xdb, 0, 0, 0}}, 3, INTEROP_DISABLE_ABSOLUTE_VOLUME},

    // Motorola Key Link
    {{{0x1c, 0x96, 0x5a, 0, 0, 0}}, 3, INTEROP_DISABLE_LE_SECURE_CONNECTIONS},

    // Motorola Roadster
    {{{0x00, 0x24, 0x1C, 0, 0, 0}}, 3, INTEROP_DISABLE_ABSOLUTE_VOLUME},

    // Mpow Cheetah - unacceptably loud volume
    {{{0x00, 0x11, 0xb1, 0, 0, 0}}, 3, INTEROP_DISABLE_ABSOLUTE_VOLUME},

    // Nissan car kits (ALPS) - auto-pairing fails and rejects next pairing
    {{{0x34, 0xc7, 0x31, 0, 0, 0}}, 3, INTEROP_DISABLE_AUTO_PAIRING},

    // SOL REPUBLIC Tracks Air - unable to adjust volume back off from max
    {{{0xa4, 0x15, 0x66, 0, 0, 0}}, 3, INTEROP_DISABLE_ABSOLUTE_VOLUME},

    // Subaru car kits (ALPS) - auto-pairing fails and rejects next pairing
    {{{0x00, 0x07, 0x04, 0, 0, 0}}, 3, INTEROP_DISABLE_AUTO_PAIRING},
    {{{0xe0, 0x75, 0x0a, 0, 0, 0}}, 3, INTEROP_DISABLE_AUTO_PAIRING},

    // Swage Rokitboost HS - unacceptably loud volume
    {{{0x00, 0x14, 0xf1, 0, 0, 0}}, 3, INTEROP_DISABLE_ABSOLUTE_VOLUME},

    // VW Car Kit - not enough granularity with volume
    {{{0x00, 0x26, 0x7e, 0, 0, 0}}, 3, INTEROP_DISABLE_ABSOLUTE_VOLUME},
    {{{0x90, 0x03, 0xb7, 0, 0, 0}}, 3, INTEROP_DISABLE_ABSOLUTE_VOLUME},

    // Unknown keyboard (carried over from auto_pair_devlist.conf)
    {{{0x00, 0x0F, 0xF6, 0, 0, 0}}, 3, INTEROP_KEYBOARD_REQUIRES_FIXED_PIN},

    // Kenwood KMM-BT518HD - no audio when A2DP codec sample rate is changed
    {{{0x00, 0x1d, 0x86, 0, 0, 0}}, 3, INTEROP_DISABLE_AVDTP_RECONFIGURE},

    // NAC FORD-2013 - Lincoln
    {{{0x00, 0x26, 0xb4, 0, 0, 0}}, 3, INTEROP_DISABLE_ROLE_SWITCH},

    // Toyota Prius - 2015
    {{{0xfc, 0xc2, 0xde, 0, 0, 0}}, 3, INTEROP_DISABLE_ROLE_SWITCH},

    // Toyota Prius - b/231092023
    {{{0x9c, 0xdf, 0x03, 0, 0, 0}}, 3, INTEROP_DISABLE_ROLE_SWITCH},

    // OBU II Bluetooth dongle
    {{{0x00, 0x04, 0x3e, 0, 0, 0}}, 3, INTEROP_DISABLE_ROLE_SWITCH},

    // Visteon carkit
    {{{0x00, 0x0a, 0x30, 0, 0, 0}}, 3, INTEROP_DISABLE_ROLE_SWITCH},

    // Roman R9020
    {{{0x00, 0x23, 0x01, 0, 0, 0}}, 3, INTEROP_DISABLE_ROLE_SWITCH},

    // Jabra Storm
    {{{0x1c, 0x48, 0xf9, 0, 0, 0}}, 3, INTEROP_DISABLE_ROLE_SWITCH},

    // Jeep Uconnect
    {{{0x00, 0x54, 0xaf, 0, 0, 0}}, 3, INTEROP_DISABLE_ROLE_SWITCH},

    // deepblue2 - cannot change smoothly the volume: b/37834035
    {{{0x0c, 0xa6, 0x94, 0, 0, 0}}, 3, INTEROP_DISABLE_ABSOLUTE_VOLUME},

    // AirPods 2 - unacceptably loud volume
    {{{0x94, 0x16, 0x25, 0, 0, 0}}, 3, INTEROP_DISABLE_ABSOLUTE_VOLUME},

    // AirPods 2 - unacceptably loud volume
    {{{0x9c, 0x64, 0x8b, 0, 0, 0}}, 3, INTEROP_DISABLE_ABSOLUTE_VOLUME},

    // for skip name request,
    // because BR/EDR address and ADV random address are the same
    {{{0xd4, 0x7a, 0xe2, 0, 0, 0}}, 3, INTEROP_DISABLE_NAME_REQUEST},

    // SUBARU Carkit
    {{{0x48, 0xf0, 0x7b, 0, 0, 0}}, 3, INTEROP_AVRCP_1_4_ONLY},

    // Audi Carkit
    {{{0x90, 0x03, 0xb7, 0, 0, 0}}, 3, INTEROP_AVRCP_1_4_ONLY},
    {{{0x28, 0xA1, 0x83, 0, 0, 0}}, 3, INTEROP_AVRCP_1_4_ONLY},

    // Lexus Carkit
    {{{0x64, 0xd4, 0xbd, 0, 0, 0}}, 3, INTEROP_AVRCP_1_4_ONLY},

    // Mazda Carkit
    {{{0xfc, 0x35, 0xe6, 0, 0, 0}}, 3, INTEROP_AVRCP_1_4_ONLY},

    // Toyota Car Audio
    {{{0x00, 0x17, 0x53, 0, 0, 0}}, 3, INTEROP_AVRCP_1_4_ONLY},

    // Honda High End Carkit
    {{{0x9c, 0x8d, 0x7c, 0, 0, 0}}, 3, INTEROP_AVRCP_1_4_ONLY},

    // Honda Civic Carkit
    {{{0x0c, 0xd9, 0xc1, 0, 0, 0}}, 3, INTEROP_AVRCP_1_4_ONLY},

    // BMW Carkit
    {{{0x9c, 0xdf, 0x03, 0, 0, 0}}, 3, INTEROP_AVRCP_1_4_ONLY},

    // KDDI Carkit
    {{{0x44, 0xea, 0xd8, 0, 0, 0}}, 3, INTEROP_DISABLE_SNIFF},

    // Toyota Camry 2018 Carkit HFP AT+BIND missing
    {{{0x94, 0xb2, 0xcc, 0x30, 0, 0}}, 4, INTEROP_SLC_SKIP_BIND_COMMAND},

    // BMW Carkit
    {{{0x00, 0x0a, 0x08, 0, 0, 0}}, 3, INTEROP_AVRCP_1_3_ONLY},

    // {0xb0, 0xe1, 0x7e},  /* Mate 10 */
    // {0x00, 0x0e, 0x9f},  /* old PCM */
    // {0x9c, 0xdf, 0x03},  /* old PCM other */
    {{{0xb0, 0xe1, 0x7e, 0, 0, 0}}, 3, INTEROP_L2CAP_ACCEPT_CONN_AS_SLAVE},
    {{{0x00, 0x0e, 0x9f, 0, 0, 0}}, 3, INTEROP_L2CAP_ACCEPT_CONN_AS_SLAVE},
    {{{0x9c, 0xdf, 0x03, 0, 0, 0}}, 3, INTEROP_L2CAP_ACCEPT_CONN_AS_SLAVE},
};

=======
>>>>>>> BASE      (f25a88 Merge "Close cursor with try resource statement in CallLogPu)
typedef struct {
  RawAddress addr_start;
  RawAddress addr_end;
  interop_feature_t feature;
} interop_addr_range_entry_t;

typedef struct {
  char name[249];
  size_t length;
  interop_feature_t feature;
} interop_name_entry_t;

typedef struct {
  uint16_t manufacturer;
  interop_feature_t feature;
} interop_manufacturer_t;

typedef struct {
  uint16_t vendor_id;
  uint16_t product_id;
  interop_feature_t feature;
} interop_hid_multitouch_t;