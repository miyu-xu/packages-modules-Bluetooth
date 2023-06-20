/******************************************************************************
 *
 *  Copyright 1999-2012 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

/******************************************************************************
 *
 *  This file contains function of the HCIC unit to format and send HCI
 *  commands.
 *
 ******************************************************************************/

#include <base/functional/callback_forward.h>
#include <stddef.h>
#include <string.h>

#include "bt_target.h"
#include "device/include/device_iot_config.h"
#include "device/include/esco_parameters.h"
#include "gd/common/init_flags.h"
#include "hci/hci_packets.h"
#include "main/shim/acl_api.h"
#include "main/shim/helpers.h"
#include "osi/include/allocator.h"
#include "stack/include/acl_hci_link_interface.h"
#include "stack/include/bt_hdr.h"
#include "stack/include/bt_name.h"
#include "stack/include/bt_octets.h"
#include "stack/include/btu.h"
#include "stack/include/btu_hcif.h"
#include "stack/include/hcimsgs.h"
#include "types/raw_address.h"

using bluetooth::hci::InquiryBuilder;

static void btsnd_hcic_inquiry(const LAP inq_lap, uint8_t duration,
                               uint8_t response_cnt) {
  bluetooth::hci::Lap lap;
  lap.lap_ = inq_lap[0] & 0x3f;
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::INQUIRY,
      bluetooth::hci::InquiryBuilder::Create(lap, duration, response_cnt));
}

static void btsnd_hcic_inq_cancel(void) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::INQUIRY_CANCEL,
                    bluetooth::hci::InquiryCancelBuilder::Create());
}

static void btsnd_hcic_disconnect(uint16_t handle, uint8_t reason) {
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::DISCONNECT,
      bluetooth::hci::DisconnectBuilder::Create(
          handle, static_cast<bluetooth::hci::DisconnectReason>(reason)));
}

void btsnd_hcic_add_SCO_conn(uint16_t handle, uint16_t packet_types) {
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::ADD_SCO_CONNECTION,
      bluetooth::hci::AddScoConnectionBuilder::Create(handle, packet_types));
}

void btsnd_hcic_create_conn_cancel(const RawAddress& dest) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::CREATE_CONNECTION_CANCEL,
                    bluetooth::hci::CreateConnectionCancelBuilder::Create(
                        bluetooth::ToGdAddress(dest)));
}

void btsnd_hcic_accept_conn(const RawAddress& dest, uint8_t role) {
  auto request_role =
      (role == 0
           ? bluetooth::hci::AcceptConnectionRequestRole::BECOME_CENTRAL
           : bluetooth::hci::AcceptConnectionRequestRole::REMAIN_PERIPHERAL);
  btu_hcif_send_cmd(bluetooth::hci::OpCode::ACCEPT_CONNECTION_REQUEST,
                    bluetooth::hci::AcceptConnectionRequestBuilder::Create(
                        bluetooth::ToGdAddress(dest), request_role));
}

void btsnd_hcic_reject_conn(const RawAddress& dest, uint8_t reason) {
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::REJECT_CONNECTION_REQUEST,
      bluetooth::hci::RejectConnectionRequestBuilder::Create(
          bluetooth::ToGdAddress(dest),
          static_cast<bluetooth::hci::RejectConnectionReason>(reason)));
}

void btsnd_hcic_link_key_req_reply(const RawAddress& bd_addr,
                                   const LinkKey& link_key) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::LINK_KEY_REQUEST_REPLY,
                    bluetooth::hci::LinkKeyRequestReplyBuilder::Create(
                        bluetooth::ToGdAddress(bd_addr), link_key));
}

void btsnd_hcic_link_key_neg_reply(const RawAddress& bd_addr) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::LINK_KEY_REQUEST_NEGATIVE_REPLY,
                    bluetooth::hci::LinkKeyRequestNegativeReplyBuilder::Create(
                        bluetooth::ToGdAddress(bd_addr)));
}

void btsnd_hcic_pin_code_req_reply(const RawAddress& bd_addr,
                                   uint8_t pin_code_len, PIN_CODE pin_code) {
  std::array<uint8_t, 16> pin{0};
  uint8_t i = 0;
  for (i = 0; i < pin_code_len && i < PIN_CODE_LEN; i++) pin[i] = pin_code[i];

  for (; i < PIN_CODE_LEN; i++) pin[i] = 0;

  btu_hcif_send_cmd(bluetooth::hci::OpCode::PIN_CODE_REQUEST_REPLY,
                    bluetooth::hci::PinCodeRequestReplyBuilder::Create(
                        bluetooth::ToGdAddress(bd_addr), pin_code_len, pin));
}

void btsnd_hcic_pin_code_neg_reply(const RawAddress& bd_addr) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::PIN_CODE_REQUEST_NEGATIVE_REPLY,
                    bluetooth::hci::PinCodeRequestNegativeReplyBuilder::Create(
                        bluetooth::ToGdAddress(bd_addr)));
}

