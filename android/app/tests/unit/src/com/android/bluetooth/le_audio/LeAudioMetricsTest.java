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

import com.android.bluetooth.TestUtils;
import com.android.bluetooth.btservice.AdapterService;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class LeAudioMetricsTest {
    @Mock private AdapterService mAdapterService;
    @Mock private com.android.bluetooth.le_audio.LeAudioService mLeAudioService;
    BluetoothAdapter mAdapter;
    com.android.bluetooth.le_audio.LeAudioMetrics mLeAudioMetrics;

    private BluetoothDevice mLeftDevice;
    private BluetoothDevice mRightDevice;
    private BluetoothDevice mSingleDevice;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        
        TestUtils.setAdapterService(mAdapterService);
        mLeAudioMetrics = new com.android.bluetooth.le_audio.LeAudioMetrics(mLeAudioService);

        mAdapter = BluetoothAdapter.getDefaultAdapter();

        mLeftDevice = TestUtils.getTestDevice(mAdapter, 0);
        mRightDevice = TestUtils.getTestDevice(mAdapter, 1);
        mSingleDevice = TestUtils.getTestDevice(mAdapter, 2);

        doReturn(1).when(mLeAudioService).getGroupId(mSingleDevice);
        doReturn(2).when(mLeAudioService).getGroupId(mLeftDevice);
        doReturn(2).when(mLeAudioService).getGroupId(mRightDevice);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSingleDevice_connectingConnectedDisconnected() {
        mLeAudioMetrics.addStateChangedEvent(mSingleDevice, BluetoothProfile.STATE_CONNECTING,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_UNKNOWN);
        mLeAudioMetrics.addStateChangedEvent(mSingleDevice, BluetoothProfile.STATE_CONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);
        mLeAudioMetrics.addStateChangedEvent(mSingleDevice, BluetoothProfile.STATE_DISCONNECTED,
                com.android.bluetooth.le_audio.LeAudioMetrics.STATUS_SUCCESS);
    }
}
