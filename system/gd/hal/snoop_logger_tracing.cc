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

#ifdef __ANDROID__
#include "hal/snoop_logger_tracing.h"

#include <bluetooth/log.h>
#include <perfetto/trace/android/bluetooth_trace.pbzero.h>
#include <perfetto/tracing.h>

#include "hal/snoop_logger.h"
#include "hci/hci_packets.h"

PERFETTO_DEFINE_DATA_SOURCE_STATIC_MEMBERS(bluetooth::hal::SnoopLoggerTracing);

using perfetto::protos::pbzero::BluetoothTracePacketType;

namespace bluetooth {
namespace hal {
void SnoopLoggerTracing::InitializePerfetto() {
  perfetto::TracingInitArgs args;
  args.backends |= perfetto::kSystemBackend;

  perfetto::Tracing::Initialize(args);
  perfetto::DataSourceDescriptor dsd;
  dsd.set_name("android.bluetooth_tracing");
  SnoopLoggerTracing::Register(dsd);
}

BluetoothTracePacketType SnoopLoggerTracing::HciToTracePacketType(
        SnoopLogger::PacketType hci_packet_type, SnoopLogger::Direction direction) {
  BluetoothTracePacketType trace_packet_type;
  switch (hci_packet_type) {
    case SnoopLogger::PacketType::CMD: {
      trace_packet_type = BluetoothTracePacketType::HCI_CMD;
    } break;
    case SnoopLogger::PacketType::EVT: {
      trace_packet_type = BluetoothTracePacketType::HCI_EVT;
    } break;
    case SnoopLogger::PacketType::ACL: {
      if (direction == SnoopLogger::INCOMING) {
        trace_packet_type = BluetoothTracePacketType::HCI_ACL_RX;
      } else {
        trace_packet_type = BluetoothTracePacketType::HCI_ACL_TX;
      }
    } break;
    case SnoopLogger::PacketType::ISO: {
      if (direction == SnoopLogger::INCOMING) {
        trace_packet_type = BluetoothTracePacketType::HCI_ISO_RX;
      } else {
        trace_packet_type = BluetoothTracePacketType::HCI_ISO_TX;
      }
    } break;
    case SnoopLogger::PacketType::SCO: {
      if (direction == SnoopLogger::INCOMING) {
        trace_packet_type = BluetoothTracePacketType::HCI_SCO_RX;
      } else {
        trace_packet_type = BluetoothTracePacketType::HCI_SCO_TX;
      }
    } break;
  }
  return trace_packet_type;
}

void SnoopLoggerTracing::TracePacket(BundleKey key, BundleDetails details) {
  SnoopLoggerTracing::Trace(
          [&key, &details](SnoopLoggerTracing::TraceContext ctx) {
            auto trace_pkt = ctx.NewTracePacket();
            trace_pkt->set_timestamp(perfetto::base::GetBootTimeNs().count());
            auto* bt_event = trace_pkt->set_bluetooth_trace_event();
            bt_event->set_packet_type(
                    HciToTracePacketType(static_cast<SnoopLogger::PacketType>(key.packet_type),
                                         static_cast<SnoopLogger::Direction>(key.direction)));
            bt_event->set_count(details.count);
            bt_event->set_length(details.total_length);
            bt_event->set_duration((details.end_ts - details.start_ts) / 1000);
            if (key.op_code.has_value()) {
              bt_event->set_op_code(*key.op_code);
            }
            if (key.event_code.has_value()) {
              bt_event->set_event_code(*key.event_code);
            }
            if (key.subevent_code.has_value()) {
              bt_event->set_subevent_code(*key.subevent_code);
            }
            if (key.handle.has_value()) {
              bt_event->set_connection_handle(*key.handle);
            }
          });
}

void SnoopLoggerTracing::OnSetup(const SetupArgs&) {}
void SnoopLoggerTracing::OnStart(const StartArgs&) {}
void SnoopLoggerTracing::OnStop(const StopArgs&) {}
void SnoopLoggerTracing::OnFlush(const FlushArgs&) {}

}  // namespace hal
}  // namespace bluetooth
#endif //__ANDROID__