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

    private final SuspendObserver mSuspendObserver;
    private final AdapterNativeInterface mAdapterNativeInterface;
    private final SuspendListener mSuspendListener =
            new SuspendListener() {
                @Override
                public void onSuspend() {
                    handleSuspend();
                }

                @Override
                public void onResume() {
                    handleResume();
                }
            };

    public AdapterSuspend(
            AdapterNativeInterface adapterNativeInterface, SuspendObserver suspendObserver) {
        mAdapterNativeInterface = requireNonNull(adapterNativeInterface);
        mSuspendObserver = requireNonNull(suspendObserver);

        mSuspendObserver.registerListener(mSuspendListener);
    }

    void cleanup() {
        mSuspendObserver.cleanup();
    }

    @VisibleForTesting
    boolean isSuspended() {
        return mSuspended;
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
        mAdapterNativeInterface.allowWakeByHid();
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

    interface SuspendObserver {
        void registerListener(SuspendListener listener);

        void cleanup();
    }

    interface SuspendListener {
        void onSuspend();

        void onResume();
    }

    static class SuspendObserverFactory {
        static SuspendObserver createDisplaySuspendObserver(
                Looper looper, DisplayManager displayManager) {
            return new DisplaySuspendObserver(looper, displayManager);
        }
    }

    static class DisplaySuspendObserver implements SuspendObserver {
        private SuspendListener mSuspendListener;

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
                        update();
                    }
                };

        DisplaySuspendObserver(Looper looper, DisplayManager displayManager) {
            mLooper = requireNonNull(looper);
            mDisplayManager = requireNonNull(displayManager);
        }

        @Override
        public void registerListener(SuspendListener suspendListener) {
            mSuspendListener = requireNonNull(suspendListener);
            mDisplayManager.registerDisplayListener(mDisplayListener, new Handler(mLooper));
        }

        @Override
        public void cleanup() {
            if (mSuspendListener != null) {
                mDisplayManager.unregisterDisplayListener(mDisplayListener);
                mSuspendListener = null;
            }
        }

        private boolean isScreenOn() {
            return Arrays.stream(mDisplayManager.getDisplays())
                    .anyMatch(display -> display.getState() == Display.STATE_ON);
        }

        private void update() {
            if (isScreenOn()) {
                mSuspendListener.onResume();
            } else {
                mSuspendListener.onSuspend();
            }
        }
    }
}