void btsnd_hcic_change_conn_type(uint16_t handle, uint16_t packet_types) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::CHANGE_CONNECTION_PACKET_TYPE,
                    bluetooth::hci::ChangeConnectionPacketTypeBuilder::Create(
                        handle, packet_types));
}

void btsnd_hcic_auth_request(uint16_t handle) {
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::AUTHENTICATION_REQUESTED,
      bluetooth::hci::AuthenticationRequestedBuilder::Create(handle));
}

void btsnd_hcic_set_conn_encrypt(uint16_t handle, bool enable) {
  bluetooth::hci::Enable en = enable ? bluetooth::hci::Enable::ENABLED
                                     : bluetooth::hci::Enable::DISABLED;
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::SET_CONNECTION_ENCRYPTION,
      bluetooth::hci::SetConnectionEncryptionBuilder::Create(handle, en));
}

void btsnd_hcic_rmt_name_req(const RawAddress& bd_addr,
                             uint8_t page_scan_rep_mode, uint8_t page_scan_mode,
                             uint16_t clock_offset) {
  bluetooth::shim::ACL_RemoteNameRequest(bd_addr, page_scan_rep_mode,
                                         page_scan_mode, clock_offset);
}

void btsnd_hcic_rmt_name_req_cancel(const RawAddress& bd_addr) {
  bluetooth::shim::ACL_CancelRemoteNameRequest(bd_addr);
}

void btsnd_hcic_rmt_ext_features(uint16_t handle, uint8_t page_num) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::READ_REMOTE_EXTENDED_FEATURES,
                    bluetooth::hci::ReadRemoteExtendedFeaturesBuilder::Create(
                        handle, page_num));
}

void btsnd_hcic_rmt_ver_req(uint16_t handle) {
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::READ_REMOTE_VERSION_INFORMATION,
      bluetooth::hci::ReadRemoteVersionInformationBuilder::Create(handle));
}

void btsnd_hcic_read_rmt_clk_offset(uint16_t handle) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::READ_CLOCK_OFFSET,
                    bluetooth::hci::ReadClockOffsetBuilder::Create(handle));
}

void btsnd_hcic_setup_esco_conn(uint16_t handle, uint32_t transmit_bandwidth,
                                uint32_t receive_bandwidth,
                                uint16_t max_latency, uint16_t voice,
                                uint8_t retrans_effort, uint16_t packet_types) {
  auto retransmission_effort =
      (retrans_effort == 1
           ? bluetooth::hci::RetransmissionEffort::OPTIMIZED_FOR_POWER
           : (retrans_effort == 2
                  ? bluetooth::hci::RetransmissionEffort::
                        OPTIMIZED_FOR_LINK_QUALITY
                  : (retrans_effort == 0xff
                         ? bluetooth::hci::RetransmissionEffort::DO_NOT_CARE
                         : bluetooth::hci::RetransmissionEffort::
                               NO_RETRANSMISSION)));
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::SETUP_SYNCHRONOUS_CONNECTION,
      bluetooth::hci::SetupSynchronousConnectionBuilder::Create(
          handle, transmit_bandwidth, receive_bandwidth, max_latency, voice,
          retransmission_effort, packet_types));
}

void btsnd_hcic_accept_esco_conn(const RawAddress& bd_addr,
                                 uint32_t transmit_bandwidth,
                                 uint32_t receive_bandwidth,
                                 uint16_t max_latency, uint16_t content_fmt,
                                 uint8_t retrans_effort,
                                 uint16_t packet_types) {
  auto retransmission_effort =
      (retrans_effort == 1
           ? bluetooth::hci::RetransmissionEffort::OPTIMIZED_FOR_POWER
           : (retrans_effort == 2
                  ? bluetooth::hci::RetransmissionEffort::
                        OPTIMIZED_FOR_LINK_QUALITY
                  : (retrans_effort == 0xff
                         ? bluetooth::hci::RetransmissionEffort::DO_NOT_CARE
                         : bluetooth::hci::RetransmissionEffort::
                               NO_RETRANSMISSION)));
  btu_hcif_send_cmd(bluetooth::hci::OpCode::ACCEPT_SYNCHRONOUS_CONNECTION,
                    bluetooth::hci::AcceptSynchronousConnectionBuilder::Create(
                        bluetooth::ToGdAddress(bd_addr), transmit_bandwidth,
                        receive_bandwidth, max_latency, content_fmt,
                        retransmission_effort, packet_types));
}

void btsnd_hcic_reject_esco_conn(const RawAddress& bd_addr, uint8_t reason) {
  // Default to "SECURITY_REASONS"
  auto reject_reason =
      (reason == 0x0d
           ? bluetooth::hci::RejectConnectionReason::LIMITED_RESOURCES
           : (reason == 0x0f
                  ? bluetooth::hci::RejectConnectionReason::UNACCEPTABLE_BD_ADDR
                  : bluetooth::hci::RejectConnectionReason::SECURITY_REASONS));
  btu_hcif_send_cmd(bluetooth::hci::OpCode::REJECT_SYNCHRONOUS_CONNECTION,
                    bluetooth::hci::RejectSynchronousConnectionBuilder::Create(
                        bluetooth::ToGdAddress(bd_addr), reject_reason));
}

