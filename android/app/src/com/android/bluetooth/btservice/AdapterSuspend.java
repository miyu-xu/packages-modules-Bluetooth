/*
 * Copyright 2024 The Android Open Source Project
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

import static android.bluetooth.BluetoothAdapter.SCAN_MODE_CONNECTABLE;
import static android.bluetooth.BluetoothAdapter.SCAN_MODE_NONE;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.hardware.devicestate.DeviceState;
import android.hardware.devicestate.DeviceStateManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.android.internal.annotations.VisibleForTesting;

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class AdapterSuspend {
    private static final String TAG = "BtAdapterSuspend";

    // Event mask bits corresponding to specific HCI events
    // as defined in Bluetooth core v5.4, Vol 4, Part E, 7.3.1.
    private static final long MASK_DISCONNECT_CMPLT = 1 << 4;
    private static final long MASK_MODE_CHANGE = 1 << 19;

    private final DeviceStateManager mDeviceStateManager;

    public final DeviceStateManager.DeviceStateCallback mDeviceStateCallback =
            new DeviceStateManager.DeviceStateCallback() {
                @Override
                public void onDeviceStateChanged(@NonNull DeviceState state) {
                    String nextState = state.getName();
                    Log.d(TAG, "Handle state transition: " + mCurrentState + " => " + nextState);
                    if (mCurrentState.equals("None")) {
                        mCurrentState = nextState;
                        Log.i(TAG, "Initialize device state to " + nextState);
                        return;
                    }

                    switch (nextState) {
                        case "CLOSED" -> {
                            switch (mCurrentState) {
                                case "DISPLAY_OFF" ->
                                        Log.d(TAG, "No action for state " + nextState);
                                default -> handleSuspend(false);
                            }
                        }
                        case "DISPLAY_OFF" -> {
                            switch (mCurrentState) {
                                case "TABLET" -> handleSuspend(false);
                                case "DOCKED", "LAPTOP" -> handleSuspend(true);
                                default -> Log.d(TAG, "No action for state " + nextState);
                            }
                        }
                        case "LAPTOP", "DOCKED", "TABLET" -> {
                            switch (mCurrentState) {
                                case "CLOSED", "DISPLAY_OFF" -> handleResume();
                                default -> Log.d(TAG, "No action for state " + nextState);
                            }
                        }
                        default -> {
                            Log.wtf(TAG, "Unknown state transition to " + nextState);
                            return;
                        }
                    }
                    mCurrentState = nextState;
                }
            };

    private boolean mSuspended = false;

    // Value should be initialized when registering the mDeviceStateCallback.
    private String mCurrentState = "None";

    private final AdapterNativeInterface mAdapterNativeInterface;

    public AdapterSuspend(
            AdapterNativeInterface adapterNativeInterface,
            Looper looper,
            DeviceStateManager deviceStateManager) {
        mAdapterNativeInterface = requireNonNull(adapterNativeInterface);
        Handler handler = new Handler(requireNonNull(looper));

        mDeviceStateManager = requireNonNull(deviceStateManager);
        mDeviceStateManager.registerCallback(handler::post, mDeviceStateCallback);
    }

    void cleanup() {
        mDeviceStateManager.unregisterCallback(mDeviceStateCallback);
    }

    @VisibleForTesting
    boolean isSuspended() {
        return mSuspended;
    }

    @VisibleForTesting
    void handleSuspend(boolean allowWakeByHid) {
        if (mSuspended) {
            return;
        }
        mSuspended = true;

        long mask = MASK_DISCONNECT_CMPLT | MASK_MODE_CHANGE;
        long leMask = 0;

        // Avoid unexpected interrupt during suspend.
        mAdapterNativeInterface.setDefaultEventMaskExcept(mask, leMask);

        // Disable inquiry scan and page scan.
        mAdapterNativeInterface.setScanMode(AdapterService.convertScanModeToHal(SCAN_MODE_NONE));

        mAdapterNativeInterface.clearEventFilter();
        mAdapterNativeInterface.clearFilterAcceptList();
        mAdapterNativeInterface.disconnectAllAcls();

        if (allowWakeByHid) {
            mAdapterNativeInterface.allowWakeByHid();
            Log.i(TAG, "configure wake by hid");
        }
        Log.i(TAG, "ready to suspend");
    }

    @VisibleForTesting
    void handleResume() {
        if (!mSuspended) {
            return;
        }
        mSuspended = false;

        long mask = 0;
        long leMask = 0;
        mAdapterNativeInterface.setDefaultEventMaskExcept(mask, leMask);
        mAdapterNativeInterface.clearEventFilter();
        mAdapterNativeInterface.restoreFilterAcceptList();
        mAdapterNativeInterface.setScanMode(
                AdapterService.convertScanModeToHal(SCAN_MODE_CONNECTABLE));
        Log.i(TAG, "resumed");
    }
}
