/*
 * Copyright 2022 The Android Open Source Project
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
#include "hci/msft.h"

#include "hci/controller.h"
#include "hci/hci_layer.h"
#include "hci/hci_packets.h"
#include "hci/mgmt.h"
#include "hci/msft_interface.h"

namespace bluetooth {
namespace hci {

// https://learn.microsoft.com/en-us/windows-hardware/drivers/bluetooth/
//         microsoft-defined-bluetooth-hci-commands-and-events
constexpr uint8_t kMsftEventPrefixLengthMax = 0x20;

struct Msft {
  // MSFT opcode needs to be configured from Bluetooth driver.
  std::optional<uint16_t> opcode;
  uint64_t features{0};
  uint8_t prefix_len{0};
  std::vector<uint8_t> prefix;
};

const ModuleFactory MsftExtensionManager::Factory = ModuleFactory([]() { return new MsftExtensionManager(); });

struct MsftExtensionManager::impl {
  impl(Module* module) : module_(module){};

  ~impl() {}

  void start(os::Handler* handler, hci::HciLayer* hci_layer, hci::Controller* controller) {
    LOG_INFO("MsftExtensionManager start()");
    module_handler_ = handler;
    hci_layer_ = hci_layer;
    controller_ = controller;
    mgmt_ = Mgmt();
    msft_start();
  }

  void stop() {
    LOG_INFO("MsftExtensionManager stop()");
  }

  void handle_msft_events(LeMetaEventView event) {
    // TODO(b/246398494): Implement.
  }

  bool get_msft_opcode() {
    if (msft_.opcode.has_value()) return true;

    uint16_t opcode = mgmt_.get_vs_opcode(MGMT_VS_OPCODE_MSFT);
    if (opcode == HCI_OP_NOP) return false;

    msft_.opcode = opcode;
    LOG_INFO("MSFT opcode 0x%4.4x", msft_.opcode.value());
    return true;
  }

  // Call this once to get information from the driver about MSFT opcode.
  void msft_read_supported_features() {
    if (!msft_.opcode.has_value()) return;

    msft_interface_->EnqueueCommand(
        MsftReadSupportedFeaturesBuilder::Create(static_cast<OpCode>(*msft_.opcode)),
        module_handler_->BindOnceOn(this, &impl::on_msft_read_supported_features_complete));
  }

  void msft_start() {
    msft_interface_ =
        hci_layer_->GetMsftInterface(module_handler_->BindOn(this, &MsftExtensionManager::impl::handle_msft_events));

    /*
     * The MSFT opcode is assigned by Bluetooth controller vendors.
     * Query the kernel/drivers to derive the MSFT opcode so that
     * we can issue MSFT vendor specific commands.
     */
    if (!get_msft_opcode()) {
      LOG_INFO("MSFT extension is not supported.");
      return;
    }

    /*
     * The vendor prefix is required to distinguish among the vendor events
     * of different vendor specifications. Read the supported features to
     * derive the vendor prefix as well as other supported features.
     */
    msft_read_supported_features();
  }

  /*
   * Get the event prefix from the packet for configuring MSFT's
   * Vendor Specific events. Also get the MSFT supported features.
   */
  void on_msft_read_supported_features_complete(CommandCompleteView view) {
    ASSERT(view.IsValid());
    auto status_view = MsftReadSupportedFeaturesCommandCompleteView::Create(MsftCommandCompleteView::Create(view));
    ASSERT(status_view.IsValid());

    if (status_view.GetStatus() != ErrorCode::SUCCESS) {
      LOG_WARN("MSFT Command complete status %s", ErrorCodeText(status_view.GetStatus()).c_str());
      return;
    }

    MsftSubcommandOpcode sub_opcode = status_view.GetSubcommandOpcode();
    if (sub_opcode != MsftSubcommandOpcode::MSFT_READ_SUPPORTED_FEATURES) {
      LOG_WARN("Wrong MSFT subcommand opcode %hhu returned", sub_opcode);
      return;
    }

    msft_.features = status_view.GetSupportedFeatures();

    msft_.prefix_len = status_view.GetPrefixLength();
    if (msft_.prefix_len > kMsftEventPrefixLengthMax)
      LOG_WARN("The MSFT prefix length %hu is too large", msft_.prefix_len);

    auto prefix = status_view.GetPrefix();
    if (prefix.size() != msft_.prefix_len)
      LOG_WARN("The actual MSFT prefix length %zu is not equal to %hu", prefix.size(), msft_.prefix_len);

    // Save the vendor prefix to distinguish upcoming MSFT vendor events.
    msft_.prefix.assign(prefix.begin(), prefix.end());

    LOG_INFO("MSFT features 0x%16.16llx prefix length %hu", (unsigned long long)msft_.features, msft_.prefix_len);
  }

  Module* module_;
  os::Handler* module_handler_;
  hci::HciLayer* hci_layer_;
  hci::Controller* controller_;
  hci::MsftInterface* msft_interface_;
  Mgmt mgmt_;
  Msft msft_;
};

MsftExtensionManager::MsftExtensionManager() {
  LOG_INFO("MsftExtensionManager()");
  pimpl_ = std::make_unique<impl>(this);
}

void MsftExtensionManager::ListDependencies(ModuleList* list) const {
  list->add<hci::HciLayer>();
  list->add<hci::Controller>();
}

void MsftExtensionManager::Start() {
  pimpl_->start(GetHandler(), GetDependency<hci::HciLayer>(), GetDependency<hci::Controller>());
}

void MsftExtensionManager::Stop() {
  pimpl_->stop();
}

std::string MsftExtensionManager::ToString() const {
  return "Microsoft Extension Manager";
}

}  // namespace hci
}  // namespace bluetooth
