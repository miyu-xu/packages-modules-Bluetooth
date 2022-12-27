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

package com.android.bluetooth.gatt;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.IAdvertisingSetCallback;
import android.bluetooth.le.PeriodicAdvertisingParameters;
import android.os.IBinder;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.TestUtils;
import com.android.bluetooth.btservice.AdapterService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Test cases for {@link AdvertiseManager}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class AdvertiseManagerTest {

    @Mock
    private AdapterService mAdapterService;

    @Mock
    private GattService service;

    @Mock
    private AdapterService adapterService;

    @Mock
    private GattService.AdvertiserMap advertiserMap;

    @Mock
    private IAdvertisingSetCallback callback;

    @Mock
    private IBinder binder;

    private AdvertiseManager advertiseManager;
    private int advertiserId;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        TestUtils.setAdapterService(mAdapterService);

        advertiseManager = new AdvertiseManager(service, adapterService, advertiserMap);
        AdvertisingSetParameters parameters = new AdvertisingSetParameters.Builder().build();
        AdvertiseData advertiseData = new AdvertiseData.Builder().build();
        AdvertiseData scanResponse = new AdvertiseData.Builder().build();
        PeriodicAdvertisingParameters periodicParameters =
                new PeriodicAdvertisingParameters.Builder().build();
        AdvertiseData periodicData = new AdvertiseData.Builder().build();
        int duration = 10;
        int maxExtAdvEvents = 15;

        doReturn(binder).when(callback).asBinder();
        doNothing().when(binder).linkToDeath(any(), eq(0));

        advertiseManager.startAdvertisingSet(parameters, advertiseData, scanResponse,
                periodicParameters, periodicData, duration, maxExtAdvEvents, callback);

        advertiserId = AdvertiseManager.sTempRegistrationId;

        assertThat(advertiseManager.findAdvertiser(advertiserId)).isNotNull();

        int expectedAdvertisersSize = 1;
        assertThat(advertiseManager.mAdvertisers.entrySet().size()).isEqualTo(
                expectedAdvertisersSize);

        verify(advertiserMap).add(advertiserId, callback, service);
        verify(advertiserMap).recordAdvertiseStart(advertiserId, parameters, advertiseData,
                scanResponse, periodicParameters, periodicData, duration, maxExtAdvEvents);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.clearAdapterService(mAdapterService);
    }

    @Test
    public void advertisingSet() {
        boolean enable = true;
        int duration = 60;
        int maxExtAdvEvents = 100;

        advertiseManager.enableAdvertisingSet(advertiserId, enable, duration, maxExtAdvEvents);

        verify(advertiserMap).enableAdvertisingSet(advertiserId, enable, duration, maxExtAdvEvents);
    }

    @Test
    public void advertisingData() {
        AdvertiseData advertiseData = new AdvertiseData.Builder().build();

        advertiseManager.setAdvertisingData(advertiserId, advertiseData);

        verify(advertiserMap).setAdvertisingData(advertiserId, advertiseData);
    }

    @Test
    public void scanResponseData() {
        AdvertiseData scanResponse = new AdvertiseData.Builder().build();

        advertiseManager.setScanResponseData(advertiserId, scanResponse);

        verify(advertiserMap).setScanResponseData(advertiserId, scanResponse);
    }

    @Test
    public void advertisingParameters() {
        AdvertisingSetParameters parameters = new AdvertisingSetParameters.Builder().build();

        advertiseManager.setAdvertisingParameters(advertiserId, parameters);

        verify(advertiserMap).setAdvertisingParameters(advertiserId, parameters);
    }

    @Test
    public void periodicAdvertisingParameters() {
        PeriodicAdvertisingParameters periodicParameters =
                new PeriodicAdvertisingParameters.Builder().build();

        advertiseManager.setPeriodicAdvertisingParameters(advertiserId, periodicParameters);

        verify(advertiserMap).setPeriodicAdvertisingParameters(advertiserId, periodicParameters);
    }

    @Test
    public void periodicAdvertisingData() {
        AdvertiseData periodicData = new AdvertiseData.Builder().build();

        advertiseManager.setPeriodicAdvertisingData(advertiserId, periodicData);

        verify(advertiserMap).setPeriodicAdvertisingData(advertiserId, periodicData);
    }
}
