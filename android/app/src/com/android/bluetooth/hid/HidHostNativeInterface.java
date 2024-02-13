/*
 * Copyright 2023 The Android Open Source Project
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

package com.android.bluetooth.hid;

import android.bluetooth.BluetoothProfile;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

/** Provides Bluetooth Hid Host profile, as a service in the Bluetooth application. */
public class HidHostNativeInterface {
    private static final String TAG = HidHostNativeInterface.class.getSimpleName();
    private static final boolean DBG = Log.isLoggable(TAG, Log.DEBUG);

    private HidHostService mHidHostService;

    @GuardedBy("INSTANCE_LOCK")
    private static HidHostNativeInterface sInstance;

    private static final Object INSTANCE_LOCK = new Object();

    static HidHostNativeInterface getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (sInstance == null) {
                sInstance = new HidHostNativeInterface();
            }
            return sInstance;
        }
    }

    /** Set singleton instance. */
    @VisibleForTesting
    public static void setInstance(HidHostNativeInterface instance) {
        synchronized (INSTANCE_LOCK) {
            sInstance = instance;
        }
    }

    void init(HidHostService service) {
        mHidHostService = service;
        initializeNative();
    }

    void cleanup() {
        cleanupNative();
    }

    boolean connectHid(byte[] address, int addressType, int transportType) {
        return connectHidNative(address, addressType, transportType);
    }

    boolean disconnectHid(byte[] address, int addressType, int transportType) {
        return disconnectHidNative(address, addressType, transportType);
    }

    boolean getProtocolMode(byte[] address, int addressType, int transportType) {
        return getProtocolModeNative(address, addressType, transportType);
    }

    boolean virtualUnPlug(byte[] address, int addressType, int transportType) {
        return virtualUnPlugNative(address, addressType, transportType);
    }

    boolean setProtocolMode(
            byte[] address, int addressType, int transportType, byte protocolMode) {
        return setProtocolModeNative(address, addressType, transportType, protocolMode);
    }

    boolean getReport(
            byte[] address,
            int addressType,
            int transportType,
            byte reportType,
            byte reportId,
            int bufferSize) {
        return getReportNative(
                address, addressType, transportType, reportType, reportId, bufferSize);
    }

    boolean setReport(
            byte[] address, int addressType, int transportType, byte reportType, String report) {
        return setReportNative(address, addressType, transportType, reportType, report);
    }

    boolean sendData(byte[] address, int addressType, int transportType, String report) {
        return sendDataNative(address, addressType, transportType, report);
    }

    boolean setIdleTime(byte[] address, int addressType, int transportType, byte idleTime) {
        return setIdleTimeNative(address, addressType, transportType, idleTime);
    }

    boolean getIdleTime(byte[] address, int addressType, int transportType) {
        return getIdleTimeNative(address, addressType, transportType);
    }

    private static int convertHalState(int halState) {
        switch (halState) {
            case CONN_STATE_CONNECTED:
                return BluetoothProfile.STATE_CONNECTED;
            case CONN_STATE_CONNECTING:
                return BluetoothProfile.STATE_CONNECTING;
            case CONN_STATE_DISCONNECTED:
                return BluetoothProfile.STATE_DISCONNECTED;
            case CONN_STATE_DISCONNECTING:
                return BluetoothProfile.STATE_DISCONNECTING;
            default:
                Log.e(TAG, "bad hid connection state: " + halState);
                return BluetoothProfile.STATE_DISCONNECTED;
        }
    }

    /**********************************************************************************************/
    /*********************************** callbacks from native ************************************/
    /**********************************************************************************************/

    private void onConnectStateChanged(
            byte[] address, int addressType, int transportType, int state) {
        if (DBG) Log.d(TAG, "onConnectStateChanged: state=" + state);
        mHidHostService.onConnectStateChanged(
                address, addressType, transportType, convertHalState(state));
    }

    private void onGetProtocolMode(byte[] address, int addressType, int transportType, int mode) {
        if (DBG) Log.d(TAG, "onGetProtocolMode()");
        mHidHostService.onGetProtocolMode(address, addressType, transportType, mode);
    }

    private void onGetReport(
            byte[] address, int addressType, int transportType, byte[] report, int rptSize) {
        if (DBG) Log.d(TAG, "onGetReport()");
        mHidHostService.onGetReport(address, addressType, transportType, report, rptSize);
    }

    private void onHandshake(byte[] address, int addressType, int transportType, int status) {
        if (DBG) Log.d(TAG, "onHandshake: status=" + status);
        mHidHostService.onHandshake(address, addressType, transportType, status);
    }

    private void onVirtualUnplug(byte[] address, int addressType, int transportType, int status) {
        if (DBG) Log.d(TAG, "onVirtualUnplug: status=" + status);
        mHidHostService.onVirtualUnplug(address, addressType, transportType, status);
    }

    private void onGetIdleTime(byte[] address, int addressType, int transportType, int idleTime) {
        if (DBG) Log.d(TAG, "onGetIdleTime()");
        mHidHostService.onGetIdleTime(address, addressType, transportType, idleTime);
    }

    /**********************************************************************************************/
    /******************************************* native *******************************************/
    /**********************************************************************************************/

    // Constants matching Hal header file bt_hh.h
    // bthh_connection_state_t
    private static final int CONN_STATE_CONNECTED = 0;

    private static final int CONN_STATE_CONNECTING = 1;
    private static final int CONN_STATE_DISCONNECTED = 2;
    private static final int CONN_STATE_DISCONNECTING = 3;

    private native void initializeNative();

    private native void cleanupNative();

    private native boolean connectHidNative(byte[] btAddress, int addressType, int transportType);

    private native boolean disconnectHidNative(
            byte[] btAddress, int addressType, int transportType);

    private native boolean getProtocolModeNative(
            byte[] btAddress, int addressType, int transportType);

    private native boolean virtualUnPlugNative(
            byte[] btAddress, int addressType, int transportType);

    private native boolean setProtocolModeNative(
            byte[] btAddress, int addressType, int transportType, byte protocolMode);

    private native boolean getReportNative(
            byte[] btAddress,
            int addressType,
            int transportType,
            byte reportType,
            byte reportId,
            int bufferSize);

    private native boolean setReportNative(
            byte[] btAddress, int addressType, int transportType, byte reportType, String report);

    private native boolean sendDataNative(
            byte[] btAddress, int addressType, int transportType, String report);

    private native boolean setIdleTimeNative(
            byte[] btAddress, int addressType, int transportType, byte idleTime);

    private native boolean getIdleTimeNative(
            byte[] btAddress, int addressType, int transportType);
}