void btsnd_hcic_hold_mode(uint16_t handle, uint16_t max_hold_period,
                          uint16_t min_hold_period) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::HOLD_MODE,
                    bluetooth::hci::HoldModeBuilder::Create(
                        handle, max_hold_period, min_hold_period));
}

void btsnd_hcic_sniff_mode(uint16_t handle, uint16_t max_sniff_period,
                           uint16_t min_sniff_period, uint16_t sniff_attempt,
                           uint16_t sniff_timeout) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::SNIFF_MODE,
                    bluetooth::hci::SniffModeBuilder::Create(
                        handle, max_sniff_period, min_sniff_period,
                        sniff_attempt, sniff_timeout));
}

void btsnd_hcic_exit_sniff_mode(uint16_t handle) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::EXIT_SNIFF_MODE,
                    bluetooth::hci::ExitSniffModeBuilder::Create(handle));
}

void btsnd_hcic_park_mode(uint16_t handle, uint16_t beacon_max_interval,
                          uint16_t beacon_min_interval) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::PARK_STATE,
                    bluetooth::hci::ParkStateBuilder::Create(
                        handle, beacon_max_interval, beacon_min_interval));
}

void btsnd_hcic_exit_park_mode(uint16_t handle) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::EXIT_PARK_STATE,
                    bluetooth::hci::ExitParkStateBuilder::Create(handle));
}

static void btsnd_hcic_switch_role(const RawAddress& bd_addr, uint8_t role) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::SWITCH_ROLE,
                    bluetooth::hci::SwitchRoleBuilder::Create(
                        bluetooth::ToGdAddress(bd_addr),
                        role == 0 ? bluetooth::hci::Role::CENTRAL
                                  : bluetooth::hci::Role::PERIPHERAL));
}

void btsnd_hcic_write_policy_set(uint16_t handle, uint16_t settings) {
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::WRITE_LINK_POLICY_SETTINGS,
      bluetooth::hci::WriteLinkPolicySettingsBuilder::Create(handle, settings));
}

void btsnd_hcic_write_def_policy_set(uint16_t settings) {
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::WRITE_DEFAULT_LINK_POLICY_SETTINGS,
      bluetooth::hci::WriteDefaultLinkPolicySettingsBuilder::Create(settings));
}

void btsnd_hcic_set_event_filter(uint8_t filt_type, uint8_t filt_cond_type,
                                 uint8_t* filt_cond, uint8_t filt_cond_len) {
  auto filter_condition_type =
      static_cast<bluetooth::hci::FilterConditionType>(filt_cond_type);
  bluetooth::hci::ClassOfDevice cod{};
  bluetooth::hci::ClassOfDevice cod_mask{};
  bluetooth::hci::AutoAcceptFlag flag;
  if (static_cast<bluetooth::hci::FilterType>(filt_type) ==
      bluetooth::hci::FilterType::CONNECTION_SETUP) {
    switch (*(filt_cond + 7)) {
      case 1:
        flag = bluetooth::hci::AutoAcceptFlag::AUTO_ACCEPT_OFF;
        break;
      case 2:
        flag =
            bluetooth::hci::AutoAcceptFlag::AUTO_ACCEPT_ON_ROLE_SWITCH_DISABLED;
        break;
      case 3:
        flag =
            bluetooth::hci::AutoAcceptFlag::AUTO_ACCEPT_ON_ROLE_SWITCH_ENABLED;
        break;
      default:
        flag = bluetooth::hci::AutoAcceptFlag::AUTO_ACCEPT_OFF;
    }
  }
  if (filter_condition_type ==
          bluetooth::hci::FilterConditionType::CLASS_OF_DEVICE &&
      filt_cond_len >= 2 * bluetooth::hci::ClassOfDevice::kLength + 1) {
    cod.FromOctets(filt_cond);
    cod_mask.FromOctets(filt_cond + bluetooth::hci::ClassOfDevice::kLength);
  }
  bluetooth::hci::Address addr{};
  if (filter_condition_type == bluetooth::hci::FilterConditionType::ADDRESS &&
      filt_cond_len >= bluetooth::hci::Address::kLength) {
    addr.FromOctets(filt_cond);
  }
  switch (static_cast<bluetooth::hci::FilterType>(filt_type)) {
    case bluetooth::hci::FilterType::CLEAR_ALL_FILTERS:
      btu_hcif_send_cmd(
          bluetooth::hci::OpCode::SET_EVENT_FILTER,
          bluetooth::hci::SetEventFilterClearAllBuilder::Create());
      break;
    case bluetooth::hci::FilterType::INQUIRY_RESULT:
      switch (filter_condition_type) {
        case bluetooth::hci::FilterConditionType::ALL_DEVICES:
          btu_hcif_send_cmd(
              bluetooth::hci::OpCode::SET_EVENT_FILTER,
              bluetooth::hci::SetEventFilterInquiryResultAllDevicesBuilder::
                  Create());
          break;
        case bluetooth::hci::FilterConditionType::CLASS_OF_DEVICE:
          btu_hcif_send_cmd(
              bluetooth::hci::OpCode::SET_EVENT_FILTER,
              bluetooth::hci::SetEventFilterInquiryResultClassOfDeviceBuilder::
                  Create(cod, cod_mask));
          break;
        case bluetooth::hci::FilterConditionType::ADDRESS:
          btu_hcif_send_cmd(
              bluetooth::hci::OpCode::SET_EVENT_FILTER,
              bluetooth::hci::SetEventFilterInquiryResultAddressBuilder::Create(
                  addr));
          break;
      }
      break;
    case bluetooth::hci::FilterType::CONNECTION_SETUP:
      switch (filter_condition_type) {
        case bluetooth::hci::FilterConditionType::ALL_DEVICES:
          btu_hcif_send_cmd(
              bluetooth::hci::OpCode::SET_EVENT_FILTER,
              bluetooth::hci::SetEventFilterConnectionSetupAllDevicesBuilder::
                  Create(flag));
          break;
        case bluetooth::hci::FilterConditionType::CLASS_OF_DEVICE:
          btu_hcif_send_cmd(
              bluetooth::hci::OpCode::SET_EVENT_FILTER,
              bluetooth::hci::
                  SetEventFilterConnectionSetupClassOfDeviceBuilder::Create(
                      cod, cod_mask, flag));
          break;
        case bluetooth::hci::FilterConditionType::ADDRESS:
          btu_hcif_send_cmd(
              bluetooth::hci::OpCode::SET_EVENT_FILTER,
              bluetooth::hci::SetEventFilterConnectionSetupAddressBuilder::
                  Create(addr, flag));
          break;
      }
      break;
  }
}

