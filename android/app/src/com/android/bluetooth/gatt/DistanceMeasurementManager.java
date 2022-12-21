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

package com.android.bluetooth.gatt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.DistanceMeasurementParams;
import android.bluetooth.le.DistanceMeasurementResult;
import android.bluetooth.le.DistanceMeasurementSession;
import android.bluetooth.le.IDistanceMeasurementCallback;
import android.os.HandlerThread;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import com.android.bluetooth.btservice.AdapterService;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages distnace measurement operations and interacts with Gabeldorsche stack.
 *
 * @hide
 */
class DistanceMeasurementManager {
    private static final boolean DBG = GattServiceConfig.DBG;
    private static final String TAG = "DistanceMeasurementManager";

    private static final int RSSI_FREQUENCY_LOW = 3000;
    private static final int RSSI_FREQUENCY_MEDIUM = 1000;
    private static final int RSSI_FREQUENCY_HIGH = 500;

    private final AdapterService mAdapterService;
    private HandlerThread mHandlerThread;

    private ConcurrentHashMap<String, HashSet<DistanceMeasurementTracker>> mRssiTrackers =
            new ConcurrentHashMap<>();

    /**
     * Constructor of {@link DistanceMeasurementManager}.
     */
    DistanceMeasurementManager(AdapterService adapterService) {
        mAdapterService = adapterService;
    }

    /**
     * Start a {@link HandlerThread} that handles distnace measurement operations.
     */
    void start() {
        mHandlerThread = new HandlerThread("DistanceMeasurementManager");
        mHandlerThread.start();
        initializeNative();
    }

    void cleanup() {
        cleanupNative();
    }


    void startDistanceMeasurement(ParcelUuid uuid, DistanceMeasurementParams params,
            IDistanceMeasurementCallback callback) {
        Log.d(TAG, "startDistanceMeasurement device:" + params.getDevice().getAnonymizedAddress()
                + ", method: " + params.getMethod());
        String identityAddress = mAdapterService.getIdentityAddress(
                params.getDevice().getAddress());
        logd("Get identityAddress: " + params.getDevice().getAddress() + " => " + identityAddress);

        int frequencyValue = getFrequencyValue(params.getFrequency(), params.getMethod());
        if (frequencyValue == -1) {
            invokeStartFail(callback, params.getDevice(),
                    DistanceMeasurementSession.Callback.REASON_INVALID_PARAMETERS);
            return;
        }

        DistanceMeasurementTracker tracker = new DistanceMeasurementTracker(
                this, params, identityAddress, uuid, frequencyValue, callback);

        switch (params.getMethod()) {
            case DistanceMeasurementParams.DISTANCE_MEASUREMENT_METHOD_AUTO:
            case DistanceMeasurementParams.DISTANCE_MEASUREMENT_METHOD_RSSI:
                startRssiTracker(tracker, params);
                break;
            default:
                invokeStartFail(callback, params.getDevice(),
                        DistanceMeasurementSession.Callback.REASON_INVALID_PARAMETERS);
        }
    }

    private synchronized void startRssiTracker(DistanceMeasurementTracker tracker,
            DistanceMeasurementParams params) {
        mRssiTrackers.putIfAbsent(tracker.mIdentityAddress,
                new HashSet<DistanceMeasurementTracker>());
        HashSet<DistanceMeasurementTracker> set = mRssiTrackers.get(tracker.mIdentityAddress);
        if (set.contains(tracker)) {
            Log.w(TAG, "Already registered");
            return;
        }
        set.add(tracker);
        startDistanceMeasurementNative(tracker.mIdentityAddress, tracker.mFrequency,
                DistanceMeasurementParams.DISTANCE_MEASUREMENT_METHOD_RSSI);
    }

    void stopDistanceMeasurement(ParcelUuid uuid, BluetoothDevice device, int method,
            boolean timeout) {
        Log.d(TAG, "stopDistanceMeasurement device:" + device.getAnonymizedAddress()
                + ", method: " + method + " timeout " + timeout);
        String identityAddress = mAdapterService.getIdentityAddress(device.getAddress());
        logd("Get identityAddress: " + device.getAddress() + " => " + identityAddress);

        switch (method) {
            case DistanceMeasurementParams.DISTANCE_MEASUREMENT_METHOD_AUTO:
            case DistanceMeasurementParams.DISTANCE_MEASUREMENT_METHOD_RSSI:
                stopRssiTracker(uuid, identityAddress, timeout);
                break;
            default:
                Log.w(TAG, "stopDistanceMeasurement with invalid method:" + method);
        }
    }

    private synchronized void stopRssiTracker(ParcelUuid uuid, String identityAddress,
            boolean timeout) {
        HashSet<DistanceMeasurementTracker> set = mRssiTrackers.get(identityAddress);
        if (set == null) {
            Log.w(TAG, "Can't find rssi tracker");
            return;
        }

        for (DistanceMeasurementTracker tracker : set) {
            if (tracker.equals(uuid, identityAddress)) {
                int reason = timeout ? DistanceMeasurementSession.Callback.REASON_DURATION_TIMEOUT :
                        DistanceMeasurementSession.Callback.REASON_LOCAL_REQUEST;
                invokeOnStopped(tracker.mCallback, tracker.mDevice, reason);
                tracker.cancelTimer();
                set.remove(tracker);
            }
        }

        if (set.isEmpty()) {
            logd("no rssi tracker");
            mRssiTrackers.remove(identityAddress);
            stopDistanceMeasurementNative(identityAddress,
                    DistanceMeasurementParams.DISTANCE_MEASUREMENT_METHOD_RSSI);
        }
    }

