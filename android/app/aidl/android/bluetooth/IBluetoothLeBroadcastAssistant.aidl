/*
 * Copyright 2022 The Android Open Source Project
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
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.IBluetoothLeBroadcastAssistantCallback;
import android.bluetooth.le.ScanFilter;

/**
 * APIs for Bluetooth LE Audio Broadcast Assistant service
 *
 * @hide
 */
interface IBluetoothLeBroadcastAssistant {
    // Public API
    int getConnectionState(in BluetoothDevice sink);
    List<BluetoothDevice> getDevicesMatchingConnectionStates(in int[] states);
    List<BluetoothDevice> getConnectedDevices();
    boolean setConnectionPolicy(in BluetoothDevice device, int connectionPolicy);
    int getConnectionPolicy(in BluetoothDevice device);
    void registerCallback(in IBluetoothLeBroadcastAssistantCallback cb);
    void unregisterCallback(in IBluetoothLeBroadcastAssistantCallback cb);
    void startSearchingForSources(in List<ScanFilter> filters);
    void stopSearchingForSources();
    boolean isSearchInProgress();
    void addSource(in BluetoothDevice sink, in BluetoothLeBroadcastMetadata sourceMetadata, in boolean isGroupOp);
    void modifySource(in BluetoothDevice sink, in int sourceId, in BluetoothLeBroadcastMetadata updatedMetadata);
    void removeSource(in BluetoothDevice sink, in int sourceId);
    List<BluetoothLeBroadcastReceiveState> getAllSources(in BluetoothDevice sink);
    int getMaximumSourceCapacity(in BluetoothDevice sink);
}