void btsnd_hcic_write_pin_type(uint8_t type) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::WRITE_PIN_TYPE,
                    bluetooth::hci::WritePinTypeBuilder::Create(
                        type == 0 ? bluetooth::hci::PinType::VARIABLE
                                  : bluetooth::hci::PinType::FIXED));
}

void btsnd_hcic_delete_stored_key(const RawAddress& bd_addr,
                                  bool delete_all_flag) {
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::DELETE_STORED_LINK_KEY,
      bluetooth::hci::DeleteStoredLinkKeyBuilder::Create(
          bluetooth::ToGdAddress(bd_addr),
          delete_all_flag
              ? bluetooth::hci::DeleteStoredLinkKeyDeleteAllFlag::ALL
              : bluetooth::hci::DeleteStoredLinkKeyDeleteAllFlag::
                    SPECIFIED_BD_ADDR));
}

void btsnd_hcic_change_name(BD_NAME name) {
  std::array<uint8_t, 248> name_array;
  for (size_t i = 0; i < 248; i++) {
    name_array[i] = name[i];
  }
  btu_hcif_send_cmd(bluetooth::hci::OpCode::WRITE_LOCAL_NAME,
                    bluetooth::hci::WriteLocalNameBuilder::Create(name_array));
}

void btsnd_hcic_read_name(void) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::READ_LOCAL_NAME,
                    bluetooth::hci::ReadLocalNameBuilder::Create());
}

void btsnd_hcic_write_page_tout(uint16_t timeout) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::WRITE_PAGE_TIMEOUT,
                    bluetooth::hci::WritePageTimeoutBuilder::Create(timeout));
}

void btsnd_hcic_write_scan_enable(uint8_t flag) {
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::WRITE_SCAN_ENABLE,
      bluetooth::hci::WriteScanEnableBuilder::Create(
          (flag == 0
               ? bluetooth::hci::ScanEnable::NO_SCANS
               : (flag == 1
                      ? bluetooth::hci::ScanEnable::INQUIRY_SCAN_ONLY
                      : (flag == 2 ? bluetooth::hci::ScanEnable::PAGE_SCAN_ONLY
                                   : bluetooth::hci::ScanEnable::
                                         INQUIRY_AND_PAGE_SCAN)))));
}

void btsnd_hcic_write_pagescan_cfg(uint16_t interval, uint16_t window) {
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::WRITE_PAGE_SCAN_ACTIVITY,
      bluetooth::hci::WritePageScanActivityBuilder::Create(interval, window));
}

void btsnd_hcic_write_inqscan_cfg(uint16_t interval, uint16_t window) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::WRITE_INQUIRY_SCAN_ACTIVITY,
                    bluetooth::hci::WriteInquiryScanActivityBuilder::Create(
                        interval, window));
}

void btsnd_hcic_write_auth_enable(uint8_t flag) {
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::WRITE_AUTHENTICATION_ENABLE,
      bluetooth::hci::WriteAuthenticationEnableBuilder::Create(
          flag == 0 ? bluetooth::hci::AuthenticationEnable::NOT_REQUIRED
                    : bluetooth::hci::AuthenticationEnable::REQUIRED));
}

void btsnd_hcic_write_dev_class(DEV_CLASS dev_class) {
  bluetooth::hci::ClassOfDevice cod;
  cod.FromOctets(dev_class);
  btu_hcif_send_cmd(bluetooth::hci::OpCode::WRITE_CLASS_OF_DEVICE,
                    bluetooth::hci::WriteClassOfDeviceBuilder::Create(
                        bluetooth::hci::ClassOfDevice(cod)));
}

