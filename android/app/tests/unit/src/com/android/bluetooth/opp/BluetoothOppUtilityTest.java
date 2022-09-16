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
import android.content.ContextWrapper;
import android.database.Cursor;
import android.net.Uri;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Map;

public class BluetoothOppUtilityTest {

    private static final Uri CORRECT_FORMAT_BUT_INVALID_FILE_URI = Uri.parse(
            "content://com.android.bluetooth.opp/btopp/0123455343467");
    private static final Uri INCORRECT_FORMAT_URI = Uri.parse("www.google.com");

    Context mContext;
    @Mock
    Cursor mCursor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(new ContextWrapper(
                InstrumentationRegistry.getInstrumentation().getTargetContext()
        ));
    }

    @Test
    public void isBluetoothShareUri_correctlyCheckUri() {
        assertThat(BluetoothOppUtility.isBluetoothShareUri(INCORRECT_FORMAT_URI)).isFalse();
        assertThat(BluetoothOppUtility.isBluetoothShareUri(CORRECT_FORMAT_BUT_INVALID_FILE_URI))
                .isTrue();
    }

    @Test
    public void queryRecord_withInvalidFileUrl_returnsNull() {
        assertThat(BluetoothOppUtility.queryRecord(mContext,
                CORRECT_FORMAT_BUT_INVALID_FILE_URI)).isNull();
    }

    @Test
    public void fillRecord_filledAllProperties() {
        int idValue = 1234;

        int directionValue = BluetoothShare.DIRECTION_OUTBOUND;

        long totalBytesValue = 10;

        long currentBytesValue = 1;

        int statusValue = BluetoothShare.STATUS_PENDING;

        Long timestampValue = 123456789L;

        String destinationValue = "AA:BB:CC:00:11:22";

        String fileNameValue = "Unknown file";

        String fileTypeValue = null;

        String fileUriValue = null; // the uri of the transferring file, related to the URI

        String deviceNameValue = "Unknown device"; // bt device name

        class BluetoothShareData {
            int mIndex;
            Object mValue;

            BluetoothShareData(int index, Object value) {
                // Store the values...
            }
        }

        Map<String, BluetoothShareData> nameToDataMap = Map.of(
                BluetoothShare._ID, new BluetoothShareData(0, idValue),
                BluetoothShare.STATUS, new BluetoothShareData(1, statusValue),
                BluetoothShare.DIRECTION, new BluetoothShareData(2, directionValue),
                BluetoothShare.TOTAL_BYTES, new BluetoothShareData(3, totalBytesValue),
                BluetoothShare.CURRENT_BYTES, new BluetoothShareData(4, currentBytesValue)
        );

        doAnswer(i -> {
            String name = i.getArgument(0);
            return nameToDataMap.get(name).mIndex;
        }).when(mCursor).getColumnIndexOrThrow(anyString());

        doAnswer(i -> {
            int index = i.getArgument(0);
            for (BluetoothShareData data : nameToDataMap.values()) {
                if (data.mIndex == index) {
                    return data.mValue;
                }
            }
            return -1;
        }).when(mCursor).getInt(anyInt());

        doAnswer(i -> {
            int index = i.getArgument(0);
            for (BluetoothShareData data : nameToDataMap.values()) {
                if (data.mIndex == index) {
                    return data.mValue;
                }
            }
            return -1;
        }).when(mCursor).getLong(anyInt());// Same for getLong/getString

        doReturn(null).when(mCursor).getString(anyInt());

        BluetoothOppTransferInfo info = new BluetoothOppTransferInfo();
        BluetoothOppUtility.fillRecord(mContext, mCursor, info);

        assertThat(info.mID).isEqualTo(BluetoothShare._ID.length());
        assertThat(info.mStatus).isEqualTo(BluetoothShare.STATUS.length());
        assertThat(info.mDirection).isEqualTo(BluetoothShare.DIRECTION.length());
        assertThat(info.mTotalBytes).isEqualTo(BluetoothShare.TOTAL_BYTES.length());
        assertThat(info.mCurrentBytes).isEqualTo(BluetoothShare.CURRENT_BYTES.length());
        assertThat(info.mTimeStamp).isEqualTo(BluetoothShare.TIMESTAMP.length());
        assertThat(info.mDestAddr).isEqualTo(destinationValue);
        assertThat(info.mFileUri).isEqualTo(null);
        assertThat(info.mFileType).isEqualTo(null);
        assertThat(info.mDeviceName).isEqualTo(deviceNameValue);
        assertThat(info.mHandoverInitiated).isEqualTo(false);
        assertThat(info.mFileName).isEqualTo(fileNameValue);
    }

    @Test
    public void fileExists_returnFalse() {
        assertThat(
                BluetoothOppUtility.fileExists(mContext, CORRECT_FORMAT_BUT_INVALID_FILE_URI)
        ).isFalse();
    }

    @Test
    public void isRecognizedFileType_withWrongFileUriAndMimeType_returnFalse() {
        assertThat(
                BluetoothOppUtility.isRecognizedFileType(mContext,
                        CORRECT_FORMAT_BUT_INVALID_FILE_URI,
                        "aWrongMimeType")
        ).isFalse();
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
    public void getStatusDescription_returnCorrectString() {
        String deviceName = "randomName";
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_PENDING, deviceName)).isEqualTo(
                "File transfer not started yet.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_RUNNING, deviceName)).isEqualTo(
                "File transfer is ongoing.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_SUCCESS, deviceName)).isEqualTo(
                "File transfer completed successfully.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_NOT_ACCEPTABLE, deviceName)).isEqualTo(
                "Content isn\'t supported.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_FORBIDDEN, deviceName)).isEqualTo(
                "Transfer forbidden by target device.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_CANCELED, deviceName)).isEqualTo(
                "Transfer canceled by user.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_FILE_ERROR, deviceName)).isEqualTo("Storage issue.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_CONNECTION_ERROR, deviceName)).isEqualTo(
                "Connection unsuccessful.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_ERROR_NO_SDCARD, deviceName)).isEqualTo(
                BluetoothOppUtility.deviceHasNoSdCard() ?
                        "No USB storage." :
                        "No SD card. Insert an SD card to save transferred files."
        );
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_ERROR_SDCARD_FULL, deviceName)).isEqualTo(
                BluetoothOppUtility.deviceHasNoSdCard() ?
                        "There isn\'t enough space on the SD card to save the file." :
                        "There isn\'t enough space in USB storage to save the file."
        );
        assertThat(BluetoothOppUtility.getStatusDescription(mContext,
                BluetoothShare.STATUS_BAD_REQUEST, deviceName)).isEqualTo(
                "Request can\'t be handled correctly.");
        assertThat(BluetoothOppUtility.getStatusDescription(mContext, 12345465,
                deviceName)).isEqualTo("Unknown error.");
    }

    @Test
    public void originalUri_trimBeforeAt() {
        Uri originalUri = Uri.parse("com.android.bluetooth.opp.BluetoothOppSendFileInfo");
        Uri uri = Uri.parse("com.android.bluetooth.opp.BluetoothOppSendFileInfo@dfe15a6");
        assertThat(BluetoothOppUtility.originalUri(uri)).isEqualTo(originalUri);
    }

    @Test
    public void fileInfo_testFileInfoFunctions() {
        assertThat(
            BluetoothOppUtility.getSendFileInfo(CORRECT_FORMAT_BUT_INVALID_FILE_URI)
        ).isEqualTo(
            BluetoothOppSendFileInfo.SEND_FILE_INFO_ERROR
        );
        assertThat(BluetoothOppUtility.generateUri(CORRECT_FORMAT_BUT_INVALID_FILE_URI,
            BluetoothOppSendFileInfo.SEND_FILE_INFO_ERROR).toString()
        ).contains(
            CORRECT_FORMAT_BUT_INVALID_FILE_URI.toString());
        try {
            BluetoothOppUtility.putSendFileInfo(CORRECT_FORMAT_BUT_INVALID_FILE_URI,
                BluetoothOppSendFileInfo.SEND_FILE_INFO_ERROR);
            BluetoothOppUtility.closeSendFileInfo(CORRECT_FORMAT_BUT_INVALID_FILE_URI);
        } catch (Exception e) {
            assertWithMessage("Exception should not happen.").fail();
        }
    }

}
