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

#include "log.h"
#include "model/controller/link_layer_controller.h"
#include "packets/hci_packets.h"

#pragma GCC diagnostic ignored "-Wunused-parameter"

namespace rootcanal::apcf {

bool ApcfScanner::HasFilterIndex(uint8_t apcf_filter_index) const {
  return std::any_of(std::begin(filters), std::end(filters), [&](auto it) {
    return it.filter_index == apcf_filter_index;
  });
}

void ApcfScanner::ClearFilterIndex(uint8_t apcf_filter_index) {
  std::remove_if(std::begin(broadcaster_address_filters),
                 std::end(broadcaster_address_filters),
                 [&](auto it) { return it.filter_index == apcf_filter_index; });
  std::remove_if(std::begin(service_uuid_filters),
                 std::end(service_uuid_filters),
                 [&](auto it) { return it.filter_index == apcf_filter_index; });
  std::remove_if(std::begin(service_solicitation_uuid_filters),
                 std::end(service_solicitation_uuid_filters),
                 [&](auto it) { return it.filter_index == apcf_filter_index; });
  std::remove_if(std::begin(local_name_filters), std::end(local_name_filters),
                 [&](auto it) { return it.filter_index == apcf_filter_index; });
  std::remove_if(std::begin(manufacturer_data_filters),
                 std::end(manufacturer_data_filters),
                 [&](auto it) { return it.filter_index == apcf_filter_index; });
  std::remove_if(std::begin(service_data_filters),
                 std::end(service_data_filters),
                 [&](auto it) { return it.filter_index == apcf_filter_index; });
  std::remove_if(std::begin(ad_type_filters), std::end(ad_type_filters),
                 [&](auto it) { return it.filter_index == apcf_filter_index; });
}

ErrorCode ApcfScanner::UpdateFilterList(std::vector<GapDataFilter> &filter_list,
                                  size_t max_filter_list_size,
                                  bluetooth::hci::ApcfAction action,
                                  uint8_t filter_index,
                                  std::vector<uint8_t> gap_data,
                                  std::vector<uint8_t> gap_data_mask) {

  if (!apcf_scanner_.HasFilterIndex(apcf_filter_index)) {
    INFO(id_, "apcf filter index {} is not configured", apcf_filter_index);
    return ErrorCode::UNKNOWN_CONNECTION;
  }


  switch (action) {
    case ApcfAction::ADD: {
      if (filter_list.size() == max_filter_list_size) {
        INFO(id_, "reached max number of apcf filters");
        return ErrorCode::MEMORY_CAPACITY_EXCEEDED;
      }

      filter_list.push_back(
          rootcanal::apcf::GapDataFilter{
              .filter_index = filter_index,
              .gap_data = gap_data,
              .gap_data_mask = gap_data_mask,
          });
      return ErrorCode::SUCCESS;
    }
    case ApcfAction::DELETE: {
      // Delete will delete the specified data in the specified filter.
      std::remove_if(
          std::begin(filter_list),
          std::end(filter_list), [&](auto it) {
            return it.filter_index == filter_index &&
                   it.gap_data == gap_data &&
                   it.gap_data_mask == gap_data_mask;
          });
      return ErrorCode::SUCCESS;
    }
    case ApcfAction::CLEAR: {
      // Clear will clear all data in the specified filter.
      std::remove_if(
          std::begin(filter_list),
          std::end(filter_list),
          [&](auto it) { return it.filter_index == filter_index; });
      return ErrorCode::SUCCESS;
    }
    default:
      INFO(id_, "unknown apcf action {}", apcf_action);
      break;
  }

  return ErrorCode::INVALID_HCI_COMMAND_PARAMETERS;
}

}  // namespace rootcanal::apcf

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
        INFO(id_, "apcf filter index {} already configured", apcf_filter_index);
        return ErrorCode::INVALID_HCI_COMMAND_PARAMETERS;
      }

      if (*apcf_available_spaces == 0) {
        INFO(id_, "reached max number of apcf filters");
        return ErrorCode::MEMORY_CAPACITY_EXCEEDED;
      }

      apcf_scanner_.filters.push_back(rootcanal::apcf::Filter{
          .filter_index = apcf_filter_index,
          .feature_selection = apcf_feature_selection,
          .list_logic_type = apcf_list_logic_type,
          .filter_logic_type = apcf_filter_logic_type,
          .rssi_high_thresh = rssi_high_thresh,
          .delivery_mode = delivery_mode,
          .onfound_timeout = onfound_timeout,
          .onfound_timeout_cnt = onfound_timeout_cnt,
          .rssi_low_thresh = rssi_low_thresh,
          .onlost_timeout = onlost_timeout,
          .num_of_tracking_entries = num_of_tracking_entries,
      });

      *apcf_available_spaces -= 1;
      return ErrorCode::SUCCESS;
    }

    case ApcfAction::DELETE: {
      if (!apcf_scanner_.HasFilterIndex(apcf_filter_index)) {
        INFO(id_, "apcf filter index {} is not configured", apcf_filter_index);
        return ErrorCode::UNKNOWN_CONNECTION;
      }

      std::remove_if(
          std::begin(apcf_scanner_.filters), std::end(apcf_scanner_.filters),
          [&](auto it) { return it.filter_index == apcf_filter_index; });

      apcf_scanner_.ClearFilterIndex(apcf_filter_index);
      *apcf_available_spaces += 1;
      return ErrorCode::SUCCESS;
    }

    case ApcfAction::CLEAR: {
      if (!apcf_scanner_.HasFilterIndex(apcf_filter_index)) {
        INFO(id_, "apcf filter index {} is not configured", apcf_filter_index);
        return ErrorCode::UNKNOWN_CONNECTION;
      }

      apcf_scanner_.ClearFilterIndex(apcf_filter_index);
      return ErrorCode::SUCCESS;
    }

    default:
      INFO(id_, "unknown apcf action {}", apcf_action);
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
      properties_.le_apcf_broadcaster_address_filter_list_size -
      apcf_scanner_.broadcaster_address_filters.size();

  if (!apcf_scanner_.HasFilterIndex(apcf_filter_index)) {
    INFO(id_, "apcf filter index {} is not configured", apcf_filter_index);
    return ErrorCode::UNKNOWN_CONNECTION;
  }

  switch (apcf_action) {
    case ApcfAction::ADD: {
      if (*apcf_available_spaces == 0) {
        INFO(id_, "reached max number of apcf filters");
        return ErrorCode::MEMORY_CAPACITY_EXCEEDED;
      }

      apcf_scanner_.broadcaster_address_filters.push_back(
          rootcanal::apcf::BroadcasterAddressFilter{
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
          std::end(apcf_scanner_.broadcaster_address_filters), [&](auto it) {
            return it.filter_index == apcf_filter_index &&
                   it.broadcaster_address == apcf_broadcaster_address &&
                   it.application_address_type == apcf_application_address_type;
          });

      *apcf_available_spaces =
          properties_.le_apcf_broadcaster_address_filter_list_size -
          apcf_scanner_.broadcaster_address_filters.size();
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
          properties_.le_apcf_broadcaster_address_filter_list_size -
          apcf_scanner_.broadcaster_address_filters.size();
      return ErrorCode::SUCCESS;
    }
    default:
      INFO(id_, "unknown apcf action {}", apcf_action);
      break;
  }

  return ErrorCode::INVALID_HCI_COMMAND_PARAMETERS;
}

