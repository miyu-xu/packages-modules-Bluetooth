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

package com.android.bluetooth.le_scan;

import static com.android.bluetooth.TestUtils.MockitoRule;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.IPeriodicAdvertisingCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.IBinder;

import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

/** Test cases for {@link PeriodicScanManager}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class PeriodicScanManagerTest {
    @Rule public final MockitoRule mMockitoRule = new MockitoRule();

    @Mock private PeriodicScanNativeInterface mPeriodicScanNativeInterface;
    @Mock private IPeriodicAdvertisingCallback mCallback;
    @Mock private IBinder mBinder;

    private BluetoothAdapter mAdapter;
    private PeriodicScanManager mPeriodicScanManager;
    private BluetoothDevice mTestDevice;
    private ScanResult mScanResult;
    private Context mTargetContext;

    private static final String REMOTE_DEVICE_ADDRESS = "00:01:02:03:04:05";

    @Before
    public void setUp() throws Exception {
        mTargetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PeriodicScanNativeInterface.setInstance(mPeriodicScanNativeInterface);
        mPeriodicScanManager = new PeriodicScanManager();

        BluetoothManager manager = mTargetContext.getSystemService(BluetoothManager.class);
        assertThat(manager).isNotNull();
        mAdapter = manager.getAdapter();
        mTestDevice =
                mAdapter.getRemoteLeDevice(
                        REMOTE_DEVICE_ADDRESS, BluetoothDevice.ADDRESS_TYPE_RANDOM);

        mScanResult = new ScanResult(mTestDevice, 0, 0, 0, 0, 0, 0, 0, null, 0);
        mCallback = mock(IPeriodicAdvertisingCallback.class);

        doReturn(mBinder).when(mCallback).asBinder();
        doNothing().when(mBinder).linkToDeath(any(), eq(0));
    }

    @After
    public void tearDown() throws Exception {
        mPeriodicScanManager.cleanup();
        PeriodicScanNativeInterface.setInstance(null);
    }

    @Test
    public void testStartSync() throws Exception {
        mPeriodicScanManager.startSync(mScanResult, 0, 0, mCallback);
        verify(mPeriodicScanNativeInterface)
                .startSync(eq(0), eq(REMOTE_DEVICE_ADDRESS), eq(0), eq(0), anyInt());
    }

    @Test
    public void testOnSyncStarted() throws Exception {
        mPeriodicScanManager.startSync(mScanResult, 0, 0, mCallback);

        ArgumentCaptor<Integer> regId = ArgumentCaptor.forClass(Integer.class);
        verify(mPeriodicScanNativeInterface)
                .startSync(eq(0), eq(REMOTE_DEVICE_ADDRESS), eq(0), eq(0), regId.capture());

        mPeriodicScanManager.onSyncStarted(
                regId.getValue(),
                0,
                0,
                BluetoothDevice.ADDRESS_TYPE_RANDOM,
                REMOTE_DEVICE_ADDRESS,
                0,
                100,
                0);
        verify(mCallback).onSyncEstablished(anyInt(), eq(mTestDevice), eq(0), eq(0), eq(0), eq(0));
    }

    @Test
    public void onSyncTransferredCallback_removesPendingTransfer() throws Exception {
        mPeriodicScanManager.transferSetInfo(mTestDevice, 0, 1, mCallback);

        mPeriodicScanManager.onSyncTransferredCallback(0, 0, REMOTE_DEVICE_ADDRESS);
        mPeriodicScanManager.onSyncTransferredCallback(0, 0, REMOTE_DEVICE_ADDRESS);

        verify(mCallback, times(1)).onSyncTransferred(eq(mTestDevice), eq(0));
    }

    @Test
    public void transferSetInfo_sameAddress_multipleUpdates_singleCallbackToLatest()
            throws Exception {
        IPeriodicAdvertisingCallback[] callbacks = new IPeriodicAdvertisingCallback[6];
        IBinder[] binders = new IBinder[6];
        for (int i = 0; i < callbacks.length; i++) {
            callbacks[i] = mock(IPeriodicAdvertisingCallback.class);
            binders[i] = mock(IBinder.class);
            doReturn(binders[i]).when(callbacks[i]).asBinder();
            doNothing().when(binders[i]).linkToDeath(any(), eq(0));
        }

        for (IPeriodicAdvertisingCallback callback : callbacks) {
            mPeriodicScanManager.transferSetInfo(mTestDevice, 0, 1, callback);
        }

        clearInvocations((Object[]) callbacks);

        mPeriodicScanManager.onSyncTransferredCallback(0, 0, REMOTE_DEVICE_ADDRESS);

        for (int i = 0; i < callbacks.length - 1; i++) {
            verify(callbacks[i], never()).onSyncTransferred(any(), anyInt());
        }
        verify(callbacks[callbacks.length - 1], times(1))
                .onSyncTransferred(eq(mTestDevice), eq(0));

        mPeriodicScanManager.onSyncTransferredCallback(0, 0, REMOTE_DEVICE_ADDRESS);

        for (IPeriodicAdvertisingCallback callback : callbacks) {
            verifyNoMoreInteractions(callback);
        }
    }
}
