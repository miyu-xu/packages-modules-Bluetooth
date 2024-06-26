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

import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothCodecType;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BufferConstraints;
import android.content.AttributionSource;

/**
 * APIs for Bluetooth A2DP service
 *
 * @hide
 */
interface IBluetoothA2dp {
    boolean connect(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean disconnect(in BluetoothDevice device, in AttributionSource attributionSource);
    List<BluetoothDevice> getConnectedDevices(in AttributionSource attributionSource);
    List<BluetoothDevice> getDevicesMatchingConnectionStates(in int[] states, in AttributionSource attributionSource);
    int getConnectionState(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean setActiveDevice(in BluetoothDevice device, in AttributionSource attributionSource);
    BluetoothDevice getActiveDevice(in AttributionSource attributionSource);
    boolean setConnectionPolicy(in BluetoothDevice device, int connectionPolicy, in AttributionSource attributionSource);
    int getConnectionPolicy(in BluetoothDevice device, in AttributionSource attributionSource);
    oneway void setAvrcpAbsoluteVolume(int volume, in AttributionSource attributionSource);
    boolean isA2dpPlaying(in BluetoothDevice device, in AttributionSource attributionSource);
    List<BluetoothCodecType> getSupportedCodecTypes(in AttributionSource attributionSource);
    BluetoothCodecStatus getCodecStatus(in BluetoothDevice device, in AttributionSource attributionSource);
    oneway void setCodecConfigPreference(in BluetoothDevice device, in BluetoothCodecConfig codecConfig, in AttributionSource attributionSource);
    oneway void enableOptionalCodecs(in BluetoothDevice device, in AttributionSource attributionSource);
    oneway void disableOptionalCodecs(in BluetoothDevice device, in AttributionSource attributionSource);
    int isOptionalCodecsSupported(in BluetoothDevice device, in AttributionSource attributionSource);
    int isOptionalCodecsEnabled(in BluetoothDevice device, in AttributionSource attributionSource);
    oneway void setOptionalCodecsEnabled(in BluetoothDevice device, int value, in AttributionSource attributionSource);
    int getDynamicBufferSupport(in AttributionSource attributionSource);
    BufferConstraints getBufferConstraints(in AttributionSource attributionSource);
    boolean setBufferLengthMillis(int codec, int size, in AttributionSource attributionSource);
}