ErrorCode LinkLayerController::LeApcfServiceUuid(
    ApcfAction apcf_action, uint8_t apcf_filter_index,
    std::vector<uint8_t> apcf_uuid_data, uint8_t* apcf_available_spaces) {

  size_t uuid_data_size = apcf_uuid_data.size() / 2;
  std::vector<uint8_t> uuid_data(
    std::begin(apcf_uuid_data),
    std::begin(apcf_uuid_data) + uuid_data_size);
  std::vector<uint8_t> uuid_data_mask(
    std::begin(apcf_uuid_data) + uuid_data_size,
    std::end(apcf_uuid_data));

  ErrorCode status = apcf_scanner_.UpdateFilterList(
    apcf_scanner_.service_uuid_filters,
    properties_.le_apcf_service_uuid_filter_list_size,
    apcf_action,
    apcf_filter_index,
    uuid_data,
    uuid_data_mask);

  *apcf_available_spaces =
      properties_.le_apcf_service_uuid_filter_list_size -
      apcf_scanner_.service_uuid_filters.size();

  return status;
}

ErrorCode LinkLayerController::LeApcfServiceSolicitationUuid(
    ApcfAction apcf_action, uint8_t apcf_filter_index,
    std::vector<uint8_t> apcf_uuid_data, uint8_t* apcf_available_spaces) {

  size_t uuid_data_size = apcf_uuid_data.size() / 2;
  std::vector<uint8_t> uuid_data(
    std::begin(apcf_uuid_data),
    std::begin(apcf_uuid_data) + uuid_data_size);
  std::vector<uint8_t> uuid_data_mask(
    std::begin(apcf_uuid_data) + uuid_data_size,
    std::end(apcf_uuid_data));

  ErrorCode status = apcf_scanner_.UpdateFilterList(
    apcf_scanner_.service_solicitation_uuid_filters,
    properties_.le_apcf_service_solicitation_uuid_filter_list_size,
    apcf_action,
    apcf_filter_index,
    uuid_data,
    uuid_data_mask);

  *apcf_available_spaces =
      properties_.le_apcf_service_solicitation_uuid_filter_list_size -
      apcf_scanner_.service_solicitation_uuid_filters.size();

  return status;
}

