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

#include <a2dp_aac_constants.h>
#include <a2dp_codec_api.h>
#include <a2dp_sbc_constants.h>
#include <bt_uuid16.h>
#include <bta_av_api.h>
#include <bta_av_co.h>
#include <btif_av_co.h>
#include <fuzzer/FuzzedDataProvider.h>

#include "a2dp_vendor_opus_constants.h"
#include "android-base/properties.h"
#include "audio_hal_interface/a2dp_encoding.h"
#include "btif_a2dp_sink.h"
#include "include/btif_av.h"

using ::bluetooth::audio::a2dp::Status;
using ::bluetooth::audio::a2dp::update_codec_offloading_capabilities;

extern "C" {
struct android_namespace_t* android_get_exported_namespace(const char*) { return nullptr; }
}

constexpr Status kStatus[] = {
        Status::UNKNOWN, Status::SUCCESS, Status::UNSUPPORTED_CODEC_CONFIGURATION,
        Status::FAILURE, Status::PENDING,
};

constexpr int16_t kRandomStringLength = 256;
constexpr uint8_t kProtectInfoSize = 3;
constexpr uint8_t kMinBtaAvHandle = 1;
constexpr uint8_t kMaxBtaAvHandle = 6;
constexpr uint8_t kMinCodecConfig = 1;
constexpr uint8_t kMinRawAddress = 1;
constexpr uint8_t kMaxRawAddress = 255;
constexpr uint8_t kPropertyValueMax = 92;
const char* kPropertyFlag =
        "persist.device_config.aconfig_flags.bluetooth.com.android.bluetooth.flags.a2dp_offload_"
        "codec_extensibility";

const uint8_t codecInfoSbc[AVDT_CODEC_SIZE] = {
        6,                   // Length (A2DP_SBC_INFO_LEN)
        0,                   // Media Type: AVDT_MEDIA_TYPE_AUDIO
        0,                   // Media Codec Type: A2DP_MEDIA_CT_SBC
        0x20 | 0x01,         // Sample Frequency: A2DP_SBC_IE_SAMP_FREQ_44 |
                             // Channel Mode: A2DP_SBC_IE_CH_MD_JOINT
        0x10 | 0x04 | 0x01,  // Block Length: A2DP_SBC_IE_BLOCKS_16 |
                             // Subbands: A2DP_SBC_IE_SUBBAND_8 |
                             // Allocation Method: A2DP_SBC_IE_ALLOC_MD_L
        2,                   // MinimumBitpool Value: A2DP_SBC_IE_MIN_BITPOOL
        53,                  // Maximum Bitpool Value: A2DP_SBC_MAX_BITPOOL
        7, 8, 9};

static void source_init_delayed(void) {}

constexpr btav_a2dp_codec_index_t kCodecIndices[] = {
        BTAV_A2DP_CODEC_INDEX_SOURCE_SBC,  BTAV_A2DP_CODEC_INDEX_SOURCE_AAC,
        BTAV_A2DP_CODEC_INDEX_SOURCE_APTX, BTAV_A2DP_CODEC_INDEX_SOURCE_APTX_HD,
        BTAV_A2DP_CODEC_INDEX_SOURCE_LDAC, BTAV_A2DP_CODEC_INDEX_SINK_SBC,
        BTAV_A2DP_CODEC_INDEX_SINK_AAC,    BTAV_A2DP_CODEC_INDEX_SINK_LDAC};

constexpr btav_a2dp_codec_index_t kCodecTypes[] = {
        BTAV_A2DP_CODEC_INDEX_SOURCE_SBC,  BTAV_A2DP_CODEC_INDEX_SOURCE_AAC,
        BTAV_A2DP_CODEC_INDEX_SOURCE_APTX, BTAV_A2DP_CODEC_INDEX_SOURCE_APTX_HD,
        BTAV_A2DP_CODEC_INDEX_SOURCE_LDAC, BTAV_A2DP_CODEC_INDEX_SOURCE_LC3,
        BTAV_A2DP_CODEC_INDEX_SOURCE_OPUS, BTAV_A2DP_CODEC_INDEX_SINK_SBC,
        BTAV_A2DP_CODEC_INDEX_SINK_AAC,    BTAV_A2DP_CODEC_INDEX_SINK_LDAC,
        BTAV_A2DP_CODEC_INDEX_SINK_OPUS,
};

