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
#include <base/unguessable_token.h>

#include <algorithm>
#include <cmath>
#include <fstream>
#include <string>

namespace mmc {
namespace {
// Record size limitation.
const int kMaximumRecords = 1000000;
}  // namespace

MmcRttLogger::MmcRttLogger(std::string codec_type) : codec_type_(codec_type) {
  record_.clear();
  // Avoid file name collision.
  out.open("/tmp/mmc_" + codec_type_ +
           base::UnguessableToken::Create().ToString() + ".txt");
}

MmcRttLogger::~MmcRttLogger() {
  record_.clear();
  out.close();
}

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
  for (auto rtt : record_) {
    out << rtt << "\n";
  }
  int num_requests = record_.size();
  int maximum_rtt = record_.back();
  double mean_rtt = ComputeMeanRtt();
  LOG(INFO) << "MmcStats: " << codec_type_ << " " << mean_rtt << " "
            << num_requests << " " << maximum_rtt;
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

}  // namespace mmc
