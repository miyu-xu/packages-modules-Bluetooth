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
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.MatrixCursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.android.bluetooth.BluetoothMethodProxy;
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
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;import static com.google.common.truth.Truth.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeoutException;


@MediumTest
@RunWith(AndroidJUnit4.class)
public class BluetoothOppServiceMockTest {
    @Rule
    public final ServiceTestRule mServiceRule = new ServiceTestRule();
    @Mock
    BluetoothMethodProxy mMethodProxy;
    private BluetoothOppService mService = null;
    private BluetoothAdapter mAdapter = null;
    @Mock
    private AdapterService mAdapterService;

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue("Ignore test when BluetoothOppService is not enabled",
                BluetoothOppService.isEnabled());
        MockitoAnnotations.initMocks(this);
        TestUtils.setAdapterService(mAdapterService);
        doReturn(true, false).when(mAdapterService).isStartedProfile(anyString());
        TestUtils.startService(mServiceRule, BluetoothOppService.class);
        mService = BluetoothOppService.getBluetoothOppService();
        Assert.assertNotNull(mService);
        // Try getting the Bluetooth adapter
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        Assert.assertNotNull(mAdapter);

        BluetoothMethodProxy.setInstanceForTesting(mMethodProxy);

        // mocking notifier thread, which causes multi-thread mockito problems
        mService.mNotifier.mHandler.removeCallbacksAndMessages(null);
        mService.mNotifier.mNotificationMgr.cancelAll();
        mService.mNotifier = mock(BluetoothOppNotification.class);
        mService.mNotifier.mNotificationMgr = mock(NotificationManager.class);
        mService.mNotifier.mHandler = mock(Handler.class);
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
    public void scanFileIfNeeded_shouldNotThrow() {
        Uri uri = Uri.parse("file://Idontknow//Justmadeitup");
        String hintString = "this is a object that take 4 bytes";
        String infoFileName = "random.jpg";
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

        BluetoothOppShareInfo info = new BluetoothOppShareInfo(0, uri, hintString, infoFileName,
                mimetype, direction, destination, visibility, confirm, status, totalBytes,
                currentBytes, timestamp, mediaScanned);

        mService.mShares.add(info);
        // To avoid really scan the file
        mService.mMediaScanInProgress = true;
        mService.scanFileIfNeeded(mService.mShares.size() - 1);
        // Can't find a good way to test if this function worked properly
    }

