/*
 * Copyright 2018 The Android Open Source Project
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

import static com.android.bluetooth.opp.BluetoothOppService.MEDIA_SCANNED;
import static com.android.bluetooth.opp.BluetoothOppService.MEDIA_SCANNED_FAILED;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;

import androidx.test.filters.MediumTest;
import androidx.test.rule.ServiceTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.BluetoothMethodProxy;
import com.android.bluetooth.R;
import com.android.bluetooth.TestUtils;
import com.android.bluetooth.btservice.AdapterService;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class BluetoothOppServiceTest {
    private BluetoothOppService mService = null;
    private BluetoothAdapter mAdapter = null;

    @Rule
    public final ServiceTestRule mServiceRule = new ServiceTestRule();

    @Mock
    private AdapterService mAdapterService;

    @Mock
    BluetoothMethodProxy mMethodProxy;

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue("Ignore test when BluetoothOppService is not enabled",
                BluetoothOppService.isEnabled());
        BluetoothMethodProxy.setInstanceForTesting(mMethodProxy);
        MockitoAnnotations.initMocks(this);
        TestUtils.setAdapterService(mAdapterService);
        doReturn(true, false).when(mAdapterService).isStartedProfile(anyString());
        TestUtils.startService(mServiceRule, BluetoothOppService.class);
        mService = spy(BluetoothOppService.getBluetoothOppService());
        Assert.assertNotNull(mService);
        // Try getting the Bluetooth adapter
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        Assert.assertNotNull(mAdapter);
    }

    @After
    public void tearDown() throws Exception {
        BluetoothMethodProxy.setInstanceForTesting(null);
        if (!BluetoothOppService.isEnabled()) {
            return;
        }
        TestUtils.stopService(mServiceRule, BluetoothOppService.class);
        TestUtils.clearAdapterService(mAdapterService);
    }

    @Test
    public void testInitialize() {
        Assert.assertNotNull(BluetoothOppService.getBluetoothOppService());
    }

    @Test
    public void scanFileIfNeeded_doesNotScan() {
        Uri uri = Uri.parse("file://Idontknow//Justmadeitup");
        String hintString = "this is a object that take 4 bytes";
        String filename = "random.jpg";
        String mimetype = "image/jpeg";
        int direction = BluetoothShare.DIRECTION_INBOUND;
        String destination = "01:23:45:67:89:AB";
        int visibility = BluetoothShare.VISIBILITY_VISIBLE;
        int confirm = BluetoothShare.USER_CONFIRMATION_CONFIRMED;
        int status = BluetoothShare.STATUS_SUCCESS;
        int totalBytes = 1023;
        int currentBytes = 42;
        int timestamp = 123456789;
        boolean mediaScanned = false;

        BluetoothOppShareInfo info = new BluetoothOppShareInfo(0, uri, hintString, filename,
                mimetype, direction, destination, visibility, confirm, status, totalBytes,
                currentBytes, timestamp, mediaScanned);

        mService.mShares.add(info);
        // To avoid really scan the file
        mService.mMediaScanInProgress = true;
        mService.scanFileIfNeeded(mService.mShares.size() - 1);

        // verify that it doesn't scan the file
        verify(mService).scanFileIfNeeded(anyInt());
        verifyNoMoreInteractions(mService);
    }

    @Test
    public void mediaScannerNotifier_OnMediaScannerConnected_callsScanFile() {
        Uri uri = Uri.parse("file://Idontknow//Justmadeitup");
        String hintString = "this is a object that take 4 bytes";
        String filename = "random.jpg";
        String mimetype = "image/jpeg";
        int direction = BluetoothShare.DIRECTION_INBOUND;
        String destination = "01:23:45:67:89:AB";
        int visibility = BluetoothShare.VISIBILITY_VISIBLE;
        int confirm = BluetoothShare.USER_CONFIRMATION_CONFIRMED;
        int status = BluetoothShare.STATUS_SUCCESS;
        int totalBytes = 1023;
        int currentBytes = 42;
        int timestamp = 123456789;
        boolean mediaScanned = false;

        BluetoothOppShareInfo info = new BluetoothOppShareInfo(0, uri, hintString, filename,
                mimetype, direction, destination, visibility, confirm, status, totalBytes,
                currentBytes, timestamp, mediaScanned);

        MediaScannerConnection mockConnection = mock(MediaScannerConnection.class);

        BluetoothOppService.MediaScannerNotifier notifier =
                new BluetoothOppService.MediaScannerNotifier(mService, info, null, mockConnection);
        notifier.onMediaScannerConnected();
        verify(mockConnection).scanFile(eq(info.mFilename), any());
    }

    @Test
    public void mediaScannerNotifier_onScanCompletedWithNullUri_sendsMediaScannedFailed() {
        Uri uri = Uri.parse("file://Idontknow//Justmadeitup");
        String hintString = "this is a object that take 4 bytes";
        String filename = "random.jpg";
        String mimetype = "image/jpeg";
        int direction = BluetoothShare.DIRECTION_INBOUND;
        String destination = "01:23:45:67:89:AB";
        int visibility = BluetoothShare.VISIBILITY_VISIBLE;
        int confirm = BluetoothShare.USER_CONFIRMATION_CONFIRMED;
        int status = BluetoothShare.STATUS_SUCCESS;
        int totalBytes = 1023;
        int currentBytes = 42;
        int timestamp = 123456789;
        boolean mediaScanned = false;

        BluetoothOppShareInfo info = new BluetoothOppShareInfo(0, uri, hintString, filename,
                mimetype, direction, destination, visibility, confirm, status, totalBytes,
                currentBytes, timestamp, mediaScanned);

        MediaScannerConnection mockConnection = mock(MediaScannerConnection.class);

        Handler handler = spy(mService.mHandler);
        BluetoothOppService.MediaScannerNotifier notifier =
                new BluetoothOppService.MediaScannerNotifier(mService, info, handler, mockConnection);
        notifier.onScanCompleted("content:///Not//important", null);
        verify(handler, timeout(3_000).atLeastOnce()).handleMessage(argThat(arg -> arg.what == MEDIA_SCANNED_FAILED));
    }

    @Test
    public void mediaScannerNotifier_onScanCompleted_sendsMediaScanned() {
        Uri uri = Uri.parse("file://Idontknow//Justmadeitup");
        String hintString = "this is a object that take 4 bytes";
        String filename = "random.jpg";
        String mimetype = "image/jpeg";
        int direction = BluetoothShare.DIRECTION_INBOUND;
        String destination = "01:23:45:67:89:AB";
        int visibility = BluetoothShare.VISIBILITY_VISIBLE;
        int confirm = BluetoothShare.USER_CONFIRMATION_CONFIRMED;
        int status = BluetoothShare.STATUS_SUCCESS;
        int totalBytes = 1023;
        int currentBytes = 42;
        int timestamp = 123456789;
        boolean mediaScanned = false;

        BluetoothOppShareInfo info = new BluetoothOppShareInfo(0, uri, hintString, filename,
                mimetype, direction, destination, visibility, confirm, status, totalBytes,
                currentBytes, timestamp, mediaScanned);

        MediaScannerConnection mockConnection = mock(MediaScannerConnection.class);

        Handler handler = spy(mService.mHandler);
        BluetoothOppService.MediaScannerNotifier notifier =
                new BluetoothOppService.MediaScannerNotifier(mService, info, handler, mockConnection);
        notifier.onScanCompleted("content:///Not//important", uri);
        verify(handler, timeout(3_000)).handleMessage(argThat(arg -> arg.what == MEDIA_SCANNED));
    }
}
