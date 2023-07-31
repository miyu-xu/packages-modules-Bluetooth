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
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.google.protobuf.Empty;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.stub.StreamObserver;

import pandora.HostGrpc;
import pandora.HostProto.AdvertiseRequest;
import pandora.HostProto.AdvertiseResponse;

@RunWith(AndroidJUnit4.class)
public class LeScanningTest {
    private static final String LOG_TAG = "LeScanningTest";
    private static final int TIMEOUT_SCANNING_MS = 10000;

    private static ManagedChannel mChannel;

    private static HostGrpc.HostBlockingStub mHostBlockingStub;

    private static HostGrpc.HostStub mHostStub;

    @BeforeClass
    public static void setUpClass() throws Exception {
        InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .adoptShellPermissionIdentity();
    }

    @Before
    public void setUp() throws Exception {
        // FactorReset is killing the server and restart
        // all channel created before the server restarted
        // cannot be reused
        ManagedChannel channel =
                OkHttpChannelBuilder.forAddress("localhost", 7999).usePlaintext().build();

        HostGrpc.HostBlockingStub stub = HostGrpc.newBlockingStub(channel);
        stub.factoryReset(Empty.getDefaultInstance());

        // terminate the channel
        channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);

        // Create a new channel for all successive grpc calls
        mChannel = OkHttpChannelBuilder.forAddress("localhost", 7999).usePlaintext().build();

        mHostBlockingStub = HostGrpc.newBlockingStub(mChannel);
        mHostStub = HostGrpc.newStub(mChannel);
        mHostBlockingStub.withWaitForReady().readLocalAddress(Empty.getDefaultInstance());
    }

    public void tearDown() throws Exception {
        // terminate the channel
        mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void startScan() {
        advertiseWithBumble();

        ScanResult result = startScanning().join();

        assertThat(result).isNotNull();
    }

    private CompletableFuture<ScanResult> startScanning() {
        CompletableFuture<ScanResult> future = new CompletableFuture<>();

        android.content.Context context = ApplicationProvider.getApplicationContext();
        BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        // Start scanning
        BluetoothLeScanner leScanner = bluetoothAdapter.getBluetoothLeScanner();

        ScanSettings scanSettings =
                new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .build();

        List<ScanFilter> scanFilters = new ArrayList<>();
        ScanFilter scanFilter = new ScanFilter.Builder().build();
        scanFilters.add(scanFilter);

        ScanCallback scanCallback =
                new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        Log.i(LOG_TAG, "onScanResult " + "callbackType: " + callbackType);
                        future.complete(result);
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        Log.i(LOG_TAG, "onScanFailed");
                        future.complete(null);
                    }
                };

        leScanner.startScan(scanFilters, scanSettings, scanCallback);
        Log.i(LOG_TAG, "startScanning");

        // Make sure completableFuture object completes with null after some timeout
        return future.completeOnTimeout(null, TIMEOUT_SCANNING_MS, TimeUnit.MILLISECONDS);
    }

    private void advertiseWithBumble() {
        AdvertiseRequest request = AdvertiseRequest.newBuilder().setLegacy(true).build();

        StreamObserver<AdvertiseResponse> responseObserver =
                new StreamObserver<>() {
                    @Override
                    public void onNext(AdvertiseResponse response) {
                        Log.i(LOG_TAG, "advertise observer: onNext");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(LOG_TAG, "advertise observer: on error " + e);
                    }

                    @Override
                    public void onCompleted() {
                        Log.i(LOG_TAG, "advertise observer: on completed");
                    }
                };

        mHostStub.advertise(request, responseObserver);
    }
}
