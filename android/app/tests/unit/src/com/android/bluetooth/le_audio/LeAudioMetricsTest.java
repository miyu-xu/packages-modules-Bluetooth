/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.bluetooth.le_audio;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;


import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.nullable;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.eq;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le_audio.LeAudioMetricsWriter;

import com.android.bluetooth.TestUtils;
import com.android.bluetooth.btservice.AdapterService;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class LeAudioMetricsTest {
    @Mock
    private AdapterService mAdapterService;
    @Mock
    private com.android.bluetooth.le_audio.LeAudioService mLeAudioService;
    BluetoothAdapter mAdapter;
    com.android.bluetooth.le_audio.LeAudioMetrics mLeAudioMetrics;
    @Mock
    private LeAudioMetricsWriter mLeAudioMetricsWriter;
    @Captor
    private ArgumentCaptor<int[]> mIntArrayCaptor;

    private BluetoothDevice mLeftDevice;
    private BluetoothDevice mRightDevice;
    private BluetoothDevice mSingleDevice;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        TestUtils.setAdapterService(mAdapterService);
        mLeAudioMetrics = new com.android.bluetooth.le_audio.LeAudioMetrics(mLeAudioService);

        mAdapter = BluetoothAdapter.getDefaultAdapter();

        mLeftDevice = TestUtils.getTestDevice(mAdapter, 1);
        mRightDevice = TestUtils.getTestDevice(mAdapter, 2);
        mSingleDevice = TestUtils.getTestDevice(mAdapter, 3);

        setLeAudioMetricsWriter(mLeAudioMetricsWriter);

        doReturn(1).when(mLeAudioService).getGroupId(mSingleDevice);
        doReturn(2).when(mLeAudioService).getGroupId(mLeftDevice);
        doReturn(2).when(mLeAudioService).getGroupId(mRightDevice);
        doReturn(1).when(mAdapterService).getMetricId(mLeftDevice);
        doReturn(2).when(mAdapterService).getMetricId(mRightDevice);
        doReturn(3).when(mAdapterService).getMetricId(mSingleDevice);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.clearAdapterService(mAdapterService);
        setLeAudioMetricsWriter(null);
    }

    @Test
    public void testSetup() {
    }

    @Test
    public void testSingleDevice_connectingConnectedDisconnected() {
        assertThat(mLeAudioMetrics.getMetricsMap()).isEmpty();

        // process
        mLeAudioMetrics.addStateChangedEvent(mSingleDevice, BluetoothProfile.STATE_CONNECTING,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_UNKNOWN);
        mLeAudioMetrics.addStateChangedEvent(mSingleDevice, BluetoothProfile.STATE_CONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);
        mLeAudioMetrics.addStateChangedEvent(mSingleDevice, BluetoothProfile.STATE_DISCONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);

        // check
        verify(mLeAudioMetricsWriter, times(1)).write(any(long[].class), any(long[].class),
                any(long[].class), any(int[].class), any(int[].class), any(int[].class));
        assertThat(mLeAudioMetrics.getMetricsMap()).isEmpty();
    }

    @Test
    public void testSingleDevice_connectingConnectedDisconnectedTwoTimes() {
        assertThat(mLeAudioMetrics.getMetricsMap()).isEmpty();

        // process
        mLeAudioMetrics.addStateChangedEvent(mSingleDevice, BluetoothProfile.STATE_CONNECTING,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_UNKNOWN);
        mLeAudioMetrics.addStateChangedEvent(mSingleDevice, BluetoothProfile.STATE_CONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);
        mLeAudioMetrics.addStateChangedEvent(mSingleDevice, BluetoothProfile.STATE_DISCONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);
        mLeAudioMetrics.addStateChangedEvent(mSingleDevice, BluetoothProfile.STATE_CONNECTING,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_UNKNOWN);
        mLeAudioMetrics.addStateChangedEvent(mSingleDevice, BluetoothProfile.STATE_CONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);
        mLeAudioMetrics.addStateChangedEvent(mSingleDevice, BluetoothProfile.STATE_DISCONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);

        // check
        verify(mLeAudioMetricsWriter, times(2)).write(any(long[].class), any(long[].class),
                any(long[].class), any(int[].class), any(int[].class), any(int[].class));
        assertThat(mLeAudioMetrics.getMetricsMap()).isEmpty();
    }

    @Test
    public void testStereoDevice_connectingConnectedDisconnected() {
        assertThat(mLeAudioMetrics.getMetricsMap()).isEmpty();

        // process
        mLeAudioMetrics.addStateChangedEvent(mLeftDevice, BluetoothProfile.STATE_CONNECTING,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_UNKNOWN);
        mLeAudioMetrics.addStateChangedEvent(mLeftDevice, BluetoothProfile.STATE_CONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);
        mLeAudioMetrics.addStateChangedEvent(mRightDevice, BluetoothProfile.STATE_CONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);
        mLeAudioMetrics.addStateChangedEvent(mLeftDevice, BluetoothProfile.STATE_DISCONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);
        mLeAudioMetrics.addStateChangedEvent(mRightDevice, BluetoothProfile.STATE_DISCONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);

        // check
        verify(mLeAudioMetricsWriter, times(1)).write(any(long[].class), any(long[].class),
                any(long[].class), any(int[].class), any(int[].class), mIntArrayCaptor.capture());
        assertThat(mIntArrayCaptor.getValue()).hasLength(2);
        assertThat(mLeAudioMetrics.getMetricsMap()).isEmpty();
    }

    @Test
    public void testStereoDevice_oneReconnection() {
        assertThat(mLeAudioMetrics.getMetricsMap()).isEmpty();

        // process
        mLeAudioMetrics.addStateChangedEvent(mLeftDevice, BluetoothProfile.STATE_CONNECTING,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_UNKNOWN);
        mLeAudioMetrics.addStateChangedEvent(mLeftDevice, BluetoothProfile.STATE_CONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);
        mLeAudioMetrics.addStateChangedEvent(mRightDevice, BluetoothProfile.STATE_CONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);
        mLeAudioMetrics.addStateChangedEvent(mRightDevice, BluetoothProfile.STATE_DISCONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);
        mLeAudioMetrics.addStateChangedEvent(mRightDevice, BluetoothProfile.STATE_CONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);
        mLeAudioMetrics.addStateChangedEvent(mLeftDevice, BluetoothProfile.STATE_DISCONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);
        mLeAudioMetrics.addStateChangedEvent(mRightDevice, BluetoothProfile.STATE_DISCONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);

        // check
        verify(mLeAudioMetricsWriter, times(1)).write(any(long[].class), any(long[].class),
                any(long[].class), any(int[].class), any(int[].class), mIntArrayCaptor.capture());
        assertThat(mIntArrayCaptor.getValue()).hasLength(3);
        assertThat(mLeAudioMetrics.getMetricsMap()).isEmpty();
    }

    @Test
    public void testMixDevices() {
        assertThat(mLeAudioMetrics.getMetricsMap()).isEmpty();

        // process
        mLeAudioMetrics.addStateChangedEvent(mSingleDevice, BluetoothProfile.STATE_CONNECTING,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_UNKNOWN);
        mLeAudioMetrics.addStateChangedEvent(mSingleDevice, BluetoothProfile.STATE_CONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);
        mLeAudioMetrics.addStateChangedEvent(mLeftDevice, BluetoothProfile.STATE_CONNECTING,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_UNKNOWN);
        mLeAudioMetrics.addStateChangedEvent(mLeftDevice, BluetoothProfile.STATE_CONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);
        mLeAudioMetrics.addStateChangedEvent(mRightDevice, BluetoothProfile.STATE_CONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);
        mLeAudioMetrics.addStateChangedEvent(mLeftDevice, BluetoothProfile.STATE_DISCONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);
        mLeAudioMetrics.addStateChangedEvent(mRightDevice, BluetoothProfile.STATE_DISCONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);
        mLeAudioMetrics.addStateChangedEvent(mSingleDevice, BluetoothProfile.STATE_DISCONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);

        // check
        verify(mLeAudioMetricsWriter, times(2)).write(any(long[].class), any(long[].class),
                any(long[].class), any(int[].class), any(int[].class), any(int[].class));
        assertThat(mLeAudioMetrics.getMetricsMap()).isEmpty();
    }

    private static void setLeAudioMetricsWriter(LeAudioMetricsWriter leAudioMetricsWriter)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        // We cannot mock AdapterService.getAdapterService() with Mockito.
        // Hence we need to use reflection to call a private method to
        // initialize properly the AdapterService.sAdapterService field.
        Method method =
                LeAudioMetricsWriter.class.getDeclaredMethod("setInstance",
                        LeAudioMetricsWriter.class);
        method.setAccessible(true);
        method.invoke(null, leAudioMetricsWriter);
    }
}
