/*
 * Copyright 2021 HIMSA II K/S - www.himsa.com.
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

package com.android.bluetooth.hap;

import static java.util.Objects.requireNonNull;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHapPresetInfo;
import android.util.Log;

import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Arrays;

/** Hearing Access Profile Client Native Interface to/from JNI. */
public class HapClientNativeInterface {
    private static final String TAG = HapClientNativeInterface.class.getSimpleName();

    private final AdapterService mAdapterService;

    public HapClientNativeInterface(AdapterService adapterService) {
        mAdapterService = requireNonNull(adapterService);
    }

    boolean connectHapClient(BluetoothDevice device) {
        return connectHapClientNative(getByteAddress(device));
    }

    boolean disconnectHapClient(BluetoothDevice device) {
        return disconnectHapClientNative(getByteAddress(device));
    }

    private BluetoothDevice getDevice(byte[] address) {
        return mAdapterService.getDeviceFromByte(address);
    }

    private byte[] getByteAddress(BluetoothDevice device) {
        if (device == null) {
            return Utils.getBytesFromAddress("00:00:00:00:00:00");
        }
        return Utils.getBytesFromAddress(device.getAddress());
    }

    void sendMessageToService(HapClientStackEvent event) {
        HapClientService service = HapClientService.getHapClientService();
        if (service != null && service.isAvailable()) {
            service.messageFromNative(event);
        } else {
            Log.e(TAG, "Event ignored, service not available: " + event);
        }
    }

    void init() {
        initNative();
    }

    void cleanup() {
        cleanupNative();
    }

    void selectActivePreset(BluetoothDevice device, int presetIndex) {
        selectActivePresetNative(getByteAddress(device), presetIndex);
    }

    void groupSelectActivePreset(int groupId, int presetIndex) {
        groupSelectActivePresetNative(groupId, presetIndex);
    }

    void nextActivePreset(BluetoothDevice device) {
        nextActivePresetNative(getByteAddress(device));
    }

    void groupNextActivePreset(int groupId) {
        groupNextActivePresetNative(groupId);
    }

    void previousActivePreset(BluetoothDevice device) {
        previousActivePresetNative(getByteAddress(device));
    }

    void groupPreviousActivePreset(int groupId) {
        groupPreviousActivePresetNative(groupId);
    }

    void getPresetInfo(BluetoothDevice device, int presetIndex) {
        getPresetInfoNative(getByteAddress(device), presetIndex);
    }

    void setPresetName(BluetoothDevice device, int presetIndex, String name) {
        setPresetNameNative(getByteAddress(device), presetIndex, name);
    }

    void groupSetPresetName(int groupId, int presetIndex, String name) {
        groupSetPresetNameNative(groupId, presetIndex, name);
    }

    // Callbacks from the native stack back into the Java framework.
    // All callbacks are routed via the Service which will disambiguate which
    // state machine the message should be routed to.