void btsnd_hcic_write_voice_settings(uint16_t flags) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::WRITE_VOICE_SETTING,
                    bluetooth::hci::WriteVoiceSettingBuilder::Create(flags));
}

void btsnd_hcic_write_auto_flush_tout(uint16_t handle, uint16_t tout) {
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::WRITE_AUTOMATIC_FLUSH_TIMEOUT,
      bluetooth::hci::WriteAutomaticFlushTimeoutBuilder::Create(handle, tout));
}

void btsnd_hcic_read_tx_power(uint16_t handle, uint8_t type) {
  bluetooth::hci::TransmitPowerLevelType power_type =
      (type == 0 ? bluetooth::hci::TransmitPowerLevelType::CURRENT
                 : bluetooth::hci::TransmitPowerLevelType::MAXIMUM);
  btu_hcif_send_cmd(bluetooth::hci::OpCode::READ_TRANSMIT_POWER_LEVEL,
                    bluetooth::hci::ReadTransmitPowerLevelBuilder::Create(
                        handle, power_type));
}

void btsnd_hcic_write_link_super_tout(uint16_t handle, uint16_t timeout) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::WRITE_LINK_SUPERVISION_TIMEOUT,
                    bluetooth::hci::WriteLinkSupervisionTimeoutBuilder::Create(
                        handle, timeout));
}

void btsnd_hcic_write_cur_iac_lap(uint8_t num_cur_iac, LAP* const iac_lap) {
  std::vector<bluetooth::hci::Lap> laps_to_write;
  for (size_t i = 0; i < num_cur_iac; i++) {
    bluetooth::hci::Lap lap;
    lap.lap_ = static_cast<uint8_t>(iac_lap[i][0]) & 0x3fu;
    laps_to_write.push_back(lap);
  }
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::WRITE_CURRENT_IAC_LAP,
      bluetooth::hci::WriteCurrentIacLapBuilder::Create(laps_to_write));
}

/******************************************
 *    Lisbon Features
 ******************************************/
void btsnd_hcic_sniff_sub_rate(uint16_t handle, uint16_t max_lat,
                               uint16_t min_remote_lat,
                               uint16_t min_local_lat) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::SNIFF_SUBRATING,
                    bluetooth::hci::SniffSubratingBuilder::Create(
                        handle, max_lat, min_remote_lat, min_local_lat));
}

/**** Extended Inquiry Response Commands ****/
void btsnd_hcic_write_ext_inquiry_response(
    const std::array<uint8_t, 240>& eir_data, uint8_t fec_req) {
  bluetooth::hci::FecRequired fec_required =
      (fec_req == 0 ? bluetooth::hci::FecRequired::NOT_REQUIRED
                    : bluetooth::hci::FecRequired::REQUIRED);
  auto eir_array =
      std::make_shared<std::vector<uint8_t>>(eir_data.begin(), eir_data.end());
  bluetooth::packet::PacketView<true> eir_to_parse(eir_array);
  auto itr = eir_to_parse.begin();
  std::vector<bluetooth::hci::GapData> eir_data_parsed;
  while (itr.NumBytesRemaining() > 0) {
    bluetooth::hci::GapData gap_data;
    itr = bluetooth::hci::GapData::Parse(&gap_data, itr);
    eir_data_parsed.push_back(std::move(gap_data));
  }

  btu_hcif_send_cmd(bluetooth::hci::OpCode::WRITE_EXTENDED_INQUIRY_RESPONSE,
                    bluetooth::hci::WriteExtendedInquiryResponseBuilder::Create(
                        fec_required, eir_data_parsed));
}

void btsnd_hcic_io_cap_req_reply(const RawAddress& bd_addr, uint8_t capability,
                                 uint8_t oob_present, uint8_t auth_req) {
  bluetooth::hci::IoCapability io_cap =
      static_cast<bluetooth::hci::IoCapability>(capability);
  bluetooth::hci::OobDataPresent oob_data_present =
      (oob_present == 1
           ? bluetooth::hci::OobDataPresent::P_192_PRESENT
           : (oob_present == 2
                  ? bluetooth::hci::OobDataPresent::P_256_PRESENT
                  : (oob_present == 3
                         ? bluetooth::hci::OobDataPresent::P_256_PRESENT
                         : bluetooth::hci::OobDataPresent::NOT_PRESENT)));
  bluetooth::hci::AuthenticationRequirements authentication_requirements =
      (auth_req == 0
           ? bluetooth::hci::AuthenticationRequirements::NO_BONDING
           : (auth_req == 1
                  ? bluetooth::hci::AuthenticationRequirements::
                        NO_BONDING_MITM_PROTECTION
                  : (auth_req == 2
                         ? bluetooth::hci::AuthenticationRequirements::
                               DEDICATED_BONDING
                         : (auth_req == 3
                                ? bluetooth::hci::AuthenticationRequirements::
                                      DEDICATED_BONDING_MITM_PROTECTION
                                : (auth_req == 4
                                       ? bluetooth::hci::
                                             AuthenticationRequirements::
                                                 GENERAL_BONDING
                                       : bluetooth::hci::AuthenticationRequirements::
                                             GENERAL_BONDING_MITM_PROTECTION)))));

  btu_hcif_send_cmd(bluetooth::hci::OpCode::IO_CAPABILITY_REQUEST_REPLY,
                    bluetooth::hci::IoCapabilityRequestReplyBuilder::Create(
                        bluetooth::ToGdAddress(bd_addr), io_cap,
                        oob_data_present, authentication_requirements));
}

