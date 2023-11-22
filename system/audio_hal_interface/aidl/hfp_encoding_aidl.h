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

#include <cstdint>

#include "common/message_loop_thread.h"
#include "hfp_transport.h"

namespace bluetooth {
namespace audio {
namespace aidl {
namespace hfp {

/***
 * Check if new bluetooth_audio is enabled
 ***/
bool is_hal_enabled();

/***
 * Initialize BluetoothAudio HAL: openProvider
 ***/
bool init(bluetooth::common::MessageLoopThread* message_loop);

/***
 * Clean up BluetoothAudio HAL
 ***/
void cleanup();

/***
 * Set up the codec into BluetoothAudio HAL
 ***/
bool setup_codec();

/***
 * Send command to the BluetoothAudio HAL: StartSession, EndSession,
 * StreamStarted, StreamSuspended
 ***/
void start_session();
void end_session();
void ack_stream_started(const tHFP_CTRL_ACK& status);
void ack_stream_suspended(const tHFP_CTRL_ACK& status);

/***
 * Read from the FMQ of BluetoothAudio HAL
 ***/
size_t read(uint8_t* p_buf, uint32_t len);

}  // namespace hfp
}  // namespace aidl
}  // namespace audio
}  // namespace bluetooth
