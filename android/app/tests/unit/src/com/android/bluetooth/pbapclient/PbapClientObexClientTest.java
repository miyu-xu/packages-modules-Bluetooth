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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
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

@MediumTest
@RunWith(AndroidJUnit4.class)
public class PbapClientObexClientTest {
    private static final int TEST_L2CAP_PSM = 4098;
    private static final int TEST_RFCOMM_CHANNEL_ID = 3;

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    private BluetoothAdapter mAdapter = null;
    private BluetoothDevice mDevice;

    private PipedOutputStream mInjectedOutput;
    private PipedInputStream mReceiveClientRequest;

    private PipedInputStream mInjectedInput;
    private PipedOutputStream mSendClientResponse;

    // Normal supported features for our client
    private static final int SUPPORTED_FEATURES =
             PbapSdpRecord.FEATURE_DOWNLOADING | PbapSdpRecord.FEATURE_DATABASE_IDENTIFIER
             | PbapSdpRecord.FEATURE_FOLDER_VERSION_COUNTERS
             | PbapSdpRecord.FEATURE_DEFAULT_IMAGE_FORMAT;

    // Default property filter for downloaded contacts
    private static final long DEFAULT_PROPERTIES = PbapApplicationParameters.PROPERTY_VERSION
            | PbapApplicationParameters.PROPERTY_FN | PbapApplicationParameters.PROPERTY_N
            | PbapApplicationParameters.PROPERTY_PHOTO | PbapApplicationParameters.PROPERTY_ADR
            | PbapApplicationParameters.PROPERTY_TEL | PbapApplicationParameters.PROPERTY_EMAIL
            | PbapApplicationParameters.PROPERTY_NICKNAME;

    // Default configuration for VCard format -> prefer 3.0 to 2.1
    private static final byte DEFAULT_VCARD_VERSION = PbapPhonebook.FORMAT_VCARD_30;

    @Mock Account mMockAccount;
    @Captor ArgumentCaptor<PbapPhonebookMetadata> mMetadataCaptor;
    @Captor ArgumentCaptor<PbapPhonebook> mPhonebookCaptor;

    @Mock PbapClientObexClient.Callback mMockCallback;
    PbapClientObexClient mObexClient;

    @Before
    public void setUp() throws IOException {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        Assert.assertNotNull(mAdapter);
        mDevice = TestUtils.getTestDevice(mAdapter, 1);

        mInjectedOutput = new PipedOutputStream();
        mReceiveClientRequest = new PipedInputStream(mInjectedOutput);
        mInjectedInput = new PipedInputStream();
        mSendClientResponse = new PipedOutputStream(mInjectedInput);

        PbapClientSocket.inject(mInjectedInput, mInjectedOutput);

        mObexClient = new PbapClientObexClient(mDevice, SUPPORTED_FEATURES, mMockCallback);
    }

    @After
    public void tearDown() throws IOException {
        mInjectedOutput.close();
        mReceiveClientRequest.close();
        mInjectedInput.close();
        mSendClientResponse.close();
    }

    //********************************************************************************************//
    // Base State
    //********************************************************************************************//

    @Test
    public void testClientCreated_inDisconnectedState() {
        assertThat(mObexClient.getTransportType()).isEqualTo(PbapClientObexClient.TRANSPORT_NONE);
        assertThat(mObexClient.getL2capPsm()).isEqualTo(PbapClientObexClient.L2CAP_INVALID_PSM);
        assertThat(mObexClient.getRfcommChannelId()).isEqualTo(PbapClientObexClient.RFCOMM_INVALID_CHANNEL_ID);
        assertThat(mObexClient.getConnectionState()).isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
        assertThat(mObexClient.isConnected()).isFalse();
    }

    //********************************************************************************************//
    // Connection Establishment
    //********************************************************************************************//

    // L2CAP

