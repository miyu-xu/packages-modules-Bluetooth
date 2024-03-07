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

#include <hardware/bt_common_types.h>

#include "hal/hci_hal.h"
#include "hci/hci_layer.h"
#include "hci/hci_packets.h"
#include "hci/vendor_specific_event_manager.h"

namespace bluetooth {
namespace hci {

// https://learn.microsoft.com/en-us/windows-hardware/drivers/bluetooth/
//         microsoft-defined-bluetooth-hci-commands-and-events
constexpr uint8_t kMsftEventPrefixLengthMax = 0x20;

struct Msft {
  // MSFT opcode needs to be configured from Bluetooth driver.
  std::optional<uint16_t> opcode;
  uint64_t features{0};
  std::vector<uint8_t> prefix;
};

const ModuleFactory MsftExtensionManager::Factory = ModuleFactory([]() { return new MsftExtensionManager(); });

struct MsftExtensionManager::impl {
  impl(Module* module) : module_(module){};

  ~impl() {}

  void start(
      os::Handler* handler,
      hal::HciHal* hal,
      hci::HciLayer* hci_layer,
      hci::VendorSpecificEventManager* vendor_specific_event_manager) {
    LOG_INFO("MsftExtensionManager start()");
    module_handler_ = handler;
    hal_ = hal;
    hci_layer_ = hci_layer;
    vendor_specific_event_manager_ = vendor_specific_event_manager;

    /*
     * The MSFT opcode is assigned by Bluetooth controller vendors.
     * Query the kernel/drivers to derive the MSFT opcode so that
     * we can issue MSFT vendor specific commands.
     */
    if (!supports_msft_extensions()) {
      LOG_INFO("MSFT extension is not supported.");
      return;
    }

    /*
     * The vendor prefix is required to distinguish among the vendor events
     * of different vendor specifications. Read the supported features to
     * derive the vendor prefix as well as other supported features.
     */
    hci_layer_->EnqueueCommand(
        MsftReadSupportedFeaturesBuilder::Create(static_cast<OpCode>(msft_.opcode.value())),
        module_handler_->BindOnceOn(this, &impl::on_msft_read_supported_features_complete));
  }

  void stop() {
    LOG_INFO("MsftExtensionManager stop()");
  }

  void handle_rssi_event(MsftRssiEventPayloadView /* view */) {
    LOG_WARN("The Microsoft MSFT_RSSI_EVENT is not supported yet.");
  }

  void handle_le_monitor_device_event(MsftLeMonitorDeviceEventPayloadView view) {
    ASSERT(view.IsValid());

    // The monitor state is 0x00 when the controller stops monitoring the device.
    if (view.GetMonitorState() == 0x00 || view.GetMonitorState() == 0x01) {
      AdvertisingFilterOnFoundOnLostInfo on_found_on_lost_info;
      on_found_on_lost_info.advertiser_address_type = view.GetAddressType();
      on_found_on_lost_info.advertiser_address = view.GetBdAddr();
      on_found_on_lost_info.advertiser_state = view.GetMonitorState();
      on_found_on_lost_info.monitor_handle = view.GetMonitorHandle();
      scanning_callbacks_->OnTrackAdvFoundLost(on_found_on_lost_info);
    } else {
      LOG_WARN("The Microsoft vendor event monitor state is invalid.");
      return;
    }
  }

  void handle_msft_events(VendorSpecificEventView view) {
    auto payload = view.GetPayload();
    for (size_t i = 0; i < msft_.prefix.size() - 1; i++) {
      if (msft_.prefix[i + 1] != payload[i]) {
        LOG_WARN("The Microsoft vendor event prefix does not match.");
        return;
      }
    }

    auto msft_view = MsftEventPayloadView::Create(
        payload.GetLittleEndianSubview(msft_.prefix.size() - 1, payload.size()));
    ASSERT(msft_view.IsValid());

    MsftEventCode ev_code = msft_view.GetMsftEventCode();
    switch (ev_code) {
      case MsftEventCode::MSFT_RSSI_EVENT:
        handle_rssi_event(MsftRssiEventPayloadView::Create(msft_view));
        break;
      case MsftEventCode::MSFT_LE_MONITOR_DEVICE_EVENT:
        handle_le_monitor_device_event(MsftLeMonitorDeviceEventPayloadView::Create(msft_view));
        break;
      default:
        LOG_WARN("Unknown MSFT event code %hhu", ev_code);
        break;
    }
  }

