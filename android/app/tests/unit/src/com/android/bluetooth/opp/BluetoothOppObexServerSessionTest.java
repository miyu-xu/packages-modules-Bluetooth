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

package com.android.bluetooth.opp;

import android.content.Context;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.bluetooth.BluetoothMethodProxy;
import com.android.bluetooth.BluetoothObexTransport;
import com.android.obex.ObexTransport;
import com.android.obex.Operation;
import com.android.obex.ReponseCodes;
import com.android.obex.HeaderSet;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


@RunWith(AndroidJUnit4.class)
public class BluetoothOppObexServerSessionTest {
    @Mock
    BluetoothMethodProxy mMethodProxy;

    Context mTargetContext;
    @Mock
    BluetoothObexTransport mTransport;

    @Mock
    BluetoothOppService mBluetoothOppService;
    @Mock
    Operation mOperation;

    BluetoothOppObexServerSession mServerSession;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        mTargetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mServerSession = new BluetoothOppObexServerSession(mTargetContext, mTransport, mBluetoothOppService);

        // to control the mServerSession.mSession
        InputStream input = mock(InputStream.class);
        OutputStream output = mock(OutputStream.class);
        doReturn(-1).when(input).read();
        doReturn(input).when(mTransport).openInputStream();
        doReturn(output).when(mTransport).openOutputStream();

