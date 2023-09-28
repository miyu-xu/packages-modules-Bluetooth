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

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.flags.Flags;

import io.grpc.stub.StreamObserver;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import pandora.HostProto.AdvertiseRequest;
import pandora.HostProto.AdvertiseResponse;
import pandora.HostProto.OwnAddressType;

/** Test cases for {@link BluetoothGattServer}. */
@RunWith(AndroidJUnit4.class)
public class GattServerTest {
    private static final String TAG = "GattServerTest";

    private static final int TIMEOUT_ADVERTISING_MS = 1_000;

    private static android.content.Context sContext;
    private static BluetoothManager sBluetoothManager;
    private static BluetoothAdapter sBluetoothAdapter;

    @Rule public final PandoraDevice mBumble = new PandoraDevice();

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    /** Set up test class */
    @BeforeClass
    public static void setUpClass() throws Exception {
        InstrumentationRegistry.getInstrumentation()
                .getUiAutomation()
                .adoptShellPermissionIdentity();

        sContext = ApplicationProvider.getApplicationContext();
        sBluetoothManager = sContext.getSystemService(BluetoothManager.class);
        sBluetoothAdapter = sBluetoothManager.getAdapter();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_BLE_GATT_SERVER_USE_ADDRESS_TYPE_IN_CONNECTION)
    public void serverConnectToRandomAddress() throws Exception {
        advertiseWithBumble(OwnAddressType.RANDOM);

        BluetoothDevice device =
                sBluetoothAdapter.getRemoteLeDevice(
                        Utils.BUMBLE_RANDOM_ADDRESS, BluetoothDevice.ADDRESS_TYPE_RANDOM);

        BluetoothGattServerCallback mockGattServerCallback =
                mock(BluetoothGattServerCallback.class);
        BluetoothGattServer gattServer =
                sBluetoothManager.openGattServer(
                        sContext, mockGattServerCallback, BluetoothDevice.TRANSPORT_LE);

        assertThat(gattServer).isNotNull();

        gattServer.connect(device, false);
        verify(mockGattServerCallback, timeout(TIMEOUT_ADVERTISING_MS))
                .onConnectionStateChange(any(), anyInt(), eq(BluetoothProfile.STATE_CONNECTED));

        gattServer.close();
    }

    private void advertiseWithBumble(OwnAddressType ownAddressType) {
        AdvertiseRequest request =
                AdvertiseRequest.newBuilder()
                        .setLegacy(true)
                        .setConnectable(true)
                        .setOwnAddressType(ownAddressType)
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
