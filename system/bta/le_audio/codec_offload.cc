#include "codec_offload.h"

#include <base/bind.h>
#include <base/callback.h>

#include <map>

#include "bt_types.h"
#include "btm_iso_api.h"
#include "client_parser.h"
#include "device/include/controller.h"
#include "osi/include/properties.h"
#include "stack/include/hci_error_code.h"

using bluetooth::hci::IsoManager;
using bluetooth::hci::iso_manager::supported_standard_codecs_t;
using bluetooth::hci::iso_manager::supported_vendor_spec_codecs_t;
using le_audio::codec_offload_caps;
using le_audio::CodecOffloadState;
using le_audio::LeAudioCodecOffload;
using le_audio::types::kLeAudioCodingFormatLC3;
using le_audio::types::kLeAudioCodingFormatVendorSpecific;
using le_audio::types::kLeAudioDirectionSink;
using le_audio::types::kLeAudioDirectionSource;
using le_audio::types::LeAudioCodecId;
using le_audio::types::LeAudioCodecLocation;
using le_audio::types::LeAudioLc3Config;
using le_audio::types::LeAudioLtvMap;

namespace {

static struct LeAudioCodecId supported_codecs[] = {
    {kLeAudioCodingFormatLC3, 0, 0},
};

class LeAudioCodecOffloadImpl;
LeAudioCodecOffloadImpl* instance;

class LeAudioCodecOffloadImpl : public LeAudioCodecOffload {
 public:
  LeAudioCodecOffloadImpl(Callbacks* codec_offload_callbacks_)
      : codec_offload_callbacks_(codec_offload_callbacks_) {}

  uint16_t GetCodecOffloadSupportedContxtTypes() {
    static constexpr char PROPERTY_NAME_LE_AUDIO_CODEC_OFFLOAD[] =
        "persist.bluetooth.leaudio.lc3_codec_offload";
    char value_str[PROPERTY_VALUE_MAX];
    uint16_t context_types = 0;

    if (osi_property_get(PROPERTY_NAME_LE_AUDIO_CODEC_OFFLOAD, value_str, "0") >
        0) {
      context_types = atoi(value_str);
    }

    LOG(INFO) << __func__ << ", context_types: " << loghex(context_types);

    return context_types;
  }

  LeAudioCodecLocation GetCodecLocation() {
    static constexpr char PROPERTY_NAME_LE_AUDIO_CODEC_LOCATION[] =
        "persist.bluetooth.leaudio.codec_location";
    char value_str[PROPERTY_VALUE_MAX];
    uint8_t value = 0;
    LeAudioCodecLocation codec_location = LeAudioCodecLocation::HOST;

    if (osi_property_get(PROPERTY_NAME_LE_AUDIO_CODEC_LOCATION, value_str,
                         "0") > 0) {
      value = atoi(value_str);
    }

    if (value < static_cast<uint8_t>(LeAudioCodecLocation::MAX)) {
      codec_location = static_cast<LeAudioCodecLocation>(value);
    }

    LOG(INFO) << __func__
              << ", codec_location: " << static_cast<int>(codec_location);

    return codec_location;
  }

  bool IsCodecOffloadCommandsSupported(LeAudioCodecLocation codec_location) {
    bool supports_read_local_supported_codecs_v2 =
        controller_get_interface()->supports_read_local_supported_codecs_v2();
    bool supports_read_local_supported_codec_capabilities =
        controller_get_interface()
            ->supports_read_local_supported_codec_capabilities();
    /*bool supports_read_local_supported_controller_delay =
      controller_get_interface()->supports_read_local_supported_controller_delay();*/
    bool supports_configure_data_path =
        controller_get_interface()->supports_configure_data_path();

    if (codec_location == LeAudioCodecLocation::CONTROLLER &&
        (!supports_read_local_supported_codecs_v2 ||
         !supports_read_local_supported_codec_capabilities ||
         //! supports_read_local_supported_controller_delay ||
         !supports_configure_data_path)) {
      LOG(WARNING) << __func__
                   << ", required commands not supported by Controller";
      return false;
    } else if (codec_location == LeAudioCodecLocation::ADSP &&
               !supports_configure_data_path) {
      LOG(WARNING) << __func__
                   << ", required commands not supported by Controller";
      return false;
    }

    return true;
  }

