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
import android.util.Log;
import android.view.Display;

import com.android.internal.annotations.VisibleForTesting;

import java.util.Arrays;

public class AdapterSuspend {
    private static final String TAG = "BtAdapterSuspend";

    // Event mask bits corresponding to specific HCI events
    // as defined in Bluetooth core v5.4, Vol 4, Part E, 7.3.1.
    private static final long MASK_DISCONNECT_CMPLT = 1 << 4;
    private static final long MASK_MODE_CHANGE = 1 << 19;

    private boolean mSuspended = false;

    private final AdapterNativeInterface mAdapterNativeInterface;
    private final Handler mHandler;

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
                        handleResume(0);
                    } else {
                        handleSuspend(0);
                    }
                }
            };

    public AdapterSuspend(
            AdapterNativeInterface adapterNativeInterface,
            Looper looper,
            DisplayManager displayManager) {
        mAdapterNativeInterface = requireNonNull(adapterNativeInterface);
        mHandler = new Handler(requireNonNull(looper));
        mDisplayManager = requireNonNull(displayManager);

        mDisplayManager.registerDisplayListener(mDisplayListener, mHandler);
    }

    void cleanup() {
        mDisplayManager.unregisterDisplayListener(mDisplayListener);
    }

    @VisibleForTesting
    boolean isSuspended() {
        return mSuspended;
    }

    private boolean isScreenOn() {
        return Arrays.stream(mDisplayManager.getDisplays())
                .anyMatch(display -> display.getState() == Display.STATE_ON);
    }

    /**
     * Determine if wake by BT is allowed according to suspend reason.
     *
     * @param reason sleep reason.
     */
    public void handleSuspend(int reason) {
        // TODO determine if allowing BT wake here.
        mHandler.post(() -> handleSuspendInternal(true));
    }

    @VisibleForTesting
    void handleSuspendInternal(boolean allowBtWake) {
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

        if (allowBtWake) {
            mAdapterNativeInterface.allowWakeByHid();
            Log.i(TAG, "configure wake by hid");
        }
        Log.i(TAG, "ready to suspend");
    }

    /**
     * Prepare for resume.
     *
     * @param reason wakeup reason.
     */
    public void handleResume(int reason) {
        mHandler.post(() -> handleResumeInternal());
    }

    @VisibleForTesting
    void handleResumeInternal() {
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
