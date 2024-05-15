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
package com.android.bluetooth.pbapclient;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserManager;
import android.util.Log;

import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.BluetoothMethodProxy;
import com.android.bluetooth.TestUtils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.storage.DatabaseManager;
import com.android.obex.ResponseCodes;
import com.android.vcard.VCardEntry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class PbapClientSocketTest {
    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    private static final int TEST_L2CAP_PSM = 4098;
    private static final int TEST_RFCOMM_CHANNEL_ID = 3;

    private BluetoothAdapter mAdapter = null;
    private BluetoothDevice mTestDevice;

    // This class is used to wrap the otherwise unmockable/untestable BluetoothSocket class. As such
    // its difficult to test that socket operations work in a unit test when we can't mock the
    // underlying socket framework. The best we can do test-wise is to test the injection framework
    @Mock private InputStream mInjectedInput;
    @Mock private OutputStream mInjectedOutput;

    @Before
    public void setUp() throws IOException {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mTestDevice = TestUtils.getTestDevice(mAdapter, 1);

        PbapClientSocket.inject(mInjectedInput, mInjectedOutput);
    }

    @Test
    public void testCreateSocketWithInjection_usingL2cap() throws IOException {
        PbapClientSocket socket = PbapClientSocket.getL2capSocketForDevice(mTestDevice, TEST_L2CAP_PSM);

        assertThat(socket.getRemoteDevice()).isEqualTo(mTestDevice);
        assertThat(socket.getConnectionType()).isEqualTo(BluetoothSocket.TYPE_L2CAP);
        assertThat(socket.getMaxTransmitPacketSize()).isEqualTo(255);
        assertThat(socket.getMaxReceivePacketSize()).isEqualTo(255);
        assertThat(socket.getInputStream()).isEqualTo(mInjectedInput);
        assertThat(socket.getOutputStream()).isEqualTo(mInjectedOutput);

        assertThat(socket.toString()).isNotNull();
        assertThat(socket.toString()).isNotEmpty();
    }

    @Test
    public void testCreateSocketWithInjection_usingRfcomm() throws IOException {
        PbapClientSocket socket = PbapClientSocket.getRfcommSocketForDevice(mTestDevice, TEST_RFCOMM_CHANNEL_ID);

        assertThat(socket.getRemoteDevice()).isEqualTo(mTestDevice);
        assertThat(socket.getConnectionType()).isEqualTo(BluetoothSocket.TYPE_RFCOMM);
        assertThat(socket.getMaxTransmitPacketSize()).isEqualTo(255);
        assertThat(socket.getMaxReceivePacketSize()).isEqualTo(255);
        assertThat(socket.getInputStream()).isEqualTo(mInjectedInput);
        assertThat(socket.getOutputStream()).isEqualTo(mInjectedOutput);

        assertThat(socket.toString()).isNotNull();
        assertThat(socket.toString()).isNotEmpty();
    }

    @Test
    public void testCloseSocketWithInjection() throws IOException {
        PbapClientSocket socket = PbapClientSocket.getL2capSocketForDevice(mTestDevice, TEST_L2CAP_PSM);

        socket.close();

        verify(mInjectedInput).close();
        verify(mInjectedOutput).close();
    }
}