    @Test
    public void testConnect_usingL2capTransport_deviceConnected() throws IOException {
        mObexClient.connectL2cap(TEST_L2CAP_PSM);

        verifyConnectionRequest();

        verify(mMockCallback, timeout(1000)).onConnectionStateChanged(eq(BluetoothProfile.STATE_DISCONNECTED), eq(BluetoothProfile.STATE_CONNECTING));
        assertThat(mObexClient.getTransportType()).isEqualTo(PbapClientObexClient.TRANSPORT_L2CAP);
        assertThat(mObexClient.getL2capPsm()).isEqualTo(TEST_L2CAP_PSM);
        assertThat(mObexClient.getRfcommChannelId()).isEqualTo(PbapClientObexClient.RFCOMM_INVALID_CHANNEL_ID);
        assertThat(mObexClient.getConnectionState()).isEqualTo(BluetoothProfile.STATE_CONNECTING);
        assertThat(mObexClient.isConnected()).isFalse();

        // Accept connection
        sendConnectionResponse(ResponseCodes.OBEX_HTTP_OK);

        verify(mMockCallback, timeout(5000)).onConnectionStateChanged(eq(BluetoothProfile.STATE_CONNECTING), eq(BluetoothProfile.STATE_CONNECTED));
        assertThat(mObexClient.getTransportType()).isEqualTo(PbapClientObexClient.TRANSPORT_L2CAP);
        assertThat(mObexClient.getL2capPsm()).isEqualTo(TEST_L2CAP_PSM);
        assertThat(mObexClient.getRfcommChannelId()).isEqualTo(PbapClientObexClient.RFCOMM_INVALID_CHANNEL_ID);
        assertThat(mObexClient.getConnectionState()).isEqualTo(BluetoothProfile.STATE_CONNECTED);
        assertThat(mObexClient.isConnected()).isTrue();
    }

    // RFCOMM

    @Test
    public void testConnect_usingRfcommTransport_deviceConnected() throws IOException {
        mObexClient.connectRfcomm(TEST_RFCOMM_CHANNEL_ID);

        verifyConnectionRequest();

        assertThat(mObexClient.getTransportType()).isEqualTo(PbapClientObexClient.TRANSPORT_RFCOMM);
        assertThat(mObexClient.getRfcommChannelId()).isEqualTo(TEST_RFCOMM_CHANNEL_ID);
        assertThat(mObexClient.getL2capPsm()).isEqualTo(PbapClientObexClient.L2CAP_INVALID_PSM);
        assertThat(mObexClient.getConnectionState()).isEqualTo(BluetoothProfile.STATE_CONNECTING);
        assertThat(mObexClient.isConnected()).isFalse();

        // Accept connection
        sendConnectionResponse(ResponseCodes.OBEX_HTTP_OK);

        verify(mMockCallback, timeout(5000)).onConnectionStateChanged(eq(BluetoothProfile.STATE_CONNECTING), eq(BluetoothProfile.STATE_CONNECTED));
        assertThat(mObexClient.getTransportType()).isEqualTo(PbapClientObexClient.TRANSPORT_RFCOMM);
        assertThat(mObexClient.getRfcommChannelId()).isEqualTo(TEST_RFCOMM_CHANNEL_ID);
        assertThat(mObexClient.getL2capPsm()).isEqualTo(PbapClientObexClient.L2CAP_INVALID_PSM);
        assertThat(mObexClient.getConnectionState()).isEqualTo(BluetoothProfile.STATE_CONNECTED);
        assertThat(mObexClient.isConnected()).isTrue();
    }

    // Errors

    @Test
    public void testConnect_transportDisconnects_obexDisconnects() throws IOException {
        mObexClient.connectL2cap(TEST_L2CAP_PSM);

        verifyConnectionRequest();

        verify(mMockCallback, timeout(1000)).onConnectionStateChanged(eq(BluetoothProfile.STATE_DISCONNECTED), eq(BluetoothProfile.STATE_CONNECTING));
        assertThat(mObexClient.getConnectionState()).isEqualTo(BluetoothProfile.STATE_CONNECTING);

        mInjectedOutput.close();
        mReceiveClientRequest.close();
        mInjectedInput.close();
        mSendClientResponse.close();

        verify(mMockCallback, timeout(1000)).onConnectionStateChanged(eq(BluetoothProfile.STATE_CONNECTING), eq(BluetoothProfile.STATE_DISCONNECTED));
        assertThat(mObexClient.getConnectionState()).isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
    }

    //********************************************************************************************//
    // Request Metadata
    //********************************************************************************************//

