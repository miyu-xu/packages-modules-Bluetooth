/*
 * Copyright 2019 The Android Open Source Project
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

#include "hal/hci_hal.h"
#include "hal/link_clocker.h"
#include "hal/snoop_logger.h"
#include "module.h"

namespace bluetooth {
namespace hal {

class HciHalHost : public HciHal {
public:
  static const ModuleFactory Factory;

  virtual ~HciHalHost() = default;

  // Register the callback for incoming packets. All incoming packets are dropped before
  // this callback is registered. Callback can only be registered once.
  //
  // @param callback implements BluetoothHciHalCallbacks which will
  //    receive callbacks when incoming HCI packets are received
  //    from the controller to be sent to the host.
  void registerIncomingPacketCallback(HciHalCallbacks* callback) override;

  // Unregister the callback for incoming packets. Drop all further incoming packets.
  void unregisterIncomingPacketCallback() override;

  // Send an HCI command (as specified in the Bluetooth Specification
  // V4.2, Vol 2, Part 5, Section 5.4.1) to the Bluetooth controller.
  // Commands must be executed in order.
  void sendHciCommand(HciPacket command) override;

  // Send an HCI ACL data packet (as specified in the Bluetooth Specification
  // V4.2, Vol 2, Part 5, Section 5.4.2) to the Bluetooth controller.
  // Packets must be processed in order.
  void sendAclData(HciPacket data) override;

  // Send an SCO data packet (as specified in the Bluetooth Specification
  // V4.2, Vol 2, Part 5, Section 5.4.3) to the Bluetooth controller.
  // Packets must be processed in order.
  void sendScoData(HciPacket data) override;

  // Send an HCI ISO data packet (as specified in the Bluetooth Specification
  // V5.2, Vol 4, Part E, Section 5.4.5) to the Bluetooth controller.
  // Packets must be processed in order.
  void sendIsoData(HciPacket data) override;

  // Get the MSFT opcode (as specified in Microsoft-defined Bluetooth HCI
  // extensions)
  uint16_t getMsftOpcode() { return 0; }

protected:
  void ListDependencies(ModuleList* list) const override;
  void Start() override;
  void Stop() override;
  std::string ToString() const override;

private:
  void write_to_fd(HciPacket packet);
  void send_packet_ready();
  bool socketRecvAll(void* buffer, int bufferLen);
  void incoming_packet_received();

  // Held when APIs are called, NOT to be held during callbacks
  std::mutex api_mutex_;
  HciHalCallbacks* incoming_packet_callback_ = nullptr;
  std::mutex incoming_packet_callback_mutex_;
  int sock_fd_;
  bluetooth::os::Thread hci_incoming_thread_ =
          bluetooth::os::Thread("hci_incoming_thread", bluetooth::os::Thread::Priority::NORMAL);
  bluetooth::os::Reactor::Reactable* reactable_ = nullptr;
  std::queue<std::vector<uint8_t>> hci_outgoing_queue_;
  SnoopLogger* btsnoop_logger_ = nullptr;
  LinkClocker* link_clocker_ = nullptr;
};

}  // namespace hal
}  // namespace bluetooth