  bool supports_msft_extensions() {
    if (msft_.opcode.has_value()) return true;

    uint16_t opcode = hal_->getMsftOpcode();
    if (opcode == 0) return false;

    msft_.opcode = opcode;
    LOG_INFO("MSFT opcode 0x%4.4x", msft_.opcode.value());
    return true;
  }

  void msft_adv_monitor_add(const MsftAdvMonitor& monitor, MsftAdvMonitorAddCallback cb) {
    if (!supports_msft_extensions()) {
      LOG_WARN("Disallowed as MSFT extension is not supported.");
      return;
    }
    LOG_INFO(
        "%s:%d, opcode=0x%x, monitor.condition_type=%d",
        __func__,
        __LINE__,
        msft_.opcode.value(),
        monitor.condition_type);

    if (monitor.condition_type == MSFT_CONDITION_TYPE_ALL) {
      return;
    }
    if (monitor.condition_type == MSFT_CONDITION_TYPE_ADDRESS) {
      msft_adv_monitor_add_cb_ = cb;
      Address addr;
      Address::FromString(monitor.addr_info.bd_addr.ToString(), addr);
      hci_layer_->EnqueueCommand(
          MsftLeMonitorAdvConditionAddressBuilder::Create(
              static_cast<OpCode>(msft_.opcode.value()),
              monitor.rssi_threshold_high,
              monitor.rssi_threshold_low,
              monitor.rssi_threshold_low_time_interval,
              monitor.rssi_sampling_period,
              monitor.addr_info.addr_type,
              addr),
          module_handler_->BindOnceOn(this, &impl::on_msft_adv_monitor_add_complete));
      return;
    } else if (monitor.condition_type == MSFT_CONDITION_TYPE_UUID) {
      msft_adv_monitor_add_cb_ = cb;
      switch (monitor.uuid.uuid_type) {
        // refer to
        // https://learn.microsoft.com/en-us/windows-hardware/drivers/bluetooth/microsoft-defined-bluetooth-hci-commands-and-events#hci_vs_msft_le_monitor_device_event
        // if uuid type is 0x01, means 2 octets in the data field, if uuid type is 0x02, means 4
        // octets, type is 0x03, 16 octets
        case MSFT_CONDITION_UUID_TYPE_16_BIT:
          if (monitor.uuid.uuid_data.size() != 2) {
            LOG_ERROR(
                "%s:%d, uuid type is 16 bits, but data size didn't match", __func__, __LINE__);
            return;
          }
          std::array<uint8_t, 2> arr2;
          std::copy(monitor.uuid.uuid_data.begin(), monitor.uuid.uuid_data.end(), arr2.begin());
          hci_layer_->EnqueueCommand(
              MsftLeMonitorAdvConditionUuid2Builder::Create(
                  static_cast<OpCode>(msft_.opcode.value()),
                  monitor.rssi_threshold_high,
                  monitor.rssi_threshold_low,
                  monitor.rssi_threshold_low_time_interval,
                  monitor.rssi_sampling_period,
                  arr2),
              module_handler_->BindOnceOn(this, &impl::on_msft_adv_monitor_add_complete));
          break;
        case MSFT_CONDITION_UUID_TYPE_32_BIT:
          if (monitor.uuid.uuid_data.size() != 4) {
            LOG_ERROR(
                "%s:%d, uuid type is 32 bits, but data size didn't match", __func__, __LINE__);
            return;
          }
          std::array<uint8_t, 4> arr4;
          std::copy(monitor.uuid.uuid_data.begin(), monitor.uuid.uuid_data.end(), arr4.begin());
          hci_layer_->EnqueueCommand(
              MsftLeMonitorAdvConditionUuid4Builder::Create(
                  static_cast<OpCode>(msft_.opcode.value()),
                  monitor.rssi_threshold_high,
                  monitor.rssi_threshold_low,
                  monitor.rssi_threshold_low_time_interval,
                  monitor.rssi_sampling_period,
                  arr4),
              module_handler_->BindOnceOn(this, &impl::on_msft_adv_monitor_add_complete));
          break;
        case MSFT_CONDITION_UUID_TYPE_128_BIT:
          if (monitor.uuid.uuid_data.size() != 16) {
            LOG_ERROR(
                "%s:%d, uuid type is 128 bits, but data size didn't match", __func__, __LINE__);
            return;
          }
          std::array<uint8_t, 16> arr16;
          std::copy(monitor.uuid.uuid_data.begin(), monitor.uuid.uuid_data.end(), arr16.begin());
          hci_layer_->EnqueueCommand(
              MsftLeMonitorAdvConditionUuid16Builder::Create(
                  static_cast<OpCode>(msft_.opcode.value()),
                  monitor.rssi_threshold_high,
                  monitor.rssi_threshold_low,
                  monitor.rssi_threshold_low_time_interval,
                  monitor.rssi_sampling_period,
                  arr16),
              module_handler_->BindOnceOn(this, &impl::on_msft_adv_monitor_add_complete));
          break;
      }
      return;
    } else if (monitor.condition_type == MSFT_CONDITION_TYPE_IRK_RESOLUTION) {
      // reserved for furture use
    }

    std::vector<MsftLeMonitorAdvConditionPattern> patterns;
    MsftLeMonitorAdvConditionPattern pattern;
    // The Microsoft Extension specifies 1 octet for the number of patterns.
    // However, the max number of patters should not exceed 61.
    // (255 - 1 (packet type) - 2 (OGF/OCF) - 1 (length) - 7 (MSFT command parameters)) /
    // 4 (min size of a pattern) = 61
    if (monitor.patterns.size() > 61) {
      LOG_ERROR("Number of MSFT patterns %zu is too large", monitor.patterns.size());
      return;
    }
    for (auto& p : monitor.patterns) {
      pattern.ad_type_ = p.ad_type;
      pattern.start_of_pattern_ = p.start_byte;
      pattern.pattern_ = p.pattern;
      patterns.push_back(pattern);
    }

    msft_adv_monitor_add_cb_ = cb;
    hci_layer_->EnqueueCommand(
        MsftLeMonitorAdvConditionPatternsBuilder::Create(
            static_cast<OpCode>(msft_.opcode.value()),
            monitor.rssi_threshold_high,
            monitor.rssi_threshold_low,
            monitor.rssi_threshold_low_time_interval,
            monitor.rssi_sampling_period,
            patterns),
        module_handler_->BindOnceOn(this, &impl::on_msft_adv_monitor_add_complete));
  }

