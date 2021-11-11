#pragma once

#include "btm_iso_api_types.h"
#include "le_audio_types.h"

using bluetooth::hci::iso_manager::supported_standard_codecs_t;
using bluetooth::hci::iso_manager::supported_vendor_spec_codecs_t;

namespace le_audio {

constexpr uint8_t CODEC_OFFLOAD_STATUS_SUCCESS = 0x00;
constexpr uint8_t CODEC_OFFLOAD_STATUS_FAIL = 0x01;

enum class CodecOffloadState {
  IDLE,
  LOCAL_SUPPORTED_CODECS_READ,
  LOCAL_SUPPORTED_CODEC_CAPABILITIES_READ,
  READY,
};

struct codec_offload_state {
  CodecOffloadState state;
  uint8_t direction;
  uint8_t codec_index;
};

struct codec_offload_caps {
  struct le_audio::types::LeAudioCodecId codec_id;
  std::vector<le_audio::types::LeAudioLtvMap> codec_spec_caps;
};

struct codec_offload {
  std::vector<struct supported_standard_codecs_t> standard_codecs;
  std::vector<struct supported_vendor_spec_codecs_t> vendor_spec_codecs;
  std::vector<struct codec_offload_caps> sink_caps;
  std::vector<struct codec_offload_caps> source_caps;
  struct codec_offload_state offload_state;
  le_audio::types::LeAudioCodecLocation codec_location;
  uint16_t context_types;
};

/* Codec Offload interface */
class LeAudioCodecOffload {
 public:
  class Callbacks {
   public:
    virtual ~Callbacks() = default;

    virtual void StatusCb(uint8_t status) = 0;
  };

  virtual ~LeAudioCodecOffload() = default;

  struct le_audio::codec_offload codec_offload_ = {};

  static void Initialize(Callbacks* codec_offload_callbacks);
  static void Cleanup(void);
  static LeAudioCodecOffload* Get(void);

  virtual bool IsRequiredCommandsSupported(void) = 0;
  virtual void InitConfig(void) = 0;
  virtual le_audio::types::LeAudioCodecLocation GetCodecLocation(void) = 0;
  virtual bool IsEnabled(const uint16_t context_type) = 0;
  virtual bool IsReady(void) = 0;
  virtual bool IsSupported(
      const uint8_t direction, const struct le_audio::types::LeAudioCodecId,
      const le_audio::types::LeAudioLc3Config& lc3_config) = 0;

  virtual void ProcessHciNotifLocalSupportedCodecsRead(
      uint8_t status,
      std::vector<struct supported_standard_codecs_t>& standard_codecs,
      std::vector<struct supported_vendor_spec_codecs_t>&
          vendor_spec_codecs) = 0;
  virtual void ProcessHciNotifLocalSupportedCodecCapabilitiesRead(
      uint8_t status, std::vector<uint8_t>& codec_caps_len,
      uint8_t* codec_caps) = 0;
  virtual void ProcessHciNotifLocalSupportedControllerDelayRead(
      uint8_t status, uint32_t min_controller_delay,
      uint32_t max_controller_delay) = 0;
};
}  // namespace le_audio