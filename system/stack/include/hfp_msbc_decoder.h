/*
 * Copyright (C) 2022 The Android Open Source Project
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
// Interface to the HFP mSBC Decoder
//

#ifndef HFP_MSBC_DECODER_H
#define HFP_MSBC_DECODER_H

#include <cstddef>
#include <cstdint>

// Initialize the HFP MSBC decoder.
bool hfp_msbc_decoder_init();

// Cleanup the HFP MSBC decoder.
bool hfp_msbc_decoder_cleanup(int* num_decoded_frames,
                              double* packet_loss_ratio);

// Decodes |i_buf| into |o_buf| with size |out_len| in bytes. |i_buf| should
// point to a complete mSBC packet with 60 bytes of data including the header.
// If |i_buf| is nil or the decoding fails, PLC is conducted.
// Returns the number of bytes of the output frame.
uint32_t hfp_msbc_decoder_decode_packet(const uint8_t* i_buf, int16_t* o_buf);

#endif  // HFP_MSBC_DECODER_H
