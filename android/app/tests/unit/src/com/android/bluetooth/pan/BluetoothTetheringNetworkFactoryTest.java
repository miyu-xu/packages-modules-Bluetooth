/*
 * Copyright 2022 The Android Open Source Project
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

package com.android.bluetooth.pan;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import android.content.Context;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Test cases for {@link BluetoothTetheringNetworkFactory}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class BluetoothTetheringNetworkFactoryTest {

    @Mock
    private PanService panService;

    private Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void networkStartReverseTetherEmptyIface() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        BluetoothTetheringNetworkFactory bluetoothTetheringNetworkFactory =
                new BluetoothTetheringNetworkFactory(context, Looper.myLooper(), panService);

        String iface = "";
        bluetoothTetheringNetworkFactory.startReverseTether(iface);

        assertThat(bluetoothTetheringNetworkFactory.getProvider()).isNull();
    }

    @Test
    public void networkStartReverseTether() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        BluetoothTetheringNetworkFactory bluetoothTetheringNetworkFactory =
                new BluetoothTetheringNetworkFactory(context, Looper.myLooper(), panService);

        String iface = "iface";
        bluetoothTetheringNetworkFactory.startReverseTether(iface);

        assertThat(bluetoothTetheringNetworkFactory.getProvider()).isNotNull();
    }

    @Test
    public void networkStartReverseTetherStop() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        BluetoothTetheringNetworkFactory bluetoothTetheringNetworkFactory =
                new BluetoothTetheringNetworkFactory(context, Looper.myLooper(), panService);

        String iface = "iface";
        bluetoothTetheringNetworkFactory.startReverseTether(iface);

        assertThat(bluetoothTetheringNetworkFactory.getProvider()).isNotNull();

        bluetoothTetheringNetworkFactory.stopReverseTether();

        // Only called in onCancelRequest
        verify(panService, times(1)).getConnectedDevices();
    }
}
