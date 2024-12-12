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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import com.android.bluetooth.flags.Flags;
import com.android.compatibility.common.util.AdoptShellPermissionsRule;

import com.google.common.util.concurrent.SettableFuture;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import pandora.HostProto.AdvertiseRequest;
import pandora.HostProto.AdvertiseResponse;
import pandora.HostProto.ConnectabilityMode;
import pandora.HostProto.DataTypes;
import pandora.HostProto.DiscoverabilityMode;
import pandora.HostProto.OwnAddressType;
import pandora.HostProto.SetConnectabilityModeRequest;
import pandora.HostProto.SetDiscoverabilityModeRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Test cases for {@link DeviceDiscoveryManager}. */
@RunWith(TestParameterInjector.class)
public class DeviceDiscoveryTest {
    private static final String TAG = "DeviceDiscoveryTest";
    private static final String TEST_16_BIT_SERVICE_UUID_STRING = "1809";
    private static final String TEST_128_BIT_SERVICE_UUID_STRING =
            "88400001-e95a-844e-c53f-fbec32ed5e54";

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final BluetoothManager mManager = mContext.getSystemService(BluetoothManager.class);
    private final BluetoothAdapter mAdapter = mManager.getAdapter();

    private SettableFuture<String> mFutureDiscoveryStartedIntent;
    private SettableFuture<String> mFutureDiscoveryFinishedIntent;

    @Rule public final AdoptShellPermissionsRule mPermissionRule = new AdoptShellPermissionsRule();

    @Rule public final PandoraDevice mBumble = new PandoraDevice();

    private ArrayList<Intent> mDeviceFoundData;

    private ArrayList<Intent> mUuidData;

