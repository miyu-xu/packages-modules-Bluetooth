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

namespace bluetooth {
namespace os {

  bool get_feature_enabled(const char* feature_name) {
    const struct VariationsFeature featured_feature = {
      .name = feature_name,
      .default_state = FEATURE_DISABLED_BY_DEFAULT,
    };
    CFeatureLibrary lib = CFeatureLibraryNew();
    // TODO: set timeout (CL:4536315)
    int enabled = CFeatureLibraryIsEnabledBlocking(lib, &featured_feature);
    CFeatureLibraryDelete(lib);
    return enabled;
  }

}  // namespace os
}  // namespace bluetooth
