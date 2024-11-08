/*
 * Copyright 2025 The Android Open Source Project
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
#ifdef __ANDROID__

#include <perfetto/trace/android/bluetooth_trace.pbzero.h>
#include <perfetto/tracing.h>

#include "hal/snoop_logger.h"

namespace bluetooth {
namespace hal {
class SnoopLoggerTracing : public perfetto::DataSource<SnoopLoggerTracing> {
public:
  static void InitializePerfetto();
  static void TracePacket(BundleKey key, BundleDetails details);
  static perfetto::protos::pbzero::BluetoothTracePacketType HciToTracePacketType(
          SnoopLogger::PacketType hci_packet_type, SnoopLogger::Direction direction);

  void OnSetup(const SetupArgs&) override;
  void OnStart(const StartArgs&) override;
  void OnStop(const StopArgs&) override;
  void OnFlush(const FlushArgs&) override;
};
}  // namespace hal
}  // namespace bluetooth

PERFETTO_DECLARE_DATA_SOURCE_STATIC_MEMBERS(bluetooth::hal::SnoopLoggerTracing);

#endif //__ANDROID__