void btsnd_hcic_enhanced_set_up_synchronous_connection(
    uint16_t conn_handle, enh_esco_params_t* p_params) {
  bluetooth::hci::ScoCodingFormat tx_coding_format;
  tx_coding_format.coding_format_ =
      static_cast<bluetooth::hci::ScoCodingFormatValues>(
          p_params->transmit_coding_format.coding_format);
  tx_coding_format.company_id_ = p_params->transmit_coding_format.company_id;
  tx_coding_format.vendor_specific_codec_id_ =
      p_params->transmit_coding_format.vendor_specific_codec_id;
  bluetooth::hci::ScoCodingFormat rx_coding_format;
  rx_coding_format.coding_format_ =
      static_cast<bluetooth::hci::ScoCodingFormatValues>(
          p_params->transmit_coding_format.coding_format);
  rx_coding_format.company_id_ = p_params->transmit_coding_format.company_id;
  rx_coding_format.vendor_specific_codec_id_ =
      p_params->transmit_coding_format.vendor_specific_codec_id;
  bluetooth::hci::ScoCodingFormat input_coding_format;
  input_coding_format.coding_format_ =
      static_cast<bluetooth::hci::ScoCodingFormatValues>(
          p_params->transmit_coding_format.coding_format);
  input_coding_format.company_id_ = p_params->transmit_coding_format.company_id;
  input_coding_format.vendor_specific_codec_id_ =
      p_params->transmit_coding_format.vendor_specific_codec_id;
  bluetooth::hci::ScoCodingFormat output_coding_format;
  output_coding_format.coding_format_ =
      static_cast<bluetooth::hci::ScoCodingFormatValues>(
          p_params->transmit_coding_format.coding_format);
  output_coding_format.company_id_ =
      p_params->transmit_coding_format.company_id;
  output_coding_format.vendor_specific_codec_id_ =
      p_params->transmit_coding_format.vendor_specific_codec_id;
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::ENHANCED_SETUP_SYNCHRONOUS_CONNECTION,
      bluetooth::hci::EnhancedSetupSynchronousConnectionBuilder::Create(
          conn_handle, p_params->transmit_bandwidth,
          p_params->receive_bandwidth, tx_coding_format, rx_coding_format,
          p_params->transmit_codec_frame_size,
          p_params->receive_codec_frame_size, p_params->input_bandwidth,
          p_params->output_bandwidth, input_coding_format, output_coding_format,
          p_params->input_coded_data_size, p_params->output_coded_data_size,
          static_cast<bluetooth::hci::ScoPcmDataFormat>(
              p_params->input_pcm_data_format),
          static_cast<bluetooth::hci::ScoPcmDataFormat>(
              p_params->output_pcm_data_format),
          p_params->input_pcm_payload_msb_position,
          p_params->output_pcm_payload_msb_position,
          static_cast<bluetooth::hci::ScoDataPath>(p_params->input_data_path),
          static_cast<bluetooth::hci::ScoDataPath>(p_params->output_data_path),
          p_params->input_transport_unit_size,
          p_params->output_transport_unit_size, p_params->max_latency_ms,
          p_params->packet_types,
          static_cast<bluetooth::hci::RetransmissionEffort>(
              p_params->retransmission_effort)));
}