    @Test
    public void testRequestPhonebookMetadata() throws IOException {
        testConnect_usingL2capTransport_deviceConnected();

        PbapApplicationParameters params = new PbapApplicationParameters(DEFAULT_PROPERTIES, DEFAULT_VCARD_VERSION, PbapApplicationParameters.RETURN_SIZE_ONLY, 0);
        mObexClient.requestPhonebookMetadata(PbapPhonebook.LOCAL_PHONEBOOK_PATH, params);

        verifyPhonebookMetadataRequest();

        sendPhonebookMetadataResponse(ResponseCodes.OBEX_HTTP_OK, 2, 1, 1, 1);

        verify(mMockCallback, timeout(5000)).onGetPhonebookMetadataComplete(eq(160), eq(PbapPhonebook.LOCAL_PHONEBOOK_PATH), mMetadataCaptor.capture());
        PbapPhonebookMetadata metadata = mMetadataCaptor.getValue();
        assertThat(metadata.getPhonebook()).isEqualTo(PbapPhonebook.LOCAL_PHONEBOOK_PATH);
        assertThat(metadata.getSize()).isEqualTo(2);
        assertThat(metadata.getDatabaseIdentifier()).isEqualTo("1");
        assertThat(metadata.getPrimaryVersionCounter()).isEqualTo("1");
        assertThat(metadata.getSecondaryVersionCounter()).isEqualTo("1");
    }

    //********************************************************************************************//
    // Request Contacts and Call History
    //********************************************************************************************//

    @Test
    public void testRequestPhonebook() throws IOException {
        testConnect_usingL2capTransport_deviceConnected();

        // Common download parameters for a client
        int numToFetch = 250;
        int batchStart = 0;

        PbapApplicationParameters params = new PbapApplicationParameters(DEFAULT_PROPERTIES, DEFAULT_VCARD_VERSION, numToFetch, batchStart);
        mObexClient.requestDownloadPhonebook(PbapPhonebook.LOCAL_PHONEBOOK_PATH, params, mMockAccount);

        verifyPhonebookDownloadRequest();

        String vcard = createVcard(VERSION_30, "Foo", "Bar", "+1-234-567-8901", "111 Test Street;Test Town;CA;90210;USA", "Foo@email.com");
        sendPhonebookDownloadResponse(ResponseCodes.OBEX_HTTP_OK, vcard);

        verify(mMockCallback, timeout(5000)).onPhonebookContactsDownloaded(eq(160), eq(PbapPhonebook.LOCAL_PHONEBOOK_PATH), mPhonebookCaptor.capture());
        PbapPhonebook phonebook = mPhonebookCaptor.getValue();
        assertThat(phonebook.getPhonebook()).isEqualTo(PbapPhonebook.LOCAL_PHONEBOOK_PATH);
        assertThat(phonebook.getOffset()).isEqualTo(0);
        assertThat(phonebook.getCount()).isEqualTo(1);
        assertThat(phonebook.getList()).isNotEmpty();
        assertThat(phonebook.getList().size()).isEqualTo(1);

        VCardEntry contact1 = phonebook.getList().get(0);
        assertThat(contact1.getDisplayName()).isEqualTo("Foo Bar");
        assertThat(contact1.getPhoneList()).isNotNull();
        assertThat(contact1.getPhoneList().size()).isEqualTo(1);
        assertThat(contact1.getPhoneList().get(0).getNumber()).isEqualTo("+1-234-567-8901");
    }

    //********************************************************************************************//
    // Disconnections
    //********************************************************************************************//

    @Test
    public void testDisconnect_obexDisconnected_nothingHappens() {
        assertThat(mObexClient.isConnected()).isFalse();

        mObexClient.disconnect();

        // Wait for disconnect to be processed?
        // mTestLooper.dispatchAll();

        // verify no onConnectionStateChanged callbacks of any kind
        assertThat(mObexClient.getTransportType()).isEqualTo(PbapClientObexClient.TRANSPORT_NONE);
        assertThat(mObexClient.getL2capPsm()).isEqualTo(PbapClientObexClient.L2CAP_INVALID_PSM);
        assertThat(mObexClient.getRfcommChannelId()).isEqualTo(PbapClientObexClient.RFCOMM_INVALID_CHANNEL_ID);
        assertThat(mObexClient.getConnectionState()).isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
        assertThat(mObexClient.isConnected()).isFalse();
    }

