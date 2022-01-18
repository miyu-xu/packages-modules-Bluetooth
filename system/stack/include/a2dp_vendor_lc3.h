/*
 * Copyright 2016 The Android Open Source Project
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
// A2DP Codec API for LC3
//

#ifndef A2DP_VENDOR_LC3_H
#define A2DP_VENDOR_LC3_H

#include "a2dp_codec_api.h"
#include "a2dp_vendor_lc3_constants.h"
#include "avdt_api.h"

class A2dpCodecConfigLc3Source : public A2dpCodecConfig {
 public:
  A2dpCodecConfigLc3Source(btav_a2dp_codec_priority_t codec_priority);
  virtual ~A2dpCodecConfigLc3Source();

  bool init() override;
  bool setCodecConfig(const uint8_t* p_peer_codec_info, bool is_capability,
                      uint8_t* p_result_codec_config) override;
  bool setPeerCodecCapabilities(
      const uint8_t* p_peer_codec_capabilities) override;

 private:
  bool useRtpHeaderMarkerBit() const override;
  void debug_codec_dump(int fd) override;
};

// Checks whether the codec capabilities contain a valid A2DP vendor-specific
// Source codec.
// NOTE: only codecs that are implemented are considered valid.
// Returns true if |p_codec_info| contains information about a valid
// vendor-specific codec, otherwise false.
bool A2DP_IsVendorSourceCodecValidLc3(const uint8_t* p_codec_info);

// Checks whether the codec capabilities contain a valid peer A2DP
// vendor-specific Sink codec.
// NOTE: only codecs that are implemented are considered valid.
// Returns true if |p_codec_info| contains information about a valid
// vendor-specific codec, otherwise false.
bool A2DP_IsVendorPeerSinkCodecValidLc3(const uint8_t* p_codec_info);

// Checks whether the A2DP vendor-specific data packets should contain RTP
// header. |content_protection_enabled| is true if Content Protection is
// enabled. |p_codec_info| contains information about the codec capabilities.
// Returns true if the A2DP vendor-specific data packets should contain RTP
// header, otherwise false.
bool A2DP_VendorUsesRtpHeaderLc3(bool content_protection_enabled,
                                 const uint8_t* p_codec_info);

// Gets the A2DP vendor-specific codec name for a given |p_codec_info|.
const char* A2DP_VendorCodecNameLc3(const uint8_t* p_codec_info);

// Checks whether two A2DP vendor-specific codecs |p_codec_info_a| and
// |p_codec_info_b| have the same type.
// Returns true if the two codecs have the same type, otherwise false.
// If the codec type is not recognized, the return value is false.
bool A2DP_VendorCodecTypeEqualsLc3(const uint8_t* p_codec_info_a,
                                   const uint8_t* p_codec_info_b);

// Checks whether two A2DP vendor-specific codecs |p_codec_info_a| and
// |p_codec_info_b| are exactly the same.
// Returns true if the two codecs are exactly the same, otherwise false.
// If the codec type is not recognized, the return value is false.
bool A2DP_VendorCodecEqualsLc3(const uint8_t* p_codec_info_a,
                               const uint8_t* p_codec_info_b);

// Gets the track sample rate value for the A2DP vendor-specific codec.
// |p_codec_info| is a pointer to the vendor-specific codec_info to decode.
// Returns the track sample rate on success, or -1 if |p_codec_info|
// contains invalid codec information.
int A2DP_VendorGetTrackSampleRateLc3(const uint8_t* p_codec_info);

// Gets the track bits per sample value for the A2DP vendor-specific codec.
// |p_codec_info| is a pointer to the vendor-specific codec_info to decode.
// Returns the track sample rate on success, or -1 if |p_codec_info|
// contains invalid codec information.
int A2DP_VendorGetTrackBitsPerSampleLc3(const uint8_t* p_codec_info);

// Gets the channel count for the A2DP vendor-specific codec.
// |p_codec_info| is a pointer to the vendor-specific codec_info to decode.
// Returns the channel count on success, or -1 if |p_codec_info|
// contains invalid codec information.
int A2DP_VendorGetTrackChannelCountLc3(const uint8_t* p_codec_info);

// Gets the bitrate for the A2DP vendor-specific codec.
// |p_codec_info| is a pointer to the vendor-specific codec_info to decode.
// Returns the channel count on success, or -1 if |p_codec_info|
// contains invalid codec information.
int A2DP_VendorGetBitRateLc3(const uint8_t* p_codec_info);

// Gets the channel mode code for the A2DP LC3 codec.
// The actual value is codec-specific - see |A2DP_LC3_CHANNEL_MODE_*|.
// |p_codec_info| is a pointer to the LC3 codec_info to decode.
// Returns the channel mode code on success, or -1 if |p_codec_info|
// contains invalid codec information.
int A2DP_VendorGetChannelModeCodeLc3(const uint8_t* p_codec_info);

// Gets the A2DP codec-specific audio data timestamp from an audio packet.
// |p_codec_info| contains the codec information.
// |p_data| contains the audio data.
// The timestamp is stored in |p_timestamp|.
// Returns true on success, otherwise false.
bool A2DP_VendorGetPacketTimestampLc3(const uint8_t* p_codec_info,
                                      const uint8_t* p_data,
                                      uint32_t* p_timestamp);

// Builds A2DP vendor-specific codec header for audio data.
// |p_codec_info| contains the codec information.
// |p_buf| contains the audio data.
// |frames_per_packet| is the number of frames in this packet.
// Returns true on success, otherwise false.
bool A2DP_VendorBuildCodecHeaderLc3(const uint8_t* p_codec_info, BT_HDR* p_buf,
                                    uint16_t frames_per_packet);

// Gets the A2DP vendor encoder interface that can be used to encode and
// prepare A2DP packets for transmission - see |tA2DP_ENCODER_INTERFACE|.
// |p_codec_info| contains the codec information.
// Returns the A2DP vendor encoder interface if the |p_codec_info| is valid and
// supported, otherwise NULL.
const tA2DP_ENCODER_INTERFACE* A2DP_VendorGetEncoderInterfaceLc3(
    const uint8_t* p_codec_info);

// Adjusts the A2DP vendor-specific codec, based on local support and Bluetooth
// specification.
// |p_codec_info| contains the codec information to adjust.
// Returns true if |p_codec_info| is valid and supported, otherwise false.
bool A2DP_VendorAdjustCodecLc3(uint8_t* p_codec_info);

// Gets the A2DP vendor Source codec index for a given |p_codec_info|.
// Returns the corresponding |btav_a2dp_codec_index_t| on success,
// otherwise |BTAV_A2DP_CODEC_INDEX_MAX|.
btav_a2dp_codec_index_t A2DP_VendorSourceCodecIndexLc3(
    const uint8_t* p_codec_info);

// Gets the A2DP vendor codec name for a given |codec_index|.
const char* A2DP_VendorCodecIndexStrLc3();

// Initializes A2DP vendor codec-specific information into |AvdtpSepConfig|
// configuration entry pointed by |p_cfg|. The selected codec is defined by
// |codec_index|.
// Returns true on success, otherwise false.
bool A2DP_VendorInitCodecConfigLc3(AvdtpSepConfig* p_cfg);

// Decodes A2DP vendor codec info into a human readable string.
// |p_codec_info| is a pointer to the codec_info to decode.
// Returns a string describing the codec information.
std::string A2DP_VendorCodecInfoStringLc3(const uint8_t* p_codec_info);

#endif  // A2DP_VENDOR_LC3_H
