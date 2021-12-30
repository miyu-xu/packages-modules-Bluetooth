#include "mock_codec_manager.h"

MockCodecManager* mock_codec_manager_pimpl_;
MockCodecManager* MockCodecManager::GetInstance() {
  le_audio::CodecManager::GetInstance();
  return mock_codec_manager_pimpl_;
}

namespace le_audio {

struct CodecManager::impl : public MockCodecManager {
 public:
  impl() = default;
  ~impl() = default;
};

CodecManager::CodecManager() {}

types::CodecLocation CodecManager::GetCodecLocation() const {
  if (!pimpl_) return types::CodecLocation::HOST;
  return pimpl_->GetCodecLocation();
}

void CodecManager::Start() {
  // It is needed here as CodecManager which is a singleton creates it, but in
  // this mock we want to destroy and recreate the mock on each test case.
  if (!pimpl_) {
    pimpl_ = std::make_unique<impl>();
  }

  mock_codec_manager_pimpl_ = pimpl_.get();
  pimpl_->Start();
}

void CodecManager::Stop() {
  // It is needed here as CodecManager which is a singleton creates it, but in
  // this mock we want to destroy and recreate the mock on each test case.
  if (pimpl_) {
    pimpl_->Stop();
    pimpl_.reset();
  }

  mock_codec_manager_pimpl_ = nullptr;
}

// CodecManager::~CodecManager() = default;

}  // namespace le_audio
