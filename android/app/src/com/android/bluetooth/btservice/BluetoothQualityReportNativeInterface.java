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

package com.android.bluetooth.btservice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothQualityReport;
import android.content.Intent;
import android.util.Log;

import com.android.bluetooth.Utils;
import com.android.internal.annotations.GuardedBy;

final class BluetoothQualityReportNativeInterface {

    private static final String TAG = "BluetoothQualityReportNativeInterface";

    @GuardedBy("INSTANCE_LOCK")
    private static BluetoothQualityReportNativeInterface sInstance;

    private static final Object INSTANCE_LOCK = new Object();

    static {
        classInitNative();
    }

    private BluetoothQualityReportNativeInterface() {}

    /** Get singleton instance. */
    public static BluetoothQualityReportNativeInterface getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (sInstance == null) {
                sInstance = new BluetoothQualityReportNativeInterface();
            }
            return sInstance;
        }
    }

    /**
     * Initializes the native interface.
     *
     * <p>priorities to configure.
     */
    public void init() {
        initNative();
    }

    /** Cleanup the native interface. */
    public void cleanup() {
        cleanupNative();
    }

    // Callbacks from the native stack back into the Java framework.
    // All callbacks are routed via the Service which will disambiguate which
    // state machine the message should be routed to.

    private void bqrDeliver(
            byte[] remoteAddr, int lmpVer, int lmpSubVer, int manufacturerId, byte[] bqrRawData) {
        String remoteName = "";
        int remoteCoD = 0;
        String addr = Utils.getAddressStringFromByte(remoteAddr);
        if (addr != null) {
            BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(addr);
            remoteName = device.getName();
            remoteCoD = device.getBluetoothClass().getClassOfDevice();
        }

        BluetoothQualityReport bqr;
        try {
            bqr = new BluetoothQualityReport(
                    addr,
                    lmpVer,
                    lmpSubVer,
                    manufacturerId,
                    remoteName,
                    remoteCoD,
                    bqrRawData);
            Log.i(TAG, bqr.toString());
        } catch (Exception e) {
            Log.e(TAG, "bqrDeliver: failed to create bqr", e);
            return;
        }
        Intent intent = new Intent(BluetoothDevice.ACTION_REMOTE_ISSUE_OCCURRED);
        intent.putExtra(BluetoothDevice.EXTRA_BQR, bqr);
        AdapterService.getAdapterService().sendBroadcast(intent, AdapterService.BLUETOOTH_PERM);
    }

    // Native methods that call into the JNI interface
    private static native void classInitNative();

    private native void initNative();

    private native void cleanupNative();
}