  void msft_adv_monitor_remove(uint8_t monitor_handle, MsftAdvMonitorRemoveCallback cb) {
    if (!supports_msft_extensions()) {
      LOG_WARN("Disallowed as MSFT extension is not supported.");
      return;
    }

    msft_adv_monitor_remove_cb_ = cb;
    hci_layer_->EnqueueCommand(
        MsftLeCancelMonitorAdvBuilder::Create(
            static_cast<OpCode>(msft_.opcode.value()), monitor_handle),
        module_handler_->BindOnceOn(this, &impl::on_msft_adv_monitor_remove_complete));
  }

  void msft_adv_monitor_enable(bool enable, MsftAdvMonitorEnableCallback cb) {
    if (!supports_msft_extensions()) {
      LOG_WARN("Disallowed as MSFT extension is not supported.");
      return;
    }

    msft_adv_monitor_enable_cb_ = cb;
    hci_layer_->EnqueueCommand(
        MsftLeSetAdvFilterEnableBuilder::Create(static_cast<OpCode>(msft_.opcode.value()), enable),
        module_handler_->BindOnceOn(this, &impl::on_msft_adv_monitor_enable_complete));
  }

  void set_scanning_callback(ScanningCallback* callbacks) {
    scanning_callbacks_ = callbacks;
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

    // Save the vendor prefix to distinguish upcoming MSFT vendor events.
    auto prefix = status_view.GetPrefix();
    msft_.prefix.assign(prefix.begin(), prefix.end());

    if (prefix.size() > kMsftEventPrefixLengthMax)
      LOG_WARN("The MSFT prefix length %u is too large", (unsigned int)prefix.size());

    LOG_INFO(
        "MSFT features 0x%16.16llx prefix length %u", (unsigned long long)msft_.features, (unsigned int)prefix.size());

    // We are here because Microsoft Extension is supported. Hence, register the
    // first octet of the vendor prefix so that the vendor specific event manager
    // can dispatch the event correctly.
    // Note: registration of the first octet of the vendor prefix is sufficient
    //       because each vendor controller should ensure that the first octet
    //       is unique within the vendor's events.
    vendor_specific_event_manager_->RegisterEventHandler(
        static_cast<VseSubeventCode>(msft_.prefix[0]),
        module_handler_->BindOn(this, &impl::handle_msft_events));
  }

