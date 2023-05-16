/*
 * Copyright 2024 The Android Open Source Project
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

#include <bluetooth/log.h>
#include <featured/c_feature_library.h>

const int FEATURE_LIBRARY_TIMEOUT_MS = 500;

class CFeatureLibraryWrapper {
 public:
  CFeatureLibraryWrapper() {
    if (!CFeatureLibraryInitialize()) {
      bluetooth::log::warn("Cannot initialize CFeatureLibrary, all results will be false.");
      return;
    }

    initialized_ = true;
    lib_ = CFeatureLibraryGet();
  }

  bool is_feature_enabled(const char* feature_name) {
    if (!initialized_) {
      return false;
    }

    const struct VariationsFeature featured_feature = {
        .name = feature_name,
        .default_state = FEATURE_DISABLED_BY_DEFAULT,
    };

    bool enabled = CFeatureLibraryIsEnabledBlockingWithTimeout(
        lib_, &featured_feature, FEATURE_LIBRARY_TIMEOUT_MS);

    bluetooth::log::debug("is_feature_enabled({}) -> {}", feature_name, enabled);

    return enabled;
  }

 private:
  bool initialized_{};
  CFeatureLibrary lib_;
};

static class CFeatureLibraryWrapper* c_feature_lib;

namespace bluetooth {
namespace os {
bool get_feature_enabled(const char* feature_name) {
  if (!c_feature_lib) {
    c_feature_lib = new CFeatureLibraryWrapper();
  }

  return c_feature_lib->is_feature_enabled(feature_name);
}
}  // namespace os
}  // namespace bluetooth
