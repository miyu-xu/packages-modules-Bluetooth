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

import android.annotation.RequiresPermission;
import android.app.PendingIntent;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.IScannerCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.AttributionSource;
import android.content.Context;
import android.content.Intent;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.util.Log;

import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.BluetoothAdapterProxy;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.flags.FeatureFlags;
import com.android.bluetooth.flags.FeatureFlagsImpl;
import com.android.bluetooth.gatt.ContextMap;
import com.android.bluetooth.gatt.GattObjectsFactory;
import com.android.bluetooth.gatt.PeriodicScanManager;
import com.android.bluetooth.gatt.ScanManager;

import java.util.List;

public class ScanManagerService extends ProfileService {
    private static final boolean DBG = true;
    private static final String TAG = "ScanManagerService";

    private FeatureFlags mFeatureFlags;
    private AdapterService mAdapterService;
    private BluetoothAdapterProxy mBluetoothAdapterProxy;

    ScanManager mScanManager;

    PeriodicScanManager mPeriodicScanManager;

    /**
     * Keep the arguments passed in for the PendingIntent.
     */
    class PendingIntentInfo {
        public PendingIntent intent;
        public ScanSettings settings;
        public List<ScanFilter> filters;
        public String callingPackage;
        public int callingUid;

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof PendingIntentInfo)) {
                return false;
            }
            return intent.equals(((PendingIntentInfo) other).intent);
        }
    }

    /**
     * List of our registered scanners.
     */
    class ScannerMap extends ContextMap<IScannerCallback, PendingIntentInfo> {}

    ScannerMap mScannerMap = new ScannerMap();

    protected ScanManagerService(Context ctx) {
        super(ctx);
    }

    @Override
    protected IProfileServiceBinder initBinder() {
        // WIP TODO
        return null;
    }

    @Override
    protected boolean start() {
        mFeatureFlags = new FeatureFlagsImpl();
        mAdapterService = AdapterService.getAdapterService();
        mBluetoothAdapterProxy = BluetoothAdapterProxy.getInstance();

        HandlerThread thread = new HandlerThread("BluetoothScanManager");
        thread.start();
        mScanManager =
            GattObjectsFactory.getInstance()
                .createScanManager(
                    this,
                    mAdapterService,
                    mBluetoothAdapterProxy,
                    thread.getLooper(),
                    mFeatureFlags);

        mPeriodicScanManager = GattObjectsFactory.getInstance()
            .createPeriodicScanManager(mAdapterService);

        return true;
    }

    @Override
    protected boolean stop() {
        mScannerMap.clear();

        return true;
    }

    @Override
    protected void cleanup() {
        if (mScanManager != null) {
            mScanManager.cleanup();
        }
        if (mPeriodicScanManager != null) {
            mPeriodicScanManager.cleanup();
        }
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    public void unregisterScanner(int scannerId, AttributionSource attributionSource) {
        if (!Utils.checkScanPermissionForDataDelivery(
            this, attributionSource, "GattService unregisterScanner")) {
            return;
        }

        if (DBG) {
            Log.d(TAG, "unregisterScanner() - scannerId=" + scannerId);
        }
        mScannerMap.remove(scannerId);
        mScanManager.unregisterScanner(scannerId);
    }

    // callback from ScanManager for dispatch of errors apps.
    public void onScanManagerErrorCallback(int scannerId, int errorCode) throws RemoteException {
        ScannerMap.App app = mScannerMap.getById(scannerId);
        if (app == null || (app.callback == null && app.info == null)) {
            Log.e(TAG, "App or callback is null");
            return;
        }
        if (app.callback != null) {
            app.callback.onScanManagerErrorCallback(errorCode);
        } else {
            try {
                sendErrorByPendingIntent(app.info, errorCode);
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, "Error sending error code via PendingIntent:" + e);
            }
        }
    }

    private void sendErrorByPendingIntent(PendingIntentInfo pii, int errorCode)
            throws PendingIntent.CanceledException {
        Intent extrasIntent = new Intent();
        extrasIntent.putExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, errorCode);
        pii.intent.send(this, 0, extrasIntent);
    }
}