  void on_msft_adv_monitor_add_complete(CommandCompleteView view) {
    ASSERT(view.IsValid());
    auto status_view =
        MsftLeMonitorAdvCommandCompleteView::Create(MsftCommandCompleteView::Create(view));
    ASSERT(status_view.IsValid());

    MsftSubcommandOpcode sub_opcode = status_view.GetSubcommandOpcode();
    if (sub_opcode != MsftSubcommandOpcode::MSFT_LE_MONITOR_ADV) {
      LOG_WARN("Wrong MSFT subcommand opcode %hhu returned", sub_opcode);
      return;
    }

    msft_adv_monitor_add_cb_.Run(status_view.GetMonitorHandle(), status_view.GetStatus());
  }

  void on_msft_adv_monitor_remove_complete(CommandCompleteView view) {
    ASSERT(view.IsValid());
    auto status_view =
        MsftLeCancelMonitorAdvCommandCompleteView::Create(MsftCommandCompleteView::Create(view));
    ASSERT(status_view.IsValid());

    MsftSubcommandOpcode sub_opcode = status_view.GetSubcommandOpcode();
    if (sub_opcode != MsftSubcommandOpcode::MSFT_LE_CANCEL_MONITOR_ADV) {
      LOG_WARN("Wrong MSFT subcommand opcode %hhu returned", sub_opcode);
      return;
    }

    msft_adv_monitor_remove_cb_.Run(status_view.GetStatus());
  }

  void on_msft_adv_monitor_enable_complete(CommandCompleteView view) {
    ASSERT(view.IsValid());
    auto status_view =
        MsftLeSetAdvFilterEnableCommandCompleteView::Create(MsftCommandCompleteView::Create(view));
    ASSERT(status_view.IsValid());

    MsftSubcommandOpcode sub_opcode = status_view.GetSubcommandOpcode();
    if (sub_opcode != MsftSubcommandOpcode::MSFT_LE_SET_ADV_FILTER_ENABLE) {
      LOG_WARN("Wrong MSFT subcommand opcode %hhu returned", sub_opcode);
      return;
    }

    msft_adv_monitor_enable_cb_.Run(status_view.GetStatus());
  }

  Module* module_;
  os::Handler* module_handler_;
  hal::HciHal* hal_;
  hci::HciLayer* hci_layer_;
  hci::VendorSpecificEventManager* vendor_specific_event_manager_;
  Msft msft_;
  MsftAdvMonitorAddCallback msft_adv_monitor_add_cb_;
  MsftAdvMonitorRemoveCallback msft_adv_monitor_remove_cb_;
  MsftAdvMonitorEnableCallback msft_adv_monitor_enable_cb_;
  ScanningCallback* scanning_callbacks_;
};

MsftExtensionManager::MsftExtensionManager() {
  LOG_INFO("MsftExtensionManager()");
  pimpl_ = std::make_unique<impl>(this);
}

void MsftExtensionManager::ListDependencies(ModuleList* list) const {
  list->add<hal::HciHal>();
  list->add<hci::HciLayer>();
  list->add<hci::VendorSpecificEventManager>();
}

void MsftExtensionManager::Start() {
  pimpl_->start(
      GetHandler(),
      GetDependency<hal::HciHal>(),
      GetDependency<hci::HciLayer>(),
      GetDependency<hci::VendorSpecificEventManager>());
}

void MsftExtensionManager::Stop() {
  pimpl_->stop();
}

std::string MsftExtensionManager::ToString() const {
  return "Microsoft Extension Manager";
}

bool MsftExtensionManager::SupportsMsftExtensions() {
  return pimpl_->supports_msft_extensions();
}

void MsftExtensionManager::MsftAdvMonitorAdd(
    const MsftAdvMonitor& monitor, MsftAdvMonitorAddCallback cb) {
  CallOn(pimpl_.get(), &impl::msft_adv_monitor_add, monitor, cb);
}

void MsftExtensionManager::MsftAdvMonitorRemove(
    uint8_t monitor_handle, MsftAdvMonitorRemoveCallback cb) {
  CallOn(pimpl_.get(), &impl::msft_adv_monitor_remove, monitor_handle, cb);
}

void MsftExtensionManager::MsftAdvMonitorEnable(bool enable, MsftAdvMonitorEnableCallback cb) {
  CallOn(pimpl_.get(), &impl::msft_adv_monitor_enable, enable, cb);
}

void MsftExtensionManager::SetScanningCallback(ScanningCallback* callbacks) {
  CallOn(pimpl_.get(), &impl::set_scanning_callback, callbacks);
}

}  // namespace hci
}  // namespace bluetooth
