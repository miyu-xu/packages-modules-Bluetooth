/*
 * Copyright 2020 HIMSA II K/S - www.himsa.com.
 * Represented by EHIMA - www.ehima.com
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
 * APIs for Bluetooth LE Audio service
 *
 * @hide
 */
interface IBluetoothLeAudio {
    // Public API
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean connect(in BluetoothDevice device, in AttributionSource attributionSource);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean disconnect(in BluetoothDevice device, in AttributionSource attributionSource);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    List<BluetoothDevice> getConnectedDevices(in AttributionSource attributionSource);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    List<BluetoothDevice> getDevicesMatchingConnectionStates(in int[] states, in AttributionSource attributionSource);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    int getConnectionState(in BluetoothDevice device, in AttributionSource attributionSource);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean setActiveDevice(in BluetoothDevice device, in AttributionSource attributionSource);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    List<BluetoothDevice> getActiveDevices(in AttributionSource attributionSource);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})")
    boolean setConnectionPolicy(in BluetoothDevice device, int connectionPolicy, in AttributionSource attributionSource);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    int getConnectionPolicy(in BluetoothDevice device, in AttributionSource attributionSource);

    /* Same value as bluetooth::groups::kGroupUnknown */
    const int LE_AUDIO_GROUP_ID_INVALID = -1;

    const int GROUP_STATUS_INACTIVE = 0;
    const int GROUP_STATUS_ACTIVE = 1;

    const int GROUP_NODE_ADDED = 1;
    const int GROUP_NODE_REMOVED = 2;

    /**
     * Get device group id. Devices with same group id belong to same group (i.e left and right
     * earbud)
     */
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    int getGroupId(in BluetoothDevice device, in AttributionSource attributionSource);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})")
    void setVolume(int volume, in AttributionSource attributionSource);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})")
    boolean groupAddNode(int group_id, in BluetoothDevice device, in AttributionSource attributionSource);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(allOf={android.Manifest.permission.BLUETOOTH_CONNECT,android.Manifest.permission.BLUETOOTH_PRIVILEGED})")
    boolean groupRemoveNode(int group_id, in BluetoothDevice device, in AttributionSource attributionSource);

    // Broadcaster API
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    void createBroadcast(in byte[] metadata, int audio_profile, in byte[] broadcast_code, in AttributionSource attributionSource);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    void updateMetadata(int instance_id, in byte[] metadata, in AttributionSource attributionSource);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    void startBroadcast(int instance_id, in AttributionSource attributionSource);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    void stopBroadcast(int instance_id, in AttributionSource attributionSource);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    void pauseBroadcast(int instance_id, in AttributionSource attributionSource);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    void destroyBroadcast(int instance_id, in AttributionSource attributionSource);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    void getBroadcastId(int instance_id, in AttributionSource attributionSource);
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    void getAllBroadcastStates(in AttributionSource attributionSource);

    const int BROADCAST_INSTANCE_ID_UNDEFINED = 0xFF;

    const int BROADCAST_STATE_STOPPED = 0;
    const int BROADCAST_STATE_CONFIGURED = 1;
    const int BROADCAST_STATE_STREAMING = 2;

    const int BROADCASTER_ADDR_TYPE_PUBLIC = 0x00;
    const int BROADCASTER_ADDR_TYPE_RANDOM = 0x01;

    const int BROADCAST_PROFILE_SONIFICATION = 0x00;
    const int BROADCAST_PROFILE_MEDIA = 0x01;

    const int BIS_SYNC_ANY = 0xFFFFFFFF;
}
