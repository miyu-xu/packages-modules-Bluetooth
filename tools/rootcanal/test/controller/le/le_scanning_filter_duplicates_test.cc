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

#include <gtest/gtest.h>

#include <cstdint>
#include <memory>
#include <vector>

#include "hci/address.h"
#include "hci/hci_packets.h"
#include "model/controller/link_layer_controller.h"
#include "packet/bit_inserter.h"
#include "packet/packet_view.h"
#include "packets/link_layer_packets.h"

namespace rootcanal {

using namespace bluetooth::hci;

class LeScanningFilterDuplicates : public ::testing::Test {
 public:
  LeScanningFilterDuplicates() {}

  ~LeScanningFilterDuplicates() override = default;

  void SetUp() override {
    event_listener_called_ = 0;
    controller_.RegisterEventChannel(event_listener_);
    controller_.RegisterRemoteChannel(remote_listener_);

    auto to_mask = [](auto event) -> uint64_t {
      return UINT64_C(1) << (static_cast<uint8_t>(event) - 1);
    };

    // Set event mask to receive (extended) Advertising Reports
    controller_.SetEventMask(to_mask(EventCode::LE_META_EVENT));

    controller_.SetLeEventMask(
        to_mask(SubeventCode::ADVERTISING_REPORT) |
        to_mask(SubeventCode::EXTENDED_ADVERTISING_REPORT) |
        to_mask(SubeventCode::DIRECTED_ADVERTISING_REPORT));
  }

  void SetUpController(bool extended, bool scan_enable,
                       FilterDuplicates filter_duplicates) {
    controller_.Reset();
    if (extended == false) {
      ASSERT_EQ(ErrorCode::SUCCESS, controller_.LeSetScanParameters(
                                        LeScanType::ACTIVE, 0x4, 0x4,
                                        OwnAddressType::PUBLIC_DEVICE_ADDRESS,
                                        LeScanningFilterPolicy::ACCEPT_ALL));
      ASSERT_EQ(
          ErrorCode::SUCCESS,
          controller_.LeSetScanEnable(
              scan_enable, filter_duplicates != FilterDuplicates::DISABLED));
    } else {
      bluetooth::hci::PhyScanParameters param;
      param.le_scan_type_ = LeScanType::ACTIVE;
      param.le_scan_interval_ = 0x4;
      param.le_scan_window_ = 0x4;

      ASSERT_EQ(ErrorCode::SUCCESS,
                controller_.LeSetExtendedScanParameters(
                    OwnAddressType::PUBLIC_DEVICE_ADDRESS,
                    LeScanningFilterPolicy::ACCEPT_ALL, 0x1, {param}));
      ASSERT_EQ(ErrorCode::SUCCESS, controller_.LeSetExtendedScanEnable(
                                        scan_enable, filter_duplicates, 0, 0));
    }
  }

  /// Helper for building ScanReponse packets
  static model::packets::LinkLayerPacketView LeScanResponse(
      std::vector<uint8_t> const data = {}) {
    return FromBuilder(*model::packets::LeScanResponseBuilder::Create(
        Address::kEmpty, Address::kEmpty, model::packets::AddressType::PUBLIC,
        data));
  }

  /// Helper for building LeLegacyAdvertisingPdu packets
  static model::packets::LinkLayerPacketView LeLegacyAdvertisingPdu(
      std::vector<uint8_t> const data = {}) {
    return FromBuilder(*model::packets::LeLegacyAdvertisingPduBuilder::Create(
        Address::kEmpty, Address::kEmpty, model::packets::AddressType::PUBLIC,
        model::packets::AddressType::PUBLIC,
        model::packets::LegacyAdvertisingType::ADV_IND, data));
  }

  static model::packets::LinkLayerPacketView
          LeExtendedAdvertisingPdu(std::vector<uint8_t> const data = {}) {
    return FromBuilder(*model::packets::LeExtendedAdvertisingPduBuilder::Create(
        Address::kEmpty, Address::kEmpty, model::packets::AddressType::PUBLIC,
        model::packets::AddressType::PUBLIC, 0, 1, 0, 0, 0,
        model::packets::PrimaryPhyType::LE_1M,
        model::packets::SecondaryPhyType::LE_1M, data));
  }

  enum Filtered {
    kFiltered,
    kReported,
  };

  void SendPacket(model::packets::LinkLayerPacketView packet) {
    controller_.IncomingPacket(packet);
  }

  /// Helper for sending the provided packet to the controller then checking if
  /// it was reported or filtered
  enum Filtered SendPacketAndCheck(model::packets::LinkLayerPacketView packet) {
    unsigned const before = event_listener_called_;
    SendPacket(packet);

    if (before == event_listener_called_) {
      return kFiltered;
    }
    return kReported;
  }

 protected:
  Address address_{};
  ControllerProperties properties_{};
  LinkLayerController controller_{address_, properties_};
  static unsigned event_listener_called_;