    @VisibleForTesting
    void onConnectionStateChanged(int state, byte[] address) {
        HapClientStackEvent event =
                new HapClientStackEvent(HapClientStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
        event.device = getDevice(address);
        event.valueInt1 = state;

        Log.d(TAG, "onConnectionStateChanged: " + event);
        sendMessageToService(event);
    }

    @VisibleForTesting
    void onDeviceAvailable(byte[] address, int features) {
        HapClientStackEvent event =
                new HapClientStackEvent(HapClientStackEvent.EVENT_TYPE_DEVICE_AVAILABLE);
        event.device = getDevice(address);
        event.valueInt1 = features;

        Log.d(TAG, "onDeviceAvailable: " + event);
        sendMessageToService(event);
    }

    @VisibleForTesting
    void onFeaturesUpdate(byte[] address, int features) {
        HapClientStackEvent event =
                new HapClientStackEvent(HapClientStackEvent.EVENT_TYPE_DEVICE_FEATURES);
        event.device = getDevice(address);
        event.valueInt1 = features;

        Log.d(TAG, "onFeaturesUpdate: " + event);
        sendMessageToService(event);
    }

    @VisibleForTesting
    void onActivePresetSelected(byte[] address, int presetIndex) {
        HapClientStackEvent event =
                new HapClientStackEvent(HapClientStackEvent.EVENT_TYPE_ON_ACTIVE_PRESET_SELECTED);
        event.device = getDevice(address);
        event.valueInt1 = presetIndex;

        Log.d(TAG, "onActivePresetSelected: " + event);
        sendMessageToService(event);
    }

    @VisibleForTesting
    void onActivePresetGroupSelected(int groupId, int presetIndex) {
        HapClientStackEvent event =
                new HapClientStackEvent(HapClientStackEvent.EVENT_TYPE_ON_ACTIVE_PRESET_SELECTED);
        event.valueInt1 = presetIndex;
        event.valueInt2 = groupId;

        Log.d(TAG, "onActivePresetGroupSelected: " + event);
        sendMessageToService(event);
    }

    @VisibleForTesting
    void onActivePresetSelectError(byte[] address, int resultCode) {
        HapClientStackEvent event =
                new HapClientStackEvent(
                        HapClientStackEvent.EVENT_TYPE_ON_ACTIVE_PRESET_SELECT_ERROR);
        event.device = getDevice(address);
        event.valueInt1 = resultCode;

        Log.d(TAG, "onActivePresetSelectError: " + event);
        sendMessageToService(event);
    }

    @VisibleForTesting
    void onActivePresetGroupSelectError(int groupId, int resultCode) {
        HapClientStackEvent event =
                new HapClientStackEvent(
                        HapClientStackEvent.EVENT_TYPE_ON_ACTIVE_PRESET_SELECT_ERROR);
        event.valueInt1 = resultCode;
        event.valueInt2 = groupId;

        Log.d(TAG, "onActivePresetGroupSelectError: " + event);
        sendMessageToService(event);
    }

    @VisibleForTesting
    void onPresetInfo(byte[] address, int infoReason, BluetoothHapPresetInfo[] presets) {
        HapClientStackEvent event =
                new HapClientStackEvent(HapClientStackEvent.EVENT_TYPE_ON_PRESET_INFO);
        event.device = getDevice(address);
        event.valueInt2 = infoReason;
        event.valueList = new ArrayList<>(Arrays.asList(presets));

        Log.d(TAG, "onPresetInfo: " + event);
        sendMessageToService(event);
    }

    @VisibleForTesting
    void onGroupPresetInfo(int groupId, int infoReason, BluetoothHapPresetInfo[] presets) {
        HapClientStackEvent event =
                new HapClientStackEvent(HapClientStackEvent.EVENT_TYPE_ON_PRESET_INFO);
        event.valueInt2 = infoReason;
        event.valueInt3 = groupId;
        event.valueList = new ArrayList<>(Arrays.asList(presets));

        Log.d(TAG, "onGroupPresetInfo: " + event);
        sendMessageToService(event);
    }

    @VisibleForTesting
    void onPresetNameSetError(byte[] address, int presetIndex, int resultCode) {
        HapClientStackEvent event =
                new HapClientStackEvent(HapClientStackEvent.EVENT_TYPE_ON_PRESET_NAME_SET_ERROR);
        event.device = getDevice(address);
        event.valueInt1 = resultCode;
        event.valueInt2 = presetIndex;

        Log.d(TAG, "onPresetNameSetError: " + event);
        sendMessageToService(event);
    }

    @VisibleForTesting
    void onGroupPresetNameSetError(int groupId, int presetIndex, int resultCode) {
        HapClientStackEvent event =
                new HapClientStackEvent(HapClientStackEvent.EVENT_TYPE_ON_PRESET_NAME_SET_ERROR);
        event.valueInt1 = resultCode;
        event.valueInt2 = presetIndex;
        event.valueInt3 = groupId;

        Log.d(TAG, "onGroupPresetNameSetError: " + event);
        sendMessageToService(event);
    }

    @VisibleForTesting
    void onPresetInfoError(byte[] address, int presetIndex, int resultCode) {
        HapClientStackEvent event =
                new HapClientStackEvent(HapClientStackEvent.EVENT_TYPE_ON_PRESET_INFO_ERROR);
        event.device = getDevice(address);
        event.valueInt1 = resultCode;
        event.valueInt2 = presetIndex;

        Log.d(TAG, "onPresetInfoError: " + event);
        sendMessageToService(event);
    }

    @VisibleForTesting
    void onGroupPresetInfoError(int groupId, int presetIndex, int resultCode) {
        HapClientStackEvent event =
                new HapClientStackEvent(HapClientStackEvent.EVENT_TYPE_ON_PRESET_INFO_ERROR);
        event.valueInt1 = resultCode;
        event.valueInt2 = presetIndex;
        event.valueInt3 = groupId;

        Log.d(TAG, "onGroupPresetInfoError: " + event);
        sendMessageToService(event);
    }

    // Native methods that call into the JNI interface
    private native void initNative();

    private native void cleanupNative();

    private native boolean connectHapClientNative(byte[] address);

    private native boolean disconnectHapClientNative(byte[] address);

    private native void selectActivePresetNative(byte[] byteAddress, int presetIndex);

    private native void groupSelectActivePresetNative(int groupId, int presetIndex);

    private native void nextActivePresetNative(byte[] byteAddress);

    private native void groupNextActivePresetNative(int groupId);

    private native void previousActivePresetNative(byte[] byteAddress);

    private native void groupPreviousActivePresetNative(int groupId);

    private native void getPresetInfoNative(byte[] byteAddress, int presetIndex);

    private native void setPresetNameNative(byte[] byteAddress, int presetIndex, String name);

    private native void groupSetPresetNameNative(int groupId, int presetIndex, String name);
}
