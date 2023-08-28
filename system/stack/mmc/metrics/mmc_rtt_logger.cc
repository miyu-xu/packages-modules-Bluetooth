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

#include "mmc/metrics/mmc_rtt_logger.h"

#include <base/logging.h>

#include <algorithm>
#include <cmath>
#include <string>

#include "stack/include/stack_metrics_logging.h"

namespace mmc {
namespace {
// Record size limitation.
const int kMaximumRecords = 1000000;
}  // namespace

MmcRttLogger::MmcRttLogger(std::string codec_type) : codec_type_(codec_type) {
  record_.clear();
}

MmcRttLogger::~MmcRttLogger() { record_.clear(); }

void MmcRttLogger::RecordRtt(int64_t elapsed_time) {
  if (elapsed_time <= 0) return;
  record_.push_back(elapsed_time);

  // When reaching size limitation, the record will be uploaded and cleaned up.
  if (record_.size() >= kMaximumRecords) {
    UploadTranscodeRttStatics();
  }
  return;
}

void MmcRttLogger::UploadTranscodeRttStatics() {
  if (record_.empty()) return;
  std::sort(record_.begin(), record_.end());
  int num_requests = record_.size();
  int maximum_rtt = record_.back();
  int median_rtt = record_[num_requests / 2];
  double mean_rtt = ComputeMeanRtt();
  double std_dev_rtt = ComputeStdDevRtt(mean_rtt);
  log_mmc_transcode_rtt_stats(maximum_rtt, mean_rtt, std_dev_rtt, median_rtt,
                              num_requests, codec_type_);
  record_.clear();
  return;
}

double MmcRttLogger::ComputeMeanRtt() {
  double total_rtt = 0;
  for (auto rtt : record_) {
    total_rtt += rtt;
  }
  return total_rtt / (int)record_.size();
}

double MmcRttLogger::ComputeStdDevRtt(double mean_rtt) {
  double total_variance = 0;
  for (auto rtt : record_) {
    total_variance += (rtt - mean_rtt) * (rtt - mean_rtt);
  }
  return std::sqrt(total_variance / (int)record_.size());
}

}  // namespace mmc
