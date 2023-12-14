/*
 * Copyright 2021 HIMSA II K/S - www.himsa.dk.
 * Represented by EHIMA - www.ehima.com
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

#include <base/functional/callback.h>
#include <gmock/gmock.h>

#include "btcore/include/version.h"
#include "hci/le_rand_callback.h"
#include "types/raw_address.h"

namespace controller {
class ControllerInterface {
 public:
  virtual uint8_t GetIsoBufferCount(void) = 0;
  virtual uint16_t GetIsoDataSize(void) = 0;
  virtual bool SupportsBleConnectedIsochronousStreamCentral(void) = 0;
  virtual bool SupportsBleConnectedIsochronousStreamPeripheral(void) = 0;
  virtual bool SupportsBleIsochronousBroadcaster(void) = 0;
  virtual bool SupportsBle2mPhy(void) = 0;
  virtual bool SupportsConfigureDataPath(void) = 0;
  virtual bool SupportsBleCodedPhy(void) = 0;
  virtual bool SupportsSimplePairing(void) = 0;
  virtual bool SupportsSecureConnections(void) = 0;
  virtual bool SupportsSimultaneousLeBredr(void) = 0;
  virtual bool SupportsReadingRemoteExtendedFeatures(void) = 0;
  virtual bool SupportsInterlacedInquiryScan(void) = 0;
  virtual bool SupportsRssiWithInquiryResults(void) = 0;
  virtual bool SupportsExtendedInquiryResponse(void) = 0;
  virtual bool SupportsCentralPeripheralRoleSwitch(void) = 0;
  virtual bool SupportsEnhancedSetupSynchronousConnection(void) = 0;
  virtual bool SupportsEnhancedAcceptSynchronousConnection(void) = 0;
  virtual bool Supports3SlotPackets(void) = 0;
  virtual bool Supports5SlotPackets(void) = 0;
  virtual bool SupportsClassic2mPhy(void) = 0;
  virtual bool SupportsClassic3mPhy(void) = 0;
  virtual bool Supports3SlotEdrPackets(void) = 0;
  virtual bool Supports5SlotEdrPackets(void) = 0;
  virtual bool SupportsSco(void) = 0;
  virtual bool SupportsHv2Packets(void) = 0;
  virtual bool SupportsHv3Packets(void) = 0;
  virtual bool SupportsEv3Packets(void) = 0;
  virtual bool SupportsEv4Packets(void) = 0;
  virtual bool SupportsEv5Packets(void) = 0;
  virtual bool SupportsEsco2mPhy(void) = 0;
  virtual bool SupportsEsco3mPhy(void) = 0;
  virtual bool Supports3SlotEscoEdrPackets(void) = 0;
  virtual bool SupportsRoleSwitch(void) = 0;
  virtual bool SupportsHoldMode(void) = 0;
  virtual bool SupportsSniffMode(void) = 0;
  virtual bool SupportsParkMode(void) = 0;
  virtual bool SupportsNonFlushablePb(void) = 0;
  virtual bool SupportsSniffSubrating(void) = 0;
  virtual bool SupportsEncryptionPause(void) = 0;
  virtual bool SupportsSetMinEncryptionKeySize(void) = 0;
  virtual bool SupportsReadEncryptionKeySize(void) = 0;
  virtual bool SupportsBle(void) = 0;
  virtual bool SupportsBlePacketExtension(void) = 0;
  virtual bool SupportsBleConnectionParametersRequest(void) = 0;
  virtual bool SupportsBlePrivacy(void) = 0;
  virtual bool SupportsBleSetPrivacyMode(void) = 0;
  virtual bool SupportsBleExtendedAdvertising(void) = 0;
  virtual bool SupportsBlePeriodicAdvertising(void) = 0;
  virtual bool SupportsBlePeripheralInitiatedFeatureExchange(void) = 0;
  virtual bool SupportsBleConnectionParameterRequest(void) = 0;
  virtual bool SupportsBlePeriodicAdvertisingSyncTransferSender(void) = 0;
  virtual bool SupportsBlePeriodicAdvertisingSyncTransferRecipient(void) = 0;
  virtual bool SupportsBleSynchronizedReceiver(void) = 0;
  virtual bool SupportsBleConnectionSubrating(void) = 0;
  virtual bool SupportsBleConnectionSubratingHost(void) = 0;
  virtual bool GetIsReady() = 0;
  virtual const RawAddress* GetAddress() = 0;
  virtual const bt_version_t* GetBtVersion() = 0;
  virtual const uint8_t* GetBleSupportedStates() = 0;
  virtual uint16_t GetAclDataSizeClassic() = 0;
  virtual uint16_t GetAclDataSizeBle() = 0;
  virtual uint16_t GetAclPacketSizeClassic() = 0;
  virtual uint16_t GetAclPacketSizeBle() = 0;
  virtual uint16_t GetIsoPacketSize() = 0;
  virtual uint16_t GetBleDefaultDataPacketLength() = 0;
  virtual uint16_t GetBleMaximumTxDataLength() = 0;
  virtual uint16_t GetBleMaximumTxTime() = 0;
  virtual uint16_t GetBleMaximumAdvertisingDataLength() = 0;
  virtual uint8_t GetBleNumberOfSupportedAdvertisingSets() = 0;
  virtual uint8_t GetBlePeriodicAdvertiserListSize() = 0;
  virtual uint16_t GetAclBufferCountClassic() = 0;
  virtual uint8_t GetAclBufferCountBle() = 0;
  virtual uint8_t GetBleAcceptlistSize() = 0;
  virtual uint8_t GetBleResolvingListMaxSize() = 0;
  virtual void SetBleResolvingListMaxSize(int resolvingListMax_size) = 0;
  virtual uint8_t* GetLocalSupportedCodecs(uint8_t* numberOfCodecs) = 0;
  virtual uint8_t GetLeAllInitiatingPhys() = 0;
  virtual uint8_t ClearEventFilter() = 0;
  virtual uint8_t ClearEventMask() = 0;
  virtual uint8_t LeRand(bluetooth::hci::LeRandCallback) = 0;
  virtual uint8_t SetEventFilterConnectionSetupAllDevices() = 0;
  virtual uint8_t SetEventFilterAllowDeviceConnection(
      std::vector<RawAddress> devices) = 0;
  virtual uint8_t SetDefaultEventMaskExcept(uint64_t mask,
                                            uint64_t le_mask) = 0;
  virtual uint8_t SetEventFilterInquiryResultAllDevices() = 0;

  virtual ~ControllerInterface() = default;
};

class MockControllerInterface : public ControllerInterface {
 public:
  MOCK_METHOD((uint8_t), GetIsoBufferCount, (), (override));
  MOCK_METHOD((uint16_t), GetIsoDataSize, (), (override));
  MOCK_METHOD((bool), SupportsBleConnectedIsochronousStreamCentral, (),
              (override));
  MOCK_METHOD((bool), SupportsBleConnectedIsochronousStreamPeripheral, (),
              (override));
  MOCK_METHOD((bool), SupportsBleIsochronousBroadcaster, (), (override));
  MOCK_METHOD((bool), SupportsBle2mPhy, (), (override));
  MOCK_METHOD((bool), SupportsConfigureDataPath, (), (override));
  MOCK_METHOD((bool), SupportsBleCodedPhy, (), (override));
  MOCK_METHOD((bool), SupportsSimplePairing, (), (override));
  MOCK_METHOD((bool), SupportsSecureConnections, (), (override));
  MOCK_METHOD((bool), SupportsSimultaneousLeBredr, (), (override));
  MOCK_METHOD((bool), SupportsReadingRemoteExtendedFeatures, (), (override));
  MOCK_METHOD((bool), SupportsInterlacedInquiryScan, (), (override));
  MOCK_METHOD((bool), SupportsRssiWithInquiryResults, (), (override));
  MOCK_METHOD((bool), SupportsExtendedInquiryResponse, (), (override));
  MOCK_METHOD((bool), SupportsCentralPeripheralRoleSwitch, (), (override));
  MOCK_METHOD((bool), SupportsEnhancedSetupSynchronousConnection, (),
              (override));
  MOCK_METHOD((bool), SupportsEnhancedAcceptSynchronousConnection, (),
              (override));
  MOCK_METHOD((bool), Supports3SlotPackets, (), (override));
  MOCK_METHOD((bool), Supports5SlotPackets, (), (override));
  MOCK_METHOD((bool), SupportsClassic2mPhy, (), (override));
  MOCK_METHOD((bool), SupportsClassic3mPhy, (), (override));
  MOCK_METHOD((bool), Supports3SlotEdrPackets, (), (override));
  MOCK_METHOD((bool), Supports5SlotEdrPackets, (), (override));
  MOCK_METHOD((bool), SupportsSco, (), (override));
  MOCK_METHOD((bool), SupportsHv2Packets, (), (override));
  MOCK_METHOD((bool), SupportsHv3Packets, (), (override));
  MOCK_METHOD((bool), SupportsEv3Packets, (), (override));
  MOCK_METHOD((bool), SupportsEv4Packets, (), (override));
  MOCK_METHOD((bool), SupportsEv5Packets, (), (override));
  MOCK_METHOD((bool), SupportsEsco2mPhy, (), (override));
  MOCK_METHOD((bool), SupportsEsco3mPhy, (), (override));
  MOCK_METHOD((bool), Supports3SlotEscoEdrPackets, (), (override));
  MOCK_METHOD((bool), SupportsRoleSwitch, (), (override));
  MOCK_METHOD((bool), SupportsHoldMode, (), (override));
  MOCK_METHOD((bool), SupportsSniffMode, (), (override));
  MOCK_METHOD((bool), SupportsParkMode, (), (override));
  MOCK_METHOD((bool), SupportsNonFlushablePb, (), (override));
  MOCK_METHOD((bool), SupportsSniffSubrating, (), (override));
  MOCK_METHOD((bool), SupportsEncryptionPause, (), (override));
  MOCK_METHOD((bool), SupportsSetMinEncryptionKeySize, (), (override));
  MOCK_METHOD((bool), SupportsReadEncryptionKeySize, (), (override));
  MOCK_METHOD((bool), SupportsBle, (), (override));
  MOCK_METHOD((bool), SupportsBlePacketExtension, (), (override));
  MOCK_METHOD((bool), SupportsBleConnectionParametersRequest, (), (override));
  MOCK_METHOD((bool), SupportsBlePrivacy, (), (override));
  MOCK_METHOD((bool), SupportsBleSetPrivacyMode, (), (override));
  MOCK_METHOD((bool), SupportsBleExtendedAdvertising, (), (override));
  MOCK_METHOD((bool), SupportsBlePeriodicAdvertising, (), (override));
  MOCK_METHOD((bool), SupportsBlePeripheralInitiatedFeatureExchange, (),
              (override));
  MOCK_METHOD((bool), SupportsBleConnectionParameterRequest, (), (override));
  MOCK_METHOD((bool), SupportsBlePeriodicAdvertisingSyncTransferSender, (),
              (override));
  MOCK_METHOD((bool), SupportsBlePeriodicAdvertisingSyncTransferRecipient, (),
              (override));
  MOCK_METHOD((bool), SupportsBleSynchronizedReceiver, (), (override));
  MOCK_METHOD((bool), SupportsBleConnectionSubrating, (), (override));
  MOCK_METHOD((bool), SupportsBleConnectionSubratingHost, (), (override));

  MOCK_METHOD((bool), GetIsReady, (), (override));
  MOCK_METHOD((const RawAddress*), GetAddress, (), (override));
  MOCK_METHOD((const bt_version_t*), GetBtVersion, (), (override));
  MOCK_METHOD((const uint8_t*), GetBleSupportedStates, (), (override));
  MOCK_METHOD((uint16_t), GetAclDataSizeClassic, (), (override));
  MOCK_METHOD((uint16_t), GetAclDataSizeBle, (), (override));
  MOCK_METHOD((uint16_t), GetAclPacketSizeClassic, (), (override));
  MOCK_METHOD((uint16_t), GetAclPacketSizeBle, (), (override));
  MOCK_METHOD((uint16_t), GetIsoPacketSize, (), (override));
  MOCK_METHOD((uint16_t), GetBleDefaultDataPacketLength, (), (override));
  MOCK_METHOD((uint16_t), GetBleMaximumTxDataLength, (), (override));
  MOCK_METHOD((uint16_t), GetBleMaximumTxTime, (), (override));
  MOCK_METHOD((uint16_t), GetBleMaximumAdvertisingDataLength, (), (override));
  MOCK_METHOD((uint8_t), GetBleNumberOfSupportedAdvertisingSets, (),
              (override));
  MOCK_METHOD((uint8_t), GetBlePeriodicAdvertiserListSize, (), (override));
  MOCK_METHOD((uint16_t), GetAclBufferCountClassic, (), (override));
  MOCK_METHOD((uint8_t), GetAclBufferCountBle, (), (override));
  MOCK_METHOD((uint8_t), GetBleAcceptlistSize, (), (override));
  MOCK_METHOD((uint8_t), GetBleResolvingListMaxSize, (), (override));
  MOCK_METHOD((void), SetBleResolvingListMaxSize, (int ResolvingListMax_size),
              (override));
  MOCK_METHOD((uint8_t*), GetLocalSupportedCodecs, (uint8_t * NumberOfCodecs),
              (override));
  MOCK_METHOD((uint8_t), GetLeAllInitiatingPhys, (), (override));
  MOCK_METHOD((uint8_t), ClearEventFilter, (), (override));
  MOCK_METHOD((uint8_t), ClearEventMask, (), (override));
  MOCK_METHOD((uint8_t), LeRand, (bluetooth::hci::LeRandCallback), (override));
  MOCK_METHOD((uint8_t), SetEventFilterConnectionSetupAllDevices, (),
              (override));
  MOCK_METHOD((uint8_t), SetEventFilterAllowDeviceConnection,
              (std::vector<RawAddress> devices), (override));
  MOCK_METHOD((uint8_t), SetDefaultEventMaskExcept,
              (uint64_t mask, uint64_t le_mask), (override));
  MOCK_METHOD((uint8_t), SetEventFilterInquiryResultAllDevices, (), (override));
};

void SetMockControllerInterface(
    MockControllerInterface* mock_controller_interface);
}  // namespace controller