    @Test
    public void testDisconnect_obexConnecting_obexDisconnects() throws IOException {
        mObexClient.connectL2cap(TEST_L2CAP_PSM);

        verifyConnectionRequest();

        verify(mMockCallback, timeout(1000)).onConnectionStateChanged(eq(BluetoothProfile.STATE_DISCONNECTED), eq(BluetoothProfile.STATE_CONNECTING));
        assertThat(mObexClient.getConnectionState()).isEqualTo(BluetoothProfile.STATE_CONNECTING);

        mObexClient.disconnect();

        sendConnectionResponse(ResponseCodes.OBEX_HTTP_OK);

        verify(mMockCallback, timeout(1000)).onConnectionStateChanged(eq(BluetoothProfile.STATE_CONNECTING), eq(BluetoothProfile.STATE_CONNECTED));

        // assumes fully connected? test doesn't verify.
        // What if connection times out?
        // can we interrupt and force it?
        verifyDisconnectRequest();
        verify(mMockCallback, timeout(1000)).onConnectionStateChanged(eq(BluetoothProfile.STATE_CONNECTED), eq(BluetoothProfile.STATE_DISCONNECTING));

        sendDisconnectResponse(ResponseCodes.OBEX_HTTP_OK);
        verify(mMockCallback, timeout(1000)).onConnectionStateChanged(eq(BluetoothProfile.STATE_DISCONNECTING), eq(BluetoothProfile.STATE_DISCONNECTED));

        assertThat(mObexClient.getConnectionState()).isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
    }

    @Test
    public void testDisconnect_obexConnected_obexDisconnects() throws IOException {
        testConnect_usingL2capTransport_deviceConnected();

        mObexClient.disconnect();

        verifyDisconnectRequest();
        verify(mMockCallback, timeout(1000)).onConnectionStateChanged(eq(BluetoothProfile.STATE_CONNECTED), eq(BluetoothProfile.STATE_DISCONNECTING));
        assertThat(mObexClient.getConnectionState()).isEqualTo(BluetoothProfile.STATE_DISCONNECTING);

        sendDisconnectResponse(ResponseCodes.OBEX_HTTP_OK);
        verify(mMockCallback, timeout(1000)).onConnectionStateChanged(eq(BluetoothProfile.STATE_DISCONNECTING), eq(BluetoothProfile.STATE_DISCONNECTED));
        assertThat(mObexClient.getConnectionState()).isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
    }

    @Test
    public void testDisconnect_obexDisconnecting_nothingHappens() throws IOException {
        testConnect_usingL2capTransport_deviceConnected();

        mObexClient.disconnect();

        verifyDisconnectRequest();
        verify(mMockCallback, timeout(1000)).onConnectionStateChanged(eq(BluetoothProfile.STATE_CONNECTED), eq(BluetoothProfile.STATE_DISCONNECTING));
        assertThat(mObexClient.getConnectionState()).isEqualTo(BluetoothProfile.STATE_DISCONNECTING);

        mObexClient.disconnect();
        // how to verify this didn't do anything?

        sendDisconnectResponse(ResponseCodes.OBEX_HTTP_OK);
        verify(mMockCallback, timeout(1000)).onConnectionStateChanged(eq(BluetoothProfile.STATE_DISCONNECTING), eq(BluetoothProfile.STATE_DISCONNECTED));
        assertThat(mObexClient.getConnectionState()).isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
    }

    @Test
    public void testDisconnect_whileRequestingPhonebookMetadata_obexDisconnects() throws IOException {
        testConnect_usingL2capTransport_deviceConnected();

        PbapApplicationParameters params = new PbapApplicationParameters(DEFAULT_PROPERTIES, DEFAULT_VCARD_VERSION, PbapApplicationParameters.RETURN_SIZE_ONLY, 0);
        mObexClient.requestPhonebookMetadata(PbapPhonebook.LOCAL_PHONEBOOK_PATH, params);

        verifyPhonebookMetadataRequest();

        // When disconnect is called, the thread operation is interrupted. All we can do is tear
        // down the tranport. Tearing down the transport implicitly/ungracefully tears down the
        // the session
        mObexClient.disconnect();

        verify(mMockCallback, timeout(1000)).onConnectionStateChanged(eq(BluetoothProfile.STATE_CONNECTED), eq(BluetoothProfile.STATE_DISCONNECTING));
        verify(mMockCallback, timeout(1000)).onConnectionStateChanged(eq(BluetoothProfile.STATE_DISCONNECTING), eq(BluetoothProfile.STATE_DISCONNECTED));
        assertThat(mObexClient.getConnectionState()).isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
    }

