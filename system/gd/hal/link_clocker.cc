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

#include "hal/link_clocker.h"

#include <base/bind_helpers.h>

#include <algorithm>

#include "common/repeating_timer.h"
#include "hci/hci_layer.h"
#include "main/shim/stack.h"

namespace bluetooth::hal {

static class : public ReadClockHandler {
  void OnEvent(uint32_t, uint32_t) override {}
} g_empty_handler;

static std::atomic<ReadClockHandler*> g_read_clock_handler = &g_empty_handler;

void LinkClocker::Register(ReadClockHandler* handler) {
  g_read_clock_handler = handler;
}

void LinkClocker::Unregister() {
  g_read_clock_handler = &g_empty_handler;
}

void LinkClocker::OnHciEvent(const HciPacket& packet) {
  const int HCI_CMD_READ_CLOCK = 0x1407;
  const int HCI_EVT_COMMAND_COMPLETE = 0x0e;

  // HCI Event [Core 4.E.5.4.4]
  // |  [0]  Event Code
  // |  [1]  Parameter Total Length
  // | [2+]  Parameters

  if (packet.size() < 2) return;

  const uint8_t* payload = packet.data() + 2;
  size_t payload_length = std::min(size_t(packet[1]), packet.size() - 2);
  int event_code = packet[0];

  if (event_code != HCI_EVT_COMMAND_COMPLETE) return;

  // HCI Command Complete Event [Core 4.E.7.7.14]
  // |    [0]  Num_HCI_Command_Packets, Ignored
  // | [1..2]  Command_Opcode, catch `HCI_LE_Set_CIG_Parameters`
  // |   [3+]  Return Parameters

  if (payload_length < 3) return;

  uint16_t op_code = payload[1] | (payload[2] << 8);
  const uint8_t* parameters = payload + 3;
  size_t parameters_length = payload_length - 3;

  if (op_code != HCI_CMD_READ_CLOCK) return;

  // HCI Read Clock return parameters [Core 4.E.7.5.6]
  // |    [0]  Status, 0 when OK
  // | [1..2]  Connection_handle
  // | [3..7]  Clock
  // | [8..9]  Accuracy

  if (parameters_length < 9) return;

  uint8_t status = parameters[0];

  if (status != 0) return;

  uint32_t local_clock = ((uint32_t)parameters[3] << 0) | ((uint32_t)parameters[4] << 8) |
                         ((uint32_t)parameters[5] << 16) | ((uint32_t)parameters[6] << 24);
  local_clock &= 0x0fffffffu;

  // The connection handle is ignored as we are reading the local clock
  // (Which_Clock parameter is 0).
  // The reason the read clock measurement is extracted here is that
  // getting the local timestamp from the bound gd HCI event callback
  // adds a lot of jitter.

  auto timestamp = std::chrono::system_clock::now().time_since_epoch();
  unsigned timestamp_us = std::chrono::duration_cast<std::chrono::microseconds>(timestamp).count();

  LOG_INFO("YAYAYA %u %u", timestamp_us, local_clock);

  (*g_read_clock_handler).OnEvent(timestamp_us, local_clock);
}

/// Filter received L2CAP PDUs for Credit acknowledgments for the registered
/// L2CAP channels.
void LinkClocker::OnAclDataReceived(const HciPacket& /*packet*/) {}

const ModuleFactory LinkClocker::Factory = ModuleFactory([]() { return new LinkClocker(); });

}  // namespace bluetooth::hal
