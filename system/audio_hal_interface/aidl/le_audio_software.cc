/*
 * Copyright 2021 HIMSA II K/S - www.himsa.com. Represented by EHIMA -
 * www.ehima.com
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

#define LOG_TAG "BTAudioClientLeAudioAIDL"

#include "le_audio_software.h"

#include <unordered_map>
#include <vector>

#include "codec_status.h"
#include "hal_version_manager.h"

namespace bluetooth {
namespace audio {
namespace aidl {
namespace le_audio {

using ::aidl::android::hardware::bluetooth::audio::AudioConfiguration;
using ::aidl::android::hardware::bluetooth::audio::AudioLocation;
using ::aidl::android::hardware::bluetooth::audio::ChannelMode;
using ::aidl::android::hardware::bluetooth::audio::CodecType;
using ::aidl::android::hardware::bluetooth::audio::PcmConfiguration;
using ::bluetooth::audio::aidl::AudioConfiguration;
using ::bluetooth::audio::aidl::BluetoothAudioCtrlAck;
using ::bluetooth::audio::le_audio::LeAudioClientInterface;
using ::bluetooth::audio::le_audio::StreamCallbacks;

static ChannelMode le_audio_channel_mode2audio_hal(uint8_t channels_count) {
  switch (channels_count) {
    case 1:
      return ChannelMode::MONO;
    case 2:
      return ChannelMode::STEREO;
  }
  return ChannelMode::UNKNOWN;
}

bool is_source_hal_enabled() {
  return LeAudioSourceTransport::interface != nullptr;
}

bool is_sink_hal_enabled() {
  return LeAudioSinkTransport::interface != nullptr;
}

LeAudioTransport::LeAudioTransport(void (*flush)(void),
                                   StreamCallbacks stream_cb,
                                   PcmConfiguration pcm_config)
    : flush_(std::move(flush)),
      stream_cb_(std::move(stream_cb)),
      remote_delay_report_ms_(0),
      total_bytes_processed_(0),
      data_position_({}),
      pcm_config_(std::move(pcm_config)),
      is_pending_start_request_(false){};

BluetoothAudioCtrlAck LeAudioTransport::StartRequest() {
  LOG(INFO) << __func__;

  if (stream_cb_.on_resume_(true)) {
    is_pending_start_request_ = true;
    return BluetoothAudioCtrlAck::PENDING;
  }

  return BluetoothAudioCtrlAck::FAILURE;
}

BluetoothAudioCtrlAck LeAudioTransport::SuspendRequest() {
  LOG(INFO) << __func__;
  if (stream_cb_.on_suspend_()) {
    flush_();
    return BluetoothAudioCtrlAck::SUCCESS_FINISHED;
  } else {
    return BluetoothAudioCtrlAck::FAILURE;
  }
}

void LeAudioTransport::StopRequest() {
  LOG(INFO) << __func__;
  if (stream_cb_.on_suspend_()) {
    flush_();
  }
}

bool LeAudioTransport::GetPresentationPosition(uint64_t* remote_delay_report_ns,
                                               uint64_t* total_bytes_processed,
                                               timespec* data_position) {
  VLOG(2) << __func__ << ": data=" << total_bytes_processed_
          << " byte(s), timestamp=" << data_position_.tv_sec << "."
          << data_position_.tv_nsec
          << "s, delay report=" << remote_delay_report_ms_ << " msec.";
  if (remote_delay_report_ns != nullptr) {
    *remote_delay_report_ns = remote_delay_report_ms_ * 1000000u;
  }
  if (total_bytes_processed != nullptr)
    *total_bytes_processed = total_bytes_processed_;
  if (data_position != nullptr) *data_position = data_position_;

  return true;
}

void LeAudioTransport::SourceMetadataChanged(
    const source_metadata_t& source_metadata) {
  auto track_count = source_metadata.track_count;

  if (track_count == 0) {
    LOG(WARNING) << ", invalid number of metadata changed tracks";
    return;
  }

  stream_cb_.on_metadata_update_(source_metadata);
}

void LeAudioTransport::SinkMetadataChanged(
    const sink_metadata_t& sink_metadata) {
  auto track_count = sink_metadata.track_count;

  if (track_count == 0) {
    LOG(WARNING) << ", invalid number of metadata changed tracks";
    return;
  }

  if (stream_cb_.on_sink_metadata_update_)
    stream_cb_.on_sink_metadata_update_(sink_metadata);
}

void LeAudioTransport::ResetPresentationPosition() {
  VLOG(2) << __func__ << ": called.";
  remote_delay_report_ms_ = 0;
  total_bytes_processed_ = 0;
  data_position_ = {};
}

void LeAudioTransport::LogBytesProcessed(size_t bytes_processed) {
  if (bytes_processed) {
    total_bytes_processed_ += bytes_processed;
    clock_gettime(CLOCK_MONOTONIC, &data_position_);
  }
}

void LeAudioTransport::SetRemoteDelay(uint16_t delay_report_ms) {
  LOG(INFO) << __func__ << ": delay_report=" << delay_report_ms << " msec";
  remote_delay_report_ms_ = delay_report_ms;
}

const PcmConfiguration& LeAudioTransport::LeAudioGetSelectedHalPcmConfig() {
  return pcm_config_;
}

void LeAudioTransport::LeAudioSetSelectedHalPcmConfig(uint32_t sample_rate_hz,
                                                      uint8_t bit_rate,
                                                      uint8_t channels_count,
                                                      uint32_t data_interval) {
  pcm_config_.sampleRateHz = (sample_rate_hz);
  pcm_config_.bitsPerSample = (bit_rate);
  pcm_config_.channelMode = le_audio_channel_mode2audio_hal(channels_count);
  pcm_config_.dataIntervalUs = data_interval;
}

bool LeAudioTransport::IsPendingStartStream(void) {
  return is_pending_start_request_;
}
void LeAudioTransport::ClearPendingStartStream(void) {
  is_pending_start_request_ = false;
}

void flush_sink() {
  if (!is_sink_hal_enabled()) return;

  LeAudioSinkTransport::interface->FlushAudioData();
}

LeAudioSinkTransport::LeAudioSinkTransport(SessionType session_type,
                                           StreamCallbacks stream_cb)
    : IBluetoothSinkTransportInstance(session_type, (AudioConfiguration){}) {
  transport_ = new LeAudioTransport(flush_sink, std::move(stream_cb),
                                    {16000, ChannelMode::STEREO, 16, 0});
};

LeAudioSinkTransport::~LeAudioSinkTransport() { delete transport_; }

BluetoothAudioCtrlAck LeAudioSinkTransport::StartRequest() {
  return transport_->StartRequest();
}

BluetoothAudioCtrlAck LeAudioSinkTransport::SuspendRequest() {
  return transport_->SuspendRequest();
}

void LeAudioSinkTransport::StopRequest() { transport_->StopRequest(); }

bool LeAudioSinkTransport::GetPresentationPosition(
    uint64_t* remote_delay_report_ns, uint64_t* total_bytes_read,
    timespec* data_position) {
  return transport_->GetPresentationPosition(remote_delay_report_ns,
                                             total_bytes_read, data_position);
}

void LeAudioSinkTransport::SourceMetadataChanged(
    const source_metadata_t& source_metadata) {
  transport_->SourceMetadataChanged(source_metadata);
}

void LeAudioSinkTransport::SinkMetadataChanged(
    const sink_metadata_t& sink_metadata) {
  transport_->SinkMetadataChanged(sink_metadata);
}

void LeAudioSinkTransport::ResetPresentationPosition() {
  transport_->ResetPresentationPosition();
}

void LeAudioSinkTransport::LogBytesRead(size_t bytes_read) {
  transport_->LogBytesProcessed(bytes_read);
}

void LeAudioSinkTransport::SetRemoteDelay(uint16_t delay_report_ms) {
  transport_->SetRemoteDelay(delay_report_ms);
}

const PcmConfiguration& LeAudioSinkTransport::LeAudioGetSelectedHalPcmConfig() {
  return transport_->LeAudioGetSelectedHalPcmConfig();
}

void LeAudioSinkTransport::LeAudioSetSelectedHalPcmConfig(
    uint32_t sample_rate_hz, uint8_t bit_rate, uint8_t channels_count,
    uint32_t data_interval) {
  transport_->LeAudioSetSelectedHalPcmConfig(sample_rate_hz, bit_rate,
                                             channels_count, data_interval);
}

bool LeAudioSinkTransport::IsPendingStartStream(void) {
  return transport_->IsPendingStartStream();
}
void LeAudioSinkTransport::ClearPendingStartStream(void) {
  transport_->ClearPendingStartStream();
}

void flush_source() {
  if (LeAudioSourceTransport::interface == nullptr) return;

  LeAudioSourceTransport::interface->FlushAudioData();
}

LeAudioSourceTransport::LeAudioSourceTransport(StreamCallbacks stream_cb)
    : IBluetoothSourceTransportInstance(
          SessionType::LE_AUDIO_SOFTWARE_DECODING_DATAPATH,
          (AudioConfiguration){}) {
  transport_ = new LeAudioTransport(flush_source, std::move(stream_cb),
                                    {16000, ChannelMode::MONO, 16, 0});
};

LeAudioSourceTransport::~LeAudioSourceTransport() { delete transport_; }

BluetoothAudioCtrlAck LeAudioSourceTransport::StartRequest() {
  return transport_->StartRequest();
}

BluetoothAudioCtrlAck LeAudioSourceTransport::SuspendRequest() {
  return transport_->SuspendRequest();
}

void LeAudioSourceTransport::StopRequest() { transport_->StopRequest(); }

bool LeAudioSourceTransport::GetPresentationPosition(
    uint64_t* remote_delay_report_ns, uint64_t* total_bytes_written,
    timespec* data_position) {
  return transport_->GetPresentationPosition(
      remote_delay_report_ns, total_bytes_written, data_position);
}

void LeAudioSourceTransport::SourceMetadataChanged(
    const source_metadata_t& source_metadata) {
  transport_->SourceMetadataChanged(source_metadata);
}

void LeAudioSourceTransport::SinkMetadataChanged(
    const sink_metadata_t& sink_metadata) {
  transport_->SinkMetadataChanged(sink_metadata);
}

void LeAudioSourceTransport::ResetPresentationPosition() {
  transport_->ResetPresentationPosition();
}

void LeAudioSourceTransport::LogBytesWritten(size_t bytes_written) {
  transport_->LogBytesProcessed(bytes_written);
}

void LeAudioSourceTransport::SetRemoteDelay(uint16_t delay_report_ms) {
  transport_->SetRemoteDelay(delay_report_ms);
}

const PcmConfiguration&
LeAudioSourceTransport::LeAudioGetSelectedHalPcmConfig() {
  return transport_->LeAudioGetSelectedHalPcmConfig();
}

void LeAudioSourceTransport::LeAudioSetSelectedHalPcmConfig(
    uint32_t sample_rate_hz, uint8_t bit_rate, uint8_t channels_count,
    uint32_t data_interval) {
  transport_->LeAudioSetSelectedHalPcmConfig(sample_rate_hz, bit_rate,
                                             channels_count, data_interval);
}

bool LeAudioSourceTransport::IsPendingStartStream(void) {
  return transport_->IsPendingStartStream();
}
void LeAudioSourceTransport::ClearPendingStartStream(void) {
  transport_->ClearPendingStartStream();
}

std::unordered_map<int32_t, uint8_t> sampling_freq_map{
    {8000, ::le_audio::codec_spec_conf::kLeAudioSamplingFreq8000Hz},
    {16000, ::le_audio::codec_spec_conf::kLeAudioSamplingFreq16000Hz},
    {24000, ::le_audio::codec_spec_conf::kLeAudioSamplingFreq24000Hz},
    {32000, ::le_audio::codec_spec_conf::kLeAudioSamplingFreq32000Hz},
    {44100, ::le_audio::codec_spec_conf::kLeAudioSamplingFreq44100Hz},
    {48000, ::le_audio::codec_spec_conf::kLeAudioSamplingFreq48000Hz},
    {88200, ::le_audio::codec_spec_conf::kLeAudioSamplingFreq88200Hz},
    {96000, ::le_audio::codec_spec_conf::kLeAudioSamplingFreq96000Hz},
    {176400, ::le_audio::codec_spec_conf::kLeAudioSamplingFreq176400Hz},
    {192000, ::le_audio::codec_spec_conf::kLeAudioSamplingFreq192000Hz}};

std::unordered_map<int32_t, uint8_t> frame_duration_map{
    {7500, ::le_audio::codec_spec_conf::kLeAudioCodecLC3FrameDur7500us},
    {10000, ::le_audio::codec_spec_conf::kLeAudioCodecLC3FrameDur10000us}};

std::unordered_map<uint32_t, uint16_t> octets_per_frame_map{
    {30, ::le_audio::codec_spec_conf::kLeAudioCodecLC3FrameLen30},
    {40, ::le_audio::codec_spec_conf::kLeAudioCodecLC3FrameLen40},
    {120, ::le_audio::codec_spec_conf::kLeAudioCodecLC3FrameLen120}};

std::unordered_map<AudioLocation, uint32_t> audio_location_map{
    {AudioLocation::UNKNOWN,
     ::le_audio::codec_spec_conf::kLeAudioLocationMonoUnspecified},
    {AudioLocation::FRONT_LEFT,
     ::le_audio::codec_spec_conf::kLeAudioLocationFrontLeft},
    {AudioLocation::FRONT_RIGHT,
     ::le_audio::codec_spec_conf::kLeAudioLocationFrontRight},
    {static_cast<AudioLocation>(
         static_cast<uint8_t>(AudioLocation::FRONT_LEFT) |
         static_cast<uint8_t>(AudioLocation::FRONT_RIGHT)),
     ::le_audio::codec_spec_conf::kLeAudioLocationFrontLeft |
         ::le_audio::codec_spec_conf::kLeAudioLocationFrontRight}};

std::vector<AudioSetConfiguration> get_offload_capabilities() { return {}; }

}  // namespace le_audio
}  // namespace aidl
}  // namespace audio
}  // namespace bluetooth