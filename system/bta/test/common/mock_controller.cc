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

#include "mock_controller.h"

#include "device/include/controller.h"

static controller::MockControllerInterface* controller_interface = nullptr;

void controller::SetMockControllerInterface(
    MockControllerInterface* interface) {
  controller_interface = interface;
}

uint16_t get_iso_data_size(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetIsoDataSize();
}

uint8_t get_iso_buffer_count(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetIsoBufferCount();
}

bool supports_ble_isochronous_broadcaster(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsBleIsochronousBroadcaster();
}

bool supports_ble_2m_phy(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsBle2mPhy();
}

bool supports_ble_connected_isochronous_stream_central(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsBleConnectedIsochronousStreamCentral();
}

bool supports_ble_connected_isochronous_stream_peripheral(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface
      ->SupportsBleConnectedIsochronousStreamPeripheral();
}

bool supports_configure_data_path(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsConfigureDataPath();
}

bool supports_ble_coded_phy(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsBleCodedPhy();
}

bool supports_simple_pairing(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsSimplePairing();
}

bool supports_secure_connections(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsSecureConnections();
}

bool supports_simultaneous_le_bredr(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsSimultaneousLeBredr();
}

bool supports_reading_remote_extended_features(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsReadingRemoteExtendedFeatures();
}

bool supports_interlaced_inquiry_scan(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsInterlacedInquiryScan();
}

bool supports_rssi_with_inquiry_results(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsRssiWithInquiryResults();
}

bool supports_extended_inquiry_response(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsExtendedInquiryResponse();
}

bool supports_central_peripheral_role_switch(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsCentralPeripheralRoleSwitch();
}

bool supports_enhanced_setup_synchronous_connection(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsEnhancedSetupSynchronousConnection();
}

bool supports_enhanced_accept_synchronous_connection(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsEnhancedAcceptSynchronousConnection();
}

bool supports_3_slot_packets(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->Supports3SlotPackets();
}

bool supports_5_slot_packets(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->Supports5SlotPackets();
}

bool supports_classic_2m_phy(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsClassic2mPhy();
}

bool supports_classic_3m_phy(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsClassic3mPhy();
}

bool supports_3_slot_edr_packets(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->Supports3SlotEdrPackets();
}

bool supports_5_slot_edr_packets(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->Supports5SlotEdrPackets();
}

bool supports_sco(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsSco();
}

bool supports_hv2_packets(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsHv2Packets();
}

bool supports_hv3_packets(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsHv3Packets();
}

bool supports_ev3_packets(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsEv3Packets();
}

bool supports_ev4_packets(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsEv4Packets();
}

bool supports_ev5_packets(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsEv5Packets();
}

bool supports_esco_2m_phy(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsEsco2mPhy();
}

bool supports_esco_3m_phy(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsEsco3mPhy();
}

bool supports_3_slot_esco_edr_packets(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->Supports3SlotEscoEdrPackets();
}

bool supports_role_switch(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsRoleSwitch();
}

bool supports_hold_mode(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsHoldMode();
}

bool supports_sniff_mode(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsSniffMode();
}

bool supports_park_mode(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsParkMode();
}

bool supports_non_flushable_pb(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsNonFlushablePb();
}

bool supports_sniff_subrating(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsSniffSubrating();
}

bool supports_encryption_pause(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsEncryptionPause();
}

bool supports_set_min_encryption_key_size(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsSetMinEncryptionKeySize();
}

bool supports_read_encryption_key_size(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsReadEncryptionKeySize();
}

bool supports_ble(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsBle();
}

bool supports_ble_packet_extension(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsBlePacketExtension();
}

bool supports_ble_connection_parameters_request(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsBleConnectionParametersRequest();
}

bool supports_ble_privacy(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsBlePrivacy();
}

bool supports_ble_set_privacy_mode(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsBleSetPrivacyMode();
}

bool supports_ble_extended_advertising(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsBleExtendedAdvertising();
}

