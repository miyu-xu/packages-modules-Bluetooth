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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothScan;
import android.bluetooth.le.IPeriodicAdvertisingCallback;
import android.bluetooth.le.IScannerCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.AttributionSource;
import android.content.Context;
import android.os.Build;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.Log;

import com.android.modules.utils.SynchronousResultReceiver;

import java.util.List;

public class ScanManagerService {
    private static final boolean DBG = Build.TYPE.equals("userdebug") || Build.TYPE.equals("eng");
    private static final String TAG = ScanManagerService.class.getSimpleName();

    public final TransitionalScanHelper mTransitionalScanHelper;

    private final BluetoothScanManagerBinder mBinder;

    private boolean isAvailable = false;

    public ScanManagerService(Context ctx) {
        mBinder = new BluetoothScanManagerBinder(this);
        TransitionalScanHelper.TestModeAccessor isTestModeEnabled = () -> false;
        mTransitionalScanHelper = new TransitionalScanHelper(ctx, isTestModeEnabled);
    }

    public void start() {
        if (DBG) {
            Log.d(TAG, "start()");
        }
        isAvailable = true;
        HandlerThread thread = new HandlerThread("BluetoothScanManager");
        thread.start();
        mTransitionalScanHelper.start(thread.getLooper());
    }

    public void stop() {
        if (DBG) {
            Log.d(TAG, "stop()");
        }
        isAvailable = false;
        mTransitionalScanHelper.stop();
        mTransitionalScanHelper.cleanup();
    }

    TransitionalScanHelper getTransitionalScanHelper() {
        return mTransitionalScanHelper;
    }

    public IBinder getBinder() {
        return mBinder;
    }

    static class BluetoothScanManagerBinder extends IBluetoothScan.Stub {
        private final ScanManagerService mService;

        BluetoothScanManagerBinder(ScanManagerService svc) {
            mService = svc;
        }

        private ScanManagerService getService() {
            if (mService.isAvailable) {
                return mService;
            }
            Log.e(TAG, "getService() - ScanManagerService requested, but not available!");
            return null;
        }

        @Override
        public void registerScanner(IScannerCallback callback, WorkSource workSource,
                AttributionSource attributionSource, SynchronousResultReceiver receiver)
                throws RemoteException {
            try {
                ScanManagerService service = getService();
                if (service == null) {
                    return;
                }
                service.getTransitionalScanHelper().registerScanner(callback, workSource,
                        attributionSource);
                receiver.send(null);
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }

        @Override
        public void unregisterScanner(int scannerId, AttributionSource attributionSource,
                SynchronousResultReceiver receiver) {
            try {
                ScanManagerService service = getService();
                if (service == null) {
                    return;
                }
                service.getTransitionalScanHelper().unregisterScanner(scannerId, attributionSource);
                receiver.send(null);
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }

        @Override
        public void startScan(int scannerId, ScanSettings settings, List<ScanFilter> filters,
                AttributionSource attributionSource, SynchronousResultReceiver receiver) {
            try {
                ScanManagerService service = getService();
                if (service == null) {
                    return;
                }
                service.getTransitionalScanHelper().startScan(scannerId, settings, filters,
                        attributionSource);
                receiver.send(null);
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }

        @Override
        public void startScanForIntent(PendingIntent intent, ScanSettings settings,
                List<ScanFilter> filters, AttributionSource attributionSource,
                SynchronousResultReceiver receiver) throws RemoteException {
            try {
                ScanManagerService service = getService();
                if (service == null) {
                    return;
                }
                service.getTransitionalScanHelper().registerPiAndStartScan(intent, settings,
                        filters, attributionSource);
                receiver.send(null);
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }

        @Override
        public void stopScanForIntent(PendingIntent intent, AttributionSource attributionSource,
                SynchronousResultReceiver receiver) throws RemoteException {
            try {
                ScanManagerService service = getService();
                if (service == null) {
                    return;
                }
                service.getTransitionalScanHelper().stopScan(intent, attributionSource);
                receiver.send(null);
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }

        @Override
        public void stopScan(int scannerId, AttributionSource attributionSource,
                SynchronousResultReceiver receiver) {
            try {
                ScanManagerService service = getService();
                if (service == null) {
                    return;
                }
                service.getTransitionalScanHelper().stopScan(scannerId, attributionSource);
                receiver.send(null);
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }

        @Override
        public void flushPendingBatchResults(int scannerId, AttributionSource attributionSource,
                SynchronousResultReceiver receiver) {
            try {
                ScanManagerService service = getService();
                if (service == null) {
                    return;
                }
                service.getTransitionalScanHelper().flushPendingBatchResults(scannerId,
                        attributionSource);
                receiver.send(null);
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }

        @Override
        public void registerSync(ScanResult scanResult, int skip, int timeout,
                IPeriodicAdvertisingCallback callback, AttributionSource attributionSource,
                SynchronousResultReceiver receiver) {
            try {
                ScanManagerService service = getService();
                if (service == null) {
                    return;
                }
                service.getTransitionalScanHelper()
                    .registerSync(scanResult, skip, timeout, callback, attributionSource);
                receiver.send(null);
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }

        @Override
        public void transferSync(BluetoothDevice bda, int serviceData , int syncHandle,
                AttributionSource attributionSource, SynchronousResultReceiver receiver) {
            try {
                ScanManagerService service = getService();
                if (service == null) {
                    return;
                }
                service.getTransitionalScanHelper()
                    .transferSync(bda, serviceData, syncHandle, attributionSource);
                receiver.send(null);
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }

        @Override
        public void transferSetInfo(BluetoothDevice bda, int serviceData , int advHandle,
                IPeriodicAdvertisingCallback callback, AttributionSource attributionSource,
                SynchronousResultReceiver receiver) {
            try {
                ScanManagerService service = getService();
                if (service == null) {
                    return;
                }
                service.getTransitionalScanHelper()
                    .transferSetInfo(bda, serviceData, advHandle, callback, attributionSource);
                receiver.send(null);
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }

        @Override
        public void unregisterSync(IPeriodicAdvertisingCallback callback,
                AttributionSource attributionSource, SynchronousResultReceiver receiver) {
            try {
                ScanManagerService service = getService();
                if (service == null) {
                    return;
                }
                service.getTransitionalScanHelper().unregisterSync(callback, attributionSource);
                receiver.send(null);
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }

        @Override
        public void numHwTrackFiltersAvailable(AttributionSource attributionSource,
                SynchronousResultReceiver receiver) {
            try {
                ScanManagerService service = getService();
                if (service == null) {
                    return;
                }
                receiver.send(service.getTransitionalScanHelper()
                        .numHwTrackFiltersAvailable(attributionSource));
            } catch (RuntimeException e) {
                receiver.propagateException(e);
            }
        }
    }
}
