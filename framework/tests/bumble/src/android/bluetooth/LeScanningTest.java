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

import static android.bluetooth.Utils.createParams;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.app.PendingIntent;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import com.google.protobuf.ByteString;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import pandora.HostProto;
import pandora.HostProto.AdvertiseRequest;
import pandora.HostProto.AdvertiseResponse;
import pandora.HostProto.OwnAddressType;

@RunWith(Enclosed.class)
public class LeScanningTest {
    private static final String TAG = "LeScanningTest";
    private static final int TIMEOUT_SCANNING_MS = 2000;
    private static final int TIMEOUT_CONNECT_MS = 2000;
    private static final int TIMEOUT_BLE_TOGGLE_MS = 3000;
    private static final String TEST_UUID_STRING = "00001805-0000-1000-8000-00805f9b34fb";
    private static final String TEST_ADDRESS_RANDOM_STATIC = "F0:43:A8:23:10:11";
    private static final String TEST_ADDRESS_PUBLIC = "F0:43:A8:23:10:11";
    private static final String ACTION_DYNAMIC_RECEIVER_SCAN_RESULT =
            "android.bluetooth.test.ACTION_DYNAMIC_RECEIVER_SCAN_RESULT";

    public abstract static class TestBase {
        @Rule
        public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

        @Rule public final PandoraDevice mBumble = new PandoraDevice();

        final Context mContext = ApplicationProvider.getApplicationContext();
        final BluetoothManager mBluetoothManager =
                mContext.getSystemService(BluetoothManager.class);
        final BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
        final BluetoothLeScanner mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        void advertiseWithBumble(
                String serviceUuid, OwnAddressType addressType, boolean isConnectable) {
            AdvertiseRequest.Builder requestBuilder =
                    AdvertiseRequest.newBuilder()
                            .setOwnAddressType(addressType)
                            .setConnectable(isConnectable);

            if (serviceUuid != null) {
                HostProto.DataTypes.Builder dataTypeBuilder = HostProto.DataTypes.newBuilder();
                dataTypeBuilder.addCompleteServiceClassUuids128(serviceUuid);
                requestBuilder.setData(dataTypeBuilder.build());
            }

            advertiseWithBumble(requestBuilder);
        }

        void advertiseWithBumble(AdvertiseRequest.Builder requestBuilder) {
            // Bumble currently only supports legacy advertising.
            requestBuilder.setLegacy(true);
            // Collect and ignore responses.
            StreamObserverSpliterator<AdvertiseResponse> responseObserver =
                    new StreamObserverSpliterator<>();
            mBumble.host().advertise(requestBuilder.build(), responseObserver);
        }

        List<ScanResult> scanWithCallback(ScanFilter scanFilter, ScanSettings scanSettings) {
            CompletableFuture<List<ScanResult>> future = new CompletableFuture<>();
            List<ScanResult> scanResults = new ArrayList<>();

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

                            if (callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                                if (scanResults.size() < 2) {
                                    scanResults.add(result);
                                } else {
                                    future.complete(scanResults);
                                }
                            } else {
                                scanResults.add(result);
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
                    future.completeOnTimeout(null, TIMEOUT_SCANNING_MS, TimeUnit.MILLISECONDS)
                            .join();

            mLeScanner.stopScan(scanCallback);

            return result;
        }

        List<ScanResult> scanWithPendingIntent(
                ScanFilter scanFilter, ScanSettings scanSettings, int pendingIntentFlags) {
            CompletableFuture<List<ScanResult>> future = new CompletableFuture<>();
            List<ScanResult> scanResults = new ArrayList<>();
            BroadcastReceiver scanResultReceiver =
                    new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (ACTION_DYNAMIC_RECEIVER_SCAN_RESULT.equals(intent.getAction())) {
                                int callbackType =
                                        intent.getIntExtra(
                                                BluetoothLeScanner.EXTRA_CALLBACK_TYPE, -1);
                                List<ScanResult> results =
                                        intent.getParcelableArrayListExtra(
                                                BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT,
                                                ScanResult.class);

                                if (results == null) {
                                    Log.i(TAG, "onScanResult scanResults: null");
                                    return;
                                }

                                Log.i(
                                        TAG,
                                        "onScanResult "
                                                + "callbackType: "
                                                + callbackType
                                                + ", results: "
                                                + results);

                                if (callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                                    for (ScanResult result : results) {
                                        if (scanResults.size() < 2) {
                                            scanResults.add(result);
                                        } else {
                                            future.complete(scanResults);
                                        }
                                    }
                                } else {
                                    future.complete(results);
                                }
                            }
                        }
                    };