constexpr btav_a2dp_codec_sample_rate_t kSampleRate[] = {
        BTAV_A2DP_CODEC_SAMPLE_RATE_NONE,   BTAV_A2DP_CODEC_SAMPLE_RATE_44100,
        BTAV_A2DP_CODEC_SAMPLE_RATE_48000,  BTAV_A2DP_CODEC_SAMPLE_RATE_88200,
        BTAV_A2DP_CODEC_SAMPLE_RATE_96000,  BTAV_A2DP_CODEC_SAMPLE_RATE_176400,
        BTAV_A2DP_CODEC_SAMPLE_RATE_192000, BTAV_A2DP_CODEC_SAMPLE_RATE_16000,
        BTAV_A2DP_CODEC_SAMPLE_RATE_24000};

constexpr btav_a2dp_codec_bits_per_sample_t kBitsPerSample[] = {
        BTAV_A2DP_CODEC_BITS_PER_SAMPLE_NONE, BTAV_A2DP_CODEC_BITS_PER_SAMPLE_16,
        BTAV_A2DP_CODEC_BITS_PER_SAMPLE_24, BTAV_A2DP_CODEC_BITS_PER_SAMPLE_32};

constexpr btav_a2dp_codec_channel_mode_t kChannelMode[] = {BTAV_A2DP_CODEC_CHANNEL_MODE_NONE,
                                                           BTAV_A2DP_CODEC_CHANNEL_MODE_MONO,
                                                           BTAV_A2DP_CODEC_CHANNEL_MODE_STEREO};

std::vector<std::vector<btav_a2dp_codec_config_t>> CodecOffloadingPreferenceGenerator() {
  std::vector<std::vector<btav_a2dp_codec_config_t>> offloadingPreferences = {
          std::vector<btav_a2dp_codec_config_t>(0)};
  btav_a2dp_codec_config_t btavCodecConfig = {};
  for (btav_a2dp_codec_index_t i : kCodecIndices) {
    btavCodecConfig.codec_type = i;
    auto duplicated_preferences = offloadingPreferences;
    for (auto iter = duplicated_preferences.begin(); iter != duplicated_preferences.end(); ++iter) {
      iter->push_back(btavCodecConfig);
    }
    offloadingPreferences.insert(offloadingPreferences.end(), duplicated_preferences.begin(),
                                 duplicated_preferences.end());
  }
  return offloadingPreferences;
}

class A2dpEncodingFuzzer {
public:
  A2dpEncodingFuzzer(const uint8_t* data, size_t size) : mFdp(data, size) {}
  ~A2dpEncodingFuzzer() {
    delete (mCodec);
    mCodec = nullptr;
  }
  void process();
  RawAddress getFuzzRawAddress();
  std::vector<btav_a2dp_codec_config_t> getCodecProperties();
  std::vector<uint8_t> getpCodecInfo();
  static A2dpCodecConfig* mCodec;

private:
  FuzzedDataProvider mFdp;
};

class TestAudioPort : public bluetooth::audio::a2dp::StreamCallbacks {
  Status StartStream(bool /*low_latency*/) const override { return Status::PENDING; }
  Status SuspendStream() const override { return Status::PENDING; }
  Status SetLatencyMode(bool /*low_latency*/) const override { return Status::SUCCESS; }
};

RawAddress A2dpEncodingFuzzer::getFuzzRawAddress() {
  RawAddress result = {{
          mFdp.ConsumeIntegralInRange<uint8_t>(kMinRawAddress, kMaxRawAddress),
          mFdp.ConsumeIntegralInRange<uint8_t>(kMinRawAddress, kMaxRawAddress),
          mFdp.ConsumeIntegralInRange<uint8_t>(kMinRawAddress, kMaxRawAddress),
          mFdp.ConsumeIntegralInRange<uint8_t>(kMinRawAddress, kMaxRawAddress),
          mFdp.ConsumeIntegralInRange<uint8_t>(kMinRawAddress, kMaxRawAddress),
          mFdp.ConsumeIntegralInRange<uint8_t>(kMinRawAddress, kMaxRawAddress),
  }};
  return result;
}

A2dpCodecConfig* A2dpEncodingFuzzer::mCodec{nullptr};
const TestAudioPort testAudioPort;