 private:
  static void event_listener_(std::shared_ptr<EventBuilder> /* event */) {
    event_listener_called_++;
  }

  static void remote_listener_(
      std::shared_ptr<model::packets::LinkLayerPacketBuilder> /* packet */,
      Phy::Type /* phy */) {}

  /// Helper for building packet view from packet builder
  static model::packets::LinkLayerPacketView FromBuilder(
      model::packets::LinkLayerPacketBuilder& builder) {
    std::shared_ptr<std::vector<uint8_t>> buffer(new std::vector<uint8_t>);
    auto bit_inserter = bluetooth::packet::BitInserter(*buffer);

    builder.Serialize(bit_inserter);

    return model::packets::LinkLayerPacketView::Create(
        PacketView<kLittleEndian>(buffer));
  }
};

unsigned LeScanningFilterDuplicates::event_listener_called_ = 0;

TEST_F(LeScanningFilterDuplicates, LegacyAdvertisingPdu) {
  // Legacy, no scanning, no filter
  SetUpController(false, false, FilterDuplicates::DISABLED);
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeLegacyAdvertisingPdu()));

  // Legacy, scanning, no filter
  SetUpController(false, true, FilterDuplicates::DISABLED);
  ASSERT_EQ(kReported, SendPacketAndCheck(LeLegacyAdvertisingPdu()));
  ASSERT_EQ(kReported, SendPacketAndCheck(LeLegacyAdvertisingPdu()));

  // Legacy, scanning, filter
  SetUpController(false, true, FilterDuplicates::ENABLED);
  ASSERT_EQ(kReported, SendPacketAndCheck(LeLegacyAdvertisingPdu()));
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeLegacyAdvertisingPdu()));
  ASSERT_EQ(kReported, SendPacketAndCheck(LeLegacyAdvertisingPdu({0})));
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeLegacyAdvertisingPdu({0})));
  ASSERT_EQ(kReported, SendPacketAndCheck(LeLegacyAdvertisingPdu({0, 1})));
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeLegacyAdvertisingPdu({0, 1})));

  // Extended, no scanning, no filter
  SetUpController(true, false, FilterDuplicates::DISABLED);
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeLegacyAdvertisingPdu()));

  // Extended, scanning, no filter
  SetUpController(true, true, FilterDuplicates::DISABLED);
  ASSERT_EQ(kReported, SendPacketAndCheck(LeLegacyAdvertisingPdu()));
  ASSERT_EQ(kReported, SendPacketAndCheck(LeLegacyAdvertisingPdu()));

  // Extended, scanning, filter
  SetUpController(true, true, FilterDuplicates::ENABLED);
  ASSERT_EQ(kReported, SendPacketAndCheck(LeLegacyAdvertisingPdu()));
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeLegacyAdvertisingPdu()));
  ASSERT_EQ(kReported, SendPacketAndCheck(LeLegacyAdvertisingPdu({0})));
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeLegacyAdvertisingPdu({0})));
  ASSERT_EQ(kReported, SendPacketAndCheck(LeLegacyAdvertisingPdu({0, 1})));
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeLegacyAdvertisingPdu({0, 1})));
}

TEST_F(LeScanningFilterDuplicates, ExtendedAdvertisingPdu) {
  // Legacy, no scanning, no filter
  SetUpController(false, false, FilterDuplicates::DISABLED);
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeExtendedAdvertisingPdu()));

  // Legacy, scanning, no filter
  SetUpController(false, true, FilterDuplicates::DISABLED);
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeExtendedAdvertisingPdu()));

  // Extended, no scanning, no filter
  SetUpController(true, false, FilterDuplicates::DISABLED);
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeExtendedAdvertisingPdu()));

  // Extended, scanning, no filter
  SetUpController(true, true, FilterDuplicates::DISABLED);
  ASSERT_EQ(kReported, SendPacketAndCheck(LeExtendedAdvertisingPdu()));
  ASSERT_EQ(kReported, SendPacketAndCheck(LeExtendedAdvertisingPdu()));

  // Extended, scanning, filter
  SetUpController(true, true, FilterDuplicates::ENABLED);
  ASSERT_EQ(kReported, SendPacketAndCheck(LeExtendedAdvertisingPdu()));
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeExtendedAdvertisingPdu()));
  ASSERT_EQ(kReported, SendPacketAndCheck(LeExtendedAdvertisingPdu({0})));
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeExtendedAdvertisingPdu({0})));
  ASSERT_EQ(kReported, SendPacketAndCheck(LeExtendedAdvertisingPdu({0, 1})));
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeExtendedAdvertisingPdu({0, 1})));
}