    private BroadcastReceiver mConnectionStateReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())) {
                        mFutureDiscoveryStartedIntent.set(
                                BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                    } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(
                            intent.getAction())) {
                        mFutureDiscoveryFinishedIntent.set(
                                BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                    } else if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                        mDeviceFoundData.add(intent);
                    } else if (BluetoothDevice.ACTION_UUID.equals(intent.getAction())) {
                        mUuidData.add(intent);
                    }
                }
            };

    @Test
    public void startDeviceDiscoveryTest() throws Exception {
        mFutureDiscoveryStartedIntent = SettableFuture.create();
        mFutureDiscoveryFinishedIntent = SettableFuture.create();

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mContext.registerReceiver(mConnectionStateReceiver, filter);

        assertThat(mAdapter.startDiscovery()).isTrue();
        assertThat(mFutureDiscoveryStartedIntent.get())
                .isEqualTo(BluetoothAdapter.ACTION_DISCOVERY_STARTED);

        // Wait for device discovery to complete
        assertThat(mFutureDiscoveryFinishedIntent.get())
                .isEqualTo(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        mContext.unregisterReceiver(mConnectionStateReceiver);
    }

    @Test
    public void cancelDeviceDiscoveryTest() throws Exception {
        mFutureDiscoveryStartedIntent = SettableFuture.create();
        mFutureDiscoveryFinishedIntent = SettableFuture.create();

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mContext.registerReceiver(mConnectionStateReceiver, filter);

        assertThat(mAdapter.startDiscovery()).isTrue();
        assertThat(mFutureDiscoveryStartedIntent.get())
                .isEqualTo(BluetoothAdapter.ACTION_DISCOVERY_STARTED);

        // Issue a cancel discovery and wait for device discovery finished
        assertThat(mAdapter.cancelDiscovery()).isTrue();
        assertThat(mFutureDiscoveryFinishedIntent.get())
                .isEqualTo(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        mContext.unregisterReceiver(mConnectionStateReceiver);
    }

    @Test
    public void checkDeviceIsDiscoveredTest() throws Exception {
        mFutureDiscoveryStartedIntent = SettableFuture.create();
        mFutureDiscoveryFinishedIntent = SettableFuture.create();
        mDeviceFoundData = new ArrayList<>();
        mUuidData = new ArrayList<>();

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        mContext.registerReceiver(mConnectionStateReceiver, filter);

        assertThat(mAdapter.startDiscovery()).isTrue();
        assertThat(mFutureDiscoveryStartedIntent.get())
                .isEqualTo(BluetoothAdapter.ACTION_DISCOVERY_STARTED);

        // Wait for device discovery to complete
        assertThat(mFutureDiscoveryFinishedIntent.get())
                .isEqualTo(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        mContext.unregisterReceiver(mConnectionStateReceiver);

        // Ensure we received at least one inquiry response
        assertThat(!mDeviceFoundData.isEmpty()).isTrue();
        Log.i(TAG, "Found inquiry results count:" + mDeviceFoundData.size());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_REPORT_UUIDS_FROM_LE_ADVERTISING_DATA)
    public void getUuidsInBleAdvertisingData() throws Exception {
        mFutureDiscoveryStartedIntent = SettableFuture.create();
        mFutureDiscoveryFinishedIntent = SettableFuture.create();
        mDeviceFoundData = new ArrayList<>();
        mUuidData = new ArrayList<>();

        // Make Bumble BLE-only by setting BR/EDR not discoverable/connectable.
        // Note: Even with these calls, Bumble does not set the 'BR/EDR not supported' flag as true,
        //       which makes this test fail when the address type is PUBLIC.
        //       Therefore, this now only tests the RANDOM address type.
        mBumble.hostBlocking()
                .setDiscoverabilityMode(
                        SetDiscoverabilityModeRequest.newBuilder()
                                .setMode(DiscoverabilityMode.NOT_DISCOVERABLE)
                                .build());
        mBumble.hostBlocking()
                .setConnectabilityMode(
                        SetConnectabilityModeRequest.newBuilder()
                                .build()
                                .newBuilder()
                                .setMode(ConnectabilityMode.NOT_CONNECTABLE)
                                .build());

        AdvertiseRequest.Builder requestBuilder =
                AdvertiseRequest.newBuilder().setOwnAddressType(OwnAddressType.RANDOM);
        DataTypes.Builder dataTypeBuilder = DataTypes.newBuilder();
        dataTypeBuilder.addCompleteServiceClassUuids16(TEST_16_BIT_SERVICE_UUID_STRING);
        dataTypeBuilder.addCompleteServiceClassUuids128(TEST_128_BIT_SERVICE_UUID_STRING);
        dataTypeBuilder.setLeDiscoverabilityModeValue(
                DiscoverabilityMode.DISCOVERABLE_GENERAL_VALUE);

        requestBuilder.setData(dataTypeBuilder.build());
        requestBuilder.setLegacy(true); // Bumble only supports legacy advertising

        advertiseWithBumble(requestBuilder);

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        mContext.registerReceiver(mConnectionStateReceiver, filter);

        assertThat(mAdapter.startDiscovery()).isTrue();
        assertThat(mFutureDiscoveryStartedIntent.get())
                .isEqualTo(BluetoothAdapter.ACTION_DISCOVERY_STARTED);

        // Wait for device discovery to complete
        assertThat(mFutureDiscoveryFinishedIntent.get())
                .isEqualTo(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        mContext.unregisterReceiver(mConnectionStateReceiver);

        assertThat(mUuidData).isNotEmpty();
        Intent intent = mUuidData.get(0);
        List<ParcelUuid> uuids =
                Arrays.asList(
                        intent.getParcelableArrayExtra(
                                BluetoothDevice.EXTRA_UUID, ParcelUuid.class));

        assertThat(uuids).contains(ParcelUuid.fromString(TEST_128_BIT_SERVICE_UUID_STRING));
        assertThat(uuids).contains(convert16bitUuidToParcelUuid(TEST_16_BIT_SERVICE_UUID_STRING));

        BluetoothDevice device =
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
        uuids = Arrays.asList(device.getUuids());
        assertThat(uuids).contains(ParcelUuid.fromString(TEST_128_BIT_SERVICE_UUID_STRING));
        assertThat(uuids).contains(convert16bitUuidToParcelUuid(TEST_16_BIT_SERVICE_UUID_STRING));
    }

    private ParcelUuid convert16bitUuidToParcelUuid(String uuidString) {
        return ParcelUuid.fromString("0000" + uuidString + "-0000-1000-8000-00805f9b34fb");
    }

    private void advertiseWithBumble(AdvertiseRequest.Builder requestBuilder) {
        // Collect and ignore responses.
        StreamObserverSpliterator<AdvertiseResponse> responseObserver =
                new StreamObserverSpliterator<>();
        mBumble.host().advertise(requestBuilder.build(), responseObserver);
    }
}