ErrorCode LinkLayerController::LeApcfLocalName(
    ApcfAction apcf_action, uint8_t apcf_filter_index,
    std::vector<uint8_t> apcf_local_name, uint8_t* apcf_available_spaces) {

  size_t local_name_data_size = apcf_local_name_data.size() / 2;
  std::vector<uint8_t> local_name_data(
    std::begin(apcf_local_name_data),
    std::begin(apcf_local_name_data) + local_name_data_size);
  std::vector<uint8_t> local_name_data_mask(
    std::begin(apcf_local_name_data) + local_name_data_size,
    std::end(apcf_local_name_data));

  ErrorCode status = apcf_scanner_.UpdateFilterList(
    apcf_scanner_.local_name_filters,
    properties_.le_apcf_local_name_filter_list_size,
    apcf_action,
    apcf_filter_index,
    local_name_data,
    local_name_data_mask);

  *apcf_available_spaces =
      properties_.le_apcf_local_name_filter_list_size -
      apcf_scanner_.local_name_filters.size();

  return status;
}

ErrorCode LinkLayerController::LeApcfManufacturerData(
    ApcfAction apcf_action, uint8_t apcf_filter_index,
    std::vector<uint8_t> apcf_manufacturer_data,
    uint8_t* apcf_available_spaces) {

  size_t manufacturer_data_size = apcf_manufacturer_data.size() / 2;
  std::vector<uint8_t> manufacturer_data(
    std::begin(apcf_manufacturer_data),
    std::begin(apcf_manufacturer_data) + manufacturer_data_size);
  std::vector<uint8_t> manufacturer_data_mask(
    std::begin(apcf_manufacturer_data) + manufacturer_data_size,
    std::end(apcf_manufacturer_data));

  ErrorCode status = apcf_scanner_.UpdateFilterList(
    apcf_scanner_.manufacturer_filters,
    properties_.le_apcf_manufacturer_filter_list_size,
    apcf_action,
    apcf_filter_index,
    manufacturer_data,
    manufacturer_data_mask);

  *apcf_available_spaces =
      properties_.le_apcf_manufacturer_filter_list_size -
      apcf_scanner_.manufacturer_filters.size();

  return status;
}

ErrorCode LinkLayerController::LeApcfServiceData(
    ApcfAction apcf_action, uint8_t apcf_filter_index,
    std::vector<uint8_t> apcf_service_data, uint8_t* apcf_available_spaces) {

  size_t service_data_size = apcf_service_data.size() / 2;
  std::vector<uint8_t> service_data(
    std::begin(apcf_service_data),
    std::begin(apcf_service_data) + service_data_size);
  std::vector<uint8_t> service_data_mask(
    std::begin(apcf_service_data) + service_data_size,
    std::end(apcf_service_data));

  ErrorCode status = apcf_scanner_.UpdateFilterList(
    apcf_scanner_.service_filters,
    properties_.le_apcf_service_filter_list_size,
    apcf_action,
    apcf_filter_index,
    service_data,
    service_data_mask);

  *apcf_available_spaces =
      properties_.le_apcf_service_filter_list_size -
      apcf_scanner_.service_filters.size();

  return status;
}

ErrorCode LinkLayerController::LeApcfAdTypeFilter(
    ApcfAction apcf_action, uint8_t apcf_filter_index, uint8_t ad_type,
    std::vector<uint8_t> apcf_ad_data, std::vector<uint8_t> apcf_ad_data_mask,
    uint8_t* apcf_available_spaces) {
  return ErrorCode::INVALID_HCI_COMMAND_PARAMETERS;
}

}  // namespace rootcanal