TEST_F(LeScanningFilterDuplicates, LeScanResponseToLegacy) {
  // Legacy, no scanning, no filter
  SetUpController(false, false, FilterDuplicates::DISABLED);
  SendPacket(LeLegacyAdvertisingPdu());
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse()));

  // Legacy, scanning, no filter
  SetUpController(false, true, FilterDuplicates::DISABLED);
  SendPacket(LeLegacyAdvertisingPdu());
  ASSERT_EQ(kReported, SendPacketAndCheck(LeScanResponse()));
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse()));
  SendPacket(LeLegacyAdvertisingPdu());
  ASSERT_EQ(kReported, SendPacketAndCheck(LeScanResponse()));
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse()));

  // Legacy, scanning, filter
  SetUpController(false, true, FilterDuplicates::ENABLED);
  SendPacket(LeLegacyAdvertisingPdu());
  ASSERT_EQ(kReported, SendPacketAndCheck(LeScanResponse()));
  SendPacket(LeLegacyAdvertisingPdu());  // Duplicate
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse()));
  SendPacket(LeLegacyAdvertisingPdu({0}));
  ASSERT_EQ(kReported, SendPacketAndCheck(LeScanResponse({0})));
  SendPacket(LeLegacyAdvertisingPdu({0}));  // Duplicate
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse({0})));
  SendPacket(LeLegacyAdvertisingPdu({0, 1}));
  ASSERT_EQ(kReported, SendPacketAndCheck(LeScanResponse({0, 1})));
  SendPacket(LeLegacyAdvertisingPdu({0, 1}));  // Duplicate
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse({0, 1})));

  // Extended, no scanning, no filter
  SetUpController(true, false, FilterDuplicates::DISABLED);
  SendPacket(LeLegacyAdvertisingPdu());
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse()));

  // Extended, scanning, no filter
  SetUpController(true, true, FilterDuplicates::DISABLED);
  SendPacket(LeLegacyAdvertisingPdu());
  ASSERT_EQ(kReported, SendPacketAndCheck(LeScanResponse()));
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse()));
  SendPacket(LeLegacyAdvertisingPdu());
  ASSERT_EQ(kReported, SendPacketAndCheck(LeScanResponse()));
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse()));

  // Extended, scanning, filter
  SetUpController(true, true, FilterDuplicates::ENABLED);
  SendPacket(LeLegacyAdvertisingPdu());
  ASSERT_EQ(kReported, SendPacketAndCheck(LeScanResponse()));
  SendPacket(LeLegacyAdvertisingPdu());  // Duplicate
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse()));
  SendPacket(LeLegacyAdvertisingPdu({0}));
  ASSERT_EQ(kReported, SendPacketAndCheck(LeScanResponse({0})));
  SendPacket(LeLegacyAdvertisingPdu({0}));  // Duplicate
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse({0})));
  SendPacket(LeLegacyAdvertisingPdu({0, 1}));
  ASSERT_EQ(kReported, SendPacketAndCheck(LeScanResponse({0, 1})));
  SendPacket(LeLegacyAdvertisingPdu({0, 1}));  // Duplicate
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse({0, 1})));
}

TEST_F(LeScanningFilterDuplicates, LeScanResponseToExtended) {
  // Legacy, no scanning, no filter
  SetUpController(false, false, FilterDuplicates::DISABLED);
  SendPacket(LeExtendedAdvertisingPdu());
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse()));

  // Legacy, scanning, no filter
  SetUpController(false, true, FilterDuplicates::DISABLED);
  SendPacket(LeExtendedAdvertisingPdu());
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse()));
  SendPacket(LeExtendedAdvertisingPdu());
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse()));

  // Extended, no scanning, no filter
  SetUpController(true, false, FilterDuplicates::DISABLED);
  SendPacket(LeExtendedAdvertisingPdu());
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse()));

  // Extended, scanning, no filter
  SetUpController(true, true, FilterDuplicates::DISABLED);
  SendPacket(LeExtendedAdvertisingPdu());
  ASSERT_EQ(kReported, SendPacketAndCheck(LeScanResponse()));
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse()));
  SendPacket(LeExtendedAdvertisingPdu());
  ASSERT_EQ(kReported, SendPacketAndCheck(LeScanResponse()));
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse()));

  // Extended, scanning, filter
  SetUpController(true, true, FilterDuplicates::ENABLED);
  SendPacket(LeExtendedAdvertisingPdu());
  ASSERT_EQ(kReported, SendPacketAndCheck(LeScanResponse()));
  SendPacket(LeExtendedAdvertisingPdu());  // Duplicate
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse()));
  SendPacket(LeExtendedAdvertisingPdu({0}));
  ASSERT_EQ(kReported, SendPacketAndCheck(LeScanResponse({0})));
  SendPacket(LeExtendedAdvertisingPdu({0}));  // Duplicate
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse({0})));
  SendPacket(LeExtendedAdvertisingPdu({0, 1}));
  ASSERT_EQ(kReported, SendPacketAndCheck(LeScanResponse({0, 1})));
  SendPacket(LeExtendedAdvertisingPdu({0, 1}));  // Duplicate
  ASSERT_EQ(kFiltered, SendPacketAndCheck(LeScanResponse({0, 1})));
}
}  // namespace rootcanal
