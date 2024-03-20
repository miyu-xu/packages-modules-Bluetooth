/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.bluetooth.le_scan;

import static java.util.Objects.requireNonNull;

import android.app.PendingIntent;
import android.bluetooth.IBluetoothScan;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.AttributionSource;
import android.content.Context;
import android.os.IBinder;
import android.os.Looper;

import com.android.bluetooth.Utils;

import java.util.List;

/** "Not a service" to wrap scan interaction */
public class ScanManagerService {
    private final Context mContext;
    public final IBinder mBinder;
    private final TransitionalScanHelper mTransitionalScanHelper;

    public ScanManagerService(Context ctx, Looper looper) {
        mContext = requireNonNull(ctx);
        mBinder = new BluetoothScanManagerBinder(this);
        mTransitionalScanHelper = new TransitionalScanHelper(mContext, this::isTestModeEnabled);
        mTransitionalScanHelper.start(looper);
    }

    private boolean isTestModeEnabled() {
        return false;
    }

    public void cleanup() {
        mTransitionalScanHelper.stop();
        mTransitionalScanHelper.cleanup();
    }

    static class BluetoothScanManagerBinder extends IBluetoothScan.Stub {
        private final ScanManagerService mService;

        BluetoothScanManagerBinder(ScanManagerService svc) {
            mService = svc;
        }

        @Override
        public void startScanForIntent(
                PendingIntent intent,
                ScanSettings settings,
                List<ScanFilter> filters,
                AttributionSource source) {
            if (!Utils.checkScanPermissionForDataDelivery(
                    mService.mContext, source, "Starting GATT scan.")) {
                return;
            }
            mService.mTransitionalScanHelper.registerPiAndStartScan(
                    intent, settings, filters, source);
        }
    }
}