  bool IsCodecSupported(const struct LeAudioCodecId codec_id,
                        const LeAudioCodecLocation codec_location) {
    if (codec_location == LeAudioCodecLocation::CONTROLLER) {
      if (codec_id.coding_format == kLeAudioCodingFormatVendorSpecific) {
        for (struct supported_vendor_spec_codecs_t& codec :
             codec_offload_.vendor_spec_codecs) {
          if (codec_id.vendor_company_id == codec.vendor_company_id &&
              codec_id.vendor_codec_id == codec.vendor_codec_id) {
            LOG(INFO) << __func__ << ", matching vendor_codec found";
            return true;
          }
        }
      } else {
        for (struct supported_standard_codecs_t& codec :
             codec_offload_.standard_codecs) {
          if (codec_id.coding_format == codec.codec_id) {
            LOG(INFO) << __func__ << ", matching standard_codec found";
            return true;
          }
        }
      }
    } else if (codec_location == LeAudioCodecLocation::ADSP) {
      /* TODO: Need to handle ADSP Codec check */
      return true;
    } else {
      LOG(WARNING) << __func__ << ", matching codec not found";
    }

    return false;
  }

  bool IsCodecConfSupported(const uint8_t direction,
                            const struct LeAudioCodecId codec_id,
                            const LeAudioLc3Config& lc3_config,
                            const LeAudioCodecLocation codec_location) {
    if (codec_location == LeAudioCodecLocation::CONTROLLER) {
      std::vector<struct codec_offload_caps> caps;
      caps = (direction == kLeAudioDirectionSink)
                 ? (this->codec_offload_.sink_caps)
                 : (this->codec_offload_.source_caps);

      for (struct codec_offload_caps& codec_offload_caps_record : caps) {
        if (codec_offload_caps_record.codec_id == codec_id) {
          for (LeAudioLtvMap& codec_spec_caps_record :
               codec_offload_caps_record.codec_spec_caps) {
            if (codec_id.coding_format == kLeAudioCodingFormatLC3 &&
                le_audio::set_configurations::IsCodecConfigurationSupported(
                    codec_spec_caps_record, lc3_config)) {
              LOG(INFO) << __func__ << ", matching codec capabilities found";
              return true;
            }
          }
        }
      }
    } else if (codec_location == LeAudioCodecLocation::ADSP) {
      /* TODO: Need to handle ADSP Codec Capabilities check */
      return true;
    } else {
      LOG(WARNING) << __func__ << ", matching codec capabilities not found";
    }

    return false;
  }

  void HandleLocalSupportedCodecsRead(
      std::vector<struct supported_standard_codecs_t>& standard_codecs,
      std::vector<struct supported_vendor_spec_codecs_t>& vendor_spec_codecs) {
    this->codec_offload_.standard_codecs.reserve(standard_codecs.size());
    for (struct supported_standard_codecs_t& codec : standard_codecs) {
      this->codec_offload_.standard_codecs.push_back(codec);
    }

    this->codec_offload_.vendor_spec_codecs.reserve(vendor_spec_codecs.size());
    for (struct supported_vendor_spec_codecs_t& codec : vendor_spec_codecs) {
      this->codec_offload_.vendor_spec_codecs.push_back(codec);
    }
  }

  bool HandleLocalSupportedCodecCapabilitiesRead(
      uint8_t direction, struct LeAudioCodecId codec_id,
      std::vector<uint8_t>& codec_caps_len, uint8_t* codec_caps) {
    struct codec_offload_caps codec_offload_caps_record;
    LeAudioLtvMap codec_spec_caps_record;
    std::vector<struct codec_offload_caps>* caps;

    caps = (direction == kLeAudioDirectionSink)
               ? &(this->codec_offload_.sink_caps)
               : &(this->codec_offload_.source_caps);

    codec_offload_caps_record.codec_id = codec_id;

    for (uint8_t& codec_spec_caps_len : codec_caps_len) {
      bool parsed;

      codec_spec_caps_record =
          LeAudioLtvMap::Parse(codec_caps, codec_spec_caps_len, parsed);

      if (!parsed) return false;

      codec_caps += codec_spec_caps_len;

      codec_offload_caps_record.codec_spec_caps.push_back(
          std::move(codec_spec_caps_record));
    }

    caps->push_back(std::move(codec_offload_caps_record));

    return true;
  }

