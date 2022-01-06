/*
 * Copyright 2016 The Android Open Source Project
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

package android.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastSourceInfo;
//import android.bluetooth.BleBroadcastSourceChannel;
import android.bluetooth.le.ScanResult;

/** @hide */
interface IBluetoothLeBroadcastAssistantCallback {
    void onBluetoothLeBroadcastSourceFound(in ScanResult result);

    void onBluetoothLeBroadcastSourceSelected(
            in BluetoothLeBroadcastSourceInfo source,
            in int status);

    void onBluetoothLeBroadcastSourceLost(
            in BluetoothLeBroadcastSourceInfo source,
            in int status);

    void onBluetoothLeBroadcastSourceAdded(
            in BluetoothDevice sink,
            in BluetoothLeBroadcastSourceInfo source,
            in int status);

    void onBluetoothLeBroadcastSourceUpdated(
            in BluetoothDevice sink,
            in BluetoothLeBroadcastSourceInfo source,
            in int status);
/*
    void onBleBroadcastPinUpdated(in BluetoothDevice rcvr,
                                  in byte srcId,
                                  in int status);
*/
    void onBluetoothLeBroadcastSourceRemoved(
            in BluetoothDevice sink,
            in BluetoothLeBroadcastSourceInfo source,
            in int status);
}
