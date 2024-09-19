/*
 * Copyright (C) 2024 The Android Open Source Project
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

#include <base/functional/bind.h>
#include <base/functional/callback.h>
#include <base/strings/string_number_conversions.h>
#include <bluetooth/log.h>
#include <com_android_bluetooth_flags.h>
#include <hardware/bt_gatt_types.h>

#include <bitset>
#include <string>
#include <vector>

#include "bta_gatt_api.h"
#include "bta_gatt_queue.h"
#include "bta_gmap_client_api.h"
#include "bta_gmap_server_api.h"
#include "bta_le_audio_uuids.h"
#include "btm_sec.h"
#include "gap_api.h"
#include "gatt_api.h"
#include "bta/le_audio/le_audio_types.h"
#include "internal_include/bt_trace.h"
#include "include/hardware/bt_gmap.h"
#include "os/log.h"
#include "osi/include/properties.h"
#include "stack/include/bt_types.h"

#include "gd/hci/uuid.h"
#include "gd/os/rand.h"
#include "os/logging/log_adapter.h"
#include "stack/include/bt_types.h"
#include "stack/include/btm_ble_addr.h"

using bluetooth::Uuid;
using namespace bluetooth;

void GattsCallback(tBTA_GATTS_EVT event, tBTA_GATTS *p_data);

void OnGattConnect(tBTA_GATTS *p_data);

void OnGattDisconnect(tBTA_GATTS *p_data);

void OnGattServerRegister(tBTA_GATTS *p_data);

void OnServiceAdded(tGATT_STATUS status, int server_if,
                    std::vector <btgatt_db_element_t> service);

void OnReadCharacteristic(tBTA_GATTS *p_data);

bool GmapServer::is_offloader_support_gmap_ = false;
uint16_t GmapServer::server_if_ = 0;
std::unordered_map <uint16_t, GmapCharacteristic> GmapServer::characteristics_ = std::unordered_map<uint16_t, GmapCharacteristic>();
std::bitset<8> GmapServer::role_ = 0b0001;
// AOSP's LE Audio source support multi-sink on default
std::bitset<8> GmapServer::UGG_feature_ = static_cast<uint8_t>(bluetooth::gmap::UGGFeatureBitMask::MultisinkFeatureSupport);

bool GmapServer::IsGmapServerEnabled() {
    // for UGG, both GMAP Server and Client are needed. So server and client share the same flag.
    bool flag = com::android::bluetooth::flags::leaudio_gmap_client();
    bool system_prop = osi_property_get_bool("bluetooth.profile.gmap.enabled", false);

    bool result = flag && system_prop && is_offloader_support_gmap_;
    log::info("GmapServerEnabled=%d, flag=%d, system_prop=%d, offloader_support=%d", result,
              system_prop, flag, GmapServer::is_offloader_support_gmap_);
    return result;
}

void GmapServer::UpdateGmapOffloaderSupport(bool value) {
    GmapServer::is_offloader_support_gmap_ = value;
}

void GmapServer::DebugDump(int fd) {
    std::stringstream stream;
    stream << "GmapServer is enabled: " << GmapServer::IsGmapServerEnabled() << "\n";

    stream << "GmapServer Role Handle: " << GmapServer::GetRoleHandle() << " Role: "
           << GmapServer::role_ << "\n";

    stream << "GmapServer UGT Handle: " << GmapServer::GetUGGFeatureHandle() << " UGT Feature: "
           << GmapServer::UGG_feature_
           << "\n";
    dprintf(fd, "%s", stream.str().c_str());
}

void GmapServer::Initialize(std::bitset<8> role, std::bitset<8> UGG_feature) {
    GmapServer::role_ = role;
    GmapServer::UGG_feature_ = UGG_feature;
    log::info("GmapServer initialized, role=%d, UGG_feature=%d", role, UGG_feature);

    BTA_GATTS_AppRegister(
            bluetooth::le_audio::uuid::kGamingAudioServiceUuid,
            [](tBTA_GATTS_EVT event, tBTA_GATTS *p_data) {
                if (p_data) {
                    GattsCallback(event, p_data);
                }
            },
            false);
}

void GmapServer::Initialize(std::bitset<8> UGG_feature) {
    GmapServer::UGG_feature_ = UGG_feature;
    log::info("GmapServer initialized, role=%d, UGG_feature=%d", GmapServer::role_, UGG_feature);

    BTA_GATTS_AppRegister(
            bluetooth::le_audio::uuid::kGamingAudioServiceUuid,
            [](tBTA_GATTS_EVT event, tBTA_GATTS *p_data) {
                if (p_data) {
                    GattsCallback(event, p_data);
                }
            },
            false);
}

std::bitset<8> GmapServer::GetRole() {
    return GmapServer::role_;
}

uint16_t GmapServer::GetRoleHandle() {
    for (auto &[attribute_handle, characteristic]: GmapServer::characteristics_) {
        if (characteristic.uuid_ == bluetooth::le_audio::uuid::kRoleCharacteristicUuid) {
            return attribute_handle;
        }
    }
    log::warn("no valid UGG feature handle");
    return 0;
}

std::bitset<8> GmapServer::GetUGGFeature() {
    return GmapServer::UGG_feature_;
}

uint16_t GmapServer::GetUGGFeatureHandle() {
    for (auto &[attribute_handle, characteristic]: GmapServer::characteristics_) {
        if (characteristic.uuid_ ==
            bluetooth::le_audio::uuid::kUnicastGameTerminalCharacteristicUuid) {
            return attribute_handle;
        }
    }
    log::warn("no valid UGG feature handle");
    return 0;
}

std::unordered_map <uint16_t, GmapCharacteristic> &GmapServer::GetCharacteristics() {
    return GmapServer::characteristics_;
}

uint16_t GmapServer::GetServerIf() {
    return GmapServer::server_if_;
}

void GmapServer::SetServerIf(uint16_t server_if) {
    GmapServer::server_if_ = server_if;
}

void GattsCallback(tBTA_GATTS_EVT event, tBTA_GATTS *p_data) {
    log::info("event: {}", gatt_server_event_text(event));
    switch (event) {
        case BTA_GATTS_CONNECT_EVT: {
            OnGattConnect(p_data);
        }
            break;
        case BTA_GATTS_DISCONNECT_EVT: {
            OnGattDisconnect(p_data);
        }
            break;
        case BTA_GATTS_REG_EVT: {
            OnGattServerRegister(p_data);
        }
            break;
        case BTA_GATTS_READ_CHARACTERISTIC_EVT: {
            OnReadCharacteristic(p_data);
        }
            break;
        default:
            log::warn("Unhandled event {}", gatt_server_event_text(event));
    }
}

void OnGattConnect(tBTA_GATTS *p_data) {
    if (p_data == nullptr) {
        log::warn("invalid p_data");
    }
    auto address = p_data->conn.remote_bda;
    log::info("Address: {}, conn_id:{}", address, p_data->conn.conn_id);
    if (p_data->conn.transport == BT_TRANSPORT_BR_EDR) {
        log::warn("Skip BE/EDR connection");
        return;
    }
}

void OnGattDisconnect(tBTA_GATTS *p_data) {
    if (p_data == nullptr) {
        log::warn("invalid p_data");
    }
    auto address = p_data->conn.remote_bda;
    log::info("Address: {}, conn_id:{}", address, p_data->conn.conn_id);
}

void OnGattServerRegister(tBTA_GATTS *p_data) {
    if (p_data == nullptr) {
        log::warn("invalid p_data");
    }
    tGATT_STATUS status = p_data->reg_oper.status;
    log::info("status: {}", gatt_status_text(p_data->reg_oper.status));

    if (status != tGATT_STATUS::GATT_SUCCESS) {
        log::warn("Register Server fail");
        return;
    }
    GmapServer::SetServerIf(p_data->reg_oper.server_if);

    std::vector <btgatt_db_element_t> service;

    // GMAP service
    btgatt_db_element_t gmap_service;
    gmap_service.uuid = bluetooth::le_audio::uuid::kGamingAudioServiceUuid;
    gmap_service.type = BTGATT_DB_PRIMARY_SERVICE;
    service.push_back(gmap_service);

    // GMAP role
    btgatt_db_element_t role_characteristic;
    role_characteristic.uuid = bluetooth::le_audio::uuid::kRoleCharacteristicUuid;
    role_characteristic.type = BTGATT_DB_CHARACTERISTIC;
    role_characteristic.properties = GATT_CHAR_PROP_BIT_READ;
    role_characteristic.permissions = GATT_PERM_READ;
    service.push_back(role_characteristic);

    // GMAP UGG feature
    btgatt_db_element_t UGG_feature_characteristic;
    UGG_feature_characteristic.uuid = bluetooth::le_audio::uuid::kUnicastGameTerminalCharacteristicUuid;
    UGG_feature_characteristic.type = BTGATT_DB_CHARACTERISTIC;
    UGG_feature_characteristic.properties = GATT_CHAR_PROP_BIT_READ;
    UGG_feature_characteristic.permissions = GATT_PERM_READ;
    service.push_back(UGG_feature_characteristic);

    BTA_GATTS_AddService(GmapServer::GetServerIf(), service,
                         base::BindRepeating([](tGATT_STATUS status, int server_if,
                                                std::vector <btgatt_db_element_t> service) {
                             OnServiceAdded(status, server_if, service);
                         }));
}

void OnServiceAdded(tGATT_STATUS status, int server_if,
                    std::vector <btgatt_db_element_t> service) {
    log::info("status: {}, server_if: {}", gatt_status_text(status), server_if);
    for (uint16_t i = 0; i < service.size(); i++) {
        uint16_t attribute_handle = service[i].attribute_handle;
        Uuid uuid = service[i].uuid;
        if (service[i].type == BTGATT_DB_CHARACTERISTIC) {
            log::info("Characteristic uuid: 0x{:04x}, handle:0x{:04x}", uuid.As16Bit(),
                      attribute_handle);
            GmapServer::GetCharacteristics()[attribute_handle].attribute_handle_ = attribute_handle;
            GmapServer::GetCharacteristics()[attribute_handle].uuid_ = uuid;
        }
    }
}

void OnReadCharacteristic(tBTA_GATTS *p_data) {
    uint16_t read_req_handle = p_data->req_data.p_data->read_req.handle;
    log::info("read_req_handle: 0x{:04x},", read_req_handle);

    tGATTS_RSP p_msg;
    p_msg.attr_value.handle = read_req_handle;
    if (GmapServer::GetCharacteristics().find(read_req_handle) ==
        GmapServer::GetCharacteristics().end()) {
        log::error("Invalid handle 0x{:04x}", read_req_handle);
        BTA_GATTS_SendRsp(p_data->req_data.conn_id, p_data->req_data.trans_id, GATT_INVALID_HANDLE,
                          &p_msg);
        return;
    }

    auto uuid = GmapServer::GetCharacteristics()[read_req_handle].uuid_;

    log::info("Read uuid, 0x{:04x}", uuid.As16Bit());
    // Check Characteristic UUID
    if (bluetooth::le_audio::uuid::kRoleCharacteristicUuid == uuid) {
        p_msg.attr_value.len = GmapServer::kGmapRoleLen;
        auto role = GmapServer::GetRole();
        memcpy(p_msg.attr_value.value, &role, sizeof(uint8_t));
    } else if (bluetooth::le_audio::uuid::kUnicastGameTerminalCharacteristicUuid == uuid) {
        p_msg.attr_value.len = GmapServer::kGmapUGGFeatureLen;
        auto UGGFeature = GmapServer::GetUGGFeature();
        memcpy(p_msg.attr_value.value, &UGGFeature, sizeof(uint8_t));
    } else {
        log::warn("Unhandled uuid {}", uuid.ToString());
        BTA_GATTS_SendRsp(p_data->req_data.conn_id, p_data->req_data.trans_id,
                          GATT_ILLEGAL_PARAMETER, &p_msg);
        return;
    }

    BTA_GATTS_SendRsp(p_data->req_data.conn_id, p_data->req_data.trans_id, GATT_SUCCESS, &p_msg);
}
