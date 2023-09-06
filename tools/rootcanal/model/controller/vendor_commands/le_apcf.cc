/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License") {

 }
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

#include <algorithm>
#include <cstdint>

#include "model/controller/link_layer_controller.h"
#include "packets/hci_packets.h"

#pragma GCC diagnostic ignored "-Wunused-parameter"

namespace rootcanal::apcf {

bool ApcfScanner::HasFilterIndex(uint8_t apcf_filter_index) const {
    return std::any_of(
        std::begin(filters),
        std::end(filters),
        [&](auto it) { return it.filter_index == apcf_filter_index; });
}

void ApcfScanner::ClearFilterIndex(uint8_t apcf_filter_index) {
    std::remove_if(
        std::begin(broadcaster_address_filters),
        std::end(broadcaster_address_filters),
        [&](auto it) { return it.filter_index == apcf_filter_index; });
    std::remove_if(
        std::begin(service_uuid_filters),
        std::end(service_uuid_filters),
        [&](auto it) { return it.filter_index == apcf_filter_index; });
    std::remove_if(
        std::begin(service_solicitation_uuid_filters),
        std::end(service_solicitation_uuid_filters),
        [&](auto it) { return it.filter_index == apcf_filter_index; });
    std::remove_if(
        std::begin(local_name_filters),
        std::end(local_name_filters),
        [&](auto it) { return it.filter_index == apcf_filter_index; });
    std::remove_if(
        std::begin(manufacturer_data_filters),
        std::end(manufacturer_data_filters),
        [&](auto it) { return it.filter_index == apcf_filter_index; });
    std::remove_if(
        std::begin(service_data_filters),
        std::end(service_data_filters),
        [&](auto it) { return it.filter_index == apcf_filter_index; });
    std::remove_if(
        std::begin(ad_type_filters),
        std::end(ad_type_filters),
        [&](auto it) { return it.filter_index == apcf_filter_index; });
}

}  // rootcanal::apcf

