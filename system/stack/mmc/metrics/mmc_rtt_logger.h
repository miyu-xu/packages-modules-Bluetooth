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

#ifndef MMC_METRICS_MMC_RTT_LOGGER_H_
#define MMC_METRICS_MMC_RTT_LOGGER_H_

#include <cstdint>
#include <fstream>
#include <string>
#include <vector>

namespace mmc {

class MmcRttLogger {
 public:
  explicit MmcRttLogger(std::string codec_type);
  ~MmcRttLogger();

  // MmcRttLogger is neither copyable nor movable.
  MmcRttLogger(const MmcRttLogger&) = delete;
  MmcRttLogger& operator=(const MmcRttLogger&) = delete;

  // Records elapsed_time (in microseconds).
  // Elapsed time should be positive, otherwise it won't be recorded.
  void RecordRtt(int64_t elapsed_time);

  // Computes transcode rtt statics, writes record to the file, and clears the
  // record. Empty record will be ignored.
  void UploadTranscodeRttStatics();

 private:
  double ComputeMeanRtt();
  std::vector<int64_t> record_;
  const std::string codec_type_;
  std::ofstream out;
};

}  // namespace mmc

#endif  // MMC_METRICS_MMC_RTT_LOGGER_H_
