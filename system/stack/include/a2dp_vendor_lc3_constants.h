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

//
// A2DP constants for lc3 codec

#ifndef A2DP_VENDOR_LC3_CONSTANTS_H
#define A2DP_VENDOR_LC3_CONSTANTS_H

// TODO
/* LC3 codec specific settings */
#define A2DP_LC3_CODEC_LEN 9

#define A2DP_LC3_DEFAULT_BITRATE 256000
#define A2DP_LC3_DEFAULT_BYTE_COUNT 320

// [Octet 0-3] Vendor ID
#define A2DP_LC3_VENDOR_ID 0x000000E0
// [Octet 4-5] Vendor Specific Codec ID
#define A2DP_LC3_CODEC_ID 0x0001
// [Ocetet 6], [Bits 0-1] Channel Mode
#define A2DP_LC3_CHANNEL_MODE_MASK 0x03
#define A2DP_LC3_CHANNEL_MODE_MONO 0x01
#define A2DP_LC3_CHANNEL_MODE_STEREO 0x02
// [Ocetet 6], [Bits 2-3] Frame Size
#define A2DP_LC3_FRAME_SIZE_MASK 0x0C
#define A2DP_LC3_FRAME_SIZE_750 0x04
#define A2DP_LC3_FRAME_SIZE_1000 0x08
// [Ocetet 6], [Bits 4] Dual Packet
#define A2DP_LC3_DUALPACKET_MASK 0x10
#define A2DP_LC3_PACKETMODE_MODE_DUAL 0x10
// [Ocetet 6], [Bits 5-7] Sample Rate
#define A2DP_LC3_SAMPLE_RATE_MASK 0xE0
#define A2DP_LC3_SAMPLE_RATE_32000 0x20
#define A2DP_LC3_SAMPLE_RATE_44100 0x40
#define A2DP_LC3_SAMPLE_RATE_48000 0x80

// Length of Media Payload header
#define A2DP_LC3_MPL_HDR_LEN 1

#define A2DP_LC3_HDR_F_MSK 0x80
#define A2DP_LC3_HDR_S_MSK 0x40
#define A2DP_LC3_HDR_L_MSK 0x20
#define A2DP_LC3_HDR_NUM_MSK 0x0F

#endif  // A2DP_VENDOR_LC3_CONSTANTS_H
