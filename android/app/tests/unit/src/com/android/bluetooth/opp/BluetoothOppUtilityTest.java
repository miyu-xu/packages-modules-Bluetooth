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

package com.android.bluetooth.opp;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class BluetoothOppUtilityTest {

    private static final Uri CORRECT_FORMAT_URI = Uri.parse(
            "content://com.android.bluetooth.opp/btopp/0123455343467");
    private static final Uri INCORRECT_FORMAT_URI = Uri.parse("www.google.com");

    @Spy
    Context mContext;

    @Mock
    Cursor mCursor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.mContext = spy(
                InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext());
    }

    @Test
    public void shareUri_workNormally() {
        assertThat(BluetoothOppUtility.isBluetoothShareUri(INCORRECT_FORMAT_URI)).isFalse();
        assertThat(BluetoothOppUtility.isBluetoothShareUri(CORRECT_FORMAT_URI)).isTrue();
    }

    @Test
    public void queryRecord_returnNull() {
        assertThat(BluetoothOppUtility.queryRecord(mContext,
                CORRECT_FORMAT_URI)).isNull();
    }

    @Test
    public void fillRecord_filledAllProperties() {
        String destination = "AA:BB:CC:00:11:22";
        doAnswer(invocation -> invocation.getArgument(0)).when(mCursor).getInt(anyInt());
        doAnswer(invocation -> invocation.getArgument(0)).when(mCursor).getLong(anyInt());
        doAnswer(invocation -> invocation.getArgument(0).toString().length()).when(
                mCursor).getColumnIndexOrThrow(anyString());
        doReturn(null).when(mCursor).getString(anyInt());

        doReturn(-100).when(mCursor).getColumnIndexOrThrow(BluetoothShare.DESTINATION);
        doReturn(destination).when(mCursor).getString(-100);

        BluetoothOppTransferInfo info = mock(BluetoothOppTransferInfo.class);
        BluetoothOppUtility.fillRecord(mContext, mCursor, info);

        assertThat(info.mID).isEqualTo(BluetoothShare._ID.length());
        assertThat(info.mStatus).isEqualTo(BluetoothShare.STATUS.length());
        assertThat(info.mDirection).isEqualTo(BluetoothShare.DIRECTION.length());
        assertThat(info.mTotalBytes).isEqualTo(BluetoothShare.TOTAL_BYTES.length());
        assertThat(info.mCurrentBytes).isEqualTo(BluetoothShare.CURRENT_BYTES.length());
        assertThat(info.mTimeStamp).isEqualTo(BluetoothShare.TIMESTAMP.length());
        assertThat(info.mDestAddr).isEqualTo(destination);
        assertThat(info.mFileUri).isNull();
        assertThat(info.mFileType).isNull();
        assertThat(info.mDeviceName).isEqualTo("Unknown device");
        assertThat(info.mHandoverInitiated).isEqualTo(false);
        assertThat(info.mFileName).isEqualTo("Unknown file");
    }

    @Test
    public void fileExists_returnFalse() {
        assertThat(
                BluetoothOppUtility.fileExists(mContext, CORRECT_FORMAT_URI)).isFalse();
    }

    @Test
    public void isRecognizedFileType_withWrongFileUriAndMimeType_returnFalse() {
        assertThat(
                BluetoothOppUtility.isRecognizedFileType(mContext, CORRECT_FORMAT_URI,
                        "aWrongMimeType")).isFalse();
    }

    @Test
    public void formatProgressText() {
        assertThat(BluetoothOppUtility.formatProgressText(100, 42)).isEqualTo("42%");
    }

    @Test
    public void formatResultText() {
        assertThat(BluetoothOppUtility.formatResultText(1, 2, mContext)).isEqualTo(
                "1 successful, 2 unsuccessful.");
    }

    @Test
    public void getStatusDescription_workCorrectly() {
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_PENDING, "randomName")).isEqualTo(
                "File transfer not started yet.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_RUNNING, "randomName")).isEqualTo(
                "File transfer is ongoing.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_SUCCESS, "randomName")).isEqualTo(
                "File transfer completed successfully.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_NOT_ACCEPTABLE, "randomName")).isEqualTo(
                "Content isn\'t supported.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_FORBIDDEN, "randomName")).isEqualTo(
                "Transfer forbidden by target device.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_CANCELED, "randomName")).isEqualTo(
                "Transfer canceled by user.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_FILE_ERROR, "randomName")).isEqualTo("Storage issue.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_CONNECTION_ERROR, "randomName")).isEqualTo(
                "Connection unsuccessful.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_ERROR_NO_SDCARD, "randomName")).isAnyOf(
                "No SD card. Insert an SD card to save transferred files.", "No USB storage.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_ERROR_SDCARD_FULL, "randomName")).isAnyOf(
                "There isn\'t enough space in USB storage to save the file.",
                "There isn\'t enough space on the SD card to save the file.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_BAD_REQUEST, "randomName")).isEqualTo(
                "Request can\'t be handled correctly.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext, 12345465,
                "randomName")).isEqualTo("Unknown error.");
    }

    @Test
    public void originalUri_workCorrectly() {
        assertThat(BluetoothOppUtility.originalUri(
                Uri.parse("com.android.bluetooth.opp.BluetoothOppSendFileInfo@dfe15a6"))).isEqualTo(
                Uri.parse("com.android.bluetooth.opp.BluetoothOppSendFileInfo"));
    }

    @Test
    public void fileInfo_testFileInfoFunctions() {
        assertThat(BluetoothOppUtility.getSendFileInfo(CORRECT_FORMAT_URI)).isEqualTo(
                BluetoothOppSendFileInfo.SEND_FILE_INFO_ERROR);
        assertThat(BluetoothOppUtility.generateUri(CORRECT_FORMAT_URI,
                BluetoothOppSendFileInfo.SEND_FILE_INFO_ERROR).toString()).contains(
                CORRECT_FORMAT_URI.toString());
        try {
            BluetoothOppUtility.putSendFileInfo(CORRECT_FORMAT_URI,
                    BluetoothOppSendFileInfo.SEND_FILE_INFO_ERROR);
            BluetoothOppUtility.closeSendFileInfo(CORRECT_FORMAT_URI);
        } catch (Exception e) {
            assertWithMessage("Exception should not happen.").fail();
        }
    }

}
