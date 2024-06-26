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

package android.bluetooth;

import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.le.ScanResult;
import android.content.AttributionSource;
import android.os.WorkSource;

import android.bluetooth.le.IScannerCallback;
import android.bluetooth.le.IPeriodicAdvertisingCallback;

/**
 * API for interacting with BLE Scan
 * @hide
 */
interface IBluetoothScan {
    void registerScanner(in IScannerCallback callback, in WorkSource workSource, in AttributionSource attributionSource);
    void unregisterScanner(in int scannerId, in AttributionSource attributionSource);
    void startScan(in int scannerId, in ScanSettings settings, in List<ScanFilter> filters,
                   in AttributionSource attributionSource);
    void startScanForIntent(in PendingIntent intent, in ScanSettings settings, in List<ScanFilter> filters,
                            in AttributionSource attributionSource);
    void stopScan(in int scannerId, in AttributionSource attributionSource);
    void stopScanForIntent(in PendingIntent intent, in AttributionSource attributionSource);
    void flushPendingBatchResults(in int scannerId, in AttributionSource attributionSource);

    void registerSync(in ScanResult scanResult, in int skip, in int timeout, in IPeriodicAdvertisingCallback callback, in AttributionSource attributionSource);
    void unregisterSync(in IPeriodicAdvertisingCallback callback, in AttributionSource attributionSource);
    void transferSync(in BluetoothDevice bda, in int serviceData, in int syncHandle, in AttributionSource attributionSource);
    void transferSetInfo(in BluetoothDevice bda, in int serviceData, in int advertisingHandle, in IPeriodicAdvertisingCallback callback,  in AttributionSource attributionSource);

    int numHwTrackFiltersAvailable(in AttributionSource attributionSource);
}
