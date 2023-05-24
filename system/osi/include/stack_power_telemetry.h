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

#pragma once

#include <base/logging.h>

#include <cstdint>
#include <memory>

#include "types/raw_address.h"

namespace power_telemetry {

struct PowerTelemetryImpl;

class PowerTelemetry {
 public:
  PowerTelemetry();
  virtual ~PowerTelemetry();

  virtual void LogScanStarted();

  virtual void LogHciCmdDetail();
  virtual void LogHciEvtDetail();

  virtual void LogLinkDetails(uint16_t handle, const RawAddress& bdaddr,
                              bool isConnected, bool is_acl_link);
  virtual void LogRxAclPktData(uint16_t len);
  virtual void LogTxAclPktData(uint16_t len);

  virtual void LogChannelConnected(uint16_t psm, int32_t src_id, int32_t dst_id,
                                   const RawAddress& bd_addr);
  virtual void LogChannelDisconnected(uint16_t psm, int32_t src_id,
                                      int32_t dst_id,
                                      const RawAddress& bd_addr);
  virtual void LogRxBytes(uint16_t psm, int32_t src_id, int32_t dst_id,
                          const RawAddress& bd_addr, int32_t num_bytes);
  virtual void LogTxBytes(uint16_t psm, int32_t src_id, int32_t dst_id,
                          const RawAddress& bd_addr, int32_t num_bytes);

  virtual void LogSniffStarted(uint16_t handle, const RawAddress& bdaddr);
  virtual void LogSniffStopped(uint16_t handle, const RawAddress& bdaddr);

  virtual void LogInqScanStarted();
  virtual void LogInqScanStopped();
  virtual void LogBleAdvStarted();
  virtual void LogBleAdvStopped();

  virtual void LogTxPower(void* res);

  virtual void Dumpsys(int32_t fd);

 protected:
  std::unique_ptr<PowerTelemetryImpl> pimpl_;
};

PowerTelemetry& GetInstance();

}  // namespace power_telemetry
