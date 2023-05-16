/*
 * Copyright 2023 The Android Open Source Project
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

#include <featured/c_feature_library.h>

const int FEATURE_LIBRARY_TIMEOUT_MS = 500;

namespace bluetooth {
namespace os {
bool get_feature_enabled(const char* feature_name) {
  const struct VariationsFeature featured_feature = {
      .name = feature_name,
      .default_state = FEATURE_DISABLED_BY_DEFAULT,
  };
  CFeatureLibrary lib = CFeatureLibraryNew();
  int enabled = CFeatureLibraryIsEnabledBlockingWithTimeout(
      lib, &featured_feature, FEATURE_LIBRARY_TIMEOUT_MS);
  CFeatureLibraryDelete(lib);
  return enabled;
}
}  // namespace os
}  // namespace bluetooth