std::vector<btav_a2dp_codec_config_t> A2dpEncodingFuzzer::getCodecProperties() {
  std::vector<btav_a2dp_codec_config_t> codecPriorities;
  uint8_t numCodecConfigs = mFdp.ConsumeIntegralInRange<uint8_t>(kMinCodecConfig, UINT8_MAX);
  for (uint8_t i = 0; i < numCodecConfigs; ++i) {
    btav_a2dp_codec_config_t codecConfig;
    codecConfig.codec_type = mFdp.PickValueInArray(kCodecTypes);
    codecConfig.codec_priority = mFdp.ConsumeBool() ? BTAV_A2DP_CODEC_PRIORITY_DEFAULT
                                                    : BTAV_A2DP_CODEC_PRIORITY_HIGHEST;
    codecConfig.sample_rate = mFdp.PickValueInArray(kSampleRate);
    codecConfig.bits_per_sample = mFdp.PickValueInArray(kBitsPerSample);
    codecConfig.channel_mode = mFdp.PickValueInArray(kChannelMode);
    codecConfig.codec_specific_1 = mFdp.ConsumeIntegral<int64_t>();
    codecConfig.codec_specific_2 = mFdp.ConsumeIntegral<int64_t>();
    codecConfig.codec_specific_3 = mFdp.ConsumeIntegral<int64_t>();
    codecConfig.codec_specific_4 = mFdp.ConsumeIntegral<int64_t>();

    codecPriorities.push_back(codecConfig);
  }
  return codecPriorities;
}

std::vector<uint8_t> A2dpEncodingFuzzer::getpCodecInfo() {
  std::vector<uint8_t> pCodecInfo(AVDT_CODEC_SIZE);
  pCodecInfo[0] = mFdp.ConsumeBool() ? A2DP_SBC_INFO_LEN : A2DP_AAC_CODEC_LEN;
  if (pCodecInfo[0] == A2DP_SBC_INFO_LEN) {
    pCodecInfo[1] = AVDT_MEDIA_TYPE_AUDIO | A2DP_MEDIA_CT_SBC;
    pCodecInfo[2] = A2DP_MEDIA_CT_SBC;
    pCodecInfo[3] = A2DP_SBC_IE_SAMP_FREQ_MSK | A2DP_SBC_IE_CH_MD_MSK;
    pCodecInfo[4] = A2DP_SBC_IE_BLOCKS_MSK | A2DP_SBC_IE_SUBBAND_MSK | A2DP_SBC_IE_ALLOC_MD_MSK;
    pCodecInfo[5] = mFdp.ConsumeIntegralInRange<uint8_t>(A2DP_SBC_IE_MIN_BITPOOL,
                                                         A2DP_SBC_IE_MAX_BITPOOL - 1);
    pCodecInfo[6] =
            mFdp.ConsumeIntegralInRange<uint8_t>(pCodecInfo[5] + 1, A2DP_SBC_IE_MAX_BITPOOL);
  } else {
    pCodecInfo[1] = AVDT_MEDIA_TYPE_AUDIO | A2DP_MEDIA_CT_AAC;
    pCodecInfo[2] = A2DP_AAC_OBJECT_TYPE_MPEG4_SCALABLE;
    pCodecInfo[3] = A2DP_AAC_SAMPLING_FREQ_MASK0;
    pCodecInfo[4] = A2DP_AAC_CHANNEL_MODE_MASK;
    pCodecInfo[5] = A2DP_AAC_VARIABLE_BIT_RATE_ENABLED;
    pCodecInfo[6] = A2DP_AAC_BIT_RATE_MASK2;
  }
  return pCodecInfo;
}

