/*
 * Copyright 2013 The Android Open Source Project
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
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.DistanceMeasurementMethod;
import android.bluetooth.le.DistanceMeasurementParams;
import android.bluetooth.le.IDistanceMeasurementCallback;
import android.bluetooth.le.PeriodicAdvertisingParameters;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.le.ResultStorageDescriptor;
import android.content.AttributionSource;
import android.os.ParcelUuid;
import android.os.WorkSource;

import android.bluetooth.IBluetoothGattCallback;
import android.bluetooth.IBluetoothGattServerCallback;
import android.bluetooth.le.IAdvertisingSetCallback;
import android.bluetooth.le.IPeriodicAdvertisingCallback;
import android.bluetooth.le.IScannerCallback;

/**
 * API for interacting with BLE / GATT
 * @hide
 */
interface IBluetoothGatt {
    List<BluetoothDevice> getDevicesMatchingConnectionStates(in int[] states, in AttributionSource attributionSource);

    void registerScanner(in IScannerCallback callback, in WorkSource workSource, in AttributionSource attributionSource);
    void unregisterScanner(in int scannerId, in AttributionSource attributionSource);
    void startScan(in int scannerId, in ScanSettings settings, in List<ScanFilter> filters,
                   in AttributionSource attributionSource);
    void startScanForIntent(in PendingIntent intent, in ScanSettings settings, in List<ScanFilter> filters,
                            in AttributionSource attributionSource);
    void stopScanForIntent(in PendingIntent intent, in AttributionSource attributionSource);
    void stopScan(in int scannerId, in AttributionSource attributionSource);
    void flushPendingBatchResults(in int scannerId, in AttributionSource attributionSource);

    void startAdvertisingSet(in AdvertisingSetParameters parameters, in AdvertiseData advertiseData,
                                in AdvertiseData scanResponse, in PeriodicAdvertisingParameters periodicParameters,
                                in AdvertiseData periodicData, in int duration, in int maxExtAdvEvents, in int gattServerIf,
                                in IAdvertisingSetCallback callback, in AttributionSource attributionSource);
    void stopAdvertisingSet(in IAdvertisingSetCallback callback, in AttributionSource attributionSource);

    void getOwnAddress(in int advertiserId, in AttributionSource attributionSource);
    void enableAdvertisingSet(in int advertiserId, in boolean enable, in int duration, in int maxExtAdvEvents, in AttributionSource attributionSource);
    void setAdvertisingData(in int advertiserId, in AdvertiseData data, in AttributionSource attributionSource);
    void setScanResponseData(in int advertiserId, in AdvertiseData data, in AttributionSource attributionSource);
    void setAdvertisingParameters(in int advertiserId, in AdvertisingSetParameters parameters, in AttributionSource attributionSource);
    void setPeriodicAdvertisingParameters(in int advertiserId, in PeriodicAdvertisingParameters parameters, in AttributionSource attributionSource);
    void setPeriodicAdvertisingData(in int advertiserId, in AdvertiseData data, in AttributionSource attributionSource);
    void setPeriodicAdvertisingEnable(in int advertiserId, in boolean enable, in AttributionSource attributionSource);

    void registerSync(in ScanResult scanResult, in int skip, in int timeout, in IPeriodicAdvertisingCallback callback, in AttributionSource attributionSource);
    void unregisterSync(in IPeriodicAdvertisingCallback callback, in AttributionSource attributionSource);
    void transferSync(in BluetoothDevice bda, in int serviceData, in int syncHandle, in AttributionSource attributionSource);
    void transferSetInfo(in BluetoothDevice bda, in int serviceData, in int advertisingHandle, in IPeriodicAdvertisingCallback callback,  in AttributionSource attributionSource);

    void registerClient(in ParcelUuid appId, in IBluetoothGattCallback callback, boolean eatt_support, in AttributionSource attributionSource);