            IntentFilter intentFilter = new IntentFilter(ACTION_DYNAMIC_RECEIVER_SCAN_RESULT);
            mContext.registerReceiver(scanResultReceiver, intentFilter);

            Intent scanIntent = new Intent(ACTION_DYNAMIC_RECEIVER_SCAN_RESULT);
            PendingIntent pendingIntent =
                    PendingIntent.getBroadcast(mContext, 0, scanIntent, pendingIntentFlags);

            mLeScanner.startScan(List.of(scanFilter), scanSettings, pendingIntent);

            List<ScanResult> results =
                    future.completeOnTimeout(null, TIMEOUT_SCANNING_MS, TimeUnit.MILLISECONDS)
                            .join();

            mLeScanner.stopScan(pendingIntent);
            mContext.unregisterReceiver(scanResultReceiver);

            return results;
        }
    }

    @RunWith(AndroidJUnit4.class)
    public static class NonParameterizedTest extends TestBase {
        @Test
        public void startBleScan_withCallbackTypeAllMatches() {
            advertiseWithBumble(TEST_UUID_STRING, OwnAddressType.PUBLIC, false);

            ScanFilter scanFilter =
                    new ScanFilter.Builder()
                            .setServiceUuid(ParcelUuid.fromString(TEST_UUID_STRING))
                            .build();
            ScanSettings scanSettings =
                    new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                            .build();

            List<ScanResult> results = scanWithCallback(scanFilter, scanSettings);

            assertThat(results).isNotNull();
            assertThat(results.get(0).getScanRecord().getServiceUuids().get(0))
                    .isEqualTo(ParcelUuid.fromString(TEST_UUID_STRING));
            assertThat(results.get(1).getScanRecord().getServiceUuids().get(0))
                    .isEqualTo(ParcelUuid.fromString(TEST_UUID_STRING));
        }

        @Test
        public void scanForIrkIdentityAddress_withCallbackTypeAllMatches() {
            advertiseWithBumble(null, OwnAddressType.RANDOM, false);

            ScanFilter scanFilter =
                    new ScanFilter.Builder()
                            .setDeviceAddress(
                                    TEST_ADDRESS_RANDOM_STATIC,
                                    BluetoothDevice.ADDRESS_TYPE_RANDOM,
                                    Utils.BUMBLE_IRK)
                            .build();
            ScanSettings scanSettings =
                    new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                            .build();

            List<ScanResult> results = scanWithCallback(scanFilter, scanSettings);

            assertThat(results).isNotEmpty();
            assertThat(results.get(0).getDevice().getAddress())
                    .isEqualTo(TEST_ADDRESS_RANDOM_STATIC);
        }

        @Test
        public void startBleScan_withCallbackTypeFirstMatchSilentlyFails() {
            advertiseWithBumble(TEST_UUID_STRING, OwnAddressType.PUBLIC, false);

            ScanSettings scanSettings =
                    new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                            .build();

            ScanFilter scanFilter =
                    new ScanFilter.Builder()
                            .setServiceUuid(ParcelUuid.fromString(TEST_UUID_STRING))
                            .build();

            ScanCallback mockScanCallback = mock(ScanCallback.class);

            mLeScanner.startScan(List.of(scanFilter), scanSettings, mockScanCallback);
            verify(mockScanCallback, after(TIMEOUT_SCANNING_MS).never()).onScanFailed(anyInt());
            mLeScanner.stopScan(mockScanCallback);
        }

        @Test
        public void startBleScan_withCallbackTypeMatchLostSilentlyFails() {
            advertiseWithBumble(TEST_UUID_STRING, OwnAddressType.PUBLIC, false);

            ScanSettings scanSettings =
                    new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .setCallbackType(ScanSettings.CALLBACK_TYPE_MATCH_LOST)
                            .build();

            ScanFilter scanFilter =
                    new ScanFilter.Builder()
                            .setServiceUuid(ParcelUuid.fromString(TEST_UUID_STRING))
                            .build();

            ScanCallback mockScanCallback = mock(ScanCallback.class);

            mLeScanner.startScan(List.of(scanFilter), scanSettings, mockScanCallback);
            verify(mockScanCallback, after(TIMEOUT_SCANNING_MS).never()).onScanFailed(anyInt());
            mLeScanner.stopScan(mockScanCallback);
        }

        @Test
        public void startBleScan_withPendingIntentAndDynamicReceiverAndCallbackTypeAllMatches() {
            BroadcastReceiver mockReceiver = mock(BroadcastReceiver.class);
            IntentFilter intentFilter = new IntentFilter(ACTION_DYNAMIC_RECEIVER_SCAN_RESULT);
            mContext.registerReceiver(mockReceiver, intentFilter);

            advertiseWithBumble(TEST_UUID_STRING, OwnAddressType.PUBLIC, false);

            ScanSettings scanSettings =
                    new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                            .build();

            ScanFilter scanFilter =
                    new ScanFilter.Builder()
                            .setServiceUuid(ParcelUuid.fromString(TEST_UUID_STRING))
                            .build();

            // NOTE: Intent.setClass() must not be called, or else scan results won't be received.
            Intent scanIntent = new Intent(ACTION_DYNAMIC_RECEIVER_SCAN_RESULT);
            PendingIntent pendingIntent =
                    PendingIntent.getBroadcast(
                            mContext, 0, scanIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            mLeScanner.startScan(List.of(scanFilter), scanSettings, pendingIntent);

            ArgumentCaptor<Intent> intent = ArgumentCaptor.forClass(Intent.class);
            verify(mockReceiver, timeout(TIMEOUT_SCANNING_MS)).onReceive(any(), intent.capture());

            mLeScanner.stopScan(pendingIntent);
            mContext.unregisterReceiver(mockReceiver);

            assertThat(intent.getValue().getAction())
                    .isEqualTo(ACTION_DYNAMIC_RECEIVER_SCAN_RESULT);
            assertThat(intent.getValue().getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, -1))
                    .isEqualTo(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);

            List<ScanResult> results =
                    intent.getValue()
                            .getParcelableArrayListExtra(
                                    BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT, ScanResult.class);
            assertThat(results).isNotEmpty();
            assertThat(results.get(0).getScanRecord().getServiceUuids()).isNotEmpty();
            assertThat(results.get(0).getScanRecord().getServiceUuids().get(0))
                    .isEqualTo(ParcelUuid.fromString(TEST_UUID_STRING));
            assertThat(results.get(0).getScanRecord().getServiceUuids())
                    .containsExactly(ParcelUuid.fromString(TEST_UUID_STRING));
        }

        @Test
        public void startBleScan_withPendingIntentAndStaticReceiverAndCallbackTypeAllMatches() {
            advertiseWithBumble(TEST_UUID_STRING, OwnAddressType.PUBLIC, false);

            ScanSettings scanSettings =
                    new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                            .build();

            ArrayList<ScanFilter> scanFilters = new ArrayList<>();
            ScanFilter scanFilter =
                    new ScanFilter.Builder()
                            .setServiceUuid(ParcelUuid.fromString(TEST_UUID_STRING))
                            .build();
            scanFilters.add(scanFilter);

            PendingIntent pendingIntent =
                    PendingIntentScanReceiver.newBroadcastPendingIntent(mContext, 0);

            mLeScanner.startScan(scanFilters, scanSettings, pendingIntent);
            List<ScanResult> results =
                    PendingIntentScanReceiver.nextScanResult()
                            .completeOnTimeout(null, TIMEOUT_SCANNING_MS, TimeUnit.MILLISECONDS)
                            .join();
            mLeScanner.stopScan(pendingIntent);
            PendingIntentScanReceiver.resetNextScanResultFuture();

            assertThat(results).isNotEmpty();
            assertThat(results.get(0).getScanRecord().getServiceUuids()).isNotEmpty();
            assertThat(results.get(0).getScanRecord().getServiceUuids())
                    .containsExactly(ParcelUuid.fromString(TEST_UUID_STRING));
        }

        @Test
        public void startBleScan_oneTooManyScansFails() {
            final int maxNumScans = 32;
            advertiseWithBumble(TEST_UUID_STRING, OwnAddressType.PUBLIC, false);

            ScanSettings scanSettings =
                    new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                            .build();

            ScanFilter scanFilter =
                    new ScanFilter.Builder()
                            .setServiceUuid(ParcelUuid.fromString(TEST_UUID_STRING))
                            .build();

            List<ScanCallback> scanCallbacks =
                    Stream.generate(() -> mock(ScanCallback.class)).limit(maxNumScans).toList();
            for (ScanCallback mockScanCallback : scanCallbacks) {
                mLeScanner.startScan(List.of(scanFilter), scanSettings, mockScanCallback);
            }
            // This last scan should fail
            ScanCallback lastMockScanCallback = mock(ScanCallback.class);
            mLeScanner.startScan(List.of(scanFilter), scanSettings, lastMockScanCallback);

            // We expect an error only for the last scan, which was over the maximum active scans
            // limit.
            for (ScanCallback mockScanCallback : scanCallbacks) {
                verify(mockScanCallback, timeout(TIMEOUT_SCANNING_MS).atLeast(1))
                        .onScanResult(eq(ScanSettings.CALLBACK_TYPE_ALL_MATCHES), any());
                verify(mockScanCallback, never()).onScanFailed(anyInt());
                mLeScanner.stopScan(mockScanCallback);
            }
            verify(lastMockScanCallback, timeout(TIMEOUT_SCANNING_MS))
                    .onScanFailed(eq(ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED));
            mLeScanner.stopScan(lastMockScanCallback);
        }

        @Test
        public void startBleScan_withNonConnectablePublicAdvertisement() {
            advertiseWithBumble(null, OwnAddressType.PUBLIC, false);

            ScanFilter scanFilter =
                    new ScanFilter.Builder()
                            .setDeviceAddress(mBumble.getRemoteDevice().getAddress())
                            .build();
            ScanSettings scanSettings =
                    new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                            .build();

            List<ScanResult> results = scanWithCallback(scanFilter, scanSettings);

            assertThat(results).isNotNull();
            assertThat(results.get(0).isConnectable()).isFalse();
            assertThat(results.get(1).isConnectable()).isFalse();
        }

        @Test
        public void startBleScan_withNonConnectableScannablePublicAdvertisement() {
            byte[] payload = {0x02, 0x03};
            // first 2 bytes are the manufacturer ID 0x00E0 (Google) in little endian
            byte[] manufacturerData = {(byte) 0xE0, 0x00, payload[0], payload[1]};
            HostProto.DataTypes.Builder scanResponse =
                    HostProto.DataTypes.newBuilder()
                            .setManufacturerSpecificData(ByteString.copyFrom(manufacturerData));

            AdvertiseRequest.Builder requestBuilder =
                    AdvertiseRequest.newBuilder()
                            .setConnectable(false)
                            .setOwnAddressType(OwnAddressType.PUBLIC)
                            .setScanResponseData(scanResponse);
            advertiseWithBumble(requestBuilder);

            ScanFilter scanFilter =
                    new ScanFilter.Builder()
                            .setDeviceAddress(mBumble.getRemoteDevice().getAddress())
                            .build();
            ScanSettings scanSettings =
                    new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                            .build();

            List<ScanResult> results = scanWithCallback(scanFilter, scanSettings);

            assertThat(results).isNotNull();
            assertThat(results.get(0).isConnectable()).isFalse();
            assertThat(results.get(0).getScanRecord().getManufacturerSpecificData(0x00E0))
                    .isEqualTo(payload);
        }
    }

    @RunWith(Parameterized.class)
    public static class ParameterizedTest extends TestBase {
        @Parameter(0)
        public boolean isBleToggled;

        @Parameter(1)
        public boolean isRemoteIdentityAddressRandom;

        @Parameter(2)
        public boolean isRemoteAdvertisingWithUuid;

        @Parameter(3)
        public boolean isRemoteConnected;

        @Parameter(4)
        public int scanMode;

        @Parameter(5)
        public int matchMode;

        /** Parameters for various configurations of tests */
        @Parameters(
                name =
                        "{index}: isBleToggled = {0}, isRemoteIdentityAddressRandom = {1},"
                                + " isRemoteAdvertisingWithUuid = {2}, isRemoteConnected = {3},"
                                + " scanMode = {4}, matchMode = {5}")
        public static Collection<Object[]> parameters() {
            Object[] booleanVariations = {true, false};
            Object[] scanModeVariations = {
                ScanSettings.SCAN_MODE_AMBIENT_DISCOVERY, ScanSettings.SCAN_MODE_LOW_POWER
            };
            Object[] matchModeVariations = {
                ScanSettings.MATCH_MODE_STICKY, ScanSettings.MATCH_MODE_AGGRESSIVE
            };

            // TODO(315852141): Include variations for LE only Bumble when supported
            // TODO(315852141): Include variations for two advertisements at the same time
            // TODO(303502437): Include variations for callback type when supported in rootcanal
            List<Object[]> paramVariations = new ArrayList<>();
            paramVariations.add(/* isBleToggled */ booleanVariations);
            paramVariations.add(/* isRemoteIdentityAddressRandom */ booleanVariations);
            paramVariations.add(/* isRemoteAdvertisingWithUuid */ booleanVariations);
            paramVariations.add(/* isRemoteConnected */ booleanVariations);
            paramVariations.add(scanModeVariations);
            paramVariations.add(matchModeVariations);

            return createParams(paramVariations);
        }

        /**
         * Scan with Identity Address & IRK
         *
         * <p>See
         * https://docs.google.com/document/d/1oQOpgI83HSJBdr5mBU00za_6XrDGo2KDGnCcX-hXPHk/edit?pli=1&tab=t.0#heading=h.yl9o93gnbyuy
         */
        @Test
        public void scanForIrkAndAddress() {
            // Arrange
            if (isBleToggled) {
                toggleBluetooth();
            }

            String uuid = isRemoteAdvertisingWithUuid ? TEST_UUID_STRING : null;
            advertiseWithBumble(uuid, OwnAddressType.RANDOM, isRemoteConnected);

            BluetoothGatt bumbleGatt = null;
            if (isRemoteConnected) {
                bumbleGatt = connectGatt();
            }

            // TODO(316001793): Retrieve identity address from Bumble
            // TODO(315852141): Use supported Bumble for the given address type
            ScanFilter scanFilter =
                    new ScanFilter.Builder()
                            .setDeviceAddress(
                                    isRemoteIdentityAddressRandom
                                            ? TEST_ADDRESS_RANDOM_STATIC
                                            : TEST_ADDRESS_PUBLIC,
                                    isRemoteIdentityAddressRandom
                                            ? BluetoothDevice.ADDRESS_TYPE_RANDOM
                                            : BluetoothDevice.ADDRESS_TYPE_PUBLIC,
                                    Utils.BUMBLE_IRK)
                            .build();
            ScanSettings scanSettings =
                    new ScanSettings.Builder()
                            .setScanMode(scanMode)
                            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                            .setMatchMode(matchMode)
                            .build();

            // Act
            List<ScanResult> results;
            results =
                    scanWithPendingIntent(
                            scanFilter,
                            scanSettings,
                            PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            // Cleanup
            if (isRemoteConnected && bumbleGatt != null) {
                bumbleGatt.disconnect();
            }

            // Assert
            assertThat(results).isNotEmpty();
            assertThat(results.get(0).getDevice().getAddress())
                    .isEqualTo(TEST_ADDRESS_RANDOM_STATIC);
        }

        private void toggleBluetooth() {
            CompletableFuture<Boolean> disableFuture = new CompletableFuture<>();
            CompletableFuture<Boolean> enableFuture = new CompletableFuture<>();
            BroadcastReceiver bluetoothAdapterStateReceiver =
                    new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                                int prevState =
                                        intent.getIntExtra(
                                                BluetoothAdapter.EXTRA_PREVIOUS_STATE,
                                                BluetoothAdapter.ERROR);
                                int currState =
                                        intent.getIntExtra(
                                                BluetoothAdapter.EXTRA_STATE,
                                                BluetoothAdapter.ERROR);

                                Log.i(
                                        TAG,
                                        "Bluetooth state changed from "
                                                + prevState
                                                + " to "
                                                + currState);

                                if (currState == BluetoothAdapter.STATE_OFF) {
                                    disableFuture.complete(true);
                                } else if (currState == BluetoothAdapter.STATE_ON) {
                                    enableFuture.complete(true);
                                }
                            }
                        }
                    };

            IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            mContext.registerReceiver(bluetoothAdapterStateReceiver, intentFilter);

            boolean toggled;

            // Disable Bluetooth
            mBluetoothAdapter.disable();
            toggled =
                    disableFuture
                            .completeOnTimeout(false, TIMEOUT_BLE_TOGGLE_MS, TimeUnit.MILLISECONDS)
                            .join();

            if (toggled) {
                // Enable Bluetooth
                mBluetoothAdapter.enable();
                toggled =
                        enableFuture
                                .completeOnTimeout(
                                        false, TIMEOUT_BLE_TOGGLE_MS, TimeUnit.MILLISECONDS)
                                .join();
            }

            mContext.unregisterReceiver(bluetoothAdapterStateReceiver);

            if (!toggled) {
                throw new IllegalStateException("Bluetooth could not be toggled!");
            }
        }

        private BluetoothGatt connectGatt() {
            BluetoothGattCallback gattCallback = mock(BluetoothGattCallback.class);
            BluetoothDevice bumbleDevice =
                    mBluetoothAdapter.getRemoteLeDevice(
                            Utils.BUMBLE_RANDOM_ADDRESS, BluetoothDevice.ADDRESS_TYPE_RANDOM);

            BluetoothGatt gatt = bumbleDevice.connectGatt(mContext, false, gattCallback);

            verify(gattCallback, timeout(TIMEOUT_CONNECT_MS))
                    .onConnectionStateChange(
                            any(),
                            eq(BluetoothGatt.GATT_SUCCESS),
                            eq(BluetoothProfile.STATE_CONNECTED));

            return gatt;
        }
    }
}