    @Test
    public void testDisconnect_whileRequestingPhonebook_obexDisconnects() throws IOException {
        testConnect_usingL2capTransport_deviceConnected();

        // Common download parameters for a client
        int numToFetch = 250;
        int batchStart = 0;

        PbapApplicationParameters params = new PbapApplicationParameters(DEFAULT_PROPERTIES, DEFAULT_VCARD_VERSION, numToFetch, batchStart);
        mObexClient.requestDownloadPhonebook(PbapPhonebook.LOCAL_PHONEBOOK_PATH, params, mMockAccount);

        verifyPhonebookDownloadRequest();

        // When disconnect is called, the thread operation is interrupted. All we can do is tear
        // down the tranport. Tearing down the transport implicitly/ungracefully tears down the
        // the session
        mObexClient.disconnect();

        verify(mMockCallback, timeout(1000)).onConnectionStateChanged(eq(BluetoothProfile.STATE_CONNECTED), eq(BluetoothProfile.STATE_DISCONNECTING));
        verify(mMockCallback, timeout(1000)).onConnectionStateChanged(eq(BluetoothProfile.STATE_DISCONNECTING), eq(BluetoothProfile.STATE_DISCONNECTED));
        assertThat(mObexClient.getConnectionState()).isEqualTo(BluetoothProfile.STATE_DISCONNECTED);
    }

    //********************************************************************************************//
    // Debug/Dump/toString()
    //********************************************************************************************//

    @Test
    public void testTransportToString() {
        assertThat(mObexClient.transportToString(PbapClientObexClient.TRANSPORT_NONE)).isEqualTo("TRANSPORT_NONE");
        assertThat(mObexClient.transportToString(PbapClientObexClient.TRANSPORT_RFCOMM)).isEqualTo("TRANSPORT_RFCOMM");
        assertThat(mObexClient.transportToString(PbapClientObexClient.TRANSPORT_L2CAP)).isEqualTo("TRANSPORT_L2CAP");
        assertThat(mObexClient.transportToString(/* unused transport id */ 4)).isEqualTo("TRANSPORT_RESERVED (4)");
    }

    @Test
    public void testPhonebookToString() throws IOException {
        String str = mObexClient.toString();
        assertThat(str).isNotNull();
        assertThat(str.length()).isNotEqualTo(0);
    }

    //********************************************************************************************//
    // Utilities
    //********************************************************************************************//

    public void verifyConnectionRequest() throws IOException {
        Log.i("FakeObexServer", "Waiting on connection request from client");

        // Check request type (1 byte)
        int requestType = mReceiveClientRequest.read();
        Log.i("FakeObexServer", "received request, type=" + requestType);

        // Read OBEX packet length (2 bytes), version (1 byte), flags (1 bytes) and max packet length (2 bytes)
        int packetLength = mReceiveClientRequest.read();
        packetLength = (packetLength << 8) + mReceiveClientRequest.read();

        Log.i("FakeObexServer", "    size=" + packetLength);

        int version = mReceiveClientRequest.read();
        int flags = mReceiveClientRequest.read();
        int maxPacketLength = mReceiveClientRequest.read();
        maxPacketLength = (maxPacketLength << 8) + mReceiveClientRequest.read();

        Log.i("FakeObexServer", "    version=" + version);
        Log.i("FakeObexServer", "    flags=" + flags);
        Log.i("FakeObexServer", "    maxPacketLength=" + maxPacketLength);

        // Rest of the payload (packet size - 7 bytes)
        byte[] headers = new byte[packetLength - 7];
        int bytesRead = mReceiveClientRequest.read(headers);

        Log.i("FakeObexServer", "    headers (size=" + bytesRead  +")=" + Arrays.toString(headers));
    }

    public void sendConnectionResponse(int responseCode) throws IOException {
        Log.i("FakeObexServer", "Sending connection resposne");

        int responseSize = 7;
        byte[] response = new byte[responseSize];
        response[0] = (byte) responseCode; // responde code
        response[1] = (byte) (255 & (responseSize >> 8)); // Size most significant byte
        response[2] = (byte) (255 & responseSize); // Size least significant byte
        response[3] = (byte) 0x10; // server version
        response[4] = (byte) 0x00; // flags
        response[5] = (byte) (255 >> 8); // max packet size for the server
        response[6] = (byte) (255 & 0xFF); // max packet size for the server

        // COUNT (0xC0) = 2 (4 bytes)
        // LENGTH (0xC3) = <whatever it is> (4 bytes)
        // WHO (0x4A) = server's uuid
        // TARGET (0x46) = looks to be client's uuid

        Log.i("FakeObexServer", "Responding to client");
        Log.i("FakeObexServer", "    code=" + responseCode);
        Log.i("FakeObexServer", "    size=7");
        Log.i("FakeObexServer", "    packet=" + Arrays.toString(response));

        mSendClientResponse.write(response);
        mSendClientResponse.flush();
    }