bool supports_ble_periodic_advertising(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsBlePeriodicAdvertising();
}

bool supports_ble_peripheral_initiated_feature_exchange(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsBlePeripheralInitiatedFeatureExchange();
}

bool supports_ble_connection_parameter_request(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsBleConnectionParameterRequest();
}

bool supports_ble_periodic_advertising_sync_transfer_sender(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface
      ->SupportsBlePeriodicAdvertisingSyncTransferSender();
}

bool supports_ble_periodic_advertising_sync_transfer_recipient(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface
      ->SupportsBlePeriodicAdvertisingSyncTransferRecipient();
}

bool supports_ble_synchronized_receiver(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsBleSynchronizedReceiver();
}

bool supports_ble_connection_subrating(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsBleConnectionSubrating();
}

bool supports_ble_connection_subrating_host(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SupportsBleConnectionSubratingHost();
}

bool get_is_ready(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetIsReady();
}

const RawAddress* get_address(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetAddress();
}

const bt_version_t* get_bt_version(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetBtVersion();
}

const uint8_t* get_ble_supported_states(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetBleSupportedStates();
}

uint16_t get_acl_data_size_classic(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetAclDataSizeClassic();
}

uint16_t get_acl_data_size_ble(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetAclDataSizeBle();
}

uint16_t get_acl_packet_size_classic(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetAclPacketSizeClassic();
}

uint16_t get_acl_packet_size_ble(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetAclPacketSizeBle();
}

uint16_t get_iso_packet_size(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetIsoPacketSize();
}

uint16_t get_ble_default_data_packet_length(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetBleDefaultDataPacketLength();
}

uint16_t get_ble_maximum_tx_data_length(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetBleMaximumTxDataLength();
}

uint16_t get_ble_maximum_tx_time(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetBleMaximumTxTime();
}

uint16_t get_ble_maximum_advertising_data_length(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetBleMaximumAdvertisingDataLength();
}

uint8_t get_ble_number_of_supported_advertising_sets(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetBleNumberOfSupportedAdvertisingSets();
}

uint8_t get_ble_periodic_advertiser_list_size(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetBlePeriodicAdvertiserListSize();
}

uint16_t get_acl_buffer_count_classic(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetAclBufferCountClassic();
}

uint8_t get_acl_buffer_count_ble(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetAclBufferCountBle();
}

uint8_t get_ble_acceptlist_size(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetBleAcceptlistSize();
}

uint8_t get_ble_resolving_list_max_size(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetBleResolvingListMaxSize();
}

void set_ble_resolving_list_max_size(int resolving_list_max_size) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SetBleResolvingListMaxSize(
      resolving_list_max_size);
}

uint8_t* get_local_supported_codecs(uint8_t* number_of_codecs) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetLocalSupportedCodecs(number_of_codecs);
}

uint8_t get_le_all_initiating_phys(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->GetLeAllInitiatingPhys();
}

uint8_t clear_event_filter(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->ClearEventFilter();
}

uint8_t clear_event_mask(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->ClearEventMask();
}

uint8_t le_rand(bluetooth::hci::LeRandCallback cb) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->LeRand(std::move(cb));
}

uint8_t set_event_filter_connection_setup_all_devices(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SetEventFilterConnectionSetupAllDevices();
}

uint8_t set_event_filter_allow_device_connection(
    std::vector<RawAddress> devices) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SetEventFilterAllowDeviceConnection(devices);
}

uint8_t set_default_event_mask_except(uint64_t mask, uint64_t le_mask) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SetDefaultEventMaskExcept(mask, le_mask);
}

uint8_t set_event_filter_inquiry_result_all_devices(void) {
  LOG_ASSERT(controller_interface) << "Mock controller not set!";
  return controller_interface->SetEventFilterInquiryResultAllDevices();
}

