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
package com.android.server.bluetooth;

/** {@hide} */
@JavaDerive(toString=true)
@Backing(type="int")
enum BluetoothServiceMessages {
    REGISTER_ADAPTER = 1,
    UNREGISTER_ADAPTER = 2,
    ENABLE = 3,
    DISABLE = 4,
    FACTORY_RESET = 5,
    IS_BLE_SCAN_AVAILABLE = 6,
    IS_HEARING_AID_SUPPORTED = 7,
    SET_SNOOP_LOG = 8,
    GET_SNOOP_LOG = 9,
    IS_AUTO_ON_SUPPORTED = 10,
    SET_AUTO_ON_ENABLED = 11,
    GET_AUTO_ON_ENABLED = 12,
}
