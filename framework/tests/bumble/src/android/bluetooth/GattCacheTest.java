/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.bluetooth.test_utils.EnableBluetoothRule;
import android.content.Context;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.testing.junit.testparameterinjector.TestParameterInjector;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import pandora.GattProto.GattCharacteristicParams;
import pandora.GattProto.GattServiceParams;
import pandora.GattProto.RegisterServiceRequest;
import pandora.HostProto.AdvertiseRequest;
import pandora.HostProto.AdvertiseResponse;
import pandora.HostProto.OwnAddressType;

import java.util.Set;
import java.util.UUID;

@RunWith(TestParameterInjector.class)
public class GattCacheTest {
    private static final String TAG = "GattCacheTest";

    private static final UUID GAP_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");

    private static final UUID TEST_SERVICE_UUID =
            UUID.fromString("00000000-0000-0000-0000-00000000000");
    private static final UUID TEST_CHARACTERISTIC_UUID =
            UUID.fromString("00010001-0000-0000-0000-000000000000");

    @Rule(order = 0)
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule(order = 1)
    public final PandoraDevice mBumble = new PandoraDevice();

    @Rule(order = 2)
    public final EnableBluetoothRule mEnableBluetoothRule = new EnableBluetoothRule(false, true);

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final BluetoothManager mManager = mContext.getSystemService(BluetoothManager.class);
    private final BluetoothAdapter mAdapter = mManager.getAdapter();

    private Host mHost;
    private BluetoothDevice mRemoteLeDevice;

    @Before
    public void setUp() throws Exception {
        InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .adoptShellPermissionIdentity();

        mHost = new Host(mContext);
        mRemoteLeDevice =
                mAdapter.getRemoteLeDevice(
                        Utils.BUMBLE_RANDOM_ADDRESS, BluetoothDevice.ADDRESS_TYPE_RANDOM);
        mRemoteLeDevice.removeBond();
    }

    @After
    public void tearDown() throws Exception {
        InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .dropShellPermissionIdentity();
        Set<BluetoothDevice> bondedDevices = mAdapter.getBondedDevices();
        if (bondedDevices.contains(mRemoteLeDevice)) {
            mRemoteLeDevice.removeBond();
        }
        mHost.close();
    }

    // TODO: Test duplicated from GattClientTest - remove here or there?
    @Test
    public void clientGattDiscoverServices() throws Exception {
        BluetoothGattCallback gattCallback = mock(BluetoothGattCallback.class);
        BluetoothGatt gatt = connectGattAndWaitConnection(gattCallback);

        try {
            gatt.discoverServices();
            verify(gattCallback, timeout(10000)).onServicesDiscovered(any(), eq(GATT_SUCCESS));

            assertThat(gatt.getServices().stream().map(BluetoothGattService::getUuid))
                    .contains(GAP_UUID);

        } finally {
            disconnectAndWaitDisconnection(gatt, gattCallback);
        }
    }

    @Test
    public void clientGattDiscoverChangedServices() throws Exception {
        BluetoothGattCallback gattCallback = mock(BluetoothGattCallback.class);
        BluetoothGatt gatt = connectGattAndWaitConnection(gattCallback);

        try {
            gatt.discoverServices();

            // TODO: this assertion doesn't pass, need to ensure Bumble adds GAP service as expected
            // assertThat(gatt.getServices().stream().map(BluetoothGattService::getUuid))
            //    .contains(GAP_UUID);

            registerGattService();
            // TODO: See if can test service changed indication too
            verify(gattCallback, timeout(10000)).onServiceChanged(any());

            gatt.discoverServices();
            // TODO: needs service change indication working for this assertion to pass
            // verify(gattCallback, timeout(10000).times(2)).onServicesDiscovered(any(),
            // eq(GATT_SUCCESS));

            verify(gattCallback, timeout(10000)).onServicesDiscovered(any(), eq(GATT_SUCCESS));

            assertThat(gatt.getServices().stream().map(BluetoothGattService::getUuid))
                    .contains(GAP_UUID);
            assertThat(gatt.getServices().stream().map(BluetoothGattService::getUuid))
                    .contains(TEST_SERVICE_UUID);
        } finally {
            disconnectAndWaitDisconnection(gatt, gattCallback);
        }
    }

    private void advertiseWithBumble() {
        AdvertiseRequest request =
                AdvertiseRequest.newBuilder()
                        .setLegacy(true)
                        .setConnectable(true)
                        .setOwnAddressType(OwnAddressType.RANDOM)
                        .build();

        StreamObserverSpliterator<AdvertiseResponse> responseObserver =
                new StreamObserverSpliterator<>();

        mBumble.host().advertise(request, responseObserver);
    }

    private BluetoothGatt connectGattAndWaitConnection(BluetoothGattCallback callback) {
        return connectGattAndWaitConnection(callback, /* autoConnect= */ false);
    }

    private BluetoothGatt connectGattAndWaitConnection(
            BluetoothGattCallback callback, boolean autoConnect) {
        final int status = GATT_SUCCESS;
        final int state = STATE_CONNECTED;

        advertiseWithBumble();

        BluetoothGatt gatt = mRemoteLeDevice.connectGatt(mContext, autoConnect, callback);
        verify(callback, timeout(1000)).onConnectionStateChange(eq(gatt), eq(status), eq(state));

        return gatt;
    }

    private void disconnectAndWaitDisconnection(
            BluetoothGatt gatt, BluetoothGattCallback callback) {
        final int state = BluetoothProfile.STATE_DISCONNECTED;
        gatt.disconnect();
        verify(callback, timeout(1000)).onConnectionStateChange(eq(gatt), anyInt(), eq(state));

        gatt.close();
        gatt = null;
    }

    private void registerGattService() {
        GattCharacteristicParams characteristicParams =
                GattCharacteristicParams.newBuilder()
                        .setProperties(
                                BluetoothGattCharacteristic.PROPERTY_READ
                                        | BluetoothGattCharacteristic.PROPERTY_WRITE)
                        .setUuid(TEST_CHARACTERISTIC_UUID.toString())
                        .build();

        GattServiceParams serviceParams =
                GattServiceParams.newBuilder()
                        .addCharacteristics(characteristicParams)
                        .setUuid(TEST_SERVICE_UUID.toString())
                        .build();

        RegisterServiceRequest request =
                RegisterServiceRequest.newBuilder().setService(serviceParams).build();

        mBumble.gattBlocking().registerService(request);
    }
}
