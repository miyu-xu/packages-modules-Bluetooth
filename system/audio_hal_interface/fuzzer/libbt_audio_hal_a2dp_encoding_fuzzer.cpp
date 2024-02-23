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

#include "a2dp_aac_constants.h"
#include "a2dp_codec_api.h"
#include "a2dp_sbc_constants.h"
#include "android-base/properties.h"
#include "audio_hal_interface/a2dp_encoding.h"
#include "bt_uuid16.h"
#include "bta_av_api.h"
#include "bta_av_co.h"
#include "btif_av_co.h"
#include "include/btif_av.h"
#include "include/hardware/bt_av.h"
#include "osi/include/properties.h"

using ::bluetooth::audio::a2dp::update_codec_offloading_capabilities;

extern "C" {
struct android_namespace_t* android_get_exported_namespace(const char*) {
  return nullptr;
}
}

constexpr tA2DP_CTRL_ACK kCtrlAckStatus[] = {
    A2DP_CTRL_ACK_SUCCESS,        A2DP_CTRL_ACK_FAILURE,
    A2DP_CTRL_ACK_INCALL_FAILURE, A2DP_CTRL_ACK_UNSUPPORTED,
    A2DP_CTRL_ACK_PENDING,        A2DP_CTRL_ACK_DISCONNECT_IN_PROGRESS};

constexpr int16_t kRandomStringLength = 256;
constexpr uint8_t kProtectInfoSize = 3;
constexpr uint8_t kMinBtaAvHandle = 1;
constexpr uint8_t kMaxBtaAvHandle = 6;
constexpr uint8_t kMinCodecConfig = 1;

constexpr btav_a2dp_codec_index_t kCodecIndices[] = {
    BTAV_A2DP_CODEC_INDEX_SOURCE_AAC,     BTAV_A2DP_CODEC_INDEX_SOURCE_APTX,
    BTAV_A2DP_CODEC_INDEX_SOURCE_APTX_HD, BTAV_A2DP_CODEC_INDEX_SOURCE_LDAC,
    BTAV_A2DP_CODEC_INDEX_SINK_AAC,       BTAV_A2DP_CODEC_INDEX_SINK_LDAC};

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

constexpr btav_a2dp_codec_channel_mode_t kChannelMode[] = {
    BTAV_A2DP_CODEC_CHANNEL_MODE_NONE, BTAV_A2DP_CODEC_CHANNEL_MODE_MONO,
    BTAV_A2DP_CODEC_CHANNEL_MODE_STEREO};

std::vector<std::vector<btav_a2dp_codec_config_t>>
CodecOffloadingPreferenceGenerator() {
  std::vector<std::vector<btav_a2dp_codec_config_t>> offloadingPreferences = {
      std::vector<btav_a2dp_codec_config_t>(0)};
  btav_a2dp_codec_config_t btavCodecConfig = {};
  for (btav_a2dp_codec_index_t i : kCodecIndices) {
    btavCodecConfig.codec_type = i;
    auto duplicated_preferences = offloadingPreferences;
    for (auto iter = duplicated_preferences.begin();
         iter != duplicated_preferences.end(); ++iter) {
      iter->push_back(btavCodecConfig);
    }
    offloadingPreferences.insert(offloadingPreferences.end(),
                                 duplicated_preferences.begin(),
                                 duplicated_preferences.end());
  }
  return offloadingPreferences;
}

class A2dpEncodingFuzzer {
 public:
  A2dpEncodingFuzzer(const uint8_t* data, size_t size) : mFdp(data, size){};
  ~A2dpEncodingFuzzer() {
    delete (mCodec);
    mCodec = nullptr;
  }
  void process();
  RawAddress getFuzzRawAddress();
  static A2dpCodecConfig* mCodec;

 private:
  FuzzedDataProvider mFdp;
};

RawAddress A2dpEncodingFuzzer::getFuzzRawAddress() {
  RawAddress result = {{
      mFdp.ConsumeIntegral<uint8_t>(),
      mFdp.ConsumeIntegral<uint8_t>(),
      mFdp.ConsumeIntegral<uint8_t>(),
      mFdp.ConsumeIntegral<uint8_t>(),
      mFdp.ConsumeIntegral<uint8_t>(),
      mFdp.ConsumeIntegral<uint8_t>(),
  }};
  return result;
}

A2dpCodecConfig* A2dpEncodingFuzzer::mCodec{nullptr};

