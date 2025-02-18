/*
 * Copyright 2019 The Android Open Source Project
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

#include <vector>

#include "a2dp_common_encoding_interface.h"
#include "a2dp_encoding.h"
#include "client_interface_hidl.h"
#include "common/message_loop_thread.h"
#include "hardware/bt_av.h"

namespace bluetooth {
namespace audio {
namespace hidl {
namespace a2dp {

using ::bluetooth::audio::a2dp::Status;

class SoftwareEncoding : public ::bluetooth::audio::a2dp::IA2dpEncoding {
public:
  SoftwareEncoding(::bluetooth::audio::hidl::BluetoothAudioSinkClientInterface* audio_interface);
  ~SoftwareEncoding() override;
  void SetRemoteDelay(uint16_t delay_report) override;
  void StartSession() override;
  void StopSession() override;
  void ConfirmStreamStarted(Status status) override;
  void ConfirmStreamSuspended(Status status) override;
  bool UpdateAudioConfigToHal(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                              [[maybe_unused]] int preferred_encoding_interval_us) override;
  bool IsCodecSupportedByHardwareOffload(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu) override;
  size_t Read(uint8_t* p_buf, uint32_t len) override;

private:
  ::bluetooth::audio::hidl::BluetoothAudioSinkClientInterface* interface_ = nullptr;
};

class HardwareOffloadEncoding : public ::bluetooth::audio::a2dp::IA2dpEncoding {
public:
  HardwareOffloadEncoding(
          ::bluetooth::audio::hidl::BluetoothAudioSinkClientInterface* audio_interface);
  ~HardwareOffloadEncoding() override;
  void SetRemoteDelay(uint16_t delay_report) override;
  void StartSession() override;
  void StopSession() override;
  void ConfirmStreamStarted(Status status) override;
  void ConfirmStreamSuspended(Status status) override;
  bool UpdateAudioConfigToHal(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu,
                              [[maybe_unused]] int preferred_encoding_interval_us) override;
  bool IsCodecSupportedByHardwareOffload(A2dpCodecConfig* a2dp_config, uint16_t peer_mtu) override;

private:
  ::bluetooth::audio::hidl::BluetoothAudioSinkClientInterface* interface_ = nullptr;
};

}  // namespace a2dp
}  // namespace hidl
}  // namespace audio
}  // namespace bluetooth
