/*
 * Copyright 2024 The Android Open Source Project
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
 */

#include "stack/include/security_client_callbacks.h"

namespace {

SecurityClientInterface default_security_client_interface = {
        .BTM_Sec_Init = []() {},
        .BTM_Sec_Free = []() {},
        .BTM_SecRegister = [](const tBTM_APPL_INFO* /* p_cb_info */) -> bool { return false; },
        .BTM_BleLoadLocalKeys = [](uint8_t /* key_type */, tBTM_BLE_LOCAL_KEYS* /* p_key */) {},

        .BTM_SecAddDevice = [](const RawAddress& /* bd_addr */, DEV_CLASS /* dev_class */,
                               LinkKey /* link_key */, uint8_t /* key_type */,
                               uint8_t /* pin_length */) {},
        .BTM_SecAddBleDevice = [](const RawAddress& /* bd_addr */, tBT_DEVICE_TYPE /* dev_type */,
                                  tBLE_ADDR_TYPE /* addr_type */) {},

        .BTM_SecDeleteDevice = [](const RawAddress& /* bd_addr */) -> bool { return false; },
        .BTM_SecAddBleKey = [](const RawAddress& /* bd_addr */, tBTM_LE_KEY_VALUE* /* p_le_key */,
                               tBTM_LE_KEY_TYPE /* key_type */) {},
        .BTM_SecClearSecurityFlags = [](const RawAddress& /* bd_addr */) {},
        .BTM_SetEncryption = [](const RawAddress& /* bd_addr */, tBT_TRANSPORT /* transport */,
                                tBTM_SEC_CALLBACK* /* p_callback */, void* /* p_ref_data */,
                                tBTM_BLE_SEC_ACT /* sec_act */) -> tBTM_STATUS {
          return BTM_SUCCESS;
        },
        .BTM_IsEncrypted = [](const RawAddress& /* bd_addr */,
                              tBT_TRANSPORT /* transport */) -> bool { return false; },
        .BTM_SecIsSecurityPending = [](const RawAddress& /* bd_addr */) -> bool { return false; },
        .BTM_IsLinkKeyKnown = [](const RawAddress& /* bd_addr */,
                                 tBT_TRANSPORT /* transport */) -> bool { return false; },

        .BTM_SetSecurityLevel = [](bool /* is_originator */, const char* /* p_name */,
                                   uint8_t /* service_id */, uint16_t /* sec_level */,
                                   uint16_t /* psm */, uint32_t /* mx_proto_id */,
                                   uint32_t /* mx_chan_id */) -> bool { return false; },
        .BTM_SecClrService = [](uint8_t /* service_id */) -> uint8_t { return 0; },
        .BTM_SecClrServiceByPsm = [](uint16_t /* psm */) -> uint8_t { return 0; },

        .BTM_SecBond = [](const RawAddress& /* bd_addr */, tBLE_ADDR_TYPE /* addr_type */,
                          tBT_TRANSPORT /* transport */,
                          tBT_DEVICE_TYPE /* device_type */) -> tBTM_STATUS { return BTM_SUCCESS; },

        .BTM_SecBondCancel = [](const RawAddress& /* bd_addr */) -> tBTM_STATUS {
          return BTM_SUCCESS;
        },
        .BTM_RemoteOobDataReply = [](tBTM_STATUS /* res */, const RawAddress& /* bd_addr */,
                                     const Octet16& /* c */, const Octet16& /* r */) {},

        .BTM_PINCodeReply = [](const RawAddress& /* bd_addr */, tBTM_STATUS /* res */,
                               uint8_t /* pin_len */, uint8_t* /* p_pin */) {},
        .BTM_SecConfirmReqReply = [](tBTM_STATUS /* res */, tBT_TRANSPORT /* transport */,
                                     const RawAddress /* bd_addr */) {},
        .BTM_BleSirkConfirmDeviceReply = [](const RawAddress& /* bd_addr */, uint8_t /* res */) {},
        .BTM_BlePasskeyReply = [](const RawAddress& /* bd_addr */, uint8_t /* res */,
                                  uint32_t /* passkey */) {},

        .BTM_GetSecurityMode = []() -> uint8_t { return 0; },

        .BTM_SecReadDevName = [](const RawAddress& /* bd_addr */) -> const char* {
          return nullptr;
        },
        .BTM_SecAddRmtNameNotifyCallback = [](tBTM_RMT_NAME_CALLBACK* /* p_callback */) -> bool {
          return false;
        },
        .BTM_SecDeleteRmtNameNotifyCallback = [](tBTM_RMT_NAME_CALLBACK* /* p_callback */) -> bool {
          return false;
        },
};

// Initialize the working btm client interface to the default
SecurityClientInterface mock_security_client_interface = default_security_client_interface;

} // namespace

// Initialize the working btm client interface to the default
// Reset the working btm client interface to the default
void reset_mock_security_client_interface() {
  mock_security_client_interface = default_security_client_interface;
}

// Serve the working mock security interface
const SecurityClientInterface& get_security_client_interface() {
  return mock_security_client_interface;
}