void A2dpEncodingFuzzer::process() {
  if (!mCodec) {
    mCodec = A2dpCodecConfig::createCodec(mFdp.PickValueInArray(kCodecIndices));
  }

  osi_property_set("persist.bluetooth.a2dp_offload.disabled",
                   mFdp.PickValueInArray({"true", "false"}));

  const std::string property =
      "persist.device_config.aconfig_flags.bluetooth.com.android.bluetooth."
      "flags.a2dp_offload_codec_extensibility";
  const std::string propValue = android::base::GetProperty(property, "false");
  android::base::SetProperty(property, "true");

  std::string name = mFdp.ConsumeRandomLengthString(kRandomStringLength);
  bluetooth::common::MessageLoopThread messageLoopThread(name);
  messageLoopThread.StartUp();

  if (mFdp.ConsumeBool()) {
    uint16_t delayReport = mFdp.ConsumeIntegral<uint16_t>();
    bluetooth::audio::a2dp::set_remote_delay(delayReport);
  }

  std::vector<btav_a2dp_codec_config_t> codecPriorities;
  uint8_t numCodecConfigs =
      mFdp.ConsumeIntegralInRange<uint8_t>(kMinCodecConfig, UINT8_MAX);
  for (uint8_t i = 0; i < numCodecConfigs; ++i) {
    btav_a2dp_codec_config_t codecConfig;
    codecConfig.codec_type = mFdp.PickValueInArray(kCodecTypes);
    codecConfig.codec_priority = mFdp.ConsumeBool()
                                     ? BTAV_A2DP_CODEC_PRIORITY_DEFAULT
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

  std::vector<uint8_t> pCodecInfo(AVDT_CODEC_SIZE);
  pCodecInfo[0] = mFdp.ConsumeBool() ? A2DP_SBC_INFO_LEN : A2DP_AAC_CODEC_LEN;
  if (pCodecInfo[0] == A2DP_SBC_INFO_LEN) {
    pCodecInfo[1] = AVDT_MEDIA_TYPE_AUDIO | A2DP_MEDIA_CT_SBC;
    pCodecInfo[2] = A2DP_MEDIA_CT_SBC;
    pCodecInfo[3] = A2DP_SBC_IE_SAMP_FREQ_MSK | A2DP_SBC_IE_CH_MD_MSK;
    pCodecInfo[4] = A2DP_SBC_IE_BLOCKS_MSK | A2DP_SBC_IE_SUBBAND_MSK |
                    A2DP_SBC_IE_ALLOC_MD_MSK;
    pCodecInfo[5] = mFdp.ConsumeIntegralInRange<uint8_t>(
        A2DP_SBC_IE_MIN_BITPOOL, A2DP_SBC_IE_MAX_BITPOOL - 1);
    pCodecInfo[6] = mFdp.ConsumeIntegralInRange<uint8_t>(
        pCodecInfo[5] + 1, A2DP_SBC_IE_MAX_BITPOOL);
  } else {
    pCodecInfo[1] = AVDT_MEDIA_TYPE_AUDIO | A2DP_MEDIA_CT_AAC;
    pCodecInfo[2] = A2DP_AAC_OBJECT_TYPE_MPEG4_SCALABLE;
    pCodecInfo[3] = A2DP_AAC_SAMPLING_FREQ_MASK0;
    pCodecInfo[4] = A2DP_AAC_CHANNEL_MODE_MASK;
    pCodecInfo[5] = A2DP_AAC_VARIABLE_BIT_RATE_ENABLED;
    pCodecInfo[6] = A2DP_AAC_BIT_RATE_MASK2;
  }

  uint8_t pSepInfoIdx = mFdp.ConsumeIntegral<uint8_t>();
  uint8_t pNumProtect = mFdp.ConsumeIntegral<uint8_t>();
  std::vector<uint8_t> pProtectInfo(kProtectInfoSize);
  std::unique_ptr<AvdtpSepConfig> pCfg = std::make_unique<AvdtpSepConfig>();
  RawAddress rawAddr = getFuzzRawAddress();

  std::vector<btav_a2dp_codec_info_t> supportedCodecs;
  bta_av_co_init(codecPriorities, &supportedCodecs);
  bta_av_co_audio_init(mFdp.PickValueInArray(kCodecIndices), pCfg.get());
  bta_av_co_audio_disc_res(
      mFdp.ConsumeIntegralInRange<uint8_t>(kMinBtaAvHandle, kMaxBtaAvHandle),
      rawAddr, mFdp.ConsumeIntegral<uint8_t>(), mFdp.ConsumeIntegral<uint8_t>(),
      mFdp.ConsumeIntegral<uint8_t>(),
      mFdp.ConsumeBool() ? UUID_SERVCLASS_AUDIO_SOURCE
                         : UUID_SERVCLASS_AUDIO_SINK);

  bta_av_co_audio_getconfig(
      mFdp.ConsumeIntegralInRange<uint8_t>(kMinBtaAvHandle, kMaxBtaAvHandle),
      rawAddr, pCodecInfo.data(), &pSepInfoIdx, mFdp.ConsumeIntegral<uint8_t>(),
      &pNumProtect, pProtectInfo.data());

  bool restartOutput = mFdp.ConsumeBool();
  bta_av_co_set_codec_user_config(rawAddr, codecPriorities.front(),
                                  &restartOutput);
  bta_av_co_audio_open(
      mFdp.ConsumeIntegralInRange<uint8_t>(kMinBtaAvHandle, kMaxBtaAvHandle),
      rawAddr, mFdp.ConsumeIntegral<uint16_t>());

  if (!bluetooth::audio::a2dp::init(&messageLoopThread)) {
    return;
  }

  if (!bluetooth::audio::a2dp::setup_codec()) {
    return;
  }

  bluetooth::audio::a2dp::set_audio_low_latency_mode_allowed(
      mFdp.ConsumeBool());
  bluetooth::audio::a2dp::start_session();

  tA2DP_CTRL_ACK status = mFdp.PickValueInArray(kCtrlAckStatus);
  bluetooth::audio::a2dp::ack_stream_started(status);

  uint16_t bufferSize = mFdp.ConsumeIntegral<uint16_t>();
  uint8_t buffer[bufferSize];
  bluetooth::audio::a2dp::read(buffer, bufferSize);

  for (auto offloadingPreference : CodecOffloadingPreferenceGenerator()) {
    update_codec_offloading_capabilities(offloadingPreference,
                                         mFdp.ConsumeBool());
  }
  status = mFdp.PickValueInArray(kCtrlAckStatus);
  bluetooth::audio::a2dp::ack_stream_suspended(status);
  bluetooth::audio::a2dp::cleanup();
  bluetooth::audio::a2dp::end_session();

  android::base::SetProperty(property, propValue);
}

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  A2dpEncodingFuzzer a2dpEncodingFuzzer(data, size);
  a2dpEncodingFuzzer.process();
  return 0;
}
