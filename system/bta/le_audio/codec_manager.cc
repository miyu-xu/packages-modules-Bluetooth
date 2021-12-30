#include "codec_manager.h"

//#include "bta_le_audio_api.h"
#include "osi/include/log.h"
#include "osi/include/properties.h"
#include "stack/acl/acl.h"
#include "stack/include/acl_api.h"
//#include "stack/include/btm_iso_api_types.h"
//#include "client_audio.h"
#include "device/include/controller.h"

namespace {

using bluetooth::hci::iso_manager::kIsoDataPathHci;
using bluetooth::hci::iso_manager::kIsoDataPathPlatformDefault;
using le_audio::CodecManager;
using le_audio::types::CodecLocation;
}  // namespace

namespace le_audio {

struct codec_manager_impl {
 public:
  codec_manager_impl() {
    offload_enable_ = osi_property_get_bool(
                          "ro.bluetooth.leaudio_offload.supported", false) &&
                      osi_property_get_bool(
                          "persist.bluetooth.leaudio_offload.enabled", true);
    if (offload_enable_ == false) {
      LOG_INFO("offload disabled");
      return;
    }

    if (!LeAudioHalVerifier::SupportsLeAudioHardwareOffload()) {
      LOG_WARN("HAL not uspport hardware offload");
      return;
    }

    if (!controller_get_interface()->supports_configure_data_path()) {
      LOG_WARN("Controller does not support config data path command");
      return;
    }

    LOG_INFO("LeAudioCodecManagerImpl: configure_data_path for encode");
    btm_configure_data_path(btm_data_direction::HOST_TO_CONTROLLER,
                            kIsoDataPathPlatformDefault, {});
    SetCodecLocation(CodecLocation::ADSP);
  }
  ~codec_manager_impl() {
    if (GetCodecLocation() != CodecLocation::HOST) {
      btm_configure_data_path(btm_data_direction::HOST_TO_CONTROLLER,
                              kIsoDataPathHci, {});
    }
  }
  CodecLocation GetCodecLocation(void) const { return codec_location_; }

 private:
  void SetCodecLocation(CodecLocation location) { codec_location_ = location; }
  CodecLocation codec_location_ = CodecLocation::HOST;
  bool offload_enable_ = false;
};

struct CodecManager::impl {
  impl(const CodecManager& codec_manager) : codec_manager_(codec_manager) {}

  void Start() {
    LOG_ASSERT(!codec_manager_impl_);
    codec_manager_impl_ = std::make_unique<codec_manager_impl>();
  }

  void Stop() {
    LOG_ASSERT(codec_manager_impl_);
    codec_manager_impl_.reset();
  }

  bool IsRunning() { return codec_manager_impl_ ? true : false; }

  const CodecManager& codec_manager_;
  std::unique_ptr<codec_manager_impl> codec_manager_impl_;
};

CodecManager::CodecManager() : pimpl_(std::make_unique<impl>(*this)) {}

void CodecManager::Start() {
  if (!pimpl_->IsRunning()) pimpl_->Start();
}

void CodecManager::Stop() {
  if (pimpl_->IsRunning()) pimpl_->Stop();
}

types::CodecLocation CodecManager::GetCodecLocation(void) const {
  if (!pimpl_->IsRunning()) {
    return CodecLocation::HOST;
  }

  return pimpl_->codec_manager_impl_->GetCodecLocation();
}

}  // namespace le_audio