    private void invokeStartFail(IDistanceMeasurementCallback callback, BluetoothDevice device,
            int reason) {
        try {
            callback.onStartFail(device, reason);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception: " + e);
        }
    }

    private void invokeOnStopped(IDistanceMeasurementCallback callback, BluetoothDevice device,
            int reason) {
        try {
            callback.onStopped(device, reason);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception: " + e);
        }
    }

    /**
     * Convert frequency into value in ms
     */
    private int getFrequencyValue(int frequency, int method) {
        switch (method) {
            case DistanceMeasurementParams.DISTANCE_MEASUREMENT_METHOD_AUTO:
            case DistanceMeasurementParams.DISTANCE_MEASUREMENT_METHOD_RSSI:
                switch (frequency) {
                    case DistanceMeasurementParams.REPORT_FREQUENCY_LOW:
                        return RSSI_FREQUENCY_LOW;
                    case DistanceMeasurementParams.REPORT_FREQUENCY_MEDIUM:
                        return RSSI_FREQUENCY_MEDIUM;
                    case DistanceMeasurementParams.REPORT_FREQUENCY_HIGH:
                        return RSSI_FREQUENCY_HIGH;
                }
                break;
            default:

        }
        Log.w(TAG, "getFrequencyValue fail frequency:" + frequency + ", method:" + method);
        return -1;
    }

    void onDistanceMeasurementStarted(String address, int method) {
        logd("onDistanceMeasurementStarted address:" + address + ", method:" + method);
        switch (method) {
            case DistanceMeasurementParams.DISTANCE_MEASUREMENT_METHOD_RSSI:
                HashSet<DistanceMeasurementTracker> set = mRssiTrackers.get(address);
                for (DistanceMeasurementTracker tracker : set) {
                    try {
                        if (!tracker.mStarted) {
                            tracker.mStarted = true;
                            tracker.mCallback.onStarted(tracker.mDevice);
                            tracker.startTimer(mHandlerThread.getLooper());
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "Exception: " + e);
                    }
                }
                break;
            default:
                Log.d(TAG, "onDistanceMeasurementResult: invalid method " + method);
        }
    }

    void onDistanceMeasurementStartFail(String address, int reason, int method) {
        logd("onDistanceMeasurementStartFail address:" + address + ", method:" + method);
        switch (method) {
            case DistanceMeasurementParams.DISTANCE_MEASUREMENT_METHOD_RSSI:
                HashSet<DistanceMeasurementTracker> set = mRssiTrackers.get(address);
                for (DistanceMeasurementTracker tracker : set) {
                    if (!tracker.mStarted) {
                        invokeStartFail(tracker.mCallback, tracker.mDevice, reason);
                    }
                }
                synchronized (set) {
                    set.removeIf(tracker -> !tracker.mStarted);
                }
                break;
            default:
                Log.w(TAG, "onDistanceMeasurementStartFail: invalid method " + method);
        }
    }

    void onDistanceMeasurementStopped(String address, int reason, int method) {
        logd("onDistanceMeasurementStopped address:" + address + ", reason:" + reason + ", method:"
                + method);
        switch (method) {
            case DistanceMeasurementParams.DISTANCE_MEASUREMENT_METHOD_RSSI:
                HashSet<DistanceMeasurementTracker> set = mRssiTrackers.get(address);
                for (DistanceMeasurementTracker tracker : set) {
                    if (tracker.mStarted) {
                        tracker.cancelTimer();
                        invokeOnStopped(tracker.mCallback, tracker.mDevice, reason);
                    }
                }
                synchronized (set) {
                    set.removeIf(tracker -> tracker.mStarted);
                }
                break;
            default:
                Log.w(TAG, "onDistanceMeasurementStopped: invalid method " + method);
        }
    }

    void onDistanceMeasurementResult(String address, int centimeter, int errorCentimeter,
            int method) {
        logd("onDistanceMeasurementResult " + address + ", centimeter " + centimeter);
        switch (method) {
            case DistanceMeasurementParams.DISTANCE_MEASUREMENT_METHOD_RSSI:
                DistanceMeasurementResult result = new DistanceMeasurementResult.Builder()
                        .setMeters(centimeter / 100.0)
                        .setErrorMeters(errorCentimeter / 100.0).build();
                handleRSSIResult(address, result);
                break;
            default:
                Log.d(TAG, "onDistanceMeasurementResult: invalid method " + method);
        }
    }

    void handleRSSIResult(String address, DistanceMeasurementResult result) {
        HashSet<DistanceMeasurementTracker> set = mRssiTrackers.get(address);
        for (DistanceMeasurementTracker tracker : set) {
            try {
                if (!tracker.mStarted) {
                    continue;
                }
                tracker.mCallback.onResult(tracker.mDevice, result);
            } catch (RemoteException e) {
                Log.e(TAG, "Exception: " + e);
            }
        }
    }

    /** Logs the message in debug ROM. */
    private static void logd(String msg) {
        if (DBG) {
            Log.d(TAG, msg);
        }
    }

    static {
        classInitNative();
    }

    private static native void classInitNative();

    private native void initializeNative();

    private native void cleanupNative();

    private native void startDistanceMeasurementNative(String address, int frequency, int method);

    private native void stopDistanceMeasurementNative(String address, int method);

}
