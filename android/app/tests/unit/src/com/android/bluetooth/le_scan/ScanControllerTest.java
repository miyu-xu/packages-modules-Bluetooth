/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.bluetooth.le_scan;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.AttributionSource;

import androidx.test.InstrumentationRegistry;

import com.android.bluetooth.TestUtils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.gatt.GattNativeInterface;
import com.android.bluetooth.gatt.GattObjectsFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/** Test cases for {@link ScanController}. */
@RunWith(JUnit4.class)
public class ScanControllerTest {

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock private AdapterService mAdapterService;
    @Mock private GattObjectsFactory mFactory;
    @Mock private GattNativeInterface mNativeInterface;
    @Mock private ScanManager mScanManager;

    private ScanController mScanController;
    private AttributionSource mAttributionSource;

    @Before
    public void setUp() {
        TestUtils.setAdapterService(mAdapterService);

        GattObjectsFactory.setInstanceForTesting(mFactory);
        doReturn(mNativeInterface).when(mFactory).getNativeInterface();
        doReturn(mScanManager).when(mFactory).createScanManager(any(), any(), any(), any(), any());

        mScanController = new ScanController(InstrumentationRegistry.getTargetContext());
        BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
        mAttributionSource = mAdapter.getAttributionSource();
    }

    @After
    public void tearDown() {
        mScanController.stop();

        TestUtils.clearAdapterService(mAdapterService);
        GattObjectsFactory.setInstanceForTesting(null);
    }

    @Test
    public void numHwTrackFiltersAvailable() {
        mScanController.getTransitionalScanHelper().numHwTrackFiltersAvailable(mAttributionSource);
        verify(mScanManager).getCurrentUsedTrackingAdvertisement();
    }

    @Test
    public void profileConnectionStateChanged_notifyScanManager() {
        mScanController.notifyProfileConnectionStateChange(
                BluetoothProfile.A2DP,
                BluetoothProfile.STATE_CONNECTING,
                BluetoothProfile.STATE_CONNECTED);
        verify(mScanManager)
                .handleBluetoothProfileConnectionStateChanged(
                        BluetoothProfile.A2DP,
                        BluetoothProfile.STATE_CONNECTING,
                        BluetoothProfile.STATE_CONNECTED);
    }
}