    public void verifyPhonebookMetadataRequest() throws IOException {
        Log.i("FakeObexServer", "Waiting on phonebook metadata request from client");

        int requestType = mReceiveClientRequest.read();
        Log.i("FakeObexServer", "received request, type=" + requestType); // 0x3/3 is GET, 0x83/131 is GET_FINAL

        int requestSize = mReceiveClientRequest.read();
        requestSize = (requestSize << 8) + mReceiveClientRequest.read();
        Log.i("FakeObexServer", "    packetLength=" + requestSize);

        int headersSize = requestSize - 3;
        byte[] headers = new byte[requestSize];
        int bytesRead = mReceiveClientRequest.read(headers);

        Log.i("FakeObexServer", "    READ " + bytesRead + " BYTES");

        Log.i("FakeObexServer", "    headers=" + Arrays.toString(headers));

        int i = 0;
        while (i < headersSize) {
            int tag = (int) headers[i] & 0xFF;
            int headerType = tag & 0xC0;

            Log.i("FakeObexServer", "        headers[" + i + "]=" + tag);
            Log.i("FakeObexServer", "            type=" + headerType);

            i += 1;

            switch (headerType) {
                // unicode null terminated string with first two bytes indicating string length
                case 0x00:
                    // fallthrough, strings and byte strings can be read the same way
                // byte sequence with the first two bytes after the header identifier being the length
                case 0x40:
                    int length = (headers[i] << 8) + headers[i + 1];
                    Log.i("FakeObexServer", "            length=" + length);
                    i += 2;

                    length -= 3;

                    byte[] valueArray = new byte[length];
                    System.arraycopy(headers, i, valueArray, 0, length);
                    Log.i("FakeObexServer", "            value=" + Arrays.toString(valueArray));
                    i += length;
                    break;

                // Byte header, just one byte after
                case 0x80:
                    byte valueByte = Byte.valueOf(headers[i]);
                    Log.i("FakeObexServer", "            length=1");
                    Log.i("FakeObexServer", "            value=" + valueByte);
                    i += 1;
                    break;

                // 4 Byte, unsigned integer header which will be converted to a long
                case 0xC0:
                    byte[] valueFourBytes = new byte[4];
                    System.arraycopy(headers, i, valueFourBytes, 0, 4);
                    Log.i("FakeObexServer", "            length=4");
                    Log.i("FakeObexServer", "            value=" + valueFourBytes);
                    i += 4;
                    break;
            }
        }
    }