    @Test
    public void mediaScannerNotifier_OnMediaScannerConnected_callsScanFile() {
        Uri uri = Uri.parse("file://Idontknow//Justmadeitup");
        String hintString = "this is a object that take 4 bytes";
        String infoFileName = "random.jpg";
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

        BluetoothOppShareInfo info = new BluetoothOppShareInfo(0, uri, hintString, infoFileName,
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
        String infoFileName = "random.jpg";
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

        BluetoothOppShareInfo info = new BluetoothOppShareInfo(0, uri, hintString, infoFileName,
                mimetype, direction, destination, visibility, confirm, status, totalBytes,
                currentBytes, timestamp, mediaScanned);

        MediaScannerConnection mockConnection = mock(MediaScannerConnection.class);
        BluetoothOppService.MediaScannerNotifier notifier =
                new BluetoothOppService.MediaScannerNotifier(mService, info, mService.mHandler,
                        mockConnection);
        notifier.onScanCompleted("content:///Not//important", null);
        verify(mMethodProxy, timeout(3_000)).contentResolverUpdate(any(),
                any(), argThat(arg -> arg.getAsInteger(Constants.MEDIA_SCANNED) == Constants.MEDIA_SCANNED_SCANNED_FAILED), any(), any());
    }

    @Test
    public void mediaScannerNotifier_onScanCompleted_sendsMediaScanned() {
        Uri uri = Uri.parse("file://Idontknow//Justmadeitup");
        String hintString = "this is a object that take 4 bytes";
        String infoFileName = "random.jpg";
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

        BluetoothOppShareInfo info = new BluetoothOppShareInfo(0, uri, hintString, infoFileName,
                mimetype, direction, destination, visibility, confirm, status, totalBytes,
                currentBytes, timestamp, mediaScanned);

        MediaScannerConnection mockConnection = mock(MediaScannerConnection.class);
        BluetoothOppService.MediaScannerNotifier notifier =
                new BluetoothOppService.MediaScannerNotifier(mService, info, mService.mHandler,
                        mockConnection);
        notifier.onScanCompleted("content:///Not//important", uri);

        verify(mMethodProxy, timeout(3_000)).contentResolverUpdate(any(),
                any(), argThat(arg -> arg.getAsInteger(Constants.MEDIA_SCANNED) == Constants.MEDIA_SCANNED_SCANNED_OK), any(), any());
    }

    @Test
    public void deleteShare() {
        Uri uri = Uri.parse("file://Idontknow//Justmadeitup");
        String hintString = "this is a object that take 4 bytes";
        String infoFileName = "random.jpg";
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

        BluetoothOppShareInfo info = new BluetoothOppShareInfo(0, uri, hintString, infoFileName,
                mimetype, direction, destination, visibility, confirm, status, totalBytes,
                currentBytes, timestamp, mediaScanned);

        mService.mShares.add(info);

        // should not throw
        mService.dump(new StringBuilder());
    }

    @Test
    public void dump_shouldNotThrow() {
        Uri uri = Uri.parse("file://Idontknow//Justmadeitup");
        String hintString = "this is a object that take 4 bytes";
        String infoFileName = "random.jpg";
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

        BluetoothOppShareInfo info = new BluetoothOppShareInfo(0, uri, hintString, infoFileName,
                mimetype, direction, destination, visibility, confirm, status, totalBytes,
                currentBytes, timestamp, mediaScanned);

        mService.mShares.add(info);

        // should not throw
        mService.dump(new StringBuilder());
    }

    @Test
    public void insertShare_withDirectionOutBoundButNoSendFileInfo_updateContentAsBadRequest() {
        long timestamp = 10L;
        int status = BluetoothShare.STATUS_PENDING;
        int dir = BluetoothShare.DIRECTION_OUTBOUND;
        int id = 0;
        long total = 200;
        long current = 100;
        int confirmation = BluetoothShare.USER_CONFIRMATION_CONFIRMED;
        String fileName = "random.txt";
        String hint = "file:///hello//kitty//random.txt";
        String mimeType = "text/plain";
        String destination = "AA:BB:CC:DD:EE:FF";
        int visibility = BluetoothShare.VISIBILITY_VISIBLE;
        int mediaScanned = Constants.MEDIA_SCANNED_NOT_SCANNED;
        Uri uri = Uri.parse("content:///abc/xyz");
        MatrixCursor cursor = new MatrixCursor(new String[]{
                BluetoothShare.TIMESTAMP, BluetoothShare.DIRECTION, BluetoothShare._ID,
                BluetoothShare.TOTAL_BYTES, BluetoothShare.CURRENT_BYTES, BluetoothShare._DATA,
                BluetoothShare.FILENAME_HINT, BluetoothShare.USER_CONFIRMATION,
                BluetoothShare.DESTINATION, BluetoothShare.STATUS, BluetoothShare.MIMETYPE,
                BluetoothShare.VISIBILITY, Constants.MEDIA_SCANNED, BluetoothShare.URI
        });
        cursor.addRow(new Object[]{
                timestamp, dir, id, total, current, fileName, hint, confirmation, destination,
                status, mimeType, visibility, mediaScanned, uri
        });
        cursor.moveToFirst();

        mService.insertShare(cursor, 0);
        Log.d("TestRunner", "method proxy is " + mMethodProxy + " " + BluetoothMethodProxy.getInstance() + " " + (mMethodProxy == BluetoothMethodProxy.getInstance()));
        verify(mMethodProxy).contentResolverUpdate(any(),
                any(), argThat(arg -> arg.getAsInteger(BluetoothShare.STATUS) == BluetoothShare.STATUS_BAD_REQUEST), any(), any());
    }

    @Test
    public void insertShare_withDirectionInBound_addShareCorrectly() {
        long timestamp = 10L;
        int status = BluetoothShare.STATUS_PENDING;
        int dir = BluetoothShare.DIRECTION_INBOUND;
        int id = 0;
        long total = 200;
        long current = 100;
        int confirmation = BluetoothShare.USER_CONFIRMATION_CONFIRMED;
        String fileName = "random.txt";
        String hint = "file:///hello//kitty//random.txt";
        String mimeType = "text/plain";
        String destination = "AA:BB:CC:DD:EE:FF";
        int visibility = BluetoothShare.VISIBILITY_VISIBLE;
        int mediaScanned = Constants.MEDIA_SCANNED_NOT_SCANNED;
        Uri uri = Uri.parse("content:///abc/xyz");
        MatrixCursor cursor = new MatrixCursor(new String[]{
                BluetoothShare.TIMESTAMP, BluetoothShare.DIRECTION, BluetoothShare._ID,
                BluetoothShare.TOTAL_BYTES, BluetoothShare.CURRENT_BYTES, BluetoothShare._DATA,
                BluetoothShare.FILENAME_HINT, BluetoothShare.USER_CONFIRMATION,
                BluetoothShare.DESTINATION, BluetoothShare.STATUS, BluetoothShare.MIMETYPE,
                BluetoothShare.VISIBILITY, Constants.MEDIA_SCANNED, BluetoothShare.URI
        });
        cursor.addRow(new Object[]{
                timestamp, dir, id, total, current, fileName, hint, confirmation, destination,
                status, mimeType, visibility, mediaScanned, uri
        });
        cursor.moveToFirst();

        // prevent BluetoothOppTranfer object to actually be started
        doReturn(false).when(mMethodProxy).bluetoothAdapterIsEnabled(mAdapter);

        int size = mService.mBatches.size();

        mService.insertShare(cursor, 0);

        assertThat(mService.mBatches.size() - 1).isEqualTo(size);
    }

    @Test
    public void updateShare_removeFinishedShare() {
        long timestamp = 10L;
        int status = BluetoothShare.STATUS_PENDING;
        int dir = BluetoothShare.DIRECTION_INBOUND;
        int id = 0;
        long total = 200;
        long current = 100;
        int confirmation = BluetoothShare.USER_CONFIRMATION_CONFIRMED;
        String fileName = "random.txt";
        String hint = "file:///hello//kitty//random.txt";
        String mimeType = "text/plain";
        String destination = "AA:BB:CC:DD:EE:FF";
        int visibility = BluetoothShare.VISIBILITY_VISIBLE;
        int mediaScanned = Constants.MEDIA_SCANNED_NOT_SCANNED;
        Uri uri = Uri.parse("content:///abc/xyz");
        MatrixCursor cursor = new MatrixCursor(new String[]{
                BluetoothShare.TIMESTAMP, BluetoothShare.DIRECTION, BluetoothShare._ID,
                BluetoothShare.TOTAL_BYTES, BluetoothShare.CURRENT_BYTES, BluetoothShare._DATA,
                BluetoothShare.FILENAME_HINT, BluetoothShare.USER_CONFIRMATION,
                BluetoothShare.DESTINATION, BluetoothShare.STATUS, BluetoothShare.MIMETYPE,
                BluetoothShare.VISIBILITY, Constants.MEDIA_SCANNED, BluetoothShare.URI
        });
        cursor.addRow(new Object[]{
                timestamp, dir, id, total, current, fileName, hint, confirmation, destination,
                status, mimeType, visibility, mediaScanned, uri
        });
        cursor.moveToFirst();

        // prevent BluetoothOppTranfer object to actually be started
        doReturn(false).when(mMethodProxy).bluetoothAdapterIsEnabled(mAdapter);


        Uri infoUri = Uri.parse("file://Idontknow//Justmadeitup");
        String hintString = "this is a object that take 4 bytes";
        String infoFileName = "random.jpg";
        String mimetype = "image/jpeg";
        int infoDir = BluetoothShare.DIRECTION_OUTBOUND;
        String infoDes = "01:23:45:67:89:AB";
        int infoVisibility = BluetoothShare.VISIBILITY_VISIBLE;
        int infoConfirm = BluetoothShare.USER_CONFIRMATION_PENDING;
        int infoStatus = BluetoothShare.STATUS_SUCCESS;
        int totalBytes = 1023;
        int currentBytes = 42;
        int infoTimestamp = 123456789;
        boolean infoScanned = false;

        BluetoothOppShareInfo info = new BluetoothOppShareInfo(0, infoUri, hintString, infoFileName,
                mimetype, infoDir, infoDes, infoVisibility, infoConfirm, infoStatus, totalBytes,
                currentBytes, infoTimestamp, infoScanned);
        BluetoothOppShareInfo info2 = new BluetoothOppShareInfo(0, infoUri, hintString, infoFileName,
                mimetype, infoDir, infoDes, infoVisibility, infoConfirm, infoStatus, totalBytes,
                currentBytes, timestamp, infoScanned);

        mService.mShares.clear();
        mService.mShares.add(info2);
        mService.mShares.add(info);

        // batch1 will be remove and batch2 will start
        BluetoothOppBatch batch1 = new BluetoothOppBatch(mService, info);
        BluetoothOppBatch batch2 = new BluetoothOppBatch(mService, info2);
        batch2.mStatus = Constants.BATCH_STATUS_FINISHED;
        mService.mBatches.clear();
        mService.mBatches.add(batch1);
        mService.mBatches.add(batch2);

        // removing batch2
        mService.updateShare(cursor, 0);

        assertThat(mService.mBatches.size()).isEqualTo(1);

        // check if the first share is updated
        assertThat(mService.mShares.get(0).mFilename).isEqualTo(fileName);
        assertThat(mService.mShares.get(0).mStatus).isEqualTo(status);
        assertThat(mService.mShares.get(0).mDirection).isEqualTo(dir);
    }
}
