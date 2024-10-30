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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public class AdapterSuspend {
    private static final String TAG = "BtAdapterSuspend";

    // Event mask bits corresponding to specific HCI events
    // as defined in Bluetooth core v5.4, Vol 4, Part E, 7.3.1.
    private static final long MASK_DISCONNECT_CMPLT = 1 << 4;
    private static final long MASK_MODE_CHANGE = 1 << 19;

    private DeviceStateManager mDeviceStateManager;

    public final DeviceStateManager.DeviceStateCallback mDeviceStateCallback =
            new DeviceStateManager.DeviceStateCallback() {
                @Override
                public void onDeviceStateChanged(@NonNull DeviceState state) {
                    String nextState = state.getName();
                    switch (mCurrentState) {
                        case "None": // initalize
                            switch (nextState) {
                                case "LAPTOP":
                                    mWakeByBt = true;
                                    break;
                                case "TABLET":
                                    mWakeByBt = false;
                                    break;
                                default:
                                    Log.i(TAG, "Unknown initial state " + nextState);
                                    return;
                            }
                            break;
                        case "CLOSED":
                            switch (nextState) {
                                case "DISPLAY_OFF":
                                    mWakeByBt = true;
                                    break;
                                case "LAPTOP":
                                    mWakeByBt = true;
                                    // fall through
                                case "TABLET":
                                    handleResume();
                                    break;
                                default:
                                    Log.i(TAG, "Ignore state " + nextState);
                                    return;
                            }
                            break;
                        case "DISPLAY_OFF":
                            switch (nextState) {
                                case "CLOSED":
                                    mWakeByBt = false;
                                    break;
                                case "TABLET":
                                    mWakeByBt = false;
                                    // fall through
                                case "LAPTOP":
                                    handleResume();
                                    break;
                                default:
                                    Log.i(TAG, "Ignore state " + nextState);
                                    return;
                            }
                            break;
                        case "LAPTOP":
                            switch (nextState) {
                                case "CLOSED":
                                    mWakeByBt = false;
                                    // fall through
                                case "DISPLAY_OFF":
                                    handleSuspend();
                                    break;
                                case "TABLET":
                                    mWakeByBt = false;
                                    break;
                                default:
                                    Log.i(TAG, "Ignore state " + nextState);
                                    return;
                            }
                            break;
                        case "TABLET":
                            switch (nextState) {
                                case "CLOSED":
                                    // fall through
                                case "DISPLAY_OFF":
                                    handleSuspend();
                                    break;
                                case "LAPTOP":
                                    mWakeByBt = true;
                                    break;
                                default:
                                    Log.i(TAG, "Ignore state " + nextState);
                                    return;
                            }
                            break;
                        default:
                            Log.e(TAG, "Unknown current state " + mCurrentState);
                            return;
                    }
                    mCurrentState = nextState;
                }
            };

    public static class HandlerExecutor implements Executor {
        private final Handler mHandler;

        public HandlerExecutor(@NonNull Handler handler) {
            mHandler = handler;
        }

        @Override
        public void execute(Runnable command) {
            if (!mHandler.post(command)) {
                throw new RejectedExecutionException(mHandler + " is shutting down");
            }
        }
    }

    public HandlerExecutor mExecutor;

    private boolean mSuspended = false;

    // Value should be initialized at boot time
    private String mCurrentState = "LAPTOP";
    private boolean mWakeByBt = true;

    private final AdapterNativeInterface mAdapterNativeInterface;
    private final Handler mHandler;

    public AdapterSuspend(
            AdapterNativeInterface adapterNativeInterface,
            Looper looper,
            DeviceStateManager deviceStateManager) {
        mAdapterNativeInterface = requireNonNull(adapterNativeInterface);
        mHandler = new Handler(requireNonNull(looper));

        mExecutor = new HandlerExecutor(mHandler);
        mDeviceStateManager = requireNonNull(deviceStateManager);
        mDeviceStateManager.registerCallback(mExecutor, mDeviceStateCallback);
    }

    void cleanup() {
        if (mDeviceStateManager != null) {
            mDeviceStateManager.unregisterCallback(mDeviceStateCallback);
            mDeviceStateManager = null;
        }
    }

    @VisibleForTesting
    boolean isSuspended() {
        return mSuspended;
    }

    /** Prepare suspend according to wake by BT status. */
    public void handleSuspend() {
        mHandler.post(() -> handleSuspendInternal(mWakeByBt));
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

    /** Prepare for resume. */
    public void handleResume() {
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
