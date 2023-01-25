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


import com.google.protobuf.Empty;
import pandora.HostGrpc;
import io.grpc.CallOptions;
import io.grpc.stub.StreamObserver;
import io.grpc.ManagedChannel;
import io.grpc.okhttp.OkHttpChannelBuilder;

/**
 * Test cases for {@link AdvertiseManager}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class PandoraAdvertiseManagerTest {

    @Mock
    private AdapterService mAdapterService;

    @Mock
    private GattService mService;

    @Mock
    private GattService.AdvertiserMap mAdvertiserMap;

    @Mock
    private IAdvertisingSetCallback mCallback;

    @Mock
    private IBinder mBinder;

    private AdvertiseManager mAdvertiseManager;
    private int mAdvertiserId;

    private ManagedChannel mChannel;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        TestUtils.setAdapterService(mAdapterService);

        mAdvertiseManager = new AdvertiseManager(mService, mAdapterService, mAdvertiserMap);
        AdvertisingSetParameters parameters = new AdvertisingSetParameters.Builder().build();
        AdvertiseData advertiseData = new AdvertiseData.Builder().build();
        AdvertiseData scanResponse = new AdvertiseData.Builder().build();
        PeriodicAdvertisingParameters periodicParameters =
                new PeriodicAdvertisingParameters.Builder().build();
        AdvertiseData periodicData = new AdvertiseData.Builder().build();
        int duration = 10;
        int maxExtAdvEvents = 15;

        doReturn(mBinder).when(mCallback).asBinder();
        doNothing().when(mBinder).linkToDeath(any(), eq(0));

        mAdvertiseManager.startAdvertisingSet(parameters, advertiseData, scanResponse,
                periodicParameters, periodicData, duration, maxExtAdvEvents, mCallback);

        mAdvertiserId = AdvertiseManager.sTempRegistrationId;

        mChannel = OkHttpChannelBuilder
            .forAddress("localhost", 8999)
            .usePlaintext()
            .build();
        HostGrpc.HostBlockingStub stub = HostGrpc.newBlockingStub(mChannel);
        stub.factoryReset(Empty.getDefaultInstance());
        stub.withWaitForReady()
            .readLocalAddress(Empty.getDefaultInstance());
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.clearAdapterService(mAdapterService);
        mChannel.shutdown();
    }

    @Test
    public void advertisingSet() {
        boolean enable = true;
        int duration = 60;
        int maxExtAdvEvents = 100;

        mAdvertiseManager.enableAdvertisingSet(mAdvertiserId, enable, duration, maxExtAdvEvents);

        verify(mAdvertiserMap).enableAdvertisingSet(mAdvertiserId, enable, duration,
                maxExtAdvEvents);
    }

    @Test
    public void advertisingData() {
        AdvertiseData advertiseData = new AdvertiseData.Builder().build();

        mAdvertiseManager.setAdvertisingData(mAdvertiserId, advertiseData);

        verify(mAdvertiserMap).setAdvertisingData(mAdvertiserId, advertiseData);
    }
}
