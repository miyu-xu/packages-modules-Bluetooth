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
import android.bluetooth.IBluetoothGatt;
import android.bluetooth.le.BluetoothLeScanner;
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
import com.android.bluetooth.gatt.GattObjectsFactory;
import com.android.bluetooth.gatt.GattService;

public class ScanManagerService extends ProfileService {
    private static final boolean DBG = false; // TODO(b/327503826). We need ScanManagerServiceConfig.
//    private static final boolean DBG = ScanManagerServiceConfig.DBG;
    private static final String TAG = "ScanManagerService"; // TODO(b/327503826). We need ScanManagerServiceConfig.
//    private static final String TAG = ScanManagerServiceConfig.TAG_PREFIX + "ScanManagerService";

    PeriodicScanManager mPeriodicScanManager;
    ScanManager mScanManager;

    public final TransitionalScanHelper mTransitionalScanHelper = new TransitionalScanHelper();

    public ScanManagerService(Context ctx) {
        super(ctx);
    }

    @Override
    protected IProfileServiceBinder initBinder() {
        return new ScanManagerService.BluetoothScanManagerBinder(this);
    }

    @Override
    public void start() {
        if (DBG) {
            Log.d(TAG, "start()");
        }

        AdapterService mAdapterService = AdapterService.getAdapterService();
        BluetoothAdapterProxy mBluetoothAdapterProxy = BluetoothAdapterProxy.getInstance();

        HandlerThread thread = new HandlerThread("BluetoothScanManager");
        thread.start();
        mScanManager =
            GattObjectsFactory.getInstance()
                .createScanManager(
                    this, mAdapterService, mBluetoothAdapterProxy, thread.getLooper());

        mPeriodicScanManager = GattObjectsFactory.getInstance()
            .createPeriodicScanManager(mAdapterService);
    }

    @Override
    public void stop() {
        if (DBG) {
            Log.d(TAG, "stop()");
        }
        mTransitionalScanHelper.getScannerMap().clear();
        cleanup();
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    public void unregisterScanner(int scannerId, AttributionSource attributionSource) {
        if (!Utils.checkScanPermissionForDataDelivery(
            this, attributionSource, "ScanManagerService unregisterScanner")) {
            return;
        }

        if (DBG) {
            Log.d(TAG, "unregisterScanner() - scannerId=" + scannerId);
        }
        mTransitionalScanHelper.getScannerMap().remove(scannerId);
        mScanManager.unregisterScanner(scannerId);
    }

    // callback from ScanManager for dispatch of errors apps.
    public void onScanManagerErrorCallback(int scannerId, int errorCode) throws RemoteException {
        TransitionalScanHelper.ScannerMap.App app =
            mTransitionalScanHelper.getScannerMap().getById(scannerId);
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

    private void sendErrorByPendingIntent(GattService.PendingIntentInfo pii, int errorCode)
        throws PendingIntent.CanceledException {
        Intent extrasIntent = new Intent();
        extrasIntent.putExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, errorCode);
        pii.intent.send(this, 0, extrasIntent);
    }

     static class BluetoothScanManagerBinder extends IBluetoothGatt.Stub
                implements IProfileServiceBinder {
         private ScanManagerService mService;

         BluetoothScanManagerBinder(ScanManagerService svc) {
             mService = svc;
         }

         // TODO(b/327503826). Do we reuse `IBluetoothGatt` or do we need a new aidl?
    }
}
