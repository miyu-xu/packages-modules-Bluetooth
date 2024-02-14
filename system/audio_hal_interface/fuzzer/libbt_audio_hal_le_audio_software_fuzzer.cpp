/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

#include <fuzzer/FuzzedDataProvider.h>

#include "android-base/properties.h"
#include "audio_hal_interface/le_audio_software.h"

using ::bluetooth::audio::le_audio::LeAudioClientInterface;

constexpr int16_t kRandomStringLength = 256;
constexpr int16_t kMinDataInterval = 1000;
constexpr int16_t kMinFrameDuration = 0;
constexpr int16_t kMinPeerDelay = 0;

constexpr char kMessageLoopThreadName[] = "FuzzerMessageLoopThread";
constexpr char kPropertyName[] =
    "persist.device_config.aconfig_flags.bluetooth.com.android.bluetooth.flags."
    "leaudio_dynamic_spatial_audio";

constexpr uint8_t kBitsPerSample[] = {
    ::bluetooth::audio::le_audio::kBitsPerSample16,
    ::bluetooth::audio::le_audio::kBitsPerSample24,
    ::bluetooth::audio::le_audio::kBitsPerSample32,
};

constexpr uint8_t kChannelCount[] = {
    ::bluetooth::audio::le_audio::kChannelNumberMono,
    ::bluetooth::audio::le_audio::kChannelNumberStereo,
};

constexpr uint32_t kSampleRates[] = {
    ::bluetooth::audio::le_audio::kSampleRate8000,
    ::bluetooth::audio::le_audio::kSampleRate16000,
    ::bluetooth::audio::le_audio::kSampleRate24000,
    ::bluetooth::audio::le_audio::kSampleRate32000,
    ::bluetooth::audio::le_audio::kSampleRate44100,
    ::bluetooth::audio::le_audio::kSampleRate48000,
};

bluetooth::audio::le_audio::StreamCallbacks streamCallbacks = {
    [](bool) { return true; } /* onResume */,
    []() { return true; } /* onSuspend */,
    [](const source_metadata_v7_t&, bluetooth::le_audio::DsaMode) {
      return true;
    } /* onMetadataUpdate */,
    [](const sink_metadata_v7_t&) { return true; } /* onSinkMetadataUpdate */,
};

extern "C" {
struct android_namespace_t* android_get_exported_namespace(const char*) {
  return nullptr;
}
}

void setParams(LeAudioClientInterface::PcmParameters* params,
               FuzzedDataProvider* fdp) {
  params->data_interval_us = fdp->ConsumeIntegralInRange<uint32_t>(
      kMinDataInterval, std::numeric_limits<uint32_t>::max());
  params->sample_rate = fdp->PickValueInArray(kSampleRates);
  params->bits_per_sample = fdp->PickValueInArray(kBitsPerSample);
  params->channels_count = fdp->PickValueInArray(kChannelCount);
}

void setOffloadConfig(bluetooth::le_audio::offload_config* config,
                      FuzzedDataProvider* fdp) {
  uint8_t streamMapSize = fdp->ConsumeIntegral<uint8_t>();
  for (uint8_t i = 0; i < streamMapSize; ++i) {
    bluetooth::le_audio::stream_map_info stream_map(
        fdp->ConsumeIntegral<uint16_t>() /* stream_handle */,
        fdp->ConsumeIntegral<uint32_t>() /* audio_channel_allocation */,
        fdp->ConsumeBool() /* is_stream_active */);

    config->stream_map.push_back(stream_map);
  }
  config->bits_per_sample = fdp->PickValueInArray(kBitsPerSample);
  config->sampling_rate = fdp->PickValueInArray(kSampleRates);
  config->frame_duration = fdp->ConsumeIntegralInRange<uint32_t>(
      kMinFrameDuration, std::numeric_limits<uint32_t>::max());
  config->octets_per_frame = fdp->ConsumeIntegral<uint16_t>();
  config->blocks_per_sdu = fdp->ConsumeIntegral<uint8_t>();
  config->peer_delay_ms = fdp->ConsumeIntegralInRange<uint32_t>(
      kMinPeerDelay, std::numeric_limits<uint32_t>::max());
}

void setBroadcastOffloadConfig(
    bluetooth::le_audio::broadcast_offload_config* config,
    FuzzedDataProvider* fdp) {
  uint8_t streamMapSize = fdp->ConsumeIntegral<uint8_t>();
  for (uint8_t i = 0; i < streamMapSize; ++i) {
    config->stream_map.push_back(
        {fdp->ConsumeIntegral<uint16_t>(), fdp->ConsumeIntegral<uint32_t>()});
  }
  config->bits_per_sample = fdp->PickValueInArray(kBitsPerSample);
  config->sampling_rate = fdp->PickValueInArray(kSampleRates);
  config->frame_duration = fdp->ConsumeIntegralInRange<uint32_t>(
      kMinFrameDuration, std::numeric_limits<uint32_t>::max());
  config->octets_per_frame = fdp->ConsumeIntegral<uint16_t>();
  config->blocks_per_sdu = fdp->ConsumeIntegral<uint8_t>();
  config->retransmission_number = fdp->ConsumeIntegral<uint8_t>();
  config->max_transport_latency = fdp->ConsumeIntegral<uint16_t>();
}