  void HandleStateError(void) {
    CodecOffloadState curr_state = this->codec_offload_.offload_state.state;

    LOG(INFO) << __func__ << ", curr_state: " << static_cast<int>(curr_state);

    this->codec_offload_ = {};
    this->codec_offload_.offload_state.state = CodecOffloadState::IDLE;

    codec_offload_callbacks_->StatusCb(le_audio::CODEC_OFFLOAD_STATUS_FAIL);
  }

  void GetNextState(void) {
    CodecOffloadState curr_state = this->codec_offload_.offload_state.state;
    CodecOffloadState next_state = (CodecOffloadState)((int)(curr_state) + 1);
    uint8_t curr_dir = this->codec_offload_.offload_state.direction;
    uint8_t next_dir = kLeAudioDirectionSink;
    uint8_t curr_codec_index = this->codec_offload_.offload_state.codec_index;
    uint8_t next_codec_index = 0;

    if (curr_state ==
        CodecOffloadState::LOCAL_SUPPORTED_CODEC_CAPABILITIES_READ) {
      if ((curr_codec_index + 1) >=
          (sizeof(supported_codecs) / sizeof(supported_codecs[0]))) {
        if (curr_dir == kLeAudioDirectionSink) {
          next_state = curr_state;
          next_dir = kLeAudioDirectionSource;
        }
      } else {
        next_state = curr_state;
        next_dir = curr_dir;
        next_codec_index = curr_codec_index + 1;
      }
    }

    this->codec_offload_.offload_state.state = next_state;
    this->codec_offload_.offload_state.direction = next_dir;
    this->codec_offload_.offload_state.codec_index = next_codec_index;

    LOG(INFO) << __func__ << ", next_state: " << static_cast<int>(next_state)
              << " next_dir: " << loghex(next_dir)
              << " next_codec_index: " << loghex(next_codec_index);
  }

  void HandleState(void) {
    CodecOffloadState next_state;
    uint8_t next_dir, next_codec_index, data_path_dir;
    struct LeAudioCodecId codec_id;

    GetNextState();

    next_state = this->codec_offload_.offload_state.state;
    next_dir = this->codec_offload_.offload_state.direction;
    next_codec_index = this->codec_offload_.offload_state.codec_index;

    codec_id = supported_codecs[next_codec_index];

    data_path_dir =
        ((next_dir == kLeAudioDirectionSink)
             ? bluetooth::hci::iso_manager::kIsoDataPathDirectionIn
             : bluetooth::hci::iso_manager::kIsoDataPathDirectionOut);

    if (next_state == CodecOffloadState::LOCAL_SUPPORTED_CODECS_READ) {
      IsoManager::GetInstance()->ReadLocalSupportedCodecs();
    } else if (next_state ==
               CodecOffloadState::LOCAL_SUPPORTED_CODEC_CAPABILITIES_READ) {
      bluetooth::hci::iso_manager::read_supp_codec_caps_params param = {
          .codec_id_format = codec_id.coding_format,
          .codec_id_company = codec_id.vendor_company_id,
          .codec_id_vendor = codec_id.vendor_codec_id,
          .logical_transport_type =
              bluetooth::hci::iso_manager::kIsoLogicalTransportTypeLeCis,
          .direction = data_path_dir,
      };

      IsoManager::GetInstance()->ReadLocalSupportedCodecCapabilities(
          std::move(param));

    } else if (next_state == CodecOffloadState::READY) {
      LOG(INFO) << __func__ << ", ready";
      codec_offload_callbacks_->StatusCb(
          le_audio::CODEC_OFFLOAD_STATUS_SUCCESS);
    }
  }

  bool IsRequiredCommandsSupported() {
    LeAudioCodecLocation codec_location = this->codec_offload_.codec_location;

    return (IsCodecOffloadCommandsSupported(codec_location));
  }

