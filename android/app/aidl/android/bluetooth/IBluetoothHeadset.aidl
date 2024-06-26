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
 * API for Bluetooth Headset service
 *
 * Note before adding anything new:
 *   Internal interactions within com.android.bluetooth should be handled through
 *   HeadsetService directly instead of going through binder
 *
 * {@hide}
 */
interface IBluetoothHeadset {
    List<BluetoothDevice> getConnectedDevices(in AttributionSource attributionSource);
    List<BluetoothDevice> getDevicesMatchingConnectionStates(in int[] states, in AttributionSource attributionSource);
    int getConnectionState(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean startVoiceRecognition(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean stopVoiceRecognition(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean isAudioConnected(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean sendVendorSpecificResultCode(in BluetoothDevice device, in String command, in String arg, in AttributionSource attributionSource);

    boolean connect(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean disconnect(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean setConnectionPolicy(in BluetoothDevice device, int connectionPolicy, in AttributionSource attributionSource);
    int getConnectionPolicy(in BluetoothDevice device, in AttributionSource attributionSource);
    int getAudioState(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean isAudioOn(in AttributionSource attributionSource);
    int connectAudio(in AttributionSource attributionSource);
    int disconnectAudio(in AttributionSource attributionSource);
    void setAudioRouteAllowed(boolean allowed, in AttributionSource attributionSource);
    boolean getAudioRouteAllowed(in AttributionSource attributionSource);
    void setForceScoAudio(boolean forced, in AttributionSource attributionSource);
    boolean startScoUsingVirtualVoiceCall(in AttributionSource attributionSource);
    boolean stopScoUsingVirtualVoiceCall(in AttributionSource attributionSource);
    void phoneStateChanged(int numActive, int numHeld, int callState, String number, int type, String name, in AttributionSource attributionSource);
    void clccResponse(int index, int direction, int status, int mode, boolean mpty, String number, int type, in AttributionSource attributionSource);
    boolean setActiveDevice(in BluetoothDevice device, in AttributionSource attributionSource);
    BluetoothDevice getActiveDevice(in AttributionSource attributionSource);
    boolean isInbandRingingEnabled(in AttributionSource attributionSource);

    boolean isNoiseReductionSupported(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean isVoiceRecognitionSupported(in BluetoothDevice device, in AttributionSource attributionSource);
}
