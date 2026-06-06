/*
 * Copyright (C) 2026 The Android Open Source Project
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

import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.AttributionSource;
import android.os.ParcelUuid;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;

/** Test cases for {@link BluetoothGatt}. */
@RunWith(JUnit4.class)
public class BluetoothGattTest {
    private static final BluetoothDevice DEVICE = new BluetoothDevice("00:11:22:AA:BB:CC");
    private static final AttributionSource ATTRIBUTION_SOURCE = AttributionSource.myAttributionSource();

    @Test
    public void disconnectBeforeClientRegisteredCancelsPendingConnect() throws Exception {
        IBluetoothGatt service = mock(IBluetoothGatt.class);
        BluetoothGattCallback callback = mock(BluetoothGattCallback.class);
        BluetoothGatt gatt =
                new BluetoothGatt(
                        service,
                        DEVICE,
                        BluetoothDevice.TRANSPORT_LE,
                        false,
                        BluetoothDevice.PHY_LE_1M_MASK,
                        ATTRIBUTION_SOURCE);

        assertThat(gatt.connect(false, callback, null)).isTrue();
        gatt.disconnect();

        ArgumentCaptor<IBluetoothGattCallback> callbackCaptor =
                ArgumentCaptor.forClass(IBluetoothGattCallback.class);
        verify(service)
                .registerClient(
                        any(ParcelUuid.class),
                        callbackCaptor.capture(),
                        eq(false),
                        eq(BluetoothDevice.TRANSPORT_LE),
                        eq(ATTRIBUTION_SOURCE));

        callbackCaptor.getValue().onClientRegistered(BluetoothGatt.GATT_SUCCESS);

        verify(service).unregisterClient(callbackCaptor.getValue(), ATTRIBUTION_SOURCE);
        verify(service, never())
                .clientConnect(
                        any(IBluetoothGattCallback.class),
                        any(BluetoothDevice.class),
                        anyInt(),
                        anyBoolean(),
                        anyInt(),
                        anyBoolean(),
                        anyInt(),
                        any(AttributionSource.class));
        verify(callback)
                .onConnectionStateChange(
                        gatt, BluetoothGatt.GATT_FAILURE, STATE_DISCONNECTED);
    }
}