void btsnd_hcic_enhanced_accept_synchronous_connection(
    const RawAddress& bd_addr, enh_esco_params_t* p_params) {
  bluetooth::hci::ScoCodingFormat tx_coding_format;
  tx_coding_format.coding_format_ =
      static_cast<bluetooth::hci::ScoCodingFormatValues>(
          p_params->transmit_coding_format.coding_format);
  tx_coding_format.company_id_ = p_params->transmit_coding_format.company_id;
  tx_coding_format.vendor_specific_codec_id_ =
      p_params->transmit_coding_format.vendor_specific_codec_id;
  bluetooth::hci::ScoCodingFormat rx_coding_format;
  rx_coding_format.coding_format_ =
      static_cast<bluetooth::hci::ScoCodingFormatValues>(
          p_params->transmit_coding_format.coding_format);
  rx_coding_format.company_id_ = p_params->transmit_coding_format.company_id;
  rx_coding_format.vendor_specific_codec_id_ =
      p_params->transmit_coding_format.vendor_specific_codec_id;
  bluetooth::hci::ScoCodingFormat input_coding_format;
  input_coding_format.coding_format_ =
      static_cast<bluetooth::hci::ScoCodingFormatValues>(
          p_params->transmit_coding_format.coding_format);
  input_coding_format.company_id_ = p_params->transmit_coding_format.company_id;
  input_coding_format.vendor_specific_codec_id_ =
      p_params->transmit_coding_format.vendor_specific_codec_id;
  bluetooth::hci::ScoCodingFormat output_coding_format;
  output_coding_format.coding_format_ =
      static_cast<bluetooth::hci::ScoCodingFormatValues>(
          p_params->transmit_coding_format.coding_format);
  output_coding_format.company_id_ =
      p_params->transmit_coding_format.company_id;
  output_coding_format.vendor_specific_codec_id_ =
      p_params->transmit_coding_format.vendor_specific_codec_id;
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::ENHANCED_ACCEPT_SYNCHRONOUS_CONNECTION,
      bluetooth::hci::EnhancedAcceptSynchronousConnectionBuilder::Create(
          bluetooth::ToGdAddress(bd_addr), p_params->transmit_bandwidth,
          p_params->receive_bandwidth, tx_coding_format, rx_coding_format,
          p_params->transmit_codec_frame_size,
          p_params->receive_codec_frame_size, p_params->input_bandwidth,
          p_params->output_bandwidth, input_coding_format, output_coding_format,
          p_params->input_coded_data_size, p_params->output_coded_data_size,
          static_cast<bluetooth::hci::ScoPcmDataFormat>(
              p_params->input_pcm_data_format),
          static_cast<bluetooth::hci::ScoPcmDataFormat>(
              p_params->output_pcm_data_format),
          p_params->input_pcm_payload_msb_position,
          p_params->output_pcm_payload_msb_position,
          static_cast<bluetooth::hci::ScoDataPath>(p_params->input_data_path),
          static_cast<bluetooth::hci::ScoDataPath>(p_params->output_data_path),
          p_params->input_transport_unit_size,
          p_params->output_transport_unit_size, p_params->max_latency_ms,
          p_params->packet_types,
          static_cast<bluetooth::hci::RetransmissionEffort>(
              p_params->retransmission_effort)));
}

void btsnd_hcic_io_cap_req_neg_reply(const RawAddress& bd_addr,
                                     uint8_t err_code) {
  bluetooth::hci::ErrorCode error_code =
      static_cast<bluetooth::hci::ErrorCode>(err_code);
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::IO_CAPABILITY_REQUEST_NEGATIVE_REPLY,
      bluetooth::hci::IoCapabilityRequestNegativeReplyBuilder::Create(
          bluetooth::ToGdAddress(bd_addr), error_code));
}

void btsnd_hcic_read_local_oob_data(void) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::READ_LOCAL_OOB_DATA,
                    bluetooth::hci::ReadLocalOobDataBuilder::Create());
}

void btsnd_hcic_user_conf_reply(const RawAddress& bd_addr, bool is_yes) {
  if (is_yes) {
    btu_hcif_send_cmd(
        bluetooth::hci::OpCode::USER_CONFIRMATION_REQUEST_REPLY,
        bluetooth::hci::UserConfirmationRequestReplyBuilder::Create(
            bluetooth::ToGdAddress(bd_addr)));
  } else {
    btu_hcif_send_cmd(
        bluetooth::hci::OpCode::USER_CONFIRMATION_REQUEST_NEGATIVE_REPLY,
        bluetooth::hci::UserConfirmationRequestNegativeReplyBuilder::Create(
            bluetooth::ToGdAddress(bd_addr)));
  }
}

void btsnd_hcic_user_passkey_reply(const RawAddress& bd_addr, uint32_t value) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::USER_PASSKEY_REQUEST_REPLY,
                    bluetooth::hci::UserPasskeyRequestReplyBuilder::Create(
                        bluetooth::ToGdAddress(bd_addr), value));
}

void btsnd_hcic_user_passkey_neg_reply(const RawAddress& bd_addr) {
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::USER_PASSKEY_REQUEST_NEGATIVE_REPLY,
      bluetooth::hci::UserPasskeyRequestNegativeReplyBuilder::Create(
          bluetooth::ToGdAddress(bd_addr)));
}

void btsnd_hcic_rem_oob_reply(const RawAddress& bd_addr, const Octet16& c,
                              const Octet16& r) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::REMOTE_OOB_DATA_REQUEST_REPLY,
                    bluetooth::hci::RemoteOobDataRequestReplyBuilder::Create(
                        bluetooth::ToGdAddress(bd_addr), c, r));
}

void btsnd_hcic_rem_oob_neg_reply(const RawAddress& bd_addr) {
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::REMOTE_OOB_DATA_REQUEST_NEGATIVE_REPLY,
      bluetooth::hci::RemoteOobDataRequestNegativeReplyBuilder::Create(
          bluetooth::ToGdAddress(bd_addr)));
}

/**** end of Simple Pairing Commands ****/

void btsnd_hcic_enhanced_flush(uint16_t handle, uint8_t /* packet_type */) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::ENHANCED_FLUSH,
                    bluetooth::hci::EnhancedFlushBuilder::Create(handle));
}

