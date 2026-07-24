/*
 * Copyright 2026 The Android Open Source Project
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

#include <cstddef>
#include <cstdint>
#include <memory>
#include <vector>

#include "hci/address.h"
#include "model/controller/dual_mode_controller.h"
#include "packets/link_layer_packets.h"

namespace {

using HciCallback = void (*)(void*, int, const uint8_t*, size_t);
using LinkLayerCallback = void (*)(void*, const uint8_t*, size_t, int, int);

struct WindowsController {
  WindowsController(const uint8_t address[6], void* context, HciCallback hci_callback,
                    LinkLayerCallback link_layer_callback)
      : context(context), hci_callback(hci_callback), link_layer_callback(link_layer_callback) {
    controller.SetAddress(bluetooth::hci::Address(
            {address[0], address[1], address[2], address[3], address[4], address[5]}));
    controller.RegisterEventChannel(
            [this](std::shared_ptr<std::vector<uint8_t>> packet) { SendHci(4, *packet); });
    controller.RegisterAclChannel(
            [this](std::shared_ptr<std::vector<uint8_t>> packet) { SendHci(2, *packet); });
    controller.RegisterScoChannel(
            [this](std::shared_ptr<std::vector<uint8_t>> packet) { SendHci(3, *packet); });
    controller.RegisterIsoChannel(
            [this](std::shared_ptr<std::vector<uint8_t>> packet) { SendHci(5, *packet); });
    controller.RegisterLinkLayerChannel(
            [this](const std::vector<uint8_t>& packet, rootcanal::Phy::Type phy, int8_t tx_power) {
              this->link_layer_callback(this->context, packet.data(), packet.size(),
                                        static_cast<int>(phy), tx_power);
            });
  }

  void SendHci(int packet_type, const std::vector<uint8_t>& packet) const {
    hci_callback(context, packet_type, packet.data(), packet.size());
  }

  rootcanal::DualModeController controller;
  void* context;
  HciCallback hci_callback;
  LinkLayerCallback link_layer_callback;
};

}  // namespace

extern "C" {

__declspec(dllexport) void* rootcanal_controller_new(const uint8_t address[6], void* context,
                                                     HciCallback hci_callback,
                                                     LinkLayerCallback link_layer_callback) {
  return new WindowsController(address, context, hci_callback, link_layer_callback);
}

__declspec(dllexport) void rootcanal_controller_delete(void* controller) {
  delete static_cast<WindowsController*>(controller);
}

__declspec(dllexport) void rootcanal_controller_receive_hci(void* controller, int packet_type,
                                                            const uint8_t* data, size_t size) {
  auto* instance = static_cast<WindowsController*>(controller);
  auto packet = std::make_shared<std::vector<uint8_t>>(data, data + size);
  switch (packet_type) {
    case 1:
      instance->controller.HandleCommand(packet);
      break;
    case 2:
      instance->controller.HandleAcl(packet);
      break;
    case 3:
      instance->controller.HandleSco(packet);
      break;
    case 5:
      instance->controller.HandleIso(packet);
      break;
    default:
      break;
  }
}

__declspec(dllexport) void rootcanal_controller_receive_ll(void* controller, const uint8_t* data,
                                                           size_t size, int phy, int rssi) {
  auto* instance = static_cast<WindowsController*>(controller);
  auto bytes = std::make_shared<std::vector<uint8_t>>(data, data + size);
  auto packet = model::packets::LinkLayerPacketView::Create(pdl::packet::slice(bytes));
  if (packet.IsValid()) {
    instance->controller.ReceiveLinkLayerPacket(packet, rootcanal::Phy::Type(phy),
                                                static_cast<int8_t>(rssi));
  }
}

__declspec(dllexport) void rootcanal_controller_tick(void* controller) {
  static_cast<WindowsController*>(controller)->controller.Tick();
}

}  // extern "C"