const controller_t* controller_get_interface() {
  static controller_t* controller_instance = new controller_t();

  controller_instance->get_iso_data_size = &get_iso_data_size;
  controller_instance->get_iso_buffer_count = &get_iso_buffer_count;
  controller_instance->SupportsBleIsochronousBroadcaster =
      &supports_ble_isochronous_broadcaster;
  controller_instance->SupportsBle2mPhy = &supports_ble_2m_phy;
  controller_instance->SupportsBleConnectedIsochronousStreamCentral =
      &supports_ble_connected_isochronous_stream_central;
  controller_instance->SupportsBleConnectedIsochronousStreamPeripheral =
      &supports_ble_connected_isochronous_stream_peripheral;
  controller_instance->supports_configure_data_path =
      &supports_configure_data_path;
  controller_instance->SupportsBleCodedPhy = &supports_ble_coded_phy;
  controller_instance->SupportsSimplePairing = &supports_simple_pairing;
  controller_instance->SupportsSecureConnections = &supports_secure_connections;
  controller_instance->SupportsSimultaneousLeBrEdr =
      &supports_simultaneous_le_bredr;
  controller_instance->supports_reading_remote_extended_features =
      &supports_reading_remote_extended_features;
  controller_instance->SupportsInterlacedInquiryScan =
      &supports_interlaced_inquiry_scan;
  controller_instance->SupportsRssiWithInquiryResults =
      &supports_rssi_with_inquiry_results;
  controller_instance->SupportsExtendedInquiryResponse =
      &supports_extended_inquiry_response;
  controller_instance->SupportsRoleSwitch =
      &supports_central_peripheral_role_switch;
  controller_instance->supports_enhanced_setup_synchronous_connection =
      &supports_enhanced_setup_synchronous_connection;
  controller_instance->supports_enhanced_accept_synchronous_connection =
      &supports_enhanced_accept_synchronous_connection;
  controller_instance->Supports3SlotPackets = &supports_3_slot_packets;
  controller_instance->Supports5SlotPackets = &supports_5_slot_packets;
  controller_instance->SupportsClassic2mPhy = &supports_classic_2m_phy;
  controller_instance->SupportsClassic3mPhy = &supports_classic_3m_phy;
  controller_instance->Supports3SlotEdrPackets = &supports_3_slot_edr_packets;
  controller_instance->Supports5SlotEdrPackets = &supports_5_slot_edr_packets;
  controller_instance->SupportsSco = &supports_sco;
  controller_instance->SupportsHv2Packets = &supports_hv2_packets;
  controller_instance->SupportsHv3Packets = &supports_hv3_packets;
  controller_instance->SupportsEv3Packets = &supports_ev3_packets;
  controller_instance->SupportsEv4Packets = &supports_ev4_packets;
  controller_instance->SupportsEv5Packets = &supports_ev5_packets;
  controller_instance->SupportsEsco2mPhy = &supports_esco_2m_phy;
  controller_instance->SupportsEsco3mPhy = &supports_esco_3m_phy;
  controller_instance->Supports3SlotEscoEdrPackets =
      &supports_3_slot_esco_edr_packets;
  controller_instance->SupportsRoleSwitch = &supports_role_switch;
  controller_instance->SupportsHoldMode = &supports_hold_mode;
  controller_instance->SupportsSniffMode = &supports_sniff_mode;
  controller_instance->SupportsParkMode = &supports_park_mode;
  controller_instance->SupportsNonFlushablePb = &supports_non_flushable_pb;
  controller_instance->SupportsSniffSubrating = &supports_sniff_subrating;
  controller_instance->SupportsEncryptionPause = &supports_encryption_pause;
  controller_instance->supports_set_min_encryption_key_size =
      &supports_set_min_encryption_key_size;
  controller_instance->supports_read_encryption_key_size =
      &supports_read_encryption_key_size;
  controller_instance->SupportsBle = &supports_ble;
  controller_instance->SupportsBleDataPacketLengthExtension =
      &supports_ble_packet_extension;
  controller_instance->SupportsBleConnectionParametersRequest =
      &supports_ble_connection_parameters_request;
  controller_instance->SupportsBlePrivacy = &supports_ble_privacy;
  controller_instance->supports_ble_set_privacy_mode =
      &supports_ble_set_privacy_mode;
  controller_instance->SupportsBleExtendedAdvertising =
      &supports_ble_extended_advertising;
  controller_instance->SupportsBlePeriodicAdvertising =
      &supports_ble_periodic_advertising;
  controller_instance->SupportsBlePeripheralInitiatedFeaturesExchange =
      &supports_ble_peripheral_initiated_feature_exchange;
  controller_instance->SupportsBleConnectionParametersRequest =
      &supports_ble_connection_parameter_request;
  controller_instance->SupportsBlePeriodicAdvertisingSyncTransferSender =
      &supports_ble_periodic_advertising_sync_transfer_sender;
  controller_instance->SupportsBlePeriodicAdvertisingSyncTransferRecipient =
      &supports_ble_periodic_advertising_sync_transfer_recipient;
  controller_instance->SupportsBleIsochronousBroadcaster =
      &supports_ble_isochronous_broadcaster;
  controller_instance->SupportsBleSynchronizedReceiver =
      &supports_ble_synchronized_receiver;
  controller_instance->SupportsBleConnectionSubrating =
      &supports_ble_connection_subrating;
  controller_instance->SupportsBleConnectionSubratingHost =
      &supports_ble_connection_subrating_host;
  controller_instance->get_is_ready = &get_is_ready;
  controller_instance->get_address = &get_address;
  controller_instance->get_bt_version = &get_bt_version;
  controller_instance->get_ble_supported_states = &get_ble_supported_states;
  controller_instance->get_acl_data_size_classic = &get_acl_data_size_classic;
  controller_instance->get_acl_data_size_ble = &get_acl_data_size_ble;
  controller_instance->get_acl_packet_size_classic =
      &get_acl_packet_size_classic;
  controller_instance->get_acl_packet_size_ble = &get_acl_packet_size_ble;
  controller_instance->get_iso_packet_size = &get_iso_packet_size;
  controller_instance->get_ble_default_data_packet_length =
      &get_ble_default_data_packet_length;
  controller_instance->get_ble_maximum_tx_data_length =
      &get_ble_maximum_tx_data_length;
  controller_instance->get_ble_maximum_tx_time = &get_ble_maximum_tx_time;
  controller_instance->get_ble_maximum_advertising_data_length =
      &get_ble_maximum_advertising_data_length;
  controller_instance->get_ble_number_of_supported_advertising_sets =
      &get_ble_number_of_supported_advertising_sets;
  controller_instance->get_ble_periodic_advertiser_list_size =
      &get_ble_periodic_advertiser_list_size;
  controller_instance->get_acl_buffer_count_classic =
      &get_acl_buffer_count_classic;
  controller_instance->get_acl_buffer_count_ble = &get_acl_buffer_count_ble;
  controller_instance->get_ble_acceptlist_size = &get_ble_acceptlist_size;
  controller_instance->get_ble_resolving_list_max_size =
      &get_ble_resolving_list_max_size;
  controller_instance->set_ble_resolving_list_max_size =
      &set_ble_resolving_list_max_size;
  controller_instance->get_local_supported_codecs = &get_local_supported_codecs;
  controller_instance->get_le_all_initiating_phys = &get_le_all_initiating_phys;
  controller_instance->clear_event_filter = &clear_event_filter;
  controller_instance->clear_event_mask = &clear_event_mask;
  controller_instance->le_rand = &le_rand;
  controller_instance->set_event_filter_connection_setup_all_devices =
      &set_event_filter_connection_setup_all_devices;
  controller_instance->set_event_filter_allow_device_connection =
      &set_event_filter_allow_device_connection;
  controller_instance->set_default_event_mask_except =
      &set_default_event_mask_except;
  controller_instance->set_event_filter_inquiry_result_all_devices =
      &set_event_filter_inquiry_result_all_devices;

  return controller_instance;
}
