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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.test_utils.BlockingBluetoothAdapter;
import android.bluetooth.test_utils.TestUtils;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import com.google.testing.junit.testparameterinjector.TestParameterInjector;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import pandora.HostProto;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RunWith(TestParameterInjector.class)
public class BleOnTest {
    private static final String TAG = "BleOnTest";
    private static final int TIMEOUT_SCANNING_MS = 2000;
    private static final String TEST_UUID_STRING = "00001805-0000-1000-8000-00805f9b34fb";

    @Rule(order = 0)
    public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule(order = 1)
    public final PandoraDevice mBumble = new PandoraDevice();

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final BluetoothManager mBluetoothManager =
            mContext.getSystemService(BluetoothManager.class);
    private final BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
    private final BluetoothLeScanner mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

    private boolean mWasBluetoothAdapterEnabled = true;

    @Before
    public void setUp() {
        assumeTrue(TestUtils.hasBluetooth());
        mWasBluetoothAdapterEnabled = mBluetoothAdapter.isEnabled();
        if (mWasBluetoothAdapterEnabled) {
            assertThat(BlockingBluetoothAdapter.disable(true)).isTrue();
        }
        assertThat(BlockingBluetoothAdapter.enableBLE(true)).isTrue();
    }

    @After
    public void tearDown() {
        assumeTrue(TestUtils.hasBluetooth());
        assertThat(BlockingBluetoothAdapter.disableBLE()).isTrue();
        if (mWasBluetoothAdapterEnabled) {
            assertThat(BlockingBluetoothAdapter.enable()).isTrue();
        }
    }

    @Test
    public void confirm_StateIsBleOn() {
        assertThat(mBluetoothAdapter.isEnabled()).isFalse();
        assertThat(mBluetoothAdapter.isLeEnabled()).isTrue();
    }

    @Test
    public void confirm_ScanWorks() {
        advertiseWithBumble(TEST_UUID_STRING, HostProto.OwnAddressType.PUBLIC);

        ScanFilter scanFilter =
                new ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid.fromString(TEST_UUID_STRING))
                        .build();

        List<ScanResult> results =
                startScanning(
                        scanFilter, ScanSettings.CALLBACK_TYPE_ALL_MATCHES, /* isLegacy= */ true);

        assertThat(results).isNotNull();
        assertThat(results.get(0).getScanRecord().getServiceUuids().get(0))
                .isEqualTo(ParcelUuid.fromString(TEST_UUID_STRING));
        assertThat(results.get(1).getScanRecord().getServiceUuids().get(0))
                .isEqualTo(ParcelUuid.fromString(TEST_UUID_STRING));
    }

    private void advertiseWithBumble(String serviceUuid, HostProto.OwnAddressType addressType) {
        HostProto.AdvertiseRequest.Builder requestBuilder =
                HostProto.AdvertiseRequest.newBuilder().setOwnAddressType(addressType);

        if (serviceUuid != null) {
            HostProto.DataTypes.Builder dataTypeBuilder = HostProto.DataTypes.newBuilder();
            dataTypeBuilder.addCompleteServiceClassUuids128(serviceUuid);
            requestBuilder.setData(dataTypeBuilder.build());
        }

        advertiseWithBumble(requestBuilder, true);
    }

    private void advertiseWithBumble(
            HostProto.AdvertiseRequest.Builder requestBuilder, boolean isLegacy) {
        requestBuilder.setLegacy(isLegacy);
        // Collect and ignore responses.
        StreamObserverSpliterator<HostProto.AdvertiseResponse> responseObserver =
                new StreamObserverSpliterator<>();
        mBumble.host().advertise(requestBuilder.build(), responseObserver);
    }

    private List<ScanResult> startScanning(
            ScanFilter scanFilter, int callbackType, boolean isLegacy) {
        CompletableFuture<List<ScanResult>> future = new CompletableFuture<>();
        List<ScanResult> scanResults = new ArrayList<>();

        ScanSettings scanSettings =
                new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setCallbackType(callbackType)
                        .setLegacy(isLegacy)
                        .build();

        ScanCallback scanCallback =
                new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        Log.i(
                                TAG,
                                "onScanResult "
                                        + "address: "
                                        + result.getDevice().getAddress()
                                        + ", connectable: "
                                        + result.isConnectable()
                                        + ", callbackType: "
                                        + callbackType
                                        + ", service uuids: "
                                        + result.getScanRecord().getServiceUuids());

                        scanResults.add(result);
                        if (callbackType != ScanSettings.CALLBACK_TYPE_ALL_MATCHES
                                || scanResults.size() > 1) {
                            future.complete(scanResults);
                        }
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        Log.i(TAG, "onScanFailed " + "errorCode: " + errorCode);
                        future.complete(null);
                    }
                };

        mLeScanner.startScan(List.of(scanFilter), scanSettings, scanCallback);

        List<ScanResult> result =
                future.completeOnTimeout(null, TIMEOUT_SCANNING_MS, TimeUnit.MILLISECONDS).join();

        mLeScanner.stopScan(scanCallback);

        return result;
    }
}