namespace rootcanal {

using bluetooth::hci::ApcfAction;

ErrorCode LinkLayerController::LeApcfEnable(bool apcf_enable) {
  apcf_scanner_.enable = apcf_enable;
  return ErrorCode::SUCCESS;
}

ErrorCode LinkLayerController::LeApcfSetFilteringParameters(
    ApcfAction apcf_action, uint8_t apcf_filter_index,
    uint16_t apcf_feature_selection, uint16_t apcf_list_logic_type,
    uint8_t apcf_filter_logic_type, uint8_t rssi_high_thresh,
    bluetooth::hci::DeliveryMode delivery_mode, uint16_t onfound_timeout,
    uint8_t onfound_timeout_cnt, uint8_t rssi_low_thresh,
    uint16_t onlost_timeout, uint16_t num_of_tracking_entries,
    uint8_t* apcf_available_spaces) {

  *apcf_available_spaces =
        properties_.le_apcf_filter_list_size - apcf_scanner_.filters.size();

  switch (apcf_action) {
  case ApcfAction::ADD: {
    if (apcf_scanner_.HasFilterIndex(apcf_filter_index)) {
        LOG_INFO(id_, "apcf filter index {} already configured", apcf_filter_index);
        return ErrorCode::INVALID_HCI_COMMAND_PARAMETERS;
    }

    if (*apcf_available_spaces == 0) {
        LOG_INFO(id_, "reached max number of apcf filters");
        return ErrorCode::MEMORY_CAPACITY_EXCEEDED;
    }

    apcf_scanner_.filters.push_back(rootcanal::apcf::Filter {
        .index = apcf_filter_index,
        .feature_selection = apcf_feature_selection,
        .list_logic_type = apcf_list_logic_type,
        .filter_logic_type = apcf_filter_logic_type,
        .rssi_high_thresh = apcf_rssi_high_thresh,
        .delivery_mode = apcf_delivery_mode,
        .onfound_timeout = apcf_onfound_timeout,
        .onfound_timeout_cnt = apcf_onfound_timeout_cnt,
        .rssi_low_thresh = apcf_rssi_low_thresh,
        .onlost_timeout = apcf_onlost_timeout,
        .num_of_tracking_entries = apcf_num_of_tracking_entries,
    });

    *apcf_available_spaces -= 1;
    return ErrorCode::SUCCESS;
  }

  case ApcfAction::DELETE: {
    if (!apcf_scanner_.HasFilterIndex(apcf_filter_index)) {
        LOG_INFO(id_, "apcf filter index {} is not configured", apcf_filter_index);
        return ErrorCode::UNKNOWN_CONNECTION;
    }

    std::remove_if(
        std::begin(apcf_scanner_.filters),
        std::end(apcf_scanner_.filters),
        [&](auto it) { return it.filter_index == apcf_filter_index; });

    apcf_scanner_.ClearFilterIndex(apcf_filter_index);
    *apcf_available_spaces += 1;
    return ErrorCode::SUCCESS;
  }

  case ApcfAction::CLEAR: {
    if (!apcf_scanner_.HasFilterIndex(apcf_filter_index)) {
        LOG_INFO(id_, "apcf filter index {} is not configured", apcf_filter_index);
        return ErrorCode::UNKNOWN_CONNECTION;
    }

    apcf_scanner_.ClearFilterIndex(apcf_filter_index);
    return ErrorCode::SUCCESS;
  }

  default:
    LOG_INFO(id_, "unknown apcf action {}", apcf_action);
    break;
  }

  return ErrorCode::INVALID_HCI_COMMAND_PARAMETERS;
}

ErrorCode LinkLayerController::LeApcfBroadcasterAddress(
    ApcfAction apcf_action, uint8_t apcf_filter_index,
    bluetooth::hci::Address apcf_broadcaster_address,
    bluetooth::hci::ApcfApplicationAddressType apcf_application_address_type,
    uint8_t* apcf_available_spaces) {

  *apcf_available_spaces =
        properties_.le_apcf_broadcaster_address_filter_list_size - apcf_scanner_.broadcaster_address_filters.size();

    if (!apcf_scanner_.HasFilterIndex(apcf_filter_index)) {
        LOG_INFO(id_, "apcf filter index {} is not configured", apcf_filter_index);
        return ErrorCode::UNKNOWN_CONNECTION;
    }

    switch (apcf_action) {
    case ApcfAction::ADD: {
        if (*apcf_available_spaces == 0) {
            LOG_INFO(id_, "reached max number of apcf filters");
            return ErrorCode::MEMORY_CAPACITY_EXCEEDED;
        }

        apcf_scanner_.push_back(rootcanal::apcf::BroadcastAddressFilter {
            .filter_index = apcf_filter_index,
            .broadcaster_address = apcf_broadcaster_address,
            .application_address_type = apcf_application_address_type,
        });

        *apcf_available_spaces -= 1;
        return ErrorCode::SUCCESS;
    }
    case ApcfAction::DELETE: {
        // Delete will delete the specified broadcaster address in the
        // specified filter.
        std::remove_if(
            std::begin(apcf_scanner_.broadcaster_address_filters),
            std::end(apcf_scanner_.broadcaster_address_filters),
            [&](auto it) { return it.filter_index == apcf_filter_index &&
                it.broadcaster_address == apcf_broadcaster_address &&
                it.application_address_type == apcf_application_address_type; });

  *apcf_available_spaces =
        properties_.le_apcf_broadcaster_address_filter_list_size - apcf_scanner_.broadcaster_address_filters.size();
        return ErrorCode::SUCCESS;
    }
    case ApcfAction::CLEAR: {
        // Clear will clear all the broadcaster addresses in the specified
        // filter.
        std::remove_if(
            std::begin(apcf_scanner_.broadcaster_address_filters),
            std::end(apcf_scanner_.broadcaster_address_filters),
            [&](auto it) { return it.filter_index == apcf_filter_index; });

  *apcf_available_spaces =
        properties_.le_apcf_broadcaster_address_filter_list_size - apcf_scanner_.broadcaster_address_filters.size();
        return ErrorCode::SUCCESS;
    }
    default:
        LOG_INFO(id_, "unknown apcf action {}", apcf_action);
        break;
    }

  return ErrorCode::INVALID_HCI_COMMAND_PARAMETERS;
}

ErrorCode LinkLayerController::LeApcfServiceUuid(
    ApcfAction apcf_action, uint8_t apcf_filter_index,
    std::vector<uint8_t> acpf_uuid_data, uint8_t* apcf_available_spaces) {
  return ErrorCode::INVALID_HCI_COMMAND_PARAMETERS;
}

ErrorCode LinkLayerController::LeApcfServiceSolicitationUuid(
    ApcfAction apcf_action, uint8_t apcf_filter_index,
    std::vector<uint8_t> acpf_uuid_data, uint8_t* apcf_available_spaces) {
  return ErrorCode::INVALID_HCI_COMMAND_PARAMETERS;
}

ErrorCode LinkLayerController::LeApcfLocalName(
    ApcfAction apcf_action, uint8_t apcf_filter_index,
    std::vector<uint8_t> apcf_local_name, uint8_t* apcf_available_spaces) {
  return ErrorCode::INVALID_HCI_COMMAND_PARAMETERS;
}

ErrorCode LinkLayerController::LeApcfManufacturerData(
    ApcfAction apcf_action, uint8_t apcf_filter_index,
    std::vector<uint8_t> apcf_manufacturer_data,
    uint8_t* apcf_available_spaces) {
  return ErrorCode::INVALID_HCI_COMMAND_PARAMETERS;
}

ErrorCode LinkLayerController::LeApcfServiceData(
    ApcfAction apcf_action, uint8_t apcf_filter_index,
    std::vector<uint8_t> apcf_service_data, uint8_t* apcf_available_spaces) {
  return ErrorCode::INVALID_HCI_COMMAND_PARAMETERS;
}

ErrorCode LinkLayerController::LeApcfAdTypeFilter(
    ApcfAction apcf_action, uint8_t apcf_filter_index,
    uint8_t ad_type, std::vector<uint8_t> apcf_ad_data,
    std::vector<uint8_t> apcf_ad_data_mask, uint8_t* apcf_available_spaces) {
  return ErrorCode::INVALID_HCI_COMMAND_PARAMETERS;
}

}  // namespace rootcanal
