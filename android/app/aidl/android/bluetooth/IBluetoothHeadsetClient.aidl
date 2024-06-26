/*
 * Copyright 2014 The Android Open Source Project
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
import android.bluetooth.BluetoothSinkAudioPolicy;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.content.AttributionSource;
import android.os.Bundle;

/**
 * API for Bluetooth Headset Client service (HFP HF Role)
 *
 * {@hide}
 */
interface IBluetoothHeadsetClient {
    boolean connect(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean disconnect(in BluetoothDevice device, in AttributionSource attributionSource);

    List<BluetoothDevice> getConnectedDevices(in AttributionSource attributionSource);
    List<BluetoothDevice> getDevicesMatchingConnectionStates(in int[] states, in AttributionSource attributionSource);
    int getConnectionState(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean setConnectionPolicy(in BluetoothDevice device, int connectionPolicy, in AttributionSource attributionSource);
    int getConnectionPolicy(in BluetoothDevice device, in AttributionSource attributionSource);

    boolean startVoiceRecognition(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean stopVoiceRecognition(in BluetoothDevice device, in AttributionSource attributionSource);

    List<BluetoothHeadsetClientCall> getCurrentCalls(in BluetoothDevice device, in AttributionSource attributionSource);
    Bundle getCurrentAgEvents(in BluetoothDevice device, in AttributionSource attributionSource);

    boolean acceptCall(in BluetoothDevice device, int flag, in AttributionSource attributionSource);
    boolean holdCall(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean rejectCall(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean terminateCall(in BluetoothDevice device, in BluetoothHeadsetClientCall call, in AttributionSource attributionSource);

    boolean enterPrivateMode(in BluetoothDevice device, int index, in AttributionSource attributionSource);
    boolean explicitCallTransfer(in BluetoothDevice device, in AttributionSource attributionSource);

    BluetoothHeadsetClientCall dial(in BluetoothDevice device, String number, in AttributionSource attributionSource);

    boolean sendDTMF(in BluetoothDevice device, byte code, in AttributionSource attributionSource);
    boolean getLastVoiceTagNumber(in BluetoothDevice device, in AttributionSource attributionSource);

    int getAudioState(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean connectAudio(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean disconnectAudio(in BluetoothDevice device, in AttributionSource attributionSource);
    void setAudioRouteAllowed(in BluetoothDevice device, boolean allowed, in AttributionSource attributionSource);
    boolean getAudioRouteAllowed(in BluetoothDevice device, in AttributionSource attributionSource);
    boolean sendVendorAtCommand(in BluetoothDevice device, int vendorId, String atCommand, in AttributionSource attributionSource);

    Bundle getCurrentAgFeatures(in BluetoothDevice device, in AttributionSource attributionSource);
}
