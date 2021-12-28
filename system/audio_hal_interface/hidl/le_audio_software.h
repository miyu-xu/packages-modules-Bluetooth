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

#pragma once

#include <hardware/audio.h>

#include <functional>

#include "../le_audio_software.h"
#include "bta/le_audio/le_audio_types.h"
#include "common/message_loop_thread.h"

namespace bluetooth {
namespace audio {
namespace hidl {
namespace le_audio {

constexpr uint8_t kChannelNumberMono = 1;
constexpr uint8_t kChannelNumberStereo = 2;

constexpr uint32_t kSampleRate48000 = 48000;
constexpr uint32_t kSampleRate44100 = 44100;
constexpr uint32_t kSampleRate32000 = 32000;
constexpr uint32_t kSampleRate24000 = 24000;
constexpr uint32_t kSampleRate16000 = 16000;
constexpr uint32_t kSampleRate8000 = 8000;

constexpr uint8_t kBitsPerSample16 = 16;
constexpr uint8_t kBitsPerSample24 = 24;
constexpr uint8_t kBitsPerSample32 = 32;

using ::bluetooth::audio::le_audio::StreamCallbacks;
using SourceStub = ::bluetooth::audio::le_audio::LeAudioClientInterface::Source;
using SinkStub = ::bluetooth::audio::le_audio::LeAudioClientInterface::Sink;

std::vector<::le_audio::set_configurations::AudioSetConfiguration>
get_offload_capabilities();

class LeAudioClientInterface
    : public ::bluetooth::audio::le_audio::LeAudioClientInterface {
 public:
  virtual ~LeAudioClientInterface() {}
  class Sink : public SinkStub {
   public:
    virtual ~Sink() = default;

    void Cleanup() override;
    void SetPcmParameters(const PcmParameters& params) override;
    void SetRemoteDelay(uint16_t delay_report_ms) override;
    void StartSession() override;
    void StopSession() override;
    void ConfirmStreamingRequest() override;
    void CancelStreamingRequest() override;

    // Read the stream of bytes sinked to us by the upper layers
    size_t Read(uint8_t* p_buf, uint32_t len) override;
  };
  class Source : public SourceStub {
   public:
    virtual ~Source() = default;

    void Cleanup() override;
    void SetPcmParameters(const PcmParameters& params) override;
    void SetRemoteDelay(uint16_t delay_report_ms) override;
    void StartSession() override;
    void StopSession() override;
    void ConfirmStreamingRequest() override;
    void CancelStreamingRequest() override;

    // Source the given stream of bytes to be sinked into the upper layers
    size_t Write(const uint8_t* p_buf, uint32_t len) override;
  };

  // Get LE Audio sink client interface if it's not previously acquired and not
  // yet released.
  Sink* GetSink(StreamCallbacks stream_cb,
                bluetooth::common::MessageLoopThread* message_loop) override;
  // This should be called before trying to get sink interface
  bool IsSinkAcquired() override;
  // Release sink interface if belongs to LE audio client interface
  bool ReleaseSink(SinkStub* sink) override;

  // Get LE Audio source client interface if it's not previously acquired and
  // not yet released.
  SourceStub* GetSource(
      StreamCallbacks stream_cb,
      bluetooth::common::MessageLoopThread* message_loop) override;
  // This should be called before trying to get source interface
  bool IsSourceAcquired() override;
  // Release source interface if belongs to LE audio client interface
  bool ReleaseSource(SourceStub* source) override;

  // Get interface, if previously not initialized - it'll initialize singleton.
  static LeAudioClientInterface* Get();

 private:
  static LeAudioClientInterface* interface;
  Sink* sink_ = nullptr;
  Source* source_ = nullptr;
};

}  // namespace le_audio
}  // namespace hidl
}  // namespace audio
}  // namespace bluetooth