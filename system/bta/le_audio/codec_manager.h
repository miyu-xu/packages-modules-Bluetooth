#pragma once

#include "le_audio_types.h"

namespace le_audio {

class CodecManager {
 public:
  CodecManager();
  virtual ~CodecManager() = default;
  static CodecManager* GetInstance(void) {
    static CodecManager* instance = new CodecManager();
    return instance;
  }
  void Start(void);
  void Stop(void);
  virtual types::CodecLocation GetCodecLocation(void) const;

 private:
  struct impl;
  std::unique_ptr<impl> pimpl_;
};
}  // namespace le_audio
