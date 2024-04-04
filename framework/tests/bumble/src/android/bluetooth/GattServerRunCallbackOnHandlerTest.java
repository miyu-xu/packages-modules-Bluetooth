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

import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.flags.Flags;
import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import pandora.HostProto.AdvertiseRequest;
import pandora.HostProto.OwnAddressType;

@RunWith(AndroidJUnit4.class)
public class GattServerRunCallbackOnHandlerTest {
    private static final String TAG = "GattServerRunCallbackOnHandlerTest";

    private static final int TIMEOUT_SCANNING_MS = 2_000;
    private static final int TIMEOUT_GATT_CONNECTION_MS = 2_000;

    @Rule public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule public final PandoraDevice mBumble = new PandoraDevice();

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final BluetoothManager mBluetoothManager =
            mContext.getSystemService(BluetoothManager.class);
    private final BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
    private final BluetoothLeScanner mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

    private final BluetoothDevice mRandomAddressDevice =
            mBluetoothAdapter.getRemoteLeDevice(
                    Utils.BUMBLE_RANDOM_ADDRESS, BluetoothDevice.ADDRESS_TYPE_RANDOM);

    @Before
    public void setUp() {
        advertiseWithBumble(OwnAddressType.RANDOM);
        assertThat(scanBumbleDevice(Utils.BUMBLE_RANDOM_ADDRESS)).isNotNull();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_GATT_SERVER_ADD_HANDLER_TO_RUN_CALLBACKS_ON)
    public void callbackRunsOnMainThread_whenMainHandlerIsProvided() throws Exception {
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        CompletableFuture<Thread> callbackThreadFuture = new CompletableFuture<>();
        BluetoothGattServerCallback gattServerCallback =
                new BluetoothGattServerCallback() {
                    @Override
                    public void onConnectionStateChange(
                            BluetoothDevice device, int status, int newState) {
                        callbackThreadFuture.complete(Thread.currentThread());
                    }
                };

        BluetoothGattServer gattServer =
                mBluetoothManager.openGattServer(
                        mContext,
                        gattServerCallback,
                        BluetoothDevice.TRANSPORT_AUTO,
                        false,
                        mainThreadHandler);

        try {
            gattServer.connect(mRandomAddressDevice, false);

            Thread callbackThread =
                    callbackThreadFuture
                            .completeOnTimeout(
                                    null, TIMEOUT_GATT_CONNECTION_MS, TimeUnit.MILLISECONDS)
                            .join();
            assertThat(callbackThread).isEqualTo(Looper.getMainLooper().getThread());
        } finally {
            gattServer.close();
        }
    }

    @Test
    public void callbackRunsOnBinderThread_whenNoHandlerIsProvided() throws Exception {
        CompletableFuture<Integer> callingUidFuture = new CompletableFuture<>();
        BluetoothGattServerCallback gattServerCallback =
                new BluetoothGattServerCallback() {
                    @Override
                    public void onConnectionStateChange(
                            BluetoothDevice device, int status, int newState) {
                        callingUidFuture.complete(Binder.getCallingUid());
                    }
                };

        BluetoothGattServer gattServer =
                mBluetoothManager.openGattServer(
                        mContext,
                        gattServerCallback,
                        BluetoothDevice.TRANSPORT_AUTO,
                        false,
                        null /* handler */);

        try {
            gattServer.connect(mRandomAddressDevice, false);

            Integer callingUid =
                    callingUidFuture
                            .completeOnTimeout(
                                    null, TIMEOUT_GATT_CONNECTION_MS, TimeUnit.MILLISECONDS)
                            .join();
            assertThat(callingUid).isEqualTo(Process.BLUETOOTH_UID);
        } finally {
            gattServer.close();
        }
    }

    private void advertiseWithBumble(OwnAddressType ownAddressType) {
        AdvertiseRequest request =
                AdvertiseRequest.newBuilder()
                        .setLegacy(true)
                        .setConnectable(true)
                        .setOwnAddressType(ownAddressType)
                        .build();
        mBumble.hostBlocking().advertise(request);
    }

    private List<ScanResult> scanBumbleDevice(String address) {
        CompletableFuture<List<ScanResult>> future = new CompletableFuture<>();
        ScanSettings scanSettings =
                new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .build();

        ScanFilter scanFilter = new ScanFilter.Builder().setDeviceAddress(address).build();

        ScanCallback scanCallback =
                new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        Log.d(TAG, "onScanResult: result=" + result);
                        future.complete(List.of(result));
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        Log.d(TAG, "onScanFailed: errorCode=" + errorCode);
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
