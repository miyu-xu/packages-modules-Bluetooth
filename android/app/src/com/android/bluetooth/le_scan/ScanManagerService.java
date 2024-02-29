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

import android.app.PendingIntent;
import android.bluetooth.IBluetoothScan;
import android.bluetooth.le.IScannerCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.AttributionSource;
import android.content.Context;
import android.os.Build;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.Log;

import com.android.bluetooth.btservice.ProfileService;
import com.android.modules.utils.SynchronousResultReceiver;

import java.util.List;

public class ScanManagerService extends ProfileService {
    private static final boolean DBG = Build.TYPE.equals("userdebug") || Build.TYPE.equals("eng");
    private static final String TAG = ScanManagerService.class.getSimpleName();

    public final TransitionalScanHelper mTransitionalScanHelper =
        new TransitionalScanHelper(this, this::isTestModeEnabled);

    public ScanManagerService(Context ctx) {
        super(ctx);
    }

    @Override
    protected IProfileServiceBinder initBinder() {
        return new BluetoothScanManagerBinder(this);
    }

    @Override
    public void start() {
        if (DBG) {
            Log.d(TAG, "start()");
        }
        HandlerThread thread = new HandlerThread("BluetoothScanManager");
        thread.start();
        mTransitionalScanHelper.start(thread.getLooper());
    }

    @Override
    public void stop() {
        if (DBG) {
            Log.d(TAG, "stop()");
        }
        mTransitionalScanHelper.stop();
        cleanup();
    }

    @Override
    public void cleanup() {
        if (DBG) {
            Log.d(TAG, "cleanup()");
        }
        mTransitionalScanHelper.cleanup();
    }

    TransitionalScanHelper getTransitionalScanHelper() {
        return mTransitionalScanHelper;
    }

     static class BluetoothScanManagerBinder extends IBluetoothScan.Stub
                implements IProfileServiceBinder {
         private ScanManagerService mService;

         BluetoothScanManagerBinder(ScanManagerService svc) {
             mService = svc;
         }

         @Override
         public void cleanup() {
             mService = null;
         }

         private ScanManagerService getService() {
             if (mService != null && mService.isAvailable()) {
                 return mService;
             }
             Log.e(TAG, "getService() - Service requested, but not available!");
             return null;
         }

         @Override
         public void registerScanner(IScannerCallback callback, WorkSource workSource,
             AttributionSource attributionSource, SynchronousResultReceiver receiver)
             throws RemoteException {
             try {
                 registerScanner(callback, workSource, attributionSource);
                 receiver.send(null);
             } catch (RuntimeException e) {
                 receiver.propagateException(e);
             }
         }
         private void registerScanner(IScannerCallback callback, WorkSource workSource,
             AttributionSource attributionSource) throws RemoteException {
             ScanManagerService service = getService();
             if (service == null) {
                 return;
             }
             service.getTransitionalScanHelper()
                 .registerScanner(callback, workSource, attributionSource);
         }

         @Override
         public void unregisterScanner(int scannerId, AttributionSource attributionSource,
             SynchronousResultReceiver receiver) {
             try {
                 unregisterScanner(scannerId, attributionSource);
                 receiver.send(null);
             } catch (RuntimeException e) {
                 receiver.propagateException(e);
             }
         }
         private void unregisterScanner(int scannerId, AttributionSource attributionSource) {
             ScanManagerService service = getService();
             if (service == null) {
                 return;
             }
             service.getTransitionalScanHelper().unregisterScanner(scannerId, attributionSource);
         }

         @Override
         public void startScan(int scannerId, ScanSettings settings, List<ScanFilter> filters,
             AttributionSource attributionSource, SynchronousResultReceiver receiver) {
             try {
                 startScan(scannerId, settings, filters,
                     attributionSource);
                 receiver.send(null);
             } catch (RuntimeException e) {
                 receiver.propagateException(e);
             }
         }
         private void startScan(int scannerId, ScanSettings settings, List<ScanFilter> filters,
             AttributionSource attributionSource) {
             ScanManagerService service = getService();
             if (service == null) {
                 return;
             }
             service.getTransitionalScanHelper()
                 .startScan(scannerId, settings, filters, attributionSource);
         }

         @Override
         public void startScanForIntent(PendingIntent intent, ScanSettings settings,
             List<ScanFilter> filters, AttributionSource attributionSource,
             SynchronousResultReceiver receiver)
             throws RemoteException {
             try {
                 startScanForIntent(intent, settings,
                     filters, attributionSource);
                 receiver.send(null);
             } catch (RuntimeException e) {
                 receiver.propagateException(e);
             }
         }
         private void startScanForIntent(PendingIntent intent, ScanSettings settings,
             List<ScanFilter> filters, AttributionSource attributionSource)
             throws RemoteException {
             ScanManagerService service = getService();
             if (service == null) {
                 return;
             }
             service.getTransitionalScanHelper()
                 .registerPiAndStartScan(intent, settings, filters, attributionSource);
         }

         @Override
         public void stopScanForIntent(PendingIntent intent, AttributionSource attributionSource,
             SynchronousResultReceiver receiver) throws RemoteException {
             try {
                 stopScanForIntent(intent, attributionSource);
                 receiver.send(null);
             } catch (RuntimeException e) {
                 receiver.propagateException(e);
             }
         }
         private void stopScanForIntent(PendingIntent intent, AttributionSource attributionSource)
             throws RemoteException {
             ScanManagerService service = getService();
             if (service == null) {
                 return;
             }
             service.getTransitionalScanHelper().stopScan(intent, attributionSource);
         }

         @Override
         public void stopScan(int scannerId, AttributionSource attributionSource,
             SynchronousResultReceiver receiver) {
             try {
                 stopScan(scannerId, attributionSource);
                 receiver.send(null);
             } catch (RuntimeException e) {
                 receiver.propagateException(e);
             }
         }
         private void stopScan(int scannerId, AttributionSource attributionSource) {
             ScanManagerService service = getService();
             if (service == null) {
                 return;
             }
             service.getTransitionalScanHelper().stopScan(scannerId, attributionSource);
         }

         @Override
         public void flushPendingBatchResults(int scannerId, AttributionSource attributionSource,
             SynchronousResultReceiver receiver) {
             try {
                 flushPendingBatchResults(scannerId, attributionSource);
                 receiver.send(null);
             } catch (RuntimeException e) {
                 receiver.propagateException(e);
             }
         }
         private void flushPendingBatchResults(int scannerId, AttributionSource attributionSource) {
             ScanManagerService service = getService();
             if (service == null) {
                 return;
             }
             service.getTransitionalScanHelper()
                 .flushPendingBatchResults(scannerId, attributionSource);
         }
     }
}