/*************************
 * End of Lisbon Commands
 *************************/

void btsnd_hcic_read_rssi(uint16_t handle) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::READ_RSSI,
                    bluetooth::hci::ReadRssiBuilder::Create(handle));
}

static void read_encryption_key_size_complete(ReadEncKeySizeCb cb,
                                              bluetooth::hci::EventView event) {
  auto complete = bluetooth::hci::ReadEncryptionKeySizeCompleteView::Create(
      bluetooth::hci::CommandCompleteView::Create(event));
  ASSERT(complete.IsValid());
  uint8_t status = static_cast<uint8_t>(complete.GetStatus());
  uint16_t handle = complete.GetConnectionHandle();
  uint8_t key_size = complete.GetKeySize();
  std::move(cb).Run(status, handle, key_size);
}

void btsnd_hcic_read_encryption_key_size(uint16_t handle, ReadEncKeySizeCb cb) {
  btu_hcif_send_cmd_with_cb(
      FROM_HERE, bluetooth::hci::OpCode::READ_ENCRYPTION_KEY_SIZE,
      bluetooth::hci::ReadEncryptionKeySizeBuilder::Create(handle),
      base::BindOnce(&read_encryption_key_size_complete, std::move(cb)));
}

void btsnd_hcic_read_failed_contact_counter(uint16_t handle) {
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::READ_FAILED_CONTACT_COUNTER,
      bluetooth::hci::ReadFailedContactCounterBuilder::Create(handle));
}

void btsnd_hcic_enable_test_mode(void) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::ENABLE_DEVICE_UNDER_TEST_MODE,
                    bluetooth::hci::EnableDeviceUnderTestModeBuilder::Create());
}

void btsnd_hcic_write_inqscan_type(uint8_t type) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::WRITE_INQUIRY_SCAN_TYPE,
                    bluetooth::hci::WriteInquiryScanTypeBuilder::Create(
                        static_cast<bluetooth::hci::InquiryScanType>(type)));
}

void btsnd_hcic_write_inquiry_mode(uint8_t mode) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::WRITE_INQUIRY_MODE,
                    bluetooth::hci::WriteInquiryModeBuilder::Create(
                        static_cast<bluetooth::hci::InquiryMode>(mode)));
}

void btsnd_hcic_write_pagescan_type(uint8_t type) {
  btu_hcif_send_cmd(bluetooth::hci::OpCode::WRITE_PAGE_SCAN_TYPE,
                    bluetooth::hci::WritePageScanTypeBuilder::Create(
                        static_cast<bluetooth::hci::PageScanType>(type)));
}

static void btsnd_hcic_vendor_spec_complete(tBTM_VSC_CMPL_CB* p_vsc_cplt_cback,
                                            uint16_t opcode, uint8_t* data,
                                            uint16_t len) {
  /* If there was a callback address for vcs complete, call it */
  if (p_vsc_cplt_cback) {
    tBTM_VSC_CMPL vcs_cplt_params;
    vcs_cplt_params.opcode = opcode;
    vcs_cplt_params.param_len = len;
    vcs_cplt_params.p_param_buf = data;
    /* Call the VSC complete callback function */
    (*p_vsc_cplt_cback)(&vcs_cplt_params);
  }
}

void btsnd_hcic_vendor_spec_cmd(uint16_t opcode, uint8_t len, uint8_t* p_data,
                                tBTM_VSC_CMPL_CB* p_cmd_cplt_cback) {
  uint16_t v_opcode = HCI_GRP_VENDOR_SPECIFIC | opcode;

  btu_hcif_send_cmd_with_cb(
      FROM_HERE, v_opcode, p_data, len,
      base::BindOnce(&btsnd_hcic_vendor_spec_complete,
                     base::Unretained(p_cmd_cplt_cback), v_opcode));
}

void btsnd_hcic_configure_data_path(uint8_t data_path_direction,
                                    uint8_t data_path_id,
                                    std::vector<uint8_t> vendor_config) {
  btu_hcif_send_cmd(
      bluetooth::hci::OpCode::CONFIGURE_DATA_PATH,
      bluetooth::hci::ConfigureDataPathBuilder::Create(
          data_path_direction == 0 ? bluetooth::hci::DataPathDirection::INPUT
                                   : bluetooth::hci::DataPathDirection::OUTPUT,
          data_path_id, std::move(vendor_config)));
}

bluetooth::legacy::hci::Interface interface_ = {
    // LINK_CONTROL
    .StartInquiry = btsnd_hcic_inquiry,                   // OCF 0x0401
    .InquiryCancel = btsnd_hcic_inq_cancel,               // OCF 0x0402
    .Disconnect = btsnd_hcic_disconnect,                  // OCF 0x0406
    .ChangeConnectionPacketType = btsnd_hcic_change_conn_type,  // OCF 0x040F,
    .StartRoleSwitch = btsnd_hcic_switch_role,               // OCF 0x080B,
};

const bluetooth::legacy::hci::Interface&
bluetooth::legacy::hci::GetInterface() {
  return interface_;
}