static void source_init_delayed(void) {}

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  FuzzedDataProvider fdp(data, size);

  bluetooth::common::MessageLoopThread messageLoopThread(
      kMessageLoopThreadName);
  messageLoopThread.StartUp();
  messageLoopThread.DoInThread(FROM_HERE, base::BindOnce(&source_init_delayed));

  LeAudioClientInterface* interface = LeAudioClientInterface::Get();

  if (!interface) {
    return 0;
  }

  LeAudioClientInterface::Source* source =
      interface->GetSource(streamCallbacks, &messageLoopThread);

  LeAudioClientInterface::Sink* sink =
      interface->GetSink(streamCallbacks, &messageLoopThread,
                         fdp.ConsumeBool() /* is_broadcasting_session_type */);

  if (!interface->IsSourceAcquired() || !sink) {
    return 0;
  }

  if (!sink->IsBroadcaster()) {
    bluetooth::le_audio::DsaModes dsaModes;
    if (fdp.ConsumeBool()) {
      dsaModes.push_back(bluetooth::le_audio::DsaMode::DISABLED);
    }
    if (fdp.ConsumeBool()) {
      dsaModes.push_back(bluetooth::le_audio::DsaMode::ACL);
    }
    if (fdp.ConsumeBool()) {
      dsaModes.push_back(bluetooth::le_audio::DsaMode::ISO_SW);
    }
    if (fdp.ConsumeBool()) {
      dsaModes.push_back(bluetooth::le_audio::DsaMode::ISO_HW);
    }

    std::string propertyValue =
        android::base::GetProperty(kPropertyName, "false");
    if (fdp.ConsumeBool()) {
      android::base::SetProperty(kPropertyName, "true");
    } else {
      android::base::SetProperty(kPropertyName, "false");
    }

    interface->SetAllowedDsaModes(dsaModes);

    android::base::SetProperty(kPropertyName, propertyValue);
  }

  source->StartSession();
  sink->StartSession();

  LeAudioClientInterface::PcmParameters params;
  setParams(&params, &fdp);

  while (fdp.remaining_bytes()) {
    auto invokeLeAudioSoftwareAPI =
        fdp.PickValueInArray<const std::function<void()>>(
            {[&]() {
               source->SetRemoteDelay(
                   fdp.ConsumeIntegral<uint16_t>() /* delay_report_ms */);
             },
             [&]() { source->SetPcmParameters(params); },
             [&]() { source->ConfirmStreamingRequest(); },
             [&]() { source->CancelStreamingRequest(); },
             [&]() {
               bluetooth::le_audio::offload_config config;
               setOffloadConfig(&config, &fdp);
               source->UpdateAudioConfigToHal(config);
             },
             [&]() { source->SuspendedForReconfiguration(); },
             [&]() { source->ReconfigurationComplete(); },
             [&]() {
               std::vector<uint8_t> writeData =
                   fdp.ConsumeBytes<uint8_t>(fdp.ConsumeIntegral<uint16_t>());
               source->Write(writeData.data(), writeData.size());
             },
             [&]() {
               sink->SetRemoteDelay(
                   fdp.ConsumeIntegral<uint16_t>() /* delay_report_ms */);
             },
             [&]() { sink->SetPcmParameters(params); },
             [&]() { sink->ConfirmStreamingRequest(); },
             [&]() { sink->CancelStreamingRequest(); },
             [&]() {
               bluetooth::le_audio::offload_config config;
               setOffloadConfig(&config, &fdp);
               sink->UpdateAudioConfigToHal(config);
             },
             [&]() {
               bluetooth::le_audio::broadcast_offload_config config;
               setBroadcastOffloadConfig(&config, &fdp);
               sink->UpdateBroadcastAudioConfigToHal(config);
             },
             [&]() { sink->SuspendedForReconfiguration(); },
             [&]() { sink->ReconfigurationComplete(); },
             [&]() {
               uint8_t readData[fdp.ConsumeIntegral<uint16_t>()];
               sink->Read(readData, sizeof(readData));
             }});

    invokeLeAudioSoftwareAPI();
  }

  interface->ReleaseSource(source);
  /**
   * Calling LeAudioClientInterface::Sink::Cleanup() explicitly because of the
   * improper setting of sink in le_audio_software.cc.
   */
  sink->Cleanup();
  interface->ReleaseSink(sink);

  return 0;
}