    void unregisterClient(in int clientIf, in AttributionSource attributionSource);
    void clientConnect(in int clientIf, in String address, in int addressType, in boolean isDirect, in int transport, in boolean opportunistic, in int phy, in AttributionSource attributionSource);
    void clientDisconnect(in int clientIf, in String address, in AttributionSource attributionSource);
    void clientSetPreferredPhy(in int clientIf, in String address, in int txPhy, in int rxPhy, in int phyOptions, in AttributionSource attributionSource);
    void clientReadPhy(in int clientIf, in String address, in AttributionSource attributionSources);
    void refreshDevice(in int clientIf, in String address, in AttributionSource attributionSource);
    void discoverServices(in int clientIf, in String address, in AttributionSource attributionSource);
    void discoverServiceByUuid(in int clientIf, in String address, in ParcelUuid uuid, in AttributionSource attributionSource);
    void readCharacteristic(in int clientIf, in String address, in int handle, in int authReq, in AttributionSource attributionSource);
    void readUsingCharacteristicUuid(in int clientIf, in String address, in ParcelUuid uuid,
                           in int startHandle, in int endHandle, in int authReq, in AttributionSource attributionSource);
    int writeCharacteristic(in int clientIf, in String address, in int handle,
                            in int writeType, in int authReq, in byte[] value, in AttributionSource attributionSource);
    void readDescriptor(in int clientIf, in String address, in int handle, in int authReq, in AttributionSource attributionSource);
    int writeDescriptor(in int clientIf, in String address, in int handle,
                            in int authReq, in byte[] value, in AttributionSource attributionSource);
    void registerForNotification(in int clientIf, in String address, in int handle, in boolean enable, in AttributionSource attributionSource);
    void beginReliableWrite(in int clientIf, in String address, in AttributionSource attributionSource);
    void endReliableWrite(in int clientIf, in String address, in boolean execute, in AttributionSource attributionSource);
    void readRemoteRssi(in int clientIf, in String address, in AttributionSource attributionSource);
    void configureMTU(in int clientIf, in String address, in int mtu, in AttributionSource attributionSource);
    void connectionParameterUpdate(in int clientIf, in String address, in int connectionPriority, in AttributionSource attributionSource);
    void leConnectionUpdate(int clientIf, String address, int minInterval,
                            int maxInterval, int peripheralLatency, int supervisionTimeout,
                            int minConnectionEventLen, int maxConnectionEventLen, in AttributionSource attributionSource);

    void registerServer(in ParcelUuid appId, in IBluetoothGattServerCallback callback, boolean eatt_support, in AttributionSource attributionSource);
    void unregisterServer(in int serverIf, in AttributionSource attributionSource);
    void serverConnect(in int serverIf, in String address, in int addressType, in boolean isDirect, in int transport, in AttributionSource attributionSource);
    void serverDisconnect(in int serverIf, in String address, in AttributionSource attributionSource);
    void serverSetPreferredPhy(in int clientIf, in String address, in int txPhy, in int rxPhy, in int phyOptions, in AttributionSource attributionSource);
    void serverReadPhy(in int clientIf, in String address, in AttributionSource attributionSource);
    void addService(in int serverIf, in BluetoothGattService service, in AttributionSource attributionSource);
    void removeService(in int serverIf, in int handle, in AttributionSource attributionSource);
    void clearServices(in int serverIf, in AttributionSource attributionSource);
    void sendResponse(in int serverIf, in String address, in int requestId,
                            in int status, in int offset, in byte[] value, in AttributionSource attributionSource);
    int sendNotification(in int serverIf, in String address, in int handle,
                            in boolean confirm, in byte[] value, in AttributionSource attributionSource);
    void disconnectAll(in AttributionSource attributionSource);
    int numHwTrackFiltersAvailable(in AttributionSource attributionSource);
    void leSubrateRequest(in int clientIf, in String address, in int subrateMin, in int subrateMax, in int maxLatency,
                          in int contNumber, in int supervisionTimeout, in AttributionSource attributionSource);
    void subrateModeRequest(in int clientIf, in String address, in int subrateMode, in AttributionSource attributionSource);
    List<DistanceMeasurementMethod> getSupportedDistanceMeasurementMethods(in AttributionSource attributionSource);
    void startDistanceMeasurement(in ParcelUuid uuid, in DistanceMeasurementParams params, in IDistanceMeasurementCallback callback,
                            in AttributionSource attributionSource);
    int stopDistanceMeasurement(in ParcelUuid uuid, in BluetoothDevice device, in int method, in AttributionSource attributionSource);
    int getChannelSoundingMaxSupportedSecurityLevel(in BluetoothDevice remoteDevice, in AttributionSource attributionSource);
    int getLocalChannelSoundingMaxSupportedSecurityLevel(in AttributionSource attributionSource);
}
