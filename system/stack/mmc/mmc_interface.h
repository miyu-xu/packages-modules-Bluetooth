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

#ifndef MMC_MMC_INTERFACE_H_
#define MMC_MMC_INTERFACE_H_

#include <stdint.h>

#include "mmc/proto/mmc_config.pb.h"

namespace mmc {

// the abstract interface provides basic functionalities of codec libraries
class MmcInterface {
 public:
  virtual ~MmcInterface() = default;
  virtual void init(ConfigParam config) = 0;
  virtual void cleanup() = 0;
  virtual bool codec(uint8_t* i_buf, int i_len, uint8_t* o_buf, int o_len) = 0;
};

}  // namespace mmc

#endif  // MMC_MMC_INTERFACE_H_