  void InitConfig(void) {
    CodecOffloadState curr_state = this->codec_offload_.offload_state.state;

    LOG(INFO) << __func__ << ", curr_state: " << static_cast<int>(curr_state);

    if (curr_state == CodecOffloadState::IDLE) {
      this->codec_offload_.codec_location = GetCodecLocation();
      this->codec_offload_.context_types =
          GetCodecOffloadSupportedContxtTypes();

      LeAudioCodecLocation codec_location = this->codec_offload_.codec_location;

      if (!IsCodecOffloadCommandsSupported(codec_location)) {
        return;
      }

      if (codec_location == LeAudioCodecLocation::CONTROLLER) {
        /* Get Controller supported Codecs and Codec Capabilities */
        HandleState();
      } else if (codec_location == LeAudioCodecLocation::ADSP) {
        /* TODO: Get ADSP supported Codecs and Codec Capabilities */
        this->codec_offload_.offload_state.state = CodecOffloadState::READY;
        LOG(INFO) << __func__ << ", ready";
        codec_offload_callbacks_->StatusCb(
            le_audio::CODEC_OFFLOAD_STATUS_SUCCESS);
      } else {
        LOG(WARNING) << __func__ << ", invalid mode for offload";
      }
    } else {
      LOG(WARNING) << __func__ << ", invalid state";
    }
  }

  LeAudioCodecLocation GetMode() {
    return (this->codec_offload_.codec_location);
  }

  bool IsEnabled(const uint16_t context_type) {
    uint16_t context_types = this->codec_offload_.context_types;

    if (!(context_types & context_type)) {
      LOG(WARNING) << __func__ << ", offload not enabled, context_type: "
                   << loghex(context_type);
      return false;
    }

    return true;
  }

  bool IsReady(void) {
    CodecOffloadState curr_state = this->codec_offload_.offload_state.state;

    LOG(INFO) << __func__ << ", curr_state: " << static_cast<int>(curr_state);

    return ((curr_state == CodecOffloadState::READY) ? true : false);
  }

  bool IsSupported(const uint8_t direction,
                   const struct LeAudioCodecId codec_id,
                   const LeAudioLc3Config& lc3_config) {
    CodecOffloadState curr_state = this->codec_offload_.offload_state.state;

    LeAudioCodecLocation codec_location = this->codec_offload_.codec_location;

    if (curr_state != CodecOffloadState::READY) {
      LOG(WARNING) << __func__ << ", not ready";
    }

    if (!(IsCodecSupported(codec_id, codec_location))) {
      return false;
    }

    if (!(IsCodecConfSupported(direction, codec_id, lc3_config,
                               codec_location))) {
      return false;
    }

    return true;
  }

  void ProcessHciNotifLocalSupportedCodecsRead(
      uint8_t status,
      std::vector<struct supported_standard_codecs_t>& standard_codecs,
      std::vector<struct supported_vendor_spec_codecs_t>& vendor_spec_codecs)
      override {
    LOG(INFO) << __func__ << ", status: " << loghex(status);

    if (status != HCI_SUCCESS) {
      HandleStateError();
      return;
    }

    HandleLocalSupportedCodecsRead(standard_codecs, vendor_spec_codecs);

    HandleState();
  }

  void ProcessHciNotifLocalSupportedCodecCapabilitiesRead(
      uint8_t status, std::vector<uint8_t>& codec_caps_len,
      uint8_t* codec_caps) override {
    uint8_t direction = this->codec_offload_.offload_state.direction;
    uint8_t codec_index = this->codec_offload_.offload_state.codec_index;
    struct LeAudioCodecId codec_id = supported_codecs[codec_index];

    LOG(INFO) << __func__ << ", status: " << loghex(status);

    if ((status != HCI_SUCCESS) ||
        (!HandleLocalSupportedCodecCapabilitiesRead(
            direction, codec_id, codec_caps_len, codec_caps))) {
      HandleStateError();
      return;
    }

    HandleState();
  }

  void ProcessHciNotifLocalSupportedControllerDelayRead(
      uint8_t status, uint32_t min_controller_delay,
      uint32_t max_controller_delay) override {
    LOG(INFO) << __func__ << ", status: " << loghex(status);
  }

 private:
  Callbacks* codec_offload_callbacks_;
};
}  // namespace

namespace le_audio {
void LeAudioCodecOffload::Initialize(Callbacks* codec_offload_callbacks_) {
  if (instance) {
    LOG(ERROR) << "Already initialized";
    return;
  }

  instance = new LeAudioCodecOffloadImpl(codec_offload_callbacks_);
}

void LeAudioCodecOffload::Cleanup() {
  if (!instance) return;

  LeAudioCodecOffloadImpl* ptr = instance;
  instance = nullptr;

  delete ptr;
}

LeAudioCodecOffload* LeAudioCodecOffload::Get() {
  CHECK(instance);
  return instance;
}
}  // namespace le_audio