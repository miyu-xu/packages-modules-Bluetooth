/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import io.grpc.stub.StreamObserver;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import pandora.HostProto;
import pandora.HostProto.AdvertiseRequest;
import pandora.HostProto.AdvertiseResponse;

@RunWith(AndroidJUnit4.class)
public class GattClientTest {
    private static final String TAG = "GattClientTest";

    private static final String TEST_UUID_STRING = "00001805-0000-1000-8000-00805f9b34fb";

    private static final int TIMEOUT_SCANNING_MS = 2000;

    @Rule public final PandoraDevice mBumble = new PandoraDevice();

    private final SynchronousQueue<Integer> mConnectionEventQueue = new SynchronousQueue<Integer>();

    @BeforeClass
    public static void setUpClass() throws Exception {
        InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .adoptShellPermissionIdentity();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .dropShellPermissionIdentity();
    }

    @Test
    public void directConnectGattAfterClose() throws Exception {
        advertiseWithBumble(TEST_UUID_STRING);

        List<ScanResult> results =
                startScanning(TEST_UUID_STRING, ScanSettings.CALLBACK_TYPE_ALL_MATCHES).join();

        BluetoothDevice device = results.get(0).getDevice();
        assertThat(device).isNotNull();

        BluetoothGatt gatt;

        for (int i = 0; i < 100; i++) {
            Log.d(TAG, "directConnectGattAfterClose, iteration: " + i);
            gatt =
                    device.connectGatt(
                            ApplicationProvider.getApplicationContext(), false, mGattCallback);
            gatt.close();

            gatt =
                    device.connectGatt(
                            ApplicationProvider.getApplicationContext(), false, mGattCallback);
            assertThat(waitForConnectionEvent()).isEqualTo(BluetoothProfile.STATE_CONNECTED);
            gatt.close();
        }
    }

    @Test
    public void fullGattClientLifecycle() throws Exception {
        advertiseWithBumble(TEST_UUID_STRING);

        List<ScanResult> results =
                startScanning(TEST_UUID_STRING, ScanSettings.CALLBACK_TYPE_ALL_MATCHES).join();

        BluetoothDevice device = results.get(0).getDevice();
        assertThat(device).isNotNull();

        BluetoothGatt gatt;

        for (int i = 0; i < 100; i++) {
            Log.d(TAG, "fullGattClientLifecycle, iteration: " + i);
            gatt =
                    device.connectGatt(
                            ApplicationProvider.getApplicationContext(), false, mGattCallback);
            assertThat(waitForConnectionEvent()).isEqualTo(BluetoothProfile.STATE_CONNECTED);

            gatt.disconnect();
            assertThat(waitForConnectionEvent()).isEqualTo(BluetoothProfile.STATE_DISCONNECTED);

            gatt.close();
        }
    }

    private Integer waitForConnectionEvent() throws InterruptedException {
        return mConnectionEventQueue.poll(1, TimeUnit.SECONDS);
    }

    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    Log.i(
                            TAG,
                            "onConnectionStateChange, status: "
                                    + status
                                    + " newState: "
                                    + newState);
                    try {
                        if (!mConnectionEventQueue.offer(newState, 1, TimeUnit.SECONDS)) {
                            Log.e(TAG, "Failed to offer connection event to synchronous queue");
                        }
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Interrupted while handling connection state change");
                    }
                }
            };

    private CompletableFuture<List<ScanResult>> startScanning(
            String serviceUuid, int callbackType) {
        CompletableFuture<List<ScanResult>> future = new CompletableFuture<>();
        List<ScanResult> scanResults = new ArrayList<>();

        android.content.Context context = ApplicationProvider.getApplicationContext();
        BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        // Start scanning
        BluetoothLeScanner leScanner = bluetoothAdapter.getBluetoothLeScanner();

        ScanSettings scanSettings =
                new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setCallbackType(callbackType)
                        .build();

        List<ScanFilter> scanFilters = new ArrayList<>();
        ScanFilter scanFilter =
                new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(serviceUuid)).build();
        scanFilters.add(scanFilter);

        ScanCallback scanCallback =
                new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        Log.i(
                                TAG,
                                "onScanResult "
                                        + "callbackType: "
                                        + callbackType
                                        + ", service uuids: "
                                        + result.getScanRecord().getServiceUuids());
                        scanResults.add(result);
                        future.complete(scanResults);
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        Log.i(TAG, "onScanFailed " + "errorCode: " + errorCode);
                        future.complete(null);
                    }
                };

        leScanner.startScan(scanFilters, scanSettings, scanCallback);

        // Make sure completableFuture object completes with null after some timeout
        return future.completeOnTimeout(null, TIMEOUT_SCANNING_MS, TimeUnit.MILLISECONDS);
    }

    private void advertiseWithBumble(String serviceUuid) {
        HostProto.DataTypes dataType =
                HostProto.DataTypes.newBuilder()
                        .addCompleteServiceClassUuids128(serviceUuid)
                        .build();

        AdvertiseRequest request =
                AdvertiseRequest.newBuilder()
                        .setLegacy(true)
                        .setData(dataType)
                        .setConnectable(true)
                        .build();

        StreamObserver<AdvertiseResponse> responseObserver =
                new StreamObserver<>() {
                    @Override
                    public void onNext(AdvertiseResponse response) {
                        Log.i(TAG, "advertise observer: onNext");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "advertise observer: on error " + e);
                    }

                    @Override
                    public void onCompleted() {
                        Log.i(TAG, "advertise observer: on completed");
                    }
                };

        mBumble.host().advertise(request, responseObserver);
    }
}
