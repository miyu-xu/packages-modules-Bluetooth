/*
 * Copyright 2023 The Android Open Source Project
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

import io.grpc.Context.CancellableContext;
import io.grpc.Deadline;
import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.stub.StreamObserver;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import pandora.HostGrpc;
import pandora.HostProto.AdvertiseRequest;
import pandora.HostProto.AdvertiseResponse;
import pandora.HostProto.DataTypes;

/** Test cases for {@BluetoothLeScanner} */
@RunWith(AndroidJUnit4.class)
public class LeScannerTest {

    private static final String LOG_TAG = "LeScannerTest";

    private static final int TIMEOUT_ADVERTISING_MS = 1000;
    private static final int TIMEOUT_SCANNING_MS = 3000;

    private static final String CCC_UUID_16 = "FFF5";
    private static final String CCC_UUID_128 = "5810bbc0-b499-11e9-a2a3-2a2ae2dbccce4";

    private ManagedChannel mChannel;

    private HostGrpc.HostBlockingStub mHostBlockingStub;

    private HostGrpc.HostStub mHostStub;

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

    @After
    public void tearDown() throws Exception {
        // terminate the channel
        mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void irkScan() throws Exception {
        advertiseWithBumble();

        List<ScanResult> results = startScanning().join();
    }

    private CompletableFuture<List<ScanResult>> startScanning() {
        CompletableFuture<List<ScanResult>> future = new CompletableFuture<>();
        List<ScanResult> scanResults = new ArrayList<>();

        android.content.Context context = ApplicationProvider.getApplicationContext();
        BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();

        // TODO: Adjust scan settings to match IRK scan flow DCK uses
        ScanSettings scanSettings =
                new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .build();

        List<ScanFilter> scanFilterList = new ArrayList<>();
        // TODO: Adjust scan filters
        ScanFilter scanFilter = new ScanFilter.Builder().build();
        scanFilterList.add(scanFilter);

        ScanCallback scanCallback =
                new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        Log.i(LOG_TAG, "onScanResult: " + result);
                        scanResults.add(result);
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        Log.e(LOG_TAG, "onScanFailed: " + errorCode);
                        future.complete(null);
                    }
                };

        scanner.startScan(scanFilterList, scanSettings, scanCallback);

        return future.completeOnTimeout(null, TIMEOUT_SCANNING_MS, TimeUnit.MILLISECONDS);
    }

    private CompletableFuture<AdvertiseResponse> advertiseWithBumble() {
        final CompletableFuture<AdvertiseResponse> future =
                new CompletableFuture<AdvertiseResponse>();
        CancellableContext withCancellation = io.grpc.Context.current().withCancellation();

        // TODO: Need to set IRK and address on Bumble device
        DataTypes advertisingData =
                DataTypes.newBuilder()
                        .addCompleteServiceClassUuids16(CCC_UUID_16)
                        .addCompleteServiceClassUuids128(CCC_UUID_128)
                        .build();
        AdvertiseRequest request =
                AdvertiseRequest.newBuilder()
                        .setLegacy(true)
                        .setConnectable(true)
                        .setData(advertisingData)
                        .build();
        StreamObserver<AdvertiseResponse> responseObserver =
                new StreamObserver<AdvertiseResponse>() {
                    public void onNext(AdvertiseResponse response) {
                        future.complete(response);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(LOG_TAG, "Advertise observer: onError: " + e);
                        future.completeExceptionally(e);
                    }

                    @Override
                    public void onCompleted() {
                        Log.i(LOG_TAG, "Advertise observer: onCompleted");
                        future.complete(null);
                    }
                };

        Deadline initialDeadline = Deadline.after(TIMEOUT_ADVERTISING_MS, TimeUnit.MILLISECONDS);
        withCancellation.run(
                () -> mHostStub.withDeadline(initialDeadline).advertise(request, responseObserver));

        return future.whenComplete(
                (input, exception) -> {
                    withCancellation.cancel(null);
                });
    }
}