void A2dpEncodingFuzzer::process() {
  if (!mCodec) {
    mCodec = A2dpCodecConfig::createCodec(mFdp.PickValueInArray(kCodecIndices));
  }

  const std::string propValue = android::base::GetProperty(kPropertyFlag, "false");
  if (mFdp.ConsumeBool()) {
    android::base::SetProperty(kPropertyFlag, "true");
  }

  bool offloadEnabled = true;
  std::string name = mFdp.ConsumeRandomLengthString(kRandomStringLength);
  uint16_t peerMtu = mFdp.ConsumeIntegral<uint16_t>();
  int preferredEncodingIntervalUs = mFdp.ConsumeIntegral<int>();

  bluetooth::common::MessageLoopThread messageLoopThread(name);
  messageLoopThread.StartUp();
  messageLoopThread.DoInThread(FROM_HERE, base::BindOnce(&source_init_delayed));
  bluetooth::audio::a2dp::set_audio_low_latency_mode_allowed(mFdp.ConsumeBool());
  if (mFdp.ConsumeBool()) {
    uint16_t delayReport = mFdp.ConsumeIntegral<uint16_t>();
    bluetooth::audio::a2dp::set_remote_delay(delayReport);
  }

  std::vector<btav_a2dp_codec_config_t> codecPriorities = getCodecProperties();
  std::vector<uint8_t> pCodecInfo = getpCodecInfo();
  uint8_t pSepInfoIdx = mFdp.ConsumeIntegral<uint8_t>();
  uint8_t pNumProtect = mFdp.ConsumeIntegral<uint8_t>();
  std::vector<uint8_t> pProtectInfo(kProtectInfoSize);
  std::unique_ptr<AvdtpSepConfig> pCfg = std::make_unique<AvdtpSepConfig>();
  RawAddress rawAddr = getFuzzRawAddress();

  std::vector<btav_a2dp_codec_info_t> supportedCodecs;
  bta_av_co_init(codecPriorities, &supportedCodecs);
  bta_av_co_audio_init(mFdp.PickValueInArray(kCodecIndices), pCfg.get());
  bta_av_co_audio_disc_res(
          mFdp.ConsumeIntegralInRange<uint8_t>(kMinBtaAvHandle, kMaxBtaAvHandle), rawAddr,
          mFdp.ConsumeIntegral<uint8_t>() /*num_seps*/,
          mFdp.ConsumeIntegral<uint8_t>() /*num_sinks*/,
          mFdp.ConsumeIntegral<uint8_t>() /*num_source*/,
          mFdp.ConsumeBool() ? UUID_SERVCLASS_AUDIO_SOURCE : UUID_SERVCLASS_AUDIO_SINK);

  bta_av_co_audio_getconfig(mFdp.ConsumeIntegralInRange<uint8_t>(kMinBtaAvHandle, kMaxBtaAvHandle),
                            rawAddr, pCodecInfo.data(), &pSepInfoIdx,
                            mFdp.ConsumeIntegral<uint8_t>(), &pNumProtect, pProtectInfo.data());

  bool restartOutput = mFdp.ConsumeBool();
  bta_av_co_set_codec_user_config(rawAddr, codecPriorities.front(), &restartOutput);
  bta_av_co_audio_open(mFdp.ConsumeIntegralInRange<uint8_t>(kMinBtaAvHandle, kMaxBtaAvHandle),
                       rawAddr, peerMtu);
  if (!bluetooth::audio::a2dp::init(&messageLoopThread, &testAudioPort, offloadEnabled)) {
    return;
  }

  if (!bta_av_co_set_active_source_peer(rawAddr)) {
    return;
  }

  uint8_t seId = mFdp.ConsumeIntegral<uint8_t>();
  uint8_t btaAvHandle = mFdp.ConsumeIntegralInRange<uint8_t>(kMinBtaAvHandle, kMaxBtaAvHandle);
  uint8_t avdtHandle = mFdp.ConsumeIntegral<uint8_t>();
  uint8_t localSep = mFdp.ConsumeBool();

  bta_av_co_audio_setconfig(btaAvHandle, rawAddr, codecInfoSbc, seId, pNumProtect,
                            pProtectInfo.data(), localSep, avdtHandle);

  bluetooth::audio::a2dp::setup_codec(bta_av_get_a2dp_current_codec(), peerMtu,
                                      preferredEncodingIntervalUs);

  bluetooth::audio::a2dp::set_audio_low_latency_mode_allowed(mFdp.ConsumeBool());
  bluetooth::audio::a2dp::start_session();

  Status status = mFdp.PickValueInArray(kStatus);
  bluetooth::audio::a2dp::ack_stream_started(status);

  uint16_t bufferSize = mFdp.ConsumeIntegral<uint16_t>();
  std::vector<uint8_t> buffer(bufferSize);
  bluetooth::audio::a2dp::read(buffer.data(), buffer.size());

  for (auto offloadingPreference : CodecOffloadingPreferenceGenerator()) {
    update_codec_offloading_capabilities(offloadingPreference, mFdp.ConsumeBool());
  }
  status = mFdp.PickValueInArray(kStatus);
  bluetooth::audio::a2dp::ack_stream_suspended(status);
  bluetooth::audio::a2dp::cleanup();
  bluetooth::audio::a2dp::end_session();
  messageLoopThread.ShutDown();

  android::base::SetProperty(kPropertyFlag, propValue);
}

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  A2dpEncodingFuzzer a2dpEncodingFuzzer(data, size);
  a2dpEncodingFuzzer.process();
  return 0;
}
