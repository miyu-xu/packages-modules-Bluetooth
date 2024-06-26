/*
 * Copyright 2008 The Android Open Source Project
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
import android.content.AttributionSource;

/**
 * System private API for Bluetooth MAP service
 *
 * {@hide}
 */
interface IBluetoothMap {
    int getState(in AttributionSource attributionSource);
    BluetoothDevice getClient(in AttributionSource attributionSource);
    boolean disconnect(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean isConnected(in BluetoothDevice device, in AttributionSource attributionSource);
    List<BluetoothDevice> getConnectedDevices(in AttributionSource attributionSource);
    List<BluetoothDevice> getDevicesMatchingConnectionStates(in int[] states, in AttributionSource attributionSource);
    int getConnectionState(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean setConnectionPolicy(in BluetoothDevice device, int connectionPolicy, in AttributionSource attributionSource);
    int getConnectionPolicy(in BluetoothDevice device, in AttributionSource attributionSource);
}
