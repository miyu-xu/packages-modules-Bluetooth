/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.bluetooth.le_scan;

import android.annotation.Nullable;
import android.bluetooth.BluetoothDevice;

record AdvtFilterOnFoundOnLostInfo(
        int clientIf,
        int advPacketLen,
        @Nullable byte[] advPacket,
        int scanRspLen,
        @Nullable byte[] scanRsp,
        int filtIndex,
        int advState,
        int advInfoPresent,
        String address,
        @BluetoothDevice.AddressType int addressType,
        int txPower,
        int rssiValue,
        int timeStamp) {

    public byte[] getResult() {
        int resultLength = advPacket.length + ((scanRsp != null) ? scanRsp.length : 0);
        byte[] result = new byte[resultLength];
        System.arraycopy(advPacket, 0, result, 0, advPacket.length);
        if (scanRsp != null) {
            System.arraycopy(scanRsp, 0, result, advPacket.length, scanRsp.length);
        }
        return result;
    }
}
