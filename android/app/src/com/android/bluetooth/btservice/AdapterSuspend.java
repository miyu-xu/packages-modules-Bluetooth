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

import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.Display;

import java.util.Arrays;

public class AdapterSuspend {
    private static final String TAG = "BtAdapterSuspend";

    // Event mask bits corresponding to specific HCI events
    // as defined in Bluetooth core v5.4, Vol 4, Part E, 7.3.1.
    private static final long MASK_DISCONNECT_CMPLT = 1 << 4;
    private static final long MASK_MODE_CHANGE = 1 << 19;

    private boolean mSuspended = false;

    private final AdapterNativeInterface mAdapterNativeInterface;
    private final Looper mLooper;
    private final DisplayManager mDisplayManager;
    private final DisplayManager.DisplayListener mDisplayListener =
            new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {}

                @Override
                public void onDisplayRemoved(int displayId) {}

                @Override
                public void onDisplayChanged(int displayId) {
                    if (isScreenOn()) {
                        handleResume();
                    } else {
                        handleSuspend();
                    }
                }
            };
    private final PowerManager mPowerManager;

    public AdapterSuspend(
            AdapterNativeInterface adapterNativeInterface,
            Looper looper,
            DisplayManager displayManager,
            PowerManager powerManager) {
        mAdapterNativeInterface = requireNonNull(adapterNativeInterface);
        mLooper = requireNonNull(looper);
        mDisplayManager = requireNonNull(displayManager);
        mPowerManager = requireNonNull(powerManager);

        mDisplayManager.registerDisplayListener(mDisplayListener, new Handler(mLooper));
    }

    void cleanup() {
        mDisplayManager.unregisterDisplayListener(mDisplayListener);
    }

    private boolean isScreenOn() {
        return Arrays.stream(mDisplayManager.getDisplays())
                .anyMatch(display -> display.getState() == Display.STATE_ON);
    }

    private void handleSuspend() {
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
        if (mPowerManager.getLastSleepReason() != mPowerManager.GO_TO_SLEEP_REASON_LID_SWITCH) {
            mAdapterNativeInterface.allowWakeByHid();
        }
        Log.i(TAG, "ready to suspend");
    }

    private void handleResume() {
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