    public void sendPhonebookMetadataResponse(int responseCode, int size, long dbIdentifier, long primaryVersion, long secondaryVersion) throws IOException {
        if (responseCode != ResponseCodes.OBEX_HTTP_OK) {
            int responseSize = 3;
            byte[] response = new byte[responseSize];

            response[0] = (byte) responseCode; // responde code
            response[1] = (byte) (255 & (responseSize >> 8)); // Size most significant byte
            response[2] = (byte) (255 & responseSize); // Size least significant byte

            mSendClientResponse.write(response);
            mSendClientResponse.flush();
            return;
        }

        byte[] dbIdentifierBytes = longToByteArray(dbIdentifier);
        byte[] primaryVersionBytes = longToByteArray(primaryVersion);
        byte[] secondaryVersionBytes = longToByteArray(secondaryVersion);

        // Size: 64
        //   Headers (6)
        //   1 (response code) + 2 (length) + 1 (app param header) + 2 (params length)
        //
        //   Phonebook Size (4):
        //   1 * (1 (param id) + 1 (param length) + 2 (param value))
        //
        //   Counters (54):
        //   3 * (1 (param id) + 1 (param length) + 16 (param value))
        int responseSize = 64;
        byte[] response = new byte[responseSize];

        response[0] = (byte) responseCode; // responde code

        response[1] = (byte) (0xFF & (responseSize >> 8)); // Size most significant byte
        response[2] = (byte) (0xFF & responseSize); // Size least significant byte

        response[3] = (byte) 0x4C; // Application parameters
        response[4] = (byte) 0x00; // app params length
        response[5] = (byte) 0x3d; // app params length

        // Phonebook size (0x08)
        response[6] = (byte) (0x08); // Param ID
        response[7] = (byte) (0x02); // Param Length
        response[8] = (byte) (0x00); // Param Value
        response[9] = (byte) (0xFF & size); // Param Value

        // Phonebook primary folder version (0x0A), fixed length of 16 bytes
        response[10] = (byte) (0x0A); // Param ID
        response[11] = (byte) (0x10); // Param Length
        System.arraycopy(primaryVersionBytes, 0, response, 12, 16);

        // Phonebook secondary folder version (0x0B), fixed length of 16 bytes
        response[28] = (byte) (0x0B); // Param ID
        response[29] = (byte) (0x10); // Param Length
        System.arraycopy(secondaryVersionBytes, 0, response, 30, 16);

        // database identifier (0x0D), fixed length of 16 bytes
        response[46] = (byte) (0x0D); // Param ID
        response[47] = (byte) (0x10); // Param Length
        System.arraycopy(secondaryVersionBytes, 0, response, 48, 16);

        Log.i("FakeObexServer", "Responding to client");
        Log.i("FakeObexServer", "    code=" + responseCode);
        Log.i("FakeObexServer", "    size=" + responseSize);
        Log.i("FakeObexServer", "    packet=" + Arrays.toString(response));

        mSendClientResponse.write(response);
        mSendClientResponse.flush();
    }

    public void verifyPhonebookDownloadRequest() throws IOException {
        Log.i("FakeObexServer", "Waiting on phonebook download request from client");

        int requestType = mReceiveClientRequest.read();
        Log.i("FakeObexServer", "received request, type=" + requestType); // 0x3/3 is GET, 0x83/131 is GET_FINAL

        int requestSize = mReceiveClientRequest.read();
        requestSize = (requestSize << 8) + mReceiveClientRequest.read();
        Log.i("FakeObexServer", "    packetLength=" + requestSize);

        int headersSize = requestSize - 3;
        byte[] headers = new byte[requestSize];
        int bytesRead = mReceiveClientRequest.read(headers);

        Log.i("FakeObexServer", "    READ " + bytesRead + " BYTES");

        Log.i("FakeObexServer", "    headers=" + Arrays.toString(headers));

        int i = 0;
        while (i < headersSize) {
            int tag = (int) headers[i] & 0xFF;
            int headerType = tag & 0xC0;

            Log.i("FakeObexServer", "        headers[" + i + "]=" + tag);
            Log.i("FakeObexServer", "            type=" + headerType);

            i += 1;

            switch (headerType) {
                // unicode null terminated string with first two bytes indicating string length
                case 0x00:
                    // fallthrough, strings and byte strings can be read the same way
                // byte sequence with the first two bytes after the header identifier being the length
                case 0x40:
                    int length = (headers[i] << 8) + headers[i + 1];
                    Log.i("FakeObexServer", "            length=" + length);
                    i += 2;

                    length -= 3;

                    byte[] valueArray = new byte[length];
                    System.arraycopy(headers, i, valueArray, 0, length);
                    Log.i("FakeObexServer", "            value=" + Arrays.toString(valueArray));
                    i += length;
                    break;

                // Byte header, just one byte after
                case 0x80:
                    byte valueByte = Byte.valueOf(headers[i]);
                    Log.i("FakeObexServer", "            length=1");
                    Log.i("FakeObexServer", "            value=" + valueByte);
                    i += 1;
                    break;

                // 4 Byte, unsigned integer header which will be converted to a long
                case 0xC0:
                    byte[] valueFourBytes = new byte[4];
                    System.arraycopy(headers, i, valueFourBytes, 0, 4);
                    Log.i("FakeObexServer", "            length=4");
                    Log.i("FakeObexServer", "            value=" + valueFourBytes);
                    i += 4;
                    break;
            }
        }
    }