        BluetoothMethodProxy.setInstanceForTesting(mMethodProxy);
    }

    @After
    public void tearDown() {
        BluetoothMethodProxy.setInstanceForTesting(null);
    }

    @Test
    public void constructor_createInstanceCorrectly() {
        assertThat(mServerSession.mBluetoothOppService).isEqualTo(mBluetoothOppService);
        assertThat(mServerSession.mTransport).isEqualTo(mTransport);
        assertThat(mServerSession.mContext).isEqualTo(mTargetContext);
    }

    @Test
    public void unblock_unblockCorrectly() {
        assertThat(mServerSession.mServerBlocking).isTrue();
        mServerSession.unblock();
        assertThat(mServerSession.mServerBlocking).isFalse();
    }

    @Test
    public void preStart_thenStart_thenStop_flowWorksCorrectly() {
        assertThat(mServerSession.mSession).isNull();
        assertThat(mServerSession.mCallback).isNull();
        mServerSession.preStart();
        assertThat(mServerSession.mSession).isNotNull();
        assertThat(mServerSession.mCallback).isNull();
        mServerSession.start(new Handler(false), 0);
        assertThat(mServerSession.mSession).isNotNull();
        assertThat(mServerSession.mCallback).isNotNull();
        mServerSession.stop();
        assertThat(mServerSession.mSession).isNull();
        assertThat(mServerSession.mCallback).isNull();
    }

    @Test
    public void addShare_shareAddedCorrectly() {
        Uri uri = Uri.parse("file://Idontknow//Justmadeitup");
        String hintString = "this is a object that take 4 bytes";
        String filename = "random.jpg";
        String mimetype = "image/jpeg";
        int direction = BluetoothShare.DIRECTION_INBOUND;
        String destination = "01:23:45:67:89:AB";
        int visibility = BluetoothShare.VISIBILITY_VISIBLE;
        int confirm = BluetoothShare.USER_CONFIRMATION_CONFIRMED;
        int status = BluetoothShare.STATUS_PENDING;
        int totalBytes = 1023;
        int currentBytes = 42;
        int timestamp = 123456789;
        boolean mediaScanned = false;
        BluetoothOppShareInfo info = new BluetoothOppShareInfo(0, uri, hintString, filename,
                mimetype, direction, destination, visibility, confirm, status, totalBytes,
                currentBytes, timestamp, mediaScanned);

        mServerSession.addShare(info);
        assertThat(mServerSession.mInfo).isEqualTo(info);
    }

    @Test
    public void onPut_userDenied_returnObexHttpForbidden() {
        mServerSession.mAccepted = BluetoothShare.USER_CONFIRMATION_DENIED;
        assertThat(mServerSession.onPut(mOperation)).isEqualTo(ReponseCodes.OBEX_HTTP_FORBIDDEN);
    }

    @Test
    public void onPut_userDenied_returnObexHttpBadRequest() {
        doThrow(new IOException()).when(mOperation).getReceivedHeader();
        assertThat(mServerSession.onPut(mOperation)).isEqualTo(ReponseCodes.OBEX_HTTP_BAD_REQUEST);
    }

    @Test
    public void onPut_zeroLengthHeader_returnLengthRequired() {
        HeaderSet headerSet = mock(HeaderSet.class);
        String name = "";
        int length = 0;
        String mimeType = "text/plain";
        doReturn(headerSet).when(mOperation).getReceivedHeader();
        doReturn(name).when(headerSet).getHeader(HeaderSet.NAME);
        doReturn(length).when(headerSet).getHeader(HeaderSet.LENGTH);
        doReturn(mimeType).when(headerSet).getHeader(HeaderSet.TYPE);
        assertThat(mServerSession.onPut(mOperation)).isEqualTo(ReponseCodes.OBEX_HTTP_LENGTH_REQUIRED);
    }

    @Test
    public void onPut_zeroLengthName_returnHttpBadRequest() {
        HeaderSet headerSet = mock(HeaderSet.class);
        String name = "";
        int length = 10;
        String mimeType = "text/plain";
        doReturn(headerSet).when(mOperation).getReceivedHeader();
        doReturn(name).when(headerSet).getHeader(HeaderSet.NAME);
        doReturn(length).when(headerSet).getHeader(HeaderSet.LENGTH);
        doReturn(mimeType).when(headerSet).getHeader(HeaderSet.TYPE);
        assertThat(mServerSession.onPut(mOperation)).isEqualTo(ReponseCodes.OBEX_HTTP_BAD_REQUEST);
    }

    @Test
    public void onPut_noMimeType_returnHttpBadRequest() {
        HeaderSet headerSet = mock(HeaderSet.class);
        String name = "randomFile";
        int length = 10;
        String mimeType = null;
        doReturn(headerSet).when(mOperation).getReceivedHeader();
        doReturn(name).when(headerSet).getHeader(HeaderSet.NAME);
        doReturn(length).when(headerSet).getHeader(HeaderSet.LENGTH);
        doReturn(mimeType).when(headerSet).getHeader(HeaderSet.TYPE);
        assertThat(mServerSession.onPut(mOperation)).isEqualTo(ReponseCodes.OBEX_HTTP_BAD_REQUEST);
    }

    @Test
    public void onPut_unsupportedMimeType_returnHttpBadRequest() {
        HeaderSet headerSet = mock(HeaderSet.class);
        String name = "randomFile.3danimation";
        int length = 10;
        String mimeType = "3danimation/superultrasonic";
        doReturn(headerSet).when(mOperation).getReceivedHeader();
        doReturn(name).when(headerSet).getHeader(HeaderSet.NAME);
        doReturn(length).when(headerSet).getHeader(HeaderSet.LENGTH);
        doReturn(mimeType).when(headerSet).getHeader(HeaderSet.TYPE);
        assertThat(mServerSession.onPut(mOperation)).isEqualTo(ReponseCodes.OBEX_HTTP_UNSUPPORTED_TYPE);
    }

    @Test
    public void onPut_unsupportedMimeType_returnHttpBadRequest() {
        // The flow of this test is as follow
        // onPut(mOperation) -> check many fileName, length, mimeType from op.getReceivedHeader()
        // insert the newly received info into ContentResolver
        // skip mFileInfo related code, then return ResponseCodes.OBEX_HTTP_OK

        Assume.assumeTrue("Ignore test when if there is not media mounted",
                Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED));
        HeaderSet headerSet = mock(HeaderSet.class);
        String name = "randomFile.txt";
        int length = 10;
        String mimeType = "text/plain";
        String contentUri = BluetoothShare.CONTENT_URI + "/1";
        doReturn(headerSet).when(mOperation).getReceivedHeader();
        doReturn(name).when(headerSet).getHeader(HeaderSet.NAME);
        doReturn(length).when(headerSet).getHeader(HeaderSet.LENGTH);
        doReturn(mimeType).when(headerSet).getHeader(HeaderSet.TYPE);

        mServerSession.unblock();
        mServerSession.mAccepted = BluetoothShare.USER_CONFIRMATION_CONFIRMED;
        doReturn(contentUri).when(mMethodProxy).contentResolverInsert(any(), eq(BluetoothShare.CONTENT_URI), any());

        // unblocking the session
        Handler handler = mock(Handler.class);
        mServerSession.start(handler, 0);
        doAnswer(arg -> {
            mServerSession.unblock();
            return true;
        }).when(handler).sendMessageDelayed(argThat(arg -> arg.what == BluetoothOppObexSession.MSG_CONNECT_TIMEOUT), any());

        assertThat(mServerSession.onPut(mOperation)).isEqualTo(ReponseCodes.OBEX_HTTP_OK);
    }
}