    public void sendPhonebookDownloadResponse(int responseCode, String vcards) throws IOException {
        byte[] contacts = vcards.getBytes();
        int contactsSize = contacts.length;


        // Size: 6 + contacts/vcard payload size
        //   Headers (6)
        //   1 (response code) + 2 (length) + 1 (end of body app param header) + 2 (params length)
        int responseSize = 6 + contactsSize;
        byte[] response = new byte[responseSize];

        response[0] = (byte) responseCode; // responde code

        response[1] = (byte) (0xFF & (responseSize >> 8)); // Size most significant byte
        response[2] = (byte) (0xFF & responseSize); // Size least significant byte

        // Body (0x48) / End-of-Body (0x49)
        int bodySize = 3 + contactsSize;
        response[3] = (byte) (0x49); // End-of-Body
        response[4] = (byte) (0xFF & (bodySize >> 8)); // Body section length
        response[5] = (byte) (0xFF & bodySize); // Body section length

        // Contact(s)
        System.arraycopy(contacts, 0, response, 6, contactsSize);

        Log.i("FakeObexServer", "Responding to client");
        Log.i("FakeObexServer", "    returnCode (1 byte)=" + responseCode);
        Log.i("FakeObexServer", "    packetSize (2 bytes)=" + responseSize);
        Log.i("FakeObexServer", "    headerEndOfBody (1 byte)=0x49");
        Log.i("FakeObexServer", "    bodySize (2 bytes)=" + bodySize);
        Log.i("FakeObexServer", "    contactSize (" + bodySize + " - 3)=" + contactsSize);
        Log.i("FakeObexServer", "    packet (" + response.length + " bytes)=" + Arrays.toString(response));

        mSendClientResponse.write(response);
        mSendClientResponse.flush();
    }

    private void verifyDisconnectRequest() throws IOException {
        Log.i("FakeObexServer", "Waiting on disconnect request from client");

        // Check request type (1 byte) - Disconnect is 0x81 / 129
        int requestType = mReceiveClientRequest.read();
        Log.i("FakeObexServer", "received request, type=" + requestType);

        // Read OBEX packet length (2 bytes), version (1 byte), flags (1 bytes) and max packet length (2 bytes)
        int packetLength = mReceiveClientRequest.read();
        packetLength = (packetLength << 8) + mReceiveClientRequest.read();

        Log.i("FakeObexServer", "    size=" + packetLength);
    }

    private void sendDisconnectResponse(int responseCode) throws IOException {
        Log.i("FakeObexServer", "Sending disconnect response");

        int responseSize = 3;
        byte[] response = new byte[responseSize];
        response[0] = (byte) responseCode; // responde code - 0xA0 (succeess) / 0xD3 (service unavailable)
        response[1] = (byte) (255 & (responseSize >> 8)); // Size most significant byte
        response[2] = (byte) (255 & responseSize); // Size least significant byte

        Log.i("FakeObexServer", "Responding to client");
        Log.i("FakeObexServer", "    code=" + responseCode);
        Log.i("FakeObexServer", "    size=3");
        Log.i("FakeObexServer", "    packet=" + Arrays.toString(response));

        mSendClientResponse.write(response);
        mSendClientResponse.flush();
    }

    public byte[] longToByteArray(long l) {
        ByteBuffer ret = ByteBuffer.allocate(16);
        ret.putLong(0); // Most significant bytes
        ret.putLong(l); // Least significant bytes
        return ret.array();
    }

    private String createPhonebook(List<String> vcardStrings) {
        StringBuilder sb = new StringBuilder();
        for (String vcard : vcardStrings) {
            sb.append(vcard).append("\n");
        }
        return sb.toString();
    }

    private static final String VERSION_21 = "2.1";
    private static final String VERSION_30 = "3.0";
    private static final String VERSION_UNSUPPORTED = "4.0";
    private static final String N = "N";
    private static final String FN = "FN";
    private static final String ADDR = "ADR;TYPE=HOME";
    private static final String CELL = "TEL;TYPE=CELL";
    private static final String EMAIL = "EMAIL;INTERNET";
    private static final String TEL = "TEL;TYPE=0";

    private String createVcard(String version, String first, String last, String phone, String addr, String email) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCARD\n");
        sb.append("VERSION:").append(version).append("\n");

        sb.append(FN).append(":").append(first).append(" ").append(last).append("\n");

        sb.append(N).append(":").append(last).append(";").append(first).append("\n");

        if (phone != null) {
            sb.append(CELL).append(":").append(phone).append("\n");
        }

        if (addr != null) {
            sb.append(ADDR).append(":").append(addr).append("\n");
        }

        if (email != null) {
            sb.append(EMAIL).append(":").append(email).append("\n");
        }

        sb.append("END:VCARD");

        return sb.toString();
    }
}
